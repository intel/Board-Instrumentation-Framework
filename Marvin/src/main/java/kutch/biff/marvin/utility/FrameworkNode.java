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

import kutch.biff.marvin.configuration.Configuration;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.widget.BaseWidget;

/**
 * This is a wrapper class for the XML Node object. It allows case non-sensitve
 * attributes and allows for alias's to be in the XML files
 *
 * @author Patrick Kutch
 */
public class FrameworkNode {

    private final static Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static final AliasMgr ALIASMANAGER = AliasMgr.getAliasMgr();

    public static String DumpRawAttributes(FrameworkNode node) {
        String strRet = "";
        int numAttrs = node._attributes.getLength();

//            for (int i = 0; i < numAttrs; i++)
//            {
//                String attrName = parentNode._attributes.item(i).getNodeName();
//                System.out.println(attrName + ": " + parentNode._attributes.item(i).getTextContent());
//            }
        for (int i = 0; i < numAttrs; i++) {
            String attrName = node._attributes.item(i).getNodeName();
            String strOrig = node._attributes.item(i).getTextContent();
            strRet += attrName + "=" + strOrig + " ";
        }
        return strRet;
    }

    public static void dumpTree(FrameworkNode doc) {
        dumpTree(doc.GetNode(), "");
    }

    public static void dumpTree(Node doc) {
        dumpTree(doc, "");
    }

