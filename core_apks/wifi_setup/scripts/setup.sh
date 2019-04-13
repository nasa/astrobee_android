#!/bin/sh

PKG="gov.nasa.arc.irg.astrobee.wifisetup"
SVC="SetupService"
TAG="$SVC"

die() {
  echo "${@}" >&2; exit 1
}

action="${1:-help}"

case "$action" in
  help)
    printf "Usage: %s { CMD | help }\n" "$(basename "$0")" >&2
    printf "Possible commands:\n" >&2
    printf "  clear     - clear existing networks on device\n" >&2
    printf "  delete ID - delete networks with ID on device\n" >&2
    printf "  list      - list existing networks on device\n" >&2
    printf "  load FILE - add networks from FILE to device\n" >&2
    exit 0
    ;;
  load)
    [ -n "$2" ] || die "no config given"
    [ -r "$2" ] || die "config $2 does not exist"
    adb push "$2" "/sdcard/wifi_config.json" >/dev/null \
      || die "unable to transfer config"
    set -- "--es" "$PKG.EXTRA_PATH" "/sdcard/wifi_config.json"
    ;;
  delete)
    [ -n "$2" ] || die "must give an id to delete"
    [ "$2" -ge 0 2>/dev/null ] || die "id must be numeric"
    set -- "--ei" "$PKG.EXTRA_NETID" "$2"
    ;;
  *)
    set --
    ;;
esac

now="$(adb exec-out date '+%s')"

adb exec-out am startservice \
  -a "$PKG.$action" "${@}" \
  "$PKG/.$SVC" >/dev/null || exit 1
exec adb logcat -b main -v color -v brief -t "$now.0" -d -s "$TAG":I
