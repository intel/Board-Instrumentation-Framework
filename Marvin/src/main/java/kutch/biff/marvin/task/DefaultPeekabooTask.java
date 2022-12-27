/*
 * ##############################################################################
 * #  Copyright (c) 2017 Intel Corporation
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

import kutch.biff.marvin.widget.BaseWidget;

/**
 * @author Patrick
 */
public class DefaultPeekabooTask extends BaseTask {

    private BaseWidget _objWidget;

    public DefaultPeekabooTask(BaseWidget objWidget) {
        _objWidget = objWidget;
    }

    @Override
    public boolean getMustBeInGUIThread() {
        return true;
    }

    @Override
    public void PerformTask() {
        _objWidget.HandleDefaultPeekaboo();
    }

}
