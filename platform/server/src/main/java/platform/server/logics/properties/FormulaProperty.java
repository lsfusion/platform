package platform.server.logics.properties;

import java.util.Collection;
import java.util.Set;

abstract public class FormulaProperty<T extends FormulaPropertyInterface> extends FunctionProperty<T> {

    protected FormulaProperty(String iSID, Collection<T> iInterfaces) {
        super(iSID, iInterfaces);
    }

    protected void fillDepends(Set<Property> depends) {
    }
}
