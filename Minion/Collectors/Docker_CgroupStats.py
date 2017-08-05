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
#       Gathers info on live docker containers from the statistics stored
#       in the cgroup directories for cpu and memory. Only run this on
#       linux as it is not configured for windows cgroup directories.
#       Entrypoint: goCollect
##############################################################################

import os
import sys
import subprocess
import time
import unicodedata

DataMap={}
Containers=[]
Logger=None
MemoryNoPerm = ["memory.force_empty", "cgroup.event_control", "memory.pressure_level", "memory.kmem.slabinfo"] 
CPU_Diff = ["cpuacct.usage_percpu", "cpuacct.usage_percpu_sys", "cpuacct.usage_percpu_user"]
CPUThreeColumn = "cpuacct.usage_all"

def GetCurrMS():
	return int(round(time.time() * 1000)) # Gives you float secs since epoch, so make it ms and chop

# This function takes a file name, path, and short ID to collect values from the
# given file name in the path. It accounts for any 3 column with headers formatted
# cgroup file. The column format is (CPU, stat1, stat2). The three CPU diff files
# are just theese statistics broken into three different files
def getThreeColumn(fileName, path, shortID):
	global DataMap
	with open (path + "/" + fileName, 'r') as file:
		loopNum = 0
		column_headers = []
		for line in file:
			if loopNum == 0:
				column_headers = line.split(" ")
			else:
				splitLine = line.split(" ")
				index = 0
				mapHead = ""

				# map the first column to the other two columns of data individually
				for column in splitLine:
					if index == 0:
						mapHead = (mapHead + "docker." + shortID + "." + fileName + 
								"." + column_headers[index] + "." + column)
						DataMap[mapHead] = column
					else:
						DataMap[mapHead + "docker." + column_headers[index]] = column
					index += 1
			loopNum += 1
	return

# This function accounts for the memory statistic file gathering stats on
# each of the Numa nodes. It has a different format than the normal files
def getNumaStats(fileName, path, shortID):
	global DataMap
	with open (path + "/" + fileName, 'r') as file:
		for line in file:
			splitLine = line.split(" ")
			index = 0
			
			for val in splitLine:
				splitVal = val.split("=")
				# The first value in each line is the stat title
				if index == 0:
					lineHead = splitVal[0]
					entry = splitVal[1]
					entryHeader = lineHead
				else:	# each numa's stat for the lines statistic
					entryHeader = lineHead + "." + splitVal[0]
					entry = splitVal[1]
	
				mapHead = ("docker." + shortID + "." + fileName + "." + entryHeader)
				DataMap[mapHead] = entry
				index += 1
	return
			

# This function uses a file path and container long ID to go and read in
# all of the cgroup statistic files in the given path
def get_cgroup(container, path):
	global DataMap, MemoryNoPerm, CPU_InThree
	
	# use short ID for Collector Labels
	shortID = container[:12]
	fileList = os.listdir(path)	# list of files in the directory to read
	fileIndex = 0

	for fileName in fileList:
		# ignore the files with bad permissions and bad formats
		if fileName not in MemoryNoPerm and fileName not in CPU_Diff:
			if fileName == CPUThreeColumn:	# three column special case
				getThreeColumn(fileName, path, shortID)
			elif fileName == "memory.numa_stat":	# numa file special case
				getNumaStats(fileName, path, shortID)
			else:
				with open (path + "/" + fileName, 'r') as file:
					# for each stat add to the DataMap
					for line in file:
						if line != "":
							splitLine = line.split(" ")
							mapHead = "docker." + shortID + "." + fileName
							mapHead.encode('ascii', 'ignore')

							# map each value if two column format
							if len(splitLine) > 1:
								mapHead = mapHead + "." + splitLine[0]
								DataMap[mapHead] = splitLine[1]
							else:
								DataMap[mapHead] = splitLine[0]
		fileIndex += 1
	return

# Entrypoint for the dynamic collector in the framework.Begins collection of data
# on the live docker containers.
def goCollect(frameworkInterface):
	global DataMap
	Logger = frameworkInterface.Logger
	
	try:
		# Call docker ps command to get the long Id value for live containers
		cmd = ["docker", "ps", "--no-trunc", "--format", "{{.ID}}"]
		process = subprocess.Popen(cmd, stdout=subprocess.PIPE)	

		(line, err) = process.communicate()		
		# decode the line and split into a list, which is the list of container ID's
		Containers = line.decode("utf-8").split('\n')
		
		# loop through and collect on all live containers
		for cont in Containers:
			if cont != '':
				# Call for two paths to monitor the cpu and memory Cgroup files
				get_cgroup(cont, "/sys/fs/cgroup/memory/docker/" + cont)
				get_cgroup(cont, "/sys/fs/cgroup/cpu,cpuacct/docker/" + cont)
				# add and update collectors as new ones are gathered
				for entry in DataMap:
					if not frameworkInterface.DoesCollectorExist(entry):
						frameworkInterface.AddCollector(entry)
					# set or update stats collector value
					frameworkInterface.SetCollectorValue(entry, DataMap[entry])
	
	except Exception as Ex:
		Logger.error("Uncaught error in Docker_Cgroup plugin: " + str(Ex))

	return "HelenKeller" # do not want to send anything
