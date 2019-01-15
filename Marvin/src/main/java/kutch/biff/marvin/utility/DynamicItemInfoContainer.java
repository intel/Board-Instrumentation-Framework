/*
 * ##############################################################################
 * #  Copyright (c) 2018 Intel Corporation
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.util.Pair;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.TabWidget;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class DynamicItemInfoContainer
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final Pair<ArrayList<String>, ArrayList<String>> __namespaceCriterea;
    private final Pair<ArrayList<String>, ArrayList<String>> __idCriterea;
    private FrameworkNode __node;
    private final HashMap<String, Boolean> __PreviouslyChecked;
    private int __NumberOfMatchesUsingThisPattern;
    private String __TokenizerToken;
    private String __MatchedSortString;
    private Map<String, String> __AliasListSnapshot = null;

    public enum SortMethod
    {
        NAMESPACE, ID, VALUE, NONE
    };
    private SortMethod __SortMethod;
    private List<String> _StyleOverrideEven, _StyleOverrideOdd;
    private String _StyleOverrideFileEven, _StyleOverrideFileOdd;
    private String _StyleOverrideIDEven, _StyleOverrideIDOdd;

    public DynamicItemInfoContainer(Pair<ArrayList<String>, ArrayList<String>> namespaceCriterea,
                                    Pair<ArrayList<String>, ArrayList<String>> idCriterea)
    {
        __PreviouslyChecked = new HashMap<>();
        __namespaceCriterea = namespaceCriterea;
        __idCriterea = idCriterea;
        __node = null;
        __TokenizerToken = null;
        __NumberOfMatchesUsingThisPattern = 0;
        __MatchedSortString = "";
        __SortMethod = SortMethod.NONE;
        _StyleOverrideEven = new ArrayList<>();
        _StyleOverrideOdd = new ArrayList<>();
        _StyleOverrideFileEven = _StyleOverrideFileOdd = null;
        _StyleOverrideIDEven = _StyleOverrideIDOdd = null;
    }

    public void TakeAliasSnapshot()
    {
        __AliasListSnapshot = AliasMgr.getAliasMgr().getSnapshot();
    }
    public boolean Matches(String namespace, String ID, String Value)
    {
        __MatchedSortString = "";
        // if already checked, no need to do it again
        if (__PreviouslyChecked.containsKey(namespace + ID))
        {
            return false;
        }
        if (__idCriterea.getKey().isEmpty() && __PreviouslyChecked.containsKey(namespace))
        {
            return false;
        }

        boolean matched = Matches(namespace, __namespaceCriterea);
        if (matched && !__idCriterea.getKey().isEmpty())
        {
            matched = Matches(ID, __idCriterea);

            __PreviouslyChecked.put(namespace + ID, matched);
        }
        else
        {
            __PreviouslyChecked.put(namespace, matched);
        }
        if (matched)
        {
            __NumberOfMatchesUsingThisPattern++;
            if (getSortByMethod() == SortMethod.NAMESPACE)
            {
                __MatchedSortString = namespace;
            }
            else if (getSortByMethod() == SortMethod.ID)
            {
                __MatchedSortString = ID;
            }
            else if (getSortByMethod() == SortMethod.VALUE)
            {
                __MatchedSortString = Value;
            }
            else
            {
                __MatchedSortString = null;
            }

        }
        return matched;
    }

    public String getLastMatchedSortStr()
    {
        return __MatchedSortString;
    }

    public int getMatchedCount()
    {
        return __NumberOfMatchesUsingThisPattern;
    }

    private boolean Matches(String compare, Pair<ArrayList<String>, ArrayList<String>> patternPair)
    {
        ArrayList<String> patternList = patternPair.getKey();
        ArrayList<String> excludePatternList = patternPair.getValue();
        for (String matchPattern : patternList)
        {
            if (Glob.check(matchPattern, compare))
            {
                boolean noGood = false;
                for (String excludePattern : excludePatternList)
                {
                    if (Glob.check(excludePattern, compare))
                    {
                        noGood = true;
                        break;
                    }
                }
                if (!noGood)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void setToken(String strToken)
    {
        __TokenizerToken = strToken;
    }

    public String getToken()
    {
        return __TokenizerToken;
    }

    public FrameworkNode getNode()
    {
        return __node;
    }

    public void setNode(FrameworkNode node)
    {
        __node = node;
    }

    public String[] tokenize(String ID)
    {
        if (getToken().equalsIgnoreCase("."))
        {
            return ID.split("\\.");
        }
        return ID.split(getToken());
    }

    public boolean tokenizeAndCreateAlias(String ID)
    {
        if (null == getToken())
        {
            return false;
        }
        String[] tokens = tokenize(ID);
        if (tokens.length <= 0)
        {
            return false;
        }
        int index = 1;
        for (String token : tokens)
        {
            String Alias = "TriggeredIDPart." + Integer.toString(index++);
            AliasMgr.getAliasMgr().AddAlias(Alias, token);
        }
        return true;
    }

    public void setSortByMethod(SortMethod sortMethod)
    {
        __SortMethod = sortMethod;
    }

    public SortMethod getSortByMethod()
    {
        return __SortMethod;
    }

    public void ReadStyles(FrameworkNode onDemandNode)
    {
        if (onDemandNode.hasChild("StyleOverride-Even"))
        {
            FrameworkNode evenNode = onDemandNode.getChild("StyleOverride-Even");
            _StyleOverrideEven = readStyleItems(evenNode);

            if (evenNode.hasAttribute("File"))
            {
                _StyleOverrideFileEven = evenNode.getAttribute("File");
            }
            if (evenNode.hasAttribute("ID"))
            {
                _StyleOverrideIDEven = evenNode.getAttribute("ID");

            }
        }

        if (onDemandNode.hasChild("StyleOverride-Odd"))
        {
            FrameworkNode oddNode = onDemandNode.getChild("StyleOverride-Odd");
            _StyleOverrideOdd = readStyleItems(oddNode);

            if (oddNode.hasAttribute("File"))
            {
                _StyleOverrideFileOdd = oddNode.getAttribute("File");
            }
            if (oddNode.hasAttribute("ID"))
            {
                _StyleOverrideIDOdd = oddNode.getAttribute("ID");
            }
        }
    }

    private List<String> readStyleItems(FrameworkNode styleNode)
    {
        ArrayList<String> retList = new ArrayList<>();
        for (FrameworkNode node : styleNode.getChildNodes())
        {
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("Item"))
            {
                retList.add(node.getTextContent());
            }
            else
            {
                LOGGER.severe("Unknown Tag under Selected : " + node.getNodeName());
            }
        }
        return retList;
    }

    public List<String> getStyleOverrideOdd()
    {
        return _StyleOverrideOdd;
    }

    public List<String> getStyleOverrideEven()
    {
        return _StyleOverrideEven;
    }

    public void ApplyOddEvenStyle(BaseWidget objWidget, int number)
    {
        if (number % 2 == 0) // even style
        {
            if (null != _StyleOverrideFileEven)
            {
                objWidget.setBaseCSSFilename(_StyleOverrideFileEven);
            }
            if (null != _StyleOverrideIDEven)
            {
                objWidget.setStyleID(_StyleOverrideIDEven);
            }
            objWidget.addOnDemandStyle(getStyleOverrideEven());
        }
        else
        {
            if (null != _StyleOverrideFileOdd)
            {
                objWidget.setBaseCSSFilename(_StyleOverrideFileOdd);
            }
            if (null != _StyleOverrideIDOdd)
            {
                objWidget.setStyleID(_StyleOverrideIDOdd);
            }
            objWidget.addOnDemandStyle(getStyleOverrideOdd());
        }
    }
    
    public void ApplyOddEvenStyle(TabWidget objWidget, int number,String Title)
    {
        if (number % 2 == 0) // even style
        {
            //LOGGER.severe("Applying Even Style to Tab " + Title);
            if (null != _StyleOverrideFileEven)
            {
                objWidget.setBaseCSSFilename(_StyleOverrideFileEven);
            }
            if (null != _StyleOverrideIDEven)
            {
                objWidget.setStyleID(_StyleOverrideIDEven);
            }
            objWidget.addOnDemandStyle(getStyleOverrideEven());
        }
        else
        {
            //LOGGER.severe("Applying Odd Style to Tab " + Title);
            if (null != _StyleOverrideFileOdd)
            {
                objWidget.setBaseCSSFilename(_StyleOverrideFileOdd);
            }
            if (null != _StyleOverrideIDOdd)
            {
                objWidget.setStyleID(_StyleOverrideIDOdd);
            }
            objWidget.addOnDemandStyle(getStyleOverrideOdd());
        }
    }

    public void putAliasListSnapshot()
    {
        if (null == __AliasListSnapshot)
        {
            return;
        }
        AliasMgr aMgr = AliasMgr.getAliasMgr();
        for (String key : __AliasListSnapshot.keySet())
        {
            aMgr.SilentAddAlias(key, __AliasListSnapshot.get(key));
        }
    }
}
