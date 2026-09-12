package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Bounce In easing animation. / Bounce In 缓动动画。
 */
public class EasingInBounce extends EasingAnimation {

    public EasingInBounce(AbstractComponent target, float fadeIn, float fadeOut,
                          int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::bounceIn, callback);
    }
}
