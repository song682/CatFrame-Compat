package decok.dfcdvadstf.catframe.ui.extended.webbify;

import decok.dfcdvadstf.catframe.ui.Text;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>
 * WebbifyApi — 简化的Webbify API接口，提供便捷的HTML屏幕创建方法。
 * </p>
 * <p>
 * WebbifyApi — Simplified Webbify API interface, providing convenient HTML 
 * screen creation methods.
 * </p>
 *
 * <h3>Quick Start / 快速开始</h3>
 * <pre>{@code
 * // Simple screen / 简单屏幕
 * WebbifyApi.openSimpleScreen("Welcome", "<h1>Welcome!</h1><p>This is HTML content.</p>");
 *
 * // With navigation / 带导航
 * Map<String, String> pages = new HashMap<>();
 * pages.put("home", "<h1>Home</h1>");
 * pages.put("about", "<h1>About</h1>");
 * 
 * WebbifyApi.openNavigationScreen("My App", pages);
 * }</pre>
 */
public class WebbifyApi {
    
    private WebbifyApi() {}
    
    // ══════════════════════════════════════════════════════════════════════
    //  Quick Open Methods / 快速打开方法
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 打开简单HTML屏幕
     * <p>Open simple HTML screen</p>
     */
    public static void openSimpleScreen(String title, String htmlContent) {
        WebScreen screen = new WebScreen(Text.literal(title), htmlContent);
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }
    
    /**
     * 打开带导航的HTML屏幕
     * <p>Open HTML screen with navigation</p>
     */
    public static void openNavigationScreen(String title, Map<String, String> pages) {
        WebScreen screen = new WebScreen(Text.literal(title), pages.get("home"), url -> {
            String html = pages.get(url);
            if (html != null) {
                return new WebPage(html);
            }
            return null;
        });
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }
    
    /**
     * 打开无工具栏的HTML屏幕
     * <p>Open HTML screen without chrome</p>
     */
    public static void openHeadlessScreen(String title, String htmlContent) {
        WebScreen screen = new WebScreen(Text.literal(title), htmlContent);
        Minecraft.getMinecraft().displayGuiScreen(screen.withoutChrome());
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Screen Creation Methods / 屏幕创建方法
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 创建WebScreen
     * <p>Create WebScreen</p>
     */
    public static WebScreen createScreen(String title, String htmlContent) {
        return new WebScreen(Text.literal(title), htmlContent);
    }
    
    /**
     * 创建带导航的WebScreen
     * <p>Create WebScreen with navigation</p>
     */
    public static WebScreen createNavigationScreen(String title, String initialHtml, 
                                                   Function<String, WebPage> pageLoader) {
        return new WebScreen(Text.literal(title), initialHtml, pageLoader);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  HTML Content Builders / HTML内容构建器
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 构建简单的HTML页面
     * <p>Build simple HTML page</p>
     */
    public static String buildSimplePage(String title, String bodyContent) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + 
               title + "</title></head><body>" + bodyContent + "</body></html>";
    }
    
    /**
     * 构建欢迎页面
     * <p>Build welcome page</p>
     */
    public static String buildWelcomePage(String appName, String description) {
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"text-align: center; padding: 20px;\">");
        html.append("<h1>").append(appName).append("</h1>");
        html.append("<p>").append(description).append("</p>");
        html.append("<hr>");
        html.append("<p>Powered by CatFrameCompact Webbify</p>");
        html.append("</div>");
        return buildSimplePage(appName, html.toString());
    }
    
    /**
     * 构建错误页面
     * <p>Build error page</p>
     */
    public static String buildErrorPage(String errorMessage) {
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"text-align: center; padding: 20px; color: #ff6666;\">");
        html.append("<h1>Error</h1>");
        html.append("<p>").append(errorMessage).append("</p>");
        html.append("<button onclick=\"window.close()\">Close</button>");
        html.append("</div>");
        return buildSimplePage("Error", html.toString());
    }
    
    /**
     * 构建菜单页面
     * <p>Build menu page</p>
     */
    public static String buildMenuPage(String title, String[] menuItems, String[] actions) {
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"padding: 20px;\">");
        html.append("<h1>").append(title).append("</h1>");
        
        for (int i = 0; i < menuItems.length; i++) {
            String action = i < actions.length ? actions[i] : "alert('Action: " + menuItems[i] + "')";
            html.append("<button onclick=\"").append(action).append("\">").append(menuItems[i]).append("</button><br><br>");
        }
        
        html.append("</div>");
        return buildSimplePage(title, html.toString());
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  Utility Methods / 工具方法
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 创建多页面系统
     * <p>Create multi-page system</p>
     */
    public static Map<String, String> createMultiPageSystem(String appName) {
        Map<String, String> pages = new HashMap<>();
        
        // Home page / 首页
        pages.put("home", buildWelcomePage(appName, "Welcome to " + appName + "!"));
        
        // About page / 关于页面
        pages.put("about", buildSimplePage("About", 
            "<h1>About</h1>" +
            "<p>" + appName + " is powered by CatFrameCompact Webbify system.</p>" +
            "<p>Version: 1.0.0</p>"));
        
        return pages;
    }
    
    /**
     * 显示示例页面
     * <p>Show example page</p>
     */
    public static void showExample() {
        String html = buildSimplePage("Webbify Example", 
            "<div style=\"padding: 20px;\">" +
            "<h1>CatFrameCompact Webbify</h1>" +
            "<p>This is an example HTML page rendered in Minecraft!</p>" +
            "<h2>Features / 特性</h2>" +
            "<ul>" +
            "<li>HTML rendering support</li>" +
            "<li>Theme integration</li>" +
            "<li>Navigation system</li>" +
            "<li>Interactive elements</li>" +
            "</ul>" +
            "<h2>Interactive Demo / 交互演示</h2>" +
            "<button onclick=\"alert('Button clicked!')\">Click Me!</button>" +
            "<button onclick=\"alert('Another button!')\">And Me!</button>" +
            "<hr>" +
            "<a href=\"about\">About Page</a>" +
            "</div>");
        
        openSimpleScreen("Webbify Example", html);
    }
}