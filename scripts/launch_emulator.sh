#!/bin/bash

#Detects if script are not running as root.
if [ "$UID" != "0" ]; then
   # $0 is the script itself (or the command used to call it).
   # $* parameters.
   if whereis sudo &>/dev/null; then
     echo "Please type the sudo password for the user $USER. If sudo access is already granted, we will continue"
     sudo -E $0 $*
     exit
   else
     echo "Sudo not found. You will need to run this script as root."
     exit
   fi
fi

# Should we run the network configuration script? Do not modify this variable.
run_net=

# -------------------------------------------- Check flags -------------------------------------------------------------------

# Read arguments and set variables from them
# Flags override environment variables
while getopts :nd:l:h:e: option
do
  case "$option" in
  e)
    EMULATOR=$OPTARG
    ;;
  h)
    HLP_IP=$OPTARG
    ;;
  l)
    LLP_IP=$OPTARG
    ;;
  d)
    AVD=$OPTARG
    ;;
  n)
    run_net=true
    ;;
  *)
    printf "\n > Invalid option received\n"
    exit 1
    ;;
  esac
done

# -------------------------------------------- Check enviromental variables --------------------------------------------------

EMULATOR=${EMULATOR:-$(command -v emulator)}

if [ -z "${EMULATOR}" ]; then
  echo "Please set \$EMULATOR to be location of android emulator, or use flag -e \"your emulator location\""
  exit 1
fi

if [ -z "${AVD}" ]; then
  echo "Please set \$AVD to name of avd to run, or use flag -d \"your AVD Device\""
  exit 1
fi

# -------------------------------------------- Network IP addresses ----------------------------------------------------------

if [ -z "${LLP_IP}" ]; then
  LLP_IP=$(getent hosts llp | awk '{ print $1 }')
  if [ -z "${LLP_IP}" ]; then
    echo "Error getting LLP_IP from HOSTS file. Please set \$LLP_IP to be IP address for ROS master or use flag -l"
    exit 1
  fi

fi

if [ -z "${HLP_IP}" ]; then
  HLP_IP=$(getent hosts hlp | awk '{ print $1 }')
  if [ -z "${HLP_IP}" ]; then
    echo "Error getting HLP_IP from HOSTS file. Please set \$HLP_IP to be IP address for emulator or use flag -h"
    exit 1
  fi
fi

# -------------------------------------------- Set bridge --------------------------------------------------------------------

if ! ip link | grep -q br0; then
  ip link add name br0 type bridge
  ip addr add "$LLP_IP"/24 dev br0
  ip link set dev br0 up
fi

if ! ip link | grep -q tap0; then
  ip tuntap add dev tap0 mode tap user ${USER}
  ip link set dev tap0 master br0
  ip link set dev tap0 up
fi

# -------------------------------------------- Configure emulator network -----------------------------------------------------

if [ ! -z $run_net ]; then
  sudo gnome-terminal -e "./hlp_setup_net.sh -e -h $HLP_IP -l $LLP_IP -w -1 -t 60"
fi

# -------------------------------------------- Start emulator -----------------------------------------------------------------

"$EMULATOR" -avd "${AVD}" -no-snapshot -writable-system -qemu -device virtio-net-pci,netdev=net1 -netdev tap,id=net1,script=no,downscript=no,ifname=tap0

# -------------------------------------------- Destroying bridge --------------------------------------------------------------

ip link delete br0
ip link delete tap0
