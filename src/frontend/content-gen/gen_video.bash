#!/bin/bash
set -e
set -o pipefail
set -x

OFFSET=4
SPEEDUP=15
CONTEST_NAME="vkoshp"
REPO_ROOT=$(git rev-parse --show-toplevel)

# echo "Step 1: Download latest event-feed.json"
# rm -f event-feed-$CONTEST_NAME.ndjson
# timeout 10s wget --header 'cookie: JSESSIONID=0000ij9lihNPu7Xe7kc_R9VimX0:cde8627c-75ce-43c7-a09a-af5b62653e86:9eb3971a-d235-44b6-af0e-68c25f91e474:6e590761-52bd-44b0-8ab8-bb8884df8caf' \ -k https://172.24.0.7:7443/api/contests/wf48_$CONTEST_NAME/event-feed -q -O event-feed-$CONTEST_NAME.ndjson || true

# echo "Step 2: Edit the config for backend"
# cp $REPO_ROOT/artifacts/live-v3-dev.jar ./
# rm -rf config
# rsync -arv --exclude ach --exclude advanced.json /Volumes/C\$/work/overlay/config/ config

# mv event-feed-$CONTEST_NAME.ndjson config/$CONTEST_NAME/event-feed.ndjson
# we do a little trolling
# cp config/systest2_replay/event-feed-systest2.ndjson config/$CONTEST_NAME/event-feed.ndjson

# jq '.feeds[0].source = "."' config/$CONTEST_NAME/settings.json > config/$CONTEST_NAME/settings.json.tmp
# mv config/$CONTEST_NAME/settings.json.tmp config/$CONTEST_NAME/settings.json

startTime=$(python3 - <<EOF
import datetime
delta=datetime.timedelta(hours=$OFFSET)
padding = datetime.timedelta(seconds=15)
print((datetime.datetime.now(datetime.timezone.utc).astimezone() - delta / $SPEEDUP + padding).isoformat())
EOF
)
echo "Start time: $startTime"

cat <<EOF > config/$CONTEST_NAME/settings.json
{
  "type": "pcms",
  "network": { "allowUnsecureConnections": true },
  "source": "runs.xml",
  "emulation": {
    "speed": $SPEEDUP,
    "startTime": "$startTime",
  }
}
EOF

echo "Step 3: add ticker messages"
mkdir config/$CONTEST_NAME/presets || true
cat <<EOF > config/$CONTEST_NAME/presets/ticker.json
[
    {
        "type": "text",
        "part": "long",
        "periodMs": 30000,
        "text": "REPLAY"
    },
    {
        "type": "text",
        "part": "short",
        "periodMs": 30000,
        "text": "ICPCLive"
    }
]
EOF

echo "Step 4: Start backend"
java -jar live-v3-dev.jar -c config/$CONTEST_NAME --no-auth > ./backend.log &
BACKEND_PID=$!
function cleanup {
  echo "Step INF: Cleanup"
  kill $BACKEND_PID
  wait $BACKEND_PID || true
}
trap cleanup EXIT
sleep 5

echo "Step 5: Show ticker messages"
curl -X POST 'http://localhost:8080/api/admin/tickerMessage/1/show' -v
curl -X POST 'http://localhost:8080/api/admin/tickerMessage/2/show' -v

echo "Step 6: Start video generation"
stopwatch() {
    start=$(gdate +%s)
    while true; do
        time="$(( $(gdate +%s) - $start ))"
        printf '%s\r' "$(gdate -u -d "@$time" +%H:%M:%S)"
        sleep 0.1
    done
}
set +x
stopwatch &
STOPWATCH_PID=$!
./node_modules/.bin/playwright test tests/story.spec.ts
kill $STOPWATCH_PID


echo "Step 7: Reencode to mp4 60fps and speed up the result video"
lastVideo=$(ls -t videos/*.webm | head -n1)
echo "Last video: $lastVideo"
targetLength=55
speedup=$(echo "scale=4; (60 / $SPEEDUP) / ($targetLength / 60)" | bc)
ffmpeg -i $lastVideo -vf "setpts=PTS/$speedup" -r 60 -c:v libx264 -crf 23 -c:a aac -b:a 128k -y $lastVideo-offset-$OFFSET-$targetLength.mp4
targetLength=12
speedup=$(echo "scale=4; (60 / $SPEEDUP) / ($targetLength / 60)" | bc)
ffmpeg -i $lastVideo -vf "setpts=PTS/$speedup" -r 60 -c:v libx264 -crf 23 -c:a aac -b:a 128k -y $lastVideo-offset-$OFFSET-$targetLength.mp4


