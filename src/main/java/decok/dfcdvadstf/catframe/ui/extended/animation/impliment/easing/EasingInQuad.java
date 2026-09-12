package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Quad In easing animation. / Quad In 缓动动画。
 */
public class EasingInQuad extends EasingAnimation {

    public EasingInQuad(AbstractComponent target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadIn, callback);
    }
}
