package decok.dfcdvadstf.catframe.ui.extended.components.notifications;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayContext;
import decok.dfcdvadstf.catframe.ui.overlay.OverlayManager;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;

/**
 * <p>
 * Notification overlay — plugs the {@link NotificationManager} into {@link OverlayManager}
 * as a single {@link OverlayContext#BOTH} {@link Overlay}. It is a thin adapter: all queue
 * state, animation and drawing stay in {@link NotificationManager}, keeping the manager
 * decoupled from the overlay system.
 * </p>
 * <p>
 * 通知 Overlay —— 将 {@link NotificationManager} 作为单个 {@link OverlayContext#BOTH}
 * {@link Overlay} 接入 {@link OverlayManager}。它是一个轻量适配器：所有队列状态、动画与绘制
 * 都留在 {@link NotificationManager}，从而使管理器与 Overlay 系统解耦。
 * </p>
 *
 * <h3>Render contexts</h3>
 * <p>
 * The {@code BOTH} context puts notifications on two render paths: the in-game HUD and any
 * open GUI screen. While a screen is open, {@link OverlayManager} skips the HUD pass for
 * {@code BOTH} overlays so nothing is drawn twice in the same frame.
 * </p>
 *
 * <h3>Registration / 注册</h3>
 * <p>
 * Call {@link #register()} during client pre-init to register this overlay with the
 * CatFrame {@link OverlayManager}.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class NotificationOverlay extends AbstractComponent implements Overlay {

    /** Singleton instance. / 单例实例。 */
    public static final NotificationOverlay INSTANCE = new NotificationOverlay();

    /** The wrapped global notification manager. / 被包裹的全局通知管理器。 */
    private final NotificationManager manager = NotificationManager.INSTANCE;

    private NotificationOverlay() {
        super(0, 0, 0, 0);
    }

    /**
     * @return the global {@link NotificationManager}.
     * <p>全局 {@link NotificationManager}。</p>
     */
    public NotificationManager getManager() {
        return manager;
    }

    /**
     * Register this overlay with the CatFrame {@link OverlayManager}.
     * Call once during client pre-init.
     * <p>将本 Overlay 注册到 CatFrame 的 {@link OverlayManager}。在客户端 preInit 调用一次。</p>
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
     * Forward the tick to {@link NotificationManager} and mirror its activity into this
     * overlay's visibility, so {@link OverlayManager} can skip rendering when idle.
     * <p>将 tick 转发给 {@link NotificationManager}，并把其活动状态映射到本 Overlay 的可见性，
     * 使 {@link OverlayManager} 在空闲时跳过渲染。</p>
     */
    @Override
    public void update() {
        manager.update();
        this.visible = manager.isActive();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rendering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Delegate drawing to {@link NotificationManager#render()}.
     * Called by {@link OverlayManager} through the standard render pipeline.
     * <p>将绘制委托给 {@link NotificationManager#render()}。
     * 由 {@link OverlayManager} 经标准渲染管线调用。</p>
     */
    @Override
    protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        manager.render();
    }
}
