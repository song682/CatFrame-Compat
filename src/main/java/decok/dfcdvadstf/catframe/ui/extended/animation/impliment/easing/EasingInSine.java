package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Sine In easing animation. / Sine In 缓动动画。
 */
public class EasingInSine extends EasingAnimation {

    public EasingInSine(GuiEventListener target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::sineIn, callback);
    }
}
