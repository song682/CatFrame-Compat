package decok.dfcdvadstf.catframe.ui.extended.animation;

/**
 * <p>
 * Base class for tick-driven UI animations. Tracks elapsed ticks against a
 * configured duration and exposes a normalised progress value (0 → 1) through
 * an easing function. Subclasses implement {@link #applyAnimation(float)} to
 * produce the concrete visual effect (e.g. alpha change, scale transform).
 * </p>
 * <p>
 * 基于 tick 驱动的 UI 动画基类。以已流逝 tick 数对比配置的持续时间，经缓动函数
 * 输出归一化进度值（0 → 1）。子类实现 {@link #applyAnimation(float)} 以产生具体的
 * 视觉效果（如透明度变化、缩放变换）。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * ScreenTransition anim = new ScreenTransition(
 *         ScreenTransition.Type.FADE_IN, 10, Easing::sineInOut);
 * anim.start();
 * // each tick:
 * anim.tick();
 * // during render:
 * anim.applyAnimation();
 * }</pre>
 */
public abstract class AbstractAnimation {

    private int duration;
    private int elapsed;
    private boolean playing;
    private boolean finished;
    private EasingFunction easing;

    /**
     * Functional interface for easing (interpolation) functions.
     * <p>缓动（插值）函数的函数式接口。</p>
     */
    @FunctionalInterface
    public interface EasingFunction {
        /**
         * Map linear progress to an eased value.
         * <p>将线性进度映射为缓动值。</p>
         *
         * @param t linear progress in [0, 1] / 线性进度 [0, 1]
         * @return eased value, typically in [0, 1] / 缓动值，通常在 [0, 1] 内
         */
        float apply(float t);
    }

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

    // ──── Lifecycle / 生命周期 ────

    /**
     * Start (or restart) this animation from the beginning.
     * <p>从头开始（或重新开始）本动画。</p>
     */
    public void start() {
        this.elapsed = 0;
        this.playing = true;
        this.finished = false;
        onStart();
    }

    /**
     * Advance the animation by one tick. No-op when not playing.
     * Automatically completes when the duration is reached.
     * <p>推进一 tick。未播放时为空操作。达到持续时间后自动结束。</p>
     */
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

    /** Force the animation to its end state immediately. / 立即将动画强制至结束状态。 */
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

    // ──── State queries / 状态查询 ────

    /** @return whether this animation is currently playing / 动画是否正在播放 */
    public boolean isPlaying() { return playing; }

    /** @return whether this animation has finished / 动画是否已结束 */
    public boolean isFinished() { return finished; }

    /** @return raw (linear) progress in [0, 1] / 原始（线性）进度 [0, 1] */
    public float getProgress() {
        return duration <= 0 ? 1.0F : Math.min((float) elapsed / duration, 1.0F);
    }

    /** @return eased progress / 缓动后的进度 */
    public float getEasedProgress() {
        return easing.apply(getProgress());
    }

    /** @return elapsed ticks / 已流逝 tick 数 */
    public int getElapsed() { return elapsed; }

    /** @return total duration in ticks / 总 tick 数 */
    public int getDuration() { return duration; }

    /** @return the current easing function / 当前缓动函数 */
    public EasingFunction getEasing() {
        return easing;
    }

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

    /**
     * Apply the visual effect for the current state. Called each render frame
     * while the animation is active.
     * <p>应用当前状态的视觉效果。动画活跃期间每渲染帧调用。</p>
     */
    public abstract void applyAnimation();
}
