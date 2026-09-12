package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Bounce In-Out easing animation. / Bounce In-Out 缓动动画。
 */
public class EasingInOutBounce extends EasingAnimation {

    public EasingInOutBounce(AbstractComponent target, float fadeIn, float fadeOut,
                             int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::bounceInOut, callback);
    }
}
