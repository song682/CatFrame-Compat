package decok.dfcdvadstf.catframe.ui.extended.animation;

import org.lwjgl.opengl.GL11;

/**
 * <p>
 * A screen-level transition animation that combines fade (alpha) and pop (scale)
 * effects. Either or both effects can be active simultaneously, each with its
 * own duration and shared easing function.
 * </p>
 * <p>
 * 界面级别的过渡动画，组合淡入淡出（透明度）与弹出进入（缩放）效果。
 * 两种效果可独立启用，各自拥有独立的持续时间，共享缓动函数。
 * </p>
 *
 * <h3>Effect types / 效果类型</h3>
 * <ul>
 *   <li>{@link Type#FADE_IN FADE_IN} — alpha 0→1</li>
 *   <li>{@link Type#FADE_OUT FADE_OUT} — alpha 1→0</li>
 *   <li>{@link Type#POP_IN POP_IN} — scale 0.8→1.0 (from centre)</li>
 *   <li>{@link Type#POP_OUT POP_OUT} — scale 1.0→0.8 (to centre)</li>
 *   <li>{@link Type#FADE_AND_POP_IN} — both combined</li>
 *   <li>{@link Type#FADE_AND_POP_OUT} — both combined</li>
 * </ul>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * // Fade + pop in over 10 ticks with sineInOut easing:
 * ScreenTransition open = new ScreenTransition(
 *         ScreenTransition.Type.FADE_AND_POP_IN, 10, Easing.Curves::sineInOut);
 * open.start();
 * }</pre>
 */
public class ScreenTransition extends AbstractAnimation {

    /**
     * Transition type. The {@code *_IN} variants animate from invisible to
     * visible; the {@code *_OUT} variants animate in reverse.
     * <p>过渡类型。{@code *_IN} 变体从不可见过渡到可见；{@code *_OUT} 变体反向。</p>
     */
    public enum Type {
        FADE_IN, FADE_OUT,
        POP_IN, POP_OUT,
        FADE_AND_POP_IN, FADE_AND_POP_OUT
    }

    private final Type type;
    private final int fadeDuration;
    private final int popDuration;

    /** Current alpha multiplier [0, 1]. Read by {@link #applyAnimation()}.
     *  <p>当前透明度乘数 [0, 1]。由 {@link #applyAnimation()} 读取。</p> */
    private float currentAlpha = 1.0F;

    /** Current scale factor (1.0 = normal size). Read by {@link #applyAnimation()}.
     *  <p>当前缩放因子（1.0 = 正常尺寸）。由 {@link #applyAnimation()} 读取。</p> */
    private float currentScale = 1.0F;

    /**
     * Create a transition with independent fade and pop durations.
     * <p>创建具有独立淡入淡出和弹出持续时间的过渡动画。</p>
     *
     * @param type         transition type / 过渡类型
     * @param fadeDuration fade duration in ticks (0 to disable) / 淡入淡出 tick 数（0 则禁用）
     * @param popDuration  pop duration in ticks (0 to disable) / 弹出 tick 数（0 则禁用）
     * @param easing       easing function / 缓动函数
     */
    public ScreenTransition(Type type, int fadeDuration, int popDuration, EasingFunction easing) {
        super(Math.max(fadeDuration, popDuration), easing);
        if (fadeDuration <= 0 && popDuration <= 0)
            throw new IllegalArgumentException("at least one duration must be > 0");
        this.type = type;
        this.fadeDuration = fadeDuration;
        this.popDuration = popDuration;
    }

    /**
     * Create a transition where fade and pop share the same duration.
     * <p>创建淡入淡出与弹出共享相同持续时间的过渡动画。</p>
     */
    public ScreenTransition(Type type, int duration, EasingFunction easing) {
        this(type, duration, duration, easing);
    }

    @Override
    public void tick() {
        if (!isPlaying()) return;
        super.tick();

        currentAlpha = 1.0F;
        currentScale = 1.0F;

        if (hasFade()) {
            float fadeT = fadeDuration > 0 ? Math.min((float) getElapsed() / fadeDuration, 1.0F) : 1.0F;
            float easedFade = getEasing().apply(fadeT);
            switch (type) {
                case FADE_IN:
                case FADE_AND_POP_IN:
                    currentAlpha = easedFade;
                    break;
                case FADE_OUT:
                case FADE_AND_POP_OUT:
                    currentAlpha = 1.0F - easedFade;
                    break;
                default:
                    break;
            }
        }

        if (hasPop()) {
            float popT = popDuration > 0 ? Math.min((float) getElapsed() / popDuration, 1.0F) : 1.0F;
            float easedPop = getEasing().apply(popT);
            switch (type) {
                case POP_IN:
                case FADE_AND_POP_IN:
                    currentScale = 0.8F + 0.2F * easedPop;
                    break;
                case POP_OUT:
                case FADE_AND_POP_OUT:
                    currentScale = 1.0F - 0.2F * easedPop;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Apply the current transition state to OpenGL.
     * Pushes GL attributes and matrix, enables blending with the current alpha,
     * and applies scale transform from the screen centre if pop is active.
     * <p>将当前过渡状态应用到 OpenGL。推入 GL 属性和矩阵、以当前透明度启用混合，
     * 并在弹出激活时从界面中心应用缩放变换。</p>
     *
     * @param screenWidth  screen width for centre calculation / 屏幕宽度
     * @param screenHeight screen height for centre calculation / 屏幕高度
     */
    public void pushGlState(int screenWidth, int screenHeight) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        if (hasFade()) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, currentAlpha);
        }

        if (hasPop() && currentScale != 1.0F) {
            float cx = screenWidth / 2.0F;
            float cy = screenHeight / 2.0F;
            GL11.glTranslatef(cx, cy, 0.0F);
            GL11.glScalef(currentScale, currentScale, 1.0F);
            GL11.glTranslatef(-cx, -cy, 0.0F);
        }
    }

    /**
     * Restore GL state after {@link #pushGlState}.
     * <p>在 {@link #pushGlState} 之后恢复 GL 状态。</p>
     */
    public void popGlState() {
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    // ──── Accessors / 访问器 ────

    /** @return the transition type / 过渡类型 */
    public Type getType() { return type; }

    /** @return current alpha value [0, 1] / 当前透明度 [0, 1] */
    public float getCurrentAlpha() { return currentAlpha; }

    /** @return current scale factor / 当前缩放因子 */
    public float getCurrentScale() { return currentScale; }

    /** @return whether this transition includes a fade effect / 是否包含淡入淡出效果 */
    public boolean hasFade() {
        return type == Type.FADE_IN || type == Type.FADE_OUT
                || type == Type.FADE_AND_POP_IN || type == Type.FADE_AND_POP_OUT;
    }

    /** @return whether this transition includes a pop (scale) effect / 是否包含弹出效果 */
    public boolean hasPop() {
        return type == Type.POP_IN || type == Type.POP_OUT
                || type == Type.FADE_AND_POP_IN || type == Type.FADE_AND_POP_OUT;
    }

    // AbstractAnimation.applyAnimation is not used directly for ScreenTransition;
    // callers use pushGlState/popGlState instead.
    @Override
    public void applyAnimation() {
        // No-op: ScreenTransition uses pushGlState(int, int) / popGlState() instead.
    }
}
