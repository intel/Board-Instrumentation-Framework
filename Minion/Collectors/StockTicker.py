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
#    Will get some stock ticker information - only tested under Python 3
#    The collector itself doesnt' return anything, instead it generates a file
#    with various info about a specific stock in a format the DynamicCollector
#    can use.  So it's a 2 part solution, call this collector and then call the
#    dynamic collectors, both using the same file.  I would suggest you put them
#    both in a <Group>
##############################################################################

import json 
import pprint 
import urllib.request 
import os

def __get_stock_quote(ticker_symbol): 
    url = 'http://finance.google.com/finance/info?q={}'.format(ticker_symbol) 
    lines = urllib.request.urlopen(url).readlines() 
    lines_string = [x.decode('utf-8').strip('\n') for x in lines] 
    #print(lines_string) 
    merged = ''.join([x for x in lines_string if x not in ('// [', ']')]) 
    #print("merged:{}".format(merged)) 
    return json.loads(merged) 

def GetCurrentStockValue(ticker_symbol):
    try:
        quote = __get_stock_quote(ticker_symbol)
        return ''.format(quote['l_cur'])
    except:
        return 'Error'

def GetStockInfo(targetFile,ticker_symbol):
    try:
        quote = __get_stock_quote(ticker_symbol)

        writeStr = ""
        writeStr += 'ticker={}'.format(quote['t']) + os.linesep
        writeStr += 'current price={}'.format(quote['l_cur']) + os.linesep
        writeStr += 'last trade={}'.format(quote['lt']) + os.linesep
        writeStr += 'market={}'.format(quote['e']) + os.linesep

        file = open(targetFile,"wt")
        file.write(writeStr)
        file.close()

    except:
        pass

    return "HelenKeller" # don't want to send anything

#GetStockInfo("Stock.txt","INTC")