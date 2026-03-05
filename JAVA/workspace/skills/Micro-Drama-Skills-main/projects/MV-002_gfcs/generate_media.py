#!/usr/bin/env python3
"""
MV媒体生成脚本 - MV-002 XWOW-凡人歌（港风茶山穿越版）
适配 MV 项目结构（segments/SEGxx/）

Phase 1:  角色参考图生成（创作者A 港风形态 + 创作者B 茶山形态）
Phase 1B: 场景四宫格图生成
Phase 1C: 道具三视图生成
Phase 2:  分镜图片生成（9宫格合成 → 切割）
"""
import os
import re
import io
import json
import time
import sys
from pathlib import Path

from google import genai
from google.genai import types
from PIL import Image

# ========== 配置 ==========
PROJECT_DIR = Path(__file__).parent
SEGMENTS_DIR = PROJECT_DIR / "segments"
CHARACTERS_DIR = PROJECT_DIR / "characters"
SCENES_DIR = PROJECT_DIR / "scenes"
PROPS_DIR = PROJECT_DIR / "props"

# ========== API 配置 ==========
def load_api_config():
    """从配置文件和环境变量加载 API Key、Base URL 和模型名"""
    api_key = None
    base_url = None
    image_model = "gemini-2.0-flash-exp"

    config_path = Path(__file__).parent.parent.parent / ".config" / "api_keys.json"
    if config_path.exists():
        with open(config_path) as f:
            config = json.load(f)
        api_key = config.get("gemini_api_key")
        base_url = config.get("gemini_base_url") or config.get("base_url")
        image_model = config.get("gemini_image_model", image_model)

    api_key = os.environ.get("GEMINI_API_KEY", api_key)
    base_url = os.environ.get("GEMINI_BASE_URL", base_url)
    image_model = os.environ.get("GEMINI_IMAGE_MODEL", image_model)

    if not api_key:
        raise RuntimeError("未找到 GEMINI_API_KEY，请配置 api_keys.json 或设置环境变量")

    return api_key, base_url, image_model


api_key, base_url, IMAGE_MODEL = load_api_config()
http_options = types.HttpOptions(base_url=base_url) if base_url else None
client = genai.Client(api_key=api_key, http_options=http_options)
print(f"🔑 API 已配置 | Base URL: {base_url or '默认'} | 模型: {IMAGE_MODEL}")

# ========== 工具函数 ==========
RATE_LIMIT_DELAY = 5


def gemini_generate_image(prompt: str, output_path: str, ref_files: list = None) -> bool:
    """统一图片生成入口：使用 Gemini 多模态模型生成图片。"""
    try:
        contents = []
        if ref_files:
            for rf in ref_files:
                contents.append(rf)
        contents.append(prompt)

        response = client.models.generate_content(
            model=IMAGE_MODEL,
            contents=contents,
            config=types.GenerateContentConfig(
                response_modalities=["IMAGE", "TEXT"],
            ),
        )

        if response.candidates:
            for part in response.candidates[0].content.parts:
                if part.inline_data and part.inline_data.mime_type.startswith("image/"):
                    image = Image.open(io.BytesIO(part.inline_data.data))
                    image.save(output_path)
                    return True

        print(f"  ⚠️ 无图片返回: {Path(output_path).name}")
        return False
    except Exception as e:
        print(f"  ❌ 生成失败 ({Path(output_path).name}): {e}")
        return False


def load_image_as_part(file_path: str, max_side: int = 768):
    """
    将本地图片文件读取为 inline Part 对象（base64 内联，无需 File API 上传）。
    自动压缩大图：长边缩放到 max_side px，JPEG 质量 80。
    """
    try:
        p = Path(file_path)
        if not p.exists():
            print(f"  ⚠️ 文件不存在: {file_path}")
            return None

        img = Image.open(str(p))
        ratio = min(max_side / img.width, max_side / img.height)
        if ratio < 1:
            new_size = (int(img.width * ratio), int(img.height * ratio))
            img = img.resize(new_size, Image.LANCZOS)
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=80)
        data = buf.getvalue()
        return types.Part.from_bytes(data=data, mime_type="image/jpeg")
    except Exception as e:
        print(f"  ⚠️ 读取失败 {file_path}: {e}")
        return None


# ========== Phase 1: 角色参考图（生成） ==========

