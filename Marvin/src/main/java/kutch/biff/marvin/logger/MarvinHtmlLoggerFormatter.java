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
 * #    File Abstract: Provides the HTML formatted log file.
 * #    
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class MarvinHtmlLoggerFormatter extends Formatter
{
    // helper to format nice timesamp
    public static String getDateStr(LogRecord rec)
    {
	long milliseconds = rec.getMillis();
	SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm");
	Date retVal = new Date(milliseconds);
	return date_format.format(retVal);
    }
    
    // Called by Log framework to add an entry
    @Override
    public String format(LogRecord record)
    {
	StringBuilder strBuffer = new StringBuilder(1000);
	strBuffer.append("<tr>");
	// Color code based on severity
	if (record.getLevel().intValue() >= Level.WARNING.intValue())
	{
	    if (record.getLevel().intValue() > Level.WARNING.intValue())
	    {
		strBuffer.append("<TD BGCOLOR=\"#FF0000\">");
	    }
	    else
	    {
		strBuffer.append("<TD BGCOLOR=\"#FFFF00\">");
	    }
	    
	    strBuffer.append("<b>");
	    strBuffer.append(record.getLevel());
	    strBuffer.append("</b>");
	}
	else
	{
	    strBuffer.append("<td>");
	    strBuffer.append(record.getLevel());
	}
	
	strBuffer.append("</td>");
	
	strBuffer.append("<td>");
	strBuffer.append(getDateStr(record));
	strBuffer.append("</td>");
	strBuffer.append("<td>");
	String Message = record.getMessage();
	if (record.getMessage().contains("\n"))
	{
	    Message = record.getMessage().replaceAll("\n", "<br>");
	}
	strBuffer.append(Message);
	return strBuffer.toString();
    }
    
    // Called to write the file header
    @Override
    public String getHead(Handler h)
    {
	String strHeader = "<HTML>\n<HEAD>\n<center>" + "BIFF Framework - Marvin Log: " + (new Date()) + "</center>"
		+ "\n</HEAD>\n<BODY>\n<PRE>\n" + "<table width=\"100%\" border>\n  " + "<tr><th>Level</th>"
		+ "<th>Time</th>" + "<th>Message</th>" + "</tr>\n";
	
	return strHeader;
    }
    
    // last thing called
    @Override
    public String getTail(Handler h)
    {
	return "</table>\n  </PRE></BODY>\n</HTML>\n";
    }
}
