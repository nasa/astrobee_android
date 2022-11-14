#!/usr/bin/env python
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
Generate base robot java classes/files from an XPJSON schema.
"""

import argparse
import logging

# modify PYTHONPATH to enable xpjsonAstrobee import
import astrobee_api_util

# isort: split

import xpjsonAstrobee

import genBaseRobotImpl

TEMPLATE_MAIN = """// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.internal;

import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Vec3d;
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.CameraMode;
import gov.nasa.arc.astrobee.types.CameraName;
import gov.nasa.arc.astrobee.types.CameraResolution;
import gov.nasa.arc.astrobee.types.FlashlightLocation;
import gov.nasa.arc.astrobee.types.FlightMode;
import gov.nasa.arc.astrobee.types.LocalizationMode;
import gov.nasa.arc.astrobee.types.PlannerType;
import gov.nasa.arc.astrobee.types.PoweredComponent;
import gov.nasa.arc.astrobee.types.TelemetryType;
import gov.nasa.arc.astrobee.Robot;

public interface BaseRobot {

%(body)s}
"""
# END TEMPLATE_MAIN

TEMPLATE_NOTES_PARAM = """
     * @param %(paramId)s%(paramNotes)s\n
"""[
    1:-1
]

TEMPLATE_NOTES_END = """
     * @return PendingResult of this command
     */
"""[
    1:-1
]

TEMPLATE_FUNC_DECL_BEGIN = """
    PendingResult %(commandId)s(
"""[
    1:-1
]

TEMPLATE_FUNC_ARGS = """
%(paramValueType)s %(paramId)s, 
"""[
    1:-1
]

TEMPLATE_FUNC_ARGS_END = """
%(paramValueType)s %(paramId)s);
"""[
    1:-1
]


def getCommandContext(cmd):
    assert "." in cmd.id, "CommandSpec without category: %s" % cmd
    category, baseId = cmd.id.split(".", 1)
    return {
        "commandId": baseId,
        "commandIdAllCaps": xpjsonAstrobee.allCaps(baseId),
        "commandCategoryUpper": category.upper(),
    }


def getCommandComments(cmd):
    notes = cmd.notes
    if notes is None:
        notes = ""

    javaNotes = getattr(cmd, "javaNotes", "")

    temp = "    /**\n"
    if notes != "":
        temp += "     * " + notes + "\n"
        temp += "     *\n"

    if javaNotes != "":
        temp += "     * " + javaNotes + "\n"
        temp += "     *\n"

    return temp


def fixCommentLine(commentLine):
    commentLine += "\n"
    if len(commentLine) > 80:
        fixedComment = ""
        line = "    "
        beginLine = "     * "
        paramLoc = commentLine.find("@param")
        if paramLoc != -1:
            paramLoc += 7
            spaces = 7
            while paramLoc < len(commentLine) and commentLine[paramLoc] != " ":
                paramLoc += 1
                spaces += 1
            spaces += 1
            beginLine += " " * spaces

        splitLine = commentLine.split(" ")
        for word in splitLine:
            if (len(word) + len(line)) > 78:
                fixedComment += line
                fixedComment += "\n"
                line = beginLine + word
            else:
                if word != "":
                    line += " "
                    line += word

        fixedComment += line
        commentLine = fixedComment

    return commentLine


def genCommandSpecDecls(cmd):
    commentsList = []
    resultList = []

    commandCtx = getCommandContext(cmd)

    commentsList.append(getCommandComments(cmd))

    resultList.append(TEMPLATE_FUNC_DECL_BEGIN % commandCtx)

    if len(cmd.params) > 0:
        for i, param in enumerate(cmd.params):
            ctx = commandCtx.copy()
            ctx.update(genBaseRobotImpl.getParamContext(param))
            commentsList.append(TEMPLATE_NOTES_PARAM % ctx)
            if (i + 1) == len(cmd.params):
                resultList.append(TEMPLATE_FUNC_ARGS_END % ctx)
            else:
                resultList.append(TEMPLATE_FUNC_ARGS % ctx)
    else:
        resultList.append(");")

    resultList.append("\n\n")
    # clean up function definition
    result = "".join(resultList)
    if len(result) > 80:
        resultList = []
        resultSplit = result.split("(")
        argsSplit = resultSplit[1].split(",")
        resultList.append(resultSplit[0] + "(" + argsSplit[0] + ",\n")
        spaces = len(resultSplit[0])
        for i in range(1, len(argsSplit)):
            arg = " " * spaces
            arg += argsSplit[i]
            if (i + 1) != len(argsSplit):
                arg += ",\n"

            resultList.append(arg)

    commentsList.append(TEMPLATE_NOTES_END)
    # clean up comments
    comments = "".join(commentsList)
    commentLines = comments.split("\n")
    commentLines = [fixCommentLine(c) for c in commentLines]
    return "".join(commentLines + resultList)


def genBaseRobot(inSchemaPath, baseRobotPath):
    schema = xpjsonAstrobee.loadDocument(inSchemaPath)

    commandSpecs = sorted(schema.commandSpecs, key=lambda spec: spec.id)

    commandDecls = [genCommandSpecDecls(spec) for spec in commandSpecs]

    body = "".join(commandDecls)

    with open(baseRobotPath, "w") as outStream:
        outStream.write(TEMPLATE_MAIN % {"body": body})
    logging.info("wrote base robot implementation to %s", baseRobotPath)


class CustomFormatter(
    argparse.RawDescriptionHelpFormatter, argparse.ArgumentDefaultsHelpFormatter
):
    pass


def main():
    parser = argparse.ArgumentParser(
        description=__doc__ + "\n\n",
        formatter_class=CustomFormatter,
    )
    parser.add_argument(
        "inSchemaPath",
        help="input XPJSON schema path",
    )
    parser.add_argument(
        "baseRobotPath",
        help="output Java base robot path",
    )
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG, format="%(message)s")
    genBaseRobot(args.inSchemaPath, args.baseRobotPath)


if __name__ == "__main__":
    main()
