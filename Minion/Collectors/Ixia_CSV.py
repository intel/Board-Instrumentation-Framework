##############################################################################
#  Copyright (c) 2017 by Intel Corporation
# 
#  Licensed under the Apache License, Version 2.0 (the "License");
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
#    Takes a file that is a csv results file from an IXIA test. Used as a 
#    dynamic collector minion plugin this will parse the CSV file and create
#    collectors for each statistic in the file.
#    Entrypoint: ixia_csv_collector 
##############################################################################

import sys
import time
import os
import csv

DataMap={}
Logger=None
Headers=[]
 
# gets current MS since epoch
def GetCurrMS():
    return  int(round(time.time() * 1000)) # Gives you float secs since epoch, so make it ms and chop

# Grabs the headers from the csv, for basic files it is the first row
def collectHeaders_basic(reader):
	for row in reader:
		# Collect headers from first line
		if reader.line_num == 1:	
			headers = row
			break
		else:
			break
	return headers	
	
# Checks to see if this results file has timestamps. For basic files the timestamp header will
# be the first value and header. It then calculates this time interval. 
def findTimeInterval_basic(file_name, min_timePeriod):
	with open(file_name, 'r') as csvfile:
		csvfile.seek(0)
		reader = csv.reader(csvfile)
		
		for row in reader:
			if reader.line_num == 2:	# skip line 1 because it is the headers
				firstTime = row[0]
			elif reader.line_num == 3:
				secondTime = row[0]
			elif reader.line_num > 4:
				break
		
	# Use first two ET to calculate interval
	try:
		firstTime = float(firstTime)
		secondTime = float(secondTime)
	except ValueError:
		return(1)
	collectorInterval = secondTime - firstTime

	# ensure that the collectorInterval is not less than min
	if collectorInterval < min_timePeriod:	
		collectorInterval = min_timePeriod

	return collectorInterval

# The other format of csv produced by IXIA has some header information at the strat of the file.
# This function reads in that header meta data and then the headers from the following rows.
def collectHeaders_cplx(reader):
	for row in reader:
		# Collector file header metadata
		if reader.line_num < 10:	
			DataMap[row[0]] = row[1]
		elif reader.line_num == 11:		# Column headers are on row 13
			headers = row
			break			
	return headers	

# This function takes the open csv reader and collection type, it will then collect data from
# the open file. It changes the line where data begins based on the collection type and uses
# the already gathered headers to get the datamap header values.
def data_collector(reader, collection_type):
    global DataMap, Headers

    starting_line = 1
    if collection_type == "complex":
        starting_line = 13

        # read the next row of data into DataMap
        for row in reader:
                if reader.line_num > starting_line:
                        header_index = 0
                        for entry in row:
                                DataMap[Headers[header_index]] = entry
                                header_index += 1
                        break                                           # only want one row at a time, so break after each
        else:
                return 1

        return 0


# The complex CSV format timestamps stored begining on a differnt row because of
# its header information and this function calculates the time interval of data
# publishing for complex results files. 	
def findTimeInterval_cplx(file_name, min_timePeriod):
	with open(file_name, 'r') as csvfile:
		csvfile.seek(0)
		reader = csv.reader(csvfile)
		
		# Read first two times to calculate the interval
		for row in reader:
			if reader.line_num == 14:	# Data does not begin until this row
				firstTime = row[0]
			elif reader.line_num == 15:
				secondTime = row[0]
				break
			elif reader.line_num > 15:
				break
	try:
		firstTime = float(firstTime)
		secondTime = float(secondTime)
	except ValueError:
		return(1)
	collectorInterval = secondTime - firstTime
	
	# ensure that the collectorInterval is not less than min
	if collectorInterval < min_timePeriod:
		collectorInterval = min_timePeriod
	return collectorInterval

# This function determines whether the first row is headers, or meta data and then returns
# the format type as basic or complex for first row headers, or first rows meta data.
def getCollectionType(file_name):
	with open(file_name, 'r') as file:
		first_line = file.readline()
		first_line = first_line.split(',')
		
		# Determine if a file header is part of the file or not
		if first_line[0] == "CSVFormatID":
			Collection_Type = "complex"
		else:
			Collection_Type = "basic"
	
	return Collection_Type
	
# This function is called to collect the csv headers. It uses the given file and collection
# type to call the basic or complex header collection. 
def getHeaders(file_name, collection_Type):
	global Headers
	
	with open(file_name, 'r') as csvfile:
		csvfile.seek(0)
		reader = csv.reader(csvfile)
		if collection_Type == "complex":
			Headers = collectHeaders_cplx(reader)
		else:
			Headers = collectHeaders_basic(reader)
	return 0

# This is the entrypoint function for the dynamic collector minion. It will use the given
# file to parse and send back the data from each row at the time interval specified inside 
# the file. However, the frequency specified in the framework is the minimum return interval.	
def ixia_csv_collector(frameworkInterface, file_name):
	Logger = frameworkInterface.Logger
	
	min_timePeriod = float(frameworkInterface.Interval / 1000)
	global DataMap, Headers
	
	try:
		Logger.info("Starting IXIA CSV Collector")
		
		# Determine the fileformat and store 
		collection_Type = getCollectionType(file_name)
		
		# Collect the headers and store in the global
		getHeaders(file_name, collection_Type)
		
		# Determine the collector time interval
		if collection_Type == "complex":
			if Headers[0] == "Elapsed Time" or Headers[0] == "ET":
				collectorInterval = int(round(findTimeInterval_cplx(file_name, min_timePeriod) * 1000))
		else:
			if Headers[0] == "~ElapsedTime" or Headers[0] == "~ET":
				collectorInterval = int(round(findTimeInterval_basic(file_name, min_timePeriod) * 1000))
		
		with open(file_name, 'r') as csvfile:
			reader = csv.reader(csvfile)
		
			# Create collectors for each statistic found in the file headers	
			for entry in DataMap:
				if not frameworkInterface.DoesCollectorExist(entry):
					frameworkInterface.AddCollector(entry)
					
			for entry in Headers:
				if not frameworkInterface.DoesCollectorExist(entry):
					frameworkInterface.AddCollector(entry)
					
			isEOF = 0
			while not frameworkInterface.KillThreadSignalled() and isEOF == 0:
				startTime = GetCurrMS() 	# collect start time for sending data
				
				isEOF = data_collector(reader, collection_Type)
				for entry in DataMap:	
					frameworkInterface.SetCollectorValue(entry, DataMap[entry])
				
				# Sleep the correct ammount of time that remains
				tDelta = GetCurrMS() - startTime
				if tDelta > collectorInterval:
					sleepTime = 100
				
				else:
					sleepTime = collectorInterval - tDelta
				
				time.sleep(sleepTime/1000.0)
		
	except Exception as Ex:
		Logger.error("Uncaught error in IxiaCsvPlugin: " + str(Ex))
	
	return "HelenKeller" # don't want to send anything		
