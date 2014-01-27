package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.formula.StringJoinConcatenateFormulaImpl;

public class StringConcatenateProperty extends FormulaImplProperty {

    public StringConcatenateProperty(String sID, String caption, int intNum, String separator) {
        this(sID, caption, intNum, separator, false);
    }

    public StringConcatenateProperty(String sID, String caption, int intNum, String separator, boolean caseInsensitive) {
        super(sID, caption, intNum, new StringJoinConcatenateFormulaImpl(separator, caseInsensitive));
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue, PrevClasses prevSameClasses) {
        if (commonValue != null) {
            return interfaces.mapValues(new GetStaticValue<ValueClass>() {
                public ValueClass getMapValue() {
                    return StringClass.get(0); // немного бред но ладно
                }
            });
        }
        return super.getInterfaceCommonClasses(commonValue, prevSameClasses);
    }
}
