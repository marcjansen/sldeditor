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
package com.sldeditor.ui.detail.config.transform;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.geotools.process.function.ProcessFunction;
import org.opengis.filter.expression.Expression;

import com.sldeditor.common.undo.UndoActionInterface;
import com.sldeditor.common.undo.UndoEvent;
import com.sldeditor.common.undo.UndoInterface;
import com.sldeditor.common.undo.UndoManager;
import com.sldeditor.ui.detail.BasePanel;
import com.sldeditor.ui.detail.MultipleFieldInterface;
import com.sldeditor.ui.detail.config.FieldConfigBase;
import com.sldeditor.ui.detail.config.FieldId;
import com.sldeditor.ui.widgets.FieldPanel;

/**
 * The Class FieldConfigTransformation wraps a text field GUI component and an optional
 * value/attribute/expression drop down, ({@link com.sldeditor.ui.attribute.AttributeSelection})
 * <p>
 * Supports undo/redo functionality. 
 * <p>
 * Instantiated by {@link com.sldeditor.ui.detail.config.ReadPanelConfig} 
 * 
 * @author Robert Ward (SCISYS)
 */
public class FieldConfigTransformation extends FieldConfigBase implements UndoActionInterface {

    /** The text field. */
    private JTextArea textField;

    /** The default value. */
    private String defaultValue = "";

    /** The old value obj. */
    private Object oldValueObj = null;

    /** The Edit button text. */
    private String editButtonText = null;

    /** The Clear button text. */
    private String clearButtonText = null;

    /** The value. */
    private ProcessFunction processFunction = null;

    /** The number of rows the text area will have. */
    private int NO_OF_ROWS = 10;

    /**
     * Instantiates a new field config string.
     *
     * @param panelId the panel id
     * @param id the id
     * @param label the label
     * @param valueOnly the value only
     * @param editButtonText the edit button text
     * @param clearButtonText the clear button text
     * @param multipleFields the multiple fields
     */
    public FieldConfigTransformation(Class<?> panelId, FieldId id, String label, boolean valueOnly, String editButtonText, String clearButtonText, boolean multipleFields) {
        super(panelId, id, label, valueOnly, multipleFields);

        this.editButtonText = editButtonText;
        this.clearButtonText = clearButtonText;
    }

