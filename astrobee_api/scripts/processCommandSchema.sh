#!/bin/bash
#
# Copyright (c) 2017, United States Government, as represented by the
# Administrator of the National Aeronautics and Space Administration.
#
# All rights reserved.
#
# The Astrobee platform is licensed under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with the
# License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

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
