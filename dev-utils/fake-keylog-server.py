#!/usr/bin/env python3
"""
Fake keylog NDJSON server for testing the live-v3 keylog feature.

Serves synthetic typing data per team:
  GET /keylog/<team-id>.ndjson  -> NDJSON of {timestamp, keys}
  GET /                         -> simple status page

Usage:
  python3 fake-keylog-server.py [--start ISO8601] [--port 5050] [--duration MIN]

If --start is omitted, defaults to NOW (UTC) minus 30 minutes — so you see
data already accumulated when you open the overlay. The same team-id always
produces the same pattern across runs (deterministic seed by team-id).

Wire it into live-v3 via `advanced.json` overrideTeamTemplate, e.g.:
  {
    "type": "overrideTeamTemplate",
    "extraMedias": {
      "keylog": {
        "type": "Text",
        "url": "http://localhost:5050/keylog/{team.id}.ndjson"
      }
    }
  }
"""
import argparse
import datetime
import hashlib
import json
import random
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse


def team_seed(team_id: str) -> int:
    return int(hashlib.md5(team_id.encode()).hexdigest()[:8], 16)


def generate_ndjson_for(team_id: str, start: datetime.datetime, duration_min: int) -> str:
    rng = random.Random(team_seed(team_id))
    base_kpm = rng.randint(40, 600)
    out = []
    for minute in range(duration_min):
        ts = start + datetime.timedelta(minutes=minute)
        # simulate variation + occasional bursts
        burst = 1.8 if rng.random() < 0.05 else 1.0
        kpm = max(0, int(rng.gauss(base_kpm, base_kpm * 0.25) * burst))
        if kpm == 0:
            continue
        shift = int(kpm * rng.uniform(0.05, 0.20))
        bare = kpm - shift
        keys = {}
        if bare > 0:
            keys["letter"] = {"bare": bare}
        if shift > 0:
            keys["Shift"] = {"shift": shift}
        out.append(json.dumps({"timestamp": ts.isoformat(), "keys": keys}))
    return "\n".join(out) + ("\n" if out else "")


class Handler(BaseHTTPRequestHandler):
    server_start: datetime.datetime = None  # set in main()
    duration_min: int = 300

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/":
            body = (
                f"Fake keylog server\n"
                f"Contest start: {self.server_start.isoformat()}\n"
                f"Duration:      {self.duration_min} min\n"
                f"\n"
                f"Try: /keylog/1.ndjson\n"
            ).encode()
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        if path.startswith("/keylog/") and path.endswith(".ndjson"):
            team_id = path[len("/keylog/"): -len(".ndjson")]
            body = generate_ndjson_for(team_id, self.server_start, self.duration_min).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/x-ndjson")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(body)
            return

        self.send_error(404)

    def log_message(self, fmt, *args):
        # keep it quiet by default; uncomment to debug
        # super().log_message(fmt, *args)
        pass


def main():
    parser = argparse.ArgumentParser(description="Fake keylog NDJSON server.")
    parser.add_argument("--start", help="ISO8601 contest start (default: now - 30min, UTC)")
    parser.add_argument("--port", type=int, default=5050)
    parser.add_argument("--duration", type=int, default=300, help="Contest length in minutes")
    args = parser.parse_args()

    if args.start:
        Handler.server_start = datetime.datetime.fromisoformat(args.start)
    else:
        Handler.server_start = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=30)
    Handler.duration_min = args.duration

    print(f"Fake keylog server on http://localhost:{args.port}/")
    print(f"Contest start: {Handler.server_start.isoformat()}")
    print(f"Duration:      {Handler.duration_min} min")
    print(f"Example:       http://localhost:{args.port}/keylog/1.ndjson")

    ThreadingHTTPServer(("0.0.0.0", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
