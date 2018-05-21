/*
##############################################################################
#  Copyright (c) 2018 Intel Corporation
# 
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##############################################################################
#    File Abstract: 
#
##############################################################################
 */
package kutch.biff.marvin.widget;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/*
<Widget File="Text\File.xml" row="0" column="0">
    <MinionSrc Namespace="foo" ID="Bar.*"/>
    <File>MyOut.txt</File>
    <Mode>Overwrite</Mode>
    <Format>KeyPair-ID-Value</Format>
</Widget>

 */
/**
 *
 * @author Patrick
 */
public class FileWriterWidget extends BaseWidget
{

    public enum WriteMode
    {
        Append, Overwrite
    };

    public enum WriteFormat
    {
        KeyPairIDValue, KeyPairNamespaceIDValue
    };
    private HashMap<String, String> _DataPointMap;
    private DataManager _dataMgr;
    private String _outFile;
    private WriteMode _writeMode;
    private WriteFormat _writeFormat;
    private String _prefixStr;

    public FileWriterWidget()
    {
        _DataPointMap = new HashMap<>(); // for quick lookup as new data comes in
        _outFile = null;
        _writeMode = WriteMode.Overwrite;
        _writeFormat = WriteFormat.KeyPairIDValue;
        _prefixStr = "";
    }

    private void writeMap(BufferedWriter writer) throws IOException
    {
        for (String key : _DataPointMap.keySet())
        {
            writer.append(_prefixStr);
            if (_writeFormat == WriteFormat.KeyPairNamespaceIDValue)
            {
                writer.append(getNamespace());
            }
            writer.append(key + "=");
            writer.append(_DataPointMap.get(key));
            writer.newLine();
        }
    }

    private void Overwrite()
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(_outFile));
            writer.write("");
            writeMap(writer);
            writer.close();

        }
        catch (IOException ex)
        {
            Logger.getLogger(FileWriterWidget.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void Append()
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(_outFile,true));
            writeMap(writer);
            writer.close();
        }

        catch (IOException ex)
        {
            Logger.getLogger(FileWriterWidget.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        SetParent(pane);
        _dataMgr = dataMgr;
        if (null == _outFile)
        {
            LOGGER.severe("Invalid FileWriterWidget, no <File> specified.");
            return false;
        }

        dataMgr.AddWildcardListener(getMinionID(), getNamespace(), new ChangeListener()
        {
            @Override
            public void changed(ObservableValue o, Object oldVal, Object newVal)
            {
                if (IsPaused())
                {
                    return;
                }

                String strVal = newVal.toString();
                String[] parts = strVal.split(":");
                if (parts.length > 1) // check to see if we have already created the widget
                {
                    String ID = parts[0];
                    String Value = parts[1];
                    _DataPointMap.put(ID.toLowerCase(), Value);
                    if (_writeMode == WriteMode.Append)
                    {
                        Append();
                    }
                    else
                    {
                        Overwrite();
                    }
                }
            }
        });
        SetupPeekaboo(dataMgr);

        return true;
    }

    /**
     *
     * @param node
     * @return
     */
    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node)
    {
        if (node.getNodeName().equalsIgnoreCase("File"))
        {
            _outFile = node.getTextContent();
            return true;
        }
        else if (node.getNodeName().equalsIgnoreCase("Format"))
        {
            String strVal = node.getTextContent();
            if (strVal.equalsIgnoreCase("KeyPair-ID-Value"))
            {
                _writeFormat = WriteFormat.KeyPairIDValue;
            }
            else if (strVal.equalsIgnoreCase("KeyPairIDValue"))
            {
                _writeFormat = WriteFormat.KeyPairIDValue;
            }
            else if (strVal.equalsIgnoreCase("KeyPair-Namespace-ID-Value"))
            {
                _writeFormat = WriteFormat.KeyPairNamespaceIDValue;
            }
            else if (strVal.equalsIgnoreCase("KeyPairNamespaceIDValue"))
            {
                _writeFormat = WriteFormat.KeyPairNamespaceIDValue;
            }
            else
            {
                LOGGER.severe("Invalid <Format> in FileWriterWidget : " + strVal);
                return false;
            }
            if (node.hasAttribute("Prefix"))
            {
                _prefixStr = node.getAttribute("Prefix");
            }
            return true;
        }
        else if (node.getNodeName().equalsIgnoreCase("Mode"))
        {
            String strVal = node.getTextContent();
            if (strVal.equalsIgnoreCase("Append"))
            {
                _writeMode = WriteMode.Append;
            }
            else if (strVal.equalsIgnoreCase("Overwrite"))
            {
                _writeMode = WriteMode.Overwrite;
            }
            else
            {
                LOGGER.severe("Invalid <Mode> in FileWriterWidget : " + strVal);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public ObservableList<String> getStylesheets()
    {
        return null;
    }

    @Override
    public Node getStylableObject()
    {
        return null;
    }

    @Override
    public void UpdateTitle(String newTitle)
    {

    }

}
