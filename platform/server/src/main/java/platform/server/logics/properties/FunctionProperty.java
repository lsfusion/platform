package platform.server.logics.properties;

import java.util.Collection;

public abstract class FunctionProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    protected FunctionProperty(String iSID, Collection<T> iInterfaces) {
        super(iSID, iInterfaces);
    }
}
