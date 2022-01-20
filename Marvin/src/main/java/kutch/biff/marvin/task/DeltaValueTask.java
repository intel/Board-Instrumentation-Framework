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

public class DeltaValueTask extends PulseTask {
   
    private String  Value1_ID,Value2_ID;
    private String  Value1_NS,Value2_NS;
    private boolean validVal = false;
    private String _Operation = null;
    
    public DeltaValueTask()
    {
        
    }
    
    public void SetFirstDatapoint(String NS, String ID)
    {
        Value1_ID = ID;
        Value1_NS = NS;
    }    
    
    public void SetSecondDatapoint(String NS, String ID)
    {
        Value2_ID = ID;
        Value2_NS = NS;
    }    
    
    @Override
    public boolean isValid()
    {
        validVal = Value1_ID != null && Value2_ID != null && Value2_NS != null && Value1_NS != null;
        
	return (super.isValid() && validVal && _Operation != null);
    }
    @Override
    public void PerformTask()
    {
	String Value1Str,Value2Str;
	double doubleVal_1,doubleVal_2;
	
	synchronized (TASKMAN.getDataMgr())
	{
	    Value1Str = TASKMAN.getDataMgr().GetValueForMath(Value1_ID, Value1_NS);
	    Value2Str = TASKMAN.getDataMgr().GetValueForMath(Value2_ID, Value2_NS);
	    if (null == Value1Str || null == Value2Str)
	    {
		LOGGER.warning(
			"DeltaValue Task failed [" + getTaskID() + "] beause the data point does not exist (yet).");
		return;
	    }
	    
	    try
	    {
		doubleVal_1 = Double.parseDouble(Value1Str);
	    }
	    catch(NumberFormatException ex)
	    {
		LOGGER.warning("Attempted DeltaValue Task on Non numrice Data point: [" + Value1_NS + ":" + Value1_ID
			+ "] = " + Value1Str);
		return;
	    }
	    try
	    {
		doubleVal_2 = Double.parseDouble(Value2Str);
	    }
	    catch(NumberFormatException ex)
	    {
		LOGGER.warning("Attempted DeltaValue Task on Non numrice Data point: [" + Value2_NS + ":" + Value2_ID
			+ "] = " + Value2Str);
		return;
	    }
	    double newVal = 0.0;
	    if (_Operation.equalsIgnoreCase("Difference"))
	    {
		newVal = Math.abs(doubleVal_1 - doubleVal_2);
	    }
	    else if (_Operation.equalsIgnoreCase("PercentDifference"))
	    {
		double diff = doubleVal_1 - doubleVal_2;
                
                newVal = diff/doubleVal_1;
                
                newVal = newVal * -100;
	    }
	    else if (_Operation.equalsIgnoreCase("FactorDifference"))
	    {
		double diff = doubleVal_1 - doubleVal_2;
                
                newVal = diff/doubleVal_1 * -1;
	    }
	    else if (_Operation.equalsIgnoreCase("PercentDifferenceAbs"))
	    {
		double diff = Math.abs(doubleVal_1 - doubleVal_2);
                
                newVal = diff/doubleVal_1;
                
                newVal = newVal * 100;
	    }
	    else if (_Operation.equalsIgnoreCase("FactorDifferenceAbs"))
	    {
		double diff = Math.abs(doubleVal_1 - doubleVal_2);
                
                newVal = diff/doubleVal_1;
	    }
	    else
	    {
		LOGGER.warning("Unknown Error processing DeltaValue Task on Non numrice Data point: [" + _Namespace
			+ ":" + _ID + "]");
		return;
	    }
            
	    TASKMAN.getDataMgr().ChangeValue(_ID, _Namespace, Double.toString(newVal));
	}
    }    
    public boolean SetOperation(String strOper)
    {
	if (strOper.equalsIgnoreCase("Difference") || strOper.equalsIgnoreCase("PercentDifference") || strOper.equalsIgnoreCase("FactorDifference"))
	{
	    _Operation = strOper;
	}
	
	return (null != _Operation);
    }
    
}
