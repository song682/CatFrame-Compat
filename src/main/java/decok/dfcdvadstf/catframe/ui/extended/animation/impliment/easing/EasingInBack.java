package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

/**
 * Back In (overshoot) easing animation. / Back In（过冲）缓动动画。
 */
public class EasingInBack extends EasingAnimation {

    public EasingInBack(Object target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::backIn, callback);
    }
}
