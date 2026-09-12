package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * Bounce Out easing animation. / Bounce Out 缓动动画。
 */
public class EasingOutBounce extends EasingAnimation {

    public EasingOutBounce(Object target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::bounceOut, callback);
    }
}
