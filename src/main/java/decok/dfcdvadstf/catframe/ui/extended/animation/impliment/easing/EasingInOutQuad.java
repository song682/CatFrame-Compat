package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Quad In-Out easing animation. / Quad In-Out 缓动动画。
 */
public class EasingInOutQuad extends EasingAnimation {

    public EasingInOutQuad(GuiEventListener target, float fadeIn, float fadeOut,
                           int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadInOut, callback);
    }
}