def parse_character_bible() -> list:
    """解析 character_bible.md，提取角色代号和AI绘图关键词"""
    bible_path = CHARACTERS_DIR / "character_bible.md"
    if not bible_path.exists():
        return []

    with open(bible_path, "r", encoding="utf-8") as f:
        content = f.read()

    characters = []
    # 匹配角色代号和AI关键词
    blocks = re.split(r'### 角色\d+[：:]', content)
    codes = re.findall(r'\*\*角色代号\*\*[：:]\s*(\S+)', content)
    names_raw = re.findall(r'### 角色\d+[：:]\s*(.+)', content)

    for i, block in enumerate(blocks[1:]):
        code = codes[i] if i < len(codes) else f"char_{i+1}"
        name = names_raw[i].strip() if i < len(names_raw) else f"角色{i+1}"

        prompt_match = re.search(r'\*{0,2}AI绘图关键词（英文）\*{0,2}[：:]\s*(.+)', block)
        if not prompt_match:
            continue

        # 跳过不需要参考图的角色（如群演）
        no_ref = re.search(r'不需要|无需|skip', block, re.IGNORECASE)
        if no_ref and "参考图来源" in block:
            ref_line = block[block.index("参考图来源"):]
            if "不需要" in ref_line[:50]:
                print(f"  ⏭️ 角色 [{code}] 不需要参考图，跳过")
                continue

        characters.append({
            "code": code,
            "name": name,
            "ai_prompt": prompt_match.group(1).strip(),
        })

    return characters


def phase1_characters() -> dict:
    """Phase 1: 生成角色参考图"""
    print("\n" + "=" * 60)
    print("🎨 Phase 1: 角色参考图生成")
    print("=" * 60)

    ref_index_path = CHARACTERS_DIR / "ref_index.json"
    ref_index = {}
    if ref_index_path.exists():
        with open(ref_index_path, encoding="utf-8") as f:
            ref_index = json.load(f)

    characters = parse_character_bible()
    print(f"  📋 发现 {len(characters)} 个需要参考图的角色")

    char_uploaded = {}

    for char_info in characters:
        code = char_info["code"]
        output_path = CHARACTERS_DIR / f"{code}_ref.png"

        if output_path.exists():
            print(f"  ⏭️ 已存在，跳过: {output_path.name}")
            part = load_image_as_part(str(output_path))
            if part:
                char_uploaded[code] = [part]
            # 更新索引状态
            if code in ref_index and ref_index[code].get("status") != "ready":
                ref_index[code]["status"] = "ready"
                ref_index[code]["source"] = "generated"
            continue

        prompt = (
            f"Generate a character reference sheet image. "
            f"Character: {char_info['ai_prompt']}. "
            f"Show 2-3 views of the character (front view and three-quarter view). "
            f"Clean background, high quality, consistent appearance across views."
        )

        print(f"  🎨 生成角色: {char_info['name']} ({code})")
        success = gemini_generate_image(prompt, str(output_path))

        if success:
            print(f"  ✅ {output_path.name}")
            part = load_image_as_part(str(output_path))
            if part:
                char_uploaded[code] = [part]
            ref_index[code] = {
                "source": "generated",
                "ref_file": f"characters/{code}_ref.png",
                "status": "ready",
                "reason": f"由 generate_media.py 自动生成角色参考图",
            }
        else:
            print(f"  ❌ 角色生成失败: {code}")

        time.sleep(RATE_LIMIT_DELAY)

    with open(ref_index_path, "w", encoding="utf-8") as f:
        json.dump(ref_index, f, ensure_ascii=False, indent=2)
    print(f"  📋 角色索引已更新: {ref_index_path.name}")

    return char_uploaded


# ========== Phase 1B: 场景四宫格图 ==========

def parse_scene_bible() -> list:
    """解析 scene_bible.md，提取场景ID和AI绘图关键词"""
    bible_path = SCENES_DIR / "scene_bible.md"
    if not bible_path.exists():
        return []

    with open(bible_path, "r", encoding="utf-8") as f:
        content = f.read()

    scenes = []
    blocks = re.split(r'## 场景\d+[：:]', content)
    ids = re.findall(r'\*\*场景ID\*\*[：:]\s*(\S+)', content)
    names = re.findall(r'## 场景\d+[：:]\s*(.+)', content)

    for i, block in enumerate(blocks[1:]):
        scene_id = ids[i] if i < len(ids) else f"scene_{i+1:02d}"
        scene_name = names[i].strip() if i < len(names) else f"场景{i+1}"

        prompt_match = re.search(r'\*{0,2}AI绘图关键词（英文）\*{0,2}[：:]\s*(.+)', block)
        if prompt_match:
            ai_prompt = prompt_match.group(1).strip()
        else:
            continue

        scenes.append({
            "id": scene_id,
            "name": scene_name,
            "ai_prompt": ai_prompt,
        })

    return scenes


