#!/usr/bin/env python

"""
A library and command-line tool for generating command type enum java files from
an XPJSON schema.
"""

import os
import sys
import re
import logging

# hack to ensure xgds_planner2 submodule is at head of PYTHONPATH
astrobee_root = os.getenv('SOURCE_PATH', (os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))))
sys.path.insert(0, os.path.join(astrobee_root, 'astrobee', 'commands', 'xgds_planner2'))
sys.path.insert(0, os.path.join(astrobee_root, 'scripts', 'build'))

from xgds_planner2 import xpjson
import xpjsonAstrobee


TEMPLATE_MAIN = '''// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

%(body)s
'''
# END TEMPLATE_MAIN

TEMPLATE_CLASS_BEGIN = '''
public enum %(paramId)s {
'''[1:-1]

TEMPLATE_ENUM = '''
    %(choiceCodeAllCaps)s("%(choiceCode)s"),
'''[1:-1]

TEMPLATE_ENUM_END = '''
    %(choiceCodeAllCaps)s("%(choiceCode)s");

'''[1:-1]

TEMPLATE_CLASS_END = '''
    private final String m_value;

    %(paramId)s(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
'''[1:-1]


def getParamContext(param):
    if '.' in param.id:
        category, baseId = param.id.split('.', 1)
    else:
        category, baseId = None, param.id
    return {
        'paramId': xpjsonAstrobee.fixName(baseId)
    }

def getChoiceContext(choiceCode):
    capChoiceCode = choiceCode
    if choiceCode[0] >= '0' and choiceCode[0] <= '9':
        capChoiceCode = 'r' + choiceCode
    return {
        'choiceCode': choiceCode,
        'choiceCodeAllCaps': xpjsonAstrobee.allCaps(capChoiceCode),
    }


def genParamDecls(param, path):
    if not param.choices:
        return

    assert '.' in param.id, 'ParamSpec without category: %s' % param
    paramCtx = getParamContext(param)
    resultList = []
    resultList.append(TEMPLATE_CLASS_BEGIN % paramCtx + '\n')
    filename = path + '/types/' + paramCtx['paramId'] + '.java'
    for i in range(len(param.choices)):
        ctx = paramCtx.copy()
        ctx.update(getChoiceContext(param.choices[i][0]))
        if (i + 1) == len(param.choices): 
            resultList.append(TEMPLATE_ENUM_END % ctx + '\n')
        else: 
            resultList.append(TEMPLATE_ENUM % ctx + '\n')
    resultList.append(TEMPLATE_CLASS_END % paramCtx)

    body = ''.join(resultList)

    with open(filename, 'w') as outStream:
        outStream.write(TEMPLATE_MAIN % {'body': body})

    return


def genCommandTypes(inSchemaPath, commandTypesPath):
    schema = xpjsonAstrobee.loadDocument(inSchemaPath)

    paramSpecs = sorted(schema.paramSpecs, key=lambda spec: spec.id)


    for spec in paramSpecs:
        genParamDecls(spec, commandTypesPath)

    logging.info('wrote command types to %s', commandTypesPath)


def main():
    import optparse
    parser = optparse.OptionParser('usage: %prog <inSchemaPath> [commandTypesPath]\n\n' + __doc__.strip())
    opts, args = parser.parse_args()
    if len(args) == 2:
        inSchemaPath, commandTypesPath = args
    else:
        parser.error('expected 2 args')
    logging.basicConfig(level=logging.DEBUG, format='%(message)s')
    genCommandTypes(inSchemaPath, commandTypesPath)


if __name__ == '__main__':
    main()
