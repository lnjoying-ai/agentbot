# Chinese Path Encoding Guide

Detailed guide on handling encoding issues with Chinese file paths across different platforms and languages.

## Understanding the Problem

### Character Encoding Basics

- **UTF-8**: Universal encoding, supports all Unicode characters including Chinese
- **GBK/GB2312**: Legacy Chinese encoding (Windows default in China)
- **System Default**: Platform-specific encoding that varies by OS and locale

### Platform Differences

| Platform | Default Encoding | Common Issues |
|----------|-----------------|---------------|
| Windows (CN) | GBK (Code Page 936) | UTF-8 files appear garbled |
| Windows (Intl) | UTF-8 (recent) | Legacy apps use ANSI |
| Linux | UTF-8 | Usually works correctly |
| macOS | UTF-8 | Usually works correctly |

## Encoding Detection

### Python

```python
import chardet

def detect_encoding(filepath):
    with open(filepath, 'rb') as f:
        raw_data = f.read()
        result = chardet.detect(raw_data)
        return result['encoding'], result['confidence']

# Usage
encoding, confidence = detect_encoding('中文文件.txt')
print(f"Detected: {encoding} (confidence: {confidence})")
```

### Java

```java
import java.io.*;
import java.nio.charset.*;

public class EncodingDetector {
    public static Charset detectEncoding(byte[] bytes) {
        // Try UTF-8 first
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            // Fall back to system default
            return Charset.defaultCharset();
        }
    }
}
```

## Conversion Between Encodings

### Python

```python
# Convert GBK file to UTF-8
def convert_gbk_to_utf8(input_path, output_path):
    with open(input_path, 'r', encoding='gbk') as f:
        content = f.read()
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(content)

# Batch convert directory
from pathlib import Path

def batch_convert(directory, from_encoding='gbk', to_encoding='utf-8'):
    for file_path in Path(directory).rglob('*.txt'):
        try:
            content = file_path.read_text(encoding=from_encoding)
            file_path.write_text(content, encoding=to_encoding)
            print(f"Converted: {file_path}")
        except Exception as e:
            print(f"Failed: {file_path} - {e}")
```

### Java

```java
import java.nio.file.*;
import java.nio.charset.*;

public class EncodingConverter {
    public static void convertEncoding(Path input, Path output, 
                                       Charset from, Charset to) throws IOException {
        byte[] bytes = Files.readAllBytes(input);
        String content = new String(bytes, from);
        Files.write(output, content.getBytes(to));
    }
    
    public static void main(String[] args) throws IOException {
        convertEncoding(
            Paths.get("gbk-file.txt"),
            Paths.get("utf8-file.txt"),
            Charset.forName("GBK"),
            StandardCharsets.UTF_8
        );
    }
}
```

## Terminal Encoding

### Windows PowerShell

```powershell
# Check current code page
chcp

# Set UTF-8 (temporary)
chcp 65001

# Set UTF-8 permanently
[Environment]::SetEnvironmentVariable("PYTHONIOENCODING", "utf-8", "User")

# Force UTF-8 in PowerShell profile
$OutputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
```

### Bash/Linux

```bash
# Check current locale
locale

# Set UTF-8 locale
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# Add to ~/.bashrc for persistence
echo 'export LANG=en_US.UTF-8' >> ~/.bashrc
echo 'export LC_ALL=en_US.UTF-8' >> ~/.bashrc
```

## Common Error Messages and Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| `UnicodeDecodeError: 'gbk' codec can't decode` | Reading UTF-8 as GBK | Explicitly use `encoding='utf-8'` |
| `UnicodeEncodeError: 'ascii' codec can't encode` | Output encoding mismatch | Set PYTHONIOENCODING=utf-8 |
| Garbled output in terminal | Terminal encoding | Set terminal to UTF-8 |
| `FileNotFoundError` | Path encoding mismatch | Use `pathlib` or encode properly |
| MalformedInputException | Java encoding issue | Use `CharsetDecoder` with error handling |

## Best Practices

1. **Always specify encoding explicitly**: Never rely on system defaults
2. **Use UTF-8 consistently**: Standardize on UTF-8 for all files
3. **Normalize paths**: Use `Path.normalize()` or equivalent
4. **Test with challenging filenames**: 
   - Mixed Chinese/English: `报告v2.0final.txt`
   - Special characters: `文件(2024)备份.txt`
   - Spaces: `我的 文档.txt`
   - Unicode symbols: `文档📄.txt`