def phase1b_scenes() -> dict:
    """Phase 1B: 生成场景四宫格参考图"""
    print("\n" + "=" * 60)
    print("🏙️ Phase 1B: 场景四宫格图生成")
    print("=" * 60)

    scenes = parse_scene_bible()
    print(f"  📋 发现 {len(scenes)} 个场景")

    scene_uploaded = {}
    ref_index_path = SCENES_DIR / "ref_index.json"
    ref_index = {}
    if ref_index_path.exists():
        with open(ref_index_path, encoding="utf-8") as f:
            ref_index = json.load(f)

    for scene in scenes:
        sid = scene["id"]
        output_path = SCENES_DIR / f"{sid}_ref.png"

        if output_path.exists():
            print(f"  ⏭️ 已存在，跳过: {output_path.name}")
            part = load_image_as_part(str(output_path))
            if part:
                scene_uploaded[sid] = part
            continue

        prompt = (
            f"Generate a single reference sheet image showing a scene in 2x2 grid layout "
            f"(4 views: front view, left 45° angle, right 45° angle, overhead/establishing shot). "
            f"Scene description: {scene['ai_prompt']}. "
            f"Each grid cell shows the same location from a different angle. "
            f"Cinematic quality, 16:9 aspect ratio."
        )

        print(f"  🎨 生成场景: {scene['name']} ({sid})")
        success = gemini_generate_image(prompt, str(output_path))

        if success:
            print(f"  ✅ {output_path.name}")
            part = load_image_as_part(str(output_path))
            if part:
                scene_uploaded[sid] = part
            ref_index[sid] = {
                "name": scene["name"],
                "source": "generated",
                "ref_file": f"scenes/{sid}_ref.png",
                "status": "ready",
                "reason": "由 generate_media.py 自动生成四宫格参考图",
            }
        else:
            print(f"  ❌ 场景生成失败: {sid}")

        time.sleep(RATE_LIMIT_DELAY)

    with open(ref_index_path, "w", encoding="utf-8") as f:
        json.dump(ref_index, f, ensure_ascii=False, indent=2)
    print(f"  📋 场景索引已更新: {ref_index_path.name}")

    return scene_uploaded


# ========== Phase 1C: 道具三视图 ==========

def parse_prop_bible() -> list:
    """解析 prop_bible.md，提取道具ID和AI绘图关键词"""
    bible_path = PROPS_DIR / "prop_bible.md"
    if not bible_path.exists():
        return []

    with open(bible_path, "r", encoding="utf-8") as f:
        content = f.read()

    props = []
    blocks = re.split(r'## 道具\d+[：:]', content)
    ids = re.findall(r'\*\*道具ID\*\*[：:]\s*(\S+)', content)
    names = re.findall(r'## 道具\d+[：:]\s*(.+)', content)

    for i, block in enumerate(blocks[1:]):
        prop_id = ids[i] if i < len(ids) else f"prop_{i+1:02d}"
        prop_name = names[i].strip() if i < len(names) else f"道具{i+1}"

        prompt_match = re.search(r'\*{0,2}AI绘图关键词（英文）\*{0,2}[：:]\s*(.+)', block)
        if prompt_match:
            ai_prompt = prompt_match.group(1).strip()
        else:
            continue

        props.append({
            "id": prop_id,
            "name": prop_name,
            "ai_prompt": ai_prompt,
        })

    return props


