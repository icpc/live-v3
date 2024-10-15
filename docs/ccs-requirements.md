To support your contest system in icpc-live, we would need to provide an API with the following data. If something is not configurable, it's okay to provide documentation for the data in API.
* Must have block means, we probably wouldn't be able to do anything reasonable without this data, or it would require a lot of manual work to setup each stream. 
* Nice to have block means, it's okey to not provide this data, but it can cause significant inconviniencies or make setup very error-prone.
* Optional block means, we don't need this data for basic support, but it's required only for advanced features. 
  
# Required information

## Information about contest
### Must have:
* Start time
* Contest-length
* Description of problems
### Nice to have:
* Freeze time
* Contest rules adjustments if they are configurable. For example penalty calculation rules
* Teams descriptions in advance
  * We can create teams in the moment of first submit, but that's annoying
### Optional
* Contest name
* Contest logos
* Awards distribution description
* Everything else supported [here](https://icpc.io/live-v3/cds/cds/core/org.icpclive.cds.api/-contest-info/index.html)

# Information about problems
### Must have:
* Problem id, to be referred in runs, consistent between different queries
* Name to display in scoreboard
### Nice to have
* Max points if configurable for IOI mode
* Rules for merging submission scores for IOI mode
* Order in which problem appear in scoreboard (can be implicit by return order in description)
### Optional
* Full problem name
* Baloon color

# Information about teams
### Must have
* Team id, to be referred in runs, consistent between different queries
* Team name to display in the scoreboard

### Optional
* Official team name
* Link to team photos/screen/webcam/etc
* University/ICPC region/other reasonable grouping
* Everything else supported [here](https://icpc.io/live-v3/cds/cds/core/org.icpclive.cds.api/-team-info/index.html)

# Information about runs
### Must-have
* Team id
* Problem id
* Time relative to contest start
* Verdict
* Submission score in IOI mode
### Nice to have
* A description of what verdicts exist, whether they are correct, and whether they are adding a penalty.
* Distribution of score per subtask in IOI mode
### Optional
* Some submit-related medias, like link to recording of team reacting submission result

# Way of loading data

By default, we want to reload all data each 5 seconds. This is the simplest way. It works correctly with rejudges and reconnections.
Some streaming options are discussable if the contest is very large, or it's your native way of distributing data.
Our experience shows that a full reload is okay for the competition of several hundred teams.

# Can you use my scoreboard calculation, instead of separate runs?

No, we can't. It would basically break everything except the scoreboard, making the overlay much less valuable. 
For example, we wouldn't be able to match result changes with specific submissions, which we need for testing queue.
Also, we need to be able to recalculate the scoreboard for different "what-if" features anyway. 
