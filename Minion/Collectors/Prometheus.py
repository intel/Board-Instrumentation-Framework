##############################################################################
#  Copyright (c) 2019 Intel Corporation
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
#    This collector will grab data from an Prometheus database.  Collecting from a 
#    database is complicted and takes a lot of parameters. The syntax is very similiar
#    to the influxDB collector: The following is an example
'''
   <DynamicCollector>
        <Plugin>
            <PythonFile>Collectors\Prometheus.py</PythonFile>
            <EntryPoint SpawnThread="True">PointCollectFunction</EntryPoint>
            <Param>nd-r720-115.jf.intel.com:9090</Param> 
                <Param>cpuInfo=
                            {    
                                "query": '{__name__=~"collectd_cpu.+",exported_instance=~"npg-svr-3.+"}', see https://prometheus.io/docs/prometheus/latest/querying/api/
                                "namespace": "{exported_instance}",
                                "id" : ["{__name__}","{type}","{cpu}"],
                                "instance" : "cpu",
                                "separator" : ".",
                                "csv" : "makelist"
                            }
                </Param> 

                <Param>cpuInfoRange=
                            {    
                                "query": 'rate({__name__=~"collectd_cpu.+",exported_instance=~"npg-svr-3.+"}[30m])',
                                "namespace": "{exported_instance}",
                                "id" : ["cpu_range","{type}","{cpu}"],
                                "instance" : "cpu",
                                "separator" : ".",
                                "csv" : "makelist"
                            }
                </Param> 
        </Plugin>
        <Precision>0</Precision>
    </DynamicCollector>
'''    
#
##############################################################################
from __future__ import print_function
import sys
import requests
import json
import urllib.parse
import logging
import ast
import time
import traceback

VersionStr="20.02.07 Build 1"

logger = object()

# just a wrapper routine for a thread sleeping.  In case needs to be OS specific or something
def Sleep(seconds):
    try:
        time.sleep(seconds)
        
    except BaseException:
        pass

def SleepMs(milliseconds):
    Sleep(float(milliseconds)/1000.0)    

#gets current MS since epoch
def GetCurrMS():
    return  int(round(time.time() *1000)) # Gives you float secs since epoch, so make it ms and chop

class Entry:
    def __init__(self,key,value):
        self._fixed = True
        self._key = key
        self._value = self._evaluate(value)

    def key(self):
        return self._key

    def value(self):
        return self._value

    def fixed(self):
        return self._fixed

    def get(self):
        return (self._key,self._value,self._fixed)

    def evaluate(self,dataMap):
        if self._fixed:
            return str(self._value)

        if self._key in dataMap:
            return str(dataMap[self._key])

        else:
            return None 

    def _evaluate(self,entry) :
        if len(entry) > 3:
            if entry[0] == '{' and entry[-1] == '}' :
                entry = entry[1:-1]
                self._fixed = False
                self._key = entry

        return entry

