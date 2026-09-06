package decok.dfcdvadstf.catframe.ui.extended.webbify;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * WebPage — HTML页面模型，负责HTML解析、布局计算和基础渲染。
 * </p>
 * <p>
 * WebPage — HTML page model, responsible for HTML parsing, layout calculation, 
 * and basic rendering.
 * </p>
 *
 * <h3>Lifecycle / 生命周期</h3>
 * <pre>{@code
 * 1. Construct with HTML content / 使用HTML内容构造
 * 2. Call layout(width, height) to compute boxes / 调用layout计算布局
 * 3. Call render(x, y, mouseX, mouseY, partialTicks) each frame / 每帧调用render
 * 4. Forward mouse/key events via dispatch methods / 通过分发方法转发鼠标/键盘事件
 * }</pre>
 */
public class WebPage {
    
    /* Source / 源数据 */
    private final String rawHtml;
    private String url = "";
    
    /* Parsed content / 解析内容 */
    private final List<HtmlElement> elements = new ArrayList<>();
    
    /* Layout / 布局 */
    private int layoutWidth;
    private int layoutHeight;
    private final List<LayoutBox> layoutBoxes = new ArrayList<>();
    
    /* Interaction / 交互 */
    private int scrollY = 0;
    private final List<ClickableArea> clickables = new ArrayList<>();
    
    /**
     * 创建WebPage
     * <p>Create WebPage</p>
     */
    public WebPage(String html) {
        this.rawHtml = html;
        parseHtml(html);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  HTML Parsing / HTML解析
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 解析HTML内容
     * <p>Parse HTML content</p>
     */
    private void parseHtml(String html) {
        // Remove script tags temporarily / 暂时移除脚本标签
        Pattern scriptPattern = Pattern.compile("<script[^>]*>.*?</script>", Pattern.DOTALL);
        Pattern stylePattern = Pattern.compile("<style[^>]*>.*?</style>", Pattern.DOTALL);
        
        html = scriptPattern.matcher(html).replaceAll("");
        html = stylePattern.matcher(html).replaceAll("");
        
        // Parse basic HTML elements / 解析基本HTML元素
        Pattern tagPattern = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>");
        Matcher matcher = tagPattern.matcher(html);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // Add text content before tag / 添加标签前的文本内容
            if (matcher.start() > lastEnd) {
                String text = html.substring(lastEnd, matcher.start()).trim();
                if (!text.isEmpty()) {
                    elements.add(new HtmlElement("text", text));
                }
            }
            
            // Add element / 添加元素
            boolean closing = matcher.group(1).equals("/");
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);
            
            if (!closing) {
                elements.add(new HtmlElement(tagName, attributes));
            } else {
                elements.add(new HtmlElement("/" + tagName, ""));
            }
            
            lastEnd = matcher.end();
        }
        
