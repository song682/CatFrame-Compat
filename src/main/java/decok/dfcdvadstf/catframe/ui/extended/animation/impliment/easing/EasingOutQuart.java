package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Quart Out easing animation. / Quart Out 缓动动画。
 */
public class EasingOutQuart extends EasingAnimation {

    public EasingOutQuart(AbstractComponent target, float fadeIn, float fadeOut,
                          int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quartOut, callback);
    }
}
