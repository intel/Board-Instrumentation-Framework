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

package eu.hansolo.enzo.lcd;

import eu.hansolo.enzo.common.ValueEvent;
import eu.hansolo.enzo.lcd.Lcd.LcdDesign;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Random;


public class DemoLcd extends Application {
    private static final Random RND = new Random();
    private static int          noOfNodes = 0;
    private Lcd                 control;
    private long                lastTimerCall;
    private double              charge;
    private int                 styleClassCounter;
    private AnimationTimer      timer;

    @Override public void init() {
        control = LcdBuilder.create()
                            .minSize(128, 40)
                            .maxSize(1280, 400)
                            .prefWidth(640)
                            .prefHeight(200)
                            .keepAspect(true)
                            .lcdDesign(LcdDesign.GREEN_DARKGREEN)
                            .foregroundShadowVisible(true)
                            .crystalOverlayVisible(true)
                            .title("Room Temp")
                            .batteryVisible(true)
                            .signalVisible(true)
                            .alarmVisible(true)
                            .unit("°C")
                            .unitVisible(true)
                            .decimals(3)
                            .animationDurationInMs(1500)
                            .minMeasuredValueDecimals(2)
                            .minMeasuredValueVisible(true)
                            .maxMeasuredValueDecimals(2)
                            .maxMeasuredValueVisible(true)
                            .formerValueVisible(true)
                            .threshold(26)
                            .thresholdVisible(true)
                            .trendVisible(true)
                            .numberSystemVisible(false)
                            .lowerRightTextVisible(true)
                            .lowerRightText("Info")
                            .minValue(-100)
                            .maxValue(100)
                            //.valueFont(Lcd.LcdFont.ELEKTRA)
                            .valueFont(Lcd.LcdFont.LCD)
                            .animated(true)
                            //.value(30)
                            .build();

        control.addEventHandler(ValueEvent.VALUE_EXCEEDED, valueEvent -> System.out.println("exceeded"));

        charge = 0.0;
        styleClassCounter = 0;
        lastTimerCall = System.nanoTime();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now > lastTimerCall + 5_000_000_000l) {
                    styleClassCounter ++;
                    if (styleClassCounter >= LcdDesign.values().length) {
                        styleClassCounter = 0;
                        control.setMainInnerShadowVisible(true);
                        control.setForegroundShadowVisible(true);
                        control.setCrystalOverlayVisible(true);
                    }
                    control.getStyleClass().setAll("lcd", LcdDesign.values()[styleClassCounter].STYLE_CLASS);
                    //System.out.println(control.getStyleClass());
                    double value = RND.nextDouble() * 200 - 100d;
                    //System.out.println(value);
                    control.setValue(value);
                    control.setTrend(Lcd.Trend.values()[RND.nextInt(5)]);
                    charge += 0.02;
                    if (charge > 1.0) charge = 0.0;
                    control.setBatteryCharge(charge);
                    control.setSignalStrength(charge);
                    if (styleClassCounter > 34) {
                        control.setMainInnerShadowVisible(false);
                        control.setForegroundShadowVisible(false);
                        control.setCrystalOverlayVisible(false);
                    }
                    lastTimerCall = now;
                }
            }
        };
    }

    @Override public void start(Stage stage) {
        StackPane pane = new StackPane();
        pane.setPadding(new Insets(10, 10, 10, 10));
        pane.getChildren().setAll(control);
        pane.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));

        Scene scene = new Scene(pane);

        stage.setTitle("Lcd demo");
        stage.centerOnScreen();
        //stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();        
                
        timer.start();

        calcNoOfNodes(scene.getRoot());
        System.out.println(noOfNodes + " Nodes in SceneGraph");
    }

    public static void main(String[] args) {
        launch(args);
    }


    // ******************** Misc **********************************************
    private static void calcNoOfNodes(Node node) {
        if (node instanceof Parent) {
            if (((Parent) node).getChildrenUnmodifiable().size() != 0) {
                ObservableList<Node> tempChildren = ((Parent) node).getChildrenUnmodifiable();
                noOfNodes += tempChildren.size();
                for (Node n : tempChildren) {
                    calcNoOfNodes(n);
                    //System.out.println(n.getStyleClass().toString());
                }
            }
        }
    }
}


