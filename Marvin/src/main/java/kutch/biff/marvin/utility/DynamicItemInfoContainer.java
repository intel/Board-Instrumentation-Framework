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

/**
 *
 * @author Patrick
 */
public class DynamicItemInfoContainer
{

    private final ArrayList<String> __patternList;
    private final ArrayList<String> __excludePatternList;
    private final String __ID;
    private FrameworkNode __node;
    private String __other;

    public DynamicItemInfoContainer(ArrayList<String> pattern, ArrayList<String> excludePattern, String ID, FrameworkNode node, String other)
    {
        __patternList = pattern;
        __excludePatternList = excludePattern;
        __ID = ID;
        __node = node;
        __other = other;
    }

    public boolean Matches(String compare)
    {
        for (String matchPattern : __patternList)
        {
            if (Glob.check(matchPattern, compare))
            {
                boolean noGood = false;
                for (String excludePattern : __excludePatternList)
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

    public String getOther()
    {
        return __other;
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
