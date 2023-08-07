# How to setup advanced.json

Apart from ```events.properties``` file, each contest can adjust imported from contest management system using ```advanced.json``` file.

## Change contestant name, hashtag, group, medias and other properties

You can adjust received participant information for each separate participant using ```advanced.json```. 
```
{
  "teamOverrides": {
    "486861": {
      "shortname": "ITMO test override", 
      "name":"ITMO NAME test override",
      "hashTag": "#ItMo",
      "groups": ["SPb ITMO", "SPb"],
      "isHidden": false,
      "isOutOfContest": false, 
      "medias": {
        "screen": {
          "type": "Video",
          "url": "https://cdn.videvo.net/videvo_files/video/free/2015-10/large_watermarked/Hacker_code_white_02_Videvo_preview.mp4"
        },
        "camera": {
          "type": "Video",
          "url": "https://cdn.videvo.net/videvo_files/video/free/2015-10/large_watermarked/Hacker_code_white_02_Videvo_preview.mp4"
        }
      }
    }
  },
}
```

`isHidden` and `isOutOfContest` can be be applied to groups:
```
  "groupOverrides": {
    "test": {"isOutOfContest":true}
  }
```

`isHidden`: allows to hide a team or a group of teams from everywhere
`isOutOfContest`: replaces team place with * sign, but still shows the team in testing queue, leaderboard and others.

Also, you can create a template rule for medias, and it would be applied to all teams.

```
  "teamMediaTemplate": {
    "record": {
      "type": "Video",
      "url": "http://localhost:8080/media/screen/record{teamId}.mp4"
    },
    "screen": {
      "type": "Video",
      "url": "http://localhost:8080/media/screen/screen{teamId}"
    },
    "camera": {
      "type": "Video",
      "url": "http://localhost:8080/media/camera/camera{teamId}"
    },
    "achievement": {
      "type": "Photo",
      "url": "http://localhost:8080/media/achievements/achievements{teamId}.svg"
    }
  },
```

Avaliable medias: `"camera"`, `"screen"`, `"record"`, `"photo"`, `"reactionVideo"`, `"achievement"`. 

Avaliable media types:
* `{ "type": "Photo", "url": "url" }` - photo
* `{ "type": "Object", "url": "url" }` - file that can be embedded in html page, e.g. svg with animations
* `{ "type": "Video", "url": "url" }` - video or http stream that supported web browser
* `{ "type": "WebRTCProxyConnection", "url": "url" }` - connection to http stream via [WebRTCProxy](https://github.com/kbats183/webrtc-proxy)
* `{ "type": "WebRTCGrabberConnection", "url": "signallingUrl (with /admin)", "peerName": "peerName", "streamType": "desktop/webcam", "credential": "optional" }` - connection to desktop, webcam or etc. using [WebRTCGrabber](https://github.com/irdkwmnsb/webrtc-grabber)

# Customize ranking rules
```
{
  "scoreboardOverrides": {
    "medals": [
      {"name": "gold", "count": 4},
      {"name": "silver", "count": 4},
      {"name": "bronze", "count": 4}
    ],
    "penaltyPerWrongAttempt": 20,
    "showTeamsWithoutSubmissions": true,
    "penaltyRoundingMode": "each_submission_down_to_minute"
  }
}
```

Default ```penaltyRoundingMode``` is CDS-specific. But you are welcome to override them here between two options:
```each_submission_down_to_minute``` or ```sum_down_to_minute```. 

# Change problem info
## Color
```
{
  "problemOverrides": {
    "A":{"color":"#e6194B"},
    "B":{"color":"#3cb44b"},
    "C":{"color":"#ffe119"},
    "D":{"color":"#4363d8"},
    "E":{"color":"#f58231"},
    "F":{"color":"#42d4f4"},
    "G":{"color":"#f032e6"},
    "H":{"color":"#fabed4"},
    "I":{"color":"#469990"},
    "J":{"color":"#dcbeff"},
    "K":{"color":"#9A6324"},
    "L":{"color":"#fffac8"},
    "M":{"color":"#800000"},
    "N":{"color":"#aaffc3"},
    "O":{"color":"#000075"},
    "P":{"color":"#a9a9a9"}
  }
}
```

## Other
```
{
  "problemOverrides": {
    "A":{
      "name":"Pineapple",
      "color":"#a9a9a9",
      "minScore":"-100",
      "maxScore":"100",
      "scoreMergeMode":"MAX_PER_GROUP"
    },
    "B":{
      "scoreMergeMode":"MAX_TOTAL"
    },
    "C":{
      "scoreMergeMode":"LAST"
    },
    "D":{
      "scoreMergeMode":"LAST_OK"
    },
    "E":{
      "scoreMergeMode":"SUM"
    }
  }
}
```
A little more info on ```scoreMergeMode``` -- it is only applicable to score-based leaderboards:
* ```MAX_TOTAL``` -- each submission has total score. Final score for that problem is max of these values
* ```MAX_PER_GROUP``` -- IOI style. Each submission total score is sum of scores for each group of tests in that problem. Final score for that problem is sum of max scores for each group over all contestant submissions on that problem.
* ```LAST``` -- ML style. Final score is just the score of last submission
* ```LAST_OK``` -- ML style. Final score is just the score of last OK submission
* ```SUM``` -- codeforces hack hack. Final score fore the problem is THE SUM OF ALL CONTESTANT'S SCORES FOR SUBMISSIONS on this problem. CF Hacks are treated as submissions into an extra problem with scores +100 or -50. 


# Advanced contest start time management

```
{
  "startTime": "2022-04-13 10:10",
  "freezeTimeSeconds": 14400,
  "holdTimeSeconds": 600
}
```

```startTime``` -- Some systems (PCMS) don't provide contest start time, so this option allows you to have countdown before the contest start.
```freezeTimeSecond``` -- after how many seconds after the start of the contest will the freeze period start. Freeze never ends.
```holdTimeSeconds``` -- only before the start of the contest. You can delay the start of the contest and hold the countdown on that value.


