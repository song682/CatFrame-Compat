package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Cubic In-Out easing animation. / Cubic In-Out 缓动动画。
 */
public class EasingInOutCubic extends EasingAnimation {

    public EasingInOutCubic(Object target, float fadeIn, float fadeOut,
                            int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::cubicInOut, callback);
    }
}
