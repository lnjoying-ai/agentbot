package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;
import com.microsoft.playwright.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Browser Control Tool implemented with Playwright.
 * Allows navigation, interaction, and data extraction from web pages.
 */
public class BrowserTool implements ToolWithDefinition {
    private static Playwright playwright;
    private static Browser browser;
    private static BrowserContext context;
    private static Page page;

    private final Path workspaceDir;

    public BrowserTool(Path workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    private synchronized void ensureInitialized() {
        if (playwright == null) {
            playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
            try {
                browser = playwright.chromium().launch(options);
            } catch (Exception e) {
                // 如果启动失败（通常是没安装），则尝试自动安装
                System.out.println("Browser not found or initialization failed. Attempting to install chromium...");
                try {
                    com.microsoft.playwright.CLI.main(new String[]{"install", "chromium"});
                    browser = playwright.chromium().launch(options);
                } catch (Exception installError) {
                    throw new RuntimeException("Failed to auto-install Playwright browsers. Please run 'mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install chromium\"' manually.", installError);
                }
            }
            context = browser.newContext();
            page = context.newPage();
        }
    }

    @Override
    public String name() {
        return "browser_control";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "browser_control",
            "Control a Chrome browser to navigate, click, type, screenshot, and extract content. Maintains session between calls.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "action", Map.of(
                        "type", "string",
                        "enum", List.of("goto", "click", "type", "screenshot", "content", "upload"),
                        "description", "The action to perform"
                    ),
                    "url", Map.of("type", "string", "description", "The URL to navigate to (for 'goto')"),
                    "selector", Map.of("type", "string", "description", "The CSS selector for the element (for 'click', 'type', 'upload')"),
                    "text", Map.of("type", "string", "description", "The text to type (for 'type')"),
                    "filePath", Map.of("type", "string", "description", "The local file path to upload (for 'upload')")
                ),
                "required", List.of("action")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        ensureInitialized();
        String action = (String) args.get("action");
        try {
            switch (action) {
                case "goto":
                    String url = (String) args.get("url");
                    if (url == null) return new ToolExecutionResult(false, "URL is required for 'goto'");
                    page.navigate(url);
                    return new ToolExecutionResult(true, "Successfully navigated to " + url + "\nTitle: " + page.title());

                case "click":
                    String clickSelector = (String) args.get("selector");
                    if (clickSelector == null) return new ToolExecutionResult(false, "Selector is required for 'click'");
                    page.click(clickSelector);
                    return new ToolExecutionResult(true, "Successfully clicked element: " + clickSelector);

                case "type":
                    String typeSelector = (String) args.get("selector");
                    String text = (String) args.get("text");
                    if (typeSelector == null || text == null) return new ToolExecutionResult(false, "Selector and text are required for 'type'");
                    page.fill(typeSelector, text);
                    return new ToolExecutionResult(true, "Successfully typed text in: " + typeSelector);

                case "screenshot":
                    String filename = "screenshot-" + UUID.randomUUID() + ".png";
                    Path path = workspaceDir.resolve(filename);
                    page.screenshot(new Page.ScreenshotOptions().setPath(path));
                    return new ToolExecutionResult(true, "Screenshot saved to workspace.\n\n![Screenshot](/workspace/" + filename + ")");

                case "content":
                    // Return a summary of the page content (text-only is often better for LLM)
                    String pageText = page.innerText("body");
                    String pageTitle = page.title();
                    return new ToolExecutionResult(true, "Page Title: " + pageTitle + "\n\nContent:\n" + pageText);

                case "upload":
                    String uploadSelector = (String) args.get("selector");
                    String uploadPath = (String) args.get("filePath");
                    if (uploadSelector == null || uploadPath == null) return new ToolExecutionResult(false, "Selector and filePath are required for 'upload'");
                    page.setInputFiles(uploadSelector, Path.of(uploadPath));
                    return new ToolExecutionResult(true, "Successfully staged file for upload: " + uploadPath);

                default:
                    return new ToolExecutionResult(false, "Unsupported action: " + action);
            }
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Browser error (" + action + "): " + e.getMessage());
        }
    }
}
