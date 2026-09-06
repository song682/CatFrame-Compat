# CatFrameCompact Webbify System

CatFrameCompact Webbify 是一个轻量级的HTML渲染系统，为 Minecraft 1.7.10 提供现代化的Web式UI开发体验。该系统基于WebLib的设计理念，集成CatFrame主题系统，支持HTML+CSS渲染和交互式界面。

## 特性

### 🎨 主题化集成
- 自动继承CatFrame主题系统
- 与ScreenExtended无缝集成
- 主题颜色和纹理支持

### ⚡ HTML渲染引擎
- 基础HTML元素支持（h1, h2, p, button, a, div, hr等）
- 自动文本换行和布局计算
- 内置交互元素（按钮、链接）

### 🎯 导航系统
- 多页面应用支持
- 前进/后退导航历史
- 页面加载器回调机制

### 🌐 简易API
- 单行代码创建HTML屏幕
- 预设页面模板
- 事件处理系统

## 快速开始

### 基础使用

```java
import decok.dfcdvadstf.catframe.ui.extended.webbify.WebbifyApi;

// 简单HTML屏幕
WebbifyApi.openSimpleScreen("Welcome", 
    "<h1>Hello World</h1><p>This is HTML content!</p>");

// 内置示例
WebbifyApi.showExample();
```

### 高级用法

```java
import decok.dfcdvadstf.catframe.ui.extended.webbify.WebbifyApi;
import java.util.HashMap;
import java.util.Map;

// 多页面应用
Map<String, String> pages = new HashMap<>();
pages.put("home", "<h1>Home</h1>");
pages.put("about", "<h1>About</h1>");
pages.put("settings", "<h1>Settings</h1>");

WebbifyApi.openNavigationScreen("My App", pages);
```

## API 文档

### WebbifyApi

#### 快速打开方法
- `openSimpleScreen(title, html)` - 打开简单HTML屏幕
- `openNavigationScreen(title, pages)` - 打开多页面应用
- `openHeadlessScreen(title, html)` - 打开无工具栏屏幕

#### 屏幕创建方法
- `createScreen(title, html)` - 创建WebScreen
- `createNavigationScreen(title, initialHtml, pageLoader)` - 创建带导航的屏幕

#### HTML构建工具
- `buildSimplePage(title, body)` - 构建简单页面
- `buildWelcomePage(appName, description)` - 构建欢迎页面
- `buildMenuPage(title, menuItems, actions)` - 构建菜单页面
- `buildErrorPage(errorMessage)` - 构建错误页面

### WebScreen

WebScreen是主要的屏幕类，继承自ScreenExtended。

#### 构造方法
```java
WebScreen(Text title, String htmlContent)
WebScreen(Text title, String htmlContent, Function<String, WebPage> pageLoader)
```

#### 导航方法
- `navigate(url, htmlContent)` - 导航到新页面
- `goBack()` - 后退
- `goForward()` - 前进
- `reload()` - 刷新当前页面

#### 配置方法
- `withoutChrome()` - 隐藏浏览器工具栏

### WebPage

WebPage是页面模型，负责HTML解析和布局计算。

#### 生命周期
```java
WebPage page = new WebPage(htmlContent);  // 1. 创建
page.layout(width, height);              // 2. 计算布局
page.render(x, y, mouseX, mouseY, partialTicks); // 3. 渲染
```

#### 事件处理
- `mouseClicked(mouseX, mouseY, mouseButton)` - 鼠标点击
- `mouseDragged(mouseX, mouseY, mouseButton)` - 鼠标拖拽
- `mouseReleased(mouseX, mouseY, mouseButton)` - 鼠标释放
- `keyTyped(typedChar, keyCode)` - 键盘输入

## 支持的HTML元素

| 元素 | 支持状态 | 说明 |
|------|----------|------|
| `<h1>` | ✅ | 一级标题 |
| `<h2>` | ✅ | 二级标题 |
| `<p>` | ✅ | 段落 |
| `<button>` | ✅ | 按钮（支持onclick） |
| `<a>` | ✅ | 链接（支持href和onclick） |
| `<div>` | ✅ | 容器 |
| `<hr>` | ✅ | 水平分隔线 |
| `<br>` | ✅ | 换行 |
| `<text>` | ✅ | 文本内容（自动生成） |

## 示例项目

查看 `examples` 包中的完整示例：

