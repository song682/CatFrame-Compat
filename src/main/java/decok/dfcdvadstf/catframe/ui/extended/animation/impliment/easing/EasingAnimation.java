package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.extended.animation.AbstractAnimation;
import decok.dfcdvadstf.catframe.ui.extended.animation.engine.GlState;
import decok.dfcdvadstf.catframe.ui.extended.animation.impliment.EasingFunction;

/**
 * <p>
 * Abstract base for component-targeted easing animations with configurable
 * fade-in and fade-out phases. Extends {@link AbstractAnimation} with a
 * three-phase lifecycle:<br>
 * <ol>
 *   <li><b>Fade-in</b> (0 → fadeIn): easing curve maps progress 0→1</li>
 *   <li><b>Sustain</b> (fadeIn → totalDuration−fadeOut): value holds at peak</li>
 *   <li><b>Fade-out</b> (totalDuration−fadeOut → totalDuration): reverse curve maps 1→0</li>
 * </ol>
 * Each interpolated value is delivered to a {@link Consumer} callback.
 * This class returns {@code null} from {@link #getGlState()} — it is a
 * value-only animation; GL effects are the caller's responsibility.
 * </p>
 * <p>
 * 面向组件的缓动动画抽象基类，支持可配置的淡入淡出阶段。在
 * {@link AbstractAnimation} 基础上增加三阶段生命周期：<br>
 * <ol>
 *   <li><b>淡入</b>（0 → fadeIn）：缓动曲线映射进度 0→1</li>
 *   <li><b>持续</b>（fadeIn → totalDuration−fadeOut）：值保持峰值</li>
 *   <li><b>淡出</b>（totalDuration−fadeOut → totalDuration）：反向曲线映射 1→0</li>
 * </ol>
 * 每个插值值通过 {@link Consumer} 回调投递。本类 {@link #getGlState()} 返回
 * {@code null} —— 这是纯值动画；GL 效果由调用方负责。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * EasingInCubic ease = new EasingInCubic(button, 10, 8, 60, y -> myY = y);
 * engine.add(ease);
 * }</pre>
 */
public abstract class EasingAnimation extends AbstractAnimation {

    private final AbstractComponent target;
    private final float fadeInTicks;
    private final float fadeOutTicks;
    private final Consumer<Float> callback;
    private float currentValue;

    /**
     * @param target        the target component this animation is applied to / 本动画应用到的目标组件
     * @param fadeIn        fade-in duration in ticks (float, fractional allowed) / 淡入 tick 数
     * @param fadeOut       fade-out duration in ticks (float, fractional allowed) / 淡出 tick 数
     * @param totalDuration total animation duration in ticks / 总 tick 数
     * @param curve         easing curve function / 缓动曲线函数
     * @param callback      receives each interpolated value / 接收每个插值结果
     */
    protected EasingAnimation(AbstractComponent target, float fadeIn, float fadeOut,
                              int totalDuration, EasingFunction curve,
                              Consumer<Float> callback) {
        super(totalDuration, curve);
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        if (fadeIn < 0) throw new IllegalArgumentException("fadeIn must be >= 0");
        if (fadeOut < 0) throw new IllegalArgumentException("fadeOut must be >= 0");
        if (fadeIn + fadeOut > totalDuration)
            throw new IllegalArgumentException("fadeIn + fadeOut must not exceed totalDuration");
        this.target = target;
        this.fadeInTicks = fadeIn;
        this.fadeOutTicks = fadeOut;
        this.callback = callback;
        this.currentValue = 0.0F;
    }

    @Override
    public void tick() {
        if (!isPlaying() && getElapsed() == 0) return;
        // Call super to advance elapsed
        if (isPlaying()) {
            super.tick();
        }

        int elapsed = getElapsed();
        int total = getDuration();
        float sustainEnd = total - fadeOutTicks;

        float progress;
        if (elapsed <= fadeInTicks) {
            // Phase 1: fade-in — curve maps 0→1
            float phaseT = fadeInTicks > 0 ? Math.min((float) elapsed / fadeInTicks, 1.0F) : 1.0F;
            progress = getEasing().apply(phaseT);
        } else if (elapsed <= sustainEnd) {
            // Phase 2: sustain — hold at peak
            progress = 1.0F;
        } else {
            // Phase 3: fade-out — reverse curve maps 1→0
            float fadeOutProgress = fadeOutTicks > 0
                    ? Math.min((elapsed - sustainEnd) / fadeOutTicks, 1.0F)
                    : 1.0F;
            progress = 1.0F - getEasing().apply(fadeOutProgress);
        }

        currentValue = progress;
        callback.accept(currentValue);
    }

    /** @return the most recently computed interpolated value / 最近一次插值结果 */
    public float getCurrentValue() { return currentValue; }

    /** @return the target component / 目标组件 */
    public AbstractComponent getTarget() { return target; }

    /** @return fade-in duration in ticks / 淡入 tick 数 */
    public float getFadeInTicks() { return fadeInTicks; }

    /** @return fade-out duration in ticks / 淡出 tick 数 */
    public float getFadeOutTicks() { return fadeOutTicks; }

    // Value-only animation — no screen-level GL state
    @Override
    public GlState getGlState() {
        return null;
    }
}
