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

import java.util.Random;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class WalkDataListTask extends BaseTask
{

    private int _interval;
    private int _RepeatCount;
    private int _currLoopCount;
    private int _currIndex;
    private double _fluxRangeLower, _fluxRangeUpper;
    private double _rangeMin, _rangeMax;
    private String[] _dataSet;
    private String _Namespace, _ID;
    private Random _rndObj = null;
    

    public WalkDataListTask(String Namespace, String ID, String[] dataset, int interval, int RepeatCount, double lowerFlux, double upperFlux)
    {
        _Namespace = Namespace;
        _ID = ID;
        _dataSet = dataset;
        _interval = interval;
        _RepeatCount = RepeatCount;
        _currLoopCount = 1;
        _currIndex = 0;
        _fluxRangeLower = lowerFlux;
        _fluxRangeUpper = upperFlux;

        boolean first = true;

        if (_fluxRangeLower != _fluxRangeUpper)
        {
            try
            {
                for (String val : _dataSet)
                {
                    double dVal = Double.parseDouble(val);
                    if (first)
                    {
                        first = false;
                        _rangeMin = dVal;
                        _rangeMax = dVal;
                    }
                    else if (dVal < _rangeMin)
                    {
                        _rangeMin = dVal;
                    }
                    else if (dVal > _rangeMax)
                    {
                        _rangeMax = dVal;
                    }
                }
                _rndObj = new Random();
            }
            catch (NumberFormatException ex)
            {
            }
        }
    }

    @Override
    public void PerformTask()
    {
        MarvinTask mt = new MarvinTask();
        String dataPoint = _dataSet[_currIndex];
        if (null != _rndObj)
        {
            dataPoint = calcFluxValue(dataPoint);
        }
        mt.AddDataset(_ID, _Namespace, dataPoint);
        TaskManager.getTaskManager().AddDeferredTaskObject(mt);
        _currIndex++;
        if (_currIndex >= _dataSet.length) // went through them all
        {
            _currLoopCount++;
            _currIndex = 0;
            if (_RepeatCount == 0 || _currLoopCount <= _RepeatCount)
            {
                // let fall through
            }
            else
            {
                return; // no more
            }
        }
        // call this task again in interval time
        TASKMAN.AddPostponedTask(this, _interval);
    }

    private String calcFluxValue(String dataPoint)
    {
        try
        {
            double dVal = Double.parseDouble(dataPoint);
            double modifier = _rndObj.doubles(_fluxRangeLower, _fluxRangeUpper).iterator().next();
            dVal += modifier;
            if (dVal < _rangeMin || dVal > _rangeMax )
            {
                // modified value falls outside of data range, so don't modify
            }
            else
            {
                return Double.toString(dVal);
            }
        }
        catch (NumberFormatException ex)
        {
        }
        return dataPoint;
    }
}
