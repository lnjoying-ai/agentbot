#!/usr/bin/env python3
"""
Skill 打包脚本
将 skill 目录打包为 .skill 文件（本质为 zip）
"""

import os
import sys
import zipfile
from pathlib import Path

def validate_skill(skill_path: str) -> bool:
    """验证 skill 结构是否正确"""
    skill_dir = Path(skill_path)
    
    # 检查 SKILL.md 是否存在
    skill_md = skill_dir / "SKILL.md"
    if not skill_md.exists():
        print(f"❌ 错误: 缺少 SKILL.md 文件")
        return False
    
    # 读取并验证 frontmatter
    content = skill_md.read_text(encoding='utf-8')
    if '---' not in content:
        print(f"❌ 错误: SKILL.md 缺少 YAML frontmatter")
        return False
    
    if 'name:' not in content or 'description:' not in content:
        print(f"❌ 错误: SKILL.md frontmatter 缺少 name 或 description 字段")
        return False
    
    print(f"✅ 结构验证通过")
    return True

def package_skill(skill_path: str, output_dir: str = None) -> str:
    """打包 skill 为 .skill 文件"""
    skill_path = Path(skill_path).resolve()
    skill_name = skill_path.name
    
    # 验证
    if not validate_skill(skill_path):
        sys.exit(1)
    
    # 确定输出目录
    if output_dir:
        output_path = Path(output_dir).resolve()
    else:
        output_path = skill_path.parent
    
    output_path.mkdir(parents=True, exist_ok=True)
    
    # 创建 zip 文件
    output_file = output_path / f"{skill_name}.skill"
    
    with zipfile.ZipFile(output_file, 'w', zipfile.ZIP_DEFLATED) as zf:
        for file_path in skill_path.rglob('*'):
            if file_path.is_file():
                # 跳过 __pycache__ 和 .pyc 文件
                if '__pycache__' in str(file_path) or file_path.suffix == '.pyc':
                    continue
                
                arcname = file_path.relative_to(skill_path)
                zf.write(file_path, arcname)
                print(f"  📦 {arcname}")
    
    print(f"\n✅ 打包完成: {output_file}")
    print(f"📊 文件大小: {output_file.stat().st_size / 1024:.1f} KB")
    return str(output_file)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python package_skill.py <skill目录> [输出目录]")
        print("示例: python package_skill.py emoji-generator ./dist")
        sys.exit(1)
    
    skill_path = sys.argv[1]
    output_dir = sys.argv[2] if len(sys.argv) > 2 else None
    
    if not os.path.exists(skill_path):
        print(f"❌ 错误: 目录不存在 {skill_path}")
        sys.exit(1)
    
    package_skill(skill_path, output_dir)
