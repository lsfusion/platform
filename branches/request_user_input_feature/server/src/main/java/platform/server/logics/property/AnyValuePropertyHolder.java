package platform.server.logics.property;

import platform.server.classes.*;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;

import java.sql.SQLException;

public class AnyValuePropertyHolder {
    private final LP objectProperty;
    private final LP textProperty;
    private final LP stringProperty;
    private final LP intProperty;
    private final LP longProperty;
    private final LP doubleProperty;
    private final LP yearProperty;
    private final LP dateTimeProperty;
    private final LP logicalProperty;
    private final LP dateProperty;
    private final LP timeProperty;
    private final LP colorProperty;
    private final LP wordFileProperty;
    private final LP imageFileProperty;
    private final LP pdfFileProperty;
    private final LP customFileProperty;
    private final LP excelFileProperty;

    public AnyValuePropertyHolder(LP objectProperty, LP textProperty, LP stringProperty, LP intProperty, LP longProperty, LP doubleProperty, LP yearProperty,
                                  LP dateTimeProperty, LP logicalProperty, LP dateProperty, LP timeProperty, LP colorProperty, LP wordFileProperty, LP imageFileProperty,
                                  LP pdfFileProperty, LP customFileProperty, LP excelFileProperty) {
        assert objectProperty.property.getType() == ObjectType.instance
                && textProperty.property.getType() == TextClass.instance
                && stringProperty.property.getType().isCompatible(StringClass.get(0))
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
                && customFileProperty.property.getType() == CustomFileClass.instance
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
            colorProperty.execute(value, context, keys);

        } else if (valueType instanceof WordClass) {
            wordFileProperty.execute(value, context, keys);

        } else if (valueType instanceof ImageClass) {
            imageFileProperty.execute(value, context, keys);

        } else if (valueType instanceof PDFClass) {
            pdfFileProperty.execute(value, context, keys);

        } else if (valueType instanceof CustomFileClass) {
            customFileProperty.execute(value, context, keys);

        } else if (valueType instanceof ExcelClass) {
            excelFileProperty.execute(value, context, keys);

        } else {
            throw new IllegalStateException(valueType + " is not supported by AnyValueProperty");
        }
    }
    public Object read(Type valueType, ExecutionContext context, DataObject... keys) throws SQLException {
        if (valueType instanceof CustomClass) {
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

        } else if (valueType instanceof CustomFileClass) {
            return customFileProperty.read(context, keys);

        } else if (valueType instanceof ExcelClass) {
            return excelFileProperty.read(context, keys);

        } else {
            throw new IllegalStateException(valueType + " is not supported by AnyValueProperty");
        }
    }
}
