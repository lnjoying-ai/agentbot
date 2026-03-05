# Tool Reference Guide

This document provides detailed specifications for all available tools in the agentbot system.

## File System Tools

### read_file
Read the entire contents of a file.

**Parameters:**
- `path` (string, required): Absolute or relative file path

**Returns:** File content as string

**Example:**
```json
{
  "name": "read_file",
  "arguments": {
    "path": "src/main/java/com/example/App.java"
  }
}
```

---

### write_file
Create a new file or overwrite an existing file.

**Parameters:**
- `path` (string, required): Target file path
- `content` (string, required): Content to write

**Returns:** Success message

**Notes:**
- Creates parent directories automatically
- Use with caution—this overwrites existing files completely

**Example:**
```json
{
  "name": "write_file",
  "arguments": {
    "path": "config/settings.json",
    "content": "{\"debug\": true}"
  }
}
```

---

### replace_in_file
Make precise edits by replacing specific text sections.

**Parameters:**
- `path` (string, required): File to edit
- `old_str` (string, required): Exact text to find and replace
- `new_str` (string, required): Replacement text

**Returns:** Success message or error if old_str not found

**Critical Rules:**
- `old_str` must match EXACTLY (including whitespace, indentation)
- If `old_str` appears multiple times, the operation fails
- Read the file first to ensure you have the correct current content

**Example:**
```json
{
  "name": "replace_in_file",
  "arguments": {
    "path": "src/Config.java",
    "old_str": "private int timeout = 30;",
    "new_str": "private int timeout = 60;"
  }
}
```

---

### list_dir
List contents of a directory.

**Parameters:**
- `path` (string, required): Directory path

**Returns:** List of files and subdirectories

**Example:**
```json
{
  "name": "list_dir",
  "arguments": {
    "path": "src/main/java/com/agentbot"
  }
}
```

---

### search_file
Find files matching a pattern.

**Parameters:**
- `pattern` (string, required): File name pattern (supports wildcards)
- `directory` (string, required): Root directory to search
- `recursive` (boolean, optional): Search subdirectories (default: true)

**Returns:** List of matching file paths

**Example:**
```json
{
  "name": "search_file",
  "arguments": {
    "pattern": "*.java",
    "directory": "src/",
    "recursive": true
  }
}
```

---

## Code Search Tools

### grep
Search for text patterns in files.

**Parameters:**
- `pattern` (string, required): Search pattern (supports regex)
- `directory` (string, required): Directory to search
- `file_types` (string, optional): Comma-separated extensions (e.g., ".java,.xml")
- `case_sensitive` (boolean, optional): Case-sensitive search (default: false)

**Returns:** Matching lines with file names and line numbers

**Example:**
```json
{
  "name": "grep",
  "arguments": {
    "pattern": "public class.*Tool",
    "directory": "src/main/java",
    "file_types": ".java"
  }
}
```

---

## Execution Tools (⚠️ Requires Approval)

### shell
Execute a shell command.

**Parameters:**
- `command` (string, required): Command to execute
- `working_dir` (string, optional): Working directory
- `confirmed` (boolean, internal): Set by system after user approval

**Returns:** Command output (stdout + stderr)

**Approval Triggers:**
Commands containing these keywords require user confirmation:
- File operations: `rm`, `del`, `rmdir`, `mv`, `cp -r`
- Permissions: `chmod`, `chown`, `sudo`, `su`
- System management: `systemctl`, `service`, `kill`, `killall`, `pkill`
- Package managers: `npm install`, `pip install`, `apt`, `yum`, `dnf`
- Redirection: `>`, `>>` (output redirection)
- Network: `curl`, `wget`, `nc`, `netcat`

**Safety Notes:**
- Commands timeout after 60 seconds
- Output is truncated at 10,000 characters
- Always explain what the command does before execution

**Example:**
```json
{
  "name": "shell",
  "arguments": {
    "command": "find . -name '*.log' -mtime +7 -delete"
  }
}
```

---

### spawn
Launch a background process.

**Parameters:**
- `command` (string, required): Command to run
- `name` (string, optional): Process identifier
- `working_dir` (string, optional): Working directory
- `confirmed` (boolean, internal): Set by system after user approval

**Returns:** Process ID and status

**Use Cases:**
- Web servers: `java -jar app.jar`
- Development servers: `npm run dev`
- File watchers: `nodemon script.js`

**Important:**
- ALL spawn operations require approval
- Process runs in background until manually stopped
- Use for long-running tasks only (>30 seconds)

**Example:**
```json
{
  "name": "spawn",
  "arguments": {
    "command": "python -m http.server 8080",
    "name": "dev-server"
  }
}
```

---

## Web Tools

### web_search
Search the internet using a search engine.

**Parameters:**
- `query` (string, required): Search query

**Returns:** Top search results with titles, URLs, and snippets

**Example:**
```json
{
  "name": "web_search",
  "arguments": {
    "query": "Spring Boot async configuration"
  }
}
```

---

