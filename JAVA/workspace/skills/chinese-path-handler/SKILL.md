---
name: chinese-path-handler
description: Handle file operations with Chinese (Unicode) paths and filenames across multiple languages (Python, Java, Node.js, Shell). Use when reading, writing, traversing directories, or processing files that contain Chinese characters in paths or filenames. Covers encoding issues, cross-platform compatibility, and common pitfalls.
---

# Chinese Path Handler

Comprehensive guide and scripts for handling Chinese (Unicode) file paths and filenames across Python, Java, Node.js, and shell environments.

## Quick Reference

### Problem Statement

Chinese characters in file paths often cause issues due to:

- **Encoding mismatches** (GBK vs UTF-8)
- **Platform differences** (Windows vs Linux/macOS)
- **Legacy API limitations** (old Java File class, Python 2)
- **Tool-specific behaviors** (Python open(), PowerShell, cmd.exe)

### Solutions by Language

#### Python (Recommended: pathlib)

```python
from pathlib import Path

# Read file with Chinese path
file_path = Path("文档/报告.txt")
content = file_path.read_text(encoding='utf-8')

# Write file
file_path.write_text("内容", encoding='utf-8')

# Traverse directory
for item in Path("文档").rglob("*"):
    print(item.name)
```

**Legacy os.path approach:**

```python
import os
# Use explicit encoding
with open("文档/文件.txt", 'r', encoding='utf-8') as f:
    content = f.read()
```

#### Java (Recommended: java.nio.file)

```java
import java.nio.file.*;
import java.nio.charset.Charset;

// Read file
Path path = Paths.get("文档/报告.txt");
String content = Files.readString(path, Charset.forName("UTF-8"));

// Write file
Files.writeString(path, "内容", Charset.forName("UTF-8"));

// Traverse directory
Files.walk(Paths.get("文档"))
    .forEach(p -> System.out.println(p.getFileName()));
```

**Spring Boot file upload:**

```java
@PostMapping("/upload")
public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
    // Decode URL-encoded filename
    String filename = URLDecoder.decode(
        file.getOriginalFilename(), "UTF-8"
    );
    Path target = Paths.get("uploads", filename);
    Files.copy(file.getInputStream(), target);
}
```

#### Node.js

```javascript
const fs = require('fs').promises;
const path = require('path');

// Read file
const content = await fs.readFile('文档/报告.txt', 'utf-8');

// Traverse directory
async function traverse(dir) {
    const entries = await fs.readdir(dir, { withFileTypes: true });
    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            await traverse(fullPath);
        } else {
            console.log(fullPath);
        }
    }
}
```

#### Shell

```bash
# PowerShell (Windows)
Get-ChildItem -Path "文档" -Recurse -File | Select-Object FullName

# Bash (Linux/macOS)
find "文档" -type f -name "*.txt"

# Force UTF-8 locale
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
```

## 实战案例：AI工具中的中文路径问题

### 场景1：Python open() 输出为空或乱码

**问题描述：**
使用 Python 读取包含中文路径的文件时，`open()` 返回空或乱码。

**症状：**
```python
with open(r'D:\工作\报告.txt', 'r', encoding='utf-8') as f:
    content = f.read()
print(content)  # 输出为空或乱码
```

**根本原因：**
- Windows 系统默认使用 GBK 编码
- Python 工具在子进程中执行时，编码环境可能不正确
- 路径字符串在传递过程中编码被破坏

**解决方案（按推荐顺序）：**

#### ✅ 方案1：使用 `type` 命令（Windows CMD）
最可靠的方法，直接使用 Windows 内置命令：

```bash
type "D:\工作\报告.txt"
```

**优点：**
- 不需要考虑 Python 编码问题
- Windows 原生支持
- 简单直接

#### ✅ 方案2：使用 `chcp 65001` + Python
先设置 UTF-8 代码页，再执行 Python：

