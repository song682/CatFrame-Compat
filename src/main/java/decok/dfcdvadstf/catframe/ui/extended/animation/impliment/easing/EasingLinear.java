package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Linear (constant speed) easing animation. / 线性（匀速）缓动动画。
 */
public class EasingLinear extends EasingAnimation {

    public EasingLinear(AbstractComponent target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::linear, callback);
    }
}
