package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Expo In easing animation. / Expo In 缓动动画。
 */
public class EasingInExpo extends EasingAnimation {

    public EasingInExpo(AbstractComponent target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::expoIn, callback);
    }
}
