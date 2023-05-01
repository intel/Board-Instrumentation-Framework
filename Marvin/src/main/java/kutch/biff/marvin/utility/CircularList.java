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

import java.util.ArrayList;

/**
 * @param <T>
 * @author Patrick Kutch
 */
public class CircularList<T> extends ArrayList<T> {
    /**
     *
     */
    private static final long serialVersionUID = -6542607529993640355L;
    private int _LastUsedIndex = 0;

    @Override
    public T get(int index) {
        if (index < 0) {
            _LastUsedIndex = size() - 1;
        } else if (index >= size()) {
            _LastUsedIndex = 0;
        } else if (index < size()) {
            _LastUsedIndex = index;
        }

        return super.get(_LastUsedIndex);
    }

    public T get(String ID) {
        ID = ID.toLowerCase();
        if (contains(ID)) {
            _LastUsedIndex = lastIndexOf(ID);
        }
        return get(_LastUsedIndex);
    }

    public T GetNext() {
        return get(_LastUsedIndex + 1);
    }

    public T GetPrevious() {
        return get(_LastUsedIndex - 1);
    }

    public boolean IsLast(String ID) {
        return ID.equalsIgnoreCase((String) super.get(super.size() - 1));
    }
}
