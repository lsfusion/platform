package platform.server.logics.properties;

import java.util.Map;

public class DefaultData<P extends PropertyInterface> extends PropertyImplement<DataPropertyInterface,P> {

    public DefaultData(Property<P> iProperty) {
        super(iProperty);
    }

    public Property<P> defaultProperty;
    public Map<DataPropertyInterface,P> defaultMap;
    public boolean onDefaultChange;

}
