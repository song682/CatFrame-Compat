package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Quart In-Out easing animation. / Quart In-Out 缓动动画。
 */
public class EasingInOutQuart extends EasingAnimation {

    public EasingInOutQuart(GuiEventListener target, float fadeIn, float fadeOut,
                            int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::quartInOut, callback);
    }
}
