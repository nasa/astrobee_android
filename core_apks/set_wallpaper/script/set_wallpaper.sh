#!/bin/sh

PKG="gov.nasa.arc.astrobee.set_wallpaper"
SERVICE="SetupService"

ADB=$(which adb)

$ADB shell am startservice "$PKG/.$SERVICE"
