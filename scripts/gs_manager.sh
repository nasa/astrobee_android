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
  printf $BOLDWHITE"\nScript usage: gs_manager.sh <action>\n"
  
  printf $BOLDWHITE"\n<action>$RESET One of the following\n\n"
  printf $BOLDWHITE"    ${arg_1[arg_start]}.$RESET It will start the target using an ADB link.\n"
  printf $BOLDWHITE"    ${arg_1[arg_stop]}.$RESET It will try to stop the target in a cleanly manner.\n"
  printf $BOLDWHITE"    ${arg_1[arg_restart]}.$RESET It will execute an stop and start command sequence.\n\n"
  printf $BOLDWHITE"    ${arg_1[arg_hard_stop]}.$RESET It will kill the target using hard-stopping methods. Don't use this option unless needed.\n"
}

# Allowed values
declare -A arg_1

arg_1["arg_start"]="start"
arg_1["arg_stop"]="stop"
arg_1["arg_restart"]="restart"
arg_1["arg_hard_stop"]="hard_stop"

# Check if the user is asking for help
if [ "$1" == "-h" ] || [ "$1" == "--help" ] || [ "$1" == "-help" ] || [ "$1" == "help" ]; then
  print_help
  exit 1
fi

# Check first argument
arg_1_ok=false
for i in "${arg_1[@]}"
do
  if [ "$i" == "$1" ]; then
    arg_1_ok=true
    break
  fi
done

if [ "$arg_1_ok" == false ]; then
  printf $BOLDRED"\nArgument unknown\n\n"$RESET
  print_help
  exit 1
fi

# Put input together
input=$1

case "$input" in
  "start")
    adb shell am startservice gov.nasa.arc.astrobee.android.gs.manager/.ManagerService
    ;;
  "stop")
    adb shell am stopservice gov.nasa.arc.astrobee.android.gs.manager/.ManagerService
    ;;
  "hard_stop")
    adb shell am force-stop gov.nasa.arc.astrobee.android.gs.manager
    ;;
  "restart")
    adb shell am force-stop gov.nasa.arc.astrobee.android.gs.manager
    adb shell am startservice gov.nasa.arc.astrobee.android.gs.manager/.ManagerService
    ;;
esac

