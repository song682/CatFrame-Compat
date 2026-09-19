package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

/**
 * Sine Out easing animation. / Sine Out 缓动动画。
 */
public class EasingOutSine extends EasingAnimation {

    public EasingOutSine(GuiEventListener target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::sineOut, callback);
    }
}
