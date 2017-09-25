/*
 * Copyright (c) 2015 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.enzo.validationpane;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


/**
 * Created by
 * User: hansolo
 * Date: 09.04.13
 * Time: 19:15
 */
public class Demo extends Application {
    private ValidationPane            validationPane;
    private TextField                 field1;
    private TextField                 field2;
    private TextField                 field3;
    private TextField                 field4;
    private TextField                 field5;
    private ToggleButton              button1;
    private ToggleButton              button2;
    private ToggleButton              button3;
    private ToggleButton              button4;
    private ToggleButton              button5;
    private CheckBox                  checkBox5;
    private EventHandler<ActionEvent> handler;


    @Override public void init() {
        handler = actionEvent -> {
        Object SRC = actionEvent.getSource();
        if (SRC.equals(button1)) {
            if (button1.isSelected()) {
                button1.setText("valid");
                validationPane.setState(field1, Validator.State.VALID);
            } else {
                button1.setText("invalid");
                validationPane.setState(field1, Validator.State.INVALID, "text is invalid");
            }
        } else if (SRC.equals(button2)) {
            if (button2.isSelected()) {
                button2.setText("valid");
                validationPane.setState(field2, Validator.State.VALID);
            } else {
                button2.setText("invalid");
                validationPane.setState(field2, Validator.State.INVALID, "value is invalid");
            }
        } else if (SRC.equals(button3)) {
            if (button3.isSelected()) {
                button3.setText("info");
                validationPane.setState(field3, Validator.State.INFO, "this is an info for you");
            } else {
                button3.setText("none");
                validationPane.setState(field3, Validator.State.CLEAR);
            }
        } else if (SRC.equals(button4)) {
            if (button4.isSelected()) {
                button4.setText("optional");
                validationPane.setState(field4, Validator.State.OPTIONAL);
            } else {
                button4.setText("none");
                validationPane.setState(field4, Validator.State.CLEAR);
            }
        } else if (SRC.equals(button5)) {
            if (button5.isSelected()) {
                button5.setText("info");
                validationPane.setState(field5, Validator.State.INFO);
            } else {
                button5.setText("none");
                validationPane.setState(field5, Validator.State.CLEAR);
            }
        } else if (SRC.equals(checkBox5)) {
            field5.setVisible(!checkBox5.isSelected());
        }
    };

        field1 = new TextField();
        field1.setPromptText("text1");
        field2 = new TextField();
        field2.setPromptText("text2");
        field3 = new TextField();
        field3.setPromptText("text3");
        field4 = new TextField();
        field4.setPromptText("text4");
        field5 = new TextField();
        field5.setPromptText("text5");

        button1 = new ToggleButton("invalid");
        button1.setPrefWidth(100);
        button1.setOnAction(handler);
        button2 = new ToggleButton("invalid");
        button2.setPrefWidth(100);
        button2.setOnAction(handler);
        button3 = new ToggleButton("none");
        button3.setPrefWidth(100);
        button3.setOnAction(handler);
        button4 = new ToggleButton("none");
        button4.setPrefWidth(100);
        button4.setOnAction(handler);
        button5 = new ToggleButton("none");
        button5.setPrefWidth(100);
        button5.setOnAction(handler);
        checkBox5 = new CheckBox("invisible");
        checkBox5.setPrefWidth(100);
        checkBox5.setOnAction(handler);

        validationPane = new ValidationPane();
        validationPane.addAll(field1, field2, field3, field4, field5);

        validationPane.infoTextProperty(field1).addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> ov, String oldInfoText, String newInfoText) {
                System.out.println("InfoText of field1 changed to: " + newInfoText);
            }
        });

        // React on Validation events fired by the ValidationPane
        validationPane.setOnValid(validationEvent -> {
            System.out.println(validationEvent.getNode() + " is now valid (" + validationEvent.getInfoText() + ")");
        });
        validationPane.setOnInvalid(validationEvent -> {
            System.out.println(validationEvent.getNode() + " is now invalid (" + validationEvent.getInfoText() + ")");
        });
        validationPane.setOnInfo(validationEvent -> {
            System.out.println(validationEvent.getNode() + " is now info (" + validationEvent.getInfoText() + ")");
        });
        validationPane.setOnOptional(validationEvent -> {
            System.out.println(validationEvent.getNode() + " is now optional (" + validationEvent.getInfoText() + ")");
        });
        validationPane.setOnClear(validationEvent -> {
            System.out.println(validationEvent.getNode() + " is now cleared (" + validationEvent.getInfoText() + ")");
        });
    }

    @Override public void start(Stage stage) {
        HBox row1 = new HBox();
        row1.setSpacing(10);
        row1.getChildren().addAll(new Label("Label 1"), field1, button1);

        HBox row2 = new HBox();
        row2.setSpacing(10);
        row2.getChildren().addAll(new Label("Label 2"), field2, button2);

        HBox row3 = new HBox();
        row3.setSpacing(10);
        row3.getChildren().addAll(new Label("Label 3"), field3, button3);

        HBox row4 = new HBox();
        row4.setSpacing(10);
        row4.getChildren().addAll(new Label("Label 4"), field4, button4);

        HBox row5 = new HBox();
        row5.setSpacing(10);
        row5.getChildren().addAll(new Label("Label 5"), field5, button5, checkBox5);


        VBox col = new VBox();
        col.setSpacing(20);
        col.setPadding(new Insets(10, 10, 10, 10));
        col.getChildren().addAll(row1, row2, row3, row4, row5);

        StackPane pane = new StackPane();
        pane.getChildren().addAll(col, validationPane);

        Scene scene = new Scene(pane, Color.DARKGRAY);

        stage.setTitle("Validation overlay");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
