#!/usr/bin/env python3
"""检查 Skill 结构是否完整"""

import os
from pathlib import Path

def check_skill(skill_path: str):
    skill_dir = Path(skill_path)
    
    print(f"🔍 检查 Skill: {skill_path}\n")
    
    # 必需文件
    required_files = [
        "SKILL.md",
    ]
    
    # 检查必需文件
    all_good = True
    for file in required_files:
        file_path = skill_dir / file
        if file_path.exists():
            size = file_path.stat().st_size
            print(f"  ✅ {file} ({size} bytes)")
        else:
            print(f"  ❌ {file} (缺失)")
            all_good = False
    
    # 列出所有文件
    print(f"\n📁 完整结构:")
    for root, dirs, files in os.walk(skill_dir):
        level = root.replace(str(skill_dir), '').count(os.sep)
        indent = ' ' * 2 * level
        print(f'{indent}{Path(root).name}/')
        subindent = ' ' * 2 * (level + 1)
        for file in files:
            filepath = Path(root) / file
            size = filepath.stat().st_size
            print(f'{subindent}{file} ({size} bytes)')
    
    print(f"\n{'✅ Skill 结构完整！' if all_good else '❌ 请修复上述问题'}")
    return all_good

if __name__ == "__main__":
    check_skill("workspace/skills/emoji-generator")
