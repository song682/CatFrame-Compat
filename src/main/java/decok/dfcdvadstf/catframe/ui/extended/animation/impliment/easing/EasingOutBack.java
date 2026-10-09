package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Back Out (overshoot) easing animation. / Back Out（过冲）缓动动画。
 */
public class EasingOutBack extends EasingAnimation {

    public EasingOutBack(GuiEventListener target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::backOut, callback);
    }
}