    public static void dumpTree(Node doc, String parent) {
        try {
            System.out.println(parent + "<" + doc.getNodeName() + ">  " + doc.getTextContent());

            NamedNodeMap cl = doc.getAttributes();
            for (int i = 0; i < cl.getLength(); i++) {
                Node node = cl.item(i);
                System.out.println("\t" + node.getNodeName() + " ->" + node.getNodeValue());
            }
            NodeList nl = doc.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node.getNodeName().equalsIgnoreCase("#Text")) {

                } else {
                    dumpTree(node, parent + "<" + doc.getNodeName() + ">");
                }
            }
        } catch (Throwable e) {
            System.out.println("Cannot print!! " + e.getMessage());
        }
    }

    /**
     * @param doc
     * @param childName
     * @param levels
     * @return
     */
    public static List<FrameworkNode> GetChildNodes(Document doc, String childName) {
        ArrayList<FrameworkNode> retList = new ArrayList<FrameworkNode>();
        NodeList docChildren = doc.getChildNodes();
        for (int iLoop = 0; iLoop < docChildren.getLength(); iLoop++) {
            Node child = docChildren.item(iLoop);
            if (child.getNodeName().equalsIgnoreCase("#Text") || child.getNodeName().equalsIgnoreCase("#Comment")) {
                // continue;
            } else {
                FrameworkNode node = new FrameworkNode(child);
                List<FrameworkNode> currList = node.getChildNodes(childName);
                retList.addAll(currList);
            }
        }

        return retList;
    }

    public static ArrayList<String> GetDirScanInfo(String strInput) {
        ArrayList<String> retList = new ArrayList<>();
        if (!strInput.contains("]")) {
            LOGGER.severe("Malformed For loop with DirScan: " + strInput);
            return null;
        }
        String userData = strInput.substring(9, strInput.indexOf(']'));
        String[] parts = userData.split(":");
        String strFolder = BaseWidget.convertToFileOSSpecific(parts[0]);
        File folder = new File(strFolder);

        if (!folder.exists()) {
            LOGGER.severe("Invalid For loop with DirScan directory: " + strInput);
            return null;
        }
        File[] listOfFiles = folder.listFiles();
        if (null == listOfFiles) {
            return null;
        }
        Arrays.sort(listOfFiles);

        ArrayList<String> extFilter = new ArrayList<>();
        if (parts.length > 1) {
            for (int index = 1; index < parts.length; index++) {
                extFilter.add(parts[index]);
            }
        }

        for (int index = 0; index < listOfFiles.length; index++) {
            File fName = listOfFiles[index];
            if (fName.isDirectory()) {
                continue;
            }
            if (extFilter.size() > 0) {
                for (String ext : extFilter) {
                    if (fName.getName().toLowerCase().endsWith(ext.toLowerCase())) {
                        retList.add(fName.getName());
                        retList.add(fName.getAbsolutePath());
                        break;
                    }
                }
            } else {
                retList.add(fName.getName());
                retList.add(fName.getAbsolutePath());
            }
        }

        return retList;
    }

    private static String ProcessLoopVars(String strInp, String AliasList[]) {
        String strRet = strInp;
        if (ALIASMANAGER.IsAliased("CurrentFileAlias")) // fugly little hack to implement iterating files in directory
        {
            strRet = replaceString(strRet, "$(" + "CurrentFileAlias" + ")", ALIASMANAGER.GetAlias("CurrentFileAlias"));
        }
        if (ALIASMANAGER.IsAliased("CurrentFileWithPathAlias")) // fugly little hack to implement iterating files in
        // directory
        {
            strRet = replaceString(strRet, "$(" + "CurrentFileWithPathAlias" + ")",
                    ALIASMANAGER.GetAlias("CurrentFileWithPathAlias"));
        }
        for (String strCheck : AliasList) {
            if (null != strCheck && strCheck.length() > 0) {
                strRet = replaceString(strRet, "$(" + strCheck + ")", ALIASMANAGER.GetAlias(strCheck));
            }
        }
        return strRet;
    }

    public static String replaceString(String source, String target, String replacement) {
        StringBuilder sbSourceStr = new StringBuilder(source);
        StringBuilder sbSourceLower = new StringBuilder(source.toLowerCase());
        String searchString = target.toLowerCase();

        int index = 0;
        while ((index = sbSourceLower.indexOf(searchString, index)) != -1) // go through the src string and find the str
        // to replace.
        {
            sbSourceStr.replace(index, index + searchString.length(), replacement);
            sbSourceLower.replace(index, index + searchString.length(), replacement);
            index += replacement.length();
        }
        sbSourceLower.setLength(0);
        sbSourceLower.trimToSize();
        sbSourceLower = null;

        return sbSourceStr.toString();
    }

    // goes and replaces contents of nodes with alias.
    public static FrameworkNode Resolve(FrameworkNode origNode, String strCountAlias, String strValueAlias) {
        FrameworkNode parentNode = new FrameworkNode(origNode.GetNode().cloneNode(true)); // dup node to manipulate

        String aliasList[] = {"CurrentValueAlias", "CurrentCountAlias", strCountAlias, strValueAlias};

        if (parentNode.hasAttributes()) {
            int numAttrs = parentNode._attributes.getLength();

            for (int i = 0; i < numAttrs; i++) {
                String strOrig = parentNode._attributes.item(i).getTextContent();
                String strResolvedVal = ProcessLoopVars(strOrig, aliasList);
                parentNode._attributes.item(i).setNodeValue(strResolvedVal);
            }
        }
        // Add current count alias as an attribute = should then be available to grids
        // and things withing a loop
        if (!parentNode.hasAttribute("CurrentValueAlias")) {
            ((Element) parentNode.GetNode()).setAttribute("CurrentValueAlias",
                    ALIASMANAGER.GetAlias("CurrentValueAlias"));
        }
        if (!parentNode.hasAttribute("CurrentCountAlias")) {
            ((Element) parentNode.GetNode()).setAttribute("CurrentCountAlias",
                    ALIASMANAGER.GetAlias("CurrentCountAlias"));
        }

        if (parentNode.hasChildren()) {
            List<Node> nodeChildrenList = new ArrayList<>();
            for (FrameworkNode childNode : parentNode.getChildNodes()) {
                FrameworkNode newChild = Resolve(childNode, strCountAlias, strValueAlias);
                nodeChildrenList.add(newChild.GetNode());
            }

            String newContent = ProcessLoopVars(parentNode.getTextContent(), aliasList);

            parentNode.removeAllChildren();
            parentNode._node.setTextContent(newContent);

            for (Node childNode : nodeChildrenList) {
                parentNode.GetNode().appendChild(childNode);
            }
        }

        return parentNode;
    }

    protected Node _node;

    private NamedNodeMap _attributes;

    public FrameworkNode(FrameworkNode otherNode) {
        Node copy = otherNode._node.cloneNode(true);
        _node = ResolveAlias(copy);
        _attributes = _node.getAttributes();
    }

    public FrameworkNode(Node baseNode) {
        _node = baseNode;
        _attributes = _node.getAttributes();
    }

    public void AddAttibute(String key, String value) {
        ((Element) _node).setAttribute(key, value);
    }

    public boolean DeleteAttribute(String AttributeName) {
        if (!hasAttributes()) {
            return false;
        }

        for (int index = 0; index < _node.getAttributes().getLength(); index++) {
            Node attribute = _node.getAttributes().item(index);

            if (attribute.getNodeName().equalsIgnoreCase(AttributeName)) {
                _node.getAttributes().removeNamedItem(attribute.getNodeName());
                return true;
            }
        }
        return false;
    }

    public void DeleteChildNodes(String childName) {
        NodeList children = _node.getChildNodes();
        ArrayList<Node> toNuke = new ArrayList<>();
        for (int iLoop = 0; iLoop < children.getLength(); iLoop++) {
            Node child = children.item(iLoop);
            if (child.getNodeName().equalsIgnoreCase(childName)) {
                toNuke.add(child);
            }
        }
        for (Node child : toNuke) {
            _node.removeChild(child);
        }
    }

    public String DumpChildren() {
        String strRet = "Children: ";
        for (FrameworkNode node : getChildNodes()) {
            strRet += node.getNodeName() + "\n";
        }
        return strRet;
    }

    public String getAttribute(String elemStr) {
        if (false == _node.hasAttributes()) {
            return null;
        }
        if (null != _attributes.getNamedItem(elemStr)) {
            return HandleAlias(_attributes.getNamedItem(elemStr).getTextContent());
        }
        // now let's do a case non-sensitive check
        for (int iLoop = 0; iLoop < _attributes.getLength(); iLoop++) {
            if (elemStr.equalsIgnoreCase(_attributes.item(iLoop).getNodeName())) {
                return HandleAlias(_attributes.item(iLoop).getTextContent());
            }
        }
        return null;
    }

    public String getAttributeList() {
        if (!hasAttributes()) {
            return "";
        }
        String strAttributes = "";

        for (int index = 0; index < _node.getAttributes().getLength(); index++) {
            Node attribute = _node.getAttributes().item(index);
            if (attribute.getNodeName().equalsIgnoreCase("File") || attribute.getNodeName().equalsIgnoreCase("Source")) {
                strAttributes = attribute.getNodeName() + "=\"" + getAttribute(attribute.getNodeName()) + "\" "
                        + strAttributes;
            } else {
                strAttributes += attribute.getNodeName() + "=\"" + getAttribute(attribute.getNodeName()) + "\" ";
            }

        }
        return strAttributes;
    }

    public boolean getBooleanAttribute(String elemStr) {
        if (hasAttribute(elemStr)) {
            String str = getAttribute(elemStr);
            if (str.equalsIgnoreCase("True")) {
                return true;
            }
            if (str.equalsIgnoreCase("False")) {
                return false;
            }
            if (!Configuration.getConfig().DoNotReportAliasErrors()) {

                LOGGER.severe("Invalid boolean attribute for [" + elemStr + "] :" + str + ". Defaulting to false");
            }
        } else {
            if (!Configuration.getConfig().DoNotReportAliasErrors()) {

                LOGGER.info("Asked to read boolean attribute [" + elemStr + "] that does not exist. Defaulting to false");
            }
        }
        return false;
    }

    public boolean getBooleanValue() {
        boolean retVal = false;
        String strBool = getTextContent();
        if ("True".equalsIgnoreCase(strBool)) {
            retVal = true;
        } else if ("False".equalsIgnoreCase(strBool)) {
        } else {
            if (!Configuration.getConfig().DoNotReportAliasErrors()) {

                LOGGER.info(
                        "Asked to read boolean value for [" + getNodeName() + "] that does not exist. Defaulting to false");
            }
        }

        return retVal;
    }

    public FrameworkNode getChild(String childName) {
        ArrayList<FrameworkNode> children = getChildNodes();

        for (FrameworkNode child : children) {
            if (childName.equalsIgnoreCase(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    // void appendChild(FrameworkNode)
    public ArrayList<FrameworkNode> getChildNodes() {
        return getChildNodes(true);
    }

    public ArrayList<FrameworkNode> getChildNodes(boolean fHandleFor) {
        NodeList children = _node.getChildNodes();

        ArrayList<FrameworkNode> list = new ArrayList<>();
        for (int iLoop = 0; iLoop < children.getLength(); iLoop++) {
            if (children.item(iLoop).getNodeName().equalsIgnoreCase("#Text")
                    || children.item(iLoop).getNodeName().equalsIgnoreCase("#comment")) {
                continue; // just skip this stuff
            }
            if (children.item(iLoop).getNodeName().equalsIgnoreCase("If")) {
                list.addAll(HandleIf(new FrameworkNode(children.item(iLoop))));
            } // this doesn't work for everything....
            else if (fHandleFor && children.item(iLoop).getNodeName().equalsIgnoreCase("For")) {
                // FrameworkNode.dumpTree(_node);
                boolean currErrorBool = Configuration.getConfig().DoNotReportAliasErrors();
                Configuration.getConfig().SetDoNotReportAliasErrors(true);

                ArrayList<FrameworkNode> tList = HandleRepeat(new FrameworkNode(children.item(iLoop)));
                if (null != tList) {
                    list.addAll(tList);
                } else {
                    // System.out.println(children.item(iLoop).getNodeName());
                }
                Configuration.getConfig().SetDoNotReportAliasErrors(currErrorBool);

            } else {
                list.add(new FrameworkNode(children.item(iLoop)));
            }
        }
        return list;
    }

    public ArrayList<FrameworkNode> getChildNodes(String childName) {
        ArrayList<FrameworkNode> children = getChildNodes(true);
        ArrayList<FrameworkNode> list = new ArrayList<>();

        for (FrameworkNode child : children) {
            if (childName.equalsIgnoreCase(child.getNodeName())) {
                list.add(child);
            }
        }
        return list;
    }

    /**
     * @param elemStr
     * @param defaultValue
     * @return
     */
    public double getDoubleAttribute(String elemStr, double defaultValue) {
        if (hasAttribute(elemStr)) {
            String str = getAttribute(elemStr);
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ex) {
                if (!Configuration.getConfig().DoNotReportAliasErrors()) {
                    LOGGER.severe("Invalid attribute rate : " + str);
                }
            }
        }
        return defaultValue;
    }

    public double getDoubleContent() {
        return Double.parseDouble(getTextContent());
    }

    public int getIntegerAttribute(String elemStr, int defaultValue) {
        if (hasAttribute(elemStr)) {
            String str = getAttribute(elemStr);
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                try {
                    return (int) Double.parseDouble(str);
                } catch (NumberFormatException ex1) {
                    if (!Configuration.getConfig().DoNotReportAliasErrors()) {
                        LOGGER.severe("Invalid attribute : " + str);
                    }
                }
            }
        }
        return defaultValue;
    }

    public int getIntegerContent() {
        try {
            return Integer.parseInt(getTextContent());
        } catch (NumberFormatException ex) {
            return (int) getDoubleContent();
        }
    }

    public Node GetNode() {
        return _node;
    }

    public String getNodeName() {
        return _node.getNodeName();
    }

    public short getNodeType() {
        return _node.getNodeType();
    }

    public String getTextContent() {
        return HandleAlias(_node.getTextContent());
    }

    /**
     * * Routine to see if there is an Alias embedded within the XML node
     * string Supports an alias within an alias. Is a reentrant routine
     *
     * @param strData the raw string
     * @return string with alias replacement
     */
    private String HandleAlias(String strData) {
        return HandleAliasWorker(strData, strData);
    }

    private String HandleAliasWorker(String strData, String origLine) {
        String retString = "";
        if (strData.contains("$(NS)")) // why is this here -8/20/19
        {
            retString = "";
        }
        if (false == strData.contains("$(")) {
            return HandleMarvinMath(strData);
        }

        int OutterIndex = strData.indexOf("$(");
        int CloseParenIndex = strData.indexOf(")", OutterIndex);
        int NextStart = strData.indexOf("$(", OutterIndex + 1);

        if (NextStart >= 0 && CloseParenIndex > 0) {
            if (strData.indexOf("$(", OutterIndex + 1) < CloseParenIndex) // have an embedded Alias
            {
                retString = strData.substring(0, OutterIndex + 2);
                String T = strData.substring(OutterIndex + 2);
                retString += HandleAliasWorker(T, origLine);
            } else {
                String Alias = strData.substring(OutterIndex + 2, CloseParenIndex);
                retString = strData.substring(0, OutterIndex);
                String strAliasedStr = ALIASMANAGER.GetAlias(Alias);
                if (strAliasedStr.startsWith("Tried to use Alias")) {
                    if (!Configuration.getConfig().DoNotReportAliasErrors()) {
                        LOGGER.severe("Alias Line with problem:" + origLine);
                    }
                }
                retString += strAliasedStr;
                retString += strData.substring(CloseParenIndex + 1);
            }
        } else if (CloseParenIndex > 0) {
            String Alias = strData.substring(OutterIndex + 2, CloseParenIndex);
            retString = strData.substring(0, OutterIndex);
            String strAliasedStr = ALIASMANAGER.GetAlias(Alias);
            if (strAliasedStr.startsWith("Tried to use Alias")) {
                LOGGER.severe("Alias Line with problem:" + origLine);
            }

            retString += strAliasedStr;
            retString += strData.substring(CloseParenIndex + 1);
        } else {
            LOGGER.warning("Something looks like an alias but is not correctly formed: " + strData);
            return strData;
        }
        return HandleAliasWorker(retString, origLine);
    }

    private ArrayList<FrameworkNode> HandleIf(FrameworkNode parentNode) {
        ArrayList<FrameworkNode> list = new ArrayList<>();
        FrameworkNode thenNode = null;
        FrameworkNode elseNode = null;
        String strVal1 = null;
        String strVal2 = null;
        String strCompare = null;
        Conditional.Type compare;
        boolean Evaluation = false;

        Utility.ValidateAttributes(new String[]{"Value1", "Value2", "Compare"}, parentNode);

        strVal1 = parentNode.getAttribute("Value1");
        strVal2 = parentNode.getAttribute("Value2");
        strCompare = parentNode.getAttribute("Compare");

        if (null == strVal1) {
            LOGGER.severe("If has no Value1 attribute. Ignoring.");
            return list;
        }
        if (null == strVal2) {
            LOGGER.severe("If has no Value2 attribute. Ignoring.");
            return list;
        }
        if (null == strCompare) {
            LOGGER.severe("If has no Compare attribute. Ignoring.");
            return list;
        }
        compare = Conditional.GetType(strCompare);
        if (Conditional.Type.Invalid == compare || Conditional.Type.CASE == compare) {
            LOGGER.severe("If has unknown Compare [" + strCompare + "]. Ignoring.");
            return list;
        }

        Evaluation = Conditional.EvaluateConditional(strVal1, strVal2, compare, false);

        for (FrameworkNode child : parentNode.getChildNodes()) {
            if (child.getNodeName().equalsIgnoreCase("#Text") || child.getNodeName().equalsIgnoreCase("#Comment")) {
                continue;
            }
            if (child.getNodeName().equalsIgnoreCase("Then")) {
                if (null == thenNode) {
                    thenNode = child;
                } else {
                    LOGGER.severe("If can have only a single <Then> tag.  Ignoring all but first.");
                }
            } else if (child.getNodeName().equalsIgnoreCase("Else")) {
                if (null == elseNode) {
                    elseNode = child;
                } else {
                    LOGGER.severe("If can have only a single <Else> tag.  Ignoring all but first.");
                }
            } else {
                LOGGER.severe(child.getNodeName() + " found within If but outside <Then> or <Else>. Ignoring.");
            }
        }

        if (null == thenNode && null == elseNode) {
            LOGGER.severe("If with nothing in Then or Else. Ignoring.");
            return list;
        }

        if (Evaluation) {
            list = thenNode.getChildNodes();
        } else if (null != elseNode) {
            list = elseNode.getChildNodes();
        }

        return list;
    }

    // Allow mathematical operations .... I know what am I thinking?
    private String HandleMarvinMath(String strData) {
        String retString = "";
        if (false == strData.toLowerCase().contains("marvinmath(")) {
            return strData;
        }

        int OutterIndex = strData.toLowerCase().indexOf("marvinmath(");
        int CloseParenIndex = strData.indexOf(")", OutterIndex);
        int NextStart = strData.toLowerCase().indexOf("marvinmath(", OutterIndex + 1);

        if (NextStart >= 0 && CloseParenIndex > 0) {
            if (strData.toLowerCase().indexOf("marvinmath(", NextStart + 1) < CloseParenIndex) // have an embedded
            // MarvinMath
            {
                retString = strData.substring(0, NextStart);
                String T = strData.substring(NextStart);

                retString += HandleMarvinMath(T);
            } else {
                String Alias = strData.substring(OutterIndex + "MarvinMath(".length(), CloseParenIndex);
                retString = strData.substring(0, OutterIndex);
                retString += ALIASMANAGER.GetAlias(Alias);
                retString += strData.substring(CloseParenIndex + 1);
            }
        } else if (CloseParenIndex > 0) {
            String OperationSet[] = strData.substring(OutterIndex + "MarvinMath(".length(), CloseParenIndex).split(",");
            if (3 != OperationSet.length && 4 != OperationSet.length) {
                LOGGER.warning("Something looks like a MarvinMath but is not correctly formed: " + strData);
                return strData;

            }
            double val1, val2;
            try {
                val1 = Double.parseDouble(OperationSet[0]);
                val2 = Double.parseDouble(OperationSet[2]);
            } catch (Exception ex) {
                LOGGER.warning("Something looks like a MarvinMath but is not correctly formed: " + strData);
                return strData;
            }

            String Operator = OperationSet[1];
            double NewVal;
            if (Operator.equalsIgnoreCase("Add") || Operator.equalsIgnoreCase("+")) {
                NewVal = val1 + val2;
            } else if (Operator.equalsIgnoreCase("Subtract") || Operator.equalsIgnoreCase("-")
                    || Operator.equalsIgnoreCase("sib")) {
                NewVal = val1 - val2;
            } else if (Operator.equalsIgnoreCase("Multiply") || Operator.equalsIgnoreCase("*")
                    || Operator.equalsIgnoreCase("mul")) {
                NewVal = val1 * val2;
            } else if (Operator.equalsIgnoreCase("Divide") || Operator.equalsIgnoreCase("div")) {
                try {
                    NewVal = val1 / val2;
                } catch (Exception Ex) {
                    LOGGER.warning("tried to divide bad MarvinMath: " + strData);
                    return "";
                }
            } else if (Operator.equalsIgnoreCase("maximum") || Operator.equalsIgnoreCase("max")) {
                try {
                    NewVal = max(val1, val2);
                } catch (Exception Ex) {
                    LOGGER.warning("tried to perform maximum MarvinMath: " + strData);
                    return "";
                }
            } else if (Operator.equalsIgnoreCase("minimum") || Operator.equalsIgnoreCase("min")) {
                try {
                    NewVal = min(val1, val2);
                } catch (Exception Ex) {
                    LOGGER.warning("tried to perform minimum MarvinMath: " + strData);
                    return "";
                }
            } else {
                LOGGER.warning("Something looks like a MarvinMath but is not correctly formed: " + strData);
                return "";
            }

            retString = strData.substring(0, OutterIndex);

            if (4 == OperationSet.length) { // they specified a precision
                try {
                    Integer.parseInt(OperationSet[3]); // test if valid
                    String fmtStr = "%." + OperationSet[3] + "f";
                    retString += String.format(fmtStr, NewVal);
                } catch (Exception ex) {
                    LOGGER.warning("Something looks like a MarvinMath but is not correctly formed: " + strData
                            + " - precision appears to be invalid.");
                    return strData;
                }
            } else if (NewVal == Math.floor(NewVal) && !Double.isInfinite(NewVal)) { // if zeor's behind decimal, turn into an int.
                retString += Integer.toString((int) NewVal);
            } else {
                retString += Double.toString(NewVal);
            }
            retString += strData.substring(CloseParenIndex + 1);
        } else {
            LOGGER.warning("Something looks like a MarvinMath but is not correctly formed: " + strData);
            return strData;
        }

        return HandleMarvinMath(retString);
    }

    private ArrayList<FrameworkNode> HandleRepeat(FrameworkNode repeatNode) {
        // dumpTree(repeatNode.GetNode());
        ArrayList<FrameworkNode> list = new ArrayList<>();
        ArrayList<String> fnames = null;
        int count, start;
        String strCountAlias = "";
        String strValueAlias = "";

        ALIASMANAGER.PushAliasList(false);
        ALIASMANAGER.AddAliasFromAttibuteList(repeatNode, new String[] // can define an alias list in <repeat>
                {"Count", "startvlaue", "currentCountAlias", "currentvalueAlias"});

        if (!repeatNode.hasAttribute("Count")) {
            LOGGER.severe("Repeat did not have Count attribute.");
            return null;
        }

        String strCount = repeatNode.getAttribute("Count");
        if (strCount.length() > 10 && strCount.substring(0, 9).equalsIgnoreCase("[dirscan:")) {
            fnames = GetDirScanInfo(strCount);
            if (null == fnames) {
                return null;
            }
            if (fnames.size() > 1) {
                count = fnames.size() / 2;
            } else {
                count = 0;
            }
        } else {
            count = repeatNode.getIntegerAttribute("Count", -1);
        }
        if (count < 1) {
            LOGGER.warning("For Count value invalid: " + repeatNode.getAttribute("Count"));
            return list;
        }
        start = repeatNode.getIntegerAttribute("StartValue", 0);

        if (start < 0) {
            LOGGER.severe("Fo> Start value invalid: " + repeatNode.getAttribute("startValue"));
            return null;
        }

        if (repeatNode.hasAttribute("CurrentCountAlias")) {
            strCountAlias = repeatNode.getAttribute("CurrentCountAlias");
        }

        if (repeatNode.hasAttribute("CurrentValueAlias")) {
            strValueAlias = repeatNode.getAttribute("CurrentValueAlias");
        }

        for (int iLoop = 0; iLoop < count; iLoop++) {
            ALIASMANAGER.PushAliasList(false);
            if (!strCountAlias.isEmpty()) {
                ALIASMANAGER.AddAlias(strCountAlias, Integer.toString(iLoop));
            }
            if (!strValueAlias.isEmpty()) {
                ALIASMANAGER.AddAlias(strValueAlias, Integer.toString(iLoop + start));
            }
            // Always have these aliases
            ALIASMANAGER.AddAlias("CurrentValueAlias", Integer.toString(iLoop + start));
            ALIASMANAGER.AddAlias("CurrentCountAlias", Integer.toString(iLoop));

            if (null != fnames) {
                ALIASMANAGER.AddAlias("CurrentFileAlias", fnames.get(2 * iLoop));
                ALIASMANAGER.AddAlias("CurrentFileWithPathAlias", fnames.get(2 * iLoop + 1));
            }

            for (FrameworkNode node : repeatNode.getChildNodes()) {
                if (node.getNodeName().equalsIgnoreCase("#Text") || node.getNodeName().equalsIgnoreCase("#comment")) {
                    continue;
                }

                // dumpTree(node.GetNode());
                node = Resolve(node, strCountAlias, strValueAlias);
                // System.out.println("^^^After of Resolve^^^^^^^");
                // dumpTree(node.GetNode());

                // System.out.println(node.getTextContent());
                // System.out.println(node._node.getTextContent());
                list.add(node);
            }
            ALIASMANAGER.PopAliasList();
        }

        ALIASMANAGER.PopAliasList();
        return list;
    }

    public boolean hasAttribute(String elemStr) {
        return null != getAttribute(elemStr);
    }

    public boolean hasAttributes() {
        return _node.hasAttributes();
    }

    public boolean hasChild(String nodeName) {
        return null != getChild(nodeName);
    }

    boolean hasChildren() {
        return _node.hasChildNodes();
    }

    private String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            LOGGER.severe(te.toString());
        }
        return sw.toString();
    }

    public void removeAllChildren() {
        while (_node.hasChildNodes()) {
            _node.removeChild(_node.getFirstChild());
        }
    }

    // handles alias on whole node level for copies.
    private Node ResolveAlias(Node inpNode) {
        String strNode = nodeToString(inpNode);
        String strAfterAlias = HandleAlias(strNode);
        if (strNode.contentEquals(strAfterAlias)) {
            return inpNode;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return inpNode;
        }

        InputStream inputStream = new ByteArrayInputStream(strAfterAlias.getBytes(Charset.forName("UTF-8")));

        try {
            return db.parse(inputStream).getDocumentElement();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return inpNode;
    }

    @Override
    public String toString() {
        String strRet;

        strRet = "Name: " + getNodeName() + "\n";
        strRet += "Attributes: ";

        if (!hasAttributes()) {
            strRet += "None\r";
        } else {
            strRet += " Raw: " + DumpRawAttributes(this);
            strRet += "\r";
        }
        strRet += DumpChildren();
        strRet += "\r";
        return strRet;
    }

}
