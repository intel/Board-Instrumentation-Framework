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
package kutch.biff.marvin.utility;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;

/**
 *
 * @author Patrick Kutch
 */
public class Conditional
{

    @Override
    public String toString()
    {
        return "Conditional{" + "_Value1_ID=" + _Value1_ID + ", _Value1_Namespace=" + _Value1_Namespace + ", _Value2_ID=" + _Value2_ID + ", _Value2_Namespace=" + _Value2_Namespace + ", _Value2=" + _Value2 + ", _type=" + _type + ", _If_Task=" + _If_Task + ", _Else_Task=" + _Else_Task + '}';
    }

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private final TaskManager TASKMAN = TaskManager.getTaskManager();

    public enum Type
    {
        EQ, NE, GT, GE, LT, LE, CASE, Invalid
    };
    private String _Value1_ID;
    private String _Value1_Namespace;
    private String _Value2_ID;
    private String _Value2_Namespace;
    private String _Value2;
    private final Type _type;
    private String _If_Task;
    private String _Else_Task;
    private boolean _CaseSensitive;
    protected boolean _UsesThen;
/*
    public Conditional(Conditional.Type type)
    {
        _type = type;
        _Value1_ID = null;
        _Value1_Namespace = null;
        _Value2_ID = null;
        _Value2_Namespace = null;
        _Value2 = null;
        _If_Task = null;
        _Else_Task = null;
        _UsesThen = true;
    }
*/
    public Conditional(Conditional.Type type,boolean usesThen)
    {
        _type = type;
        _Value1_ID = null;
        _Value1_Namespace = null;
        _Value2_ID = null;
        _Value2_Namespace = null;
        _Value2 = null;
        _If_Task = null;
        _Else_Task = null;
        _UsesThen = usesThen;
    }
    
