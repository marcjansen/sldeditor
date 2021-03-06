/*
 * SLD Editor - The Open Source Java SLD Editor
 *
 * Copyright (C) 2016, SCISYS UK Limited
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sldeditor.test;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.apache.commons.io.IOUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.LiteralExpressionImpl;
import org.junit.Assert;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;

import com.sldeditor.SLDEditor;
import com.sldeditor.TreeSelectionData;
import com.sldeditor.common.vendoroption.VendorOptionManager;
import com.sldeditor.common.vendoroption.VersionData;
import com.sldeditor.common.xml.ParseXML;
import com.sldeditor.common.xml.ui.FieldIdEnum;
import com.sldeditor.common.xml.ui.GroupIdEnum;
import com.sldeditor.common.xml.ui.SldEditorTest;
import com.sldeditor.common.xml.ui.XMLFieldAttribute;
import com.sldeditor.common.xml.ui.XMLFieldBase;
import com.sldeditor.common.xml.ui.XMLFieldDisabled;
import com.sldeditor.common.xml.ui.XMLFieldExpression;
import com.sldeditor.common.xml.ui.XMLFieldLiteralBase;
import com.sldeditor.common.xml.ui.XMLFieldLiteralBoolean;
import com.sldeditor.common.xml.ui.XMLFieldLiteralDouble;
import com.sldeditor.common.xml.ui.XMLFieldLiteralInt;
import com.sldeditor.common.xml.ui.XMLFieldLiteralString;
import com.sldeditor.common.xml.ui.XMLFieldTest;
import com.sldeditor.common.xml.ui.XMLIndex;
import com.sldeditor.common.xml.ui.XMLPanelTest;
import com.sldeditor.common.xml.ui.XMLSetFieldAttribute;
import com.sldeditor.common.xml.ui.XMLSetFieldLiteralBase;
import com.sldeditor.common.xml.ui.XMLSetFieldLiteralInterface;
import com.sldeditor.common.xml.ui.XMLSetGroup;
import com.sldeditor.common.xml.ui.XMLSetMultiOptionGroup;
import com.sldeditor.common.xml.ui.XMLSetup;
import com.sldeditor.common.xml.ui.XMLVendorOption;
import com.sldeditor.ui.detail.GraphicPanelFieldManager;
import com.sldeditor.ui.detail.config.FieldConfigBase;
import com.sldeditor.ui.detail.config.FieldConfigColour;
import com.sldeditor.ui.detail.config.FieldId;
import com.sldeditor.ui.detail.config.base.GroupConfig;
import com.sldeditor.ui.detail.config.base.MultiOptionGroup;
import com.sldeditor.ui.detail.config.base.OptionGroup;
import com.sldeditor.ui.iface.PopulateDetailsInterface;
import com.sldeditor.ui.widgets.ValueComboBoxData;

/**
 * The Class SLDTestRunner.
 * 
 * @author Robert Ward (SCISYS)
 */
public class SLDTestRunner
{

    /** The sld editor. */
    private SLDEditor sldEditor;

    /** The Constant SCHEMA_RESOURCE. */
    private static final String SCHEMA_RESOURCE = "/xsd/testvalue.xsd";

    /** The epsilon. */
    private double epsilon = 0.0001;

    /** The colour fields list. */
    private List<FieldIdEnum> colourFieldsList = new ArrayList<FieldIdEnum>();

    /** The filename list. */
    private List<FieldIdEnum> filenameList = new ArrayList<FieldIdEnum>();
    private SLDOutputTest testOutput = new SLDOutputTest();

    public static final String PREFIX = "extracted";
    public static final String SUFFIX = ".sld";

    /** The filter factory. */
    private static FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    /**
     * Run test.
     *
     * @param folder the folder
     * @param testConfig the test config
     */
    public static void runTest(String folder, String testConfig)
    {
        SLDTestRunner test = new SLDTestRunner();
        test.internal_runTest(folder, testConfig);
    }

