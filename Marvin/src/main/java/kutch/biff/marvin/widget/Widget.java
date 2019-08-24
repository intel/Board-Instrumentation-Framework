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
package kutch.biff.marvin.widget;

import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick Kutch
 */
public interface Widget
{
    public boolean Create(GridPane pane, DataManager dataMgr);
    
    public Region getRegionObject();
    
    public javafx.scene.Node getStylableObject();
    
    public ObservableList<String> getStylesheets();
    
    public void HandleCustomStyleOverride(FrameworkNode styleNode);
    
    public void OnPaused();
    
    public void OnResumed();
    
    public boolean PerformPostCreateActions(GridWidget objParentGrid, boolean flag);
    
    public void PrepareForAppShutdown();
    
    public void resetState(String strParam);
    
    public void SetClickThroughTransparentRegion(boolean _CanClickOnTransparent);
    
    public boolean SupportsEnableDisable();
    
    public boolean SupportsSteppedRanges();
    
    public void UpdateTitle(String newTitle);
    
    public void UpdateValueRange();
}
