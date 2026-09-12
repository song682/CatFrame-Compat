package decok.dfcdvadstf.catframe.ui.extended.animation;

import decok.dfcdvadstf.catframe.ui.extended.animation.engine.AnimationEngine;
import decok.dfcdvadstf.catframe.ui.extended.animation.engine.GlState;
import decok.dfcdvadstf.catframe.ui.extended.animation.impliment.ScreenTransition;
import decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing.EasingAnimation;

/**
 * <p>
 * Minimal contract for any UI animation. Anything that can be started, ticked,
 * and queried for completion is an Animation — duck-typed.<br>
 * Implementations include {@link AbstractAnimation} (tick-driven base),
 * {@link EasingAnimation} (component-targeted easing with fade phases), and
 * {@link ScreenTransition} (screen-level fade + pop).
 * </p>
 * <p>
 * 任意 UI 动画的最小契约。任何可启动、可推进、可查询完成状态的对象都是
 * Animation —— 鸭子类型。<br>
 * 实现包括 {@link AbstractAnimation}（tick 驱动基类）、
 * {@link EasingAnimation}（面向组件的带淡入淡出阶段的缓动）与
 * {@link ScreenTransition}（界面级淡入淡出 + 弹出）。
 * </p>
 *
 * <p>
 * GL state management is handled by {@link AnimationEngine}, not by
 * implementations. Animations that need screen-level visual effects override
 * {@link #getGlState()} to expose their current state.
 * </p>
 * <p>
 * GL 状态管理由 {@link AnimationEngine} 负责，而非实现类自身。需要界面级视觉
 * 效果的动画可覆写 {@link #getGlState()} 以暴露其当前状态。
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
     * <p>
     * Return the current GL state for this animation, or {@code null} if this
     * animation does not produce screen-level visual effects (e.g. value-only
     * easing animations that deliver values via callback).<br>
     * 返回本动画当前的 GL 状态；若本动画不产生界面级视觉效果（例如仅通过回调
     * 投递值的缓动动画），则返回 {@code null}。
     * </p>
     *
     * @return mutable GL state, or {@code null} / 可变 GL 状态，或 {@code null}
     */
    default GlState getGlState() {
        return null;
    }
}