    /**
     * Instantiates a new SLD test runner.
     */
    private SLDTestRunner()
    {
        // Populate the list of fields that are colours
        colourFieldsList.add(FieldIdEnum.FILL_COLOUR);
        colourFieldsList.add(FieldIdEnum.STROKE_FILL_COLOUR);
        colourFieldsList.add(FieldIdEnum.STROKE_STROKE_COLOUR);
        colourFieldsList.add(FieldIdEnum.HALO_COLOUR);

        filenameList.add(FieldIdEnum.EXTERNAL_GRAPHIC);
        filenameList.add(FieldIdEnum.TTF_SYMBOL);
    }

    /**
     * Writes an InputStream to a temporary file.
     *
     * @param in the in
     * @return the file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static File stream2file (InputStream in) throws IOException {
        final File tempFile = File.createTempFile(PREFIX, SUFFIX);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }

    /**
     * Internal_run test.
     *
     * @param folder the folder
     * @param testConfig the test config
     */
    private void internal_runTest(String folder, String testConfig)
    {
        // read JSON file data as String
        String fullPath = "/" + folder + "/test/" + testConfig;

        SldEditorTest testSuite = (SldEditorTest) ParseXML.parseFile("", fullPath, SCHEMA_RESOURCE, SldEditorTest.class);

        Assert.assertNotNull("Failed to read test config file : " + fullPath, testSuite);

        String testsldfile = testSuite.getTestsldfile();
        if(!testsldfile.startsWith("/"))
        {
            testsldfile = "/" + testsldfile;
        }

        System.out.println("Opening : " + testsldfile);

        sldEditor = SLDEditor.createAndShowGUI(null, null, true);

        List<XMLVendorOption> xmlVendorOptionList = testSuite.getVendorOption();
        if((xmlVendorOptionList != null) && !xmlVendorOptionList.isEmpty())
        {
            List<VersionData> versionDataList = new ArrayList<VersionData>();

            for(XMLVendorOption vo : xmlVendorOptionList)
            {
                try {
                    String vendorOptionClassName = vo.getClassType().trim();
                    Class<?> vendorOptionClass = Class.forName(vendorOptionClassName);
                    VersionData versionData = VersionData.decode(vendorOptionClass, vo.getVersion());
                    versionDataList.add(versionData);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            if(!versionDataList.contains(VendorOptionManager.getInstance().getDefaultVendorOptionVersionData()))
            {
                versionDataList.add(VendorOptionManager.getInstance().getDefaultVendorOptionVersionData());
            }
            sldEditor.setVendorOptions(versionDataList);
        }

        InputStream inputStream = SLDTestRunner.class.getResourceAsStream(testsldfile);

        if(inputStream == null)
        {
            Assert.assertNotNull("Failed to find sld test file : " + testsldfile, inputStream);
        }
        else
        {
            File f = null;
            try {
                f = stream2file(inputStream);
                sldEditor.openFile(f.toURI().toURL());
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            GraphicPanelFieldManager mgr = sldEditor.getFieldDataManager();

            for(XMLPanelTest test : testSuite.getPanelTests())
            {
                XMLSetup selectedItem = test.getSetup();

                TreeSelectionData selectionData = new TreeSelectionData();
                selectionData.setLayerIndex(getXMLValue(selectedItem.getLayer()));
                selectionData.setStyleIndex(getXMLValue(selectedItem.getStyle()));
                selectionData.setFeatureTypeStyleIndex(getXMLValue(selectedItem.getFeatureTypeStyle()));
                selectionData.setRuleIndex(getXMLValue(selectedItem.getRule()));
                selectionData.setSymbolizerIndex(getXMLValue(selectedItem.getSymbolizer()));
                selectionData.setSymbolizerDetailIndex(getXMLValue(selectedItem.getSymbolizerDetail()));
                try {
                    selectionData.setSelectedPanel(Class.forName(selectedItem.getExpectedPanel()));
                } catch (ClassNotFoundException e1) {
                    Assert.fail("Unknown class : " + selectedItem.getExpectedPanel());
                }

                boolean result = sldEditor.selectTreeItem(selectionData);

                Assert.assertTrue("Failed to select tree item", result);
                PopulateDetailsInterface panel = sldEditor.getSymbolPanel();

                String panelClassName = panel.getClass().getName();
                Assert.assertEquals(panelClassName, selectedItem.getExpectedPanel());

                Assert.assertEquals("Check panel data present", panel.isDataPresent(), selectedItem.isEnabled());

                Class<?> panelId = null;
                try {
                    panelId = Class.forName(selectedItem.getExpectedPanel());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if(test.getFieldTests() != null)
                {
                    for(XMLFieldTest testItem : test.getFieldTests())
                    {
                        if(testItem != null)
                        {
                            if(testItem.getDisabledOrLiteralStringOrLiteralInt() != null)
                            {
                                for(Object xmlTestValueObj : testItem.getDisabledOrLiteralStringOrLiteralInt())
                                {
                                    if(xmlTestValueObj instanceof XMLSetMultiOptionGroup)
                                    {
                                        XMLSetMultiOptionGroup testValue = (XMLSetMultiOptionGroup) xmlTestValueObj;
                                        GroupIdEnum groupId = testValue.getMultiOptionGroupId();
                                        String outputText = "Checking multioption group : " + groupId;

                                        System.out.println(outputText);
                                        Assert.assertNotNull(outputText, groupId);

                                        MultiOptionGroup multiOptionGroup = mgr.getMultiOptionGroup(panelId, groupId);

                                        Assert.assertNotNull(panelId.getName() + "/" + groupId + " multi option group should exist", multiOptionGroup);

                                        multiOptionGroup.setOption(testValue.getOption());

                                        OptionGroup optionGroupSelected = multiOptionGroup.getSelectedOptionGroup();

                                        Assert.assertTrue(groupId + " should be set", optionGroupSelected.getId() == testValue.getOption());
                                    }
                                    else if(xmlTestValueObj instanceof XMLSetGroup)
                                    {
                                        XMLSetGroup testValue = (XMLSetGroup) xmlTestValueObj;
                                        GroupIdEnum groupId = testValue.getGroupId();
                                        String outputText = "Checking group : " + groupId;

                                        System.out.println(outputText);
                                        Assert.assertNotNull(outputText, groupId);

                                        GroupConfig groupConfig = mgr.getGroup(panelId, groupId);

                                        Assert.assertNotNull(panelId.getName() + "/" + groupId + " group should exist", groupConfig);

                                        groupConfig.enable(testValue.isEnable());

                                        Assert.assertTrue(groupId + " should be set", groupConfig.isPanelEnabled() == testValue.isEnable());
                                    }
                                    else
                                    {
                                        XMLFieldBase testValue = (XMLFieldBase) xmlTestValueObj;
                                        FieldId fieldId = new FieldId(testValue.getField());
                                        String outputText = "Checking : " + fieldId;
                                        System.out.println(outputText);
                                        Assert.assertNotNull(outputText, fieldId);

                                        try {
                                            Thread.sleep(100);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        FieldConfigBase fieldConfig = mgr.getData(panelId, fieldId);

                                        Assert.assertNotNull(String.format("Failed to field panel %s field %s", selectedItem.getExpectedPanel(),
                                                fieldId), fieldConfig);

                                        if(testValue instanceof XMLSetFieldLiteralBase)
                                        {
                                            XMLSetFieldLiteralInterface testInterface = (XMLSetFieldLiteralInterface)testValue;
                                            testInterface.accept(fieldConfig, fieldId);

                                            if(!((XMLSetFieldLiteralBase) testValue).isIgnoreCheck())
                                            {
                                                String sldContentString = sldEditor.getSLDString();

                                                boolean actualResult = testOutput.testValue(sldContentString, selectionData, testValue.getField(), testValue);

                                                Assert.assertTrue(fieldId + " should be set", actualResult);
                                            }
                                        }
                                        else if(testValue instanceof XMLSetFieldAttribute)
                                        {
                                            XMLSetFieldLiteralInterface testInterface = (XMLSetFieldLiteralInterface)testValue;
                                            testInterface.accept(fieldConfig, fieldId);

                                            String sldContentString = sldEditor.getSLDString();

                                            boolean actualResult = testOutput.testAttribute(sldContentString, selectionData, testValue.getField(), (XMLSetFieldAttribute)testValue);

                                            Assert.assertTrue(fieldId + " should be set", actualResult);
                                        }
                                        else if(testValue instanceof XMLFieldDisabled)
                                        {
                                            Assert.assertFalse(fieldId + " should be disabled", fieldConfig.isEnabled());
                                        }
                                        else
                                        {
                                            Assert.assertTrue(fieldId + " should be enabled", fieldConfig.isEnabled());
                                            Expression expression = null;

                                            if(fieldConfig.isValueOnly())
                                            {
                                                String expectedValue = "";

                                                if(testValue instanceof XMLFieldLiteralBase)
                                                {
                                                    Object literalValue = getLiteralValue((XMLFieldLiteralBase)testValue);
                                                    expectedValue = String.valueOf(literalValue);
                                                }
                                                else if(testValue instanceof XMLFieldAttribute)
                                                {
                                                    expectedValue = ((XMLFieldAttribute)testValue).getAttribute();
                                                }
                                                else if(testValue instanceof XMLFieldExpression)
                                                {
                                                    expectedValue = ((XMLFieldExpression)testValue).getExpression();
                                                }
                                                else
                                                {
                                                    Assert.fail(fieldId + " has unsupported type " + testValue.getClass().getName());
                                                }

                                                String actualValue = fieldConfig.getStringValue();

                                                String msg = String.format("%s Expected : '%s' Actual : '%s'", outputText, expectedValue, actualValue);

                                                boolean condition;
                                                if(comparingFilename(fieldId))
                                                {
                                                    condition = actualValue.endsWith(expectedValue);
                                                }
                                                else
                                                {
                                                    condition = (expectedValue.compareTo(actualValue) == 0);
                                                }
                                                Assert.assertTrue(msg, condition);
                                            }
                                            else
                                            {
                                                if(colourFieldsList.contains(fieldId.getFieldId()))
                                                {
                                                    FieldConfigColour fieldColour = (FieldConfigColour)fieldConfig;

                                                    expression = fieldColour.getColourExpression();
                                                }
                                                else
                                                {
                                                    expression = fieldConfig.getExpression();

                                                    if(fieldId.getFieldId() == FieldIdEnum.SYMBOL_TYPE)
                                                    {
                                                        String string = expression.toString();

                                                        expression = ff.literal(string.replace(File.separatorChar, '/'));
                                                    }
                                                }

                                                if(expression != null)
                                                {
                                                    if(testValue instanceof XMLFieldLiteralBase)
                                                    {
                                                        Object literalValue = getLiteralValue((XMLFieldLiteralBase)testValue);

                                                        if(literalValue.getClass() == Double.class)
                                                        {
                                                            checkLiteralValue(outputText, expression, (Double)literalValue);
                                                        }
                                                        else if(literalValue.getClass() == Integer.class)
                                                        {
                                                            checkLiteralValue(outputText, expression, (Integer)literalValue);
                                                        }
                                                        else if(literalValue.getClass() == String.class)
                                                        {
                                                            checkLiteralValue(outputText, expression, (String)literalValue);
                                                        }
                                                    }
                                                }
                                                else
                                                {
                                                    String actualValue;
                                                    String expectedValue = fieldConfig.getStringValue();

                                                    Object literalValue = getLiteralValue((XMLFieldLiteralBase)testValue);

                                                    if(literalValue.getClass() == Double.class)
                                                    {
                                                        actualValue = String.valueOf((Double)literalValue);
                                                    }
                                                    else if(literalValue.getClass() == Integer.class)
                                                    {
                                                        actualValue = String.valueOf((Integer)literalValue);
                                                    }
                                                    else if(literalValue.getClass() == String.class)
                                                    {
                                                        actualValue = (String)literalValue;
                                                    }
                                                    else
                                                    {
                                                        actualValue = "";
                                                    }

                                                    String msg = String.format("%s Expected : '%s' Actual : '%s'", outputText, expectedValue, actualValue);
                                                    boolean condition = (expectedValue.compareTo(actualValue) == 0);
                                                    Assert.assertTrue(msg, condition);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        JFrame frame = sldEditor.getApplicationFrame();
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * Gets the literal value as an Object.
     *
     * @param testValue the test value
     * @return the literal value
     */
    private Object getLiteralValue(XMLFieldLiteralBase testValue) {
        if(testValue instanceof XMLFieldLiteralString)
        {
            return ((XMLFieldLiteralString)testValue).getValue();
        }
        else if(testValue instanceof XMLFieldLiteralInt)
        {
            return ((XMLFieldLiteralInt)testValue).getValue();
        }
        else if(testValue instanceof XMLFieldLiteralDouble)
        {
            return ((XMLFieldLiteralDouble)testValue).getValue();
        }
        else if(testValue instanceof XMLFieldLiteralBoolean)
        {
            return ((XMLFieldLiteralBoolean)testValue).isValue();
        }

        return null;
    }

    /**
     * Gets the XML value.
     *
     * @param xmlIndex the xml index
     * @return the XML value
     */
    private int getXMLValue(XMLIndex xmlIndex) {
        if(xmlIndex == null)
        {
            return -1;
        }
        return xmlIndex.getIndex();
    }

    /**
     * Comparing filename.
     *
     * @param field the field
     * @return true, if successful
     */
    private boolean comparingFilename(FieldId field)
    {
        return filenameList.contains(field.getFieldId());
    }

    /**
     * Check literal value.
     *
     * @param message the message
     * @param expression the expression
     * @param expectedValue the expected value
     */
    private void checkLiteralValue(String message, Expression expression, double expectedValue)
    {
        Assert.assertEquals(expression.getClass(), LiteralExpressionImpl.class);
        LiteralExpressionImpl literalExpression = (LiteralExpressionImpl)expression;
        Object value = literalExpression.getValue();
        Assert.assertEquals(message, value.getClass(), Double.class);
        Double actualValue = (Double)value;
        String additional = String.format(" Expected '%f' Actual '%f'", expectedValue, actualValue);
        Assert.assertTrue(message + additional, Math.abs(expectedValue - actualValue) < epsilon);
    }

    /**
     * Check literal value.
     *
     * @param message the message
     * @param expression the expression
     * @param expectedValue the expected value
     */
    private void checkLiteralValue(String message, Expression expression, int expectedValue)
    {
        Assert.assertEquals(expression.getClass(), LiteralExpressionImpl.class);
        LiteralExpressionImpl literalExpression = (LiteralExpressionImpl)expression;
        Object value = literalExpression.getValue();
        Assert.assertEquals(message, value.getClass(), Integer.class);
        Integer actualValue = (Integer)value;
        String additional = String.format(" Expected '%d' Actual '%d'", expectedValue, actualValue);
        Assert.assertTrue(message + additional, (expectedValue == actualValue));
    }

    /**
     * Check literal value.
     *
     * @param message the message
     * @param expression the expression
     * @param expectedValue the expected value
     */
    private void checkLiteralValue(String message, Expression expression, String expectedValue)
    {
        Assert.assertEquals(expression.getClass(), LiteralExpressionImpl.class);
        LiteralExpressionImpl literalExpression = (LiteralExpressionImpl)expression;
        Object value = literalExpression.getValue();
        String actualValue = null;
        if(value.getClass() == ValueComboBoxData.class)
        {
            actualValue = ((ValueComboBoxData)value).getKey();
        }
        else
        {
            Assert.assertEquals(message, value.getClass(), String.class);
            actualValue = (String)value;
        }
        String additional = String.format(" Expected '%s' Actual '%s'", expectedValue, actualValue);
        Assert.assertTrue(message + additional, (expectedValue.equals(actualValue)));
    }
}
