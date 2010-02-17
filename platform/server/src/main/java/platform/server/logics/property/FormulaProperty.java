package platform.server.logics.property;

import java.util.Collection;
import java.util.Set;
import java.util.List;

abstract public class FormulaProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    protected FormulaProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    @Override
    public boolean check() {
        return true;
    }
}
