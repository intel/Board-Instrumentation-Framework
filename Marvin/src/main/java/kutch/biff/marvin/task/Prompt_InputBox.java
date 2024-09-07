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

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import kutch.biff.marvin.configuration.Configuration;
 
/**
 * @author Patrick Kutch
 */
/**
 * Represents a prompt input box for user input.
 * Extends the BasePrompt class.
 */
public class Prompt_InputBox extends BasePrompt {

    private String _PrevVal = null;

    public Prompt_InputBox(String ID) {
        super(ID);
    }

    /**
     * Sets up the dialog for prompting user input.
     * 
     * @param dialog The stage for the dialog window.
     * @return The root pane of the dialog.
     */
    @Override
    protected Pane SetupDialog(Stage dialog) {
        GridPane root = new GridPane();
        root.setHgap(5.0);
        root.setVgap(5.0);
        root.setPadding(new Insets(5, 5, 2, 5));
        Button btn = new Button();
        btn.setText("OK");
        Label lblMessage = new Label(getMessage());
        Label lblSpacer = new Label("  ");
        lblMessage.setWrapText(true);
        TextField objPrompt = new TextField();
        if (null != _PrevVal) {
            objPrompt.setText(_PrevVal);
        }
        root.setAlignment(Pos.CENTER_LEFT);

        GridPane.setColumnSpan(lblMessage, 2);
        GridPane.setHalignment(btn, HPos.CENTER);

        if (getWidth() > 0) {
            objPrompt.setPrefWidth(getWidth());
        }
        if (getHeight() > 0) {
            root.setPrefHeight(getHeight());
        }

        root.add(lblMessage, 0, 0);
        root.add(objPrompt, 0, 2);
        root.add(btn, 0, 3);
        root.add(lblSpacer, 0, 4);

        // place on correct screen and center
        int xPos = (int) (Configuration.getConfig().getPrimaryScreen().getVisualBounds().getMinX());
        int yPos = (int) (Configuration.getConfig().getPrimaryScreen().getVisualBounds().getMinY());
        dialog.setX(xPos);
        dialog.setY(yPos);

        dialog.centerOnScreen();

        objPrompt.setOnKeyPressed(ke -> {
            KeyCode kc = ke.getCode();
            if (kc.equals(KeyCode.ENTER)) {
                SetPromptedValue(objPrompt.getText());
                _PrevVal = objPrompt.getText();
                dialog.close();
            }
        });

        btn.setOnAction((ActionEvent event) ->
        {
            SetPromptedValue(objPrompt.getText());
            _PrevVal = objPrompt.getText();
            dialog.close();
        });

        return root;
    }

}
