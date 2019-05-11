/*
 * ##############################################################################
 * #  Copyright (c) 2019 by Intel Corporation
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
 */

Marvin is the highly configurable UI component of the Intel Instrumentation 
Framework project.

To compile, run:
gradlew buildEnzo
gradlew copyEnzoJar
gradlew build

Resulting BIFF.Marvin.Jar will be in the build\libs directory. Copy this to where 
you have your application setup - the .jar file should be at same location as
the Widgets directory.  This shoud look something like:

+---BIFF.Marvin.jar
+---Starter_Application
|   +---Images
|   \---Media
\---Widget
    +---Button
    +---Chart
    +---CPU
    +---FlipPanel
    +---Gauge
    +---Image
    +---Indicator
    +---LCARS
    +---LCD
    +---LED
    +---Media
    +---Memory
    +---Networking
    +---PDF
    +---Quick
    +---Storage
    +---SVG
    +---System
    +---Text
    \---Web