class Measurement:
    def __init__(self,configMap,name):
        self._MeasurementList = None
        self._listMap={}  # Keep it around, as member in case I get partial data
        self._Name = name

        if not 'query' in configMap:
            raise ValueError("Measurement {} did not specify query instructions".format(self._Name))

        self._Query = configMap['query']


        if 'separator' in configMap:
            self._separator = configMap['separator']

        else:
            self._separator = "."

        if not 'id' in configMap:
            raise ValueError("Measurement {} did not specify id instructions".format(self._Name))
        
        self._id_keys = []
        for keyEntry in configMap['id']:
            self._id_keys.append(Entry('',keyEntry))

        if 'namespace' in configMap:
            self._namespace = Entry('namespace',configMap['namespace'])
        
        else:
            self._namespace = None

        if 'instance' in configMap:
            self._instance = configMap['instance']
        
        else:
            self._instance = None

        if 'csv' in configMap:
            if 'makelist' == configMap['csv']:
                self._makeList = True
                self._makeListOnly = False
                logger.info("Will attempt to make a list of items from Prometheus")

            elif 'makelist_only' == configMap['csv']:
                self._makeList = True
                self._makeListOnly = True
                logger.info("Will attempt to make a list of items from Prometheus, and exclude the individual datapoints that make up the list")

            else:
                raise ValueError("Measurement {} had invalid csv of {}".format(self._Name,configMap['csv']))

            if 'csv_sort_by' in configMap:
                self._sortBy = Entry('sortBy',configMap['csv_sort_by'])

            self._csvSizeDetermined = False

        else:
            self._makeList = False
            self._makeListOnly = False


    def performQuery(self,dbClient):            
        response = dbClient.query(self._Query)
        if response.status_code != 200:
            logger.error(f'issue fetching data [code:{response.status_code}  {response.text}]')
            return None

        body = json.loads(response.text)
        metrics = body['data']['result']
        listMap={}
        returnMap={}
        
        for result in metrics:
            #metric = result['metric']['__name__']
            if not 'exported_instance' in result['metric']:
                continue # means is probably prometheus daemon stats, so ignore

            #server = result['metric']['exported_instance']
            #timestamp = result['value'][0]
            value = result['value'][1]
            ID=''
            ListID=''

            ## look at returned data, and build the ID from parts specified in Measurement (from config file)
            for id_part in self._id_keys:
                idTag = id_part.evaluate(result['metric'])
                if None == idTag:
                    logger.warning(f"Prometheus collector - configuration for query:{self._Query} had ID field of {id_part._value} that cannot be found")

                if id_part.key() == self._instance:
                    pass

                else:
                    if '' == ListID:
                        ListID = idTag
                    else:
                        ListID += self._separator + idTag

                if '' == ID:
                    ID = idTag    
                else:
                    ID += self._separator + idTag

            if None != self._namespace and len(result['metric']) > 0:
                NS = self._namespace.evaluate(result['metric'])
            else:
                NS = "None"

            if not NS in returnMap:
                returnMap[NS] = {}

            nsDataMap = returnMap[NS]

            if self._makeList:
                ListID += self._separator + "list"
                if not ListID in listMap:
                    listMap[ListID] = []
                ## data may not come in sorted, so just place in list and sort it later
                listMap[ListID].append((ID,value))

            if not self._makeListOnly:
                if ID in nsDataMap:
                    logger.warning(f"Prometheus collector.  Duplicate ID {ID} being generated.  Make sure your configuration is correct.")
                nsDataMap[ID] = value

        for listID in listMap: # did we create some lists, if so, sort the data and create real list
            listValues=""
            for _,val in sorted(listMap[listID], key=lambda  x:x[0]):
                if "" == listValues:
                    listValues = val
                else:
                    listValues += "," + val

            nsDataMap[listID] = listValues

        return returnMap # returns a map of Namespaces that contains a map of values

class Prometheus:
    def __init__(self, target):
        self.target = target

    def getAllMetrics(self):
        return self._sendGet('/metrics',None)

    def query(self, query='prometheus_build_info'):
        return self._sendGet(uri='/api/v1/query',params=dict(query=query))

    def _sendGet(self, uri, params):
        url = urllib.parse.urljoin(self.target, uri)
        result = requests.get(url=url,params=params)

        return result

    # just a test fn
    def gatherAllInfo(self,prom_query=r'{__name__=~".+"}'):
        serverMap={}

        response = self.query(prom_query)
        if response.status_code != 200:
            print(f'issue fetching data [code:{response.status_code}  {response.text}]')
            return None

        body = json.loads(response.text)
        
        for result in body['data']['result']:
            metric = result['metric']['__name__']
            if not 'exported_instance' in result['metric']:
                continue # means is probably prometheus daemon stats, so ignore

            server = result['metric']['exported_instance']
            #print(f'{timestamp} {value}')            
            if not server in serverMap:
                serverMap[server]={}

            metricName = metric
            numericList=[]
            for key in result['metric']:
                if key == '__name__' or key == 'instance' or key == 'job' or key == 'exported_instance':
                    continue
                tag = f".{result['metric'][key]}"
                try:  # if is a numeric value, is something like cpu core #, so put at end
                    float(result['metric'][key])
                    numericList.append(tag)
                except:
                    metricName += tag

            for tag in numericList:
                metricName += tag

            if not metricName in serverMap[server]:
                serverMap[server][metricName]=1

            else:
                serverMap[server][metricName] += 1

        return serverMap

def _ValidateInputFilter(inputStr):
    filter = inputStr.replace('\n', '').replace('\r', '').strip()
    try:
        return ast.literal_eval(filter)
    except Exception as ex:
        raise ValueError("invalid filter specified for Prometheus collector: " + str(ex))


