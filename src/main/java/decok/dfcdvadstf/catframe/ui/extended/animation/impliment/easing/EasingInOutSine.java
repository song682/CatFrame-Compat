package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Sine In-Out easing animation. / Sine In-Out 缓动动画。
 */
public class EasingInOutSine extends EasingAnimation {

    public EasingInOutSine(AbstractComponent target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::sineInOut, callback);
    }
}
