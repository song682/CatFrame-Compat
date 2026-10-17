package decok.dfcdvadstf.catframe.ui.extended.components.notifications;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Client-side notification manager — singleton that owns the notification queue,
 * the list of actively animating notifications, and the per-tick lifecycle logic.<br>
 * It is deliberately decoupled from the CatFrame overlay system: this class holds no
 * {@code Overlay} contract and knows nothing about registration or anchors. The thin
 * {@link NotificationOverlay} plugs it into the {@link decok.dfcdvadstf.catframe.ui.overlay.OverlayManager}
 * and forwards tick/render calls here.<br>
 * At most {@value #MAX_VISIBLE} notifications are shown simultaneously.
 * Additional notifications are held in a pending queue and promoted as slots open up.
 * </p>
 * <p>
 * 客户端通知管理器 —— 单例，管理通知队列、活跃动画通知列表与每 tick 的生命周期逻辑。<br>
 * 它与 CatFrame Overlay 系统刻意解耦：本类不实现 {@code Overlay} 契约，也完全不关心注册或锚点。
 * 轻量的 {@link NotificationOverlay} 负责将其接入
 * {@link decok.dfcdvadstf.catframe.ui.overlay.OverlayManager}，并把 tick / 渲染调用转发到这里。<br>
 * 最多同时显示 {@value #MAX_VISIBLE} 条通知。额外通知保存在等待队列中，待有空位时提升。
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
 * Register {@link NotificationOverlay#INSTANCE} during client pre-init; it wires this
 * manager into the CatFrame {@code OverlayManager}. The existing
 * {@code ClientOverlayHandler} then drives tick and render for all overlays.
 * </p>
 * <p>
 * 在客户端 preInit 期间注册 {@link NotificationOverlay#INSTANCE}，将本管理器接入
 * CatFrame 的 {@code OverlayManager}。已有的 {@code ClientOverlayHandler} 随后负责驱动
 * 所有 Overlay 的 tick 与渲染。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class NotificationManager {

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
     * @return {@code true} while at least one notification is on screen or queued.
     * <p>屏幕上有通知或队列非空时返回 {@code true}。</p>
     */
    public boolean isActive() {
        return !active.isEmpty() || !pending.isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Advance all active animations and promote pending notifications into empty slots.
     * Called by {@link NotificationOverlay#update()} every client tick.
     * <p>推进所有活跃动画并将等待中的通知提升到空槽位。
     * 由 {@link NotificationOverlay#update()} 每客户端 tick 调用。</p>
     */
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
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Render all active notification cards via {@link NotificationRenderer}.
     * Called by {@link NotificationOverlay} through the standard render pipeline.
     * <p>通过 {@link NotificationRenderer} 渲染所有活跃通知卡片。
     * 由 {@link NotificationOverlay} 经标准渲染管线调用。</p>
     */
    public void render() {
        if (active.isEmpty()) {
            return;
        }
        NotificationRenderer.render(active);
    }
}
