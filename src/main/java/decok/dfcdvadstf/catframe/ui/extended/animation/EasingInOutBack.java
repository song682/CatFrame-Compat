package decok.dfcdvadstf.catframe.ui.extended.animation;

import java.util.function.Consumer;

/**
 * Back In-Out (overshoot) easing animation. / Back In-Out（过冲）缓动动画。
 */
public class EasingInOutBack extends EasingAnimation {

    public EasingInOutBack(Object target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::backInOut, callback);
    }
}
