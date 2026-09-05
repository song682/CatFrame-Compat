package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * <p>
 * A property-interpolating animation: eases a single {@code float} value from
 * {@code start} to {@code end} over a given duration and delivers each
 * interpolated value to a {@link Consumer}.<br>
 * Typically created via the {@link Easing} factory, e.g.
 * {@code Easing.sineIn(10, 0, 1, alpha -> ...)}.
 * </p>
 * <p>
 * 属性插值动画：在给定持续时间内将单个 {@code float} 值从 {@code start}
 * 缓动至 {@code end}，并将每个插值结果投递给 {@link Consumer}。<br>
 * 通常通过 {@link Easing} 工厂创建，如
 * {@code Easing.sineIn(10, 0, 1, alpha -> ...)}。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * Animation fadeIn = Easing.sineIn(10, 0F, 1F, alpha -> {
 *     GlStateManager.enableBlend();
 *     GlStateManager.color(1, 1, 1, alpha);
 * });
 * fadeIn.start();
 * }</pre>
 */
public class ValueAnimation extends AbstractAnimation {

    private final float startValue;
    private final float endValue;
    private final Consumer<Float> consumer;
    private float currentValue;

    /**
     * @param duration ticks / 持续时间（tick 数）
     * @param easing   easing curve / 缓动曲线
     * @param from     start value / 起始值
     * @param to       end value / 结束值
     * @param target   receives each interpolated value / 接收每个插值结果
     */
    public ValueAnimation(int duration, EasingFunction easing,
                          float from, float to, Consumer<Float> target) {
        super(duration, easing);
        if (target == null) throw new IllegalArgumentException("target consumer must not be null");
        this.startValue = from;
        this.endValue = to;
        this.consumer = target;
        this.currentValue = from;
    }

    @Override
    public void tick() {
        super.tick();
        float t = getEasedProgress();
        currentValue = startValue + (endValue - startValue) * t;
        consumer.accept(currentValue);
    }

    @Override
    public void applyAnimation() {
        // Value is delivered via the consumer during tick();
        // no additional GL work needed here.
    }

    /** @return the most recently computed interpolated value / 最近一次插值结果 */
    public float getCurrentValue() { return currentValue; }

    /** @return the start value / 起始值 */
    public float getStartValue() { return startValue; }

    /** @return the end value / 结束值 */
    public float getEndValue() { return endValue; }
}
