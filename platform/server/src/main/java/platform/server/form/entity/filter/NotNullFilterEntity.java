package platform.server.form.entity.filter;

import platform.server.form.instance.InstanceFactory;
import platform.server.logics.property.PropertyInterface;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.form.entity.PropertyObjectEntity;

public class NotNullFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public NotNullFilterEntity() {
        
    }
    
    public NotNullFilterEntity(PropertyObjectEntity<P> iProperty) {
        super(iProperty);
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }
}
