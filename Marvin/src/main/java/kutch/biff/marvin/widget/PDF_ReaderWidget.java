/*
 * ##############################################################################
 * #  Copyright (c) 2017 Intel Corporation
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
package kutch.biff.marvin.widget;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.task.MarvinTask;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick
 */
public class PDF_ReaderWidget extends BaseWidget {
    private static int _AutoAdvancePageNumber = 0;
    // private final org.jpedal.PdfDecoderFX _pdf = new org.jpedal.PdfDecoderFX();
    private Node _pdf = null;
    @SuppressWarnings("unused")
    private String _SrcFile;
    private Group _objGroup;
    private int _CurrPage = 1;
    private int _AutoAdvanceInterval;
    @SuppressWarnings("unused")
    private boolean _AutoAdvance, _AutoLoopWithAdvance;

    public PDF_ReaderWidget() {
        _AutoAdvance = false;
        _AutoLoopWithAdvance = false;
    }

    @Override
    public void ConfigureAlignment() {
        super.ConfigureAlignment();

        GridPane.setValignment(_objGroup, getVerticalPosition());
        GridPane.setHalignment(_objGroup, getHorizontalPosition());
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        SetParent(pane);
        if (setupPDF()) {
            _objGroup = new Group();
            _objGroup.getChildren().add(_pdf);
            ConfigureAlignment();
            SetupPeekaboo(dataMgr);
            SetupTaskAction();
            ConfigureDimentions();

            pane.add(_objGroup, getColumn(), getRow(), getColumnSpan(), getRowSpan());

            gotoPage(_CurrPage);

            if (_AutoAdvance) {
                if (null == getMinionID() || null == getNamespace()) {
                    String ID = this.toString() + "." + Integer.toBinaryString(PDF_ReaderWidget._AutoAdvancePageNumber);
                    PDF_ReaderWidget._AutoAdvancePageNumber++;

                    if (null == getMinionID()) {
                        setMinionID(ID);
                    }
                    if (null == getNamespace()) {
                        setNamespace(ID);
                    }
                }
                MarvinTask mt = new MarvinTask();
                mt.AddDataset(getMinionID(), getNamespace(), "Next");
                TASKMAN.AddPostponedTask(mt, _AutoAdvanceInterval);
            }

            dataMgr.AddListener(getMinionID(), getNamespace(), (o, oldVal, newVal) -> {
                MyHandler(newVal);
            });
            return ApplyCSS();
        }
        return false;
    }

    @Override
    public Node getStylableObject() {
        return _pdf;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return null;
        // return _pdf.getStylesheets();
    }

    private void gotoPage(int pageNum) {
        /*
         * if (pageNum > _pdf.getPageCount()) {
         * LOGGER.warning("Tried to go to PDF page " + Integer.toString(pageNum) +
         * " but it only has " + Integer.toBinaryString(_pdf.getPageCount())+
         * " pages."); return; } try { final PdfPageData pageData =
         * _pdf.getPdfPageData(); final int rotation = pageData.getRotation(pageNum);
         * //rotation angle of current page float scaleVal = 1; if (getWidth() != 0 ||
         * getHeight() != 0) { float xScale = 0; float yScale = 0; // figure out which
         * is the smaller scale value and use it if (getWidth() > 0) { xScale = (float)
         * getWidth() / pageData.getCropBoxWidth2D(pageNum); } if (getHeight() > 0) {
         * yScale = (float) getHeight() / pageData.getCropBoxHeight2D(pageNum); }
         *
         * if (xScale > yScale) { scaleVal = yScale; } else { scaleVal = xScale; } } //
         * The sample App does this, and if I don't, it looks wrong if (rotation == 0 ||
         * rotation == 180) { _pdf.setPageParameters(scaleVal, pageNum); }
         *
         * _pdf.decodePage(pageNum); _CurrPage = pageNum;
         * _pdf.waitForDecodingToFinish(); }
         *
         * catch (final Exception e) { LOGGER.severe(e.toString()); }
         */
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        if (node.getNodeName().equalsIgnoreCase("Source")) {
            setSrcFile(node.getTextContent());
            return true;
        }
        if (node.getNodeName().equalsIgnoreCase("AutoAdvance")) {
            /*
             * <AutoAdvance Frequency='1000' Loop='False'/>
             */
            if (node.hasAttribute("Frequency")) {
                _AutoAdvanceInterval = node.getIntegerAttribute("Frequency", -1);
                if (_AutoAdvanceInterval < 100) {
                    LOGGER.severe(
                            "Frequency specified for PDF_ReaderWidget is invalid: " + node.getAttribute("Frequency"));
                    return false;
                }

                if (node.hasAttribute("Loop")) {
                    _AutoLoopWithAdvance = node.getBooleanAttribute("Loop");
                }
                _AutoAdvance = true;
                return true;
            }

            return false;
        }
        if (node.getNodeName().equalsIgnoreCase("InitialPage")) {
            try {
                _CurrPage = Integer.parseInt(node.getTextContent());
            } catch (NumberFormatException ex) {
                LOGGER.severe("Initial Page for PDF_ReaderWidget: " + node.getTextContent());
                return false;
            }
            return true;
        }

        return false;
    }

    private void MyHandler(Object newVal) {
        /*
         * if (IsPaused()) { return; }
         *
         * String strVal = newVal.toString().replaceAll("(\\r|\\n)", "");
         *
         * String key;
         *
         * if (strVal.equalsIgnoreCase("Next")) // go to next image in the list { if
         * (_CurrPage < _pdf.getPageCount()) { _CurrPage++; } else if
         * (_AutoLoopWithAdvance) { _CurrPage = 1; } else { return; }
         * gotoPage(_CurrPage); if (_AutoAdvance) { MarvinTask mt = new MarvinTask();
         * mt.AddDataset(getMinionID(), getNamespace(), "Next");
         * TASKMAN.AddPostponedTask(mt, _AutoAdvanceInterval); } }
         *
         * else if (strVal.equalsIgnoreCase("Previous")) // go to previous image in the
         * list { if (_CurrPage > 1) { _CurrPage++; gotoPage(_CurrPage); } }
         *
         * else { try { int newPage = Integer.parseInt(strVal); if (_pdf.getPageCount()
         * < newPage) { _CurrPage = newPage; gotoPage(_CurrPage); }
         *
         * } catch (NumberFormatException ex) {
         * LOGGER.warning("Received unknown item : [" + strVal +
         * "] for PDF_ReaderWidget" + getName() + ": [" + getNamespace() + ":" +
         * getMinionID() + "]"); return; } }
         */
    }

    public void setSrcFile(String strFile) {
        _SrcFile = strFile;
    }

    private boolean setupPDF() {
        /*
         * if (getWidth() != 0 && getHeight() != 0) { LOGGER.
         * warning("Both Height and Width specified for PDF Reader Widget.  Only one will be used; see docs."
         * ); } try { //Op if (_SrcFile.startsWith("http")) {
         * _pdf.openPdfFileFromURL(_SrcFile, false); } else { String fname =
         * convertToFileOSSpecific(_SrcFile); File file = new File(fname);
         *
         * _pdf.openPdfFile(file.getAbsolutePath()); } }
         *
         * catch (final PdfException ex) {
         * LOGGER.severe("PDF file does not exist, or is invalid:" + _SrcFile);
         * LOGGER.severe(ex.toString()); return false; }
         */
        return true;
    }

    @Override
    public void UpdateTitle(String newTitle) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
        // Tools | Templates.
    }

}
