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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.CircularList;
import kutch.biff.marvin.utility.FrameworkNode;
import static kutch.biff.marvin.widget.BaseWidget.LOGGER;

/**
 *
 * @author Patrick Kutch
 */
public abstract class MediaPlayerWidget extends BaseWidget
{
    private HashMap<String, String> _MediaURI;
    private HashMap<String, String> _MediaFilesAndTags;
    protected HashMap<String, String> _TaskMap;
    private CircularList<String> _ListOfIDs;
    protected String _CurrentMediaID;
    private MediaPlayer _mediaPlayer;
    private boolean _RepeatList;
    private boolean _RepeatSingleMedia;
    private final String _WidgetType;
    private String _PlaybackControlID, _PlaybackControl_Namespace;
    private boolean _AutoStart;
    private double _VolumeLevel;
    private HashMap<String, List<Pair<String, String>>> _EventMarkerMap; // each Media (in has by ID) has a potiential list of Markers

    public MediaPlayerWidget(String strType)
    {
        _WidgetType = strType;
        _ListOfIDs = new CircularList<>();
        _MediaURI = new HashMap<>();
        _CurrentMediaID = null;
        _PlaybackControlID = null;
        _PlaybackControlID = null;
        _RepeatList = false;
        _RepeatSingleMedia = false;
        _mediaPlayer = null;
        _MediaFilesAndTags = new HashMap<>();
        _TaskMap = new HashMap<>();
        _AutoStart = false;
        _VolumeLevel = 50;
        _EventMarkerMap = new HashMap<>();
    }

    protected boolean Create(DataManager dataMgr)
    {
        if (!VerifyInputFiles())
        {
            return false;
        }
        if (!VerifySupportsMedia())
        {
            if (CONFIG.getEnforceMediaSupport())
            {
                return false;
            }
            return true;
        }
        
        if (_ListOfIDs.size() == 1)
        {
            _CurrentMediaID = _ListOfIDs.get(0);
        }
        if (null != _PlaybackControlID && null != _PlaybackControl_Namespace)
        {
            ConfigurePlayback(dataMgr);
        }

        dataMgr.AddListener(getMinionID(), getNamespace(), new ChangeListener()
        {
            @Override
            public void changed(ObservableValue o, Object oldVal, Object newVal)
            {
                String strVal = newVal.toString().replaceAll("(\\r|\\n)", "");
                String key;

                if (strVal.equalsIgnoreCase("Next")) // go to next media in the list
                {
                    key = _ListOfIDs.GetNext();
                }
                else if (strVal.equalsIgnoreCase("Previous")) // go to previous media in the list
                {
                    key = _ListOfIDs.GetPrevious();
                }
                else
                {
                    key = strVal; // expecting an ID
                    _ListOfIDs.get(key); // just to keep next/prev alignment
                }

                key = key.toLowerCase();
                PlayMedia(key);
            }
        });

        if (null != _CurrentMediaID)
        {
            _ListOfIDs.get(_CurrentMediaID);
            if (!PlayMedia(_CurrentMediaID))
            {
                return false;
            }
        }

        return true;
    }

    private void ConfigurePlayback(DataManager dataMgr)
    {
        dataMgr.AddListener(_PlaybackControlID, _PlaybackControl_Namespace, new ChangeListener()
        {
            @Override
            public void changed(ObservableValue o, Object oldVal, Object newVal)
            {
                String strPlaybackCmd = newVal.toString();
                if (strPlaybackCmd.equalsIgnoreCase("Play"))
                {
                    OnPlay();
                }
                else if (strPlaybackCmd.equalsIgnoreCase("Pause"))
                {
                    OnPause();
                }
                else if (strPlaybackCmd.equalsIgnoreCase("Stop"))
                {
                    OnStop();
                }
                else if (strPlaybackCmd.contains(":")) // could be Volume or JumpTo
                {
                    String[] parts = strPlaybackCmd.split(":");
                    double dVal;
                    if (parts.length > 1)
                    {
                        String strTask = parts[0];
                        try
                        {
                            dVal = Double.parseDouble(parts[1]);
                        }
                        catch (NumberFormatException ex)
                        {
                            LOGGER.severe(_WidgetType + " received invalid command --> " + strPlaybackCmd);
                            return;
                        }
                        if (strTask.equalsIgnoreCase("Volume"))
                        {
                            OnSetVolume(dVal);
                        }
                        else if (strTask.equalsIgnoreCase("JumpTo"))
                        {
                            OnJumpTo(dVal);
                        }
                        else
                        {
                            LOGGER.severe(_WidgetType + " received invalid command --> " + strPlaybackCmd);
                        }
                    }
                    else
                    {
                        LOGGER.severe(_WidgetType + " received invalid command --> " + strPlaybackCmd);
                    }
                }
            }
        });
    }

