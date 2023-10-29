import asyncio
import uvicorn
import json
import datetime
from starlette.applications import Starlette
from starlette.routing import Route
from starlette.responses import HTMLResponse, StreamingResponse

events = []
with open("./event-feed.json") as f:
    for line in f.readlines():
        events.append(json.loads(line))
print(f"Read {len(events)} events")

def event_key(event):
    if "data" in event:
        if "start_time" in event["data"]:
            return event["data"]["start_time"]
        if "time" in event["data"]:
            return event["data"]["time"]
    return ""

# events.sort(key=event_key)
cur_pos = 0

def read_until(predicate):
    global cur_pos
    while not predicate(events[cur_pos]):
        cur_pos += 1

def is_submission(ev):
    return ev["type"] == "submissions"

def is_ac(ev):
    return "data" in ev and ev["data"].get("judgement_type_id") == "AC"

read_until(is_submission)

generators = []

start_clock = datetime.datetime.now()
async def make_event_from_now(ev):
    if ev["type"] == "contest":
        ev["data"]["start_time"] = start_clock.isoformat()
    if "data" in ev:
        if "time" in ev["data"]:
            ev["data"]["time"] = datetime.datetime.now().isoformat()
    return ev

async def event_generator():
    ev = asyncio.Event()
    generators.append(ev)
    self_pos = 0
    while True:
        ev.clear()
        for i in range(self_pos, cur_pos):
            print(f"Sending event {i}")
            evi = await make_event_from_now(events[i])
            yield json.dumps(evi).encode() + b"\n"
        self_pos = cur_pos
        await ev.wait()


async def sse(request):
    return StreamingResponse(event_generator())

async def notify():
    for generator in generators:
        generator.set()
async def increment(request):
    global cur_pos
    cur_pos += 1
    await notify()
    return HTMLResponse("ok")

async def skip(request):
    read_until(is_submission)
    global cur_pos
    cur_pos += 1
    await notify()
    return HTMLResponse("ok")

async def ac(request):
    read_until(is_ac)
    global cur_pos
    cur_pos += 1
    await notify()
    return HTMLResponse("ok")

routes = [
    Route("/event-feed", endpoint=sse),
    Route("/increment", endpoint=increment),
    Route("/skip", endpoint=skip),
    Route("/ac", endpoint=ac)
]

app = Starlette(debug=True, routes=routes)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level='info')
