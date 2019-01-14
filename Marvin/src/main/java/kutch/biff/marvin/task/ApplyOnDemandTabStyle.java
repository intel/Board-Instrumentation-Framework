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

import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.DynamicItemInfoContainer;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandTabBuilder;
import kutch.biff.marvin.widget.widgetbuilder.OnDemandWidgetBuilder;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class ApplyOnDemandTabStyle extends BaseTask
{
    @Override
    public void PerformTask()
    {
        for (Pair<DynamicItemInfoContainer, OnDemandWidgetBuilder> entry : DataManager.getDataManager().getOnDemandList())
        {
            if (entry.getValue() instanceof OnDemandTabBuilder)
            {
                OnDemandTabBuilder tabBuilder = (OnDemandTabBuilder) entry.getValue();
                tabBuilder.ApplyOddEvenStyle();
            }
        }

    }
}
