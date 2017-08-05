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

package kutch.biff.marvin.task;

import java.util.logging.Logger;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 *
 * @author Patrick Kutch
 */
abstract public class BasePrompt
{
    private String _dlgTitle;
    private String _Message;
    private String _Prompt;
    private String _PromptedValue;
    private String _ID;
    protected final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());    
    
    public BasePrompt(String ID)
    {
        _dlgTitle = null;
        _Message = null;
        _Prompt = null;    
        _PromptedValue = null;
        _ID = ID;
    }

    @Override
    public String toString()
    {
        return _ID;
    }
    public String getDlgTitle()
    {
        return _dlgTitle;
    }

    public void setDlgTitle(String _dlgTitle)
    {
        this._dlgTitle = _dlgTitle;
    }

    public String getMessage()
    {
        return _Message;
    }

    public void setMessage(String _Message)
    {
        this._Message = _Message;
    }

    public String getPrompt()
    {
        return _Prompt;
    }

    public void setPrompt(String _Prompt)
    {
        this._Prompt = _Prompt;
    }
    public String GetPromptedValue()
    {
        return _PromptedValue;
    }
    protected void SetPromptedValue(String newValue)
    {
        _PromptedValue = newValue;
    }
    
    public boolean PerformPrompt()
    {
        _PromptedValue = null;
        Stage objStage = CreateDialog();
        if (null == objStage)
        {
            return false;
        }
        CreateDialog().showAndWait();  
        
        return null != GetPromptedValue();
    }    
    private Stage CreateDialog()
    {
        Stage dialog = new Stage();
        dialog.setTitle(getDlgTitle());
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Pane dlgPane = SetupDialog(dialog);
        if (null == dlgPane)
        {
            return null;
        }
        Scene objScene = new Scene(dlgPane);
        dialog.setScene(objScene);

        return dialog;
    }

    protected Pane SetupDialog(Stage dialog)
    {
        return null;
    }
    public boolean HandlePromptSpecificConfig(FrameworkNode baseNode) 
    {
        return false;
    }
    
}
