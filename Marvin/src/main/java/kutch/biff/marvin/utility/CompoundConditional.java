/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kutch.biff.marvin.utility;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;

/**
 *
 * @author Patrick
 */
public class CompoundConditional extends Conditional
{
    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final TaskManager TASKMAN = TaskManager.getTaskManager();
    protected ArrayList<Conditional> AndList;
    protected ArrayList<Conditional> OrList;
    public CompoundConditional(Type type)
    {
        super(type,true);
        AndList = new ArrayList<>();
        OrList = new ArrayList<>();
        _UsesThen = true;
    }
    
    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 59 * hash + AndList.hashCode();
        hash = 59 * hash + OrList.hashCode();
        return hash;
    }    

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final CompoundConditional other = (CompoundConditional) obj;
        if (!Objects.equals(this.AndList, other.AndList))
        {
            return false;
        }
        if (!Objects.equals(this.OrList, other.OrList))
        {
            return false;
        }
        return true;
    }
    
    private static Conditional ReadCompoundConditional(FrameworkNode condNode)
    {
        String strType = null;
        boolean CaseSensitive = false;

        Utility.ValidateAttributes(new String[]
        {
            "CaseSensitive", "Type"
        }, condNode);
        if (!condNode.hasAttribute("type"))
        {
            LOGGER.severe("Conditional defined with no Type");
            return null;
        }
        if (condNode.hasAttribute("CaseSensitive"))
        {
            CaseSensitive = condNode.getBooleanAttribute("CaseSensitive");
        }

        strType = condNode.getAttribute("type");
        Conditional.Type type = Conditional.GetType(strType);
        if (type == Conditional.Type.Invalid)
        {
            LOGGER.severe("Compound Conditional defined with invalid type: " + strType);
            return null;
        }

        Conditional objConditional = Conditional.BuildConditional(type, condNode,false);
        if (null != objConditional)
        {
            objConditional.setCaseSensitive(CaseSensitive);
        }

        return objConditional;
    }

    
    @Override
    protected boolean readCondition(FrameworkNode condNode)    
    {
        if (!super.readCondition(condNode))
        {
            return false;
        }
        for (FrameworkNode andNode : condNode.getChildNodes("And"))
        {
            Conditional andCond = ReadCompoundConditional(andNode);
            if (null != andCond)
            {
                AndList.add(andCond);
            }
            else
            {
                return false;
            }
        }
        for (FrameworkNode orNode : condNode.getChildNodes("Or"))
        {
            Conditional orCond = ReadCompoundConditional(orNode);
            if (null != orCond)
            {
                OrList.add(orCond);
            }
            else
            {
                return false;
            }
        }
        return true;
    }
    
    protected void Evaluated(boolean evaluation)
    {
        if (evaluation)
        {
            TASKMAN.AddDeferredTask(getThenTask());
        }
        else if (getElseTask() != null)
        {
            TASKMAN.AddDeferredTask(getElseTask());
        }
    }

    /**
     *
     * @param Val1
     */
    @Override
   protected void Perform(String Val1)
   {
        String Val2 = GetValue2();
        if (null == Val1 || null == Val2)
        {
            LOGGER.warning("Tried to perform Conditional, but data not yet available");
            return;
        }

        // do 'normal' evauation of 1st minionsrc (that changed and triggered this evaluation)
        boolean primaryEvaluation = Conditional.EvaluateConditional(Val1, Val2, getType(), isCaseSensitive());
        // now go check all the OR's - if any are TRUE, then go perform Then task, else go try ANDs
        if (!OrList.isEmpty())
        {
            if (primaryEvaluation)
            {
                Evaluated(true);
                return;
            }
            else
            {
                for (Conditional orCond : OrList)
                {
                    String val1 = orCond.GetValue1();
                    String val2 = orCond.GetValue2();
                    if (null == val1 || null == val2)
                    {
                        LOGGER.warning("Tried to perform Conditional [OR], but data not yet available");
                        return;
                    }
                    if (Conditional.EvaluateConditional(val1,val2, orCond.getType(),orCond.isCaseSensitive()))
                    {
                        Evaluated(true);
                        return;
                    }
                }
            }
        }
        // No OR's, or none of the evauated to true, so go look at all ANDs
        if (!AndList.isEmpty())
        {
            if (!primaryEvaluation) // since now in AND's the 1st had to be True to continue, it wasn't
            {
                Evaluated(false);
                return;
            }
            else
            {
                for (Conditional andCond : AndList)
                {
                    String val1 = andCond.GetValue1();
                    String val2 = andCond.GetValue2();
                    if (null == val1 || null == val2)
                    {
                        LOGGER.warning("Tried to perform Conditional [AND], but data not yet available");
                        return;
                    }
                    if (!Conditional.EvaluateConditional(val1,val2, andCond.getType(),andCond.isCaseSensitive()))
                    {
                        Evaluated(false);// didn't pass, so is now an or
                        return;
                    }
                }
            }
            Evaluated(true);
            return;
        }
   }
}
