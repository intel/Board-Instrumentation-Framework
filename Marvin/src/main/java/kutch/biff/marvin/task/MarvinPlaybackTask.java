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
package kutch.biff.marvin.task;

import java.io.File;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.MarvinPlayback;
import kutch.biff.marvin.utility.MarvinPlaybackManager;
import kutch.biff.marvin.widget.BaseWidget;

/**
 *
 * @author Patrick Kutch
 */

/*
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Load File</Task>
 * <Param>File=MarvinFile.BIFM</Param> </TaskItem>
 * 
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Play</Task>
 * <Param>Repeat=True</Param> <Param>Speed=1.5</Param> </TaskItem>
 * 
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Set Options</Task>
 * <Param>Repeat=False</Param> <Param>Speed=0.5</Param> </TaskItem>
 * 
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Stop</Task> </TaskItem>
 * 
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Play</Task> </TaskItem>
 * 
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Pause</Task>
 * </TaskItem>
 * 
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Resume</Task>
 * </TaskItem>
 * 
 * <TaskItem Type="MarvinPlayback"> <Task PlayerID="Foo">Play File</Task>
 * <Param>File=MarvinFile.BIFM</Param> <Param>Repeat=True</Param>
 * <Param>Speed=1.5</Param> </TaskItem>
 * 
 * 
 * 
 */

public class MarvinPlaybackTask extends BaseTask
{
    public enum PlaybackAction
    {
	STOP, PLAY, PAUSE, RESUME, PLAY_FILE, SET_OPTIONS, LOAD_FILE, INVALID;
    }
    
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private String _PlayerID;
    private PlaybackAction _Action;
    private Double _Speed;
    private boolean _Loop;
    private boolean _LoopSet;
    private String _fileName;
    
    public MarvinPlaybackTask(String PlayerID, PlaybackAction Action)
    {
	_PlayerID = PlayerID;
	_Action = Action;
	_Speed = 0.0;
	_Loop = false;
	_fileName = null;
    }
    
    @Override
    public void PerformTask()
    {
	LOGGER.info("Performing MarvinPlayback Task: " + _Action.toString());
	MarvinPlayback pb = MarvinPlaybackManager.getMarvinPlayback(_PlayerID);
	if (null == pb)
	{
	    LOGGER.severe("Invalid Playback Item");
	    return;
	}
	switch (_Action)
	{
	    case LOAD_FILE:
		if (pb.loadFile(_fileName))
		{
		    if (_Speed > 0.0)
		    {
			pb.setSpeed(_Speed);
		    }
		    if (_LoopSet)
		    {
			pb.setRepeat(_Loop);
		    }
		}
		break;
	    case STOP:
		pb.stopPlayback();
		break;
	    case PLAY:
		pb.Play();
		break;
	    case PAUSE:
		pb.pausePlayback();
		break;
	    case PLAY_FILE:
		if (pb.loadFile(_fileName))
		{
		    if (_Speed > 0.0)
		    {
			pb.setSpeed(_Speed);
		    }
		    if (_LoopSet)
		    {
			pb.setRepeat(_Loop);
		    }
		    pb.Play();
		}
		break;
	    case RESUME:
		pb.resumePlayback();
		break;
	    case SET_OPTIONS:
		if (_Speed > 0.0)
		{
		    pb.setSpeed(_Speed);
		}
		if (_LoopSet)
		{
		    pb.setRepeat(_Loop);
		}
		break;
	    default:
		break;
	}
	
    }
    
    public Double get_Speed()
    {
	return _Speed;
    }
    
    public void set_Speed(Double _Speed)
    {
	if (_Speed <= 0)
	{
	    throw new IllegalArgumentException("must be positive");
	}
	this._Speed = _Speed;
    }
    
    public boolean is_Loop()
    {
	return _Loop;
    }
    
    public void set_Loop(boolean _Loop)
    {
	this._LoopSet = true;
	this._Loop = _Loop;
    }
    
    public String get_fileName()
    {
	return _fileName;
    }
    
    public boolean set_fileName(String _fileName)
    {
	String fName = BaseWidget.convertToFileOSSpecific(_fileName);
	if (null == fName)
	{
	    LOGGER.severe("MarvinPlayback task specified invalid file: " + _fileName);
	    return false;
	}
	File file = new File(fName);
	if (!file.exists())
	{
	    LOGGER.severe("MarvinPlayback task specified invalid file: " + _fileName);
	    return false;
	}
	this._fileName = fName;
	return true;
    }
    
}
