/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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

import static kutch.biff.marvin.widget.BaseWidget.convertToFileOSSpecific;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import kutch.biff.marvin.configuration.Configuration;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class SaveScreenshotTask extends BaseTask
{
    public enum SaveMode
    {
        OVERWRITE,SEQUENCE,PROMPT
    }
    private String _strFileName;
    private SaveMode _mode;
    
    public SaveScreenshotTask(String fName, SaveMode mode)
    {
        _strFileName = fName;
        _mode = mode;
    }
    private String getFileExtension(String fileName) 
    {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) 
        {
            return ""; 
        }
        return fileName.substring(lastIndexOf);
    }
    private String getFileWithoutExtension(String fileName) 
    {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) 
        {
            return fileName;
        }
        return fileName.substring(0,lastIndexOf);
    }
    
    private void HandlePrompt(String fName)
    {
        //FileNameExtensionFilter ft = new FileNameExtensionFilter(".jpg",".jpeg",".png",".gif");
        
    }
    
    private void HandleSequence(String fName)
    {
        int iNum = 1;
        String fileName = fName;
        String extension = getFileExtension(fName);
        String base = getFileWithoutExtension(fName);
        while (new File(fileName).exists())
        {
            fileName = base + String.format("%d", iNum);
            if (extension.length()>0)
            {
                fileName = fileName + extension;
            }
            iNum++;
        }
        WriteToFile(fileName);
    }

    @Override
    public void PerformTask()
    {
        String fname = getDataValue(_strFileName);
        fname = convertToFileOSSpecific(_strFileName);
        
        if (_mode == SaveMode.OVERWRITE)
        {
            WriteToFile(fname);
        }
        else if (_mode == SaveMode.SEQUENCE)
        {
            HandleSequence(fname);
        }
        else if (_mode == SaveMode.PROMPT)
        {
            HandlePrompt(fname);
        }
        else
        {
            LOGGER.severe("Unknown mode in SaveScreenshotTask: " + _mode.toString());
        }
    }
    
    private void WriteToFile(String fName)
    {
        Configuration CONFIG = Configuration.getConfig();
        WritableImage image = CONFIG.getAppScene().getRoot().snapshot(new SnapshotParameters(),null);
        if (image == null)
        {
            LOGGER.severe("Unknown error taking snapsot in SaveScreenshotTask: " + _mode.toString());
            return;
        }
        BufferedImage buffImg = SwingFXUtils.fromFXImage(image, null);
        if (buffImg == null)
        {
            LOGGER.severe("Unknown error taking snapsot in SaveScreenshotTask: " + _mode.toString());
            return;
        }
        File outputfile = new File(fName);
        String extension = getFileExtension(fName).substring(1);
        try
        {
            ImageIO.write(buffImg,extension,outputfile);
        }
        catch (IOException ex)
        {
            Logger.getLogger(SaveScreenshotTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
