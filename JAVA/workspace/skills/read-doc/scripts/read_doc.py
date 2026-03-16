#!/usr/bin/env python3
"""
读取旧版 Word 文档 (.doc) 的 Python 工具
支持转换为 docx、txt、md 格式
"""

import os
import sys
import subprocess
import tempfile
import shutil
from pathlib import Path
from typing import Optional, List, Dict


class DocReader:
    """读取 .doc 文件的类"""
    
    def __init__(self, libreoffice_path: Optional[str] = None):
        """
        初始化 DocReader
        
        Args:
            libreoffice_path: LibreOffice 可执行文件路径（可选，自动检测）
        """
        self.libreoffice = libreoffice_path or self._find_libreoffice()
        self.temp_dir = None
        
    def _find_libreoffice(self) -> str:
        """自动查找 LibreOffice 可执行文件"""
        # 尝试不同平台的常见路径
        candidates = [
            # Windows
            "soffice",
            "soffice.exe",
            r"C:\Program Files\LibreOffice\program\soffice.exe",
            r"C:\Program Files (x86)\LibreOffice\program\soffice.exe",
            # Linux
            "libreoffice",
            "/usr/bin/libreoffice",
            "/usr/bin/soffice",
            # macOS
            "/Applications/LibreOffice.app/Contents/MacOS/soffice",
        ]
        
        for candidate in candidates:
            if shutil.which(candidate):
                return candidate
                
        raise RuntimeError(
            "未找到 LibreOffice。请安装 LibreOffice 或指定路径。\n"
            "下载地址: https://www.libreoffice.org/download/"
        )
    
    def read_to_text(self, doc_path: str, encoding: str = 'utf-8') -> str:
        """
        读取 .doc 文件内容为纯文本
        
        Args:
            doc_path: .doc 文件路径
            encoding: 输出文本编码
            
        Returns:
            文档的纯文本内容
        """
        doc_path = Path(doc_path)
        if not doc_path.exists():
            raise FileNotFoundError(f"文件不存在: {doc_path}")
        
        # 创建临时目录
        with tempfile.TemporaryDirectory() as temp_dir:
            # 转换为 txt
            txt_path = self._convert(doc_path, temp_dir, 'txt')
            
            # 读取内容
            with open(txt_path, 'r', encoding=encoding, errors='replace') as f:
                return f.read()
    
    def convert_to_docx(self, doc_path: str, output_path: Optional[str] = None) -> str:
        """
        将 .doc 转换为 .docx 格式
        
        Args:
            doc_path: 输入 .doc 文件路径
            output_path: 输出 .docx 文件路径（可选）
            
        Returns:
            输出文件的路径
        """
        doc_path = Path(doc_path)
        if not doc_path.exists():
            raise FileNotFoundError(f"文件不存在: {doc_path}")
        
        if output_path is None:
            output_path = doc_path.with_suffix('.docx')
        else:
            output_path = Path(output_path)
        
        # 创建临时目录
        with tempfile.TemporaryDirectory() as temp_dir:
            # 转换
            converted = self._convert(doc_path, temp_dir, 'docx')
            
            # 移动到目标位置
            shutil.move(converted, output_path)
            
        return str(output_path)
    
    def convert_to_txt(self, doc_path: str, output_path: Optional[str] = None) -> str:
        """
        将 .doc 转换为 .txt 格式
        
        Args:
            doc_path: 输入 .doc 文件路径
            output_path: 输出 .txt 文件路径（可选）
            
        Returns:
            输出文件的路径
        """
        doc_path = Path(doc_path)
        if not doc_path.exists():
            raise FileNotFoundError(f"文件不存在: {doc_path}")
        
        if output_path is None:
            output_path = doc_path.with_suffix('.txt')
        else:
            output_path = Path(output_path)
        
        # 创建临时目录
        with tempfile.TemporaryDirectory() as temp_dir:
            # 转换
            converted = self._convert(doc_path, temp_dir, 'txt')
            
            # 移动到目标位置
            shutil.move(converted, output_path)
            
        return str(output_path)
    
    def _convert(self, doc_path: Path, output_dir: str, format: str) -> str:
        """
        使用 LibreOffice 执行转换
        
        Args:
            doc_path: 输入文件路径
            output_dir: 输出目录
            format: 目标格式 (docx, txt, md)
            
        Returns:
            转换后的文件路径
        """
        cmd = [
            self.libreoffice,
            '--headless',
            '--convert-to', format,
            '--outdir', output_dir,
            str(doc_path)
        ]
        
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60
            )
            
            if result.returncode != 0:
                raise RuntimeError(f"转换失败: {result.stderr}")
            
            # 查找生成的文件
            output_files = list(Path(output_dir).glob(f'*.{format}'))
            if not output_files:
                raise RuntimeError(f"未找到转换后的文件")
            
            return str(output_files[0])
            
        except subprocess.TimeoutExpired:
            raise RuntimeError("转换超时（60秒），文件可能过大或损坏")
        except Exception as e:
            raise RuntimeError(f"转换出错: {e}")
    
    def batch_convert(self, doc_paths: List[str], output_dir: str, format: str = 'docx') -> Dict[str, str]:
        """
        批量转换多个 .doc 文件
        
        Args:
            doc_paths: .doc 文件路径列表
            output_dir: 输出目录
            format: 目标格式
            
        Returns:
            原始路径到输出路径的映射字典
        """
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        
        results = {}
        for doc_path in doc_paths:
            try:
                doc_path = Path(doc_path)
                output_path = output_dir / f"{doc_path.stem}.{format}"
                
                if format == 'docx':
                    self.convert_to_docx(str(doc_path), str(output_path))
                elif format == 'txt':
                    self.convert_to_txt(str(doc_path), str(output_path))
                else:
                    with tempfile.TemporaryDirectory() as temp_dir:
                        converted = self._convert(doc_path, temp_dir, format)
                        shutil.move(converted, output_path)
                
                results[str(doc_path)] = str(output_path)
                print(f"✅ {doc_path.name} -> {output_path.name}")
                
            except Exception as e:
                print(f"❌ {doc_path.name} 转换失败: {e}")
                results[str(doc_path)] = None
        
        return results


def main():
    """命令行入口"""
    import argparse
    
    parser = argparse.ArgumentParser(description='读取 .doc 文件')
    parser.add_argument('input', help='输入 .doc 文件路径')
    parser.add_argument('--format', choices=['docx', 'txt', 'md'], default='txt',
                        help='输出格式（默认: txt）')
    parser.add_argument('--output', '-o', help='输出文件路径')
    parser.add_argument('--libreoffice', help='LibreOffice 可执行文件路径')
    
    args = parser.parse_args()
    
    reader = DocReader(args.libreoffice)
    
    try:
        if args.format == 'txt':
            text = reader.read_to_text(args.input)
            if args.output:
                with open(args.output, 'w', encoding='utf-8') as f:
                    f.write(text)
                print(f"✅ 已保存到: {args.output}")
            else:
                print(text)
                
        elif args.format == 'docx':
            output = reader.convert_to_docx(args.input, args.output)
            print(f"✅ 已转换为: {output}")
            
        elif args.format == 'md':
            # Markdown 也先转为 txt，但保留 .md 扩展名
            with tempfile.TemporaryDirectory() as temp_dir:
                converted = reader._convert(Path(args.input), temp_dir, 'txt')
                output_path = args.output or Path(args.input).with_suffix('.md')
                shutil.copy(converted, output_path)
                print(f"✅ 已保存为 Markdown: {output_path}")
                
    except Exception as e:
        print(f"❌ 错误: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
