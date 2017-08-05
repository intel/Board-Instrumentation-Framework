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

import java.util.logging.Logger;
import javafx.geometry.Pos;
import kutch.biff.marvin.logger.MarvinLogger;


/**
 *
 * @author Patrick Kutch
 */
public class PanelSideInfo
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private String ButtonText;
    private String CSSFile;
    private String StyleID;
    private Pos Position;
    private boolean ButtonOnTop;
    
    public PanelSideInfo(String loc, String text,String ID, String file)
    {
        ButtonText = text;
        StyleID = ID;
        CSSFile = file;
        ButtonOnTop = true;
        Position = setButtonAlignment(loc);
    }

    public String getButtonText()
    {
        if (null == ButtonText)
        {
            return "";
        }
        return ButtonText;
    }

    public String getCSSFile()
    {
        if (null == CSSFile)
        {
            return "";
        }
        return CSSFile;
    }

    public String getStyleID()
    {
        return StyleID;
    }
    public Pos GetButtonAlignment()
    {
        return this.Position;
    }
    public boolean IsButtonOnTop()
    {
        return ButtonOnTop;
    }
    private Pos setButtonAlignment(String alignString)
    {
        if (0 == alignString.compareToIgnoreCase("Center"))
        {
            return (Pos.CENTER);
        }
        else if (0 == alignString.compareToIgnoreCase("N"))
        {
            return (Pos.TOP_CENTER);
        }
        else if (0 == alignString.compareToIgnoreCase("NE"))
        {
            return (Pos.TOP_RIGHT);
        }
        else if (0 == alignString.compareToIgnoreCase("E"))
        {
            return (Pos.CENTER_RIGHT);
        }
        else if (0 == alignString.compareToIgnoreCase("SE"))
        {
            ButtonOnTop = false;
            return (Pos.BOTTOM_RIGHT);
        }
        else if (0 == alignString.compareToIgnoreCase("S"))
        {
            ButtonOnTop = false;
            return (Pos.BOTTOM_CENTER);
        }
        else if (0 == alignString.compareToIgnoreCase("SW"))
        {
            ButtonOnTop = false;
            return (Pos.BOTTOM_LEFT);
        }
        else if (0 == alignString.compareToIgnoreCase("W"))
        {
            return (Pos.CENTER_LEFT);
        }
        else if (0 == alignString.compareToIgnoreCase("NW"))
        {
            return (Pos.TOP_LEFT);
        }
        else
        {
            LOGGER.severe("Invalid FlipPanel Button in config file: " +alignString + ". Ignoring.");
            return (Pos.CENTER_LEFT);
        }
    }    
}
