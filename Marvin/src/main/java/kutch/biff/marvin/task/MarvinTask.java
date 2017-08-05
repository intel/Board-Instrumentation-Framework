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
 * @author Patrick Kutch
 */
public class MarvinTask extends BaseTask
{
    class DataSet
    {
        public String ID;
        public String Namespace;
        public String Data;
    }
    private ArrayList<DataSet> _Dataset;
    public MarvinTask( )
    {
        _Dataset = null;
    }
    @Override
    public boolean getMustBeInGUIThread()
    {
        return true;
    }
    
    public boolean isValid()
    {
        return null != _Dataset;
    }
    
    public void AddDataset(String ID, String Namespace, String Data)
    {
        DataSet objSet = new DataSet();
        objSet.Data = Data;
        objSet.ID = ID;
        objSet.Namespace = Namespace;
        if (null == _Dataset)
        {
            _Dataset = new ArrayList<>();
        }
        _Dataset.add(objSet);
    }
    @Override
    public  void PerformTask()
    {
        if (null == TASKMAN.getDataMgr())
        {
            return;
        }
        if (null == _Dataset)
        {
            LOGGER.severe("Encounted null Dataset while performing a task - Did you declare a Marvin task but not add Data?");
            return;
        }
        for (DataSet data : _Dataset)
        {
            String strID = getDataValue(data.ID);
            String strNamespace = getDataValue(data.Namespace);
            String strData = getDataValue(data.Data);
            
            if (null != strID && null != strNamespace && null != strData)
            {
                TASKMAN.getDataMgr().ChangeValue(strID, strNamespace, strData);
            }
        }
    }
}
