package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncChange;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.Serializable;

public class AsyncMapChange<X extends PropertyInterface, T extends PropertyInterface> extends AsyncMapFormExec<T> {

    public final PropertyMapImplement<X, T> property;

    public Serializable value;

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        PropertyDrawEntity changedProperty = form.findChangedProperty(property.mapEntityObjects(mapObjects), value == null);
        if(changedProperty != null)
            return new AsyncChange(changedProperty, value);
        return null;
    }
}
