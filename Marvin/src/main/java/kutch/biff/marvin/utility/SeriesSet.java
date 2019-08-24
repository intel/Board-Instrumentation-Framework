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

import java.util.ArrayList;
import java.util.logging.Logger;

import javafx.scene.chart.XYChart;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick Kutch
 */
public class SeriesSet
{
    @SuppressWarnings("unused")
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private ArrayList<SeriesDataSet> _MinionSrcList;
    @SuppressWarnings("rawtypes")
    private XYChart.Series _Series;
    private String _Title;
    
    @SuppressWarnings("rawtypes")
    public SeriesSet(String strTitle)
    {
        _MinionSrcList = new ArrayList<>();

        _Series = new XYChart.Series();
        _Series.setName(strTitle);
                
        _Title = strTitle;
    }
    
    /**
     *
     * @param objSeries
     */
    @SuppressWarnings("unchecked")
    public void AddSeries(SeriesDataSet objSeries)
    {
        _MinionSrcList.add(objSeries);
        _Series.getData().add(new XYChart.Data<>(objSeries.getTitle(),110000.0));
    }

    @SuppressWarnings("rawtypes")
    public XYChart.Series getSeries()
    {
        return _Series;
    }

    public ArrayList<SeriesDataSet> getSeriesList()
    {
        return _MinionSrcList;
    }

    public String getTitle()
    {
        return _Title;
    }
   
}
