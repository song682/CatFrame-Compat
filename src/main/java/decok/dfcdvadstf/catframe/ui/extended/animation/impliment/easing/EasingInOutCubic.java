package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Cubic In-Out easing animation. / Cubic In-Out 缓动动画。
 */
public class EasingInOutCubic extends EasingAnimation {

    public EasingInOutCubic(GuiEventListener target, float fadeIn, float fadeOut,
                            int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::cubicInOut, callback);
    }
}
