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
 * #  Utility class that performs the task of sending notifications to Oscars
 * ##############################################################################
 */

package kutch.biff.marvin.task;

import kutch.biff.marvin.configuration.Configuration;

/**
 * @author Patrick
 */
public class OscarBullhornTask extends BaseTask {
    public OscarBullhornTask() {

    }

    @Override
    public void PerformTask() {
        Configuration.getConfig().getOscarBullhornList().stream().forEach((objBullhorn) ->
        {
            objBullhorn.SendNotification();
        });
    }
}
