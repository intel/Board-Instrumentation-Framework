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
 * #    
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.utility;

import kutch.biff.marvin.datamanager.DataManager;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */

public class DataPointGenerator
{
    private String __Namespace;
    private String __ID;
    private String __Value;
    
    public DataPointGenerator(String Namesace, String ID, String Value)
    {
	__Namespace = Namesace;
	__ID = ID;
	__Value = Value;
    }
    
    public void generate()
    {
	DataManager.getDataManager().ChangeValue(__ID, __Namespace, __Value);
    }
}
