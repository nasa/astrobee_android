#!/usr/bin/env python

"""
Generate base robot java classes/files from an XPJSON schema.
"""

import argparse
import logging
import os
import sys

# hack to ensure xgds_planner2 submodule is at head of PYTHONPATH
astrobee_root = os.getenv(
    "SOURCE_PATH",
    (
        os.path.dirname(
            os.path.dirname(
                os.path.dirname(
                    os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
                )
            )
        )
    ),
)
sys.path.insert(0, os.path.join(astrobee_root, "astrobee", "commands", "xgds_planner2"))
sys.path.insert(0, os.path.join(astrobee_root, "scripts", "build"))

import xpjsonAstrobee

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

public abstract class BaseRobotImpl extends AbstractRobot implements BaseRobot {

%(body)s}
"""
# END TEMPLATE_MAIN

TEMPLATE_FUNC_BEG = """
    @Override
    public PendingResult %(commandId)s(
"""[
    1:-1
]

TEMPLATE_FUNC_ARGS = """
%(paramValueType)s %(paramId)s, 
"""[
    1:-1
]

TEMPLATE_FUNC_ARGS_END = """
%(paramValueType)s %(paramId)s) {
"""[
    1:-1
]

TEMPLATE_FUNC_BODY_BEG = """
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("%(commandId)s")
"""[
    1:-1
]

TEMPLATE_FUNC_BODY_ARG = """
                .addArgument("%(paramId)s", %(paramId)s)
"""[
    1:-1
]

TEMPLATE_FUNC_END_ARG = """
                .addArgument("%(paramId)s", %(paramId)s);
        return publish(builder.build());
    }
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


def getParamContext(param):
    if "." in param.id:
        category, baseId = param.id.split(".", 1)
    else:
        category, baseId = None, param.id

    parent = getattr(param, "parent", None)
    valueType = ""
    if parent is None:
        valueType = param.valueType
        if valueType == "long":
            valueType = "int"
        elif valueType == "long long":
            valueType = "long"
        elif valueType == "string":
            valueType = "String"
        elif valueType == "array[3].double":
            valueType = "Vec3d"
        elif valueType == "array[9].float":
            valueType = "Mat33f"
        elif valueType == "quaternion":
            valueType = "Quaternion"
    else:
        if "." in parent:
            category, valueType = parent.split(".", 1)
        else:
            category, valueType = None, parent

    notes = param.notes
    if notes is None:
        notes = ""

    result = {
        "paramId": xpjsonAstrobee.fixName(baseId),
        "paramNotes": notes,
        "paramValueType": valueType,
    }
    return result


def genCommandSpecDecls(cmd):
    resultList = []
    bodyList = []

    commandCtx = getCommandContext(cmd)

    resultList.append(TEMPLATE_FUNC_BEG % commandCtx)
    bodyList.append(TEMPLATE_FUNC_BODY_BEG % commandCtx)

    if len(cmd.params) > 0:
        bodyList.append("\n")
        for i, param in enumerate(cmd.params):
            ctx = commandCtx.copy()
            ctx.update(getParamContext(param))
            if (i + 1) == len(cmd.params):
                resultList.append(TEMPLATE_FUNC_ARGS_END % ctx)
                bodyList.append(TEMPLATE_FUNC_END_ARG % ctx + "\n\n")
            else:
                resultList.append(TEMPLATE_FUNC_ARGS % ctx)
                bodyList.append(TEMPLATE_FUNC_BODY_ARG % ctx + "\n")
    else:
        resultList.append(") {")
        bodyList.append(";\n")
        bodyList.append("        return publish(builder.build());\n    }\n\n")

    resultList.append("\n")
    # clean up function definition
    result = "".join(resultList)
    if len(result) > 94:
        resultList = []
        resultSplit = result.split("(")
        argsSplit = resultSplit[1].split(",")
        resultList.append(resultSplit[0] + "(" + argsSplit[0] + ",\n")
        # Subtract size of overrided
        spaces = len(resultSplit[0]) - 14
        for i in range(1, len(argsSplit)):
            arg = ""
            for j in range(spaces):
                arg += " "

            arg += argsSplit[i]
            if (i + 1) != len(argsSplit):
                arg += ",\n"

            resultList.append(arg)

    return "".join(resultList + bodyList)


def genBaseRobotImpl(inSchemaPath, baseRobotImplPath):
    schema = xpjsonAstrobee.loadDocument(inSchemaPath)

    commandSpecs = sorted(schema.commandSpecs, key=lambda spec: spec.id)

    commandDecls = [genCommandSpecDecls(spec) for spec in commandSpecs]

    body = "".join(commandDecls)

    with open(baseRobotImplPath, "w") as outStream:
        outStream.write(TEMPLATE_MAIN % {"body": body})
    logging.info("wrote base robot implementation to %s", baseRobotImplPath)


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
        "baseRobotImplPath",
        help="output Java base robot implementation path",
    )
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG, format="%(message)s")
    genBaseRobotImpl(args.inSchemaPath, args.baseRobotImplPath)


if __name__ == "__main__":
    main()
