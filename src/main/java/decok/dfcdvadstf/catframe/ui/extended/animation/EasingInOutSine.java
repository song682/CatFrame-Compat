package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * Sine In-Out easing animation. / Sine In-Out 缓动动画。
 */
public class EasingInOutSine extends EasingAnimation {

    public EasingInOutSine(Object target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::sineInOut, callback);
    }
}
