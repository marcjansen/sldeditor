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
package com.sldeditor.filter.v2.function.temporal;

import java.util.Date;
import java.util.List;

import org.geotools.filter.temporal.TEqualsImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

import com.sldeditor.filter.v2.expression.ExpressionTypeEnum;
import com.sldeditor.filter.v2.function.FilterConfigInterface;
import com.sldeditor.filter.v2.function.FilterName;
import com.sldeditor.filter.v2.function.FilterNameParameter;

/**
 * The Class TEquals.
 *
 * @author Robert Ward (SCISYS)
 */
public class TEquals implements FilterConfigInterface {

    public class TEqualsExtended extends TEqualsImpl
    {
        public TEqualsExtended()
        {
            super(null, null);
        }

        public TEqualsExtended(Expression expression1, Expression expression2)
        {
            super(expression1, expression2);
        }

        public String toString() {
            return "[ " + getExpression1() + " TEquals " + getExpression2() + " ]";
        }
    }

    /**
     * Default constructor
     */
    public TEquals()
    {
    }

    /**
     * Gets the filter configuration.
     *
     * @return the filter configuration
     */
    @Override
    public FilterName getFilterConfiguration() {
        FilterName filterName = new FilterName("TEquals", Boolean.class);
        filterName.addParameter(new FilterNameParameter("expression1", ExpressionTypeEnum.PROPERTY, Date.class));
        filterName.addParameter(new FilterNameParameter("expression2", ExpressionTypeEnum.LITERAL, Date.class));

        return filterName;
    }

    /**
     * Creates the filter.
     *
     * @return the filter
     */
    @Override
    public Filter createFilter() {
        return new TEqualsExtended();
    }

    /**
     * Gets the filter class.
     *
     * @return the filter class
     */
    @Override
    public Class<?> getFilterClass() {
        return TEqualsImpl.class;
    }

    /**
     * Creates the filter.
     *
     * @param parameterList the parameter list
     * @return the filter
     */
    @Override
    public Filter createFilter(List<Expression> parameterList) {

        TEqualsImpl filter = new TEqualsExtended(parameterList.get(0), parameterList.get(1));

        return filter;
    }

    /**
     * Creates the logic filter.
     *
     * @param filterList the filter list
     * @return the filter
     */
    @Override
    public Filter createLogicFilter(List<Filter> filterList) {
        // Do nothing
        return null;
    }
}
