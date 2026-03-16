#!/usr/bin/env python3
"""
Generate seedance_project_tasks.json for MV-001 project.

Reads all storyboard_config.json from segments/SEGxx/ and assembles
Seedance video generation tasks with proper prompts and reference files.
"""

import json
import os
from pathlib import Path
from datetime import date

PROJECT_DIR = Path(__file__).parent
SEGMENTS_DIR = PROJECT_DIR / "segments"

# ──────────────────────────────────────────────
# Exclusion boilerplate (same as DM projects)
# ──────────────────────────────────────────────
EXCLUSION_BOILERPLATE = (
    "从镜头1开始，不要展示多宫格分镜参考图片。"
    "分镜图制作成电影级别的高清影视级别的视频。"
    "严禁参考图出现在画面中。"
    "每个画面为单一画幅，独立展示，没有任何分割线或多宫格效果画面。"
    "(Exclusions); Do not show speech bubbles, do not show comic panels, "
    "remove all text, full technicolor."
    "排除项: No speech bubbles(无对话气泡),No text(无文字), "
    "No comic panels(无漫画分镜),No split screen(无分屏),"
    "No monochrome(非单色/黑白),No manga effects(无漫画特效线)."
    "正向替代:Fullscreen(全屏),Single continuous scene(单一连续场景)."
    "去掉图片中的水印，不要出现任何水印。没有任何字幕。"
)

# ──────────────────────────────────────────────
# Character reference file mapping
# ──────────────────────────────────────────────
CHAR_REFS = {
    "style_a":      ["characters/程序员_ref.jpeg"],
    "style_a_warm": ["characters/程序员_ref.jpeg"],
    "style_b":      ["characters/程序员_ref.jpeg", "characters/程序员_styleB_ref.png"],
    "style_ab_flash":["characters/程序员_ref.jpeg", "characters/程序员_styleB_ref.png"],
    "style_fusion": ["characters/程序员_ref.jpeg", "characters/程序员_styleB_ref.png"],
}

CHAR_LABELS = {
    "characters/程序员_ref.jpeg":     ("程序员", "角色「程序员」的参考形象"),
    "characters/程序员_styleB_ref.png": ("程序员·觉醒态", "角色「程序员·觉醒态」赛博铠甲参考形象"),
}


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def scene_ref_path(scene_id):
    return f"scenes/{scene_id}_ref.png"


def prop_ref_path(prop_id):
    return f"props/{prop_id}_ref.png"


# ──────────────────────────────────────────────
# Build the prompt text for a single segment
# ──────────────────────────────────────────────
def build_prompt(config):
    video_id       = config["video_id"]
    seg_title      = config["segment_title"]
    visual_mode    = config["active_visual_mode"]
    video_prompt   = config["video_prompt"]
    grids          = config["storyboard_9grid"]
    music          = config.get("music_sync", {})
    atmosphere     = config.get("atmosphere", {})
    scene_refs_ids = config.get("scene_refs", [])
    prop_refs_ids  = config.get("prop_refs", [])

    # ── Part 1: Reference intro ───────────────
    composite_file = f"{video_id}_composite.png"
    intro_parts = [f"(@{composite_file}) 为9宫格分镜参考图"]

    for ref_path in CHAR_REFS.get(visual_mode, CHAR_REFS["style_a"]):
        _, label = CHAR_LABELS[ref_path]
        fname = os.path.basename(ref_path)
        intro_parts.append(f"(@{fname}) 为{label}")

    ref_intro = "，".join(intro_parts) + "。"

    # ── Part 2: Scene / Prop reference tags ───
    sp_parts = []
    if scene_refs_ids:
        sp_parts.append("场景参考 " + " ".join(
            f"(@{os.path.basename(scene_ref_path(s))})" for s in scene_refs_ids
        ))
    if prop_refs_ids:
        sp_parts.append("道具参考 " + " ".join(
            f"(@{os.path.basename(prop_ref_path(p))})" for p in prop_refs_ids
        ))
    sp_text = "。".join(sp_parts) + "。" if sp_parts else ""

    # ── Part 3: Segment description ───────────
    overall_mood  = atmosphere.get("overall_mood", "")
    lyrics        = music.get("lyrics", "")
    music_section = music.get("music_section", "")

    seg_desc = (
        f"{video_id}「{seg_title}」。{video_prompt} "
        f"氛围：{overall_mood}。歌词：{lyrics}。"
        f"音乐段落：{music_section}。{sp_text}"
    )

    # ── Part 4: Per-grid shot descriptions ────
    grid_lines = []
    for grid in grids:
        gn         = grid["grid_number"]
        t0         = grid["time_start"]
        t1         = grid["time_end"]
        scene_desc = grid.get("scene_description", "")
        lyrics_at  = grid.get("lyrics_at_grid", "")
        cam        = grid.get("camera", {})
        cam_str    = ", ".join(filter(None, [
            cam.get("type", ""),
            cam.get("movement", ""),
            cam.get("angle", ""),
        ]))
        chars      = grid.get("characters", [])
        atm        = grid.get("atmosphere", "")
        sfx        = grid.get("sfx", "")

        # Character action text
        char_parts = []
        for c in chars:
            if isinstance(c, dict):
                name = c.get("name", "")
                action = c.get("action", "")
                expr = c.get("expression", "")
                if name and action:
                    char_parts.append(f"{name}{action}，表情{expr}")
        char_text = " ".join(char_parts) + "。" if char_parts else ""

        line = (
            f"镜头{gn}({t0}s-{t1}s): "
            f"{scene_desc}。歌词「{lyrics_at}」。"
            f"{cam_str}。{char_text}"
            f"{atm}。音效:{sfx}。"
        )
        grid_lines.append(line)

    # ── Assemble full prompt ──────────────────
    prompt = (
        f"{ref_intro}\n\n"
        f"{EXCLUSION_BOILERPLATE}\n\n"
        f"{seg_desc}\n\n"
        + "\n".join(grid_lines)
    )
    return prompt


