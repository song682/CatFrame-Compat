package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Quad Out easing animation. / Quad Out 缓动动画。
 */
public class EasingOutQuad extends EasingAnimation {

    public EasingOutQuad(Object target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadOut, callback);
    }
}
