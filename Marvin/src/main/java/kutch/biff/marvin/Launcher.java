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
 * #	Launcher for JavaFX 11 application
 * #
 * ##############################################################################
 */
package kutch.biff.marvin;

public class Launcher {

    public static void main(String[] args) {
            try {
                Thread.sleep(2000);  // 2-second delay for debugger
            } catch (InterruptedException e) {
            }
        if (Boolean.getBoolean("debugBuild")) {
            try {
                System.out.println("Sleeping to allow debugger to activate");
                Thread.sleep(2000);  // 2-second delay for debugger
            } catch (InterruptedException e) {
            }
        }           
        Marvin.main(args);
    }
}
