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
 * #    Simply 'pulses' an existing data point.  This can be used to cause a
 * #    conditional to be re-evaluated on a regular basis
 * ##############################################################################
 */
package kutch.biff.marvin.task;

/**
 *
 * @author Patrick
 */
public class PulseTask extends BaseTask
{
    protected String _ID;
    protected String _Namespace;

    public PulseTask()
    {
        _ID = null;
        _Namespace = null;
    }
    
    public void SetNamespaceAndID(String ns, String ID)
    {
        _ID = ID;
        _Namespace = ns;
    }
    
    public boolean isValid()
    {
        return (_ID != null && _Namespace != null);
    }

    @Override
    public void PerformTask()
    {
        String currValue;
        currValue = TASKMAN.getDataMgr().GetValue(_ID, _Namespace);
        if (null == currValue)
        {
            LOGGER.warning("Pulse Task failed [" + getTaskID() +"] beause the data point does not exist (yet).");
        }
        else
        {
            TASKMAN.getDataMgr().ChangeValue(_ID, _Namespace, currValue);
        }
    }
}
