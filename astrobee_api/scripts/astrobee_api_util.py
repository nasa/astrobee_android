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

"""
Factor out boilerplate of PYTHONPATH hack.
"""

import os
import sys

up = os.path.dirname
root = os.getenv("SOURCE_PATH")
if root is None:
    root = up(up(up(up(up(os.path.abspath(__file__))))))

# For development purposes, allow xgds_planner2 to be checked out within the
# main astrobee repo under astrobee/commands alongside
# freeFlyerPlanSchema.json. (More typically, it would be installed by pip in
# the default PYTHONPATH, in which case this entry won't matter.)
sys.path.insert(0, os.path.join(root, "astrobee", "commands", "xgds_planner2"))

# xpjsonAstrobee.py is found in the main astrobee repo under scripts/build.
sys.path.insert(0, os.path.join(root, "scripts", "build"))
