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

import java.util.Random;

/**
 * @author Patrick Kutch
 */
public class OscarTask extends BaseTask {
    private String _OscarID;
    private String _TaskID;
    private Random rnd = new Random();

    public OscarTask() {
        _OscarID = null;
    }

    public String getOscarID() {
        return _OscarID;
    }

    @Override
    public String getTaskID() {
        return _TaskID;
    }

    @Override
    public void PerformTask() {
        String strOscarID = getDataValue(_OscarID);
        String strTaskID = getDataValue(getTaskID());
        String logParams = "";

        if (null == strOscarID || null == strTaskID) {
            return;
        }
        String sendBuffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        sendBuffer += "<Marvin Type=\"OscarTask\">";
        sendBuffer += "<Version>1.0</Version>";
        sendBuffer += "<OscarID>" + strOscarID + "</OscarID>";
        sendBuffer += "<UniqueID>" + Integer.toString(rnd.nextInt()) + "</UniqueID>";
        sendBuffer += "<Task>" + strTaskID + "</Task>";

        if (null != getParams()) {
            for (Parameter param : getParams()) {
                String strParam = param.toString();
                if (null == strParam) {
                    return;
                }

                sendBuffer += "<Param>" + strParam + "</Param>";
                logParams += strParam + " ";
            }
        }
        sendBuffer += "</Marvin>";

        if (strOscarID.equalsIgnoreCase("Broadcast")) {
            TASKMAN.SendToAllOscars(sendBuffer.getBytes());
            LOGGER.info("Sending broadcast Oscar Task to Oscar");
        } else {
            TASKMAN.SendToOscar(getDataValue(strOscarID), sendBuffer.getBytes());
            LOGGER.info("Sending Oscar Task to Oscar[" + _OscarID + ":" + getTaskID() + "] " + logParams);
        }
    }

    public void setOscarID(String _OscarID) {
        this._OscarID = _OscarID;
    }

    @Override
    public void setTaskID(String _TaskID) {
        this._TaskID = _TaskID;
    }
}