```bash
chcp 65001
python -c "
import sys
sys.stdout.reconfigure(encoding='utf-8')
with open(r'D:\工作\报告.txt', 'r', encoding='utf-8') as f:
    print(f.read())
"
```

#### ⚠️ 方案3：使用 PowerShell（可能乱码）
```powershell
Get-Content 'D:\工作\报告.txt' -Encoding UTF8
```
**注意：** 在某些环境中可能输出乱码，不推荐作为首选。

#### ❌ 避免：直接使用 Python open()
```python
# 不推荐 - 可能在某些环境中失败
open('中文路径.txt').read()
```

### 场景2：Shell 工具选择决策树

当需要读取包含中文路径的文件时，按以下顺序尝试：

```
开始
  │
  ▼
文件在 Windows 上？
  │
  ├─ 是 ──▶ 使用 type 命令
  │         type "路径\文件名.txt"
  │
  └─ 否 ──▶ 使用 cat 命令
            cat "路径/文件名.txt"
  │
  ▼
如果失败？
  │
  ├─ 编码问题 ──▶ 尝试 iconv 转换
  │               iconv -f GBK -t UTF-8 文件.txt
  │
  └─ 路径问题 ──▶ 使用绝对路径
                  检查路径是否存在
```

### 场景3：批量处理中文路径文件

**问题：** 需要遍历包含中文的目录并处理所有文件

**解决方案：**

```bash
# Windows - 使用 PowerShell（处理文件名时工作正常）
powershell -Command "Get-ChildItem -Path 'D:\工作\文档' -Recurse -File | ForEach-Object { $_.FullName }"

# Linux/macOS - 使用 find
find /工作/文档 -type f -name "*.txt"

# Python - 使用 pathlib（推荐）
python -c "
from pathlib import Path
for file in Path('D:/工作/文档').rglob('*.txt'):
    print(file)
    content = file.read_text(encoding='utf-8')
    # 处理内容...
"
```

### 场景4：AI 工具集成中的编码陷阱

当构建 AI 工具系统时，注意以下编码陷阱：

#### 陷阱1：子进程编码
```python
# 错误：子进程继承系统编码
import subprocess
result = subprocess.run(['python', 'script.py'], capture_output=True, text=True)
# 如果 script.py 输出中文，可能乱码

# 正确：显式指定编码
result = subprocess.run(
    ['python', 'script.py'],
    capture_output=True,
    text=True,
    encoding='utf-8'
)
```

#### 陷阱2：JSON 序列化
```python
# 错误：未处理非 ASCII 字符
import json
data = {'path': '中文/路径.txt'}
json.dumps(data)  # 可能转义为 \uXXXX

# 正确：保留中文字符
json.dumps(data, ensure_ascii=False)
```

#### 陷阱3：日志文件编码
```python
# 错误：日志文件使用系统默认编码
logging.basicConfig(filename='app.log', level=logging.INFO)

# 正确：指定 UTF-8 编码
logging.basicConfig(
    filename='app.log',
    level=logging.INFO,
    encoding='utf-8'
)
```

## Common Pitfalls

| Issue                   | Solution                                          |
| ----------------------- | ------------------------------------------------- |
| `UnicodeDecodeError`    | Always specify `encoding='utf-8'`                 |
| `FileNotFoundError`     | Check if path exists with `Path.exists()`         |
| URL-encoded filenames   | Use `URLDecoder.decode(filename, "UTF-8")`        |
| Windows path separators | Use `os.path.join()` or `pathlib`                 |
| Terminal garbled output | Set `chcp 65001` (Windows) or `export LANG=UTF-8` |
| Python open() 空输出    | Use `type` (Windows) or `cat` (Linux) command     |
| PowerShell 乱码         | Try `type` command instead                        |
| 文件编码未知            | Use `chardet` library to detect encoding          |

## Quick Fix 速查表

