# Sniper Tools

Here are some useful tools to control AXIS V5915 cameras (aka snipers)

# SniperCalibrator

This tool allows you to setup the position of all teams on the 
contest floor in realate to the sniper. 

Requires file `input.txt` with 2D coordinates of teams.

Produces file `output.txt` with 3D coordinates of teams. This file
later need to be renamed as `coordinates-X.txt`, where `X` is the number
of sniper.

Usage: 
* Run `java SniperCalibrator`
* Mark the position of four teams
* Confirm that all other teams are marked correctly

# SniperMover

This tools points sniper to the desired team 
