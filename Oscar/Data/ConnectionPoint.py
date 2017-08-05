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
#    Utility class to keep a target connection point info
#
##############################################################################

class ConnectionType(object):
    UpstreamServer = 0
    DownstreamServer = 1
    Marvin = 2
    Minion = 3
    UpstreamOscar = 4
    DownstreamOscar = 5
    DynamicMarvin = 6
    DynamicMarvin_To_Remove = 7

    Unknown = 20

    def toString(val):
        if val == ConnectionType.DownstreamServer:
            return "Downstream Server"

        if val == ConnectionType.UpstreamServer:
            return "Upstream Server"

        if val == ConnectionType.Marvin:
            return "Marvin"

        if val == ConnectionType.Minion:
            return "Minion"

        if val == ConnectionType.UpstreamOscar:
            return "Upstream Oscar"

        if val == ConnectionType.DownstreamOscar:
            return "Oscar"

        if val == ConnectionType.DynamicMarvin:
            return "^Marvin^"

        if val == ConnectionType.DynamicMarvin_To_Remove:
            return "#Marvin#"

        return "Unknown"


class ConnectionPoint:
    def __init__(self,ip=None,Port=None,TargetType=ConnectionType.Unknown):
        self.IP = str(ip)
        if None == Port:
            Port = 0
        self.Port = int(Port)
        self.Type = TargetType

    def getIP(self):
        return self.IP

    def getPort(self):
        return self.Port

    def getType(self):
        return self.Type

    def getTypeStr(self):
        return ConnectionType.toString(self.Type)
    
    def __str__(self):
        return str(self.IP).lower() + ":" + str(self.Port)

