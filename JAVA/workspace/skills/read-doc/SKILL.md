---
name: read-doc
description: 读取和提取旧版 Word 文档（.doc 格式，二进制格式）的内容。支持将 .doc 转换为 .docx、纯文本或 Markdown 格式。适用于处理 97-2003 版本的 Word 文档。当用户需要：(1) 读取 .doc 文件内容，(2) 将 .doc 转换为现代格式，(3) 批量处理多个 .doc 文件 时，使用此技能。
---

# 读取旧版 Word 文档 (.doc)

## 概述

本技能用于读取和提取 Microsoft Word 97-2003 格式（.doc，二进制格式）的文档内容。由于 .doc 是专有二进制格式，需要特殊处理才能读取。

## 解决方案

### 方法1：使用 LibreOffice 转换（推荐，跨平台）

LibreOffice 提供可靠的 .doc 到 .docx/.txt 转换，支持 Windows、Linux 和 macOS。

**转换命令：**

```bash
# 转换为 .docx（保留格式）
soffice --headless --convert-to docx input.doc --outdir output_dir

# 转换为纯文本（仅保留文字内容）
soffice --headless --convert-to txt input.doc --outdir output_dir

# 转换为 Markdown（保留基本格式）
soffice --headless --convert-to md input.doc --outdir output_dir
```

**Python 脚本使用：**

```python
# 使用 skill 提供的脚本
python scripts/read_doc.py input.doc --format docx --output output.docx
python scripts/read_doc.py input.doc --format txt --output output.txt
```

### 方法2：使用 antiword（Linux/macOS）

antiword 是一个轻量级的 .doc 转文本工具，适合快速提取文字内容。

```bash
# 安装
sudo apt-get install antiword  # Debian/Ubuntu
brew install antiword          # macOS

# 使用
antiword input.doc > output.txt
```

### 方法3：使用 textract（Python 库）

textract 支持多种文档格式的文本提取。

```bash
pip install textract
```

```python
import textract
text = textract.process("input.doc").decode('utf-8')
print(text)
```

**注意：** textract 依赖 antiword 或 LibreOffice，需要先安装这些底层工具。

## 使用脚本

本技能提供 `scripts/read_doc.py` 脚本，封装了转换和读取流程：

```python
from scripts.read_doc import DocReader

# 读取 .doc 文件内容为文本
reader = DocReader()
text = reader.read_to_text("document.doc")

# 转换为 .docx
reader.convert_to_docx("document.doc", "output.docx")

# 批量处理
docs = reader.batch_convert(["doc1.doc", "doc2.doc"], "output_dir", format="txt")
```

### 脚本参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `input` | 输入 .doc 文件路径 | `document.doc` |
| `--format` | 输出格式：`docx`/`txt`/`md` | `--format txt` |
| `--output` | 输出文件路径 | `--output output.txt` |
| `--keep-temp` | 保留临时转换文件 | `--keep-temp` |

## 中文路径处理

处理包含中文路径的 .doc 文件时，使用脚本方式避免编码问题：

```python
# 正确：使用脚本文件
python scripts/read_doc.py "D:\工作\文档.doc" --format txt

# 错误：直接 shell 命令可能遇到编码问题
soffice --convert-to txt "中文路径.doc"  # 某些环境可能失败
```

## 最佳实践

1. **优先转换为 .docx**：保留格式信息，便于后续用 python-docx 处理
2. **仅需文字时使用 txt**：提取速度快，文件体积小
3. **批量处理**：使用 `batch_convert` 方法处理多个文件
4. **检查 LibreOffice 安装**：
   - Windows: `soffice --version`
   - Linux: `libreoffice --version`

## 依赖安装

### Windows
```powershell
# 安装 LibreOffice（推荐）
winget install LibreOffice.LibreOffice

# 或下载安装包
# https://www.libreoffice.org/download/download/
```

### Linux
```bash
sudo apt-get install libreoffice
# 或
sudo yum install libreoffice
```

### macOS
```bash
brew install --cask libreoffice
```

## 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 转换失败/超时 | 文件损坏或格式异常 | 尝试使用 antiword |
| 中文乱码 | 编码问题 | 使用 txt 格式时指定 UTF-8 |
| 图片丢失 | txt 格式不支持 | 改用 docx 格式 |
| 表格格式错乱 | txt 格式限制 | 改用 docx 格式 |

## 限制说明

- .doc 到 .docx 转换可能无法 100% 还原复杂格式
- 某些宏和高级功能可能丢失
- 密码保护的文档需要先解除保护
