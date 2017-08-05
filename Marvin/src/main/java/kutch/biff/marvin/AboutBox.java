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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.BaseTask;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.version.Version;
import kutch.biff.marvin.widget.BaseWidget;

/**
 *
 * @author Patrick Kutch
 */
public class AboutBox
{

    public static void ShowAboutBox()
    {
        Stage dialog = new Stage();
        dialog.setTitle("About Marvin");
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setWidth(250);
        Scene scene = new Scene(AboutBox.Setup(dialog));

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static Pane Setup(Stage stage)
    {
        TaskManager TASKMAN = TaskManager.getTaskManager();
        ConfigurationReader CONFIG = ConfigurationReader.GetConfigReader();

        GridPane grid = new GridPane();
        URL resource = AboutBox.class.getResource("About.png");

        Image img = new Image(resource.toString());

        ImageView aboutImg = new ImageView(img);

        Button OKBtn = new Button("OK");
        Label By = new Label("by");
        Label Author = new Label("Patrick Kutch");
        Label With = new Label("with Brian Johnson");
       
        Label Where = new Label("https://github.com/PatrickKutch");
        Label strJVM = new Label(System.getProperty("java.version"));
        Label DataCount = new Label("Datapoints: " + Integer.toString(DataManager.getDataManager().NumberOfRegisteredDatapoints()));

        Author.setAlignment(Pos.CENTER);
        GridPane.setHalignment(Author, HPos.CENTER);
        GridPane.setHalignment(By, HPos.CENTER);
        GridPane.setHalignment(With, HPos.CENTER);
        GridPane.setHalignment(Where, HPos.CENTER);

        Label VerLabel = new Label(Version.getVersion());
        Label Widgets = new Label("Number of Widgets - " + Integer.toString(BaseWidget.getWidgetCount()));
        Label Tasks = new Label("Number of Tasks - " + Integer.toString(BaseTask.getTaskCount()));
        if (CONFIG.getConfiguration().GetApplicationID().length()>0)
        {
            Label ID = new Label("ID : " + CONFIG.getConfiguration().GetApplicationID());
            grid.add(ID, 1, 6);
        }
        grid.setAlignment(Pos.CENTER);
        grid.add(aboutImg, 1, 0);
        grid.add(By, 1, 1);
        grid.add(Author, 1, 2);
        grid.add(With, 1, 4);
        grid.add(Where, 1, 5);
        grid.add(new Label(" "), 1, 6);
        grid.add(VerLabel, 1, 10);
        grid.add(Widgets, 1, 11);
        grid.add(Tasks, 1, 12);
        grid.add(DataCount, 1, 13);
        GridPane.setHalignment(OKBtn, HPos.CENTER);
        grid.add(new Label(" "), 1, 14);
        int newBottom = AboutBox.SetupExtraInfoPane(grid, 14, 1);

        Slider objSlider = new Slider(.25, 3, CONFIG.getConfiguration().getScaleFactor());
        objSlider.valueProperty().bindBidirectional(CONFIG.getConfiguration().getScaleProperty());
        grid.add(objSlider, 1, newBottom++);
        objSlider.setVisible(false);

        aboutImg.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (event.isShiftDown())
                {
                    objSlider.setVisible(true);
                }
            }
        });

        grid.add(OKBtn, 1, newBottom);

        grid.setStyle("-fx-padding: 5; -fx-background-color: cornsilk; -fx-border-width:5; -fx-border-color: linear-gradient(to bottom, chocolate, derive(chocolate, 50%));");

        OKBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent t)
            {
                stage.close();
            }
        });
        return grid;
    }

    private static int SetupExtraInfoPane(GridPane grid, int iStartRow, int column)
    {
        TaskManager TASKMAN = TaskManager.getTaskManager();
        ConfigurationReader CONFIG = ConfigurationReader.GetConfigReader();
        //ConfigurationReader CONFIG = GetConfigReader();

        Label lblScaling;
        Label lblAppDimensions;
        Label lblVersionJVM;
        Label lblTabCount;

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        int appWidth = (int) visualBounds.getWidth();
        int appHeight = (int) visualBounds.getHeight();

        String foo = System.getProperty("java.runtime.version");
        String[] javaVersionElements = System.getProperty("java.runtime.version").split("\\.|_|-b");

        String discard = javaVersionElements[0];
        String major = javaVersionElements[1];
        String minor = javaVersionElements[2];
        String update = javaVersionElements[3];
//String build   = javaVersionElements[4];        

        lblTabCount = new Label("Number of Tabs: " + Integer.toString(CONFIG.getTabs().size()));
        String scalingString = "Scaling: ";
        if (CONFIG.getConfiguration().isAutoScale())
        {
            scalingString += "[AutoScale] ";
        }
        scalingString += String.format("%.2f", CONFIG.getConfiguration().getScaleFactor());
        lblScaling = new Label(scalingString);
        
        CONFIG.getConfiguration().getScaleProperty().addListener(new ChangeListener<Number>() // when the scale changes
        {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2)
            {
                String scalingString = "Scaling: ";
                if (CONFIG.getConfiguration().isAutoScale())
                {
                    scalingString += "[AutoScale] ";
                }
                scalingString += String.format("%.2f", CONFIG.getConfiguration().getScaleFactor());
                lblScaling.setText(scalingString);
            }
        });

        
        lblAppDimensions = new Label("Screen Size: " + Integer.toString(appWidth) + "x" + Integer.toString(appHeight));
        lblVersionJVM = new Label("JVM Version - " + foo);

        grid.add(lblTabCount, column, iStartRow++);
        grid.add(lblScaling, column, iStartRow++);
        grid.add(lblAppDimensions, column, iStartRow++);
        grid.add(lblVersionJVM, column, iStartRow++);

        return iStartRow;

    }
}
