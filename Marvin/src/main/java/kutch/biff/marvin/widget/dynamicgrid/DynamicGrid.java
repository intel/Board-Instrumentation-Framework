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
package kutch.biff.marvin.widget.dynamicgrid;

import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.GridWidget;

/**
 *
 * @author Patrick Kutch
 */
public class DynamicGrid extends GridWidget
{
    
    private DynamicTransition _objTransition;
    
    public DynamicGrid()
    {
	super();
	_objTransition = new DynamicTransition(DynamicTransition.Transition.NONE);
    }
    
    public Color getBackgroundColorForTransition()
    {
	return _objTransition.getSnapshotColor();
    }
    
    public DynamicTransition getTransition(DynamicGrid objFrom, GridPane parent)
    {
	_objTransition.Transition(objFrom, this, parent);
	return _objTransition;
    }
    
    public DynamicTransition ReadTransitionInformation(FrameworkNode baseNode)
    {
	_objTransition = DynamicTransition.ReadTransitionInformation(baseNode);
	return _objTransition;
    }
}
