/*
 * Copyright (c) 2015 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.enzo.gauge.skin;

import eu.hansolo.enzo.gauge.FlatGauge;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;


/**
 * Created by hansolo on 31.10.15.
 */
public class FlatGaugeSkin extends SkinBase<FlatGauge> implements Skin<FlatGauge> {


    // ******************** Constructors **************************************
    public FlatGaugeSkin(FlatGauge gauge) {
        super(gauge);
    }
}
