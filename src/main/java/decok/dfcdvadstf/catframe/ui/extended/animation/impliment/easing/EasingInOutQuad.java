package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Quad In-Out easing animation. / Quad In-Out 缓动动画。
 */
public class EasingInOutQuad extends EasingAnimation {

    public EasingInOutQuad(AbstractComponent target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadInOut, callback);
    }
}
