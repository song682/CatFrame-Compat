package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Quart Out easing animation. / Quart Out 缓动动画。
 */
public class EasingOutQuart extends EasingAnimation {

    public EasingOutQuart(Object target, float fadeIn, float fadeOut,
                          int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quartOut, callback);
    }
}
