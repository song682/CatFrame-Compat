package decok.dfcdvadstf.catframe.ui.extended;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.extended.animation.AbstractAnimation;
import decok.dfcdvadstf.catframe.ui.extended.animation.Easing;
import decok.dfcdvadstf.catframe.ui.extended.animation.ScreenTransition;
import decok.dfcdvadstf.catframe.ui.extended.theme.DefaultTheme;
import decok.dfcdvadstf.catframe.ui.extended.theme.Theme;
import decok.dfcdvadstf.catframe.ui.extended.theme.ThemeKeys;
import decok.dfcdvadstf.catframe.ui.extended.theme.ThemeManager;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.catframe.ui.util.TextureStretching;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * <p>
 * Themed GUI screen base class — extends CatFrame's {@link Screen} with
 * built-in theme-aware texture, colour, and sound resolution helpers.<br>
 * Other mods extend this class to build GUIs that automatically adapt to
 * the active {@link Theme}.
 * </p>
 * <p>
 * 主题化 GUI 屏幕基类 —— 在 CatFrame 的 {@link Screen} 之上提供内置的主题感知纹理、
 * 颜色与音效解析辅助方法。其他模组可继承此类来构建自动适应当前 {@link Theme} 的界面。
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * public class MyScreen extends ScreenExtended {
 *     public MyScreen() {
 *         super(Text.of("My Screen"));
 *     }
 *
 *     @Override
 *     protected void init() {
 *         addRenderableWidget(Button.builder(Text.of("OK"), btn -> onClose())
 *                 .pos(10, 10).build());
 *     }
 *
 *     @Override
 *     protected void renderBackground(int mouseX, int mouseY, float partialTicks) {
 *         drawThemedPanel(0, 0, width, height);
 *     }
 * }
 * }</pre>
 */
@SideOnly(Side.CLIENT)
public abstract class ScreenExtended extends Screen {

