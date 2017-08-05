##############################################################################
#  Copyright (c) 2017 Intel Corporation
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
#
#       Gathers info on live docker containers by calling the docker stats
#       command and monitoring what it gives back. 
#       Entrypoint: docker_stats_collector
##############################################################################

import os
import sys
import subprocess
import time

DataMap={}
Headers=[]
Containers=[]
Logger=None

def GetCurrMS():
	return int(round(time.time() * 1000)) # Gives you float secs since epoch, so make it ms and chop

# Entrypoint to start collection and monitoring of the published docker stats
# command return values.
def docker_stats_collector(frameworkInterface):
	Logger = frameworkInterface.Logger
	
	try:
		Logger.info("Starting Docker Stats Collector")
		
		# Call the docker stats command in subprocess to monitor 
		cmd = ["docker", "stats", "--no-stream"]
		process = subprocess.Popen(cmd,
					stdout=subprocess.PIPE,
					stderr=subprocess.STDOUT)
							
		(line, err) = process.communicate()
		lines = line.decode("utf-8").split('\n')			

		lineNum = 0
		for row in lines:
			if lineNum == 0:
				splitLine = row.split('   ')

				for word in splitLine:
					word = word.strip()
					if len(word) != 0 and word not in Headers:
						Headers.append(word) # store in global list
			else:
				splitLine = row.split('   ')
				loopNum = 0
				# sort through the columns list(splitLine)
				for word in splitLine:
					word = word.strip()
					
					# take this branch on container ID column
					if len(word) != 0 and loopNum == 0:
						currContainer = word

						# check id and add to containers if needed
						if word not in Containers:
							Containers.append(word)

						DataMap["docker.ID." + word] = word
						loopNum += 1	
					# otherwise store value in corect column
					elif len(word) != 0:
						if Headers[loopNum] == "CPU %" or Headers[loopNum] == "MEM %":
							word = word[:-1]
						DataMap["docker." + currContainer + "." + Headers[loopNum]] = word
						loopNum += 1
					# create collectors and update collector values				
			lineNum += 1			

			for entry in DataMap:
				if not frameworkInterface.DoesCollectorExist(entry):
					frameworkInterface.AddCollector(entry)
		
				frameworkInterface.SetCollectorValue(entry, DataMap[entry])
				
	except Exception as Ex:
		Logger.error("Uncaught error in Docker_Stats plugin: " + str(Ex))
		
	return "HelenKeller" # don't want to send anything
