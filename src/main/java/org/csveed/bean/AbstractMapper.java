package org.csveed.bean;

import org.csveed.api.Row;
import org.csveed.bean.conversion.BeanWrapper;
import org.csveed.bean.conversion.ConversionException;
import org.csveed.bean.conversion.DefaultConverters;
import org.csveed.common.Column;
import org.csveed.report.CsvException;
import org.csveed.report.RowError;

import java.util.Set;

public abstract class AbstractMapper<T> {

    protected BeanReaderInstructionsImpl beanReaderInstructions;

    private boolean verified = false;

    private DefaultConverters defaultConverters = new DefaultConverters();

    public abstract BeanProperty getBeanProperty(Column currentColumn);

    protected abstract Set<Column> keys();

    protected abstract void checkKey(Row row, Column key);

    public void verifyHeader(Row row) {
        if (verified) {
            return;
        }
        for (Column key : keys()) {
            if (!getBeanProperty(key).isDynamicColumnProperty()) {
                checkKey(row, key);
            }
        }
        verified = true;
    }

    protected abstract Column getColumn(Row row);

    public T convert(T bean, Row row, int lineNumber, Column currentDynamicColumn) {
        BeanWrapper beanWrapper = new BeanWrapper(defaultConverters, bean);

        Column currentColumn = null;
        for (String cell : row) {
            currentColumn = currentColumn == null ? getColumn(row) : currentColumn.nextColumn();

            if (currentDynamicColumn != null && currentColumn.getColumnIndex() == currentDynamicColumn.getColumnIndex()) {
                setDynamicColumnProperties(row, lineNumber, beanWrapper, currentColumn);
                continue;
            }

            BeanProperty beanProperty = getBeanProperty(currentColumn);
            if (beanProperty == null) {
                continue;
            }
            if (beanProperty.isRequired() && (cell == null || cell.equals(""))) {
                throw new CsvException(
                        new RowError("Bean property \""+beanProperty.getPropertyName()+
                                "\" is required and may not be empty or null",
                        row.reportOnColumn(currentColumn.getColumnIndex()), lineNumber));
            }
            setBeanProperty(row, lineNumber, beanWrapper, currentColumn, cell, beanProperty);
        }
        return bean;
    }

    private void setDynamicColumnProperties(Row row, int lineNumber, BeanWrapper beanWrapper, Column currentColumn) {
        String dynamicHeaderName = row.getHeader().getName(currentColumn.getColumnIndex());
        BeanProperty headerNameProperty = beanReaderInstructions.getProperties().getHeaderNameProperty();
        setBeanProperty(row, lineNumber, beanWrapper, currentColumn, dynamicHeaderName, headerNameProperty);

        String dynamicHeaderValue = row.get(currentColumn.getColumnIndex() - 1);
        BeanProperty headerValueProperty = beanReaderInstructions.getProperties().getHeaderValueProperty();
        setBeanProperty(row, lineNumber, beanWrapper, currentColumn, dynamicHeaderValue, headerValueProperty);
    }

    private void setBeanProperty(Row row, int lineNumber, BeanWrapper beanWrapper, Column currentColumn, String cell, BeanProperty beanProperty) {
        try {
            beanWrapper.setProperty(beanProperty, cell);
        } catch (ConversionException err) {
            String message =
                    err.getMessage()+" cell"+currentColumn.getColumnText()+" ["+cell+"] to "+
                    beanProperty.getPropertyName() + ": " + err.getTypeDescription();
            throw new CsvException(new RowError(message, row.reportOnColumn(currentColumn.getColumnIndex()), lineNumber));
        }
    }

    public void setBeanReaderInstructions(BeanReaderInstructionsImpl beanReaderInstructions) {
        this.beanReaderInstructions = beanReaderInstructions;
    }

}