def phase1c_props() -> dict:
    """Phase 1C: 生成道具三视图参考图"""
    print("\n" + "=" * 60)
    print("🔧 Phase 1C: 道具三视图生成")
    print("=" * 60)

    props = parse_prop_bible()
    print(f"  📋 发现 {len(props)} 个道具")

    prop_uploaded = {}
    ref_index_path = PROPS_DIR / "ref_index.json"
    ref_index = {}
    if ref_index_path.exists():
        with open(ref_index_path, encoding="utf-8") as f:
            ref_index = json.load(f)

    for prop in props:
        pid = prop["id"]
        output_path = PROPS_DIR / f"{pid}_ref.png"

        if output_path.exists():
            print(f"  ⏭️ 已存在，跳过: {output_path.name}")
            part = load_image_as_part(str(output_path))
            if part:
                prop_uploaded[pid] = part
            continue

        prompt = (
            f"Generate a single reference sheet image showing a prop/object in 1x3 grid layout "
            f"(3 views side by side: front view, side profile, top-down view). "
            f"Prop description: {prop['ai_prompt']}. "
            f"Each grid cell shows the same object from a different angle. "
            f"Product design style, clean background, 16:9 aspect ratio."
        )

        print(f"  🎨 生成道具: {prop['name']} ({pid})")
        success = gemini_generate_image(prompt, str(output_path))

        if success:
            print(f"  ✅ {output_path.name}")
            part = load_image_as_part(str(output_path))
            if part:
                prop_uploaded[pid] = part
            ref_index[pid] = {
                "name": prop["name"],
                "source": "generated",
                "ref_file": f"props/{pid}_ref.png",
                "status": "ready",
                "reason": "由 generate_media.py 自动生成三视图参考图",
            }
        else:
            print(f"  ❌ 道具生成失败: {pid}")

        time.sleep(RATE_LIMIT_DELAY)

    with open(ref_index_path, "w", encoding="utf-8") as f:
        json.dump(ref_index, f, ensure_ascii=False, indent=2)
    print(f"  📋 道具索引已更新: {ref_index_path.name}")

    return prop_uploaded


# ========== Phase 2: 分镜图片生成（9宫格合成） ==========

# 角色与视觉模式映射——决定分镜生成时使用哪些角色参考图
VISUAL_MODE_CHAR_MAP = {
    "style_a":              ["创作者A"],
    "style_a_warm":         ["创作者A"],
    "style_a_transition":   ["创作者A", "创作者B"],
    "style_a_to_b":         ["创作者A", "创作者B"],
    "transition":           ["创作者A"],
    "transition_to_style_b":["创作者A", "创作者B"],
    "style_b":              ["创作者B"],
    "style_ab_flash":       ["创作者A", "创作者B"],
    "style_b_to_a":         ["创作者B", "创作者A"],
}


