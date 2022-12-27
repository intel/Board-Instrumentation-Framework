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
package kutch.biff.marvin.widget;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class AudioPlayerWidget extends MediaPlayerWidget {
    private static boolean _HasBeenVerified = false;
    private static boolean _IsValid = true;
    private Button _objDummyButton;

    public AudioPlayerWidget() {
        super("AudioPlayerWidget");
        _objDummyButton = new Button();
        _objDummyButton.setVisible(false);
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        return Create(dataMgr);
    }

    @Override
    public Node getStylableObject() {
        return _objDummyButton;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return _objDummyButton.getStylesheets();
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        return HandleWidgetSpecificSettings(node, "Audio");
    }

    @Override
    public boolean HasBeenVerified() {
        return AudioPlayerWidget._HasBeenVerified;
    }

    @Override
    public boolean IsValid() {
        return _IsValid;
    }

    @Override
    protected boolean OnNewMedia(MediaPlayer objMediaPlayer) {
        return true;
    }

    @Override
    public void setHasBeenVerified(boolean _HasBeenVerified) {
        AudioPlayerWidget._HasBeenVerified = _HasBeenVerified;
    }

    @Override
    public void SetIsValid(boolean flag) {
        _IsValid = flag;
    }

    @Override
    protected boolean VerifyMedia(Media objMedia) {
        return true;
    }

}
