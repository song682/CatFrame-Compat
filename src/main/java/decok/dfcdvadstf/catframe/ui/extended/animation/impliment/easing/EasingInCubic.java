package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Cubic In easing animation. / Cubic In 缓动动画。
 */
public class EasingInCubic extends EasingAnimation {

    public EasingInCubic(GuiEventListener target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::cubicIn, callback);
    }
}
