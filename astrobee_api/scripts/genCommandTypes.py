#!/usr/bin/env python

"""
Generate command type enum java files from an XPJSON schema.
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

package gov.nasa.arc.astrobee.types;

%(body)s
"""
# END TEMPLATE_MAIN

TEMPLATE_CLASS_BEGIN = """
public enum %(paramId)s {
"""[
    1:-1
]

TEMPLATE_ENUM = """
    %(choiceCodeAllCaps)s("%(choiceCode)s"),
"""[
    1:-1
]

TEMPLATE_ENUM_END = """
    %(choiceCodeAllCaps)s("%(choiceCode)s");

"""[
    1:-1
]

TEMPLATE_CLASS_END = """
    private final String m_value;

    %(paramId)s(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
"""[
    1:-1
]


def getParamContext(param):
    if "." in param.id:
        category, baseId = param.id.split(".", 1)
    else:
        category, baseId = None, param.id
    return {"paramId": xpjsonAstrobee.fixName(baseId)}


def getChoiceContext(choiceCode):
    capChoiceCode = choiceCode
    if choiceCode[0].isdigit():
        capChoiceCode = "r" + choiceCode
    return {
        "choiceCode": choiceCode,
        "choiceCodeAllCaps": xpjsonAstrobee.allCaps(capChoiceCode),
    }


def genParamDecls(param, pathTemplate):
    if not param.choices:
        return

    assert "." in param.id, "ParamSpec without category: %s" % param
    paramCtx = getParamContext(param)
    resultList = []
    resultList.append(TEMPLATE_CLASS_BEGIN % paramCtx + "\n")
    for i, choice in enumerate(param.choices):
        ctx = paramCtx.copy()
        ctx.update(getChoiceContext(choice[0]))
        if (i + 1) == len(param.choices):
            resultList.append(TEMPLATE_ENUM_END % ctx + "\n")
        else:
            resultList.append(TEMPLATE_ENUM % ctx + "\n")
    resultList.append(TEMPLATE_CLASS_END % paramCtx)

    body = "".join(resultList)

    path = pathTemplate % {"paramId": paramCtx["paramId"]}
    with open(path, "w") as outStream:
        outStream.write(TEMPLATE_MAIN % {"body": body})
    logging.info("wrote command types to %s", path)


def genCommandTypes(inSchemaPath, commandTypesPathTemplate):
    schema = xpjsonAstrobee.loadDocument(inSchemaPath)

    paramSpecs = sorted(schema.paramSpecs, key=lambda spec: spec.id)

    for spec in paramSpecs:
        genParamDecls(spec, commandTypesPathTemplate)


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
        "commandTypesPathTemplate",
        help="output Java base robot path",
    )
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG, format="%(message)s")
    genCommandTypes(args.inSchemaPath, args.commandTypesPathTemplate)


if __name__ == "__main__":
    main()
