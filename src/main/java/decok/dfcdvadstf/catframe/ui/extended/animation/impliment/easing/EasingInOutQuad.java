package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Quad In-Out easing animation. / Quad In-Out 缓动动画。
 */
public class EasingInOutQuad extends EasingAnimation {

    public EasingInOutQuad(Object target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadInOut, callback);
    }
}
