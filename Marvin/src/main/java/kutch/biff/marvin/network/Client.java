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

package kutch.biff.marvin.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick Kutch
 */
public class Client
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final String _Address;
    private final int _Port;
    private InetAddress _IPAddress;
    DatagramSocket _Socket;
    
    public Client(String Address, int Port)
    {
        _Address = Address;
        _Port = Port;
        _IPAddress = null;
        _Socket = null;
        
    }
    private boolean SetupSocket()
    {
        try
        {
           _IPAddress = InetAddress.getByName(_Address); 
           _Socket = new DatagramSocket();
        }
        catch (Exception ex)
        {
            return false;
        }
        return true;
    }
    
    public boolean send(byte[] sendData)
    {
        if (null == _Socket)
        {
            SetupSocket(); // if it fails, will get caught in next try/catch block when tries to use
        }
        try
        {
           DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, _IPAddress, _Port);
           _Socket.send(sendPacket);
        }
        catch (Exception ex)
        {
            LOGGER.severe("Unable to send data to Oscar at [" + getAddress() +":"+Integer.toString(_Port)+"]");
            return false;
        }
        
        return true;
    }
    
    public void Close()
    {
        if (null != _Socket)
        {
            _Socket.close();
            _Socket = null;
        }
    }

    public String getAddress()
    {
        return _Address;
    }

    public int getPort()
    {
        return _Port;
    }
    
}
