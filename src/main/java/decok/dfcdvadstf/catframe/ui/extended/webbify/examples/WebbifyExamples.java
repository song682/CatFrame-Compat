package decok.dfcdvadstf.catframe.ui.extended.webbify.examples;

import decok.dfcdvadstf.catframe.ui.extended.webbify.WebbifyApi;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Webbify系统使用示例 —— 展示如何使用HTML+CSS创建现代化模组界面。
 * </p>
 * <p>
 * Webbify System Usage Examples — Demonstrates how to use HTML+CSS to 
 * create modern mod interfaces.
 * </p>
 *
 * <h3>Examples / 示例</h3>
 * <ul>
 *   <li>简单欢迎页面 - Simple Welcome Page</li>
 *   <li>多页面应用 - Multi-page Application</li>
 *   <li>交互式菜单 - Interactive Menu</li>
 *   <li>自定义内容 - Custom Content</li>
 * </ul>
 */
public class WebbifyExamples {
    
    private WebbifyExamples() {}
    
    // ══════════════════════════════════════════════════════════════════════
    //  基础示例
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 显示简单欢迎页面
     * <p>Show simple welcome page</p>
     */
    public static void showSimpleWelcome() {
        String html = WebbifyApi.buildWelcomePage(
            "CatFrameCompact", 
            "A modern Minecraft mod development framework"
        );
        WebbifyApi.openSimpleScreen("Welcome", html);
    }
    
