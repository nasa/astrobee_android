#!/bin/sh

set -e

ip addr add 10.42.0.36/24 dev eth1
ip link set dev eth1 up

ip rule flush
ip rule add pref 32766 from all lookup main
ip rule add pref 32767 from all lookup default
