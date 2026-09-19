package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import java.util.function.Consumer;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

/**
 * Quad Out easing animation. / Quad Out 缓动动画。
 */
public class EasingOutQuad extends EasingAnimation {

    public EasingOutQuad(GuiEventListener target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadOut, callback);
    }
}