    /**
     * 显示内置示例页面
     * <p>Show built-in example page</p>
     */
    public static void showBuiltInExample() {
        WebbifyApi.showExample();
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  多页面应用
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 显示多页面应用
     * <p>Show multi-page application</p>
     */
    public static void showMultiPageApp() {
        Map<String, String> pages = new HashMap<>();
        
        // Home page / 首页
        pages.put("home", 
            "<div style=\"padding: 20px;\">" +
            "<h1>CatFrameCompact Webbify</h1>" +
            "<p>Welcome to the Webbify system demo!</p>" +
            "<p>Use the navigation buttons to explore different pages.</p>" +
            "<hr>" +
            "<button onclick=\"navigate('features')\">Features</button><br><br>" +
            "<button onclick=\"navigate('settings')\">Settings</button><br><br>" +
            "<button onclick=\"navigate('about')\">About</button>" +
            "</div>");
        
        // Features page / 功能页面
        pages.put("features",
            "<div style=\"padding: 20px;\">" +
            "<h1>Features</h1>" +
            "<h2>Core Capabilities</h2>" +
            "<ul>" +
            "<li>✅ HTML rendering engine</li>" +
            "<li>✅ Theme integration</li>" +
            "<li>✅ Navigation system</li>" +
            "<li>✅ Interactive elements</li>" +
            "</ul>" +
            "<h2>Supported Elements</h2>" +
            "<ul>" +
            "<li>Headings (h1, h2)</li>" +
            "<li>Paragraphs (p)</li>" +
            "<li>Buttons (button)</li>" +
            "<li>Links (a)</li>" +
            "<li>Dividers (hr)</li>" +
            "</ul>" +
            "<hr>" +
            "<button onclick=\"navigate('home')\">Back Home</button>" +
            "</div>");
        
        // Settings page / 设置页面
        pages.put("settings",
            "<div style=\"padding: 20px;\">" +
            "<h1>Settings</h1>" +
            "<div style=\"margin-bottom: 16px;\">" +
            "<label><input type=\"checkbox\" checked> Enable notifications</label><br><br>" +
            "<label><input type=\"checkbox\"> Auto-save settings</label><br><br>" +
            "<label>Theme: <select>" +
            "<option>Dark</option>" +
            "<option>Light</option>" +
            "<option>Custom</option>" +
            "</select></label><br><br>" +
            "</div>" +
            "<button onclick=\"alert('Settings saved!')\">Save Settings</button><br><br>" +
            "<button onclick=\"navigate('home')\">Back Home</button>" +
            "</div>");
        
        // About page / 关于页面
        pages.put("about",
            "<div style=\"padding: 20px;\">" +
            "<h1>About</h1>" +
            "<p>CatFrameCompact Webbify System</p>" +
            "<p>Version: 1.0.0</p>" +
            "<p>Author: CatFrame Team</p>" +
            "<hr>" +
            "<h2>Technical Details</h2>" +
            "<p>Built on top of CatFrameCompact's theme system and UI framework.</p>" +
            "<p>Supports basic HTML elements and provides a modern web-like experience in Minecraft.</p>" +
            "<hr>" +
            "<button onclick=\"navigate('home')\">Back Home</button>" +
            "</div>");
        
        WebbifyApi.openNavigationScreen("CatFrameCompact Demo", pages);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  交互式菜单
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 显示交互式菜单
     * <p>Show interactive menu</p>
     */
    public static void showInteractiveMenu() {
        String[] menuItems = {
            "Open Inventory",
            "Show Crafting",
            "View Statistics",
            "System Settings",
            "Help & Support"
        };
        
        String[] actions = {
            "alert('Opening inventory...')",
            "alert('Showing crafting table...')",
            "alert('Statistics: Items crafted: 0')",
            "navigate('settings')",
            "alert('Help documentation not available')"
        };
        
        String html = WebbifyApi.buildMenuPage("Main Menu", menuItems, actions);
        WebbifyApi.openSimpleScreen("Main Menu", html);
    }
    
    /**
     * 显示游戏菜单
     * <p>Show game menu</p>
     */
    public static void showGameMenu() {
        String html = "<div style=\"padding: 20px; text-align: center;\">" +
            "<h1>Game Menu</h1>" +
            "<hr>" +
            "<button onclick=\"alert('Back to game')\" style=\"width: 200px; margin: 8px;\">Back to Game</button><br>" +
            "<button onclick=\"alert('Options...')\" style=\"width: 200px; margin: 8px;\">Options...</button><br>" +
            "<button onclick=\"alert('Quitting...')\" style=\"width: 200px; margin: 8px;\">Quit Game</button><br>" +
            "<hr>" +
            "<p style=\"font-size: 12px; color: #666;\">Press ESC to close</p>" +
            "</div>";
        
        WebbifyApi.openSimpleScreen("Game Menu", html);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  自定义内容示例
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 显示仪表板
     * <p>Show dashboard</p>
    */
    public static void showDashboard() {
        String html = "<div style=\"padding: 16px;\">" +
            "<h1>Dashboard</h1>" +
            "<div style=\"display: flex; gap: 16px; margin: 16px 0;\">" +
            "<div style=\"flex: 1; background: rgba(255,255,255,0.1); padding: 16px; border-radius: 8px;\">" +
            "<h3>Statistics</h3>" +
            "<p>Items crafted: 1,234</p>" +
            "<p>Blocks placed: 5,678</p>" +
            "<p>Distance traveled: 12.3 km</p>" +
            "</div>" +
            "<div style=\"flex: 1; background: rgba(255,255,255,0.1); padding: 16px; border-radius: 8px;\">" +
            "<h3>Quick Actions</h3>" +
            "<button onclick=\"alert('Action 1')\">Quick Save</button><br><br>" +
            "<button onclick=\"alert('Action 2')\">Quick Load</button>" +
            "</div>" +
            "</div>" +
            "<div style=\"background: rgba(255,255,255,0.1); padding: 16px; border-radius: 8px; margin: 16px 0;\">" +
            "<h3>Recent Activity</h3>" +
            "<p>✓ Connected to server</p>" +
            "<p>✓ Loaded world \"New World\"</p>" +
            "<p>✓ Saved settings</p>" +
            "</div>" +
            "</div>";
        
        WebbifyApi.openHeadlessScreen("Dashboard", html);
    }
    
    /**
     * 显示帮助页面
     * <p>Show help page</p>
     */
    public static void showHelpPage() {
        String html = "<div style=\"padding: 20px;\">" +
            "<h1>Help & Documentation</h1>" +
            "<h2>Getting Started</h2>" +
            "<p>Welcome to CatFrameCompact Webbify! This system allows you to create modern web-like interfaces in Minecraft.</p>" +
            "<h2>Basic HTML Elements</h2>" +
            "<ul>" +
            "<li><code>&lt;h1&gt;Heading&lt;/h1&gt;</code> - Large heading</li>" +
            "<li><code>&lt;p&gt;Paragraph&lt;/p&gt;</code> - Paragraph text</li>" +
            "<li><code>&lt;button&gt;Click me&lt;/button&gt;</code> - Button element</li>" +
            "<li><code>&lt;a href=\"url\"&gt;Link&lt;/a&gt;</code> - Navigation link</li>" +
            "</ul>" +
            "<h2>Tips</h2>" +
            "<p>• Use simple HTML for best compatibility</p>" +
            "<p>• Theme colors are automatically integrated</p>" +
            "<p>• Navigation system supports multi-page apps</p>" +
            "<hr>" +
            "<button onclick=\"alert('More help coming soon!')\">Need more help?</button>" +
            "</div>";
        
        WebbifyApi.openSimpleScreen("Help", html);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    //  演示方法
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * 显示所有可用示例
     * <p>Show all available examples</p>
     */
    public static void showAllExamples() {
        System.out.println("=== CatFrameCompact Webbify Examples ===");
        System.out.println("Available examples:");
        System.out.println("1. simpleWelcome    - Simple welcome page");
        System.out.println("2. builtInExample   - Built-in example page");
        System.out.println("3. multiPageApp     - Multi-page application");
        System.out.println("4. interactiveMenu  - Interactive menu");
        System.out.println("5. gameMenu         - Game menu");
        System.out.println("6. dashboard        - Dashboard view");
        System.out.println("7. helpPage         - Help documentation");
        System.out.println("=== Usage Example ===");
        System.out.println("WebbifyExamples.showSimpleWelcome();");
        System.out.println("WebbifyExamples.showMultiPageApp();");
    }
    
    /**
     * 根据名称显示示例
     * <p>Show example by name</p>
     */
    public static void showExample(String exampleName) {
        switch (exampleName.toLowerCase()) {
            case "simplewelcome":
            case "1":
                showSimpleWelcome();
                break;
            case "builtinexample":
            case "2":
                showBuiltInExample();
                break;
            case "multipageapp":
            case "3":
                showMultiPageApp();
                break;
            case "interactivemenu":
            case "4":
                showInteractiveMenu();
                break;
            case "gamemenu":
            case "5":
                showGameMenu();
                break;
            case "dashboard":
            case "6":
                showDashboard();
                break;
            case "helppage":
            case "7":
                showHelpPage();
                break;
            default:
                showAllExamples();
                System.out.println("Unknown example: " + exampleName);
        }
    }
    
    /**
     * 快速演示入口
     * <p>Quick demo entry point</p>
     */
    public static void demo() {
        showMultiPageApp();
    }
}