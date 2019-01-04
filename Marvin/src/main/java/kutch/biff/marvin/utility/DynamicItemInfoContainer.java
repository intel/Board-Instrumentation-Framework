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
import java.util.logging.Logger;
import javafx.util.Pair;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class DynamicItemInfoContainer
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final Pair<ArrayList<String>,ArrayList<String>> __namespaceCriterea;
    private final Pair<ArrayList<String>,ArrayList<String>> __idCriterea;
    private FrameworkNode __node;
    private final HashMap<String,Boolean> __PreviouslyChecked;
    private int __NumberOfMatchesUsingThisPattern;
    private String __TokenizerToken;
    
    public DynamicItemInfoContainer(Pair<ArrayList<String>,ArrayList<String>> namespaceCriterea,
                                    Pair<ArrayList<String>,ArrayList<String>> idCriterea)
    {
        __PreviouslyChecked = new HashMap<>();
        __namespaceCriterea = namespaceCriterea;
        __idCriterea = idCriterea;
        __node = null;
        __TokenizerToken = null;
        __NumberOfMatchesUsingThisPattern = 0;
    }

    public boolean Matches(String namespace, String ID)
    {
        // if already checked, no need to do it again
        if (__PreviouslyChecked.containsKey(namespace+ID))
        {
            return false;
        }
        if (__idCriterea.getKey().isEmpty() && __PreviouslyChecked.containsKey(namespace))
        {
            return false;
        }
            
        Boolean matched = Matches(namespace,__namespaceCriterea);
        if (matched && !__idCriterea.getKey().isEmpty())
        {
            matched = Matches(ID,__idCriterea);
            __PreviouslyChecked.put(namespace+ID, matched);
        }
        else
        {
            __PreviouslyChecked.put(namespace, matched);
        }
        if (matched)
        {
            __NumberOfMatchesUsingThisPattern++;
        }
        return matched;
    }
    
    public int getMatchedCount()
    {
        return __NumberOfMatchesUsingThisPattern;
    }
    
    private boolean Matches(String compare, Pair<ArrayList<String>,ArrayList<String>> patternPair)
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
}
