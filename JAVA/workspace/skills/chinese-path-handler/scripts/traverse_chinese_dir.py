#!/usr/bin/env python3
"""
Traverse directory with Chinese (Unicode) path support.
Usage: python traverse_chinese_dir.py <directory>
"""

import sys
from pathlib import Path
from typing import List, Tuple


def traverse_chinese_dir(directory: str, pattern: str = "*") -> List[Tuple[str, str]]:
    """
    Traverse directory containing Chinese filenames.
    
    Args:
        directory: Root directory path
        pattern: Glob pattern for filtering (default: "*")
    
    Returns:
        List of tuples (file_path, file_name)
    """
    root = Path(directory)
    
    if not root.exists():
        raise FileNotFoundError(f"Directory not found: {directory}")
    
    if not root.is_dir():
        raise ValueError(f"Path is not a directory: {directory}")
    
    results = []
    
    # rglob for recursive traversal
    for item in root.rglob(pattern):
        if item.is_file():
            results.append((str(item), item.name))
    
    return results


def print_tree(directory: str, prefix: str = ""):
    """
    Print directory tree with Chinese path support.
    
    Args:
        directory: Root directory path
        prefix: Prefix for tree visualization
    """
    root = Path(directory)
    
    if not root.exists():
        print(f"Directory not found: {directory}")
        return
    
    entries = sorted(root.iterdir(), key=lambda x: (not x.is_dir(), x.name))
    
    for i, entry in enumerate(entries):
        is_last = i == len(entries) - 1
        connector = "└── " if is_last else "├── "
        print(f"{prefix}{connector}{entry.name}")
        
        if entry.is_dir():
            extension = "    " if is_last else "│   "
            print_tree(entry, prefix + extension)


def main():
    if len(sys.argv) < 2:
        print("Usage: python traverse_chinese_dir.py <directory> [pattern]")
        print("Example: python traverse_chinese_dir.py '文档' '*.txt'")
        sys.exit(1)
    
    directory = sys.argv[1]
    pattern = sys.argv[2] if len(sys.argv) > 2 else "*"
    
    try:
        print(f"\nDirectory tree of: {directory}\n")
        print_tree(directory)
        
        print(f"\nFiles matching '{pattern}':")
        files = traverse_chinese_dir(directory, pattern)
        
        if not files:
            print("  (No files found)")
        else:
            for file_path, file_name in files:
                print(f"  {file_name}")
                print(f"    → {file_path}")
                
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