def process_segment(
    seg_dir: Path,
    seg_num: str,
    char_uploaded: dict,
    scene_uploaded: dict,
    prop_uploaded: dict,
) -> dict:
    """
    处理单个段落：读取 storyboard_config.json，
    一次性生成完整3×3九宫格合成图，再切割为9张单格图。
    """
    config_path = seg_dir / "storyboard_config.json"
    if not config_path.exists():
        print(f"  ⚠️ {seg_num}: storyboard_config.json 不存在，跳过")
        return {"images": 0, "failed": 0}

    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)

    video_id = config.get("video_id", seg_num)
    title = config.get("segment_title", "未知")
    grids = config.get("storyboard_9grid", [])

    # 获取视觉风格后缀
    vs = config.get("visual_style", {})
    active_mode = config.get("active_visual_mode", vs.get("active", "style_a"))
    style_suffix = ""
    if active_mode in vs:
        style_suffix = vs[active_mode].get("prompt_suffix", "")
    elif "style_a" in vs:
        style_suffix = vs["style_a"].get("prompt_suffix", "")

    # 收集参考图（角色 + 场景 + 道具）
    ref_files = []

    # 根据视觉模式选择角色参考图
    char_codes = VISUAL_MODE_CHAR_MAP.get(active_mode, ["创作者A"])
    for cn in char_codes:
        if cn in char_uploaded:
            ref_files.extend(char_uploaded[cn])
            print(f"   🎭 角色参考: {cn}")

    for sid in config.get("scene_refs", []):
        if sid in scene_uploaded:
            ref_files.append(scene_uploaded[sid])
    for pid in config.get("prop_refs", []):
        if pid in prop_uploaded:
            ref_files.append(prop_uploaded[pid])

    print(f"\n{'='*55}")
    print(f"📺 {seg_num}: {title} ({video_id})")
    print(f"   🎨 风格: {active_mode} | 参考图: {len(ref_files)} 张")
    print(f"{'='*55}")

    images_dir = seg_dir / "images"
    images_dir.mkdir(exist_ok=True)

    results = {"images": 0, "failed": 0}

    # 检查是否所有9张单格图都已存在
    all_exist = True
    for grid in grids:
        gn = grid["grid_number"]
        if not (images_dir / f"{video_id}_grid{gn:02d}.png").exists():
            all_exist = False
            break

    if all_exist and grids:
        print(f"  ⏭️ 全部 {len(grids)} 格已存在，跳过")
        results["images"] = len(grids)
        return results

    # 构建9格描述
    grid_descriptions = []
    for grid in grids:
        gn = grid["grid_number"]
        ai_prompt = grid.get("ai_image_prompt", "")
        if not ai_prompt:
            ai_prompt = grid.get("scene_description", f"Grid {gn}")

        grid_chars = grid.get("characters", [])
        char_parts = []
        for c in grid_chars:
            name = c.get("name", "")
            action = c.get("action", "")
            expr = c.get("expression", "")
            if name:
                desc = name
                if action:
                    desc += f" {action}"
                if expr:
                    desc += f" ({expr})"
                char_parts.append(desc)

        cell_desc = ai_prompt
        if char_parts:
            cell_desc += ". Characters: " + "; ".join(char_parts)

        grid_descriptions.append(f"Cell {gn} (row {(gn-1)//3+1}, col {(gn-1)%3+1}): {cell_desc}")

    grid_layout_text = "\n".join(grid_descriptions)

    # 构建完整的9宫格合成 prompt
    composite_prompt = (
        f"Generate a single composite storyboard image in a 3×3 grid layout (3 rows, 3 columns). "
        f"The image should be a cinematic storyboard for a music video segment titled '{title}'. "
        f"Each cell is a separate scene shot in 16:9 aspect ratio. "
        f"Draw thin white dividing lines between cells. "
        f"The 9 cells from left-to-right, top-to-bottom are:\n\n"
        f"{grid_layout_text}\n\n"
    )

    if style_suffix:
        composite_prompt += f"Overall visual style for ALL cells: {style_suffix}. "

    all_char_names = set()
    for grid in grids:
        for c in grid.get("characters", []):
            if c.get("name"):
                all_char_names.add(c["name"])
    if all_char_names and ref_files:
        composite_prompt += (
            f"Keep the character(s) [{', '.join(all_char_names)}] appearance consistent "
            f"with the provided reference images across all cells. "
        )

    composite_prompt += (
        "High quality, cinematic storyboard, consistent art style across all 9 cells. "
        "The overall image aspect ratio should be approximately 16:9 (wider than tall)."
    )

    # 合成图输出路径
    composite_path = images_dir / f"{video_id}_composite.png"

    if composite_path.exists():
        print(f"  ⏭️ 合成图已存在，直接切割: {composite_path.name}")
    else:
        print(f"  🎨 生成 3×3 九宫格合成图...")
        success = gemini_generate_image(composite_prompt, str(composite_path), ref_files if ref_files else None)
        if not success:
            print(f"  ❌ 合成图生成失败")
            results["failed"] = len(grids)
            return results
        print(f"  ✅ 合成图: {composite_path.name}")

    # 切割合成图为9张单格图
    try:
        composite_img = Image.open(str(composite_path))
        w, h = composite_img.size
        cell_w = w // 3
        cell_h = h // 3

        for grid in grids:
            gn = grid["grid_number"]
            row = (gn - 1) // 3
            col = (gn - 1) % 3

            left = col * cell_w
            upper = row * cell_h
            right = left + cell_w
            lower = upper + cell_h

            cell_img = composite_img.crop((left, upper, right, lower))
            cell_path = images_dir / f"{video_id}_grid{gn:02d}.png"
            cell_img.save(str(cell_path))
            print(f"  ✂️ grid {gn}: {cell_path.name} ({cell_img.size})")
            results["images"] += 1

        print(f"  🎉 {seg_num} 完成: {results['images']} 张分镜图")
    except Exception as e:
        print(f"  ❌ 切割失败: {e}")
        results["failed"] = len(grids)

    return results


# ========== 媒体索引 ==========

