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
package kutch.biff.marvin.widget;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import kutch.biff.marvin.datamanager.DataManager;

/**
 * @author Patrick Kutch
 */
public class SpacerWidget extends BaseWidget {
    // private final Rectangle _panel;
    private final Label _panel;

    public SpacerWidget() {
        // _panel = new Rectangle();
        _panel = new Label();
        if (CONFIG.isDebugMode()) {
            AddAdditionalStyleOverride("-fx-background-color:red;");
        }
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        if ((getWidth() == 0 && getWidthPercentOfParentGrid() == 0)
                || (getHeight() == 0 && getHeightPercentOfParentGrid() == 0)) {
            return true; // if it's got no height or width, just skip
        }
        SetParent(pane);
        ConfigureDimentions();
        ConfigureAlignment();

        pane.add(_panel, getColumn(), getRow(), getColumnSpan(), getRowSpan());
        SetupTaskAction();
        return ApplyCSS();
    }

    protected Label GetPanel() {
        return _panel;
    }

    @Override
    public Region getRegionObject() {
        return _panel;
    }

    @Override
    public javafx.scene.Node getStylableObject() {
        return _panel;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return _panel.getStylesheets();
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a Spacer to " + strTitle);
    }

    @Override
    public boolean ZeroDimensionOK() {
        return true;
    }

}
