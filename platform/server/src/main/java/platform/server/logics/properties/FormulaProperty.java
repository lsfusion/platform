package platform.server.logics.properties;

import platform.server.session.DataChanges;
import platform.server.data.types.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract public class FormulaProperty<T extends FormulaPropertyInterface> extends FunctionProperty<T> {

    protected FormulaProperty(String iSID, Collection<T> iInterfaces) {
        super(iSID, iInterfaces);
    }

    protected boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        return false;
    }
}
