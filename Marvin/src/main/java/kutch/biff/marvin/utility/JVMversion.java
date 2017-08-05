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

public class JVMversion
{

    public static final int MINIMUM_MAJOR_VERSION = 8;
    public static final int MINIMUM_BUILD_VERSION = 20;

    public static boolean meetsMinimumVersion()
    {
        String version = System.getProperty("java.version");
        int sys_major_version = Integer.parseInt(String.valueOf(version.charAt(2)));

        if (sys_major_version < MINIMUM_MAJOR_VERSION)
        {
            return false;
        }
        else if (sys_major_version > MINIMUM_MAJOR_VERSION)
        {
            return true;
        }
        else
        {
            int splitPosition = version.lastIndexOf("_");

            try
            {
                int majorVer = Integer.parseInt(version.substring(splitPosition + 1));

                return (majorVer >= MINIMUM_BUILD_VERSION);

            }
            catch (Exception ex)
            {
                return false;
            }
        }
    }

}
