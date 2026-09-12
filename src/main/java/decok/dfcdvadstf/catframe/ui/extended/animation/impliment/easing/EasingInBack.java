package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;

/**
 * Back In (overshoot) easing animation. / Back In（过冲）缓动动画。
 */
public class EasingInBack extends EasingAnimation {

    public EasingInBack(AbstractComponent target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::backIn, callback);
    }
}
