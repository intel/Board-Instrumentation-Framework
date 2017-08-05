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
#    Takes a file from OVS statistics and massages it to give a file that can be    
#    used as a DynamicCollector
#
##############################################################################

import os.path
import os
import time
import re

#system@ovs-system:
#	lookups: hit:427088506 missed:150 lost:0
#	flows: 0
#	masks: hit:535472211 total:0 hit/pkt:1.25
#	port 0: ovs-system (internal)
#		RX packets:0 errors:0 dropped:0 overruns:0 frame:0
#		TX packets:0 errors:0 dropped:0 aborted:0 carrier:0
#		collisions:0
#		RX bytes:0  TX bytes:0
#	port 1: br-tun (internal)
#		RX packets:0 errors:0 dropped:0 overruns:0 frame:0
#		TX packets:139569 errors:0 dropped:0 aborted:0 carrier:0
#		collisions:0
#		RX bytes:0  TX bytes:1159554746 (1.1 GiB)
#	port 2: vxlan_sys_4789 (vxlan: df_default=false, ttl=0)	
#		RX packets:0 errors:0 dropped:0 overruns:0 frame:0
#		TX packets:0 errors:0 dropped:0 aborted:0 carrier:0
#		collisions:0
#		RX bytes:0  TX bytes:0
#Makes path (if passed) os independent
def convertPath(path):
    separator = os.path.sep

    if separator == '/':
        path = path.replace('\\',os.path.sep)

    elif separator =='\\':
        path = path.replace('/',os.path.sep)

    return path

def ReadFromFile(Filename):
    Filename = convertPath(Filename)
    try:
        file = open(Filename,'rt')
    except:
        return None

    return file

def GetValue(vals):
    parts = vals.split(":")
    return parts[0]+"="+parts[1]


def ParseOVS_File(inFile,outFile):
    inFP = ReadFromFile(inFile)

    if None == inFP:
        outFP.write("Invalid file: " + inFile)

    else:
        writeData = ""
        for line in inFP:
            line.strip()
            if "port " in line:
                for i in range (0,2):
                    parsedItems = line.split(":")
                    name = parsedItems[1].split("(")[0].strip()
                    parts = inFP.readline().strip().split(" ")
                    SectName = parts[0].strip().replace(" ","_")
                    for part in parts[1:]:
                        if len(part)>1:
                             strVal = GetValue(part)
                             writeData += name+"."+SectName+"."+strVal + os.linesep
                             pass
                parts = inFP.readline().strip().split(":")  # colissions
                writeData += name+"."+parts[0]+"="+parts[1] + os.linesep

                line = inFP.readline().strip()
                rx = line.split("RX bytes")[1]
                val = rx.split(":")[1].split(" ")[0]
                if "\t" in val:
                    val=val.split("\t")[1]

                writeData += name + ".RX_bytes=" + val + os.linesep

                tx = line.split("TX bytes")[1]
                val = tx.split(":")[1].split(" ")[0]

                writeData += name + ".TX_bytes=" + val + os.linesep

               # print(writeData)


    inFP.close()
    outFP = open(convertPath(outFile),"wt")
    outFP.writelines(writeData)
    outFP.close()

    return "HelenKeller" # don't want to send anything    





#ParseOVS_File("ovsdpctl.show.stats.raw","foo.out")