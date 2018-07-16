#!/bin/bash

RED="\033[31;1m"
RESET="\033[0m"

function check_range() {
  if [[ $1 -lt 0 || $1 -gt 255 ]]; then
    echo -e "${RED}error: $2 color out of range${RESET}" >&2
    exit 1
  fi
}

if [ $# -ne 3 ]; then
  echo -e "Usage: $0 <r> <g> <b>" >&2
  exit 1
fi

red=$1
green=$2
blue=$3

check_range $red "red"
check_range $green "green"
check_range $blue "blue"

rostopic pub -1 "/rgb_led/command" \
  ff_msgs/CommandStamped \
  "{ cmd_name: "set_color", \
     args: [ \
       { data_type: 3, i: ${red} }, \
       { data_type: 3, i: ${green} }, \
       { data_type: 3, i: ${blue} } \
     ]\
   }"

