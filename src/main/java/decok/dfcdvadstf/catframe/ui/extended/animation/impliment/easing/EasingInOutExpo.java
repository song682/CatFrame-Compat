package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Expo In-Out easing animation. / Expo In-Out 缓动动画。
 */
public class EasingInOutExpo extends EasingAnimation {

    public EasingInOutExpo(Object target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::expoInOut, callback);
    }
}