### 1. 简单欢迎页面
```java
WebbifyExamples.showSimpleWelcome();
```

### 2. 多页面应用
```java
WebbifyExamples.showMultiPageApp();
```

### 3. 交互式菜单
```java
WebbifyExamples.showInteractiveMenu();
```

### 4. 游戏菜单
```java
WebbifyExamples.showGameMenu();
```

### 5. 仪表板
```java
WebbifyExamples.showDashboard();
```

### 6. 帮助页面
```java
WebbifyExamples.showHelpPage();
```

## 最佳实践

### 1. 使用简单HTML
```java
// ✅ 推荐使用简单HTML
String html = "<h1>Title</h1><p>Content</p>";

// ❌ 避免复杂的CSS和JavaScript
String complex = "<style>...</style><script>...</script>";
```

### 2. 利用主题系统
```java
// Webbify会自动继承CatFrame主题
// 无需手动设置颜色，主题会自动应用
```

### 3. 事件处理
```java
// 使用onclick属性处理按钮点击
String html = "<button onclick='alert(\"Clicked!\")'>Click Me</button>";

// 使用href属性进行页面导航
String link = "<a href='about'>About Page</a>";
```

### 4. 布局设计
```java
// 使用div和基础元素进行简单布局
String layout = "<div>" +
    "<h2>Section</h2>" +
    "<p>Content here</p>" +
    "<hr>" +
    "</div>";
```

## 与WebLib的差异

| 特性 | Webbify | WebLib |
|------|---------|--------|
| 目标版本 | Minecraft 1.7.10 | Minecraft 1.21+ |
| 主题集成 | CatFrame主题系统 | 独立样式系统 |
| CSS支持 | 基础样式属性 | 完整CSS引擎 |
| JavaScript | 有限支持 | 完整JS引擎 |
| 复杂度 | 轻量级 | 完整浏览器功能 |
| 依赖 | CatFrameCompact | 独立模组 |

## 常见问题

### Q: 如何添加自定义样式？
```java
// 目前支持内联style属性
String styled = "<div style='color: #ff6b6b; background: rgba(255,255,255,0.1);'>Styled content</div>";
```

### Q: 如何处理表单输入？
```java
// 目前表单元素支持有限，建议使用按钮交互
String html = "<button onclick='handleInput()'>Submit</button>";
```

### Q: 支持图片吗？
```java
// 当前版本不支持<img>标签，建议使用文本替代
String html = "<p>[Image: screenshot.png]</p>";
```

### Q: 如何调试？
```java
// 使用系统控制台输出
String html = "<button onclick=\"alert('Debug info')\">Debug</button>";
```

## 扩展开发

### 自定义页面加载器
```java
WebbifyApi.createNavigationScreen("My App", initialHtml, url -> {
    // 根据URL返回相应的WebPage
    if (url.equals("settings")) {
        return new WebPage(loadSettingsHtml());
    }
    return null; // 取消导航
});
```

### 集成现有功能
```java
// 在WebScreen中集成现有模组功能
WebScreen screen = WebbifyApi.createScreen("My Screen", html);
// 可以在screen中添加自定义逻辑
```

## 更新日志

### v1.0.0
- 初始版本发布
- 基础HTML渲染引擎
- 导航系统支持
- ScreenExtended主题集成
- 示例项目

## 贡献

欢迎提交Issue和Pull Request来改进这个项目！

## 技术架构

### 组件结构
```
webbify/
├── WebScreen.java          # 主屏幕类
├── WebPage.java            # 页面模型
├── WebbifyApi.java         # API接口
└── examples/
    └── WebbifyExamples.java # 示例项目
```

### 设计理念
- **轻量级**: 最小化依赖，专注核心功能
- **集成性**: 与CatFrameCompact主题系统深度集成
- **易用性**: 提供简洁的API和预设模板
- **兼容性**: 适配Minecraft 1.7.10的限制

## 许可证

本项目遵循CatFrameCompact的开源许可证。

## 致谢

- WebLib - 提供了核心设计灵感
- CatFrameCompact - 提供主题系统和UI框架支持
- Minecraft社区 - 提供了宝贵的反馈和需求

---

**注意**: 这是一个轻量级的HTML渲染系统，旨在为Minecraft 1.7.10提供现代化的UI开发体验。如需完整的浏览器功能，请考虑使用其他平台的WebLib版本。