    /**
     * Creates the ui.
     *
     * @param parentPanel the parent panel
     * @param parentBox the parent box
     */
    /* (non-Javadoc)
     * @see com.sldeditor.ui.detail.config.FieldConfigBase#createUI()
     */
    @Override
    public void createUI(MultipleFieldInterface parentPanel, Box parentBox) {
        final UndoActionInterface parentObj = this;

        int xPos = getXPos();
        FieldPanel fieldPanel = createFieldPanel(xPos, BasePanel.WIDGET_HEIGHT * NO_OF_ROWS , getLabel(), parentPanel, parentBox);

        int width = BasePanel.FIELD_PANEL_WIDTH - xPos - 20;
        int height = BasePanel.WIDGET_HEIGHT * (NO_OF_ROWS - 1);
        textField = new JTextArea();
        textField.setBounds(xPos, BasePanel.WIDGET_HEIGHT, width, height);
        textField.setEditable(false);

        JScrollPane scroll = new JScrollPane(textField);
        scroll.setBounds(xPos, BasePanel.WIDGET_HEIGHT, width, height);
        scroll.setAutoscrolls(true);

        fieldPanel.add(scroll);

        //
        // Edit button
        //
        final JButton buttonEdit = new JButton(editButtonText);
        buttonEdit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                TransformationInterface impl = TransformationExchange.getInstance().getImpl();

                if(impl != null)
                {
                    ProcessFunction expression = impl.showTransformationDialog(processFunction);

                    if(expression != null)
                    {
                        ProcessFunction newValueObj = processFunction;
                        processFunction = expression;

                        textField.setText(ParameterFunctionUtils.getString(processFunction));

                        UndoManager.getInstance().addUndoEvent(new UndoEvent(parentObj, getFieldId(), oldValueObj, newValueObj));

                        valueUpdated();
                    }
                }
            }
        });

        buttonEdit.setBounds(xPos + BasePanel.WIDGET_X_START, 0, BasePanel.WIDGET_BUTTON_WIDTH, BasePanel.WIDGET_HEIGHT);
        fieldPanel.add(buttonEdit);

        //
        // Clear button
        //
        final JButton buttonClear = new JButton(clearButtonText);
        buttonClear.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processFunction = null;

                textField.setText("");

                UndoManager.getInstance().addUndoEvent(new UndoEvent(parentObj, getFieldId(), oldValueObj, null));

                valueUpdated();
            }
        });

        buttonClear.setBounds((int)buttonEdit.getBounds().getMaxX() + 5, 0, BasePanel.WIDGET_BUTTON_WIDTH, BasePanel.WIDGET_HEIGHT);
        fieldPanel.add(buttonClear);
    }

    /**
     * Attribute selection.
     *
     * @param field the field
     */
    /* (non-Javadoc)
     * @see com.sldeditor.ui.iface.AttributeButtonSelectionInterface#attributeSelection(java.lang.String)
     */
    @Override
    public void attributeSelection(String field)
    {
        // Not used
    }

    /**
     * Sets the enabled.
     *
     * @param enabled the new enabled
     */
    /* (non-Javadoc)
     * @see com.sldeditor.ui.detail.config.FieldConfigBase#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        if(textField != null)
        {
            textField.setEnabled(enabled);
        }
    }

    /**
     * Generate expression.
     *
     * @return the expression
     */
    /* (non-Javadoc)
     * @see com.sldeditor.ui.detail.config.FieldConfigBase#generateExpression()
     */
    @Override
    protected Expression generateExpression()
    {
        Expression expression = null;

        if(this.textField != null)
        {
            String text = textField.getText();
            if((text != null) && !text.isEmpty())
            {
                expression = getFilterFactory().literal(text);
            }
        }
        return expression;
    }

    /**
     * Checks if is enabled.
     *
     * @return true, if is enabled
     */
    /* (non-Javadoc)
     * @see com.sldeditor.ui.detail.config.FieldConfigBase#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        if((attributeSelectionPanel != null) && !isValueOnly())
        {
            return attributeSelectionPanel.isEnabled();
        }
        else
        {
            if(textField != null)
            {
                return textField.isEnabled();
            }
        }

        return false;
    }

    /**
     * Revert to default value.
     */
    /* (non-Javadoc)
     * @see com.sldeditor.ui.detail.config.FieldConfigBase#revertToDefaultValue()
     */
    @Override
    public void revertToDefaultValue()
    {
        populateField(defaultValue);
    }

    /**
     * Populate expression.
     *
     * @param objValue the obj value
     * @param opacity the opacity
     */
    /* (non-Javadoc)
     * @see com.sldeditor.ui.detail.config.FieldConfigBase#populateExpression(java.lang.Object, org.opengis.filter.expression.Expression)
     */
    @Override
    public void populateExpression(Object objValue, Expression opacity)
    {
        ProcessFunction processFunction = (ProcessFunction) objValue;

        populateField(processFunction);
    }

    /**
     * Sets the default value.
     *
     * @param defaultValue the new default value
     */
    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the string value.
     *
     * @return the string value
     */
    @Override
    public String getStringValue()
    {
        if(textField != null)
        {
            return textField.getText();
        }
        return null;
    }

    /**
     * Undo action.
     *
     * @param undoRedoObject the undo/redo object
     */
    @Override
    public void undoAction(UndoInterface undoRedoObject)
    {
        if(textField != null)
        {
            String oldValue = (String)undoRedoObject.getOldValue();

            textField.setText(oldValue);
        }
    }

    /**
     * Redo action.
     *
     * @param undoRedoObject the undo/redo object
     */
    @Override
    public void redoAction(UndoInterface undoRedoObject)
    {
        if(textField != null)
        {
            String newValue = (String)undoRedoObject.getNewValue();

            textField.setText(newValue);
        }
    }

    /**
     * Sets the test string value.
     *
     * @param fieldId the field id
     * @param testValue the test value
     */
    @Override
    public void setTestValue(FieldId fieldId, String testValue) {
        populateField(testValue);
    }

    /**
     * Populate field.
     *
     * @param value the value
     */
    @Override
    public void populateField(String value) {
        if(textField != null)
        {
            textField.setText(value);

            UndoManager.getInstance().addUndoEvent(new UndoEvent(this, getFieldId(), oldValueObj, value));

            oldValueObj = value;

            valueUpdated();
        }
    }

    /**
     * Creates a copy of the field.
     *
     * @param fieldConfigBase the field config base
     * @return the field config base
     */
    @Override
    protected FieldConfigBase createCopy(FieldConfigBase fieldConfigBase) {
        FieldConfigTransformation copy = new FieldConfigTransformation(fieldConfigBase.getPanelId(),
                fieldConfigBase.getFieldId(),
                fieldConfigBase.getLabel(),
                fieldConfigBase.isValueOnly(),
                this.editButtonText,
                this.clearButtonText,
                fieldConfigBase.hasMultipleValues());
        return copy;
    }


    /**
     * Gets the class type supported.
     *
     * @return the class type
     */
    @Override
    public Class<?> getClassType() {
        return String.class;
    }

    /**
     * Sets the field visible.
     *
     * @param visible the new visible state
     */
    @Override
    public void setVisible(boolean visible) {
        if(textField != null)
        {
            textField.setVisible(visible);
        }
    }

    /**
     * Populate process function field
     *
     * @param value the value
     */
    @Override
    public void populateField(ProcessFunction value) {
        processFunction = value;

        if(textField != null)
        {
            textField.setText(ParameterFunctionUtils.getString(processFunction));

            UndoManager.getInstance().addUndoEvent(new UndoEvent(this, getFieldId(), oldValueObj, value));

            oldValueObj = value;

            valueUpdated();
        }
    }

    /**
     * Gets the process function, overridden if necessary.
     *
     * @return the process function
     */
    @Override
    public ProcessFunction getProcessFunction()
    {
        return processFunction;
    }
}
