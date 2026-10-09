package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Quad In easing animation. / Quad In 缓动动画。
 */
public class EasingInQuad extends EasingAnimation {

    public EasingInQuad(GuiEventListener target, float fadeIn, float fadeOut,
                        int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quadIn, callback);
    }
}
