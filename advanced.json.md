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
      "medias": {
        "screen": "https://cdn.videvo.net/videvo_files/video/free/2015-10/large_watermarked/Hacker_code_white_02_Videvo_preview.mp4",
        "camera": "https://cdn.videvo.net/videvo_files/video/free/2016-12/large_watermarked/Typing_dark_04_Videvo_preview.mp4"
      }
    }
  },
}
```

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
    "showTeamsWithoutSubmissions": true
  }
}
```

# Change problem colors
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

# Set contest start time, if it is not available from the contest management system
Please, do not use this option, if contest management system provides you with the scheduled contest start time. 
Some systems don't (PCMS), so this option allows you to have countdown before the contest start

```
{
  "startTime": "2022-04-13 10:10"
}
```
