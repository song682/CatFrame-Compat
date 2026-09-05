package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * <p>
 * Easing curve definitions and animation factory. Each factory method returns
 * a new {@link ValueAnimation} — a fully stateful, startable, tickable
 * {@link Animation} that interpolates a {@code float} property from one value
 * to another using the named curve.<br>
 * This is the "duck-typed" entry point: anything returned here can be passed
 * directly to {@code ScreenExtended#startAnimation(Animation)}.
 * </p>
 * <p>
 * 缓动曲线定义与动画工厂。每个工厂方法返回一个新的 {@link ValueAnimation}
 * —— 一个完整的、可启动、可推进的 {@link Animation}，使用对应曲线将
 * {@code float} 属性从一个值插值到另一个值。<br>
 * 这是"鸭子类型"入口：此处返回的任何对象都可直接传入
 * {@code ScreenExtended#startAnimation(Animation)}。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * // Each call creates a new, independent animation instance:
 * Animation fadeIn  = Easing.sineIn(10, 0F, 1F, alpha -> myAlpha = alpha);
 * Animation slideUp = Easing.backOut(8, 20F, 0F, y -> myY = y);
 *
 * startAnimation(fadeIn);
 *
 * // Curves can also be used standalone (e.g. for ScreenTransition):
 * new ScreenTransition(Type.FADE_IN, 10, Easing.Curves::sineInOut);
 * }</pre>
 */
public final class Easing {

    private Easing() {}

    // ══════════════════════════════════════════════════════════════════════
    //  Animation factory — each method returns a new ValueAnimation
    // ══════════════════════════════════════════════════════════════════════

    /** Linear (constant speed). / 线性（匀速）。 */
    public static ValueAnimation linear(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::linear, from, to, target);
    }

    /** Sine In. */
    public static ValueAnimation sineIn(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::sineIn, from, to, target);
    }

    /** Sine Out. */
    public static ValueAnimation sineOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::sineOut, from, to, target);
    }

    /** Sine In-Out. */
    public static ValueAnimation sineInOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::sineInOut, from, to, target);
    }

    /** Quad In. */
    public static ValueAnimation quadIn(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::quadIn, from, to, target);
    }

    /** Quad Out. */
    public static ValueAnimation quadOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::quadOut, from, to, target);
    }

    /** Quad In-Out. */
    public static ValueAnimation quadInOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::quadInOut, from, to, target);
    }

    /** Cubic In. */
    public static ValueAnimation cubicIn(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::cubicIn, from, to, target);
    }

    /** Cubic Out. */
    public static ValueAnimation cubicOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::cubicOut, from, to, target);
    }

    /** Cubic In-Out. */
    public static ValueAnimation cubicInOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::cubicInOut, from, to, target);
    }

    /** Quart In. */
    public static ValueAnimation quartIn(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::quartIn, from, to, target);
    }

    /** Quart Out. */
    public static ValueAnimation quartOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::quartOut, from, to, target);
    }

    /** Quart In-Out. */
    public static ValueAnimation quartInOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::quartInOut, from, to, target);
    }

    /** Expo In. */
    public static ValueAnimation expoIn(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::expoIn, from, to, target);
    }

    /** Expo Out. */
    public static ValueAnimation expoOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::expoOut, from, to, target);
    }

    /** Expo In-Out. */
    public static ValueAnimation expoInOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::expoInOut, from, to, target);
    }

    /** Back In (overshoot). */
    public static ValueAnimation backIn(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::backIn, from, to, target);
    }

    /** Back Out (overshoot). */
    public static ValueAnimation backOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::backOut, from, to, target);
    }

    /** Back In-Out (overshoot). */
    public static ValueAnimation backInOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::backInOut, from, to, target);
    }

    /** Bounce In. */
    public static ValueAnimation bounceIn(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::bounceIn, from, to, target);
    }

    /** Bounce Out. */
    public static ValueAnimation bounceOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::bounceOut, from, to, target);
    }

    /** Bounce In-Out. */
    public static ValueAnimation bounceInOut(int duration, float from, float to, Consumer<Float> target) {
        return new ValueAnimation(duration, Curves::bounceInOut, from, to, target);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Pure curve functions — usable as EasingFunction anywhere
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <p>
     * Pure easing curve functions. Each maps {@code t} in [0, 1] to an output
     * value (typically in [0, 1]). These are stateless and can be used as
     * method references for {@link EasingFunction} consumers such as
     * {@link ScreenTransition}.
     * </p>
     * <p>
     * 纯缓动曲线函数。每个函数将 [0, 1] 的 {@code t} 映射为输出值（通常在
     * [0, 1] 内）。无状态，可作为方法引用传递给 {@link EasingFunction} 消费方
     * （如 {@link ScreenTransition}）。
     * </p>
     */
    public static final class Curves {

        private Curves() {}

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
}
