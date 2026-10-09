package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Cubic Out easing animation. / Cubic Out 缓动动画。
 */
public class EasingOutCubic extends EasingAnimation {

    public EasingOutCubic(GuiEventListener target, float fadeIn, float fadeOut,
                          int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::cubicOut, callback);
    }
}
