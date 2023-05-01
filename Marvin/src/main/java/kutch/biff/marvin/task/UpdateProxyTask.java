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

import kutch.biff.marvin.datamanager.DataManager;

/**
 * @author Patrick.Kutch@gmail.com
 */

public class UpdateProxyTask extends BaseTask {
    private String _ProxyID;
    private String _NewNamespaceCriterea = null;
    private String _NewIDCriterea = null;
    private String _newListEntry = null;

    public UpdateProxyTask(String proxyID) {
        _ProxyID = proxyID;
    }

    @Override
    public void PerformTask() {
        DataManager.getDataManager().UpdateGenerateDatapointProxy(getDataValue(_ProxyID),
                getDataValue(_NewNamespaceCriterea), getDataValue(_NewIDCriterea), getDataValue(_newListEntry));
    }

    public void setIDMask(String newID) {
        _NewIDCriterea = newID;
    }

    public void setListEntry(String newEntry) {
        _newListEntry = newEntry;
    }

    public void setNamespaceMask(String newNS) {
        _NewNamespaceCriterea = newNS;
    }
}
