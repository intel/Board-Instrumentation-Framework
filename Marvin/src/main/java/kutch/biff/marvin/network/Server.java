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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class Server {
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    private DatagramSocket _socket;
    private ReceiveThreadMgr _RecvThreadMgr;
    private Thread _Thread;
    private DataManager _DataManager;

    public Server(DataManager DM) {
        _socket = null;
        _RecvThreadMgr = null;
        _DataManager = DM;
    }

    public boolean Setup(String IpAddr, int Port) {
        InetAddress address;
        try {
            address = InetAddress.getByName(IpAddr);
        } catch (UnknownHostException ex) {
            LOGGER.severe("Problem setting up network - likely something wrong with Address or Port: " + IpAddr + ":"
                    + Integer.toString(Port));
            return false;
        }

        try {
            _socket = new DatagramSocket(Port, address);
            _socket.setSoTimeout(10);
        } catch (SocketException ex) {
            LOGGER.severe(
                    "Problem setting up network - likely something already using port: " + Integer.toString(Port));
            return false;
        }
        return true;
    }

    public void Start() {
        _RecvThreadMgr = new ReceiveThreadMgr(_socket, _DataManager);
        _Thread = new Thread(_RecvThreadMgr, "Receve Thread Manager Worker");
        _Thread.start();
    }

    public void Stop() {
        if (null != _RecvThreadMgr) {
            _RecvThreadMgr.Stop();
        }
    }
}
