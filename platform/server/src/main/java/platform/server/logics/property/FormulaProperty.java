package platform.server.logics.property;

import java.util.Collection;

abstract public class FormulaProperty<T extends FormulaPropertyInterface> extends FunctionProperty<T> {

    protected FormulaProperty(String sID, String caption, Collection<T> interfaces) {
        super(sID, caption, interfaces);
    }

    @Override
    public boolean check() {
        return true;
    }
}
