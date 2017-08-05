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
 * #    File Abstract: 
 * #    Performs simple mathematic operations on a data point
 * ##############################################################################
*/ 


package kutch.biff.marvin.task;

/**
 *
 * @author Patrick
 */
public class MathematicTask extends PulseTask
{

    private double _Value;
    private boolean validVal = false;
    private String _Operation = null;

    public MathematicTask()
    {

    }

    public boolean setValue(String strValue)
    {
        try
        {
            _Value = Double.parseDouble(strValue);
            validVal = true;
        }
        catch (NumberFormatException ex)
        {

        }
        return validVal;
    }

    @Override
    public boolean isValid()
    {
        return (super.isValid() && validVal && _Operation != null);
    }

    public boolean SetOperation(String strOper)
    {
        if (strOper.equalsIgnoreCase("Add")
            || strOper.equalsIgnoreCase("Subtract")
            || strOper.equalsIgnoreCase("Multipley"))
        {
            _Operation = strOper;
        }

        return (null != _Operation);
    }

    @Override
    public void PerformTask()
    {
        String currValueStr;
        double doubleVal;

        currValueStr = TASKMAN.getDataMgr().GetValue(_ID, _Namespace);
        if (null == currValueStr)
        {
            LOGGER.warning("Mathematic Task failed [" + getTaskID() +"] beause the data point does not exist (yet).");
            return;
        }
        
        try
        {
            doubleVal = Double.parseDouble(currValueStr);
        }
        catch (NumberFormatException ex)
        {
            LOGGER.warning("Attempted Mathematic Task on Non numrice Data point: [" + _Namespace + ":" + _ID + "] = " + currValueStr);
            return;
        }
        double newVal = 0.0;
        if (_Operation.equalsIgnoreCase("Add"))
        {
            newVal = doubleVal + _Value;
        }
        else if (_Operation.equalsIgnoreCase("Subtract"))
        {
            newVal = doubleVal - _Value;
        }
        else if (_Operation.equalsIgnoreCase("Multiply"))
        {
            newVal = doubleVal * _Value;
        }
        else
        {
            LOGGER.warning("Unknown Error processing Mathematic Task on Non numrice Data point: [" + _Namespace + ":" + _ID + "]");
            return;
        }
        int intVal = (int)newVal;
        TASKMAN.getDataMgr().ChangeValue(_ID, _Namespace, Integer.toString(intVal));
    }
}
