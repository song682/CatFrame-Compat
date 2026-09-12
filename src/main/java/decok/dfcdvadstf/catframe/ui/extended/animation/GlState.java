package decok.dfcdvadstf.catframe.ui.extended.animation;

/**
 * <p>
 * Mutable OpenGL state data holder — populated by {@link Animation} implementations
 * and consumed by {@link AnimationEngine}. Contains no GL calls itself; it is a
 * pure data transfer object.<br>
 * 可变 OpenGL 状态数据载体 —— 由 {@link Animation} 实现类填充，由
 * {@link AnimationEngine} 消费。本身不包含任何 GL 调用；纯数据传输对象。
 * </p>
 */
public class GlState {

    private float alpha = 1.0F;
    private float scaleX = 1.0F;
    private float scaleY = 1.0F;
    private float offsetX = 0.0F;
    private float offsetY = 0.0F;
    private float offsetZ = 0.0F;
    private boolean fadeEnabled = false;
    private boolean scaleEnabled = false;

    // ──── Reset ────

    /** Reset all fields to identity defaults. / 将所有字段重置为默认值。 */
    public void reset() {
        alpha = 1.0F;
        scaleX = 1.0F;
        scaleY = 1.0F;
        offsetX = 0.0F;
        offsetY = 0.0F;
        offsetZ = 0.0F;
        fadeEnabled = false;
        scaleEnabled = false;
    }

    // ──── Copy ────

    /**
     * Copy all fields from another GlState. / 从另一个 GlState 复制所有字段。
     */
    public void copyFrom(GlState other) {
        this.alpha = other.alpha;
        this.scaleX = other.scaleX;
        this.scaleY = other.scaleY;
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
        this.offsetZ = other.offsetZ;
        this.fadeEnabled = other.fadeEnabled;
        this.scaleEnabled = other.scaleEnabled;
    }

    // ──── Builder-style setters ────

    public GlState setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public GlState setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        return this;
    }

    public GlState setOffset(float offsetX, float offsetY, float offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        return this;
    }

    public GlState setFadeEnabled(boolean fadeEnabled) {
        this.fadeEnabled = fadeEnabled;
        return this;
    }

    public GlState setScaleEnabled(boolean scaleEnabled) {
        this.scaleEnabled = scaleEnabled;
        return this;
    }

    // ──── Getters ────

    /** @return current alpha multiplier [0, 1] / 当前透明度乘数 [0, 1] */
    public float getAlpha() { return alpha; }

    /** @return current X scale factor / 当前 X 缩放因子 */
    public float getScaleX() { return scaleX; }

    /** @return current Y scale factor / 当前 Y 缩放因子 */
    public float getScaleY() { return scaleY; }

    /** @return current X offset / 当前 X 偏移 */
    public float getOffsetX() { return offsetX; }

    /** @return current Y offset / 当前 Y 偏移 */
    public float getOffsetY() { return offsetY; }

    /** @return current Z offset / 当前 Z 偏移 */
    public float getOffsetZ() { return offsetZ; }

    /** @return whether fade (alpha) effect is active / 淡入淡出效果是否激活 */
    public boolean isFadeEnabled() { return fadeEnabled; }

    /** @return whether scale effect is active / 缩放效果是否激活 */
    public boolean isScaleEnabled() { return scaleEnabled; }
}
