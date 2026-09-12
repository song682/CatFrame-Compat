package decok.dfcdvadstf.catframe.ui.extended.animation;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Central animation engine — owns the animation list, drives the tick loop,
 * and performs all OpenGL state management (push/pop) on behalf of animations
 * that expose a {@link GlState}.<br>
 * Animations themselves are pure state containers; they never call GL directly.
 * The engine reads their {@link GlState} and wraps rendering accordingly.
 * </p>
 * <p>
 * 动画引擎核心 —— 管理动画列表、驱动 tick 循环，并代替暴露 {@link GlState}
 * 的动画执行所有 OpenGL 状态管理（push/pop）。<br>
 * 动画本身为纯状态容器，不直接调用 GL。引擎读取其 {@link GlState} 并据此
 * 包裹渲染。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * AnimationEngine engine = new AnimationEngine();
 * engine.add(someScreenTransition);
 *
 * // each tick:
 * engine.tickAll();
 *
 * // each render frame:
 * engine.pushGlState(screenWidth, screenHeight);
 * try {
 *     // render content...
 * } finally {
 *     engine.popGlState();
 * }
 * }</pre>
 */
public class AnimationEngine {

    private final List<Animation> animations = new ArrayList<>();

    /** Whether GL state has been pushed and needs popping.
     *  <p>GL 状态是否已推入、需要弹出。</p> */
    private boolean glPushed = false;

    // ──── Animation management / 动画管理 ────

    /**
     * Add and automatically start an animation.
     * <p>添加并自动启动一个动画。</p>
     */
    public void add(Animation animation) {
        if (animation == null) throw new IllegalArgumentException("animation must not be null");
        animations.add(animation);
        animation.start();
    }

    /**
     * Remove an animation from the engine.
     * <p>从引擎中移除一个动画。</p>
     */
    public void remove(Animation animation) {
        animations.remove(animation);
    }

    /**
     * Remove all animations from the engine.
     * <p>移除引擎中的所有动画。</p>
     */
    public void clear() {
        animations.clear();
    }

    /**
     * @return the number of active animations / 活跃动画数量
     */
    public int size() {
        return animations.size();
    }

    // ──── Tick loop / Tick 循环 ────

    /**
     * Advance all active animations by one tick. Finished animations are
     * automatically removed.
     * <p>推进所有活跃动画一个 tick。已完成的动画自动移除。</p>
     */
    public void tickAll() {
        Iterator<Animation> it = animations.iterator();
        while (it.hasNext()) {
            Animation anim = it.next();
            if (anim.isPlaying()) {
                anim.tick();
            }
            if (anim.isFinished()) {
                it.remove();
            }
        }
    }

    // ──── GL state management / GL 状态管理 ────

    /**
     * <p>
     * Scan all active animations, composite their {@link GlState} (multiply
     * alpha, multiply scale), and push the resulting GL state via
     * {@code glPushAttrib} / {@code glPushMatrix}.<br>
     * If no animation exposes a non-null GlState, this is a no-op.
     * </p>
     * <p>
     * 扫描所有活跃动画，组合其 {@link GlState}（透明度相乘、缩放相乘），
     * 并通过 {@code glPushAttrib} / {@code glPushMatrix} 推入结果 GL 状态。<br>
     * 若无动画暴露非 null 的 GlState，则为空操作。
     * </p>
     *
     * @param screenWidth  screen width for centre-pivot calculation / 屏幕宽度
     * @param screenHeight screen height for centre-pivot calculation / 屏幕高度
     */
    public void pushGlState(int screenWidth, int screenHeight) {
        // Composite all active GlStates
        float alpha = 1.0F;
        float scaleX = 1.0F;
        float scaleY = 1.0F;
        float offsetX = 0.0F;
        float offsetY = 0.0F;
        float offsetZ = 0.0F;
        boolean hasFade = false;
        boolean hasScale = false;
        boolean hasAny = false;

        for (Animation anim : animations) {
            if (!anim.isPlaying()) continue;
            GlState state = anim.getGlState();
            if (state == null) continue;

            hasAny = true;

            if (state.isFadeEnabled()) {
                hasFade = true;
                alpha *= state.getAlpha();
            }
            if (state.isScaleEnabled()) {
                hasScale = true;
                scaleX *= state.getScaleX();
                scaleY *= state.getScaleY();
            }
            offsetX += state.getOffsetX();
            offsetY += state.getOffsetY();
            offsetZ += state.getOffsetZ();
        }

        if (!hasAny) return;

        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TRANSFORM_BIT);
        GL11.glPushMatrix();

        if (hasFade) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        }

        if (hasScale && (scaleX != 1.0F || scaleY != 1.0F)) {
            float cx = screenWidth / 2.0F;
            float cy = screenHeight / 2.0F;
            GL11.glTranslatef(cx, cy, 0.0F);
            GL11.glScalef(scaleX, scaleY, 1.0F);
            GL11.glTranslatef(-cx, -cy, 0.0F);
        }

        if (offsetX != 0.0F || offsetY != 0.0F || offsetZ != 0.0F) {
            GL11.glTranslatef(offsetX, offsetY, offsetZ);
        }

        glPushed = true;
    }

    /**
     * Restore GL state after {@link #pushGlState}.
     * No-op if nothing was pushed.
     * <p>在 {@link #pushGlState} 之后恢复 GL 状态。若无推入则为空操作。</p>
     */
    public void popGlState() {
        if (!glPushed) return;
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        glPushed = false;
    }

    /**
     * @return whether there are any active animations with screen-level GL state
     *         / 是否存在具有界面级 GL 状态的活跃动画
     */
    public boolean hasActiveScreenAnimation() {
        for (Animation anim : animations) {
            if (anim.isPlaying() && anim.getGlState() != null) return true;
        }
        return false;
    }
}
