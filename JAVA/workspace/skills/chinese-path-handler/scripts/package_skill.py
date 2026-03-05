#!/usr/bin/env python3
"""
Package the chinese-path-handler skill into a .skill file
Usage: python scripts/package_skill.py
"""

import os
import zipfile
import sys
from pathlib import Path

def package_skill(skill_dir: str = None, output_dir: str = None):
    """Package skill into .skill file (zip format)"""
    
    # Default paths
    if skill_dir is None:
        skill_dir = Path(__file__).parent.parent
    else:
        skill_dir = Path(skill_dir)
    
    if output_dir is None:
        output_dir = skill_dir.parent
    else:
        output_dir = Path(output_dir)
    
    skill_name = skill_dir.name
    output_file = output_dir / f"{skill_name}.skill"
    
    # Validate skill structure
    required_files = ['SKILL.md']
    for req_file in required_files:
        if not (skill_dir / req_file).exists():
            print(f"[ERROR] Required file '{req_file}' not found")
            return False
    
    # Read SKILL.md to validate frontmatter
    skill_md = skill_dir / 'SKILL.md'
    content = skill_md.read_text(encoding='utf-8')
    
    if not content.startswith('---'):
        print("[ERROR] SKILL.md missing YAML frontmatter")
        return False
    
    # Create zip file
    print(f"Packaging {skill_name}...")
    
    with zipfile.ZipFile(output_file, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(skill_dir):
            # Skip __pycache__ and .pyc files
            dirs[:] = [d for d in dirs if d != '__pycache__']
            files = [f for f in files if not f.endswith('.pyc')]
            
            for file in files:
                file_path = Path(root) / file
                arc_name = file_path.relative_to(skill_dir)
                zf.write(file_path, arc_name)
                print(f"  + Added: {arc_name}")
    
    print(f"\n[SUCCESS] Created: {output_file}")
    print(f"Size: {output_file.stat().st_size / 1024:.1f} KB")
    return True

if __name__ == '__main__':
    if len(sys.argv) > 1:
        package_skill(sys.argv[1])
    else:
        package_skill()
