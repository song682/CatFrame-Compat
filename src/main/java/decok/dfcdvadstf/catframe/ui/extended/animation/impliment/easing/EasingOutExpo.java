package decok.dfcdvadstf.catframe.ui.extended.animation.impliment.easing;

import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Expo Out easing animation. / Expo Out 缓动动画。
 */
public class EasingOutExpo extends EasingAnimation {

    public EasingOutExpo(GuiEventListener target, float fadeIn, float fadeOut,
                         int totalDuration, Consumer<Float> callback) {
        super(target, fadeIn, fadeOut, totalDuration, EasingCurves::expoOut, callback);
    }
}
