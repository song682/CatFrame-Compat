package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * Expo In easing animation. / Expo In 缓动动画。
 */
public class EasingInExpo extends EasingAnimation {

    public EasingInExpo(Object target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::expoIn, callback);
    }
}
