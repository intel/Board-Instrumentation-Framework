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
package kutch.biff.marvin.widget;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class ToggleButtonWidget extends ButtonWidget {
    private ToggleButton _Button;
    private String _UnToggleTask = null;

    public ToggleButtonWidget() {
        _Button = new ToggleButton();
    }

    @Override
    protected ButtonBase getButton() {
        return _Button;
    }

    @Override
    public void HandleWidgetSpecificAttributes(FrameworkNode widgetNode) {
        if (widgetNode.hasAttribute("ToggleTask")) {
            _UnToggleTask = widgetNode.getAttribute("ToggleTask");
        }

    }

    @Override
    public void mouseHandler(MouseEvent event) {
        BaseWidget objWidget = this;

        if (CONFIG.isDebugMode() && event.isShiftDown()) {
            LOGGER.info(objWidget.toString(true));
        } else if (CONFIG.isDebugMode() && event.isControlDown()) {
            if (null != getStylableObject()) {
                AddAdditionalStyleOverride(DebugStyles.GetNext());
                ApplyCSS();
            }
        } else if (null != getTaskID() && true == CONFIG.getAllowTasks()) {
            if (_Button.isSelected()) {
                TASKMAN.PerformTask(getTaskID());
            } else {
                if (null != _UnToggleTask) {
                    TASKMAN.PerformTask(_UnToggleTask);
                }
            }
        }
    }

}