    public boolean AddMediaFile(String newFile, String ID)
    {
        if (null == newFile)
        {
            return false;
        }
        String strFileName = BaseWidget.convertToFileOSSpecific(newFile);
        
        if (null == strFileName)
        {
            return false;
        }

        String uriFile = VerifyFilename(strFileName);

        if (null != uriFile)
        {
            try
            {
                Media objMedia = getMedia(uriFile); // just a test

                if (null != objMedia)
                {
                    if (false == _MediaURI.containsKey(ID))
                    {
                        _MediaURI.put(ID, uriFile); // has of uri's
                        _ListOfIDs.add(ID);
                        return true;
                    }
                    LOGGER.severe("Duplicate media ID specified for " + _WidgetType + " Widget:" + ID);
                }
            }
            catch (Exception ex)
            {
                LOGGER.severe(newFile + " is not a valid or supported media file ");
                LOGGER.severe(ex.toString());
            }
        }
        return false;
    }

    private MediaPlayer CreateMediaPlayer(String strID)
    {
        if (false == _MediaURI.containsKey(strID))
        {
            LOGGER.severe("Tried to read media with ID of " + strID);
            return null;
        }
        if (!IsValid())
        {
            return null;
        }
        String strFileID = BaseWidget.convertToFileOSSpecific(strID);
        Media objMedia = getMedia(_MediaURI.get(strFileID)); // just a test
        MediaPlayer objPlayer;
        try
        {
            objPlayer = new MediaPlayer(objMedia);
            //Duration D = objPlayer.getTotalDuration();
            objPlayer.setOnError(() ->
            {
                if (null != objMedia && null != objMedia.getError() )
                {
                    LOGGER.severe("Unable to play media file: " + _MediaURI.get(strFileID) + ". " + objMedia.getError().getMessage());
                }
                else
                {
                    LOGGER.severe("Unable to play media file: " + _MediaURI.get(strFileID) + ". ");
                }
                OnErrorOcurred();
            });
            objPlayer.setOnEndOfMedia(() ->
            {
                //LOGGER.info(_WidgetType + " [" + strID + "] --> End of media");
                OnPlaybackDone();
            });
            objPlayer.setOnPaused(() ->
            {
                LOGGER.info(_WidgetType + " [" + strID + "] --> Paused");
            });
            objPlayer.setOnPlaying(() ->
            {
                LOGGER.info(_WidgetType + " [" + strID + "] --> Playing");
            });
            objPlayer.setOnStopped(() ->
            {
                LOGGER.info(_WidgetType + " [" + strID + "] --> Stopped");
            });
        }
        catch (Exception ex)
        {
            LOGGER.severe(ex.toString());
            return null;
        }
        return objPlayer;
    }

    protected void OnPause()
    {
        if (null != _mediaPlayer)
        {
            _mediaPlayer.pause();
        }
    }

    protected void OnPlay()
    {
        if (null != _mediaPlayer)
        {
            _mediaPlayer.play();
        }
    }

    protected void OnStop()
    {
        if (null != _mediaPlayer)
        {
            _mediaPlayer.stop();
        }

    }

    protected void OnSetVolume(double newVal)
    {
        if (null != _mediaPlayer)
        {
            if (newVal < 0)
            {
                newVal = 0.0;
            }
            else if (newVal > 100)
            {
                newVal = 100;
            }
            while (newVal > 1) // takes range of 0.0 to 1.0.
            {
                newVal /= 100;
            }
            _mediaPlayer.setVolume(newVal);
            _VolumeLevel = newVal;
        }

    }

    protected void OnJumpTo(double newVal)
    {
        if (null != _mediaPlayer)
        {
            if (newVal < 0)
            {
                newVal = 0.0;
            }
            else if (newVal > 100)
            {
                newVal = 100;
            }
            while (newVal > 1) // takes range of 0.0 to 1.0.
            {
                newVal /= 100;
            }
            boolean playing = _mediaPlayer.getStatus() == Status.PLAYING;

            Duration mediaDuration = _mediaPlayer.getTotalDuration();

            Duration seekLocation = mediaDuration.multiply(newVal);

            if (_mediaPlayer.getStatus() == Status.STOPPED)
            {
                OnPause();
            }

            _mediaPlayer.seek(seekLocation);
            if (playing)
            {
                OnPlay();
            }

        }
    }

