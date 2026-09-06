package decok.dfcdvadstf.catframe.ui.extended.webbify;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.extended.ScreenExtended;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

/**
 * <p>
 * WebScreen — 继承ScreenExtended的HTML渲染屏幕，支持HTML+CSS内容显示
 * 并集成CatFrame主题系统。
 * </p>
 * <p>
 * WebScreen — HTML rendering screen extending ScreenExtended, supporting 
 * HTML+CSS content display with CatFrame theme integration.
 * </p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * // Simple page / 简单页面
 * Minecraft.getMinecraft().displayGuiScreen(
 *     new WebScreen(Text.literal("My Page"), "<h1>Hello World</h1>")
 * );
 *
 * // With navigation / 带导航功能
 * Minecraft.getMinecraft().displayGuiScreen(
 *     new WebScreen(Text.literal("Home"), homeHtml, url -> {
 *         if (url.equals("/about")) return aboutHtml;
 *         return null; // cancel navigation
 *     })
 * );
 * }</pre>
 */
public class WebScreen extends ScreenExtended {
    
    /* Chrome dimensions / 浏览器工具栏尺寸 */
    private static final int CHROME_HEIGHT  = 20;
    private static final int CHROME_BG      = 0xFF2D2D2D;
    private static final int CHROME_TEXT    = 0xFFCCCCCC;
    
    /* Config / 配置 */
    private boolean showChrome = true;
    
    /* Page state / 页面状态 */
    private WebPage currentPage;
    private final Deque<WebPage> backStack   = new ArrayDeque<>();
    private final Deque<WebPage> forwardStack = new ArrayDeque<>();
    
    /* Page loader / 页面加载器 */
    private final Function<String, WebPage> pageLoader;
    
    /**
     * 创建简单的WebScreen
     * <p>Create simple WebScreen</p>
     */
    public WebScreen(Text title, String htmlContent) {
        this(title, htmlContent, null);
    }
    
    /**
     * 创建带导航支持的WebScreen
     * <p>Create WebScreen with navigation support</p>
     */
    public WebScreen(Text title, String htmlContent, Function<String, WebPage> pageLoader) {
        super(title);
        this.pageLoader = pageLoader;
        
        // Create initial page / 创建初始页面
        this.currentPage = new WebPage(htmlContent);
        currentPage.setUrl("home");
    }
    
