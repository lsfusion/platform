package platform.server.logics.property;

import java.util.List;

// кроме OrderGroupProperty и FormulaUnionProperty
public abstract class ComplexIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public ComplexIncrementProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return false;
    }
}
