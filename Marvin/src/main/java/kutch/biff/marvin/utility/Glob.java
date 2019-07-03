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
 * #    Does 'DOS' or unix style file system matching for strings.  Since
 * #    Java does not have a glob library, use the file sysem code to do the 
 * #    dirty work.
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.utility;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

/**
 *
 * @author Patrick
 */
public class Glob
{
    public static boolean check(String globPattern, String stringToCheck)
    {
        if (null == globPattern || null == stringToCheck)
        {
            return false;
        }
        globPattern=globPattern.toUpperCase();
        stringToCheck=stringToCheck.toUpperCase();
        PathMatcher matcher = ("*".equals(globPattern)) ? null
                              : FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

        try
        {
            return "*".equals(globPattern) || matcher.matches(Paths.get(stringToCheck));
        }
        catch(Exception ex)
        {
            return false;
        }
       
    }
}
