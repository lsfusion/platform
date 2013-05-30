package platform.server.logics.property;

import platform.server.classes.*;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;

import java.sql.SQLException;

public class AnyValuePropertyHolder {
    private final LCP objectProperty;
    private final LCP stringProperty;
    private final LCP intProperty;
    private final LCP longProperty;
    private final LCP doubleProperty;
    private final LCP numericProperty;
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

    public AnyValuePropertyHolder(LCP objectProperty, LCP stringProperty, LCP intProperty, LCP longProperty, LCP doubleProperty, LCP numericProperty, LCP yearProperty,
                                  LCP dateTimeProperty, LCP logicalProperty, LCP dateProperty, LCP timeProperty, LCP colorProperty, LCP wordFileProperty, LCP imageFileProperty,
                                  LCP pdfFileProperty, LCP customFileProperty, LCP excelFileProperty) {
        assert objectProperty.property.getType() == ObjectType.instance
                && stringProperty.property.getType().getCompatible(StringClass.get(1))!=null
                && intProperty.property.getType() == IntegerClass.instance
                && longProperty.property.getType() == LongClass.instance
                && doubleProperty.property.getType() == DoubleClass.instance
                && numericProperty.property.getType().getCompatible(NumericClass.get(0, 0)) != null
                && yearProperty.property.getType() == YearClass.instance
                && dateTimeProperty.property.getType() == DateTimeClass.instance
                && logicalProperty.property.getType() == LogicalClass.instance
                && dateProperty.property.getType() == DateClass.instance
                && timeProperty.property.getType() == TimeClass.instance
                && colorProperty.property.getType() == ColorClass.instance
                && wordFileProperty.property.getType() == WordClass.get(false, false)
                && imageFileProperty.property.getType() == ImageClass.get(false, false)
                && pdfFileProperty.property.getType() == PDFClass.get(false, false)
                && customFileProperty.property.getType() == DynamicFormatFileClass.get(false, false)
                && excelFileProperty.property.getType() == ExcelClass.get(false, false)
                ;

        this.objectProperty = objectProperty;
        this.stringProperty = stringProperty;
        this.intProperty = intProperty;
        this.longProperty = longProperty;
        this.doubleProperty = doubleProperty;
        this.numericProperty = numericProperty;
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

    public LCP<?> getLCP(Type valueType) {
        if (valueType instanceof ObjectType) {
            return objectProperty;
        } else if (valueType instanceof StringClass) {
            return stringProperty;
        } else if (valueType instanceof IntegerClass) {
            return intProperty;
        } else if (valueType instanceof LongClass) {
            return longProperty;
        } else if (valueType instanceof DoubleClass) {
            return doubleProperty;
        } else if (valueType instanceof NumericClass) {
            return numericProperty;
        } else if (valueType instanceof YearClass) {
            return yearProperty;
        } else if (valueType instanceof DateTimeClass) {
            return dateTimeProperty;
        } else if (valueType instanceof LogicalClass) {
            return logicalProperty;
        } else if (valueType instanceof DateClass) {
            return dateProperty;
        } else if (valueType instanceof TimeClass) {
            return timeProperty;
        } else if (valueType instanceof ColorClass) {
            return colorProperty;
        } else if (valueType instanceof WordClass) {
            return wordFileProperty;
        } else if (valueType instanceof ImageClass) {
            return imageFileProperty;
        } else if (valueType instanceof PDFClass) {
            return pdfFileProperty;
        } else if (valueType instanceof DynamicFormatFileClass) {
            return customFileProperty;
        } else if (valueType instanceof ExcelClass) {
            return excelFileProperty;
        } else {
            throw new IllegalStateException(valueType + " is not supported by AnyValueProperty");
        }
    }
        
    public void write(Type valueType, ObjectValue value, ExecutionContext context, DataObject... keys) throws SQLException {
        getLCP(valueType).change(value, context, keys);
    }

    public ObjectValue read(Type valueType, ExecutionContext context, DataObject... keys) throws SQLException {
        return getLCP(valueType).readClasses(context, keys);
    }
}
