#!/bin/bash

cd "$( dirname "${BASH_SOURCE[0]}" )"

DIR=`pwd`

SRC_DIR="${DIR%/astrobee_api/*}/astrobee_api/api/src/main/generated/gov/nasa/arc/astrobee"

if [[ -z "${SOURCE_PATH}" ]]; then
  SOURCE_PATH="${DIR%/submodules/*}"
fi

SCHEMA="$SOURCE_PATH/astrobee/commands/freeFlyerPlanSchema.json"

[ -d $SRC_DIR ] || mkdir -p $SRC_DIR
./genCommandTypes.py $SCHEMA "$SRC_DIR/types/{paramId}.java"
./genBaseRobot.py $SCHEMA $SRC_DIR/internal/BaseRobot.java
./genBaseRobotImpl.py $SCHEMA $SRC_DIR/internal/BaseRobotImpl.java
