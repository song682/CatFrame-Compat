package decok.dfcdvadstf.catframe.ui.extended.components.notifications;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayContext;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Client-side notification manager — singleton that manages the notification queue,
 * the list of actively animating notifications, and serves as the single
 * {@link Overlay} registered with {@link OverlayManager} for rendering.<br>
 * At most {@value #MAX_VISIBLE} notifications are shown simultaneously.
 * Additional notifications are held in a pending queue and promoted as slots open up.
 * </p>
 * <p>
 * 客户端通知管理器 —— 单例，管理通知队列、活跃动画通知列表，并作为注册到
 * {@link OverlayManager} 的单个 {@link Overlay} 进行渲染。<br>
 * 最多同时显示 {@value #MAX_VISIBLE} 条通知。额外通知保存在等待队列中，
 * 待有空位时提升。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * NotificationBase n = NotificationBase.builder("Title", "Body")
 *         .durationSeconds(5)
 *         .borderColor(0xFFFF4444)
 *         .build();
 *
 * NotificationManager.show(n);
 * }</pre>
 *
 * <h3>Registration / 注册</h3>
 * <p>
 * Call {@link #register()} during client pre-init to register this manager as a
 * HUD + SCREEN overlay with the CatFrame {@link OverlayManager}. The existing
 * {@code ClientOverlayHandler} drives tick and render for all overlays.
 * </p>
 * <p>
 * 在客户端 preInit 期间调用 {@link #register()}，将本管理器作为 HUD + SCREEN
 * Overlay 注册到 CatFrame 的 {@link OverlayManager}。已有的 {@code ClientOverlayHandler}
 * 负责驱动所有 Overlay 的 tick 与渲染。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class NotificationManager extends AbstractComponent implements Overlay {

    /** Singleton instance. / 单例实例。 */
    public static final NotificationManager INSTANCE = new NotificationManager();

    /** Maximum number of notifications displayed simultaneously. / 同时显示的最大通知数。 */
    public static final int MAX_VISIBLE = 4;

    /** Currently animating notifications. / 当前正在动画中的通知。 */
    private final List<ActiveNotification> active = new ArrayList<>();

    /** Pending queue — promoted into {@link #active} when a slot opens. / 等待队列。 */
    private final Deque<Notification> pending = new ArrayDeque<>();

    private NotificationManager() {
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Show a notification. It will appear as soon as a display slot is available.
     * <p>显示一条通知。它将在显示槽位可用时立即出现。</p>
     *
     * @param notification the notification to display / 要显示的通知
     */
    public static void show(Notification notification) {
        if (notification == null) {
            return;
        }
        INSTANCE.pending.addLast(notification);
    }

    /**
     * Remove all active and pending notifications immediately.
     * <p>立即移除所有活跃和等待中的通知。</p>
     */
    public static void clearAll() {
        INSTANCE.active.clear();
        INSTANCE.pending.clear();
    }

    /**
     * @return unmodifiable view of the currently active notifications.
     * <p>当前活跃通知的只读视图。</p>
     */
    public static List<ActiveNotification> getActive() {
        return Collections.unmodifiableList(INSTANCE.active);
    }

    /**
     * Register this manager with the CatFrame {@link OverlayManager}.
     * Call once during client pre-init.
     * <p>将本管理器注册到 CatFrame 的 {@link OverlayManager}。在客户端 preInit 调用一次。</p>
     */
    public void register() {
        OverlayManager.INSTANCE.register(this);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Overlay implementation
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public OverlayContext getContext() {
        return OverlayContext.BOTH;
    }

    @Override
    public ScreenAnchor getAnchor() {
        return ScreenAnchor.TOP_RIGHT;
    }

    @Override
    public int getOffsetX() {
        return 0;
    }

    @Override
    public int getOffsetY() {
        return 0;
    }

    /**
     * Advance all active animations and promote pending notifications into empty slots.
     * Called by {@link OverlayManager#updateAll()} every client tick.
     * <p>推进所有活跃动画并将等待中的通知提升到空槽位。由 {@link OverlayManager#updateAll()}
     * 每客户端 tick 调用。</p>
     */
    @Override
    public void update() {
        // Advance animations and remove finished ones
        Iterator<ActiveNotification> it = active.iterator();
        while (it.hasNext()) {
            ActiveNotification an = it.next();
            an.update();
            if (an.isDone()) {
                it.remove();
            }
        }

        // Promote pending notifications into available slots
        while (active.size() < MAX_VISIBLE && !pending.isEmpty()) {
            active.add(new ActiveNotification(pending.pollFirst()));
        }

        // Visibility: OverlayManager only renders when isVisible() returns true
        this.visible = !active.isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render all active notification cards via {@link NotificationRenderer}.
     * Called by {@link OverlayManager} through the standard render pipeline.
     * <p>通过 {@link NotificationRenderer} 渲染所有活跃通知卡片。
     * 由 {@link OverlayManager} 经标准渲染管线调用。</p>
     */
    @Override
    protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (active.isEmpty()) {
            return;
        }
        NotificationRenderer.render(active);
    }
}
