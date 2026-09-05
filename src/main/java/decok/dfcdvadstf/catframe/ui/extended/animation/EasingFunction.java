package decok.dfcdvadstf.catframe.ui.extended.animation;

/**
 * <p>
 * A function that maps linear progress {@code t} in [0, 1] to an eased value.
 * Used by {@link ValueAnimation}, {@link ScreenTransition}, and the
 * {@link Easing} factory.
 * </p>
 * <p>
 * 将线性进度 {@code t}（[0, 1]）映射为缓动值的函数。
 * 供 {@link ValueAnimation}、{@link ScreenTransition} 及 {@link Easing}
 * 工厂使用。
 * </p>
 */
@FunctionalInterface
public interface EasingFunction {

    /**
     * @param t linear progress in [0, 1] / 线性进度 [0, 1]
     * @return eased value, typically in [0, 1] / 缓动值，通常在 [0, 1] 内
     */
    float apply(float t);
}
