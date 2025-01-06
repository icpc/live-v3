This doc is a bit outdated, although it is still a good source of examples.
Full description of all properties supported can be found [here](https://icpc.io/live-v3/cds/cds/core/org.icpclive.cds.tunning/-tuning-rule/index.html)

# How to setup advanced.json

Apart from ```settings.json``` file, each contest can adjust imported from contest management system using ```advanced.json``` file.
Check the current status at [http://localhost:8080/api/admin/advancedJsonPreview?fields=all](http://localhost:8080/api/admin/advancedJsonPreview?fields=all)

## Change contestant name, hashtag, group, medias and other properties

You can adjust received participant information for each separate participant using ```advanced.json```. 

```
{
    "type": "overrideTeams",
    "rules": {
        "spb058": {
            "fullName": "SPb ITMO: pengzoo (Iakovlev, Golikov, Perveev)",
            "displayName": "SPb ITMO pengzoo",
            "organizationId": "SPb ITMO",
            "hashTag": "#spb058",
            "isHidden": false,
            "isOutOfContest": false
            "groups": [
                "spb-site", "SPb ITMO"
            ],
            "customFields": {
                "funnyName": "pengzoo",
                "grabberPeerName": "058",
                "grabberIp": "192.168.0.112"
            },
            "medias": {
                "achievement": {
                    "type": "Image",
                    "url": "/media/ach/spb058.svg"
                },
                "photo": {
                    "type": "Image",
                    "url": "/media/ach/spb058.svg"
                },
                "screen": {
                    "type": "WebRTCGrabberConnection",
                    "url": "http://192.168.0.112:8080",
                    "peerName": "058",
                    "streamType": "desktop",
                    "credential": "live"
                },
                "camera": {
                    "type": "WebRTCGrabberConnection",
                    "url": "{grabberUrl}",
                    "peerName": "http://192.168.0.112:8080",
                    "streamType": "webcam",
                    "credential": "live"
                }
            }
        }
    }
}
```

`isHidden` and `isOutOfContest` can be be applied to groups:
```
{
    "type": "overrideGroups",
    "rules": {
        "judges": {
            "isHidden": true,
            "isOutOfContest": false
        },
        "sponsors": {
            "isHidden": false,
            "isOutOfContest": true
        },
        "local": {
            "isHidden": false,
            "isOutOfContest": false
        }
    }
}
```

`isHidden`: allows to hide a team or a group of teams from everywhere
`isOutOfContest`: replaces team place with * sign, but still shows the team in testing queue, leaderboard and others.

Also, you can create a template rule for medias, and it would be applied to all teams.

```
{
    "type": "overrideTeamTemplate",
   "medias": {
        "screen": {
            "type": "WebRTCGrabberConnection",
            "url": "http://{grabberIp}:13478",
            "peerName": "{grabberPeerName}",
            "streamType": "desktop",
            "credential": "live"
        },
        "camera": {
            "type": "WebRTCGrabberConnection",
            "url": "http://{grabberIp}:13478",
            "peerName": "{grabberPeerName}",
            "streamType": "webcam",
            "credential": "live"
        },
        "achievement": {
            "type": "Image",
            "url": "/media/ach/{team.id}.svg"
        }
    }
}
```

Avaliable medias: `"camera"`, `"screen"`, `"record"`, `"photo"`, `"reactionVideo"`, `"achievement"`. 

Avaliable media types:
* `{ "type": "Image", "url": "url" }` - any picture than can be embedded in img html tag
* `{ "type": "Object", "url": "url" }` - file that can be embedded in html page, e.g. svg with animations
* `{ "type": "Video", "url": "url" }` - video or http stream that supported web browser and can embedded in video tag
* `{ "type": "M2tsVideo", "url": "url" }` - video or http stream in mpeg ts container
* `{ "type": "HLSVideo", "url": "url", "jwtTocken": "optional" }` - HLS video or stream
* `{ "type": "WebRTCProxyConnection", "url": "url" }` - connection to http stream via [WebRTCProxy](https://github.com/kbats183/webrtc-proxy)
* `{ "type": "WebRTCGrabberConnection", "url": "signallingUrl (for example https://grabber.kbats.ru)", "peerName": "peerName", "streamType": "desktop/webcam", "credential": "optional" }` - connection to desktop, webcam or etc. using [WebRTCGrabber](https://github.com/irdkwmnsb/webrtc-grabber)

# Customize ranking rules
```
{
    "type": "overrideScoreboardSettings",
    "penaltyPerWrongAttempt": 20,
    "penaltyRoundingMode": "each_submission_down_to_minute",
    "showTeamsWithoutSubmissions": true
}
```

Default ```penaltyRoundingMode``` is CDS-specific. But you are welcome to override them here between two options:
```each_submission_down_to_minute``` or ```sum_down_to_minute```. 

# Customizing awards

Typical awards setup includes only medals, and can be done like this

```
{
    "type": "addMedals",
    "gold": 4,
    "silver": 4,
    "bronze": 4
},
{
    "type": "overrideAwards",
    "championTitle": "Northern Eurasia Champions",
    "rankAwardsMaxRank": 13,
    "medalGroups": [
        {
            "medals": [
                {
                    "id": "first-diploma",
                    "citation": "First Award",
                    "minScore": 8.0,
                    "tiebreakMode": "NONE"
                },
                {
                    "id": "second-diploma",
                    "citation": "Second Award",
                    "minScore": 6.0,
                    "tiebreakMode": "NONE"
                },
                {
                    "id": "third-diploma",
                    "citation": "Third Award",
                    "minScore": 5.0,
                    "tiebreakMode": "NONE"
                }
            ]
        }
    ]
},
```

More different types of awards are supported for cds-converter. 
Check [full awards settings doc](https://icpc.io/live-v3/cds/cds/core/org.icpclive.cds.api/-awards-settings/index.html) for details.

# Change problem info
## Color
```
{
    "type": "overrideProblems",
    "rules": {
            "A": {"color": "#e6194B"},
            "B": {"color": "#3cb44b"},
            "C": {"color": "#ffe119"},
            "D": {"color": "#4363d8"},
            "E": {"color": "#f58231"},
            "F": {"color": "#42d4f4"},
            "G": {"color": "#f032e6"},
            "H": {"color": "#fabed4"},
            "I": {"color": "#469990"},
            "J": {"color": "#dcbeff"},
            "K": {"color": "#9A6324"},
            "L": {"color": "#fffac8"},
            "M": {"color": "#800000"},
            "N": {"color": "#aaffc3"},
            "O": {"color": "#000075"},
            "P": {"color": "#a9a9a9"}
    }
}
```

## Other
```
{
  "type": "overrideProblems",
  "rules": {
  {
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
    "type": "overrideContestSettings",
    "startTime": "2024-12-14 23:13:04 -08:00",
    "freezeTimeSeconds": 14400
},
```

```startTime``` -- Some systems (PCMS) don't provide contest start time, so this option allows you to have countdown before the contest start.
```freezeTimeSecond``` -- after how many seconds after the start of the contest will the freeze period start. Freeze never ends.
```holdTimeSeconds``` -- only before the start of the contest. You can delay the start of the contest and hold the countdown on that value.


