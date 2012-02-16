package platform.server.logics.property;

import platform.server.classes.*;
import platform.server.data.type.ObjectType;
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

    public AnyValuePropertyHolder(LP objectProperty, LP textProperty, LP stringProperty, LP intProperty, LP longProperty, LP doubleProperty, LP yearProperty, LP dateTimeProperty, LP logicalProperty, LP dateProperty) {
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
    }

    public void write(ValueClass valueClass, Object value, ExecutionContext context, DataObject... keys) throws SQLException {
        if (valueClass instanceof CustomClass) {
            objectProperty.execute(value, context, keys);

        } else if (valueClass instanceof TextClass) {
            textProperty.execute(value, context, keys);

        } else if (valueClass instanceof StringClass) {
            stringProperty.execute(value, context, keys);

        } else if (valueClass instanceof IntegerClass) {
            intProperty.execute(value, context, keys);

        } else if (valueClass instanceof LongClass) {
            longProperty.execute(value, context, keys);

        } else if (valueClass instanceof DoubleClass) {
            doubleProperty.execute(value, context, keys);

        } else if (valueClass instanceof YearClass) {
            yearProperty.execute(value, context, keys);

        } else if (valueClass instanceof DateTimeClass) {
            dateTimeProperty.execute(value, context, keys);

        } else if (valueClass instanceof LogicalClass) {
            logicalProperty.execute(value, context, keys);

        } else if (valueClass instanceof DateClass) {
            dateProperty.execute(value, context, keys);

        } else {
            assert false:valueClass + " is not supported by AnyValueProperty";
        }
    }
}
