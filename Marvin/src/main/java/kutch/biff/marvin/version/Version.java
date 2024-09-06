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
package kutch.biff.marvin.version;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class Version {
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static Properties _Props = null;

    /**
     *
     */
    public static void Display() {
        System.out.println(Version.getVersion());
    }

    public static String getBuildNumber() {
        if (null != Version.getProperties()) {
            return Version.getProperties().getProperty("Build_Version");
        }
        return "1";
    }

    public static String getDayVersion() {
        if (null != Version.getProperties()) {
            return Version.getProperties().getProperty("Day_Version");
        }
        return "9";
    }

    public static String getMonthVersion() {
        if (null != Version.getProperties()) {
            return Version.getProperties().getProperty("Month_Version");
        }
        return "9";
    }

    private static Properties getProperties() {
        if (null != Version._Props) {
            return Version._Props;
        }
        InputStream in = Version.class.getResourceAsStream("Marvin.version.properties");
        Properties props = new Properties();
        if (in != null) {
            try {
                props.load(in);
                Version._Props = props;
                in.close();
                return props;
            } catch (Exception ex) {
                LOGGER.severe(ex.toString());
            }
        } else {
            LOGGER.severe("Unable to load version properties file");
        }
        return null;
    }

    public static String getRelease() {
        if (null != Version.getProperties()) {
            return Version.getProperties().getProperty("Release");
        }
        return "[Undefined]";
    }

    public static boolean isDebugRelease() {
        if (null != Version.getProperties()) {
            return "true".equalsIgnoreCase(Version.getProperties().getProperty("DEBUG"));
        }
        return false;
    }

    public static String getVersion() {
        return Version.getRelease() + " - " + Version.getYearVersion() + "." + Version.getMonthVersion() + "."
                + Version.getDayVersion() + " build " + Version.getBuildNumber();
    }

    public static String getYearVersion() {
        if (null != Version.getProperties()) {
            return Version.getProperties().getProperty("Year_Version");
        }
        return "0";
    }
}
