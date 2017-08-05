##############################################################################
#  Copyright (c) 2016 Intel Corporation
# 
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##############################################################################
#    File Abstract: 
#       A little utility to do the versioning of my python app
#
##############################################################################

import os
import sys
import argparse
import re
import datetime

verString = "__version__"

def extant_file(x,args):
    """
    'Type' for argparse - checks that file exists but does not open.
    """
    if not os.path.exists(x):
        raise argparse.ArgumentError(args,x + ' does not exist')


def GenerateVersionString():
    now = datetime.datetime.now()

    year = str(now.year)[2:].zfill(2)
    month = str(now.month).zfill(2)
    day = str(now.day).zfill(2)

    return year +"." + month + "." + day

def GenerateBuildNumber(strCurrent):
    strCurrent = strCurrent.lower()
    if not "build " in strCurrent:
        return "Build 100"
    try:
        index = strCurrent.find("build")
        num = int(re.search(r'\d+', strCurrent[index:]).group())
        return "Build " + str(num+1)
    except:
        return "Build 100"


def main():
    parser = argparse.ArgumentParser(description='Version Tool')
    parser.add_argument("-i","--input",dest='argFilename',help='specifies input file',required=True,type=str)
    parser.add_argument("-p","--prefix",dest='argPrefix', help="Prefix for version string (WIP,Beta,etc.)",type=str)

    
    try:
        args = parser.parse_args()
   
    except:
        return

    if not os.path.exists(args.argFilename):
        parser.error(args.argFilename + ' does not exist')
        return

    inFile = open(args.argFilename,"rt")
    lines=[]
    for line in inFile:
        lines.append(line)

    inFile.close()

    for line in lines:
        if verString in line:
            prefix=""
            if None != args.argPrefix:
                prefix = args.argPrefix + " "

            NewVerStr = verString + ' = "' + prefix + GenerateVersionString() + ' ' + GenerateBuildNumber(line) + '"'

            loc = lines.index(line)
            lines.remove(line)
            lines.insert(loc,NewVerStr)
            
            break

    outFile = open(args.argFilename,"wt")
    for line in lines:
        outFile.write(line)

    outFile.close()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        # do nothing here
        pass

