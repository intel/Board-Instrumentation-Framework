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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick.Kutch@gmail.com
 */
public class MarvinPlayback implements Runnable
{
    class DataSet
    {
	public String ID;
	public String Namespace;
	public String Data;
	public int Time;
    }
    
    private String _Name;
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private double _PlaybackSpeed;
    private boolean _LoopPlayback;
    private boolean _Playing;
    private int _NextEntry;
    List<DataSet> _playbackData;
    private boolean _Terminate;
    private boolean _Paused;
    private Thread _PlaybackThread;
    
    public MarvinPlayback(String strName)
    {
	_Name = strName;
	_PlaybackSpeed = 10.0;
	_LoopPlayback = false;
	LOGGER.info("Creating new Marvin Playback with name of " + strName);
    }
    
    String getName()
    {
	return _Name;
    }
    
    public void stopPlayback()
    {
	_Terminate = true;
	if (_Paused)
	{
	    _Paused = false;
	}
	while (_Playing)
	{
	    try
	    {
		Thread.sleep(5);
	    }
	    catch(InterruptedException e)
	    {
	    }
	}
    }
    
    public void pausePlayback()
    {
	if (_Paused)
	{
	    LOGGER.warning("Asked to pause Playback " + getName() + ", when already paused.");
	}
	_Paused = true;
    }
    
    public void resumePlayback()
    {
	if (!_Paused)
	{
	    LOGGER.warning("Asked to resume Playback " + getName() + ", when not paused.");
	}
	_Paused = false;
    }
    
    public void setSpeed(double newSpeed)
    {
	_PlaybackSpeed = newSpeed;
    }
    
    public void setRepeat(boolean repeat)
    {
	_LoopPlayback = repeat;
    }
    
    public void Play()
    {
	if (_Playing)
	{
	    LOGGER.warning("Asked to Playback Playback " + getName() + ", when already playing, starting over.");
	    stopPlayback();
	}
	if (null == _playbackData)
	{
	    LOGGER.warning("Asked to Playback Playback " + getName() + ", but no data loaded.");
	    return;
	}
	
	_NextEntry = 0;
	_Terminate = false;
	_Paused = false;
	_PlaybackThread = new Thread(this);
	_PlaybackThread.start();
    }
    
    public boolean loadFile(String fName)
    {
	if (_Playing)
	{
	    stopPlayback();
	}
	List<DataSet> newData = ReadFile(fName);
	_playbackData = newData;
	
	if (null == newData)
	{
	    return false;
	}
	_NextEntry = 0;
	
	return true;
    }
    
    private List<DataSet> ReadFile(String fName)
    {
	InputStream inputStream;
	BufferedInputStream bis;
	DataInputStream inp;
	List<DataSet> retList = new ArrayList<>();
	
	try
	{
	    inputStream = new FileInputStream(fName);
	    bis = new BufferedInputStream(inputStream);
	    inp = new DataInputStream(bis);
	}
	catch(FileNotFoundException e)
	{
	    LOGGER.severe("Invalid Playback File: " + fName);
	    return null;
	}
	try
	{
	    String fType = new String(inp.readNBytes(4));
	    if (!fType.contentEquals("BIFM"))
	    {
		LOGGER.severe("Invalid Playback File: " + fName);
		return null;
	    }
	    byte fileVersion = inp.readByte();
	    
	    int entries = inp.readInt();
	    int len;
	    LOGGER.info(
		    "Loading " + Integer.toString(entries) + " entries from file " + fName + " Version:" + fileVersion);
	    
	    for (int iLoop = 0; iLoop < entries; iLoop++)
	    {
		DataSet ds = new DataSet();
		ds.Time = inp.readInt();
		len = inp.readInt();
		ds.ID = new String(inp.readNBytes(len));
		
		len = inp.readInt();
		ds.Namespace = new String(inp.readNBytes(len));
		
		len = inp.readInt();
		ds.Data = new String(inp.readNBytes(len));
		retList.add(ds);
		// LOGGER.info(Integer.toString(iLoop));
	    }
	    
	}
	catch(IOException e)
	{
	    LOGGER.severe("Invalid Playback File: " + fName);
	    return null;
	}
	finally
	{
	    try
	    {
		inp.close();
	    }
	    catch(IOException e)
	    {
	    }
	}
	
	return retList;
	
    }
    
    @Override
    public void run()
    {
	DataManager dm = DataManager.getDataManager();
	int lastInterval;
	
	_Playing = true;
	while (false == _Terminate)
	{
	    lastInterval = 0;
	    while (_NextEntry < _playbackData.size() && !_Terminate)
	    {
		DataSet dp = _playbackData.get(_NextEntry);
		int interval = dp.Time;
		if (lastInterval == interval)
		{ // multiple datapoints came it @ same time (like a group), just blast through
		  // them
		    dm.ChangeValue(dp.ID, dp.Namespace, dp.Data);
		}
		else
		{
		    try
		    {
			double sleepTime = (interval - lastInterval) / _PlaybackSpeed;
			
			Thread.sleep((long) sleepTime);
			
			lastInterval = interval;
			
			dm.ChangeValue(dp.ID, dp.Namespace, dp.Data);
		    }
		    catch(InterruptedException e)
		    {
		    }
		}
		while (_Paused && !_Terminate)
		{
		    try
		    {
			Thread.sleep(5);
		    }
		    catch(InterruptedException e)
		    {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		    }
		}
		
		_NextEntry++;
	    }
	    if (_LoopPlayback)
	    {
		_NextEntry = 0;
	    }
	    else
	    {
		_Terminate = true;
	    }
	}
	_PlaybackThread = null;
	LOGGER.info("Playback [" + getName() + "] Finished");
	_Playing = false;
    }
}
