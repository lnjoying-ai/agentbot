---
name: chinese-file-lister
description: 查看中文目录路径下的中文文件列表，支持多种编码格式，确保中文文件名和路径不会出现乱码。适用于 Windows/Linux/macOS 系统，处理 GBK/UTF-8 编码问题。当用户需要：(1) 列出包含中文路径的目录内容，(2) 处理中文文件名显示乱码问题，(3) 批量获取文件列表时，使用此技能。
---

# Chinese File Lister

处理中文目录路径和文件名的文件列表工具，确保中文显示无乱码。

## 问题场景

在以下情况下，常规命令（如 `ls`、`dir`）可能出现中文乱码：
- 目录路径包含中文字符
- 文件名包含中文字符
- 系统默认编码与文件编码不一致（如 Windows GBK vs UTF-8）
- 使用 Python、Shell 等工具时子进程编码环境不正确

## 解决方案

### 方法1：使用提供的 Python 脚本（推荐）

```bash
python scripts/list_files.py <目录路径>
```

**示例：**
```bash
# 列出中文目录下的文件
python scripts/list_files.py "D:\工作\文档"

# 列出当前目录（含子目录）
python scripts/list_files.py . --recursive

# 只显示特定类型的文件
python scripts/list_files.py "D:\项目" --ext .txt,.md
```

**特点：**
- ✅ 自动检测系统编码
- ✅ 正确处理 UTF-8 和 GBK 编码
- ✅ 支持递归遍历子目录
- ✅ 输出格式整齐，无乱码
- ✅ 跨平台支持（Windows/Linux/macOS）

### 方法2：命令行快速方案

**Windows（CMD）：**
```cmd
# 使用 dir 命令，设置 UTF-8 编码
chcp 65001
dir /s /b "中文路径"
```

**Windows（PowerShell）：**
```powershell
# 使用 Get-ChildItem，自动处理编码
Get-ChildItem -Path "中文路径" -Recurse | Select-Object FullName
```

**Linux/macOS：**
```bash
# 确保使用 UTF-8 环境
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
find "中文路径" -type f
```

### 方法3：Python 代码片段

```python
from pathlib import Path
import sys

# 配置输出编码
sys.stdout.reconfigure(encoding='utf-8')

# 列出文件
dir_path = Path("中文路径")
for file in dir_path.iterdir():
    print(file.name)
```

## 脚本参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `path` | 目标目录路径（必需） | `"D:\工作\文档"` |
| `--recursive` / `-r` | 递归遍历子目录 | `--recursive` |
| `--ext` | 按扩展名过滤 | `--ext .txt,.pdf` |
| `--output` / `-o` | 输出到文件 | `-o file_list.txt` |
| `--full-path` | 显示完整路径 | `--full-path` |

## 常见错误处理

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 输出为空白 | Python 子进程编码问题 | 使用脚本文件方式执行 |
| 文件名显示为 ??? | 编码不匹配 | 设置 `chcp 65001` 或 `export LANG=UTF-8` |
| UnicodeDecodeError | 文件系统编码问题 | 使用 `pathlib` 替代 `os.path` |

## 最佳实践

1. **优先使用脚本文件**：避免直接在 shell 中运行 Python 代码处理中文路径
2. **使用原始字符串**：Windows 路径使用 `r"路径"` 避免转义问题
3. **设置正确编码**：Windows 使用 `chcp 65001`，Linux/macOS 使用 `export LANG=en_US.UTF-8`
4. **使用 pathlib**：Python 3 的 `pathlib` 库对 Unicode 支持更好

## 输出示例

```
📁 目录: D:\work\秒如\战略规划\2026

📄 文件列表:
   1. 跟踪事项清单.md
   2. 快速参考表.xlsx
   3. 会议纪要_2026-01-15.docx
   4. 项目计划书.pdf

📁 子目录:
   📂 01月工作计划
   📂 02月工作计划
   📂 参考资料

共找到 4 个文件，3 个子目录
```

## 跨平台注意事项

| 平台 | 默认编码 | 推荐设置 |
|------|----------|----------|
| Windows 10/11 | GBK (936) | UTF-8 (65001) |
| Linux | UTF-8 | 保持默认 |
| macOS | UTF-8 | 保持默认 |
| WSL | UTF-8 | 保持默认 |

---

**提示**：如果遇到乱码问题，首先检查控制台编码设置，然后使用本技能提供的脚本工具。
