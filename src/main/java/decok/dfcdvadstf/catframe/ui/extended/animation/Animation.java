package decok.dfcdvadstf.catframe.ui.extended.animation;

/**
 * <p>
 * Minimal contract for any UI animation. Anything that can be started, ticked,
 * and queried for completion is an Animation — duck-typed.<br>
 * Implementations include {@link AbstractAnimation} (tick-driven base),
 * {@link ValueAnimation} (property interpolation via easing), and
 * {@link ScreenTransition} (screen-level fade + pop).
 * </p>
 * <p>
 * 任意 UI 动画的最小契约。任何可启动、可推进、可查询完成状态的对象都是
 * Animation —— 鸭子类型。<br>
 * 实现包括 {@link AbstractAnimation}（tick 驱动基类）、
 * {@link ValueAnimation}（经缓动的属性插值）与
 * {@link ScreenTransition}（界面级淡入淡出 + 弹出）。
 * </p>
 */
public interface Animation {

    /** Start or restart this animation from the beginning.
     *  <p>从头开始或重新开始本动画。</p> */
    void start();

    /** Advance the animation by one tick. / 推进一 tick。 */
    void tick();

    /** @return whether this animation is currently playing / 是否正在播放 */
    boolean isPlaying();

    /** @return whether this animation has completed / 是否已完成 */
    boolean isFinished();

    /**
     * Apply the current visual effect. Called each render frame while the
     * animation is active.
     * <p>应用当前视觉效果。动画活跃期间每渲染帧调用。</p>
     */
    void applyAnimation();
}
