package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * Linear (constant speed) easing animation. / 线性（匀速）缓动动画。
 */
public class EasingLinear extends EasingAnimation {

    public EasingLinear(Object target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::linear, callback);
    }
}
