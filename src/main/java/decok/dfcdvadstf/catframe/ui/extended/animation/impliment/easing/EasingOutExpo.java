package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Expo Out easing animation. / Expo Out 缓动动画。
 */
public class EasingOutExpo extends EasingAnimation {

    public EasingOutExpo(AbstractComponent target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::expoOut, callback);
    }
}
