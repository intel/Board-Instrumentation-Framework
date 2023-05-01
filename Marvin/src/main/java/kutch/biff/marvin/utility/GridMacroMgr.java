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
package kutch.biff.marvin.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick
 */
public class GridMacroMgr {
    private final static GridMacroMgr _Mgr = new GridMacroMgr();
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static GridMacroMgr getGridMacroMgr() {
        return _Mgr;
    }

    @SuppressWarnings("rawtypes")
    private final ArrayList<Map> _GridMacroList;

    private GridMacroMgr() {
        _GridMacroList = new ArrayList<>();
    }

    public boolean AddGridMacro(String nameMacro, FrameworkNode macroNode) {
        if (null == nameMacro) {
            LOGGER.severe("Tried to add GridMacro that has null name.");
            return false;
        }
        if (null == macroNode) {
            LOGGER.severe("Tried to add GridMacro that is null.");
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, FrameworkNode> map = _GridMacroList.get(0);
        map.put(nameMacro.toUpperCase(), macroNode);

        return true;
    }

    public FrameworkNode getGridMacro(String strMacro) {
        String strName = strMacro.toUpperCase();
        for (Map<?, ?> map : _GridMacroList) {
            if (map.containsKey(strName)) {
                return (FrameworkNode) map.get(strName);
            }
        }

        return null;
    }

    public boolean macroExists(String strMacro) {
        String strName = strMacro.toUpperCase();
        for (Map<?, ?> map : _GridMacroList) {
            if (map.containsKey(strName)) {
                return true;
            }
        }
        return false;
    }

    public void PopGridMacroList() {
        _GridMacroList.remove(0);
    }

    public void PushGridMacroList() {
        _GridMacroList.add(0, new HashMap<>()); // put in position 0

    }
}