    protected String getThenTask()
    {
        return _If_Task;
    }
    protected String getElseTask()
    {
        return _Else_Task;
    }
    public void SetNamespaceAndID(String namespace, String id)
    {
        _Value1_ID = id;
        _Value1_Namespace = namespace;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this._Value1_ID);
        hash = 59 * hash + Objects.hashCode(this._Value1_Namespace);
        hash = 59 * hash + Objects.hashCode(this._Value2_ID);
        hash = 59 * hash + Objects.hashCode(this._Value2_Namespace);
        hash = 59 * hash + Objects.hashCode(this._Value2);
        hash = 59 * hash + Objects.hashCode(this._type);
        hash = 59 * hash + Objects.hashCode(this._Else_Task);
        hash = 59 * hash + (this._CaseSensitive ? 1 : 0);
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
        final Conditional other = (Conditional) obj;
        if (this._CaseSensitive != other._CaseSensitive)
        {
            return false;
        }
        if (!Objects.equals(this._Value1_ID, other._Value1_ID))
        {
            return false;
        }
        if (!Objects.equals(this._Value1_Namespace, other._Value1_Namespace))
        {
            return false;
        }
        if (!Objects.equals(this._Value2_ID, other._Value2_ID))
        {
            return false;
        }
        if (!Objects.equals(this._Value2_Namespace, other._Value2_Namespace))
        {
            return false;
        }
        if (!Objects.equals(this._Value2, other._Value2))
        {
            return false;
        }
        if (!Objects.equals(this._If_Task, other._If_Task))
        {
            return false;
        }
        if (!Objects.equals(this._Else_Task, other._Else_Task))
        {
            return false;
        }
        if (this._type != other._type)
        {
            return false;
        }
        return true;
    }
    
    protected Type getType()
    {
        return _type;
    }

    public static Type GetType(String strType)
    {
        if (null == strType)
        {
            return Type.Invalid;
        }

        if (strType.equalsIgnoreCase("IF_EQ"))
        {
            return Type.EQ;
        }
        if (strType.equalsIgnoreCase("IF_NE"))
        {
            return Type.NE;
        }
        if (strType.equalsIgnoreCase("IF_GE"))
        {
            return Type.GE;
        }
        if (strType.equalsIgnoreCase("IF_GT"))
        {
            return Type.GT;
        }
        if (strType.equalsIgnoreCase("IF_LE"))
        {
            return Type.LE;
        }
        if (strType.equalsIgnoreCase("IF_LT"))
        {
            return Type.LT;
        }
        if (strType.equalsIgnoreCase("IF_EQ"))
        {
            return Type.EQ;
        }
        if (strType.equalsIgnoreCase("CASE"))
        {
            return Type.CASE;
        }
        return Type.Invalid;
    }

    public void Enable()
    {
        DataManager.getDataManager().AddListener(_Value1_ID, _Value1_Namespace, new ChangeListener()
                                         {
                                             @Override
                                             @SuppressWarnings("unchecked")
                                             public void changed(ObservableValue o, Object oldVal, Object newVal)
                                             {
                                                 Perform(newVal.toString());
                                             }
                                         });
    }
    // used for compound conditionals only
    protected String GetValue1()
    {
        return DataManager.getDataManager().GetValue(_Value1_ID, _Value1_Namespace);
    }

    protected String GetValue2()
    {
        if (_Value2_ID != null && _Value2_Namespace != null)
        {
            return DataManager.getDataManager().GetValue(_Value2_ID, _Value2_Namespace);
        }
        return _Value2;
    }

    public static boolean EvaluateConditional(String strValue1, String strValue2, Type Conditional, boolean isCaseSensitive)
    {
        boolean result;
        
        try
        {
            result = PerformValue(Double.parseDouble(strValue1), Double.parseDouble(strValue2), Conditional);
        }
        catch (NumberFormatException ex)
        {
            result = PerformString(strValue1, strValue2, Conditional, isCaseSensitive);
        }
        return result;
    }

   
    protected void Perform(String Val1)
    {
        String Val2 = GetValue2();
        if (null == Val1 || null == Val2)
        {
            LOGGER.warning("Tried to perform Conditional, but data not yet available");
            return;
        }

        if (Conditional.EvaluateConditional(Val1, Val2, _type, isCaseSensitive()))
        {
            TASKMAN.AddDeferredTask(_If_Task);
        }
        else if (_Else_Task != null)
        {
            TASKMAN.AddDeferredTask(_Else_Task);
        }
    }

    protected static boolean PerformString(String Val1, String Val2, Type testType, boolean isCaseSensitive)
    {
        if (!isCaseSensitive)
        {
            Val1 = Val1.toLowerCase();
            Val2 = Val2.toLowerCase();
        }

        Val1 = Val1.trim();
        Val2 = Val2.trim();
        switch (testType)
        {
            case EQ:
                return Val1.equals(Val2);
            case NE:
                return !Val1.equals(Val2);

            case GT:
                return Val1.compareTo(Val2) > 0;

            case GE:
                return Val1.compareTo(Val2) >= 0;

            case LT:
                return Val1.compareTo(Val2) < 0;

            case LE:
                return Val1.compareTo(Val2) <= 0;
        }
        return false;
    }

    protected static boolean PerformValue(double Val1, double Val2, Type testType)
    {
        switch (testType)
        {
            case EQ:
                return Val1 == Val2;

            case NE:
                return Val1 != Val2;

            case GT:
                return Val1 > Val2;

            case GE:
                return Val1 >= Val2;

            case LT:
                return Val1 < Val2;

            case LE:
                return Val1 <= Val2;
        }
        return false;
    }

    public String getValue1_Namespace()
    {
        return _Value1_Namespace;
    }

    public void setValue1_Namespace(String _Value1_Namespace)
    {
        this._Value1_Namespace = _Value1_Namespace;
    }

    public String getValue2_ID()
    {
        return _Value2_ID;
    }

    public void setValue2_ID(String _Value2_ID)
    {
        this._Value2_ID = _Value2_ID;
    }

    public String getValue2_Namespace()
    {
        return _Value2_Namespace;
    }

    public void setValue2_Namespace(String _Value2_Namespace)
    {
        this._Value2_Namespace = _Value2_Namespace;
    }

    public String getValue2()
    {
        return _Value2;
    }

    public void setValue2(String _Value2)
    {
        this._Value2 = _Value2;
    }

    public String getIf_Task()
    {
        return _If_Task;
    }

    public void setIf_Task(String _If_Task)
    {
        this._If_Task = _If_Task;
    }

    public String getElse_Task()
    {
        return _Else_Task;
    }

    public void setElse_Task(String _Else_Task)
    {
        this._Else_Task = _Else_Task;
    }

    public boolean isCaseSensitive()
    {
        return _CaseSensitive;
    }

    public void setCaseSensitive(boolean _CaseSensitive)
    {
        this._CaseSensitive = _CaseSensitive;
    }

    protected boolean ReadMinionSrc(FrameworkNode condNode)
    {
        String ID = null, Namespace = null;

        for (FrameworkNode node : condNode.getChildNodes())
        {
            String strValue = null;
            String strTask = null;
            if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#Comment"))
            {
                continue;
            }
            if (node.getNodeName().equalsIgnoreCase("MinionSrc"))
            {
                if (node.hasAttribute("ID"))
                {
                    ID = node.getAttribute("ID");
                }
                else
                {
                    LOGGER.severe("Conditional defined with invalid MinionSrc, no ID");
                    return false;
                }
                if (node.hasAttribute("Namespace"))
                {
                    Namespace = node.getAttribute("Namespace");
                }
                else
                {
                    LOGGER.severe("Conditional defined with invalid MinionSrc, no Namespace");
                    return false;
                }
            }
        }
        SetNamespaceAndID(Namespace, ID);
        return true;
    }

    protected boolean readCondition(FrameworkNode condNode)
    {
        boolean retVal = true;

        if (ReadMinionSrc(condNode))
        {
            for (FrameworkNode node : condNode.getChildNodes())
            {
                if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#Comment"))
                {
                    continue;
                }
                else if (node.getNodeName().equalsIgnoreCase("MinionSrc"))
                {
                    continue;
                }
                if (node.getNodeName().equalsIgnoreCase("Value"))
                {
                    if (node.hasChild("MinionSrc"))
                    {
                        FrameworkNode valNode = node.getChild("MinionSrc");
                        if (valNode.hasAttribute("ID"))
                        {
                            _Value2_ID = valNode.getAttribute("ID");
                        }
                        else
                        {
                            LOGGER.severe("Conditional <Value><MinionSrc> defined with invalid MinionSrc, no ID");
                            retVal = false;
                        }
                        if (valNode.hasAttribute("Namespace"))
                        {
                            _Value2_Namespace = valNode.getAttribute("Namespace");
                        }
                        else
                        {
                            LOGGER.severe("Conditional  <Value><MinionSrc> defined with invalid MinionSrc, no Namespace");
                            retVal = false;
                        }
                    }
                    else
                    {
                        _Value2 = node.getTextContent();
                        if (_Value2.length() < 1)
                        {
                            LOGGER.severe("Conditional <Value> is empty");
                            retVal = false;
                        }
                    }
                }

                if (_UsesThen)
                {
                    if (node.getNodeName().equalsIgnoreCase("Then"))
                    {
                        _If_Task = node.getTextContent();
                        if (_If_Task.length() < 1)
                        {
                            LOGGER.severe("Conditional <Then> is empty");
                            retVal = false;
                        }
                    }

                    if (node.getNodeName().equalsIgnoreCase("Else"))
                    {
                        _Else_Task = node.getTextContent();
                        if (_Else_Task.length() < 1)
                        {
                            LOGGER.severe("Conditional <Else> is empty");
                            retVal = false;
                        }
                    }
                }
            }
            if (null == _If_Task && _UsesThen)
            {
                LOGGER.severe("Conditional defined with no <Then>");
                retVal = false;
            }

            if (null == _Value1_ID && _Value2 == null)
            {
                LOGGER.severe("Conditional defined with no <Value>");
                retVal = false;
            }
        }
        else
        {
            retVal = false;
        }
        return retVal;
    }

    public static Conditional BuildConditional(Type type, FrameworkNode condNode,boolean usesThen)
    {
        Conditional objCond = null;
        if (type == Type.CASE)
        {
            objCond = ConditionalCase.BuildConditionalCase(condNode);
        }
        else if (type != Type.Invalid)
        {
            ArrayList<FrameworkNode> OrChildren = condNode.getChildNodes("OR");
            ArrayList<FrameworkNode> AndChildren = condNode.getChildNodes("And");

            if (OrChildren.isEmpty() && AndChildren.isEmpty())
            {
                
                objCond = new Conditional(type,usesThen);
            }
            else
            {
                objCond = new CompoundConditional(type);
            }
            if (!objCond.readCondition(condNode))
            {
                objCond = null;
            }
        }

        return objCond;
    }
}