# ──────────────────────────────────────────────
# Build the referenceFiles list for a segment
# ──────────────────────────────────────────────
def build_reference_files(config):
    video_id    = config["video_id"]
    seg_num     = video_id.split("-")[-1]          # e.g. "SEG01"
    visual_mode = config["active_visual_mode"]
    scene_ids   = config.get("scene_refs", [])
    prop_ids    = config.get("prop_refs", [])

    files = []

    # 1) Composite storyboard image
    files.append(f"segments/{seg_num}/images/{video_id}_composite.png")

    # 2) Character refs (based on visual mode)
    for ref in CHAR_REFS.get(visual_mode, CHAR_REFS["style_a"]):
        files.append(ref)

    # 3) Scene refs
    for s in scene_ids:
        files.append(scene_ref_path(s))

    # 4) Prop refs
    for p in prop_ids:
        files.append(prop_ref_path(p))

    return files


# ──────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────
def main():
    metadata     = load_json(PROJECT_DIR / "metadata.json")
    project_id   = metadata["project_id"]
    project_name = metadata.get("title", project_id)

    tasks = []

    for seg_num in range(1, 11):
        seg_id      = f"SEG{seg_num:02d}"
        seg_dir     = SEGMENTS_DIR / seg_id
        config_path = seg_dir / "storyboard_config.json"

        if not config_path.exists():
            print(f"⚠ {config_path} not found, skipping")
            continue

        config      = load_json(config_path)
        video_id    = config["video_id"]
        seg_title   = config["segment_title"]
        visual_mode = config["active_visual_mode"]

        prompt    = build_prompt(config)
        ref_files = build_reference_files(config)

        task = {
            "prompt":         prompt,
            "description":    f"{project_id} {seg_id}「{seg_title}」9宫格分镜→视频",
            "modelConfig": {
                "model":          "Seedance 2.0",
                "referenceMode":  "全能参考",
                "aspectRatio":    "16:9",
                "duration":       "15s",
            },
            "referenceFiles": ref_files,
            "realSubmit":     False,
            "priority":       1,
            "tags":           [project_id, seg_id, visual_mode],
        }
        tasks.append(task)
        print(f"  ✓ {video_id}「{seg_title}」— {visual_mode} — {len(ref_files)} refs")

    output = {
        "project_id":   project_id,
        "project_name": project_name,
        "total_tasks":  len(tasks),
        "created_date": str(date.today()),
        "tasks":        tasks,
    }

    output_path = PROJECT_DIR / "seedance_project_tasks.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"\n✅ Generated {len(tasks)} tasks → {output_path.name}")
    print(f"   Total reference files: {sum(len(t['referenceFiles']) for t in tasks)}")


if __name__ == "__main__":
    main()
