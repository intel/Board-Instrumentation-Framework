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

package kutch.biff.marvin.datamanager;

/**
 * Had to create my own class so I could overwrite equals(), otherwise 
 * sending same value twice would not result in change listeners getting called
 * for 2nd value
 * @author Patrick Kutch
 */
public class DataObject extends Object
{
    private final String _Data;
    public DataObject(String data)
    {
        _Data = data;
    }
    
    @Override
    public String toString()
    {
        return _Data;
    }
    public boolean equals(Object obj)
    {
        return false;
    }
    
    public String getData()
    {
        return _Data;
    }
}
