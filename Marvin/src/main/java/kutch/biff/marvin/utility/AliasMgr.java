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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import kutch.biff.marvin.configuration.Configuration;
import static kutch.biff.marvin.configuration.ConfigurationReader.OpenXMLFile;
import kutch.biff.marvin.logger.MarvinLogger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class handles aliases.
 *
 * @author Patrick Kutch
 */
public class AliasMgr
{
   private final ArrayList<Map> _AliasList;
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final static AliasMgr _Mgr = new AliasMgr();
    private final static String strCurrentRowAlias = "CurrentRowAlias";
    private final static String strNextRowAlias = "NextRowAlias";
    private final static String strCurrentColumnAlias = "CurrentColumnAlias";
    private final static String strNextColumnAlias = "NextColumnAlias";
    private final static String strPrevColumnAlias = "PrevColumnAlias";
    private final static String strPrevRowAlias = "PrevRowAlias";

    public static AliasMgr getAliasMgr()
    {
        return _Mgr;
    }

    private AliasMgr()
    {
        _AliasList = new ArrayList<>();
        PushAliasList(true);
        AddAlias("DEBUG_STYLE","-fx-border-color:blue;-fx-border-style: dashed");
        PushAliasList(true);
        AddEnvironmentVars();
    }

    /**
     * Fetches the string associated with the alias if exists, else null
     *
     * @param strAliasRequested
     * @return
     */
    public String GetAlias(String strAliasRequested)
    {
        String strAlias = strAliasRequested.toUpperCase();
        for (Map map : _AliasList)
        {
            if (map.containsKey(strAlias))
            {
                String strRetVal = (String) map.get(strAlias);
                if (strAlias.equalsIgnoreCase(strNextRowAlias))
                {
                    int currVal = Integer.parseInt(GetAlias(strCurrentRowAlias));
                    strRetVal =  Integer.toString(currVal+1);
                }
                else if (strAlias.equalsIgnoreCase(strPrevRowAlias))
                {
                    int currVal = Integer.parseInt(GetAlias(strCurrentRowAlias));
                    currVal -=1;
                    if (currVal < 0)
                    {
                        currVal = 0;
                    }
                    strRetVal =  Integer.toString(currVal);
                }
                else if (strAlias.equalsIgnoreCase(strNextColumnAlias))
                {
                    int currVal = Integer.parseInt(GetAlias(strCurrentColumnAlias));
                    strRetVal =  Integer.toString(currVal+1);
                }
                else if (strAlias.equalsIgnoreCase(strPrevColumnAlias))
                {
                    int currVal = Integer.parseInt(GetAlias(strCurrentColumnAlias));
                    currVal -=1;
                    if (currVal < 0)
                    {
                        currVal = 0;
                    }
                    strRetVal =  Integer.toString(currVal);
                }
                return strRetVal;
            }
        }
        String strError = "Tried to use Alias [" + strAliasRequested + "] that has not been set.";
        LOGGER.severe(strError); 
        return strError;
    }

