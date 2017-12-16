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

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.FrameworkNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Patrick Kutch
 */
public class ReceiveThreadMgr implements Runnable
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final static Configuration CONFIG = Configuration.getConfig();
    private final TaskManager TASKMAN = TaskManager.getTaskManager();

    private boolean _fStopped;
    private boolean fKillRequested;
    private final DatagramSocket _socket;
    private final DataManager _DataManager;
    private final HashMap<String, String> LastMarvinTaskReceived;
    private final LinkedBlockingQueue _DataQueue;
    private final AtomicInteger _WorkerThreadCount;

    public ReceiveThreadMgr(DatagramSocket sock, DataManager DM)
    {
        _fStopped = false;
        fKillRequested = false;
        _socket = sock;
        _DataManager = DM;
        LastMarvinTaskReceived = new HashMap<>();
        _DataQueue = new LinkedBlockingQueue();
        _WorkerThreadCount = new AtomicInteger();
        _WorkerThreadCount.set(0);
    }
    
    @Override
    public void run()
    {
        Runnable processQueuedDataThread = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while (false == fKillRequested)
                    {
                        if (!_DataQueue.isEmpty())
                        {
                            HashMap<InetAddress, String> dataItem = (HashMap<InetAddress, String>) _DataQueue.take();
                            //dataItem[dataItem.keySet()[0]]
                            InetAddress addr = dataItem.keySet().iterator().next();
                            Process(dataItem.get(addr).getBytes(), addr);
                        }
                        else
                        {
                            if (_WorkerThreadCount.get() > 1)
                            {
                                _WorkerThreadCount.decrementAndGet();
                                LOGGER.info("Reducing processing Thread Count");
                                return;
                            }
                            try
                            {
                                Thread.sleep(5);  // didn't read anything, socket read timed out, so take a nap
                            }
                            catch (InterruptedException ex1)
                            {
                            }
                        }
                    }
                    _WorkerThreadCount.decrementAndGet();
                    //LOGGER.info("Receive Queue Processing Thread successfully terminated.");
                }
                catch (InterruptedException e)
                {
                    LOGGER.severe(e.toString());
                }
            }
        };

        Thread procThread = new Thread(processQueuedDataThread,">>>> Base Process Queue Thread <<<<<");

        procThread.start();
        _WorkerThreadCount.set(1);

        while (false == fKillRequested)
        {
            byte[] buffer = new byte[CONFIG.getMaxPacketSize()];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try
            {
                _socket.receive(packet);
                if (false == fKillRequested)
                {
                    String trimmed = new String(packet.getData(), 0, packet.getLength());

                    _DataQueue.add(new HashMap<InetAddress, String>()
                    {
                        {
                            put(packet.getAddress(), trimmed);

                            if (_WorkerThreadCount.get() < 1 || _DataQueue.size()/_WorkerThreadCount.get() > 100)
                            {
                                LOGGER.info("Traffic burst - adding processing Thread Count, there are " + Integer.toString(_DataQueue.size()) + " packets to process.");
                                int threadNum = _WorkerThreadCount.incrementAndGet();
                                Thread procThread = new Thread(processQueuedDataThread,">>>> Additionial Process Queue Thread #" + Integer.toString(threadNum)+" <<<<<");
                                procThread.start();
                            }
                        }
                    });
                    //Process(trimmed.getBytes(), packet.getAddress());
                }
                else
                {
                    return; // kill
                }
                
            }
            catch (IOException ex)
            {
                if (false == fKillRequested)
                {
                    try
                    {
                        Thread.sleep(1);  // didn't read anything, socket read timed out, so take a nap
                    }
                    catch (InterruptedException ex1)
                    {
                    }
                }
            }
        }
        
        procThread.stop();
        _fStopped = true;
    }

    public void Stop()
    {
        _fStopped = false;
        fKillRequested = true;
        try
        {
            Thread.sleep(50); // let the worker theads have a chance to end
        }
        catch (InterruptedException ex)
        {
            
        }
        int tryCount = 0;
        while (false == _fStopped || _WorkerThreadCount.get() > 1)
        {
            tryCount += 1;

            try
            {
                Thread.sleep(50);
                //LOGGER.info("Waiting:" + Boolean.toString(_fStopped)+ ":" + Integer.toString(_WorkerThreadCount.get()));
            }
            catch (InterruptedException ex)
            {
            }
            
            if (tryCount ++ > 100) // don't think this will every happen again, fixed problem elsewhere
            {
                LOGGER.severe("Problem trying to terminate Receive Thread.  Using Brute Force.");
                for (Thread threadObj : Thread.getAllStackTraces().keySet())
                {
                    if (threadObj.getState() == Thread.State.RUNNABLE)
                    {
                        threadObj.interrupt();
                    }
                }
                return;
            }
        }
    }

    private void Process(byte[] Packet, InetAddress address)
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc;
        String str = new String(Packet);

        try
        {
            db = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
            return;
        }

        try
        {
            doc = db.parse(new InputSource(new StringReader(str)));
        }
        catch (SAXException | IOException ex)
        {
            LOGGER.warning("Received Invalid packet: " + str);
            LOGGER.warning(ex.toString());

            return;
        }
        NodeList Children = doc.getChildNodes(); // convert to my node
        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++)
        {
            Node node = Children.item(iLoop);
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
            }
            else if (node.getNodeName().equalsIgnoreCase("Oscar"))
            {
                HandleIncomingOscarPacket(node, address);
            }
            else if (node.getNodeName().equalsIgnoreCase("OscarGroup"))
            {
                HandleIncomingOscarGroupPacket(node, address);
            }
            else if (node.getNodeName().equalsIgnoreCase("Marvin"))
            {
                HandleIncomingMarvinPacket(node);
            }

            else
            {
                LOGGER.warning("Unknown Packet received: " + node.getNodeName());
            }
        }
    }

    private void HandleIncomingMarvinPacket(Node objNode)
    {
        FrameworkNode node = new FrameworkNode(objNode);
        if (!node.hasAttribute("Type"))
        {
            LOGGER.severe("Received Marvin Packet with not Type attribute");
            return;
        }
        String type = node.getAttribute("Type");
        if (type.equalsIgnoreCase("RemoteMarvinTask"))
        {
            HandleIncomingRemoteMarvinTaskPacket(objNode);
        }
        else
        {
            LOGGER.severe("Received Oscar Packet with unknown Type: " + type);
        }

    }

    private void HandleIncomingOscarPacket(Node objNode, InetAddress address)
    {
        FrameworkNode node = new FrameworkNode(objNode);
        if (!node.hasAttribute("Type"))
        {
            LOGGER.severe("Received Oscar Packet with not Type attribute");
            return;
        }
        String type = node.getAttribute("Type");
        if (type.equalsIgnoreCase("Data"))
        {
            HandleIncomingDatapoint(objNode);
        }
        else if (type.equalsIgnoreCase("ConnectionInformation"))
        {
            HandleIncomingOscarConnectionInfoPacket(objNode, address);
        }
        else
        {
            LOGGER.severe("Received Oscar Packet with unknown Type: " + type);
        }
    }

    /**
     * Parses through a grouped packet
     *
     * @param objNode
     * @param address
     */
    private void HandleIncomingOscarGroupPacket(Node objNode, InetAddress address)
    {
        NodeList children = objNode.getChildNodes();
        for (int iLoop = 0; iLoop < children.getLength(); iLoop++)
        {
            HandleIncomingOscarPacket(children.item(iLoop), address);
        }
    }

    private void HandleIncomingDatapoint(Node dpNode)
    {
        NodeList Children = dpNode.getChildNodes();
        String ID = null;
        String Namespace = null;
        String Data = null;
        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++)
        {
            Node node = Children.item(iLoop);
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
                continue;
            }
            else if (node.getNodeName().equalsIgnoreCase("Version"))
            {

            }
            else if (node.getNodeName().equalsIgnoreCase("Namespace"))
            {
                Namespace = node.getTextContent();
            }
            else if (node.getNodeName().equalsIgnoreCase("ID"))
            {
                ID = node.getTextContent();
            }
            else if (node.getNodeName().equalsIgnoreCase("Value"))
            {
                Data = node.getTextContent();
                FrameworkNode objNode = new FrameworkNode(node);
                if (objNode.hasAttribute("LiveData"))
                {
                    String strLive = objNode.getAttribute("LiveData");
                    if (strLive.equalsIgnoreCase("True"))
                    {
                        CONFIG.OnLiveDataReceived();
                    }
                    else if (strLive.equalsIgnoreCase("False"))
                    {
                        CONFIG.OnRecordedDataReceived();
                    }
                    else
                    {
                        LOGGER.warning("Received Data packet with unknown LiveData attribute: " + strLive);
                    }
                }

            }
            else
            {
                LOGGER.warning("Unknown Tag in received Datapoint: " + node.getNodeName());
            }
        }

        if (ID != null && Namespace != null && Data != null)
        {
            _DataManager.ChangeValue(ID, Namespace, Data);
        }
        else
        {
            LOGGER.severe("Malformed Data Received: " + dpNode.getTextContent());
        }
    }

    private void HandleIncomingOscarConnectionInfoPacket(Node adminNode, InetAddress address)
    {
        NodeList Children = adminNode.getChildNodes();
        String OscarID = null;
        String OscarVersion = "Unknown";
        int Port = 0;

        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++)
        {
            Node node = Children.item(iLoop);
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment"))
            {
                continue;
            }
            else if (node.getNodeName().equals("Version"))
            {
                continue;
            }
            else if (node.getNodeName().equals("OscarVersion"))
            {
                OscarVersion = node.getTextContent();
            }
            else if (node.getNodeName().equals("ID"))
            {
                OscarID = node.getTextContent();
            }
            if (node.getNodeName().equalsIgnoreCase("Port"))
            {
                String strPort = node.getTextContent();
                try
                {
                    Port = Integer.parseInt(strPort);
                }
                catch (NumberFormatException ex)
                {
                    LOGGER.severe("Received invalid Connection Information packet from Oscar " + node.toString());
                }
            }
        }
        if (OscarID != null)
        {
            TASKMAN.OscarAnnouncementReceived(OscarID, address.getHostAddress(), Port, OscarVersion);
        }
    }

    private void HandleIncomingRemoteMarvinTaskPacket(Node baseNode)
    {
        /*
         <?xml version=\"1.0\" encoding=\"UTF-8\"?>
         <RemoteMarvinTask>
         <Version>1.0</Version>
         <Requester> 192.168.1.1</Requester>
         <MarvinID>DemoApp</MarvinID>
         <Task>Button2Push</Task>
         </RemoteMarvinTask>
         */
        FrameworkNode node = new FrameworkNode(baseNode);
        try
        {
            String Version = node.getChild("Version").getTextContent();
            String Remote = node.getChild("Requester").getTextContent();
            String MarvinID = node.getChild("MarvinID").getTextContent();
            String Task = node.getChild("Task").getTextContent();
            if (!Version.equalsIgnoreCase("1.0"))
            {
                String RequestNumber = node.getChild("RequestNumber").getTextContent();
                if (LastMarvinTaskReceived.containsKey(Remote))
                {
                    if (LastMarvinTaskReceived.get(Remote).equalsIgnoreCase(RequestNumber))
                    {
                        return; // Already received this one, remember is UDP so it is sent a few times
                    }
                }
                LastMarvinTaskReceived.put(Remote, RequestNumber);
            }

            if (false == MarvinID.equalsIgnoreCase(CONFIG.GetApplicationID()) && false == MarvinID.equalsIgnoreCase("Broadcast"))
            {
                LOGGER.info("Received Remote Marvin Task, but is not targeted at this Marvin, is going to :" + MarvinID);
                return;
            }
            LOGGER.info("Received RemoteMarvinTask [" + Task + " ]from [" + Remote + "]");
            TASKMAN.AddDeferredTask(Task); // can't run it here, because in worker thread, so queue it up for later
        }
        catch (Exception ex)
        {
            LOGGER.warning("Received invalid RemoteMarvinTask:" + baseNode.toString());
        }
    }
}
