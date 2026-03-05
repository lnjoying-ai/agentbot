# Chinese Path Handling Quick Reference

## The Golden Rule

> **Always create a Python script file, then execute it.**
> 
> Never try to execute Python code directly in shell for files with Chinese paths.

## Why Direct Execution Fails

```
Your Request → Shell (GBK/936) → Python subprocess → ❌ Encoding corruption
```

Windows console uses GBK encoding (code page 936) by default. When Python runs in a subprocess:
1. Chinese characters in the path get corrupted
2. File operations fail silently or return empty results
3. Even `chcp 65001` (UTF-8) often doesn't help in subprocess environments

## The Working Pattern

```
Your Request → Create script file (UTF-8) → Execute script → ✅ Success
```

Script files preserve UTF-8 encoding, and Python reads them correctly.

## Quick Solution Template

```python
# read_excel_temp.py
import pandas as pd
import sys
sys.stdout.reconfigure(encoding='utf-8')

file_path = r'PATH_WITH_CHINESE_HERE'
all_sheets = pd.read_excel(file_path, sheet_name=None)

for sheet_name, df in all_sheets.items():
    print(f"\n=== {sheet_name} ===")
    print(df.head(10).to_string())
```

```bash
# Execute
python read_excel_temp.py

# Cleanup
del read_excel_temp.py
```

## One-Liner Using Pre-built Script

If the skill's scripts are available:

```bash
# Text format output
python workspace/skills/xlsx/scripts/read_chinese_excel.py "D:\工作\报告.xlsx"

# Markdown format output
python workspace/skills/xlsx/scripts/excel_to_markdown.py "D:\工作\报告.xlsx"
```

## Common Error Patterns

### ❌ Don't Do This

```bash
# These will FAIL
python -c "import pandas as pd; print(pd.read_excel('中文路径.xlsx'))"
chcp 65001 && python -c "import pandas as pd; pd.read_excel('中文路径.xlsx')"
powershell -Command "python -c '...中文路径...'"
```

### ✅ Do This Instead

```python
# Create file
with open('temp.py', 'w', encoding='utf-8') as f:
    f.write('''
import pandas as pd
file_path = r'中文路径.xlsx'
df = pd.read_excel(file_path)
print(df)
''')
```

```bash
# Then execute
python temp.py
del temp.py
```

## Path Formatting Tips

### Windows Paths

```python
# ✅ Good - Raw string
file_path = r'D:\工作\文档\报告.xlsx'

# ✅ Good - Forward slashes
file_path = 'D:/工作/文档/报告.xlsx'

# ❌ Bad - Backslashes without raw string (escape sequences)
file_path = 'D:\工作\文档\报告.xlsx'  # \x, \n, etc. become escape sequences

# ❌ Bad - Double backslashes (hard to read)
file_path = 'D:\\工作\\文档\\报告.xlsx'
```

### Cross-Platform Paths

```python
from pathlib import Path, PureWindowsPath

# Convert Windows path to universal format
if sys.platform == 'win32':
    file_path = r'D:\工作\文档\报告.xlsx'
else:
    file_path = '/mnt/d/工作/文档/报告.xlsx'

# Or use pathlib
file_path = Path('D:/工作/文档/报告.xlsx')
```

## Troubleshooting

| Problem | Check | Solution |
|---------|-------|----------|
| Empty output | File exists? | `os.path.exists(file_path)` |
| Empty output | Encoding? | Use script file approach |
| Garbled text | Output encoding? | `sys.stdout.reconfigure(encoding='utf-8')` |
| Permission error | File locked? | Close Excel/other programs |
| Import error | pandas installed? | `pip install pandas openpyxl` |

## Complete Working Example

```python
import pandas as pd
import sys
import os

# 1. Configure encoding
sys.stdout.reconfigure(encoding='utf-8')

# 2. Define path (use raw string)
file_path = r'D:\工作\秒如\报价方案.xlsx'

# 3. Verify file exists
if not os.path.exists(file_path):
    print(f"File not found: {file_path}")
    sys.exit(1)

# 4. Read Excel
try:
    all_sheets = pd.read_excel(file_path, sheet_name=None)
    
    for sheet_name, df in all_sheets.items():
        print(f"\n{'='*50}")
        print(f"Sheet: {sheet_name}")
        print(f"{'='*50}")
        print(f"Shape: {df.shape}")
        print(f"\nFirst 5 rows:")
        print(df.head().to_string())
        
except Exception as e:
    print(f"Error: {e}")
```

## Reference Scripts

This skill includes helper scripts in `scripts/`:

- `read_chinese_excel.py` - Extract to formatted text
- `excel_to_markdown.py` - Convert to Markdown tables

Usage:
```bash
python scripts/read_chinese_excel.py "your_file.xlsx"
python scripts/excel_to_markdown.py "your_file.xlsx" "output.md"
```

## Key Takeaways

1. **Script file > Direct execution** for Chinese paths
2. **Raw strings (`r'...'`)** for Windows paths
3. **UTF-8 encoding** for both file and stdout
4. **Verify file exists** before reading
5. **Clean up** temporary files after use