    /**
     * Just checks to see if the given string is an Alias
     *
     * @param strAlias
     * @return
     */
    public boolean IsAliased(String strAlias)
    {
        strAlias = strAlias.toUpperCase();
        for (Map map : _AliasList)
        {
            if (map.containsKey(strAlias))
            {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param Alias
     * @param Value
     */
    @SuppressWarnings({"unchecked", "unchecked"})
    public void AddAlias(String Alias, String Value)
    {
        if (null == Alias)
        {
            LOGGER.severe("Attempted to set an ALIAS ID to NULL");
            return;
        }
        Map map = _AliasList.get(0);
        if (map.containsKey(Alias.toUpperCase()))
        {
            LOGGER.config("Duplicate Alias detected for : " + Alias + ". Ignoring.");
            return;
        }
        if (null == Value)
        {
            String strError = "Alias [" + Alias + "] has NULL value!";
            LOGGER.severe(strError);
            map.put(Alias.toUpperCase(),strError);
        }
        else
        {
            map.put(Alias.toUpperCase(), Value);
        }
    }
    
    public void AddRootAlias(String Alias, String Value)
    {
        if (null == Alias)
        {
            LOGGER.severe("Attempted to set a Root ALIAS ID to NULL");
            return;
        }
        Map map = _AliasList.get(_AliasList.size()-1);
        if (map.containsKey(Alias.toUpperCase()))
        {
            return;
        }
        if (null == Value)
        {
            String strError = "Root Alias [" + Alias + "] has NULL value!";
            LOGGER.severe(strError);
            map.put(Alias.toUpperCase(),strError);
        }
        else
        {
            map.put(Alias.toUpperCase(), Value);
        }
    }
    
    public void UpdateCurrentColumn(int newValue)
    {
        UpdateAlias(strNextColumnAlias, Integer.toString(newValue + 1));
        UpdateAlias(strCurrentColumnAlias, Integer.toString(newValue));
    }

    public void UpdateCurrentRow(int newValue)
    {
        UpdateAlias(strNextRowAlias, Integer.toString(newValue + 1));
        UpdateAlias(strCurrentRowAlias, Integer.toString(newValue));
    }

    @SuppressWarnings("unchecked")
    private boolean UpdateAlias(String Alias, String newValue)
    {
        String strCheck = Alias.toUpperCase();

        for (Map map : _AliasList)
        {
            if (map.containsKey(strCheck))
            {
                map.replace(strCheck, newValue);
                return true;
            }
        }
        LOGGER.severe("Asked to updated alias: " + Alias + ". However it did not exist.");
        return false;
    }

    /**
     * Implemented as a kind of stack for scope reasons meaning that if an alias
     * is used in a file, it is valid for all nested files, but not outside of
     * that scope
     */
    public final void PushAliasList(boolean addRowColAliases)
    {
        _AliasList.add(0, new HashMap<>()); // put in position 0 
        
        if (addRowColAliases)
        {
            AddAlias(strCurrentRowAlias, "0");
            AddAlias(strNextRowAlias, "1");
            AddAlias(strCurrentColumnAlias, "0");
            AddAlias(strNextColumnAlias, "1");
            AddAlias(strPrevColumnAlias, "0");
            AddAlias(strPrevRowAlias, "0");
        }
    }

    /**
     *
     */
    public void PopAliasList()
    {
        _AliasList.remove(0);
    }

    /**
     * Simple debug routine to dump top alias list
     */
    public void DumpTop()
    {
        Map map = _AliasList.get(0);
        String AliasStr = "Global Alias List:\n";

        if (map.isEmpty())
        {
            return;
        }
        
        for (Object objKey : map.keySet())
        {
            String key = (String) objKey;
            AliasStr += key + "-->" + map.get(objKey) + "\n";
        }
        LOGGER.info(AliasStr);
    }

    /**
     * Looks for <AliasList> aliases and processes them
     *
     * @param aliasNode <AliasList> node
     * @return true if successful, else false
     */
    public static boolean HandleAliasNode(FrameworkNode aliasNode)
    {
        if (aliasNode.hasAttribute("File"))
        {
            String filename = aliasNode.getAttribute("File");
            getAliasMgr().ReadExternalAliasFile(filename);
        }
        for (FrameworkNode nodeAlias : aliasNode.getChildNodes())
        {
            if (nodeAlias.getNodeName().equalsIgnoreCase("Alias"))
            {
                NamedNodeMap map = nodeAlias.GetNode().getAttributes();
                for (int iLoop = 0; iLoop < map.getLength(); iLoop++)
                {
                    FrameworkNode node = new FrameworkNode(map.item(iLoop));

                    String strAlias = node.getNodeName();

                    String strValue = node.getTextContent();
                    AliasMgr._Mgr.AddAlias(strAlias, strValue);
                }
            }
            // Default Alias, only alias if not already aliased
            else if (nodeAlias.getNodeName().equalsIgnoreCase("DefaultAlias"))
            {
                NamedNodeMap map = nodeAlias.GetNode().getAttributes();
                for (int iLoop = 0; iLoop < map.getLength(); iLoop++)
                {
                    FrameworkNode node = new FrameworkNode(map.item(iLoop));

                    String strAlias = node.getNodeName();
                    String strValue = node.getTextContent();

                    if (false == getAliasMgr().IsAliased(strAlias))
                    {
                        AliasMgr._Mgr.AddAlias(strAlias, strValue);
                    }
                }
            }
            // Allow reading alias's defined in other files
            else if (nodeAlias.getNodeName().equalsIgnoreCase("Import"))
            {
                String strImportFile = nodeAlias.getTextContent();
                if (false == ReadAliasFromExternalFile(strImportFile))
                {
                    return false;
                }
            }
            else // unknown
            {
                 LOGGER.severe("Unknown <AliasList> entry: " + nodeAlias.getNodeName());
                 return false;
            }
        }
        return true;
    }

    public static boolean ReadAliasFromExternalFile(String FileName)
    {
        Document doc = OpenXMLFile(FileName);
        AliasMgr.getAliasMgr().SetCurrentConfigFile(FileName);
        return ReadAliasFromRootDocument(doc);
    }

    public static boolean ReadAliasFromRootDocument(Document doc)
    {
        if (null != doc)
        {
            NodeList rootNodes = doc.getElementsByTagName("Marvin");
            if (0 == rootNodes.getLength())
            {
                rootNodes = doc.getElementsByTagName("MarvinExternalFile");
            }
            if (0 == rootNodes.getLength())
            {
                rootNodes = doc.getElementsByTagName("Widget");
            }
            if (0 == rootNodes.getLength())
            {
                LOGGER.severe("Requested to read <AliasList> from invalid xml file. Root node must be Marvin, MarvinExternalFile or Widget");
                return false;
            }
            
            FrameworkNode rootNode = new FrameworkNode(rootNodes.item(0));
            
            for (FrameworkNode child : rootNode.getChildNodes())
            {
                if (child.getNodeName().equalsIgnoreCase("AliasList"))
                {
                    if (false == AliasMgr.HandleAliasNode(child))
                    {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
    
    public void addMarvinInfo()
    {
        Configuration CONFIG = Configuration.getConfig();        
        
        Rectangle2D visualBounds = CONFIG.getPrimaryScreen().getVisualBounds();
        AddAlias("SCREEN_WIDTH", Integer.toString((int) visualBounds.getWidth()));
        AddAlias("SCREEN_HEIGHT", Integer.toString((int) visualBounds.getHeight()));
    }

    private void AddEnvironmentVars()
    {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet())
        {
            AddAlias(envName, env.get(envName));
        }
    }

    public void AddAliasFromAttibuteList(FrameworkNode node, String KnownAttributes[])
    {
        if (node.hasAttributes())
        {
            NamedNodeMap attrs = node.GetNode().getAttributes();

            for (int oLoop = 0; oLoop < attrs.getLength(); oLoop++)
            {
                boolean found = false;
                Attr attribute = (Attr) attrs.item(oLoop);
                String strAlias = attribute.getName();
                for (int iLoop = 0; iLoop < KnownAttributes.length; iLoop++) // compare to list of valid
                {
                    if (0 == KnownAttributes[iLoop].compareToIgnoreCase(strAlias)) // 1st check case independent just for fun
                    {
                        found = true;
                        break;
                    }
                }
                if (false == found)
                {
                    String strValue = node.getAttribute(strAlias);
                    AddAlias(strAlias, strValue);
                    LOGGER.config("Adding Alias for external file from attribute list : " + strAlias + "-->" + strValue);
                }
            }
        }
    }

    private int ReadExternalAliasFile(String filename)
    {
        LOGGER.info("Reading external Alias File: " + filename);

        BufferedReader br;
        try
        {
            br = new BufferedReader(new FileReader(filename));
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(AliasMgr.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
        String line;
        SetCurrentConfigFile(filename);
        try
        {
            while ((line = br.readLine()) != null)
            {
                if (line.trim() != null)
                {
                    StringTokenizer st = new StringTokenizer(line, "=#");
                    String strAlias, Value;
                    if (st.hasMoreElements())
                    {
                        strAlias = ((String) st.nextElement()).trim();
                    }
                    else
                    {
                        continue; // no more
                    }
                    if (st.hasMoreElements())
                    {
                        Value = ((String) st.nextElement()).trim();
                        if (Value.charAt(0) == '"' && Value.charAt(Value.length() - 1) == '"')
                        {
                            Value = Value.substring(1, Value.length() - 1);
                        }
                    }
                    else
                    {
                        if (line.charAt(0) != '#')
                        {
                            LOGGER.severe("Bad Alias in Alias File: " + line); // only be here if line is something like alias=
                        }
                        continue;
                    }
                    AddAlias(strAlias, Value);
                }
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(AliasMgr.class.getName()).log(Level.SEVERE, null, ex);
        }
        try
        {
            br.close();
        }
        catch (IOException ex)
        {
            Logger.getLogger(AliasMgr.class.getName()).log(Level.SEVERE, null, ex);
        }

        // process the line.
        return 0;
    }

    public int LoadAliasFile(String encodedFilename)
    {
        StringTokenizer tokens = new StringTokenizer(encodedFilename, "=");
        tokens.nextElement(); // eat up the -alaisfile=
        String filename = (String) tokens.nextElement(); // to get to the real filename!
        return ReadExternalAliasFile(filename);
    }
    
    public void SetCurrentConfigFile(String strFname)
    {
        AddAlias("CurrentConfigFilename", strFname);
    }
}
