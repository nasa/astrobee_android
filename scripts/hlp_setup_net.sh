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

DEF_LLP_IP_DEV="10.42.0.34"
DEF_HLP_IP_DEV="10.42.0.36"
DEF_INTERFACE_DEV="eth0"

DEF_LLP_IP_EMU="10.42.0.31"
DEF_HLP_IP_EMU="10.42.0.33"
DEF_INTERFACE_EMU="eth1"

DEF_MASK="/24"

# You may edit these values if you happen to use this script a lot
llp_ip=$DEF_LLP_IP_DEV
hlp_ip=$DEF_HLP_IP_DEV
hlp_ip_mask=$DEF_MASK
hlp_interface=$DEF_INTERFACE_DEV

# Timeout for retrying (seconds). You may edit as well
timeout=30

# Initial wait
wait=

# Do NOT modify this variable
adb_device=

declare -A devices

# --------------------------------------------------- Functions -------------------------------------------------------

function isPackageInstalled {
  echo $(dpkg-query -W -f='${Status}' $1 2>/dev/null | grep -c "ok installed")
}

function isDeviceOnline {
  local device_present=$(adb devices -l | grep -c $adb_device)
  
  if [ "$device_present" -eq 1 ]; then
    echo true  
  else
    # Devices are less or more than one. That's an error
    echo false
  fi
}

function findDevices {
  local flag=false

  while read -r device type
  do
    if ! $flag; then
        flag=true
        continue
    fi

    if [ "$device" != "" ]; then
      devices[$device]=$type
    fi
    
  done < <(adb devices)
}

function isHlpAddressSet {
  local result=$(adb -s $adb_device shell "ifconfig $hlp_interface" | grep -c $hlp_ip)
  [[ $result = 1 ]] && echo true || echo false
}

function setupHlpNetwork {
  if [ $(isHlpAddressSet) = false ]; then
    adb -s $adb_device shell echo "ip link set dev $hlp_interface down" \| su
    adb -s $adb_device shell echo "ip addr flush dev $hlp_interface" \| su
    adb -s $adb_device shell echo "ip link set dev $hlp_interface up" \| su
    adb -s $adb_device shell echo "ip addr add $hlp_ip$hlp_ip_mask dev $hlp_interface" \| su
    adb -s $adb_device shell echo "ip rule flush" \| su
    adb -s $adb_device shell echo "ip rule add pref 32766 from all lookup main" \| su
    adb -s $adb_device shell echo "ip rule add pref 32767 from all lookup default" \| su
  else
    echo -e "\t- IP is already set"
  fi
}

function checkNetworkConnection {
  local is_link_to_mlp_ok=false
  local is_link_to_hlp_ok=false
  local output

  output=$(ping -c1 $hlp_ip)
  if [ $? -eq 0 ]; then
    is_link_to_hlp_ok=true
  fi

  output=$(adb -s $adb_device shell "ping -c 1 $llp_ip &> /dev/null && echo 0 || echo 1")
  if [ $output -eq 0 ]; then
    is_link_to_mlp_ok=true
  fi

  $($is_link_to_hlp_ok && $is_link_to_mlp_ok) && echo true || echo false
}

function isBootCompleted {
  local isBooting=$(adb -s $adb_device shell getprop init.svc.bootanim)
  if [ "$isBooting" = "stopped" ]; then
    echo 1
  else
    echo 0
  fi
}

# -------------------------------------------- Check dependencies --------------------------------------------------

# Ensure ADB is installed
if [ $(isPackageInstalled "adb") = 0 ] && [ $(isPackageInstalled "android-tools-adb") = 0 ]; then
  printf $RED"\nADB is not installed. Please install it and run the script again\n\n"$RESET
  adb version
  read -p "Press Return to exit"
  exit 1
fi

# -------------------------------------------- Parse arguments --------------------------------------------------

# TODO Add validation

# Read arguments and set variables from them
while getopts :ebh:l:i:m:d:w:t: option
do
  case "$option" in
  e) 
    llp_ip=$DEF_LLP_IP_EMU
    hlp_ip=$DEF_HLP_IP_EMU
    hlp_ip_mask=$DEF_MASK
    hlp_interface=$DEF_INTERFACE_EMU
    ;;
  b) 
    llp_ip=$DEF_LLP_IP_DEV
    hlp_ip=$DEF_HLP_IP_DEV
    hlp_ip_mask=$DEF_MASK
    hlp_interface=$DEF_INTERFACE_DEV
    ;;
  h) 
    hlp_ip=$OPTARG
    ;;
  l)
    llp_ip=$OPTARG
    ;;
  i)
    hlp_interface=$OPTARG
    ;;
  m)
    hlp_ip_mask=$OPTARG
    ;;
  d)
    adb_device=$OPTARG
    ;;
  t)
    timeout=$OPTARG
    ;;
  w)
    wait=$OPTARG
    ;;
  *)
    printf $BOLDRED"\n > Invalid option received\n"$RESET
    read -p "Press Return to exit"
    exit 1
    ;;
  esac
