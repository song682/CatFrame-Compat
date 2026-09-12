package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Bounce In easing animation. / Bounce In 缓动动画。
 */
public class EasingInBounce extends EasingAnimation {

    public EasingInBounce(Object target, float fadeIn, float fadeOut,
                          int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::bounceIn, callback);
    }
}