    /**
     * 隐藏浏览器工具栏
     * <p>Hide browser chrome</p>
     */
    public WebScreen withoutChrome() {
        this.showChrome = false;
        return this;
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Navigation / 导航
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 导航到新页面
     * <p>Navigate to new page</p>
     */
    public void navigate(String url, String htmlContent) {
        if (currentPage != null) {
            backStack.push(currentPage);
        }
        forwardStack.clear();
        
        WebPage newPage = new WebPage(htmlContent);
        newPage.setUrl(url);
        currentPage = newPage;
        
        currentPage.layout(width, height - (showChrome ? CHROME_HEIGHT : 0));
    }
    
    /**
     * 后退
     * <p>Go back</p>
     */
    public void goBack() {
        if (!backStack.isEmpty()) {
            forwardStack.push(currentPage);
            currentPage = backStack.pop();
            currentPage.layout(width, height - (showChrome ? CHROME_HEIGHT : 0));
        }
    }
    
    /**
     * 前进
     * <p>Go forward</p>
     */
    public void goForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(currentPage);
            currentPage = forwardStack.pop();
            currentPage.layout(width, height - (showChrome ? CHROME_HEIGHT : 0));
        }
    }
    
    /**
     * 刷新当前页面
     * <p>Reload current page</p>
     */
    public void reload() {
        if (currentPage != null) {
            currentPage.layout(width, height - (showChrome ? CHROME_HEIGHT : 0));
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Screen lifecycle / 屏幕生命周期
    // ══════════════════════════════════════════════════════════════════════
    
    @Override
    protected void init() {
        super.init();
        if (currentPage != null) {
            currentPage.layout(width, height - (showChrome ? CHROME_HEIGHT : 0));
        }
    }
    
    @Override
    protected void renderBackground(int mouseX, int mouseY, float partialTicks) {
        // Draw themed background / 绘制主题背景
        drawThemedPanel(0, 0, width, height);
        
        // Draw chrome / 绘制工具栏
        if (showChrome) {
            drawChrome(mouseX, mouseY);
        }
        
        // Draw page content / 绘制页面内容
        if (currentPage != null) {
            int contentY = showChrome ? CHROME_HEIGHT : 0;
            currentPage.render(0, contentY, mouseX, mouseY, partialTicks);
        }
    }
    
    /**
     * 绘制浏览器工具栏
     * <p>Draw browser chrome</p>
     */
    private void drawChrome(int mouseX, int mouseY) {
        // Background / 背景
        drawRect(0, 0, width, CHROME_HEIGHT, CHROME_BG);
        
        // Title / 标题
        String title = currentPage != null ? currentPage.getUrl() : "WebScreen";
        drawCenteredString(title, width / 2, CHROME_HEIGHT / 2 - 4, CHROME_TEXT);
        
        // Navigation buttons / 导航按钮
        int buttonX = width - 80;
        
        // Back button / 后退按钮
        boolean backHovered = mouseX >= buttonX && mouseX <= buttonX + 35 && 
                             mouseY >= 0 && mouseY <= CHROME_HEIGHT;
        drawRect(buttonX, 0, 35, CHROME_HEIGHT, backHovered ? 0xFF666666 : CHROME_BUTTON);
        drawString("← Back", buttonX + 5, CHROME_HEIGHT / 2 - 4, CHROME_TEXT);
        
        // Forward button / 前进按钮
        boolean forwardHovered = mouseX >= buttonX + 40 && mouseX <= buttonX + 75 && 
                                mouseY >= 0 && mouseY <= CHROME_HEIGHT;
        drawRect(buttonX + 40, 0, 35, CHROME_HEIGHT, forwardHovered ? 0xFF666666 : CHROME_BUTTON);
        drawString("Forward →", buttonX + 45, CHROME_HEIGHT / 2 - 4, CHROME_TEXT);
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Handle chrome clicks / 处理工具栏点击
        if (showChrome && mouseY < CHROME_HEIGHT) {
            int buttonX = width - 80;
            
            // Back button / 后退按钮
            if (mouseX >= buttonX && mouseX <= buttonX + 35) {
                goBack();
                return;
            }
            
            // Forward button / 前进按钮
            if (mouseX >= buttonX + 40 && mouseX <= buttonX + 75) {
                goForward();
                return;
            }
        }
        
        // Forward to page / 转发到页面
        if (currentPage != null) {
            int contentY = showChrome ? CHROME_HEIGHT : 0;
            if (mouseY >= contentY) {
                currentPage.mouseClicked(mouseX, mouseY - contentY, mouseButton);
            }
        }
    }
    
    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        
        if (currentPage != null) {
            int contentY = showChrome ? CHROME_HEIGHT : 0;
            if (mouseY >= contentY) {
                currentPage.mouseDragged(mouseX, mouseY - contentY, clickedMouseButton);
            }
        }
    }
    
    @Override
    public void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
        
        if (currentPage != null) {
            int contentY = showChrome ? CHROME_HEIGHT : 0;
            if (mouseY >= contentY) {
                currentPage.mouseReleased(mouseX, mouseY - contentY, mouseButton);
            }
        }
    }
    
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        
        if (currentPage != null) {
            currentPage.keyTyped(typedChar, keyCode);
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Utility methods / 工具方法
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 绘制矩形
     * <p>Draw rectangle</p>
     */
    private void drawRect(int x, int y, int w, int h, int color) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow("", x, y, color);
        // Placeholder for actual rectangle drawing
        // 实际矩形绘制的占位符
    }
    
    /**
     * 绘制字符串
     * <p>Draw string</p>
     */
    private void drawString(String text, int x, int y, int color) {
        Minecraft.getMinecraft().fontRenderer.drawString(text, x, y, color);
    }
    
    /**
     * 绘制居中字符串
     * <p>Draw centered string</p>
     */
    private void drawCenteredString(String text, int x, int y, int color) {
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
        drawString(text, x - width / 2, y, color);
    }
}