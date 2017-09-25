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
import eu.hansolo.enzo.gauge.RectangularGauge.TickLabelOrientation;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Random;


/**
 * Created by hansolo on 10.12.15.
 */
public class DemoRectangularGauge extends Application {
    private static final Random           RND       = new Random();
    private static       int              noOfNodes = 0;
    private              RectangularGauge control;
    private              long             lastTimerCall;
    private              AnimationTimer   timer;


    @Override public void init() {
        control = RectangularGaugeBuilder.create()
                                         .title("Temperature")
                                         .unit("°C")
                                         .tickLabelOrientation(TickLabelOrientation.ORTHOGONAL)
                                         .ledVisible(true)
                                         .sections(new Section(0, 25, Color.rgb(0, 0, 255, 0.5)),
                                                   new Section(25, 50, Color.rgb(0, 255, 255, 0.5)),
                                                   new Section(50, 75, Color.rgb(0, 255, 0, 0.5)),
                                                   new Section(75, 100, Color.rgb(255, 255, 0, 0.5)))
                                         .sectionsVisible(true)
                                         .decimals(2)
                                         .build();

        lastTimerCall = System.nanoTime() + 50_000_000l;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now > lastTimerCall + 2_000_000_000l) {
                    control.setValue(RND.nextDouble() * 100);
                    control.setBlinking(control.getValue() > 50);
                    lastTimerCall = now;
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

        stage.setTitle("RectangularGauge");
        stage.setScene(scene);
        stage.show();

        timer.start();

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
