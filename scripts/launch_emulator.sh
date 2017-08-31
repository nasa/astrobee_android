#!/bin/sh

EMULATOR=${EMULATOR:-$(command -v emulator)}

if [ -z "${EMULATOR}" ]; then
  echo "Please set \$EMULATOR to be location of andorid emulator"
  exit 1
fi

if [ -z "${AVD}" ]; then
  echo "Please set \$AVD to name of avd to run"
  exit 1
fi

if ! ip link | grep -q br0; then
  sudo ip link add name br0 type bridge
  sudo ip addr add 10.0.3.1/24 dev br0
  sudo ip link set dev br0 up
fi

if ! ip link | grep -q tap0; then
  sudo ip tuntap add dev tap0 mode tap user ${USER}
  sudo ip link set dev tap0 master br0
  sudo ip link set dev tap0 up
fi


"$EMULATOR" -avd "${AVD}" -qemu -device virtio-net-pci,netdev=net1 \
  -netdev tap,id=net1,script=no,downscript=no,ifname=tap0
