#!/bin/bash

# This script is an alternative solution for the HLP initialization method. Although there is an official procedure 
# to set up the HLP, this script can be useful to automatize the process for developers and testers.
#
# If you want to run this script on the actual robot for some reason, you would have to do it on the LLP, since it has
# USB connection to the HLP. On the other hand, if you are running this script on a developer VM using a developer board,
# make sure to have USB connection between them.
#
# The sequence of tasks to be performed are described as followed:
# 
# 1. Check if there is a device called as described in "hlp_model" connected over USB to the OS executing this code.
# 2. Check if the intention_state interface is being shown on the HLP, if not, execute it.
# 3. Check if the HLP has the IP set as defined in the "ip_add" variable. If not, set it.
# 4. Check if there is communication over Ethernet between the HLP and the OS running this code.
# 5. Check if the ROS Master is running and wait for it.
# 6. RUn the intention_state service and start listening for ROS messages on the android app in the HLP


# --------------------------------------------------- Global variables -----------------------------------------------

# IMPORTANT: Check HLP Model
hlp_model="MSM8996"

# IMPORTANT: Set the right net config
ip_range="10.42.0"
ip_add="10.42.0.33"
mask="/24"
interface="eth0"
mlp_ip_add="10.42.0.1"

# Timeout for retrying
timeout=120

# --------------------------------------------------- Functions -------------------------------------------------------

function isDeviceOnline {
  local device_present=$(adb devices -l | grep -c $hlp_model)
  
  if [ "$device_present" -eq 1 ]; then
    echo true  
  else
    # Devices are less or more than one. That's an error
    echo false
  fi
}

function setupHlpNetwork {
  local hasCorrectIp=$(adb shell "ifconfig $interface" | grep -c $ip_add)

  if [ "$hasCorrectIp" != 1 ]; then
    adb shell echo "ip link set dev $interface down" \| su
    adb shell echo "ip link set dev $interface up" \| su
    adb shell echo "ip addr add $ip_add$mask dev $interface" \| su
    adb shell echo "ip rule flush" \| su
    adb shell echo "ip rule add pref 32766 from all lookup main" \| su
    adb shell echo "ip rule add pref 32767 from all lookup default" \| su
  else
    echo "IP is already setted"
  fi
}

function checkNetworkConnection {
  local is_link_to_mlp_ok=false
  local is_link_to_hlp_ok=false
  local output

  output=$(ping -c2 $ip_add)
  if [ $? -eq 0 ]; then
    is_link_to_hlp_ok=true
  fi

  output=$(adb shell "ping -c2 $mlp_ip_add")
  output=$(adb shell "echo \$?")
  if [ $output -eq 0 ]; then
    is_link_to_mlp_ok=true
  fi

  echo $is_link_to_hlp_ok && $is_link_to_mlp_ok
}

# -------------------------------------------- Check if HLP is online --------------------------------------------------

isHlpOnline=$(isDeviceOnline)

# Time counter
count=0

# Try to reach the HLP
while [ "$isHlpOnline" == false ]
do
  # Device is offline.
  if [ "$count" -ge "$timeout" ]; then
    echo "Connection timeout. HLP didn't answer"
    exit 1
  fi

  echo "Couldn't connect with ADB Shell. Try replugging USB adapter. Waiting for it..."
  sleep 10
  ((count+=10))
  
  # Check again
  isHlpOnline=$(isDeviceOnline)
done

# Connected
echo "Connected to HLP"

# -------------------------------------------- Check Signal State Activity -----------------------------------------------

# Check signal state activity presence
signal_state_focus=$(adb shell dumpsys activity activities | grep mFocusedActivity | grep -c signal_intention_state)

# If no focus launch/resume eyes activity
if [ $signal_state_focus -eq 0 ]; then
  adb shell am start gov.nasa.arc.astrobee.signal_intention_state/.MainActivity
fi

# ----------------------------------------------- Setup HLP network -------------------------------------------------------

# Check network
setupHlpNetwork

# ---------------------------------------------- Check HLP Network Connectivity ---------------------------------------------

# Check communication

is_net_link_ok=$(checkNetworkConnection)

count=0;

while [ "$is_net_link_ok" == false ]
do
  # Reconfigure net and test again.

  if [ $count -ge $timeout ]; then
    echo "Timeout. It did't work. Connection Test: Failed"
    exit 1
  fi

  echo "Reconfiguring network and testing again"
  setupHlpNetwork
  sleep 10
  ((count+=10))
  is_net_link_ok=$(checkNetworkConnection)
done

echo "Connection Test: Success!"

# ---------------------------------------------- Check ROS Connection -------------------------------------------------------

# Check ROS
check_command_output=$(rostopic list)

count=0;

while [ -z $check_command_output ]
do
  # Wait for ROS master.
  if [ $count -ge $timeout ]; then
    echo "Connection timeout. Restart the HLP"
    exit 1
  fi

  echo "Waiting for ROS Master..."
  sleep 5
  ((count+=5))
  check_command_output=$(rostopic list)
done

# ------------------------------------------------- Run startup services on HLP ------------------------------------------------

# Start astrobee ayes service app
adb shell am startservice gov.nasa.arc.astrobee.signal_intention_state/.SignalStateService

# Start Guest Science Manager
adb shell am startservice gov.nasa.arc.astrobee.android.gs.manager/.ManagerService

echo "DONE"
