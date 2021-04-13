package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class ContextFilterSelector<P extends PropertyInterface, V extends PropertyInterface, O extends ObjectSelector> {

    public abstract ImSet<? extends ContextFilterEntity<?, V, O>> getEntities();

    public abstract <C extends PropertyInterface> ContextFilterSelector<P, C, O> map(ImRevMap<V, C> map);
    
    public abstract PropertyMapImplement<P, V> getWhereProperty(ImRevMap<O, V> objects);
}
