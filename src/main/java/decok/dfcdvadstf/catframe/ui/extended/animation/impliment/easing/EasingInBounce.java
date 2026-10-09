package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Bounce In easing animation. / Bounce In 缓动动画。
 */
public class EasingInBounce extends EasingAnimation {

    public EasingInBounce(GuiEventListener target, float fadeIn, float fadeOut,
                          int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::bounceIn, callback);
    }
}
