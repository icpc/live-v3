import json
import math
import time
import threading
from collections import defaultdict
from datetime import datetime, timedelta
from flask import Flask, jsonify, Response
from flask_cors import CORS
from pathlib import Path

# CONFIG
KEYLOG_FOLDER = "keylogs"
API_PORT = 5050
POLL_INTERVAL = 2
MAX_MINUTES = 300

# IMPORTANT:
start_time = datetime.fromisoformat("2026-03-22T09:54:56.000-04:00")

# SHARED STATE
team_buckets = defaultdict(lambda: defaultdict(lambda: {
    "keys": defaultdict(lambda: defaultdict(int)),
    "mouse": {"distance": 0.0}
}))
team_last_positions = defaultdict(int)

# для mouse x/y
team_last_mouse_pos = defaultdict(lambda: None)

lock = threading.Lock()


def bucket_timestamp(minute: int) -> str:
    return (start_time + timedelta(minutes=minute)).isoformat()


def get_minute_index(timestamp: str):
    try:
        dt = datetime.fromisoformat(timestamp)
        minute = int((dt - start_time).total_seconds() // 60)
#        print(minute)
        if 0 <= minute < MAX_MINUTES:
            return minute
    except Exception:
        pass
    return None


def ensure_bucket(team_name: str, minute: int):
    _ = team_buckets[team_name][minute]


def handle_key_event(team_name: str, minute: int, log_entry: dict):
    data = log_entry.get("data", {})
    if data.get("event_type") != "down":
        return

    key = data.get("key")
    if not key or key.startswith("KEY"):
        return

    shift = bool(data.get("shift"))
    ctrl = bool(data.get("ctrl"))

    bucket = team_buckets[team_name][minute]
    key_stats = bucket["keys"][key]

    # raw = просто любое нажатие этой клавиши
    key_stats["raw"] += 1

    if ctrl and shift:
        key_stats["ctrl+shift"] += 1
    elif ctrl:
        key_stats["ctrl"] += 1
    elif shift:
        key_stats["shift"] += 1
    else:
        key_stats["bare"] += 1


def handle_mouse_event(team_name: str, minute: int, log_entry: dict):
    data = log_entry.get("data", {})
    bucket = team_buckets[team_name][minute]
    mouse = bucket["mouse"]

    # 1) Если distance уже пришел готовым
    if "distance" in data:
        try:
            mouse["distance"] += float(data["distance"])
            return
        except Exception:
            pass

    # 2) Если есть относительное смещение dx/dy
    if "dx" in data or "dy" in data:
        dx = float(data.get("dx", 0))
        dy = float(data.get("dy", 0))
        mouse["distance"] += math.hypot(dx, dy)
        return

    # 3) Если есть абсолютные координаты x/y — считаем расстояние между точками
    if "x" in data and "y" in data:
        try:
            x = float(data["x"])
            y = float(data["y"])
            prev = team_last_mouse_pos[team_name]
            if prev is not None:
                mouse["distance"] += math.hypot(x - prev[0], y - prev[1])
            team_last_mouse_pos[team_name] = (x, y)
        except Exception:
            pass


def parse_line(line: str, team_name: str):
    try:
        log_entry = json.loads(line)
    except json.JSONDecodeError:
        print("Skipping invalid JSON line")
        return

    timestamp = log_entry.get("time")
    if not timestamp:
        return

    minute = get_minute_index(timestamp)
    if minute is None:
        return

    ensure_bucket(team_name, minute)

    event_name = log_entry.get("name")
    if event_name == "modifier":
        handle_key_event(team_name, minute, log_entry)
    elif event_name == "mouse":
        handle_mouse_event(team_name, minute, log_entry)


def watch_file(file_path, team_name):
    while True:
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                f.seek(team_last_positions[team_name])
                new_lines = f.readlines()

                with lock:
                    for line in new_lines:
                        parse_line(line, team_name)

                team_last_positions[team_name] = f.tell()
        except Exception as e:
            print(f"Error reading file {file_path}: {e}")

        time.sleep(POLL_INTERVAL)


def start_watchers():
    for file in Path(KEYLOG_FOLDER).glob("*.txt"):
        team_name = file.stem.split("_")[0]
        thread = threading.Thread(
            target=watch_file,
            args=(file, team_name),
            daemon=True
        )
        thread.start()


def cleanup_record(record: dict) -> dict:
    """
    Убираем пустые ключи/поля.
    """
    result = {
        "timestamp": record["timestamp"]
    }

    if record.get("keys"):
        cleaned_keys = {}
        for key_name, stats in record["keys"].items():
            filtered_stats = {k: v for k, v in stats.items() if v != 0}
            if filtered_stats:
                cleaned_keys[key_name] = filtered_stats
        if cleaned_keys:
            result["keys"] = cleaned_keys

    mouse = record.get("mouse", {})
    cleaned_mouse = {}
    for k, v in mouse.items():
        if isinstance(v, (int, float)) and v != 0:
            cleaned_mouse[k] = round(v, 3)
        elif v:
            cleaned_mouse[k] = v

    if cleaned_mouse:
        result["mouse"] = cleaned_mouse

    return result


def build_team_records(team_name: str):
    records = []
    buckets = team_buckets.get(team_name, {})

    for minute in sorted(buckets.keys()):
        bucket = buckets[minute]
        record = {
            "timestamp": bucket_timestamp(minute),
            "keys": {},
            "mouse": {}
        }

        # keys
        for key_name, stats in bucket["keys"].items():
            record["keys"][key_name] = dict(stats)

        # mouse
        if bucket["mouse"]["distance"] != 0:
            record["mouse"]["distance"] = bucket["mouse"]["distance"]

        cleaned = cleanup_record(record)

        # пропускаем совсем пустые записи
        if len(cleaned.keys()) > 1:
            records.append(cleaned)

    return records


app = Flask(__name__)
CORS(app)


@app.route("/teams", methods=["GET"])
def list_teams():
    with lock:
        return jsonify(sorted(team_buckets.keys()))


@app.route("/keylog/<team_name>", methods=["GET"])
def get_team_actions_json(team_name):
    with lock:
        return jsonify(build_team_records(team_name))


@app.route("/keylog/<team_name>.ndjson", methods=["GET"])
def get_team_actions_ndjson(team_name):
    def generate():
        with lock:
            records = build_team_records(team_name)
        for record in records:
            yield json.dumps(record, ensure_ascii=False) + "\n"

    return Response(generate(), mimetype="application/x-ndjson")


if __name__ == "__main__":
    start_watchers()
    app.run(host="0.0.0.0", port=API_PORT)
