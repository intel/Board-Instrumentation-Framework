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
package kutch.biff.marvin;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.BaseTask;
import kutch.biff.marvin.version.Version;
import kutch.biff.marvin.widget.BaseWidget;

/**
 * @author Patrick Kutch
 */
public class AboutBox {
    private static Pane Setup(Stage stage) {
        // TaskManager TASKMAN = TaskManager.getTaskManager();
        ConfigurationReader CONFIG = ConfigurationReader.GetConfigReader();

        GridPane grid = new GridPane();
        URL resource = AboutBox.class.getResource("About.png");

        Image img = new Image(resource.toString());

        ImageView aboutImg = new ImageView(img);

        Button OKBtn = new Button("OK");
        Label By = new Label("by");
        Label Author = new Label("Patrick Kutch");
        Label With = new Label("with Brian Johnson");
        Label With2 = new Label("and Michael Shearer");

        Label Where = new Label("https://github.com/PatrickKutch");

        Label DataCount = new Label(
                "Datapoints: " + Integer.toString(DataManager.getDataManager().NumberOfRegisteredDatapoints()));

        Author.setAlignment(Pos.CENTER);
        GridPane.setHalignment(Author, HPos.CENTER);
        GridPane.setHalignment(By, HPos.CENTER);
        GridPane.setHalignment(With, HPos.CENTER);
        GridPane.setHalignment(With2, HPos.CENTER);
        GridPane.setHalignment(Where, HPos.CENTER);

        Label VerLabel = new Label(Version.getVersion());
        Label Widgets = new Label("Number of Widgets - " + Integer.toString(BaseWidget.getWidgetCount()));
        Label Tasks = new Label("Number of Tasks - " + Integer.toString(BaseTask.getTaskCount()));
        long freeMem = Runtime.getRuntime().freeMemory();
        long totalMem = Runtime.getRuntime().maxMemory();
        long usedMem = totalMem - freeMem;
        usedMem /= 1024.0;
        String MBMemStr = NumberFormat.getNumberInstance(Locale.US).format(usedMem / 1024);
        Label MemUsage = new Label("Mem usage (MB) - " + MBMemStr);

        int newBottom = 1;
        grid.setAlignment(Pos.CENTER);
        grid.add(aboutImg, 1, newBottom++);
        grid.add(By, 1, newBottom++);
        grid.add(Author, 1, newBottom++);
        grid.add(With, 1, newBottom++);
        grid.add(With2, 1, newBottom++);
        grid.add(Where, 1, newBottom++);
        grid.add(new Label(" "), 1, newBottom++);
        if (CONFIG.getConfiguration().GetApplicationID().length() > 0) {
            Label ID = new Label("ID : " + CONFIG.getConfiguration().GetApplicationID());
            grid.add(ID, 1, newBottom++);
        }
        grid.add(VerLabel, 1, newBottom++);
        grid.add(Widgets, 1, newBottom++);
        grid.add(Tasks, 1, newBottom++);
        grid.add(DataCount, 1, newBottom++);
        grid.add(MemUsage, 1, newBottom++);
        GridPane.setHalignment(OKBtn, HPos.CENTER);
        // grid.add(new Label(" "), 1, newBottom++);
        newBottom = AboutBox.SetupExtraInfoPane(grid, newBottom++, 1);

        Slider objSlider = new Slider(.25, 3, CONFIG.getConfiguration().getScaleFactor());
        objSlider.valueProperty().bindBidirectional(CONFIG.getConfiguration().getScaleProperty());
        grid.add(objSlider, 1, newBottom++);
        objSlider.setVisible(false);

        aboutImg.setOnMouseClicked(event -> {
            if (event.isShiftDown()) {
                objSlider.setVisible(true);
            }
        });

        grid.add(OKBtn, 1, newBottom);

        grid.setStyle(
                "-fx-padding: 5; -fx-background-color: cornsilk; -fx-border-width:5; -fx-border-color: linear-gradient(to bottom, chocolate, derive(chocolate, 50%));");

        OKBtn.setOnAction(t -> {
            stage.close();
        });

        // place on correct screen.
        int xPos = (int) (CONFIG.getConfiguration().getPrimaryScreen().getVisualBounds().getMinX());
        int yPos = (int) (CONFIG.getConfiguration().getPrimaryScreen().getVisualBounds().getMinY());
        stage.setX(xPos);
        stage.setY(yPos);
        stage.centerOnScreen(); // and center it.
        return grid;
    }

    private static int SetupExtraInfoPane(GridPane grid, int iStartRow, int column) {
        ConfigurationReader CONFIG = ConfigurationReader.GetConfigReader();
        // ConfigurationReader CONFIG = GetConfigReader();

        Label lblScaling;
        Label lblAppDimensions;
        Label lblVersionJVM;
        Label lblTabCount;

        Rectangle2D visualBounds = CONFIG.getConfiguration().getPrimaryScreen().getVisualBounds();
        int appWidth = (int) visualBounds.getWidth();
        int appHeight = (int) visualBounds.getHeight();

        String rtv = System.getProperty("java.runtime.version");
        if (null == rtv) {
            rtv = "1.1.0_11-b32"; // bogus one - should NEVER happen
        }

        lblTabCount = new Label("Number of Tabs: " + Integer.toString(CONFIG.getTabs().size()));
        String scalingString = "Scaling: ";
        if (CONFIG.getConfiguration().isAutoScale()) {
            scalingString += "[AutoScale] ";
        }
        scalingString += String.format("%.2f", CONFIG.getConfiguration().getScaleFactor());
        lblScaling = new Label(scalingString);

        CONFIG.getConfiguration().getScaleProperty().addListener((observableValue, number, number2) -> {
            String scalingStr = "Scaling: ";
            if (CONFIG.getConfiguration().isAutoScale()) {
                scalingStr += "[AutoScale] ";
            }
            scalingStr += String.format("%.2f", CONFIG.getConfiguration().getScaleFactor());
            lblScaling.setText(scalingStr);
        });

        lblAppDimensions = new Label("Screen Size: " + Integer.toString(appWidth) + "x" + Integer.toString(appHeight));
        lblVersionJVM = new Label("JVM Version - " + rtv);

        grid.add(lblTabCount, column, iStartRow++);
        grid.add(lblScaling, column, iStartRow++);
        grid.add(lblAppDimensions, column, iStartRow++);
        grid.add(lblVersionJVM, column, iStartRow++);

        return iStartRow;

    }

    public static void ShowAboutBox() {
        Stage dialog = new Stage();
        dialog.setTitle("About Marvin");
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setWidth(250);
        Scene scene = new Scene(AboutBox.Setup(dialog));

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
