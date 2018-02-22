/*
 * ##############################################################################
 * #  Copyright (c) 2016 Intel Corporation
 * # 
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * #  you may not use this file except in compliance with the License.
 * #  You may obtain a copy of the License at
 * # 
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * # 
 * #  Unless required by applicable law or agreed to in writing, software
 * #  distributed under the License is distributed on an "AS IS" BASIS,
 * #  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * #  See the License for the specific language governing permissions and
 * #  limitations under the License.
 * ##############################################################################
 * #    File Abstract: Cusom Logger for Marvin, creates a HTML formatted file
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MarvinLogger
{
    static private FileHandler _marvinLogFile;
    static private Formatter _marvinLogFormatObj;
    //private static ConsoleHandler _

    static public void setup(String fileName) throws IOException
    {
        try
        {
            Logger logger =  Logger.getLogger(MarvinLogger.class.getName());

//            logger.setLevel(Level.ALL);
                          
             int limit = 1024000 * 10; // 10 Mb maximum, then cut off
            _marvinLogFile = new FileHandler(fileName,limit,1);

            _marvinLogFormatObj = new MarvinHtmlLoggerFormatter();
            _marvinLogFile.setFormatter(_marvinLogFormatObj);
            
            logger.addHandler(_marvinLogFile);

            /*
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);
            */
            setDebugLevel(Level.WARNING);
        }
        catch (Exception ex)
        {
            System.out.println(ex.toString());
        }
    }
    static public void setDebugLevel(Level newLevel)
    {
        Logger logger =  Logger.getLogger(MarvinLogger.class.getName());
        logger.setLevel(newLevel);
        for (Handler hdl :logger.getHandlers())
        {
            hdl.setLevel(newLevel);
        }
    }
}
