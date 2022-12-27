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
 * #    Handles SVG (Scalable Vector Graphics) widget;
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.widget;

import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */

public class SVG_Widget extends TextWidget {
    private String _strShape = null;

    public SVG_Widget() {

    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        if (null == _strShape) {
            LOGGER.severe("No shape defined for SVG Shape Widget");
            return false;
        }
        String strShape = "-fx-shape:\"" + _strShape + "\"";
        AddAdditionalStyleOverride(strShape);

        return super.Create(pane, dataMgr);
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if (node.getNodeName().equalsIgnoreCase("Shape")) {
            SetShape(node.getTextContent());
            return true;
        }
        return false;
    }

    public void SetShape(String strShape) {
        _strShape = strShape;
    }
}
