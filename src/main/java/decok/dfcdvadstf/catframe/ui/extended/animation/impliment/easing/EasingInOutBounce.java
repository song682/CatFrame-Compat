package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Bounce In-Out easing animation. / Bounce In-Out 缓动动画。
 */
public class EasingInOutBounce extends EasingAnimation {

    public EasingInOutBounce(Object target, float fadeIn, float fadeOut,
                             int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::bounceInOut, callback);
    }
}
