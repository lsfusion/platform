package platform.server.form.entity.filter;

import platform.interop.Compare;
import platform.server.classes.DataClass;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.OrderEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.PropertyInterface;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.filter.CompareFilterInstance;

import java.sql.SQLException;
import java.util.Set;

public class CompareFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public Compare compare;

    public OrderEntity<?> value;

    public CompareFilterEntity(PropertyObjectEntity<P> iProperty, Compare iCompare, OrderEntity<?> iValue) {
        super(iProperty);
        value = iValue;
        compare = iCompare;
    }

    public CompareFilterEntity(PropertyObjectEntity<P> iProperty, Compare iCompare, Object iValue) {
        this(iProperty,iCompare,new DataObject(iValue,(DataClass)iProperty.property.getType()));
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    protected void fillObjects(Set<ObjectEntity> objects) {
        super.fillObjects(objects);
        value.fillObjects(objects);
    }
}