    protected void OnErrorOcurred()
    {
        SetIsValid(false);
    }
    protected void OnPlaybackDone()
    {
        if (!_RepeatList && !_RepeatSingleMedia)
        {
      //      OnStop(); // reset the media to start and be able to play it again
            return;
        }
        String strNextID = "";
        if (_RepeatList && _ListOfIDs.size() > 1)
        {
            strNextID = _ListOfIDs.GetNext();
            PlayMedia(strNextID);
        }
        else // must repeat current media
        {
            //LOGGER.info("Setting repeat to infinite. [" + _mediaPlayer.toString() + "]");
            _mediaPlayer.seek(_mediaPlayer.getStartTime());
            return;
        }

        _mediaPlayer.play();
    }

    protected String VerifyFilename(String strFile)
    {
        if (strFile.startsWith("http"))
        {
            //return strFile; // TODO: suport http targets
        }
        File file = new File(strFile);

        if (file.exists())
        {
            return file.toURI().toString();
        }
        return null;
    }

    private Media getMedia(String uriFile)
    {
        Media objMedia = null;

        try
        {
            objMedia = new Media(uriFile);
        }
        catch (Exception ex)
        {
            LOGGER.severe(ex.toString());
            return null;
        }
        if (!VerifyMedia(objMedia))
        {
            objMedia = null;
        }
        return objMedia;
    }

    protected abstract boolean VerifyMedia(Media objMedia);

    protected abstract boolean OnNewMedia(MediaPlayer objMediaPlayer);

    protected abstract boolean HasBeenVerified();

    protected abstract void setHasBeenVerified(boolean _HasBeenVerified);
    
    protected abstract boolean IsValid();
    protected abstract void SetIsValid(boolean flag);

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected boolean HandleWidgetSpecificSettings(FrameworkNode node, String inputType)
    {
        if (node.getNodeName().equalsIgnoreCase("Initial"))
        {
            if (node.hasAttribute("ID"))
            {
                _CurrentMediaID = node.getAttribute("ID");
                return true;
            }
            LOGGER.config("No ID for <Initial> tag for " + _WidgetType + " ignoring");
        }
        else if (node.getNodeName().equalsIgnoreCase("AutoStart"))
        {
            _AutoStart = node.getBooleanValue();
            return true;
        }
        else if (node.getNodeName().equalsIgnoreCase("PlaybackControl"))
        {
            if (node.hasAttribute("ID"))
            {
                _PlaybackControlID = node.getAttribute("ID");
            }
            if (node.hasAttribute("Namespace"))
            {
                _PlaybackControl_Namespace = node.getAttribute("Namespace");
            }
            if (null == _PlaybackControl_Namespace && null == _PlaybackControlID)
            {
                LOGGER.severe(_WidgetType + "has tag invalid <PlaybackControl> tag");
                return false;
            }
            return true;
        }

        else if (node.getNodeName().equalsIgnoreCase("Repeat"))
        {
            boolean bVal = node.getBooleanValue();
            if (bVal)
            {
                if (node.hasAttribute("Mode"))
                {
                    if (node.getAttribute("Mode").equalsIgnoreCase("LoopList"))
                    {
                        _RepeatList = true;
                        _RepeatSingleMedia = false;
                    }
                    else if (node.getAttribute("Mode").equalsIgnoreCase("Single"))
                    {
                        _RepeatList = false;
                        _RepeatSingleMedia = true;
                    }
                    else
                    {
                        LOGGER.severe(_WidgetType + "has tag invalid <Repeat> Mide Attribute tag, expecting either LoopList or Single, got " + node.getAttribute("Mode"));
                        return false;
                    }
                }
                else
                {
                    _RepeatList = false;
                    _RepeatSingleMedia = true;

                }
            }
            return true;
        }

        else if (node.getNodeName().equalsIgnoreCase(inputType))
        {
            String strSrc = node.getAttribute("Source");
            String strID = node.getAttribute("ID");
            if (null != strSrc && null != strID)
            {
                String key = strID.toLowerCase(); // store keys in lower case
                if (_MediaFilesAndTags.containsKey(key))
                {
                    LOGGER.severe(_WidgetType + " had duplicate source " + inputType + " ID of " + strID);
                    return false;
                }
                _MediaFilesAndTags.put(key, strSrc);

                List<Pair<String, String>> markers = new ArrayList<>(); // list of markers of tasks
                _EventMarkerMap.put(key, markers);
                
                if (node.hasAttribute("Task"))
                {
                    String taskID = node.getAttribute("task");
                    if (null != taskID)
                    {
                        _TaskMap.put(key,taskID);
                    }
                }

                return GetMarkers(node, inputType, markers);
            }
            LOGGER.severe(_WidgetType + "has tag invalid <" + inputType + "> tag");
        }

        return false;
    }