        // Add remaining text / 添加剩余文本
        if (lastEnd < html.length()) {
            String text = html.substring(lastEnd).trim();
            if (!text.isEmpty()) {
                elements.add(new HtmlElement("text", text));
            }
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Layout / 布局
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 计算页面布局
     * <p>Calculate page layout</p>
     */
    public void layout(int width, int height) {
        this.layoutWidth = width;
        this.layoutHeight = height;
        
        layoutBoxes.clear();
        clickables.clear();
        
        int currentY = 10;
        int currentX = 10;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        
        for (HtmlElement element : elements) {
            if (element.isClosingTag()) {
                continue; // Skip closing tags for simple layout
            }
            
            switch (element.tagName) {
                case "text":
                    // Render text / 渲染文本
                    String text = element.content;
                    int textWidth = font.getStringWidth(text);
                    
                    // Simple word wrap / 简单自动换行
                    if (currentX + textWidth > width - 20) {
                        currentX = 10;
                        currentY += font.FONT_HEIGHT + 4;
                    }
                    
                    layoutBoxes.add(new LayoutBox(currentX, currentY, textWidth, font.FONT_HEIGHT, element));
                    currentX += textWidth;
                    break;
                    
                case "h1":
                    // Heading 1 / 标题1
                    layoutBoxes.add(new LayoutBox(10, currentY, width - 20, 24, element));
                    currentY += 28;
                    currentX = 10;
                    break;
                    
                case "h2":
                    // Heading 2 / 标题2
                    layoutBoxes.add(new LayoutBox(10, currentY, width - 20, 20, element));
                    currentY += 24;
                    currentX = 10;
                    break;
                    
                case "p":
                    // Paragraph / 段落
                    layoutBoxes.add(new LayoutBox(10, currentY, width - 20, font.FONT_HEIGHT + 8, element));
                    currentY += font.FONT_HEIGHT + 12;
                    currentX = 10;
                    break;
                    
                case "button":
                    // Button / 按钮
                    int buttonWidth = Math.min(100, width - 40);
                    layoutBoxes.add(new LayoutBox(10, currentY, buttonWidth, 24, element));
                    
                    // Add clickable area / 添加可点击区域
                    String onClick = element.getAttribute("onclick");
                    if (onClick != null) {
                        clickables.add(new ClickableArea(10, currentY, buttonWidth, 24, onClick));
                    }
                    
                    currentY += 28;
                    currentX = 10;
                    break;
                    
                case "div":
                    // Div / 容器
                    layoutBoxes.add(new LayoutBox(10, currentY, width - 20, font.FONT_HEIGHT + 8, element));
                    currentY += font.FONT_HEIGHT + 12;
                    currentX = 10;
                    break;
                    
                case "br":
                    // Line break / 换行
                    currentX = 10;
                    currentY += font.FONT_HEIGHT;
                    break;
                    
                case "hr":
                    // Horizontal rule / 水平线
                    layoutBoxes.add(new LayoutBox(10, currentY, width - 20, 2, element));
                    currentY += 6;
                    currentX = 10;
                    break;
                    
                case "a":
                    // Link / 链接
                    String linkText = element.content;
                    if (linkText.isEmpty() && !element.getAttribute("href").isEmpty()) {
                        linkText = element.getAttribute("href");
                    }
                    int linkWidth = font.getStringWidth(linkText);
                    
                    if (currentX + linkWidth > width - 20) {
                        currentX = 10;
                        currentY += font.FONT_HEIGHT + 4;
                    }
                    
                    layoutBoxes.add(new LayoutBox(currentX, currentY, linkWidth, font.FONT_HEIGHT, element));
                    
                    // Add clickable area for link / 为链接添加可点击区域
                    String href = element.getAttribute("href");
                    if (href != null) {
                        clickables.add(new ClickableArea(currentX, currentY, linkWidth, font.FONT_HEIGHT, "navigate:" + href));
                    }
                    
                    currentX += linkWidth;
                    break;
                    
                default:
                    // Unknown element, treat as text / 未知元素，按文本处理
                    if (!element.content.isEmpty()) {
                        int unknownWidth = font.getStringWidth(element.content);
                        layoutBoxes.add(new LayoutBox(currentX, currentY, unknownWidth, font.FONT_HEIGHT, element));
                        currentX += unknownWidth;
                    }
                    break;
            }
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Rendering / 渲染
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 渲染页面
     * <p>Render page</p>
     */
    public void render(int x, int y, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y - scrollY, 0);
        
        for (LayoutBox box : layoutBoxes) {
            renderBox(box, mouseX - x, mouseY - y + scrollY, font, mc);
        }
        
        GL11.glPopMatrix();
        
        // Draw scroll indicator if needed / 如果需要，绘制滚动指示器
        if (scrollY > 0) {
            drawScrollIndicator(x, y, mouseX, mouseY);
        }
    }
    
    /**
     * 渲染单个元素
     * <p>Render single element</p>
     */
    private void renderBox(LayoutBox box, int mouseX, int mouseY, FontRenderer font, Minecraft mc) {
        HtmlElement element = box.element;
        int color = 0xFFFFFFFF; // Default white
        
        switch (element.tagName) {
            case "text":
                // Render text / 渲染文本
                font.drawString(element.content, box.x, box.y, color);
                break;
                
            case "h1":
                // Heading 1 / 标题1
                font.drawString(element.content, box.x, box.y + 2, 0xFFFFFF);
                break;
                
            case "h2":
                // Heading 2 / 标题2
                font.drawString(element.content, box.x, box.y + 2, 0xE0E0E0);
                break;
                
            case "p":
                // Paragraph / 段落
                if (!element.content.isEmpty()) {
                    font.drawString(element.content, box.x, box.y + 4, 0xC0C0C0);
                }
                break;
                
            case "button":
                // Button / 按钮
                boolean hovered = mouseX >= box.x && mouseX <= box.x + box.width &&
                                 mouseY >= box.y && mouseY <= box.y + box.height;
                int buttonColor = hovered ? 0xFF666666 : 0xFF444444;
                int textColor = hovered ? 0xFFFFFFFF : 0xE0E0E0;
                
                // Draw button background / 绘制按钮背景
                drawRect(box.x, box.y, box.width, box.height, buttonColor);
                
                // Draw button text / 绘制按钮文本
                String buttonText = element.content.isEmpty() ? "Button" : element.content;
                int textWidth = font.getStringWidth(buttonText);
                font.drawString(buttonText, box.x + (box.width - textWidth) / 2, box.y + 8, textColor);
                break;
                
            case "a":
                // Link / 链接
                boolean linkHovered = mouseX >= box.x && mouseX <= box.x + box.width &&
                                      mouseY >= box.y && mouseY <= box.y + box.height;
                int linkColor = linkHovered ? 0xFFFF55 : 0x55FFFF;
                
                font.drawString(element.content.isEmpty() ? "Link" : element.content, box.x, box.y, linkColor);
                
                if (linkHovered) {
                    font.drawString(element.content.isEmpty() ? "Link" : element.content, box.x, box.y, linkColor);
                    mc.fontRenderer.drawString("_", box.x + font.getStringWidth(element.content.isEmpty() ? "Link" : element.content), box.y, linkColor);
                }
                break;
                
            case "hr":
                // Horizontal rule / 水平线
                drawRect(box.x, box.y, box.width, box.height, 0xFF555555);
                break;
                
            case "div":
                // Div / 容器
                if (!element.content.isEmpty()) {
                    font.drawString(element.content, box.x, box.y + 4, 0xA0A0A0);
                }
                break;
                
            default:
                // Default text rendering / 默认文本渲染
                if (!element.content.isEmpty()) {
                    font.drawString(element.content, box.x, box.y, 0xD0D0D0);
                }
                break;
        }
    }
    
    /**
     * 绘制滚动指示器
     * <p>Draw scroll indicator</p>
     */
    private void drawScrollIndicator(int x, int y, int mouseX, int mouseY) {
        int scrollbarWidth = 10;
        int scrollbarX = x + layoutWidth - scrollbarWidth - 2;
        int scrollbarHeight = layoutHeight;
        int trackHeight = scrollbarHeight - 4;
        int thumbHeight = Math.max(20, (int)(trackHeight * ((double)layoutHeight / (layoutHeight + scrollY * 2))));
        int thumbY = y + 2 + (int)((double)scrollY / (layoutHeight + scrollY * 2) * (trackHeight - thumbHeight));
        
        // Draw track / 绘制轨道
        drawRect(scrollbarX, y + 2, scrollbarWidth - 2, trackHeight, 0xFF333333);
        
        // Draw thumb / 绘制滑块
        boolean thumbHovered = mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                              mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
        drawRect(scrollbarX + 1, thumbY, scrollbarWidth - 4, thumbHeight, thumbHovered ? 0xFF666666 : 0xFF555555);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Event Handling / 事件处理
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 处理鼠标点击
     * <p>Handle mouse click</p>
     */
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int adjustedY = mouseY + scrollY;
        
        for (ClickableArea clickable : clickables) {
            if (mouseX >= clickable.x && mouseX <= clickable.x + clickable.width &&
                adjustedY >= clickable.y && adjustedY <= clickable.y + clickable.height) {
                
                if (clickable.action.startsWith("navigate:")) {
                    String url = clickable.action.substring("navigate:".length());
                    System.out.println("[WebPage] Navigate to: " + url);
                } else {
                    System.out.println("[WebPage] Click action: " + clickable.action);
                }
                
                return;
            }
        }
    }
    
    /**
     * 处理鼠标拖拽
     * <p>Handle mouse drag</p>
     */
    public void mouseDragged(int mouseX, int mouseY, int mouseButton) {
        // Scroll handling could be added here / 可以在这里添加滚动处理
    }
    
    /**
     * 处理鼠标释放
     * <p>Handle mouse release</p>
     */
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        // End of drag handling / 拖拽处理结束
    }
    
    /**
     * 处理键盘输入
     * <p>Handle keyboard input</p>
     */
    public void keyTyped(char typedChar, int keyCode) {
        // Handle keyboard input / 处理键盘输入
        if (keyCode == 200) { // Up arrow / 上箭头
            scrollY = Math.max(0, scrollY - 20);
        } else if (keyCode == 208) { // Down arrow / 下箭头
            scrollY += 20;
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Getters & Setters / 获取器和设置器
    // ══════════════════════════════════════════════════════════════════════
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Inner Classes / 内部类
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * HTML元素
     * <p>HTML element</p>
     */
    static class HtmlElement {
        String tagName;
        String content;
        String attributes;
        
        HtmlElement(String tagName, String content) {
            this.tagName = tagName;
            this.content = content;
            this.attributes = content;
        }
        
        boolean isClosingTag() {
            return tagName.startsWith("/");
        }
        
        String getAttribute(String name) {
            Pattern attrPattern = Pattern.compile(name + "=\"([^\"]*)\"");
            Matcher matcher = attrPattern.matcher(attributes);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        }
    }
    
    /**
     * 布局框
     * <p>Layout box</p>
     */
    static class LayoutBox {
        int x, y, width, height;
        HtmlElement element;
        
        LayoutBox(int x, int y, int width, int height, HtmlElement element) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.element = element;
        }
    }
    
    /**
     * 可点击区域
     * <p>Clickable area</p>
    */
    static class ClickableArea {
        int x, y, width, height;
        String action;
        
        ClickableArea(int x, int y, int width, int height, String action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }
    }
    
    /**
     * 绘制矩形
     * <p>Draw rectangle</p>
     */
    private void drawRect(int x, int y, int width, int height, int color) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow("", x, y, color);
        // Placeholder for actual rectangle drawing
        // 实际矩形绘制的占位符
    }
}