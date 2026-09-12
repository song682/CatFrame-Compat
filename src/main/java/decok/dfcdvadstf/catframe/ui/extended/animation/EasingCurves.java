package decok.dfcdvadstf.catframe.ui.extended.animation;

/**
 * <p>
 * Pure easing curve functions. Each maps {@code t} in [0, 1] to an output
 * value (typically in [0, 1]). These are stateless and can be used as
 * method references for {@link EasingFunction} consumers such as
 * {@link ScreenTransition} or {@link EasingAnimation}.<br>
 * Extracted from the former {@code Easing.Curves} inner class.
 * </p>
 * <p>
 * 纯缓动曲线函数。每个函数将 [0, 1] 的 {@code t} 映射为输出值（通常在
 * [0, 1] 内）。无状态，可作为方法引用传递给 {@link EasingFunction} 消费方
 * （如 {@link ScreenTransition} 或 {@link EasingAnimation}）。<br>
 * 从原 {@code Easing.Curves} 内部类提取。
 * </p>
 */
public final class EasingCurves {

    private EasingCurves() {}

    private static final float PI = (float) Math.PI;
    private static final float C1 = 1.70158F;
    private static final float C2 = C1 * 1.525F;
    private static final float C3 = C1 + 1.0F;
    private static final float N = 7.5625F;
    private static final float D = 2.75F;

    // ──── Linear ────

    /** No easing — constant speed. / 无缓动 —— 匀速。 */
    public static float linear(float t) { return t; }

    // ──── Sine ────

    public static float sineIn(float t) {
        return 1.0F - (float) Math.cos(t * PI / 2.0F);
    }

    public static float sineOut(float t) {
        return (float) Math.sin(t * PI / 2.0F);
    }

    public static float sineInOut(float t) {
        return -(float) (Math.cos(PI * t) - 1.0F) / 2.0F;
    }

    // ──── Quad ────

    public static float quadIn(float t) { return t * t; }

    public static float quadOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    public static float quadInOut(float t) {
        return t < 0.5F ? 2.0F * t * t : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
    }

    // ──── Cubic ────

    public static float cubicIn(float t) { return t * t * t; }

    public static float cubicOut(float t) {
        float f = 1.0F - t;
        return 1.0F - f * f * f;
    }

    public static float cubicInOut(float t) {
        return t < 0.5F ? 4.0F * t * t * t
                : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
    }

    // ──── Quart ────

    public static float quartIn(float t) { return t * t * t * t; }

    public static float quartOut(float t) {
        float f = 1.0F - t;
        return 1.0F - f * f * f * f;
    }

    public static float quartInOut(float t) {
        return t < 0.5F ? 8.0F * t * t * t * t
                : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
    }

    // ──── Expo ────

    public static float expoIn(float t) {
        return t == 0.0F ? 0.0F : (float) Math.pow(2.0, 10.0 * t - 10.0);
    }

    public static float expoOut(float t) {
        return t == 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * t);
    }

    public static float expoInOut(float t) {
        if (t == 0.0F) return 0.0F;
        if (t == 1.0F) return 1.0F;
        return t < 0.5F
                ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2.0F
                : (2.0F - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2.0F;
    }

    // ──── Back (overshoot) ────

    public static float backIn(float t) {
        return C3 * t * t * t - C1 * t * t;
    }

    public static float backOut(float t) {
        float f = t - 1.0F;
        return 1.0F + C3 * f * f * f + C1 * f * f;
    }

    public static float backInOut(float t) {
        return t < 0.5F
                ? ((2.0F * t) * (2.0F * t) * (C3 * 2.0F * t - C2)) / 2.0F
                : ((2.0F * t - 2.0F) * (2.0F * t - 2.0F) * (C3 * (2.0F * t - 2.0F) + C2) + 2.0F) / 2.0F;
    }

    // ──── Bounce ────

    public static float bounceIn(float t) {
        return 1.0F - bounceOut(1.0F - t);
    }

    public static float bounceOut(float t) {
        if (t < 1.0F / D) return N * t * t;
        if (t < 2.0F / D) return N * (t -= 1.5F / D) * t + 0.75F;
        if (t < 2.5F / D) return N * (t -= 2.25F / D) * t + 0.9375F;
        return N * (t -= 2.625F / D) * t + 0.984375F;
    }

    public static float bounceInOut(float t) {
        return t < 0.5F
                ? (1.0F - bounceOut(1.0F - 2.0F * t)) / 2.0F
                : (1.0F + bounceOut(2.0F * t - 1.0F)) / 2.0F;
    }
}
