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
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.task;

import java.util.ArrayList;

/**
 *
 * @author Patrick
 */
public class RandomTask extends BaseTask
{
    class RandomSet
    {
	
	public String TaskID;
	public double Weight;
    }
    
    private final TaskManager TASKMAN = TaskManager.getTaskManager();
    
    private ArrayList<RandomSet> _TaskList;
    private boolean _WieghtAdjusted;
    
    public RandomTask()
    {
	_TaskList = new ArrayList<>();
	_WieghtAdjusted = false;
    }
    
    public void AddTask(String strTaskID, double weight)
    {
	RandomSet objSet = new RandomSet();
	objSet.TaskID = strTaskID;
	objSet.Weight = weight;
	_TaskList.add(objSet);
    }
    
    private void AdjustWeight()
    {
	if (_WieghtAdjusted)
	{
	    return;
	}
	_WieghtAdjusted = true;
	double totalWeight = 0.0d;
	int notWeightedCount = 0;
	for (RandomSet objRandomSet : _TaskList)
	{
	    totalWeight += objRandomSet.Weight;
	    if (0 == objRandomSet.Weight)
	    {
		notWeightedCount++;
	    }
	}
	if (notWeightedCount > 0)
	{
	    double DefaultWeight = (101.0d - totalWeight) / notWeightedCount;
	    if (DefaultWeight < 1.0d)
	    {
		DefaultWeight = 1.0d;
	    }
	    for (RandomSet objRandomSet : _TaskList)
	    {
		if (0 == objRandomSet.Weight)
		{
		    objRandomSet.Weight = DefaultWeight;
		}
	    }
	}
	
    }
    
    @Override
    public void PerformTask()
    {
	double totalWeight = 0.0d;
	AdjustWeight();
	for (RandomSet objRandomSet : _TaskList)
	{
	    totalWeight += objRandomSet.Weight;
	}
// Now choose a random item
	int randomIndex = -1;
	double random = Math.random() * totalWeight;
	for (int index = 0; index < _TaskList.size(); ++index)
	{
	    random -= _TaskList.get(index).Weight;
	    if (random <= 0.0d)
	    {
		randomIndex = index;
		break;
	    }
	}
	String strTaskToRun = _TaskList.get(randomIndex).TaskID;
	TASKMAN.PerformTask(strTaskToRun);
    }
    
}