    private void SetupMarkers(MediaPlayer objPlayer, String PlayerID)
    {
        if (_EventMarkerMap.containsKey(PlayerID.toLowerCase())) // should never fail
        {
            List<Pair<String, String>> markers = _EventMarkerMap.get(PlayerID.toLowerCase());
            if (!markers.isEmpty())
            {
                for (Pair<String, String> item : markers)
                {
                    String strMarker = item.getKey();
                    double msMarker = 0;
                    String strTask = item.getValue();

                    double tDuration = objPlayer.getTotalDuration().toMillis();
                    try
                    {
                        //LOGGER.severe(Double.toString(tDuration));
                        if (strMarker.equalsIgnoreCase("end"))
                        {
                            msMarker = tDuration;
                        }
                        else if (strMarker.equalsIgnoreCase("start"))
                        {
                            msMarker = 0;
                        }

                        else if (strMarker.contains("%"))
                        {
                            msMarker = tDuration * (Double.parseDouble(strMarker.replace("%", "")) / 100);
                        }
                        
                        else
                        {
                            msMarker = Double.parseDouble(strMarker);
                        }
                        
                        if (msMarker > tDuration)
                        {
                            LOGGER.config(_WidgetType + " has task [" + strTask + "] marker > length of media.  Ignoring");
                            continue;
                        }
                        if (this instanceof AudioPlayerWidget) // For some reason, audio won't trigger events at end of medai,
                        {
                            if (msMarker + 300 > tDuration) // so this is a work around
                            {
                                msMarker = tDuration - 300;
                            }
                        }
                        objPlayer.getMedia().getMarkers().put(strTask, Duration.millis(msMarker));
                    }

                    catch (Exception Ex) // was verifed earlier, so should NEVER happen
                    {
                        LOGGER.severe("Error Setting up Markers, Marker= " + strMarker);
                        return;
                    }
                }
                objPlayer.setOnMarker((MediaMarkerEvent event) ->
                {
                    String strTask = event.getMarker().getKey();
                    TASKMAN.AddDeferredTask(strTask); // fire off that task!
                });
            }
        }
    }

    private boolean VerifyMarker(String strMarker)
    {
        String Test = strMarker;
        if (Test.equalsIgnoreCase("end"))
        {
            return true;
        }
        if (Test.equalsIgnoreCase("start"))
        {
            return true;
        }

        if (Test.contains("%"))
        {
            Test = Test.trim().replace("%", "");
        }
        try
        {
            int iMarker = Integer.parseInt(Test);
            if (strMarker.contains("%") && iMarker > 100)
            {

            }
            else if (iMarker >= 0)
            {
                return true;
            }
        }
        catch (NumberFormatException ex)
        {
        }
        LOGGER.severe("Invalid Marker specified for " + _WidgetType + ": " + strMarker);

        return false;
    }

