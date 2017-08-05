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
# This collector dissects the raw data in a text file, the data is expected to be separated by spaces
# You will need to update the 'pieces' array to define how you want the data dissected
# You can dissect it into a HTML decoded file, or into a text file with the desired data displayed
#
##############################################################################

import random
import os.path

import struct
import codecs

# ID, start,lenth,color, if you don't want color, use ''
pieces = [["DEST_MAC",0,6,'gray'],["SRC_MAC",6,6,'brown'],["DEST_IP",26,4,'purple'],["SRC_IP",30,4,'pink'],['VNI',46,3,'red'],['ServiceIndex',0x35,1,'blue'],['ServicePath',0x36,3,'green']]

#what separates the pieces of data in the input file
DataSep = ' '

#Makes path (if passed) os independent
def convertPath(path):
    separator = os.path.sep

    if separator == '/':
        path = path.replace('\\',os.path.sep)

    elif separator =='\\':
        path = path.replace('/',os.path.sep)

    return path


class Decoder:
    def __init__(self):
        pass

    def IsStartLoc(self,index):
        for delim in pieces:
            if delim[1] == index:
                if len(delim[3]) < 1:
                    return None
                return delim[3]

        return None

    def IsStopLoc(self,index):

        for delim in pieces:
            if delim[1] + delim[2] == index:
                if len(delim[3]) < 1:
                    return False
                return True

        return False

    def DecodeToHTML(self,data,lineWidth):
        retStr = "<PRE>"
        width = 0
        index = 0
        
        for b in data:
            color=self.IsStartLoc(index)
            if None != color:
                retStr += "<FONT COLOR=\"" + color +"\">"

            retStr += data[index]
            index+=1
            width += 1
            if width == lineWidth:
                retStr+="<br>"
                width=0

            else:
              retStr += " "

            if self.IsStopLoc(index):
                retStr += "</FONT>"


        retStr+="</PRE>"
        return retStr

    def DecodeToText(self,data):
        retStr = ""
        index = 0
        
        for b in data:
            retStr += data[index]
            index+=1
            retStr += " "

        return retStr


def GetPart(packet,partInfo):
    retData = ""
    start = partInfo[1]
    end = partInfo[1]+partInfo[2]

    for x in range(start,end):
        retData += packet[x]
        if x+1 < end:
          retData += " "

    return retData

def CreateHTMLKey(packet):
    retStr = "<p>"
    for part in pieces:
        strPart = GetPart(packet,part)
        if len(part[3])>0:
            fntStart="<FONT Color=\"" + part[3]  + "\">"
            fntEnd = "</FONT>"
        else:
            fntStart=""
            fntEnd = ""

        retStr +=part[0] +"=" + fntStart + strPart + fntEnd + "<br>"

    retStr += "</p>"
    return retStr

def CreateTextKey(packet):
    retStr = ""
    for part in pieces:
        strPart = GetPart(packet,part)
        retStr +=part[0] +"=" + strPart + "\n"

    return retStr



def ReadPacketTextFile(Filename):
    # expected format: 0x00 0xaa 0xaa 0xaa 0xaa 0xaa 0x00 0xbb 0xbb 0xbb 0xbb 0xb
    packet=[]
    Filename = convertPath(Filename)
    inp = open(Filename,'rt')
    for line in inp.readlines():
        datalist = line.rstrip('\r\n').split(DataSep)
        for data in datalist:
            if len(data) > 0:
                packet.append(data)

    return packet

def ReadPacketTextFileAndMassage(Filename):
# dump mbuf at 0x0x7fce163519c0, phys=ae951a00, buf_len=2176
#  pkt_len=326, ol_flags=1020, nb_segs=1, in_port=0
#  segment at 0x0x7fce163519c0, data=0x0x7fce16351a36, data_len=326
#  Dump data at [0x7fce16351a36], len=326
#00000000: 00 03 03 03 03 03 00 02 02 02 02 02 08 00 45 00 | ..............E.
#00000010: 00 00 00 00 40 00 40 11 00 00 C0 A8 0A 02 C0 A8 | ....@.@.........
#00000020: 0A 03 E0 F1 12 B6 01 24 00 00 04 00 00 04 DE AD | .......$........
#00000030: BE 00 00 06 01 03 00 00 DD 02 00 00 00 00 00 00 | ................
#00000040: 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 01 | ................
#00000050: 00 10 94 00 00 02 08 00 45 00 00 EE 00 00 00 00 | ........E.......
#00000060: FF FD 38 BA C0 55 01 02 C0 00 00 01 00 00 00 00 | ..8..U..........
#00000070: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 | ................

    packet=[]
    Filename = convertPath(Filename)
    inp = open(Filename,'rt')
    for line in inp.readlines():
        if ':' in line and '|' in line:
            datalist = line.rstrip('\r\n').split()
            for data in datalist:
                bad = False
                if ':' in data or '|' in data or '.' in data:
                    bad = True
                    continue

                if not bad and len(data) > 0:
                    packet.append("0x"+ data)

    return packet


## To be called as a collector, that in turn generates a HTML formmatted file
## input file is the data file, output is the HTML target, and BytesPerLIne is how many pieces of data on a given line
def GenerateHTMLFile(inputFilename,outputFilename,BytesPerLine):
    inputFilename = convertPath(inputFilename)
    outputFilename = convertPath(outputFilename)
    BytesPerLine = int(BytesPerLine)

    packet = ReadPacketTextFileAndMassage(inputFilename)
    
    inst = Decoder()
    hdr = "<![CDATA[<html><head></head><body>" 
    trailer = "]]>"
    
    htmlData = hdr + CreateHTMLKey(packet) +"<font size=\"3\">" + "<p>" + inst.DecodeToHTML(packet,BytesPerLine) + "</p></font></body></html>" + trailer

    fp = open(outputFilename,'w+t')
    fp.write(htmlData)
    fp.close()

    return "HelenKeller"


## To be called as a collector, that in turn generates a text file, with the desired data pulled out for you
## input file is the data file, output is the text target
def GenerateTextFile(inputFilename,outputFilename):
    inputFilename = convertPath(inputFilename)
    outputFilename = convertPath(outputFilename)

    packet = ReadPacketTextFileAndMassage(inputFilename)
    
    inst = Decoder()
    strWrite = CreateTextKey(packet)

    strWrite += "Packet=" + inst.DecodeToText(packet)

    fp = open(outputFilename,'w+t')
    fp.write(strWrite)
    fp.close()

    return "HelenKeller" 

    
GenerateHTMLFile("mwc\sf2_pkt.txt","MWC\TestHTML.html",16)
#GenerateTextFile("mwc\sf2_pkt.txt","MWC\DecodedPacket.txt")
