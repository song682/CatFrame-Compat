package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Quart In-Out easing animation. / Quart In-Out 缓动动画。
 */
public class EasingInOutQuart extends EasingAnimation {

    public EasingInOutQuart(AbstractComponent target, float fadeIn, float fadeOut,
                            int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quartInOut, callback);
    }
}
