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
#    Wrapper class for a piece of data, could be from file or from network
#
##############################################################################
from Util import Time

class MarvinData(object):
    __FirstTime = Time.GetCurrMS()

    def __init__(self,Namespace,ID,Value,ElapsedTime,FormatVersion,isLive=True): 
        from Helpers import Configuration

        self.FormatVersion=FormatVersion
        self.Value = Value
        if True == isLive:
            self.ArrivalTime = Time.GetCurrMS() - MarvinData.__FirstTime # just a delta from start is fine, no need for a 10 digit string for time

        else:
            self.ArrivalTime = ElapsedTime

        self.Namespace = Configuration.get().HandleBITWNamespace(Namespace) # if Bump in the Wire, change NS

        self.ID = ID
        self.Live = isLive


    def ToXML(self,destIsFile=False): 
        startCDATA="<![CDATA["
        endCDATA="]]>"
        if False == destIsFile:
            buffer = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        else:
            buffer = ""
        
        buffer = buffer + "<Oscar Type=\"Data\">"
        buffer = buffer + "<Version>1</Version>"
        buffer = buffer + "<Namespace>"+self.Namespace+"</Namespace>"
        buffer = buffer + "<ID>"+self.ID+"</ID>"
        if False == destIsFile:
            buffer = buffer + "<Value LiveData=\""+str(self.Live)+"\">"+startCDATA+self.Value+endCDATA+"</Value>"
        else:
            buffer = buffer + "<Value>"+self.Value+"</Value>"
        buffer = buffer + "</Oscar>"

        return buffer

    
        
