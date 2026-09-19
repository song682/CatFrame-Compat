package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

/**
 * Bounce Out easing animation. / Bounce Out 缓动动画。
 */
public class EasingOutBounce extends EasingAnimation {

    public EasingOutBounce(GuiEventListener target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::bounceOut, callback);
    }
}