def _CollectFunction(frameworkInterface,target,**filterList):    
    global logger
    logger = frameworkInterface.Logger
    logger.info("Starting Prometheus Collector {0}".format(VersionStr))

    if 0 == len(filterList):
        raise ValueError("No filters specified for Prometheus collector")

    FilterSet=[]
    for key in filterList:
        filter = filterList[key]
        dbFilter = _ValidateInputFilter(filter)
        FilterSet.append(Measurement(dbFilter,key))

    if not ':' in target:
        raise ValueError("Prometheus requires 1st Parameter to be IP:Port")

    # make sure has the http url prefix!
    if target[:4].lower() != 'http':
        target = 'http://' + target

    sleepInterval = frameworkInterface.Interval
    loopStart = GetCurrMS()
    try:
        warningOcurredCount = 0
        while not frameworkInterface.KillThreadSignalled():
            loopTime = GetCurrMS() - loopStart
            if loopTime > frameworkInterface.Interval:
                warningOcurredCount += 1
                if warningOcurredCount == 5:
                    logger.error("Prometheus Collector takes {} ms for one loop, but interval set for {}".format(loopTime,frameworkInterface.Interval))
                
                if warningOcurredCount > 20:
                    warningOcurredCount = 0

                sleepInterval = 0
            else:
                sleepInterval = frameworkInterface.Interval - loopTime

            SleepMs(sleepInterval)
            loopStart = GetCurrMS()
            client = Prometheus(target+'/')

            dataMap={}

            try:
                for entry in FilterSet:
                    newDataMap = entry.performQuery(client)
                    for namespace in newDataMap:
                        if not namespace in dataMap:
                            dataMap[namespace] = {}

                        dataMap[namespace].update(newDataMap[namespace])
                    

            except Exception as Ex: #Prometheus does NOT have a robust Exception system :-(
                logger.info("Prometheus Collector:  {}".format(Ex))
                traceback.print_exc()
                continue

            for Namespace in dataMap:
                nsMap = dataMap[Namespace]
                for ID in nsMap:
                    Data = nsMap[ID]

                    if not frameworkInterface.DoesCollectorExist(ID,Namespace): # Do we already have this ID?
                        frameworkInterface.AddCollector(ID,Namespace)    # Nope, so go add it, and maybe Custom NS!

                    frameworkInterface.SetCollectorValue(ID,Data,None,Namespace)

    except Exception as Ex:
        logger.error("Unrecoverable error in Prometheus Collector plugin: " + str(Ex))            

'''
Just for internal testing and learning
def testFn():
    # few moving parts
    # query to get all current collectd data
    prom_query = urllib.parse.quote(r'{__name__=~"collectd_.+"}')
    prom_query = urllib.parse.quote(r'{__name__=~".+"}')
    #prom_query = urllib.parse.quote(r'{__name__=~"collectd_ipmi_.+"}')

    # getting data
    targ="http://nd-r720-115.jf.intel.com:9090"
    query_url = f'{targ}/api/v1/query?query={prom_query}'
    print(query_url)
    resp = get(query_url)
    if resp.status_code != 200:
        print(f'issue fetching data [code:{resp.status_code}]')

    # parse response
    body = json.loads(resp.text)
    #pprint.pprint(body)
    for result in body['data']['result']:
        # parse as needed for biff
        #pprint(result['metric'])
        timestamp = result['value'][0]
        value = result['value'][1]
        #print(f'{timestamp} {value}')


    #labels = getLabels(targ)            
    #pprint(labels)

    prom = Prometheus(targ+'/')
    matchStr='  {exported_instance="npg-srv-31"}'

    query = r'{__name__=~"collectd_ipmi_.+"}'
            
    #infoMap = prom.gatherAllInfo(query)
    #query = r'collectd_ipmi_fanspeed {exported_instance="npg-svr-31"}'
    query = r'{__name__=~"collectd_ipmi_.+"} &query={exported_instance="npg-svr-31"}'
    exported_instance = 'npg-svr-31'
    query = f'{{__name__=~"collectd_.+",exported_instance="{exported_instance}",type=~"id.+"}}'
    query1 = '{__name__=~"collectd_ipmi_.+",exported_instance="npg-svr-31"}'
    query1 = 'rate({__name__=~"collectd_cpu.+",exported_instance=~"npg-svr-3.+"}[30m])'
    #rate(http_requests_total{job="api-server"}[5m])

    infoMap = prom.gatherAllInfo(query)
    infoMap1 = prom.gatherAllInfo(query1)
    pass
'''
## Dynamic Collector interface
def PointCollectFunction(frameworkInterface,target,**filterList):
    _CollectFunction(frameworkInterface,target,**filterList)
