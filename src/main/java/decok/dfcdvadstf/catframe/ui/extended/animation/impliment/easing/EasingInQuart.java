package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Quart In easing animation. / Quart In 缓动动画。
 */
public class EasingInQuart extends EasingAnimation {

    public EasingInQuart(Object target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quartIn, callback);
    }
}
