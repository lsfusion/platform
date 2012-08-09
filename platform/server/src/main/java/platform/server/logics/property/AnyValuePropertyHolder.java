package platform.server.logics.property;

import platform.server.classes.*;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;

import java.sql.SQLException;

public class AnyValuePropertyHolder {
    private final LCP objectProperty;
    private final LCP textProperty;
    private final LCP stringProperty;
    private final LCP intProperty;
    private final LCP longProperty;
    private final LCP doubleProperty;
    private final LCP yearProperty;
    private final LCP dateTimeProperty;
    private final LCP logicalProperty;
    private final LCP dateProperty;
    private final LCP timeProperty;
    private final LCP colorProperty;
    private final LCP wordFileProperty;
    private final LCP imageFileProperty;
    private final LCP pdfFileProperty;
    private final LCP customFileProperty;
    private final LCP excelFileProperty;

    public AnyValuePropertyHolder(LCP objectProperty, LCP textProperty, LCP stringProperty, LCP intProperty, LCP longProperty, LCP doubleProperty, LCP yearProperty,
                                  LCP dateTimeProperty, LCP logicalProperty, LCP dateProperty, LCP timeProperty, LCP colorProperty, LCP wordFileProperty, LCP imageFileProperty,
                                  LCP pdfFileProperty, LCP customFileProperty, LCP excelFileProperty) {
        assert objectProperty.property.getType() == ObjectType.instance
                && textProperty.property.getType() == TextClass.instance
                && stringProperty.property.getType().getCompatible(StringClass.get(0))!=null
                && intProperty.property.getType() == IntegerClass.instance
                && longProperty.property.getType() == LongClass.instance
                && doubleProperty.property.getType() == DoubleClass.instance
                && yearProperty.property.getType() == YearClass.instance
                && dateTimeProperty.property.getType() == DateTimeClass.instance
                && logicalProperty.property.getType() == LogicalClass.instance
                && dateProperty.property.getType() == DateClass.instance
                && timeProperty.property.getType() == TimeClass.instance
                && colorProperty.property.getType() == ColorClass.instance
                && wordFileProperty.property.getType() == WordClass.instance
                && imageFileProperty.property.getType() == ImageClass.instance
                && pdfFileProperty.property.getType() == PDFClass.instance
                && customFileProperty.property.getType() == DynamicFormatFileClass.instance
                && excelFileProperty.property.getType() == ExcelClass.instance
                ;

        this.objectProperty = objectProperty;
        this.textProperty = textProperty;
        this.stringProperty = stringProperty;
        this.intProperty = intProperty;
        this.longProperty = longProperty;
        this.doubleProperty = doubleProperty;
        this.yearProperty = yearProperty;
        this.dateTimeProperty = dateTimeProperty;
        this.logicalProperty = logicalProperty;
        this.dateProperty = dateProperty;
        this.timeProperty = timeProperty;
        this.colorProperty = colorProperty;
        this.wordFileProperty = wordFileProperty;
        this.imageFileProperty = imageFileProperty;
        this.pdfFileProperty = pdfFileProperty;
        this.customFileProperty = customFileProperty;
        this.excelFileProperty = excelFileProperty;
    }

    public void write(Type valueType, Object value, ExecutionContext context, DataObject... keys) throws SQLException {
        if (valueType instanceof ObjectType) {
            objectProperty.change(value, context, keys);

        } else if (valueType instanceof TextClass) {
            textProperty.change(value, context, keys);

        } else if (valueType instanceof StringClass) {
            stringProperty.change(value, context, keys);

        } else if (valueType instanceof IntegerClass) {
            intProperty.change(value, context, keys);

        } else if (valueType instanceof LongClass) {
            longProperty.change(value, context, keys);

        } else if (valueType instanceof DoubleClass) {
            doubleProperty.change(value, context, keys);

        } else if (valueType instanceof YearClass) {
            yearProperty.change(value, context, keys);

        } else if (valueType instanceof DateTimeClass) {
            dateTimeProperty.change(value, context, keys);

        } else if (valueType instanceof LogicalClass) {
            logicalProperty.change(value, context, keys);

        } else if (valueType instanceof DateClass) {
            dateProperty.change(value, context, keys);

        } else if (valueType instanceof TimeClass) {
            timeProperty.change(value, context, keys);

        } else if (valueType instanceof ColorClass) {
            colorProperty.change(value, context, keys);

        } else if (valueType instanceof WordClass) {
            wordFileProperty.change(value, context, keys);

        } else if (valueType instanceof ImageClass) {
            imageFileProperty.change(value, context, keys);

        } else if (valueType instanceof PDFClass) {
            pdfFileProperty.change(value, context, keys);

        } else if (valueType instanceof DynamicFormatFileClass) {
            customFileProperty.change(value, context, keys);

        } else if (valueType instanceof ExcelClass) {
            excelFileProperty.change(value, context, keys);

        } else {
            throw new IllegalStateException(valueType + " is not supported by AnyValueProperty");
        }
    }
    public Object read(Type valueType, ExecutionContext context, DataObject... keys) throws SQLException {
        if (valueType instanceof ObjectType) {
            return objectProperty.read(context, keys);

        } else if (valueType instanceof TextClass) {
            return textProperty.read(context, keys);

        } else if (valueType instanceof StringClass) {
            return stringProperty.read(context, keys);

        } else if (valueType instanceof IntegerClass) {
            return intProperty.read(context, keys);

        } else if (valueType instanceof LongClass) {
            return longProperty.read(context, keys);

        } else if (valueType instanceof DoubleClass) {
            return doubleProperty.read(context, keys);

        } else if (valueType instanceof YearClass) {
            return yearProperty.read(context, keys);

        } else if (valueType instanceof DateTimeClass) {
            return dateTimeProperty.read(context, keys);

        } else if (valueType instanceof LogicalClass) {
            return logicalProperty.read(context, keys);

        } else if (valueType instanceof DateClass) {
            return dateProperty.read(context, keys);

        } else if (valueType instanceof TimeClass) {
            return timeProperty.read(context, keys);

        } else if (valueType instanceof ColorClass) {
            return colorProperty.read(context, keys);

        } else if (valueType instanceof WordClass) {
            return wordFileProperty.read(context, keys);

        } else if (valueType instanceof ImageClass) {
            return imageFileProperty.read(context, keys);

        } else if (valueType instanceof PDFClass) {
            return pdfFileProperty.read(context, keys);

        } else if (valueType instanceof DynamicFormatFileClass) {
            return customFileProperty.read(context, keys);

        } else if (valueType instanceof ExcelClass) {
            return excelFileProperty.read(context, keys);

        } else {
            throw new IllegalStateException(valueType + " is not supported by AnyValueProperty");
        }
    }
}
