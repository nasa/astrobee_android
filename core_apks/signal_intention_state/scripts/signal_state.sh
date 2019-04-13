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
  printf $BOLDWHITE"\nScript usage: signal_state.sh <target> <action>\n\n<target>$RESET One of the following\n\n"
  printf $BOLDWHITE"    ${arg_1[arg_app]}.$RESET The action will be aplied to the whole app\n"
  printf $BOLDWHITE"    ${arg_1[arg_interface]}.$RESET The action will be aplied to the interface (android activity) showing the signals\n"
  printf $BOLDWHITE"    ${arg_1[arg_service]}.$RESET The action will be aplied to the android service in charge of the app-ROS connection\n"

  printf $BOLDWHITE"\n<action>$RESET One of the following\n\n"
  printf $BOLDWHITE"    ${arg_2[arg_start]}.$RESET It will start the target using an ADB link.\n"
  printf $BOLDWHITE"    ${arg_2[arg_stop]}.$RESET It will try to stop the target in a cleanly manner.\n"
  printf $BOLDWHITE"    ${arg_2[arg_restart]}.$RESET It will execute an stop and start command sequence.\n\n"
  printf $BOLDWHITE"    ${arg_2[arg_hard_stop]}.$RESET It will kill the target using hard-stopping methods. Don't use this option unless needed.\n"
  printf "    If use this method make sure to start again the app before turning the HLP off or the app won't run broadcast receivers on boot completed\n\n"

  printf $BOLDYELLOW"IMPORTANT.$YELLOW Don't execute \"start\" or \"restart\" on \"app\" or \"service\" targets unless you are sure the HLP is communicating to the MLP and the ROS master\n\n"$RESET
}

# Allowed values
declare -A arg_1

arg_1["arg_app"]="app"
arg_1["arg_interface"]="interface"
arg_1["arg_service"]="service"


declare -A arg_2

arg_2["arg_start"]="start"
arg_2["arg_stop"]="stop"
arg_2["arg_restart"]="restart"
arg_2["arg_hard_stop"]="hard_stop"

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
  printf $BOLDRED"\nFirst argument unknown\n\n"$RESET
  print_help
  exit 1
fi

# Check second argument
arg_2_ok=false
for i in "${arg_2[@]}"
do
  if [ "$i" == "$2" ]; then
    arg_2_ok=true
    break
  fi
done

if [ "$arg_2_ok" == false ]; then
  printf $BOLDRED"\nSecond argument unknown\n\n"$RESET
  print_help
  exit 1
fi

# Put input together
input=$1"_"$2

case "$input" in
  "app_start")
    adb shell am start -n gov.nasa.arc.astrobee.signal_intention_state/.MainActivity
    adb shell am startservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService
    ;;
  "app_stop")
    adb shell am broadcast -a "gov.nasa.arc.astrobee.signal_intention_state.ACTION_STOP"
    adb shell am stopservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService
    ;;
  "app_hard_stop")
    adb shell am force-stop gov.nasa.arc.astrobee.signal_intention_state
    ;;
  "app_restart")
    adb shell am force-stop gov.nasa.arc.astrobee.signal_intention_state
    adb shell am start -n gov.nasa.arc.astrobee.signal_intention_state/.MainActivity
    adb shell am startservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService
    ;;
  "interface_start")
    adb shell am start -n gov.nasa.arc.astrobee.signal_intention_state/.MainActivity
    ;;
  "interface_stop")
    adb shell am broadcast -a "gov.nasa.arc.astrobee.signal_intention_state.ACTION_STOP"
    ;;
  "interface_hard_stop")
    printf "Not implemented yet"
    ;;
  "interface_restart")
    adb shell am broadcast -a "gov.nasa.arc.astrobee.signal_intention_state.ACTION_STOP"
    adb shell am start -n gov.nasa.arc.astrobee.signal_intention_state/.MainActivity
    ;;
  "service_start")
    adb shell am startservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService
    ;;
  "service_stop")
    adb shell am stopservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService
    ;;
  "service_hard_stop")
    printf "Not implemented yet"    
    ;;
  "service_restart")
    adb shell am stopservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService
    adb shell am startservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService
    ;;
esac

