### Emulation mode

Emulation mode is useful to check everything on already happened contest.

If emulation is defined, instead of regular updating data, 
cds provider would download everything once, and events would be timely pushed further automatically.

Also, emulation can be speeded up several times for faster testing.

To enable the emulation mode, add following in your `settings.json` file
```
{
  "emulation": {
     "speed": 10,
     "startTime": "2022-04-03 22:50"
  }
}
```

This means that overlay would pretend that the contest was started at 22:50 
local time at the 4-th of March 2022. This is recommended format of time, although
some others would be parsed. Also, you can write `now` and it would be converted to time when
overlay was started. 


Emulation only works if the contest is finished.
