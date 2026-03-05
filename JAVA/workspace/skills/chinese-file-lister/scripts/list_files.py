#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
中文路径文件列表工具
解决中文目录和文件名乱码问题
"""

import os
import sys
import argparse
from pathlib import Path


def list_files_recursive(directory, show_hidden=False):
    """
    递归列出目录中的所有文件和文件夹
    
    Args:
        directory: 目标目录路径
        show_hidden: 是否显示隐藏文件
    """
    directory = Path(directory)
    
    if not directory.exists():
        print(f"错误: 路径不存在 - {directory}", file=sys.stderr)
        sys.exit(1)
    
    if not directory.is_dir():
        print(f"错误: 不是目录 - {directory}", file=sys.stderr)
        sys.exit(1)
    
    # 使用 os.walk 遍历目录
    for root, dirs, files in os.walk(directory):
        root_path = Path(root)
        
        # 计算当前目录的相对深度
        try:
            rel_path = root_path.relative_to(directory)
            depth = len(rel_path.parts) if str(rel_path) != '.' else 0
        except ValueError:
            depth = 0
        
        # 打印当前目录
        indent = "  " * depth
        dir_name = root_path.name or directory.name
        print(f"{indent}📁 {dir_name}/")
        
        # 过滤隐藏文件夹
        if not show_hidden:
            dirs[:] = [d for d in dirs if not d.startswith('.')]
        
        # 打印文件
        file_indent = "  " * (depth + 1)
        for file in sorted(files):
            if not show_hidden and file.startswith('.'):
                continue
            print(f"{file_indent}📄 {file}")


def list_files_flat(directory, show_hidden=False):
    """
    平铺列出目录中的所有文件（仅当前目录）
    
    Args:
        directory: 目标目录路径
        show_hidden: 是否显示隐藏文件
    """
    directory = Path(directory)
    
    if not directory.exists():
        print(f"错误: 路径不存在 - {directory}", file=sys.stderr)
        sys.exit(1)
    
    if not directory.is_dir():
        print(f"错误: 不是目录 - {directory}", file=sys.stderr)
        sys.exit(1)
    
    # 列出所有项目
    items = sorted(directory.iterdir(), key=lambda x: (not x.is_dir(), x.name.lower()))
    
    for item in items:
        if not show_hidden and item.name.startswith('.'):
            continue
        
        if item.is_dir():
            print(f"📁 {item.name}/")
        else:
            print(f"📄 {item.name}")


def main():
    parser = argparse.ArgumentParser(
        description='列出中文路径下的文件列表（支持Unicode，无乱码）',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
使用示例:
  python list_files.py "D:\\工作\\文档"          # 递归列出
  python list_files.py "D:\\工作\\文档" --flat  # 仅列出当前目录
  python list_files.py . --hidden               # 包含隐藏文件
        '''
    )
    
    parser.add_argument('path', help='目标目录路径（支持中文）')
    parser.add_argument('-f', '--flat', action='store_true', 
                        help='仅列出当前目录，不递归')
    parser.add_argument('-H', '--hidden', action='store_true',
                        help='显示隐藏文件（以.开头的文件/文件夹）')
    parser.add_argument('--encoding', default='utf-8',
                        help='输出编码（默认: utf-8）')
    
    args = parser.parse_args()
    
    # 设置输出编码
    if sys.platform == 'win32':
        import io
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding=args.encoding)
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding=args.encoding)
    
    # 执行列表操作
    if args.flat:
        list_files_flat(args.path, args.hidden)
    else:
        list_files_recursive(args.path, args.hidden)


if __name__ == '__main__':
    main()
