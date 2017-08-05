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
#       Does some voodoo to parse DMI data from a file
##############################################################################

import os

#Makes path (if passed) os independent
def convertPath(path):
    separator = os.path.sep

    if separator == '/':
        path = path.replace('\\',os.path.sep)

    elif separator =='\\':
        path = path.replace('/',os.path.sep)

    return path

def GetDMI_Info(inpfile,section,instance,item):
    Filename = convertPath(inpfile)
    file = open(Filename,'rt')
    if None == file:
        return "File [" + Filename + "] does not exist"

    try:
        instance = int(instance)
    except:
        return "Invalid instance: " + instance + " for GetDMI_Info()"

    sectionFound = False
    currInst = 0
    
    for line in file:
        if not sectionFound:
            if line[0] == ' ': #sections start @ left, with no spaces
                continue
            if line[0] == '\t': #sections start @ left, with no spaces
                continue

            if line.strip() == section:
                if currInst == instance:
                    sectionFound = True
                else:
                    currInst += 1
        else:
            line = line.strip() # get rid of leading spaces
            parts = line.split(':')
            if len(parts) > 1:
                if parts[0].strip() == item:
                    return parts[1].strip()

    return "not found"

def GetNumbeOfSockets(inputFile):

    for count in range(1,100): #can start at 1, cause we know we have at least one socket :-)
        if "not found" == GetDMI_Info(inputFile,"Processor Information",count,"Socket Designation"):
            return str(count)


def GetDMIInfo(inputFile,outputFile):
    dataStr = ""

    dataStr += "System.BIOS.Vendor=" + GetDMI_Info(inputFile,"BIOS Information",0,"Vendor") + os.linesep
    dataStr += "System.BIOS.Version=" + GetDMI_Info(inputFile,"BIOS Information",0,"Version") + os.linesep
    dataStr += "System.BIOS.ReleaseDate=" + GetDMI_Info(inputFile,"BIOS Information",0,"Release Date") + os.linesep
    dataStr += "System.BIOS.Revision=" + GetDMI_Info(inputFile,"BIOS Information",0,"BIOS Revision") + os.linesep
    dataStr += "System.BIOS.FirmwareRevision=" + GetDMI_Info(inputFile,"BIOS Information",0,"Firmware Revision") + os.linesep

    dataStr += "System.Manufacturer=" + GetDMI_Info(inputFile,"System Information",0,"Manufacturer") + os.linesep
    dataStr += "System.ProductName=" + GetDMI_Info(inputFile,"System Information",0,"Product Name") + os.linesep
    dataStr += "System.SerialNumber=" + GetDMI_Info(inputFile,"System Information",0,"Serial Number") + os.linesep
    dataStr += "System.Family=" + GetDMI_Info(inputFile,"System Information",0,"Family") + os.linesep

    sockCountStr = GetNumbeOfSockets(inputFile)
    coreCountStr = str(int(GetDMI_Info(inputFile,"Processor Information",0,"Core Count")) * int(sockCountStr))

    dataStr += "System.SocketCount=" + sockCountStr + os.linesep
    dataStr += "System.PhysicalCoreCount=" + coreCountStr + os.linesep
    dataStr += "System.LogicalCoreCount=" + str(int(sockCountStr) * int(GetDMI_Info(inputFile,"Processor Information",count,"Thread Count"))) + os.linesep

    file = open(outputFile,"wt")
    file.write(dataStr)
    file.close()
    return "HelenKeller" # don't want to send anything



#GetDMIInfo("dmi.txt","dmi.txt.nuke")