    protected ScreenExtended(Text title) {
        super(title);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Animation support
    // ══════════════════════════════════════════════════════════════════════

    @Nullable
    private AbstractAnimation currentAnimation;

    /**
     * Start (or restart) the given animation.
     * <p>启动（或重新开始）给定动画。</p>
     */
    protected void startAnimation(AbstractAnimation animation) {
        this.currentAnimation = animation;
        animation.start();
    }

    /** @return the currently running animation, or {@code null} / 当前动画，或 {@code null} */
    @Nullable
    protected AbstractAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    /**
     * Convenience: start a screen transition with the given type, duration, and
     * {@link Easing#sineInOut} easing.
     * <p>便捷方法：以指定类型、持续时间和 {@link Easing#sineInOut} 缓动启动界面过渡。</p>
     */
    protected void startTransition(ScreenTransition.Type type, int duration) {
        startTransition(type, duration, Easing::sineInOut);
    }

    /**
     * Convenience: start a screen transition with full control over duration
     * and easing.
     * <p>便捷方法：完全控制持续时间与缓动函数来启动界面过渡。</p>
     */
    protected void startTransition(ScreenTransition.Type type, int duration,
                                   AbstractAnimation.EasingFunction easing) {
        startAnimation(new ScreenTransition(type, duration, easing));
    }

    /**
     * Called each tick to advance the animation. Override to add custom
     * per-tick logic alongside the animation.
     * <p>每 tick 调用以推进动画。覆写可在动画之外添加自定义 tick 逻辑。</p>
     */
    @Override
    public void tick() {
        if (currentAnimation != null && currentAnimation.isPlaying()) {
            currentAnimation.tick();
        }
    }

    /**
     * Override of {@link Screen#drawScreen} that wraps the entire render
     * pipeline with the current animation's GL state (if any). Subclasses that
     * override this method <b>must</b> call {@code super.drawScreen(...)}.
     * <p>覆写 {@link Screen#drawScreen}，以当前动画的 GL 状态包裹整个渲染管线
     * （如有）。覆写此方法的子类<b>必须</b>调用 {@code super.drawScreen(...)}。</p>
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        AbstractAnimation anim = this.currentAnimation;
        ScreenTransition st = (anim instanceof ScreenTransition && anim.isPlaying())
                ? (ScreenTransition) anim : null;

        if (st != null) {
            st.pushGlState(this.width, this.height);
        }
        try {
            super.drawScreen(mouseX, mouseY, partialTicks);
        } finally {
            if (st != null) {
                st.popGlState();
            }
        }
    }

    // ──── Visibility conflict resolution ────
    // GuiScreen declares keyTyped/mouseClicked as protected; GuiEventListener
    // requires them public. Screen already resolves this, but the Java compiler
    // re-flags the conflict on concrete subclasses. Explicit public overrides
    // here silence the error.

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Theme access
    // ══════════════════════════════════════════════════════════════════════

    /**
     * @return the currently active theme (shortcut to {@link ThemeManager}) / 当前活动主题
     */
    protected Theme getActiveTheme() {
        return ThemeManager.getInstance().getActive();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Texture resolution
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Resolve a texture key through the active theme.
     * <p>通过活动主题解析纹理键。</p>
     *
     * @return resolved texture, or {@code null} if no theme provides it
     */
    @Nullable
    protected ResourceLocation resolveTexture(String key) {
        return ThemeManager.getInstance().resolveTexture(key);
    }

    /**
     * Resolve a texture key with an explicit fallback.
     * <p>解析纹理键，若无法解析则使用显式回退值。</p>
     */
    protected ResourceLocation resolveTexture(String key, ResourceLocation fallback) {
        return ThemeManager.getInstance().resolveTextureOrDefault(key, fallback);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Colour resolution
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Resolve a colour key through the active theme.
     * <p>通过活动主题解析颜色键。</p>
     *
     * @return resolved ARGB colour, or {@code null} if no theme provides it
     */
    @Nullable
    protected Integer resolveColor(String key) {
        return ThemeManager.getInstance().resolveColor(key);
    }

    /**
     * Resolve a colour key with an explicit fallback.
     * <p>解析颜色键，若无法解析则使用显式回退值。</p>
     */
    protected int resolveColor(String key, int fallback) {
        return ThemeManager.getInstance().resolveColorOrDefault(key, fallback);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Sound
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Resolve a sound key through the active theme and play it.
     * If the theme returns {@code null} for the key, nothing is played (silent).
     * <p>通过活动主题解析音效键并播放。若主题对该键返回 {@code null}，则不播放（静音）。</p>
     */
    protected void playThemeSound(String soundKey) {
        ResourceLocation sound = ThemeManager.getInstance().resolveSound(soundKey);
        if (sound != null) {
            Minecraft.getMinecraft().getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(sound, 1.0F));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Themed rendering helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Draw a themed button background — resolves the button texture from the
     * active theme and renders it with three-patch stretching.
     * <p>绘制主题按钮背景 —— 从活动主题解析按钮纹理并以三段式拉伸渲染。</p>
     *
     * @param x        left edge / 左边缘
     * @param y        top edge / 上边缘
     * @param w        width / 宽度
     * @param h        height / 高度
     * @param active   whether the button is enabled / 按钮是否启用
     * @param hovered  whether the button is hovered / 按钮是否悬停
     */
    protected void drawThemedButton(int x, int y, int w, int h,
                                    boolean active, boolean hovered) {
        String key;
        if (!active) {
            key = ThemeKeys.Textures.BUTTON_DISABLED;
        } else if (hovered) {
            key = ThemeKeys.Textures.BUTTON_HIGHLIGHTED;
        } else {
            key = ThemeKeys.Textures.BUTTON_ENABLED;
        }
        ResourceLocation tex = resolveTexture(key,
                DefaultTheme.INSTANCE.getTexture(key));
        if (tex != null) {
            TextureStretching.drawAutoThreePatch(tex, x, y, w, h, 200, 20, 2);
        }
    }

    /**
     * Draw a themed content panel — header separator, tiled background, and
     * footer separator, all resolved from the active theme.
     * <p>绘制主题内容面板 —— 头部分隔线、平铺背景与底部分隔线，均从活动主题解析。</p>
     *
     * @param x      left edge / 左边缘
     * @param top    Y of header separator top / 头部分隔线顶部 Y
     * @param width  panel width / 面板宽度
     * @param bottom Y of footer separator top / 底部分隔线顶部 Y
     */
    protected void drawThemedPanel(int x, int top, int width, int bottom) {
        ResourceLocation header = resolveTexture(ThemeKeys.Textures.PANEL_HEADER_SEPARATOR,
                DefaultTheme.INSTANCE.getTexture(ThemeKeys.Textures.PANEL_HEADER_SEPARATOR));
        ResourceLocation footer = resolveTexture(ThemeKeys.Textures.PANEL_FOOTER_SEPARATOR,
                DefaultTheme.INSTANCE.getTexture(ThemeKeys.Textures.PANEL_FOOTER_SEPARATOR));
        ResourceLocation bg = resolveTexture(ThemeKeys.Textures.PANEL_BACKGROUND,
                DefaultTheme.INSTANCE.getTexture(ThemeKeys.Textures.PANEL_BACKGROUND));

        // Background between separators / 分隔线之间的背景
        int bgTop = top + 2;
        if (bottom > bgTop && bg != null) {
            TextureStretching.drawTiled(bg, x, bgTop, width, bottom - bgTop, 16, 16);
        }
        // Header separator / 头部分隔线
        if (header != null) {
            TextureStretching.drawTiled(header, x, top, width, 2, 32, 2);
        }
        // Footer separator / 底部分隔线
        if (footer != null) {
            TextureStretching.drawTiled(footer, x, bottom, width, 2, 32, 2);
        }
    }

    /**
     * Draw a themed toast background — resolves the default toast texture from
     * the active theme and renders it with nine-patch stretching.
     * <p>绘制主题 Toast 背景 —— 从活动主题解析默认 Toast 纹理并以九宫格拉伸渲染。</p>
     *
     * @param x left edge / 左边缘
     * @param y top edge / 上边缘
     * @param w width / 宽度
     * @param h height / 高度
     */
    protected void drawThemedToast(int x, int y, int w, int h) {
        ResourceLocation tex = resolveTexture(ThemeKeys.Textures.TOAST_DEFAULT,
                DefaultTheme.INSTANCE.getTexture(ThemeKeys.Textures.TOAST_DEFAULT));
        if (tex != null) {
            TextureStretching.drawAutoNinePatch(tex, x, y, w, h, 160, 32, 4);
        }
    }

    /**
     * Get the themed text colour for a button in the given state.
     * <p>获取按钮在给定状态下的主题文本颜色。</p>
     */
    protected int getButtonTextColor(boolean active, boolean hovered) {
        if (!active) {
            return resolveColor(ThemeKeys.Colors.BUTTON_TEXT_DISABLED, 0xA0A0A0);
        }
        if (hovered) {
            return resolveColor(ThemeKeys.Colors.BUTTON_TEXT_HOVER, 0xFFFFA0);
        }
        return resolveColor(ThemeKeys.Colors.BUTTON_TEXT_ENABLED, 0xE0E0E0);
    }

    /**
     * Get the themed text colour for a tab in the given state.
     * <p>获取 Tab 在给定状态下的主题文本颜色。</p>
     */
    protected int getTabTextColor(boolean selected, boolean hovered) {
        if (selected) {
            return resolveColor(ThemeKeys.Colors.TAB_TEXT_SELECTED, 0xFFFFFF);
        }
        if (hovered) {
            return resolveColor(ThemeKeys.Colors.TAB_TEXT_HOVERED, 0xFFFF55);
        }
        return resolveColor(ThemeKeys.Colors.TAB_TEXT_NORMAL, 0xA0A0A0);
    }
}
