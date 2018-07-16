#!/bin/bash

RED="\033[31;1m"
YELLOW="\033[33;1m"
GREEN="\033[32;1m"
RESET="\033[0m"

PKG_NAME=gov.nasa.arc.irg.astrobee.picoflexx
ACTIVITY=MainActivity

self_path=$(dirname "$0")
source $self_path/common.sh

error() {
  local s; _lookup s 'error'
  echo -e "${RED}$s: $*${RESET}" >&2
}

warn() {
  local s; _lookup s 'warning'
  echo -e "${YELLOW}$s: $*${RESET}" >&2
}

die() { call error "$1"; exit ${2:-1}; }

if [[ $# -lt 1 ]]; then
  call error args
  call echo usage >&2
  exit 1
fi

ADB=$(which adb)
if [[ -z $ADB ]]; then
  die 'no_adb'
fi

# ensure we are connected to the hlp
$ADB devices | grep "10.42.0.28" || \
  $ADB connect 10.42.0.28 | grep "connected to" || \
    die 'cannot_connect'

if [[ "${1,,}" == "start" ]]; then
  $ADB shell am start \
    --es 'PICOFLEXX_ACTION' 'capture_start' \
    $PKG_NAME/.$ACTIVITY || \
      die 'cannot_start'
else
  $ADB shell am start \
    --es 'PICOFLEXX_ACTION' 'capture_stop' \
    $PKG_NAME/.$ACTIVITY || \
      die 'cannot_stop'
fi

local s; _lookup s 'success'
echo -e ${GREEN}${s}!${RESET}