    private boolean GetMarkers(FrameworkNode mediaNode, String inputType, List<Pair<String, String>> markers)
    {
        if (mediaNode.hasChild("Task"))
        {
            for (FrameworkNode node : mediaNode.getChildNodes())
            {
                if (node.getNodeName().equalsIgnoreCase("Task"))
                {
                    String Task = node.getTextContent();
                    if (node.hasAttribute("Marker"))
                    {
                        String strMarker = node.getAttribute("Marker");
                        if (VerifyMarker(strMarker))
                        {
                            markers.add(new Pair<>(strMarker, Task)); // add the marker and the task to the list
                        }
                        else
                        {
                            LOGGER.severe(_WidgetType + "has invalid Marker associated with Task: " + strMarker);
                            return false;
                        }
                    }
                    else
                    {
                        LOGGER.severe(_WidgetType + "has invalid no Marker associated with Task");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean VerifySupportsMedia()
    {
        if (HasBeenVerified())
        {
            return true; // should be OK, because on 1st failure below, init should stop
        }
        setHasBeenVerified(true);
        LOGGER.info("Verifying that the OS has support for " + _WidgetType);
        try
        {
            // try to create a media player with 1st file
            String strFileID = BaseWidget.convertToFileOSSpecific(_MediaFilesAndTags.keySet().iterator().next().toLowerCase());
            Media objMedia; // just a test
            objMedia = getMedia(_MediaURI.get(strFileID));

            MediaPlayer mediaPlayer = new MediaPlayer(objMedia); // will throw exception if not valid
            //LOGGER.info("Verified that the OS has support for " + _WidgetType);
            return true;
        }
        catch (Exception ex)
        {
        }
        LOGGER.severe("Unable to create " + _WidgetType + " - not supported by the Operating System (likely need to install it)");
        return false;
    }

    private boolean VerifyInputFiles()
    {
        boolean retVal = true;
        if (_MediaFilesAndTags.isEmpty())
        {
            LOGGER.severe(_WidgetType + " No Media files specified.");
            retVal = false;
        }
        else
        {
            LOGGER.info("Verifying " + _WidgetType + " input files");
        }
        for (String strKey : _MediaFilesAndTags.keySet())
        {
            String strFile = _MediaFilesAndTags.get(strKey);
            if (!AddMediaFile(strFile, strKey))
            {
                LOGGER.severe(_WidgetType + " Invalid media file: " + strFile);
                retVal = false;
            }
        }
        return retVal;
    }

    private boolean PlayMedia(String strKey)
    {
        strKey = strKey.toLowerCase();
        if (_MediaFilesAndTags.containsKey(strKey))
        {
            if (!strKey.equalsIgnoreCase(_CurrentMediaID) || _mediaPlayer == null) // may be just repeating existing media, no reason to re-load
            {
                OnStop();
                MediaPlayer objPlayer = CreateMediaPlayer(strKey);
                if (null == objPlayer)
                {
                    if (!IsValid())
                    {
                        LOGGER.severe("Platform does not support Media Player: " + _WidgetType);
                    }
                    else
                    {
                        LOGGER.warning("Error creating MediaPlayer ID for " + _WidgetType + "[" + getNamespace() + ":" + getMinionID() + "] : " + strKey);
                    }
                    return false;
                }
                objPlayer.setAutoPlay(_AutoStart);
                //SetupMarkers(objPlayer, strKey);
                _mediaPlayer = objPlayer;
                _CurrentMediaID = strKey;
                _ListOfIDs.get(strKey);
                if (!_EventMarkerMap.get(strKey).isEmpty())
                {
                    _mediaPlayer.totalDurationProperty().addListener(new TotalDurationListener());
                }
            }
            OnNewMedia(_mediaPlayer); // widget specific goodies
            OnSetVolume(_VolumeLevel);
            return true;
        }
        LOGGER.warning("Received unknown ID for " + _WidgetType + "[" + getNamespace() + ":" + getMinionID() + "] : " + strKey);

        return false;
    }

    public boolean isRepeatList()
    {
        return _RepeatList;
    }

    public void setRepeatList(boolean _RepeatList)
    {
        this._RepeatList = _RepeatList;
    }

    public boolean isRepeatSingleMedia()
    {
        return _RepeatSingleMedia;
    }

    public void setRepeatSingleMedia(boolean _RepeatSingleMedia)
    {
        this._RepeatSingleMedia = _RepeatSingleMedia;
    }

    public boolean isAutoStart()
    {
        return _AutoStart;
    }

    public void setAutoStart(boolean _AutoStart)
    {
        this._AutoStart = _AutoStart;
    }

    public double getVolumeLevel()
    {
        return _VolumeLevel;
    }

    public void setVolumeLevel(double _VolumeLevel)
    {
        this._VolumeLevel = _VolumeLevel;
    }

    public String getWidgetType()
    {
        return _WidgetType;
    }

    @Override
    protected void ConfigureDimentions()
    {

    }

    private class TotalDurationListener implements InvalidationListener
    {

        @Override
        public void invalidated(javafx.beans.Observable observable)
        {
            SetupMarkers(_mediaPlayer, _CurrentMediaID);
        }
    }
    
    @Override
    public void UpdateTitle(String strTitle)
    {
        LOGGER.warning("Tried to update Title of a " + _WidgetType + " to " + strTitle);
    }

    @Override
    public void PrepareForAppShutdown()
    {
        if (null != _mediaPlayer)
        {
            _mediaPlayer.stop();  // can leave a hanging thread if you don't do this
        }
    }
    
    
}
