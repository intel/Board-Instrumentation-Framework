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
package kutch.biff.marvin.utility;

import javafx.scene.chart.XYChart;

/**
 * @author Patrick Kutch
 */
public class SeriesDataSet {
    private String ID;
    private String Title;
    private String Namespace;
    @SuppressWarnings("rawtypes")
    private XYChart.Series _Series;
    private double _ScaleValue;

    public SeriesDataSet(String title, String id, String namespace) {
        Title = title;
        ID = id;
        Namespace = namespace;
        _Series = new XYChart.Series<>();
        _Series.setName(Title);
        _ScaleValue = 1.0;
    }

    public String getHashKey() {
        return Namespace.toUpperCase() + ID.toUpperCase();
    }

    public String getID() {
        return ID;
    }

    public String getNamespace() {
        return Namespace;
    }

    public double getScaleValue() {
        return _ScaleValue;
    }

    @SuppressWarnings("rawtypes")
    public XYChart.Series getSeries() {
        return _Series;
    }

    public String getTitle() {
        return Title;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setNamespace(String Namespace) {
        this.Namespace = Namespace;
    }

    public void setScaleValue(double dVal) {
        _ScaleValue = dVal;
    }
}