### web_fetch
Fetch and extract main content from a URL.

**Parameters:**
- `url` (string, required): Web page URL

**Returns:** Extracted text content

**Notes:**
- JavaScript-rendered content may not be available
- Content is truncated at 8,000 characters
- Respects robots.txt

**Example:**
```json
{
  "name": "web_fetch",
  "arguments": {
    "url": "https://docs.spring.io/spring-boot/reference/features/task-execution.html"
  }
}
```

---

## Communication Tools

### message
Send a message to the user (used internally by the system).

**Parameters:**
- `content` (string, required): Message text
- `channel` (string, optional): Target channel
- `chat_id` (string, optional): Target chat ID

**Returns:** Delivery confirmation

**Note:** This is primarily used by the system for routing. Direct calls are rare.

---

## Tool Execution Flow

### Standard Flow (Safe Tools)
```
User Request → LLM decides tool → Tool executes → Result returned → LLM continues
```

### Approval Flow (Risky Tools)
```
User Request → LLM decides tool → System detects risk
             ↓
System creates pending action → Frontend shows confirmation card
             ↓
User clicks "确认执行" → System executes with confirmed=true
             ↓
Result returned → LLM continues
```

### Approval Policy Config
You can control tool approval behavior via `agentbot.approvals.tools` in `config/agentbot.yml`:
- `security`: `deny` | `allowlist` | `full`
- `ask`: `off` | `on-miss` | `always`
- `askFallback`: `deny` | `allow` (used when UI is unavailable)
- `uiChannels`: channels that can display approval UI (default: `web`)
- `allowlist`: tool + arg pattern rules that are auto-allowed

Example:
```
approvals:
  tools:
    security: "allowlist"
    ask: "on-miss"
    askFallback: "deny"
    uiChannels:
      - "web"
    allowlist:
      - tool: "shell"
        match:
          command: '/^(ls|pwd|dir)(\\s|$).*/'
```

---


## Error Handling

### Common Errors

**FileNotFoundError:**
- Cause: File path doesn't exist
- Solution: Use `list_dir` to verify path, check for typos

**PermissionDenied:**
- Cause: Insufficient permissions to read/write file
- Solution: Check file ownership, use appropriate working directory

**TimeoutError (shell/spawn):**
- Cause: Command took longer than 60 seconds
- Solution: Break into smaller operations or use spawn for long tasks

**DuplicateMatchError (replace_in_file):**
- Cause: `old_str` appears multiple times in file
- Solution: Make `old_str` more specific with surrounding context

---

## Best Practices

### 1. Read Before Write
```
❌ Wrong:
replace_in_file(path="config.json", old_str="port: 8080", new_str="port: 9000")

✅ Correct:
read_file(path="config.json") → inspect content
replace_in_file with exact match including surrounding context
```

### 2. Verify After Edit
```
replace_in_file(...) → success message
read_file(...) → verify changes applied correctly
```

### 3. Use Appropriate Tools
```
❌ Don't use shell for file operations:
shell(command="cat file.txt")

✅ Use dedicated tools:
read_file(path="file.txt")
```

### 4. Batch Related Operations
```
Instead of:
- read_file("a.txt")
- read_file("b.txt")
- read_file("c.txt")

Do:
- list_dir to get all files
- Process in one tool call if possible
```

---

## Browser Anti-Bot Config
Use `agentbot.browser.antiBot` to enable anti-bot strategies for `browser_control`:
- `level`: `basic` | `enhanced` | `advanced`
  - `basic`: only User-Agent + headers
  - `enhanced`: stealth script + resource blocking
  - `advanced`: proxy pool + behavior simulation + detection
- `userAgent`, `headers`, `locale`, `timezoneId`: browser fingerprint controls
- `blockResourceTypes`, `blockUrlPatterns`: resource blocking allowlist
- `proxies`: proxy pool list (e.g. `http://user:pass@host:port`)

Example:
```
agentbot:
  browser:
    antiBot:
      level: "enhanced"
      userAgent: ""
      headers:
        Accept-Language: "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7"
      blockResourceTypes: ["image", "font", "media"]
      blockUrlPatterns: ["*://*/*analytics*", "*://*/*gtag*"]
```

---

## Extending Tools


To add custom tools to the system:

1. Create a Java class implementing the `Tool` interface
2. Register in `ToolRegistry`
3. Implement `requiresApproval()` logic if needed
4. Update this documentation

**Example Tool Structure:**
```java
public class MyTool implements Tool {
    @Override
    public String name() { return "my_tool"; }
    
    @Override
    public String description() { return "Tool description"; }
    
    @Override
    public Map<String, Object> parameters() { /* ... */ }
    
    @Override
    public ToolExecutionResult execute(Map<String, Object> args) { /* ... */ }
}
```

---

**For more information, see:**
- `AGENTS.md` - Agent behavior guidelines
- `src/main/java/com/agentbot/core/tools/` - Tool implementations
- `ToolRegistry.java` - Tool registration and approval logic
