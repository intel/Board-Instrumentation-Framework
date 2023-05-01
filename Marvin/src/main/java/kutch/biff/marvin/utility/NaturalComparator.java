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
 * #   Does 'natural compare. Where 'CPU4' < CPU12
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class NaturalComparator implements Comparator<String> {
    private static String _MatchPattern = "(\\-?\\d+\\.\\d+)|(\\-?\\.\\d+)|(\\-?\\d+)";

    @Override
    public int compare(String strFirstString, String strSecondString) {
        if (strFirstString == null || strSecondString == null) {
            return 0;
        }

        List<String> parts1 = tokenizeString(strFirstString);
        List<String> parts2 = tokenizeString(strSecondString);
        int delta = 0;

        for (int index = 0; delta == 0 && index < parts1.size() && index < parts2.size(); index++) {
            String token1 = parts1.get(index);
            String token2 = parts2.get(index);

            if (token1.matches(_MatchPattern) && token2.matches(_MatchPattern)) {
                delta = (int) Math.signum(Double.parseDouble(token1) - Double.parseDouble(token2));
            } else {
                delta = token1.compareToIgnoreCase(token2);
            }
        }
        if (delta != 0) {
            return delta;
        } else {
            return parts1.size() - parts2.size();
        }
    }

    private List<String> tokenizeString(String strString) {
        List<String> tokenList = new ArrayList<>();
        Scanner scanner = new Scanner(strString);
        int index = 0;
        String number = null;
        while ((number = scanner.findInLine(_MatchPattern)) != null) {
            int indexOfNumber = strString.indexOf(number, index);
            if (indexOfNumber > index) {
                tokenList.add(strString.substring(index, indexOfNumber));
            }
            tokenList.add(number);
            index = indexOfNumber + number.length();
        }
        if (index < strString.length()) {
            tokenList.add(strString.substring(index));
        }
        scanner.close();
        return tokenList;
    }
}
