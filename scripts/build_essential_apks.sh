#!/bin/sh

dir=$(dirname "$(readlink -f "$0")")
. "$dir"/common.sh || { echo >&2 "something is really messed up"; exit 1; }

user=${NDC_USERNAME:+${NDC_USERNAME}@}

key_alias=""
password=""
build_type="debug"

if [ "$#" = 2 ]; then
  if [ ! -d "$wd/keys" ]; then
    mkdir -p "$wd/keys"
  fi

  if [ ! -e "$wd/keys/astrobee_keystore.jks" ]; then
     scp "$user@volar:/home/p-free-flyer/free-flyer/FSW/keys/astrobee_keystore.jks" \
       "$wd/keys" || die "Unable to copy over key store from volar."
  fi

  key_alias=$1
  password=$2
  build_type="signed"
fi

apks_dir="$wd/out/android/apks/$build_type/"
core_apks_dir="$dir/../core_apks/"
sdk_dir="$wd/toolchain/sdk"

# Args for function are apk location, apk package name, apk human readable name.
build_apk () {
  if [ -d "$1" ]; then
    cd "$1"
    ./gradlew build -x lint || die "Unable to build $3."
    if [ "$build_type" = "signed" ]; then
      # Check to make sure the correct build tools were installed
      if [ ! -e "$sdk_dir/build-tools/25.0.3/zipalign" ]; then
        die "Unable to find zipalign in sdk build tools."
      fi

      # Remove signed apk since zipalign errors if the signed apk already exists
      if [ -e "$apks_dir/$2" ]; then
        rm "$apks_dir/$2"
      fi

      if [ -e "app/build/outputs/apk/app-release-unsigned.apk" ]; then
        jarsigner -keystore "$wd/keys/astrobee_keystore.jks" -storepass \
          "$password" "app/build/outputs/apk/app-release-unsigned.apk" \
          "$key_alias" || die "Unable to sign $3 apk."

        "$sdk_dir/build-tools/25.0.3/zipalign" -v 4 \
          "app/build/outputs/apk/app-release-unsigned.apk" "$apks_dir/$2" || \
          die "Unable to zigalign apk."
      elif [ -e "activity/build/outputs/apk/activity-release-unsigned.apk" ]; then
        jarsigner -keystore "$wd/keys/astrobee_keystore.jks" -storepass \
          "$password" \
          "activity/build/outputs/apk/activity-release-unsigned.apk" \
          "$key_alias" || die "Unable to sign $3 apk."

        "$sdk_dir/build-tools/25.0.3/zipalign" -v 4 \
          "activity/build/outputs/apk/activity-release-unsigned.apk" \
          "$apks_dir/$2" || die "Unable to zipalign apk."
      else
        err "Unable to find built apk for $3"
        return 1
      fi
    else
      if [ -e "app/build/outputs/apk/app-debug.apk" ]; then
        mv "app/build/outputs/apk/app-debug.apk" "$apks_dir/$2"
      elif [ -e "activity/build/outputs/apk/activity-debug.apk" ]; then
        mv "activity/build/outputs/apk/activity-debug.apk" \
          "$apks_dir/$2"
      else
        err "Unable to find built apk for $3"
        return 1
      fi
    fi
  else
    warn "$3 project folder doesn't exist or isn't in the expected location."
    return 1
  fi
  return 0
}

"$dir/fetch_sdk.sh" || die "Unable to fetch sdk."

export ANDROID_HOME="$sdk_dir"

if [ ! -d "$apks_dir" ]; then
  mkdir -p "$apks_dir"
fi

not_built=""

info "Building CPU monitor." 
build_apk "$core_apks_dir/cpu_monitor" "gov.nasa.arc.astrobee.cpu_monitor.apk" \
  "CPU monitor" || not_built="$not_built CPU Monitor "

info "Building disk monitor."
build_apk "$core_apks_dir/disk_monitor" \
  "gov.nasa.arc.astrobee.disk_monitor.apk" "disk monitor" || \
  not_built="$not_built Disk Monitor "

info "Building guest science manager."
build_apk "$core_apks_dir/guest_science_manager" \
  "gov.nasa.arc.astrobee.android.gs.manager.apk" "guest science manager" || \
  not_built="$not_built Guest Science Monitor "

info "Building mic test."
build_apk "$core_apks_dir/mic_test" "gov.nasa.arc.irg.astrobee.mictest.apk" \
  "Mic test" || not_built="$not_built Mic Test "

info "Building signal intention state."
build_apk "$core_apks_dir/signal_intention_state" \
  "gov.nasa.arc.astrobee.signal_intention_state.apk" "Signal intention state" \
  || not_built="$not_built Signal Intention State "

info "Building sci cam."
build_apk "$core_apks_dir/scicam" \
  "gov.nasa.arc.astrobee.android.scicam.apk" "Sci cam" || \
  not_built="$not_built Science Camera "

info "Building set wallpaper."
build_apk "$core_apks_dir/set_wallpaper" \
  "gov.nasa.arc.astrobee.set_wallpaper.apk" "Set wallpaper" || \
  not_built="$not_built Set Wallpaper "

info "Building Wifi setup helper."
build_apk "$core_apks_dir/wifi_setup" \
  "gov.nasa.arc.irg.astrobee.wifisetup.apk" "Wifi setup" || \
  not_built="$not_built Wifi Setup Helper "

info "Building battery monitor."
build_apk "$dir/../gs_examples/battery_monitor" \
  "gov.nasa.arc.irg.astrobee.battery_monitor.apk" "Battery monitor" || \
  not_built="$not_built Battery Monitor "

[ -n "$not_built" ] && warn "APKs not built: $not_built"
