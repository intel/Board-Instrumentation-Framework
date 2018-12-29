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
import javafx.util.Pair;

/**
 *
 * @author Patrick
 */
public class DynamicItemInfoContainer
{

    private final Pair<ArrayList<String>,ArrayList<String>> __namespaceCriterea;
    private final Pair<ArrayList<String>,ArrayList<String>> __idCriterea;
    private String __ID;
    private FrameworkNode __node;
    private String __other;
    private final HashMap<String,Boolean> __PreviouslyChecked;
    
    public DynamicItemInfoContainer(Pair<ArrayList<String>,ArrayList<String>> namespaceCriterea,
                                    Pair<ArrayList<String>,ArrayList<String>> idCriterea,
                                    FrameworkNode node)
    {
        __PreviouslyChecked = new HashMap<>();
        __namespaceCriterea = namespaceCriterea;
        __idCriterea = idCriterea;
        __node = node;
        __other = this.toString();
        __ID = "";
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

        return matched;
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

    public String getID()
    {
        return __ID;
    }
    public void setID(String newID)
    {
        __ID = newID;
    }

    public String getOther()
    {
        return __other;
    }
    
    public void setOther(String otherValue)
    {
        __other = otherValue;
    }

    public FrameworkNode getNode()
    {
        return __node;
    }

    public void setNode(FrameworkNode node)
    {
        __node = node;
    }
    
}
