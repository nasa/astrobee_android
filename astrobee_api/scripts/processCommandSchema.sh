#!/bin/bash

DIR=`pwd`

FFPATH=${DIR%/freeflyer/*}

FSWPATH="$FFPATH/freeflyer"

SCHEMA="$FSWPATH/submodules/common/plans/freeFlyerPlanSchema.json"
SRC_DIR="$FSWPATH/submodules/android/astrobee_api/api/src/main/generated/gov/nasa/arc/astrobee/"

cd "$( dirname "${BASH_SOURCE[0]}" )"
[ -d $SRC_DIR ] || mkdir -p $SRC_DIR
./genCommandTypes.py $SCHEMA $SRC_DIR
./genBaseRobot.py $SCHEMA $SRC_DIR
./genBaseRobotImpl.py $SCHEMA $SRC_DIR
