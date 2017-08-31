#!/bin/bash

PKG_NAME=gov.nasa.arc.irg.astrobee.picoflexx
ACTIVITY=MainActivity

die() {
  echo "$@" >&2
  exit -1
}

ADB=$(which adb)
if [[ -z $ADB ]]; then
	die "error: adb not found"
fi

# ensure we are connected to the hlp
$ADB devices | grep "10.42.0.28" || \
  $ADB connect 10.42.0.28 | grep "connected to" || \
    die "error: unable to connect to HLP"

function stop_app() {
  $ADB shell am force-stop $PKG_NAME || \
	die "error: unable to kill child"
  exit 0
}

# start the app
$ADB shell am start \
  --es 'ROS_MASTER_URI' "${ROS_MASTER_URI}" \
  $PKG_NAME/.$ACTIVITY || \
    die "error: unable to start ros node"

# 긑날때 자살한다
trap stop_app SIGINT SIGTERM

# do nothing waiting for the node to be stopped
while true; do
	sleep 60
done

