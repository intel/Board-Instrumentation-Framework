/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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

import kutch.biff.marvin.network.OscarBullhorn;

/**
 * @author Patrick
 */
public class OscarBindTask extends BaseTask {
    private String Address;
    private int Port;
    private String Key;

    public OscarBindTask(String address, int port, String hashStr) {
        Address = address;
        Port = port;
        Key = hashStr;
    }

    @Override
    public void PerformTask() {
        OscarBullhorn objBH = new OscarBullhorn(Address, Port, Key);
        objBH.SendNotification();
        LOGGER.info("Sending OscarBind to " + Address + ":" + Integer.toString(Port) + " Key:" + Key);
    }
}
