/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kutch.biff.marvin.task;

import kutch.biff.marvin.network.OscarBullhorn;

/**
 *
 * @author Patrick
 */
public class OscarBindTask extends BaseTask
{

    public OscarBindTask()
    {
        
    }
    
    @Override
    public void PerformTask()
    {
        if (null != getParams() && getParams().size() == 3)
        {
            int Port;
            String Address = getParams().get(0).toString();
            try
            {
                Port = Integer.parseInt(getParams().get(1).toString());
            }
            catch (Exception ex)
            {
                LOGGER.severe("Asked to perform an OscarBindTask with invalid port: " + getParams().get(1).toString());
                return;
            }
                  
            String Key = getParams().get(2).toString();
            
            OscarBullhorn objBH = new OscarBullhorn(Address,Port,Key);
            objBH.SendNotification();
            LOGGER.info("Sending OscarBind to " + Address + ":" + Integer.toString(Port) + " Key:" + Key);
        }
        else
        {
            LOGGER.severe("Asked to perform an OscarBindTask with no or incorrect number of params");
        }
    }
    
}
