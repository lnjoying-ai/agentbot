#!/usr/bin/env python3
"""
Read file with Chinese (Unicode) path support.
Usage: python read_chinese_file.py <filepath>
"""

import sys
from pathlib import Path


def read_chinese_file(filepath: str) -> str:
    """
    Read a file with Chinese characters in path.
    
    Args:
        filepath: Path to file (may contain Chinese characters)
    
    Returns:
        File content as string
    
    Raises:
        FileNotFoundError: If file doesn't exist
        UnicodeDecodeError: If file encoding issues
    """
    path = Path(filepath)
    
    if not path.exists():
        raise FileNotFoundError(f"File not found: {filepath}")
    
    if not path.is_file():
        raise ValueError(f"Path is not a file: {filepath}")
    
    # Explicitly use UTF-8 encoding
    return path.read_text(encoding='utf-8')


def main():
    if len(sys.argv) < 2:
        print("Usage: python read_chinese_file.py <filepath>")
        print("Example: python read_chinese_file.py '文档/报告.txt'")
        sys.exit(1)
    
    filepath = sys.argv[1]
    
    try:
        content = read_chinese_file(filepath)
        print(f"Successfully read: {filepath}")
        print("-" * 40)
        print(content[:500])  # Print first 500 chars
        if len(content) > 500:
            print("...")
    except FileNotFoundError as e:
        print(f"Error: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error reading file: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
