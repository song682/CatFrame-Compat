package decok.dfcdvadstf.catframe.ui.extended.animation;

/**
 * <p>
 * Tick-driven base implementation of {@link Animation}. Tracks elapsed ticks
 * against a configured duration and exposes a normalised progress value
 * (0 → 1) through an {@link EasingFunction}. Subclasses implement
 * {@link #applyAnimation()} to produce the concrete visual effect.
 * </p>
 * <p>
 * {@link Animation} 的 tick 驱动基础实现。以已流逝 tick 数对比配置的持续时间，
 * 经 {@link EasingFunction} 输出归一化进度值（0 → 1）。子类实现
 * {@link #applyAnimation()} 以产生具体的视觉效果。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * // Subclass example:
 * class FadeIn extends AbstractAnimation {
 *     FadeIn(int ticks, EasingFunction easing) { super(ticks, easing); }
 *     {@literal @}Override public void applyAnimation() {
 *         float alpha = getEasedProgress();
 *         // apply alpha to GL state...
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractAnimation implements Animation {

    private final int duration;
    private int elapsed;
    private boolean playing;
    private boolean finished;
    private EasingFunction easing;

    /**
     * @param duration animation duration in ticks (must be &gt; 0) / 动画持续时间（tick 数，须 &gt; 0）
     * @param easing   easing function / 缓动函数
     */
    protected AbstractAnimation(int duration, EasingFunction easing) {
        if (duration <= 0) throw new IllegalArgumentException("duration must be > 0");
        if (easing == null) throw new IllegalArgumentException("easing must not be null");
        this.duration = duration;
        this.easing = easing;
    }

    // ──── Animation lifecycle / 动画生命周期 ────

    @Override
    public void start() {
        this.elapsed = 0;
        this.playing = true;
        this.finished = false;
        onStart();
    }

    @Override
    public void tick() {
        if (!playing) return;
        elapsed++;
        if (elapsed >= duration) {
            elapsed = duration;
            playing = false;
            finished = true;
            onComplete();
        }
    }

    @Override
    public boolean isPlaying() { return playing; }

    @Override
    public boolean isFinished() { return finished; }

    // ──── Extended control / 扩展控制 ────

    /** Force the animation to its end state immediately.
     *  <p>立即将动画强制至结束状态。</p> */
    public void skip() {
        this.elapsed = duration;
        this.playing = false;
        this.finished = true;
        onComplete();
    }

    /** Cancel the animation without triggering the completion callback.
     *  <p>取消动画，不触发结束回调。</p> */
    public void cancel() {
        this.playing = false;
    }

    // ──── Progress queries / 进度查询 ────

    /** @return raw (linear) progress in [0, 1] / 原始（线性）进度 [0, 1] */
    public float getProgress() {
        return duration <= 0 ? 1.0F : Math.min((float) elapsed / duration, 1.0F);
    }

    /** @return eased progress via the configured easing function / 经缓动函数映射后的进度 */
    public float getEasedProgress() {
        return easing.apply(getProgress());
    }

    /** @return elapsed ticks / 已流逝 tick 数 */
    public int getElapsed() { return elapsed; }

    /** @return total duration in ticks / 总 tick 数 */
    public int getDuration() { return duration; }

    /** @return the current easing function / 当前缓动函数 */
    public EasingFunction getEasing() { return easing; }

    /** Replace the easing function. / 替换缓动函数。 */
    public void setEasing(EasingFunction easing) {
        if (easing == null) throw new IllegalArgumentException("easing must not be null");
        this.easing = easing;
    }

    // ──── Hooks (override in subclasses) / 钩子（子类可覆写） ────

    /** Called when the animation starts or restarts. / 动画开始或重新开始时调用。 */
    protected void onStart() {}

    /** Called when the animation reaches its end. / 动画到达结束时调用。 */
    protected void onComplete() {}
}
