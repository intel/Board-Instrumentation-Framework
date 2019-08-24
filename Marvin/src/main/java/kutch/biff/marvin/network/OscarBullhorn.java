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
 * #  Utility class that allows Marvin to send a message to an Oscar, that in turn
 * #  instructs Oscar to add this marvin to it's list of targets.
 * #  Hash generation code from http://www.codejava.net/coding/how-to-calculate-md5-and-sha-hash-values-in-java
 * ##############################################################################
 */
package kutch.biff.marvin.network;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 *
 * @author Patrick
 */
public class OscarBullhorn extends Client
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    
    private static String convertByteArrayToHexString(byte[] arrayBytes)
    {
	StringBuffer stringBuffer = new StringBuffer();
	for (int i = 0; i < arrayBytes.length; i++)
	{
	    stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
	}
	return stringBuffer.toString();
    }
    
    private static String hashString(String message, String algorithm)
    {
	MessageDigest digest;
	try
	{
	    digest = MessageDigest.getInstance(algorithm);
	}
	catch(NoSuchAlgorithmException ex)
	{
	    Logger.getLogger(OscarBullhorn.class.getName()).log(Level.SEVERE, null, ex);
	    return null;
	}
	byte[] hashedBytes;
	try
	{
	    hashedBytes = digest.digest(message.getBytes("UTF-8"));
	}
	catch(UnsupportedEncodingException ex)
	{
	    Logger.getLogger(OscarBullhorn.class.getName()).log(Level.SEVERE, null, ex);
	    return null;
	}
	
	return convertByteArrayToHexString(hashedBytes);
    }
    
    private String _Key;
    
    public OscarBullhorn(String Address, int Port, String Key)
    {
	super(Address, Port);
	_Key = Key;
    }
    
    public boolean SendNotification()
    {
	String HashStr = OscarBullhorn.hashString(_Key, "MD5");
	
	if (null == HashStr)
	{
	    return false;
	}
	
	Random rnd = new Random();
	String UniqueID = Integer.toString(rnd.nextInt());
	String strPort = Integer.toString(Configuration.getConfig().getPort());
	String strHostname = "Unknown";
	try
	{
	    strHostname = InetAddress.getLocalHost().getHostName();
	}
	catch(UnknownHostException ex)
	{
	    Logger.getLogger(OscarBullhorn.class.getName()).log(Level.SEVERE, null, ex);
	}
	
	/*
	 * <?xml version="1.0" encoding="UTF-8"?>" <Marvin Type="Bullhorn">
	 * <Version>1.0</Version> <UniqueID>234234</UniqueID>
	 * <Key>sadfjklasdfjklsdafdsa</Key> <Port>5300</Port> </Marvin>
	 * 
	 */
	String sendBuffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	sendBuffer += "<Marvin Type=\"Bullhorn\">";
	sendBuffer += "<Version>1.0</Version>";
	sendBuffer += "<UniqueID>" + UniqueID + "</UniqueID>";
	sendBuffer += "<Hostname>" + strHostname + "</Hostname>";
	sendBuffer += "<Key>" + HashStr + "</Key>";
	sendBuffer += "<Port>" + strPort + "</Port>";
	sendBuffer += "</Marvin>";
	
	LOGGER.log(Level.CONFIG, "Sending Oscar Bullhorn notification to: {0}:{1}",
		new Object[] { getAddress(), Integer.toString(getPort()) });
	
	// is UDP so send it thrice
	boolean retVal = send(sendBuffer.getBytes());
	if (retVal)
	{
	    send(sendBuffer.getBytes());
	    send(sendBuffer.getBytes());
	}
	
	Close();
	return retVal;
    }
}