def generate_media_index(start_seg: int, end_seg: int):
    """生成 media_index.json"""
    index = {
        "project_id": "MV-002",
        "project_type": "mv",
        "characters": [],
        "scenes": [],
        "props": [],
        "segments": [],
    }

    for f in sorted(CHARACTERS_DIR.iterdir()):
        if f.suffix in (".png", ".jpeg", ".jpg"):
            index["characters"].append({"filename": f.name, "size_bytes": f.stat().st_size})

    for f in sorted(SCENES_DIR.iterdir()):
        if f.suffix in (".png", ".jpeg", ".jpg"):
            index["scenes"].append({"filename": f.name, "size_bytes": f.stat().st_size})

    for f in sorted(PROPS_DIR.iterdir()):
        if f.suffix in (".png", ".jpeg", ".jpg"):
            index["props"].append({"filename": f.name, "size_bytes": f.stat().st_size})

    for seg in range(start_seg, end_seg + 1):
        seg_num = f"SEG{seg:02d}"
        images_dir = SEGMENTS_DIR / seg_num / "images"
        if not images_dir.exists():
            continue

        seg_entry = {"segment": seg_num, "files": []}
        for f in sorted(images_dir.iterdir()):
            if f.suffix in (".png", ".jpeg", ".jpg"):
                seg_entry["files"].append({
                    "filename": f.name,
                    "type": "image",
                    "size_bytes": f.stat().st_size,
                })
        index["segments"].append(seg_entry)

    index_path = PROJECT_DIR / "media_index.json"
    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(index, f, ensure_ascii=False, indent=2)
    print(f"\n📋 媒体索引已生成: {index_path}")


# ========== 主流程 ==========

def main():
    start_seg = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    end_seg = int(sys.argv[2]) if len(sys.argv) > 2 else 10
    skip_assets = "--skip-assets" in sys.argv
    only_assets = "--only-assets" in sys.argv

    print(f"🎬 MV 媒体生成 - XWOW 凡人歌（港风茶山穿越版）")
    print(f"📁 项目: {PROJECT_DIR}")
    print(f"📺 范围: SEG{start_seg:02d} - SEG{end_seg:02d}")
    print()

    # ===== Phase 1: 角色参考图 =====
    char_uploaded = phase1_characters()

    # ===== Phase 1B & 1C: 场景/道具 =====
    scene_uploaded = {}
    prop_uploaded = {}
    if not skip_assets:
        scene_uploaded = phase1b_scenes()
        prop_uploaded = phase1c_props()
    else:
        print("\n⏭️ 跳过资产生成（--skip-assets）")
        # 加载已有资产
        for f in SCENES_DIR.iterdir():
            if f.suffix == ".png" and f.stem.endswith("_ref"):
                sid = f.stem.replace("_ref", "")
                part = load_image_as_part(str(f))
                if part:
                    scene_uploaded[sid] = part
        for f in PROPS_DIR.iterdir():
            if f.suffix == ".png" and f.stem.endswith("_ref"):
                pid = f.stem.replace("_ref", "")
                part = load_image_as_part(str(f))
                if part:
                    prop_uploaded[pid] = part

    if only_assets:
        print("\n🏁 仅资产模式完成（--only-assets）")
        generate_media_index(start_seg, end_seg)
        return

    # ===== Phase 2: 逐段生成分镜图 =====
    print("\n" + "=" * 60)
    print("🖼️ Phase 2: 分镜图片生成")
    print("=" * 60)

    total = {"images": 0, "failed": 0}

    for seg in range(start_seg, end_seg + 1):
        seg_num = f"SEG{seg:02d}"
        seg_dir = SEGMENTS_DIR / seg_num
        if not seg_dir.exists():
            print(f"  ⚠️ {seg_num} 不存在，跳过")
            continue

        result = process_segment(seg_dir, seg_num, char_uploaded, scene_uploaded, prop_uploaded)
        for k in total:
            total[k] += result[k]

    # ===== 生成索引 =====
    generate_media_index(start_seg, end_seg)

    # ===== 汇总 =====
    print(f"\n{'='*60}")
    print(f"🏁 全部完成!")
    print(f"👤 角色参考图: {len(char_uploaded)} 个角色")
    print(f"🏙️ 场景参考图: {len(scene_uploaded)} 张")
    print(f"🔧 道具参考图: {len(prop_uploaded)} 张")
    print(f"🖼️ 分镜图片: {total['images']} 张成功")
    print(f"❌ 失败: {total['failed']} 个")
    print(f"{'='*60}")


if __name__ == "__main__":
    main()
