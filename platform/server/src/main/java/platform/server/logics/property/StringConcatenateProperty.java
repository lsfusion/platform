package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.formula.StringConcatenateFormulaImpl;

public class StringConcatenateProperty extends FormulaImplProperty {

    public StringConcatenateProperty(String sID, String caption, int intNum, String separator) {
        this(sID, caption, intNum, separator, true);
    }

    public StringConcatenateProperty(String sID, String caption, int intNum, String separator, boolean caseSensitive) {
        super(sID, caption, intNum, new StringConcatenateFormulaImpl(separator, caseSensitive));
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if (commonValue != null) {
            return interfaces.mapValues(new GetStaticValue<ValueClass>() {
                public ValueClass getMapValue() {
                    return StringClass.get(0); // немного бред но ладно
                }
            });
        }
        return super.getInterfaceCommonClasses(commonValue);
    }
}