done

printf $BOLDWHITE"\n This script will set the Android-Ubuntu network for you\n\n"$RESET

# -------------------------------------------- Check HLP is online --------------------------------------------------

# Ensure ADB server is running
adb start-server

# Check for ADB devices
echo -e "\n > Searching devices..."
findDevices

# Checking device presence
counter=0
while [ ${#devices[@]} -eq 0 ]
do
  if [ $counter -gt $timeout ]; then
    # Timeout
    printf $BOLDRED"\n > Timeout searching for devices. Shutting down...\n\n"$RESET
    read -p "Press Return to exit"
    exit 1
  fi

  # Retring...
  printf $BOLDYELLOW"\t- No device found. We will try again...\n"$RESET
  sleep 5
  ((counter+=5))
  findDevices
done

# Ensure we only access one single ADB device
if [ ${#devices[@]} -gt 1 ]; then
  # More than 1 device found
  printf $BOLDYELLOW"\n > MORE than one device found. Please choose one the following and run the script again using -d option\n"$RESET
  echo -e "\t- ${!devices[@]}"
  read -p "Press Return to exit"
  exit 1
elif [ "${devices[@]}" = "no permissions" ]; then
  printf $BOLDYELLOW"\n > We found one device. But it DOES NOT have sufficient PERMISSIONS\n\n"$RESET
  read -p "Press Return to exit"
  exit 1
else
  printf $BOLDGREEN"\n > We found one device. We will use:\n"$RESET
  adb_device="${!devices[@]}"
  echo -e "\t- $adb_device"
  sleep 1
fi

if [ ! -z $wait ]; then
  waiting_msg=$([[ $wait -eq -1 ]] && echo "until device is fully boot" || echo "$wait seconds or until device is fully boot")
  printf $YELLOW"\n > Waiting for the device to fully boot...\n > We will wait $waiting_msg\n"$RESET
  counter=0
  while [[ $(isBootCompleted) -eq 0 && ( $wait -eq -1 || $counter -le $wait ) ]]
  do
    sleep 1
    ((counter+=1))
  done
  echo -e "\n > Waiting completed"
fi

# Safety wait
sleep 5

# ----------------------------------------------- Setup HLP network -------------------------------------------------------

# TODO Manage exceptions

echo -e "\n > Setting HLP network..."
setupHlpNetwork

# ---------------------------------------------- Double Check HLP Network Connectivity ---------------------------------------------

# Check ADB commands went OK
echo -e "\n > Checking HLP IP settings..."
count=0;
while [ $(isHlpAddressSet) = false ]
do
  if [ $count -ge $timeout ]; then
    printf $BOLDRED"\n > Too many failed attempts. Shutting down...\n"
    printf "\n > RESULT: Fail - Unable to set configuration through ADB"$RESET
    read -p "Press Return to exit"
    exit 1
  fi
  printf $BOLDYELLOW"\t- A problem ocurred. Retrying...\n"$RESET
  setupHlpNetwork
  sleep 5
  ((count+=5))
done

printf $BOLDGREEN"\n > HLP IP address set successfully\n"$RESET

# Check network communication
echo -e "\n > Checking HLP-LLP network link..."
count=0;

while [ $(checkNetworkConnection) = false ]
do
  # Reconfigure net and test again.
  if [ $count -ge $timeout ]; then
    printf $RED"\n > Timeout. Connection Test: Failed\n"$RESET
    printf $BOLDYELLOW"\n > RESULT: Partial fail - HLP has configuration set, but we were unable to ping HLP/LLP. Is your Ethernet adapter ready?\n\n"$RESET
    read -p "Press Return to exit"
    exit 1
  fi
  printf $BOLDYELLOW"\t- Reconfiguring network and testing again\n"$RESET
  setupHlpNetwork
  sleep 5
  ((count+=5))
done

printf $BOLDGREEN"\n > Connection Test: Success!\n"
printf "\n > RESULT: Success!\n\n"$RESET
read -p "Press Return to exit"

