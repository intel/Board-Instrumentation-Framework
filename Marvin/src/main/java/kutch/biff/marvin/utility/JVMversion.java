/*
 * ##############################################################################
 * #  Copyright (c) 2016-2023 Intel Corporation
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
import java.util.*;

public class JVMversion {
    public static final int MINIMUM_MAJOR_VERSION = 12;

    public static boolean meetsMinimumVersion() {
        String versionStr = System.getProperty("java.version");
        String versionParts[] = versionStr.split("\\.");
        System.out.println(versionStr);
        System.out.println(Arrays.toString(versionParts));
        try {
            int sys_major_version = Integer.parseInt(versionParts[0]);

            return sys_major_version >= MINIMUM_MAJOR_VERSION;
        } 
        catch (Exception ex) {
            return false;
        }
        
    }
}
