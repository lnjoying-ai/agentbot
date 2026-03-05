#!/usr/bin/env python3
"""
修复中文路径问题的实用脚本
适用于：dir/shell 命令显示乱码的情况
注意：Windows控制台可能不支持emoji，使用纯文本符号
"""
import os
import sys
from pathlib import Path

def list_files_safe(directory="."):
    """安全列出目录内容，正确处理中文文件名"""
    try:
        path = Path(directory)
        print(f"[DIR] {path.resolve()}")
        print("-" * 50)
        
        for item in path.iterdir():
            # 使用纯文本符号避免编码问题
            prefix = "[DIR]  " if item.is_dir() else "[FILE] "
            # 正确显示中文文件名
            print(f"{prefix}{item.name}")
            
    except Exception as e:
        print(f"[ERROR] {e}")

def find_files_recursive(directory=".", pattern="*"):
    """递归搜索文件，正确处理中文路径"""
    try:
        path = Path(directory)
        print(f"[SEARCH] 在 '{path.resolve()}' 中搜索 '{pattern}'")
        print("-" * 50)
        
        for item in path.rglob(pattern):
            prefix = "[DIR]  " if item.is_dir() else "[FILE] "
            print(f"{prefix}{item}")
            
    except Exception as e:
        print(f"[ERROR] {e}")

def create_test_structure():
    """创建测试用的中文路径结构"""
    test_dirs = [
        "测试文档/报告",
        "测试文档/数据/2024",
        "测试文档/备份"
    ]
    
    test_files = [
        "测试文档/说明.txt",
        "测试文档/报告/月度总结.txt",
        "测试文档/数据/2024/统计.csv"
    ]
    
    print("[INFO] 创建测试目录结构...")
    
    # 创建目录
    for dir_path in test_dirs:
        Path(dir_path).mkdir(parents=True, exist_ok=True)
        print(f"[CREATE DIR] {dir_path}")
    
    # 创建文件
    for file_path in test_files:
        Path(file_path).write_text("测试内容", encoding='utf-8')
        print(f"[CREATE FILE] {file_path}")
    
    print("[OK] 测试结构创建完成")

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="中文路径处理工具")
    parser.add_argument("command", choices=["list", "find", "init"], 
                       help="list: 列出目录, find: 搜索文件, init: 创建测试结构")
    parser.add_argument("--path", default=".", help="目标路径")
    parser.add_argument("--pattern", default="*", help="搜索模式 (仅用于find)")
    
    args = parser.parse_args()
    
    if args.command == "list":
        list_files_safe(args.path)
    elif args.command == "find":
        find_files_recursive(args.path, args.pattern)
    elif args.command == "init":
        create_test_structure()
