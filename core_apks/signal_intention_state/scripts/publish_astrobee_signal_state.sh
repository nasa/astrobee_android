#!/bin/bash

RESET="\033[0m"
BLACK="\033[30m"
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
BLUE="\033[34m"
MAGENTA="\033[35m"
CYAN="\033[36m"
WHITE="\033[37m"
BOLDBLACK="\033[1m\033[30m"
BOLDRED="\033[1m\033[31m"
BOLDGREEN="\033[1m\033[32m"
BOLDYELLOW="\033[1m\033[33m"
BOLDBLUE="\033[1m\033[34m"
BOLDMAGENTA="\033[1m\033[35m"
BOLDCYAN="\033[1m\033[36m"
BOLDWHITE="\033[1m\033[37m"

function print_help {
  printf $BOLDWHITE"\n\nScript usage: publish_astrobee_signal_state.sh <state> <publish_options>\n\n<state>.$RESET One of the following:\n\n"
  for i in "${!states[@]}" 
  do
    echo "  $i"
  done
  printf $BOLDWHITE"\n\n<publish_options>.$RESET Same options showed in rostopic pub -h. This must be enclosed in \"\" if contains spaces \n\n"
  rostopic pub -h | grep -v Options | grep -v Usage
}

# Args
state=$1
rospub_args=$2

# State to be published
number_state=-1

# States
declare -A states

states["video_on"]=0
states["video_off"]=1
states["success"]=3
states["enter_hatchway"]=4
states["undock"]=5
states["unperch"]=6
states["motion_impaired"]=7
states["thrust_forward"]=8
states["thrust_aft"]=9
states["turn_right"]=10
states["turn_left"]=11
states["turn_up"]=12
states["turn_down"]=13
states["clear"]=14
states["sleep"]=15
states["wake"]=16

# Check if the user is asking for help
if [ "$1" == "-h" ] || [ "$1" == "--help" ] || [ "$1" == "-help" ] || [ "$1" == "help" ]; then
  print_help
  exit 1
fi

# Validation
if [ -z $state ]; then
  printf $BOLDRED"\nState is missing"$RESET
  print_help
  exit 1
fi

if [ -z $rospub_args ]; then
  printf $BOLDRED"\nRostopic pub args are missing"$RESET
  print_help
  exit 1
fi

# Check input
for i in "${!states[@]}" 
do
  if [ "$i" == "$state" ]; then
    number_state="${states[$i]}"
    break
  fi
done

if [ $number_state -eq -1 ]; then
  printf $BOLDRED"\nUnrecognized state"$RESET
  print_help
  exit 1
fi

# Publish
rostopic pub $rospub_args /signals ff_msgs/SignalState "header:
  seq: 0
  stamp:
    secs: 0
    nsecs: 0
  frame_id: ''
state: $number_state"
