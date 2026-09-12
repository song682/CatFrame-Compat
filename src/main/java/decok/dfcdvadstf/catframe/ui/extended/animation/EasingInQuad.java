package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * Quad In easing animation. / Quad In 缓动动画。
 */
public class EasingInQuad extends EasingAnimation {

    public EasingInQuad(Object target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadIn, callback);
    }
}
