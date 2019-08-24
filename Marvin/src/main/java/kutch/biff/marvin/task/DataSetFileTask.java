/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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
 * #    File Abstract: 
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.task;

import static kutch.biff.marvin.widget.BaseWidget.convertToFileOSSpecific;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class DataSetFileTask extends BaseTask
{
    private int _interval;
    private int _RepeatCount;
    private String _strFileName;
    private double _fluxRangeLower, _fluxRangeUpper;

    public DataSetFileTask(String inpFile, int interval)
    {
        _RepeatCount = 0;
        _strFileName = inpFile;
        _interval = interval;
        _fluxRangeLower = _fluxRangeUpper = 0;
    }

    public void setRepeatCount(int count)
    {
        _RepeatCount = count;
    }
    
    public void setFluxRange(double lower, double upper)
    {
        _fluxRangeLower = lower;
        _fluxRangeUpper = upper;
    }

    @Override
    public void PerformTask()
    {
        String fname = getDataValue(_strFileName);
        fname = convertToFileOSSpecific(_strFileName);
        @SuppressWarnings("unused")
	int count = HandleDataFile(fname);
    }

    private int HandleDataFile(String inpFile)
    {
        int addedCount = 0;
        List<String[]> dataSets = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inpFile)))
        {
            String line = "";
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (line.startsWith("#"))
                {
                    continue;
                }
                dataSets.add(line.split(","));
                addedCount += 1;
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(DataSetFileTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        for (String[] dataList : dataSets)
        {
            if (dataList.length < 3)
            {
                LOGGER.severe("Invalid datalist DataSetFileTask in file " + inpFile +". List: " + dataList.toString());
                continue;
            }
            String Namespace = dataList[0];
            String ID = dataList[1];
            String DataPoints[] = new String[dataList.length-2];
            for (int index = 2; index < dataList.length; index++)
            {
                DataPoints[index-2] = dataList[index];
            }
            WalkDataListTask dlTask = new WalkDataListTask(Namespace, ID, DataPoints, _interval, _RepeatCount,_fluxRangeLower,_fluxRangeUpper);
            TASKMAN.AddDeferredTaskObject(dlTask);
        }
        
        return addedCount;
    }
}
