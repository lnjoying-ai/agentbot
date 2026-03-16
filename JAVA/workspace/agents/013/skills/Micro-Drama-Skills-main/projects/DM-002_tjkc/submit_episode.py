#!/usr/bin/env python3
"""
提交单集 Seedance 任务脚本

用法:
    python submit_episode.py 1        # 提交第1集
    python submit_episode.py 5        # 提交第5集
    python submit_episode.py 1-5      # 提交第1~5集
"""

import json
import base64
import os
import sys
import mimetypes
from datetime import datetime

import requests

PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
API_BASE = "http://localhost:3456"


def expand_reference_files(ref_paths, project_dir):
    """将相对路径列表展开为 base64 对象列表"""
    result = []
    for rel_path in ref_paths:
        abs_path = os.path.join(project_dir, rel_path)
        file_name = os.path.basename(rel_path)
        mime_type = mimetypes.guess_type(abs_path)[0] or "image/png"
        with open(abs_path, "rb") as f:
            b64 = base64.b64encode(f.read()).decode()
        result.append({
            "fileName": file_name,
            "base64": f"data:{mime_type};base64,{b64}",
            "fileType": mime_type,
        })
    return result


def submit_episode(ep_num):
    """提交单集任务，返回 (success, result_dict)"""
    ep_code = f"EP{ep_num:02d}"
    tasks_file = os.path.join(PROJECT_DIR, "episodes", ep_code, "seedance_tasks.json")

    if not os.path.exists(tasks_file):
        print(f"❌ {ep_code}: seedance_tasks.json 不存在")
        return False, None

    with open(tasks_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 构造提交 payload
    tasks_payload = []
    for task in data["tasks"]:
        payload = {
            "prompt": task["prompt"],
            "description": task.get("description", ""),
            "modelConfig": task.get("modelConfig", {}),
            "referenceFiles": expand_reference_files(task["referenceFiles"], PROJECT_DIR),
            "realSubmit": task.get("realSubmit", True),
            "priority": task.get("priority", 1),
            "tags": task.get("tags", []),
        }
        tasks_payload.append(payload)

    # 提交
    resp = requests.post(f"{API_BASE}/api/tasks/push", json={"tasks": tasks_payload})
    result = resp.json()

    if resp.status_code == 200 and result.get("success"):
        codes = result.get("taskCodes", [])
        print(f"✅ {ep_code}: 提交成功 → {', '.join(codes)}")
    else:
        print(f"❌ {ep_code}: 提交失败 → {result}")

    return result.get("success", False), result


def parse_episodes(arg):
    """解析集数参数，支持 '1', '01', '1-5' 格式"""
    if "-" in arg:
        start, end = arg.split("-", 1)
        return list(range(int(start), int(end) + 1))
    return [int(arg)]


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    episodes = parse_episodes(sys.argv[1])
    print(f"📦 准备提交 {len(episodes)} 集: {[f'EP{e:02d}' for e in episodes]}")
    print(f"🔗 API: {API_BASE}\n")

    all_codes = []
    failed = []

    for ep in episodes:
        ok, result = submit_episode(ep)
        if ok and result:
            all_codes.extend(result.get("taskCodes", []))
        else:
            failed.append(f"EP{ep:02d}")

    # 汇总
    print(f"\n{'='*40}")
    print(f"总集数: {len(episodes)}")
    print(f"成功任务: {len(all_codes)}")
    print(f"失败集数: {len(failed)}")
    if all_codes:
        print(f"TaskCodes: {', '.join(all_codes)}")
    if failed:
        print(f"失败: {', '.join(failed)}")


if __name__ == "__main__":
    main()