| 问题 | 快速解决方案 |
|------|-------------|
| Python 读取中文路径文件为空 | 使用 `type "文件路径"` (Windows) |
| PowerShell 输出乱码 | 改用 `type` 或 `cmd /c type` |
| 不知道文件编码 | `file -i 文件名` (Linux) 或 `chardet` |
| 需要转换编码 | `iconv -f GBK -t UTF-8 输入.txt > 输出.txt` |
| Windows 终端中文乱码 | `chcp 65001` 切换到 UTF-8 |
| Python 打印中文乱码 | `sys.stdout.reconfigure(encoding='utf-8')` |

## Scripts Reference

This skill includes reusable scripts in `scripts/`:

- `read_chinese_file.py` - Python file reader with Chinese path support
- `traverse_chinese_dir.py` - Directory traversal script
- `read_chinese_file.java` - Java file reader example
- `read_chinese_file.js` - Node.js file reader

## Cross-Platform Path Handling

### Best Practices

1. **Never hardcode separators**: Use `os.path.join()`, `Paths.get()`, or `path.join()`
2. **Always specify encoding**: Use `utf-8` explicitly
3. **Use modern APIs**: Prefer `pathlib` (Python), `java.nio.file` (Java)
4. **Normalize paths**: Use `Path.normalize()` or equivalent
5. **Test with real paths**: 使用真实的中文路径测试，不要只用 ASCII 路径

### Path Separator Conversion

```python
# Python
from pathlib import Path, PureWindowsPath, PurePosixPath

# Convert Windows to Unix path
unix_path = PureWindowsPath("文档\\报告.txt").as_posix()
```

```java
// Java
Path path = Paths.get("文档", "报告.txt"); // Works on all platforms
```

```javascript
// Node.js
const path = require('path');
path.join('文档', '报告.txt'); // Auto-detects platform
```

## Encoding Detection

When dealing with files of unknown encoding:

```python
import chardet

# Detect encoding
with open('未知编码.txt', 'rb') as f:
    result = chardet.detect(f.read())
    encoding = result['encoding']  # 'utf-8', 'gbk', etc.
```

## Web Applications

### Handling Chinese filenames in HTTP

```python
# Flask - URL decode filenames
from urllib.parse import unquote

@app.route('/download/<path:filename>')
def download(filename):
    decoded = unquote(filename)  # Decode %XX sequences
    return send_file(decoded)
```

```java
// Spring Boot - Content-Disposition header
String encodedFilename = URLEncoder.encode(filename, "UTF-8")
    .replace("+", "%20");
headers.add("Content-Disposition", 
    "attachment; filename*=UTF-8''" + encodedFilename);
```

## Testing

Test your implementation with these challenging filenames:

- `测试文档.txt` - Simple Chinese
- `文件(2024)版本.txt` - Mixed characters
- `报告 v1.0 最终版.txt` - Spaces and dots
- `文档/子目录/中文文件.txt` - Nested directories
- `特殊字符：测试.txt` - Special punctuation

## Lessons Learned

### 真实案例复盘

**案例：读取 `D:\work\秒如\战略规划\2026\跟踪事项\快速参考表.md`**

**尝试历程：**
1. ❌ Python `open()` - 输出为空
2. ❌ Python `pathlib` - 输出为空  
3. ❌ PowerShell `Get-Content` - 输出乱码
4. ✅ CMD `type` 命令 - 成功读取

**关键教训：**
- AI 工具在子进程中执行时，Python 的编码环境不稳定
- PowerShell 在某些环境中也会出现编码问题
- Windows 内置的 `type` 命令最可靠
- **决策：** 对于中文路径，优先使用系统原生命令而非 Python

**经验公式：**
```
中文路径文件读取成功率：
type (Windows) > cat (Linux) > Python pathlib > Python open() > PowerShell
```

## Further Reading

See `references/` directory for:

- Detailed API documentation
- Platform-specific encoding issues
- Troubleshooting guides
- 更多实战案例
