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
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Random;


/**
 * Created by hansolo on 20.10.15.
 */
public class DemoFlatGauge extends Application {
    private static final Random         RND       = new Random();
    private static       int            noOfNodes = 0;
    private              FlatGauge      gauge;
    private              long           lastTimerCall;
    private              AnimationTimer timer;

    @Override public void init() {
        gauge = FlatGaugeBuilder.create()
                                .prefSize(600, 600)
                                .title("Temperature")
                                .unit("°C")
                                .minValue(0)
                                .maxValue(100)
                                .backgroundColor(Color.TRANSPARENT)
                                .barColor(Color.CYAN)
                                .animationDuration(2000)
                                .build();

        lastTimerCall = System.nanoTime() + 3_000_000_000l;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now > lastTimerCall + 5_000_000_000l) {
                    gauge.setValue(RND.nextDouble() * (gauge.getMaxValue() - gauge.getMinValue()) + gauge.getMinValue());
                    lastTimerCall = now;
                }
            }
        };
    }

    @Override public void start(Stage stage) throws Exception {
        HBox pane = new HBox();
        pane.setPadding(new Insets(10, 10, 10, 10));
        pane.setSpacing(10);
        pane.getChildren().addAll(gauge);
        pane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        final Scene scene = new Scene(pane, Color.WHITE);
        //scene.setFullScreen(true);

        stage.setTitle("FlatGauge");
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
