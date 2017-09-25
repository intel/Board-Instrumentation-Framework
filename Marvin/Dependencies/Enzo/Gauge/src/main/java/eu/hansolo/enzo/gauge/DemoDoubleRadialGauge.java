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

package eu.hansolo.enzo.gauge;

import eu.hansolo.enzo.common.Section;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Random;


/**
 * Created by hansolo on 01.12.15.
 */
public class DemoDoubleRadialGauge extends Application {
    private static final Random RND       = new Random();
    private static       int    noOfNodes = 0;
    private DoubleRadialGauge   control;
    private long                lastTimerCallOne;
    private AnimationTimer      timerOne;
    private long                lastTimerCallTwo;
    private AnimationTimer      timerTwo;


    @Override public void init() {
        control = DoubleRadialGaugeBuilder.create()
                                          //.style("-body: #333333; -tick-mark-fill-one: white; -tick-label-fill-one: white; -tick-mark-fill-two: white; -tick-label-fill-two: white;")
                                          .titleOne("Title One")
                                          .unitOne("°C")
                                          .minValueOne(0).maxValueOne(100)
                                          .ledVisibleOne(true).ledColorOne(Color.RED)
                                          .needleColorOne(Color.DARKBLUE)
                                          .sectionsVisibleOne(true)
                                          .sectionsOne(new Section(0, 20, Color.rgb(200, 0, 0, 0.2)),
                                                       new Section(20, 40, Color.rgb(200, 0, 0, 0.4)),
                                                       new Section(40, 60, Color.rgb(200, 0, 0, 0.6)),
                                                       new Section(60, 80, Color.rgb(200, 0, 0, 0.8)),
                                                       new Section(80, 100, Color.rgb(200, 0, 0, 1.0)))
                                          .areasVisibleOne(true)
                                          .areasOne(new Section(80, 100, Color.rgb(200, 0, 0, 1.0)))
                                          .titleTwo("Title Two")
                                          .unitTwo("°F")
                                          .minValueTwo(0).maxValueTwo(100)
                                          .ledVisibleTwo(true).ledColorTwo(Color.BLUE)
                                          .needleColorTwo(Color.DARKRED)
                                          .sectionsVisibleTwo(true)
                                          .sectionsTwo(new Section(0, 20, Color.rgb(0, 0, 200, 0.2)),
                                                       new Section(20, 40, Color.rgb(0, 0, 200, 0.4)),
                                                       new Section(40, 60, Color.rgb(0, 0, 200, 0.6)),
                                                       new Section(60, 80, Color.rgb(0, 0, 200, 0.8)),
                                                       new Section(80, 100, Color.rgb(0, 0, 200, 1.0)))
                                          .areasVisibleTwo(true)
                                          .areasTwo(new Section(80, 100, Color.rgb(0, 0, 200, 1.0)))
                                          .build();

        lastTimerCallOne = System.nanoTime() + 50_000_000l;
        timerOne = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now > lastTimerCallOne + 4_200_000_000l) {
                    control.setValueOne(RND.nextDouble() * 100);
                    control.setBlinkingOne(control.getValueOne() > 50);
                    lastTimerCallOne = now;
                }
            }
        };
        lastTimerCallTwo = System.nanoTime() + 400_000_000l;
        timerTwo = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now > lastTimerCallTwo + 3_500_000_000l) {
                    double value = RND.nextDouble() * 100;
                    control.setValueTwo(value);
                    control.setBlinkingTwo(control.getValueTwo() > 70);
                    lastTimerCallTwo = now;
                }
            }
        };
    }

    @Override public void start(Stage stage) throws Exception {
        StackPane pane = new StackPane(control);
        pane.setPadding(new Insets(5, 5, 5, 5));

        final Scene scene = new Scene(pane, Color.BLACK);
        //scene.getStylesheets().add(getClass().getResource("demo.css").toExternalForm());
        //scene.setFullScreen(true);

        stage.setTitle("DoubleRadialGauge");
        stage.setScene(scene);
        stage.show();

        timerOne.start();
        timerTwo.start();

        calcNoOfNodes(scene.getRoot());
        System.out.println(noOfNodes + " Nodes in SceneGraph");
    }

    @Override public void stop() {

    }

    public static void main(final String[] args) {
        Application.launch(args);
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

