package lsfusion.server.logics.form.stat;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class StaticKeyData {

    public final ImOrderSet<ObjectEntity> objects;
    public final ImOrderSet<ImMap<ObjectEntity, Object>> data;
    
    public StaticKeyData(ImOrderSet<ObjectEntity> objects, ImOrderSet<ImMap<ObjectEntity, Object>> data) {
        this.objects = objects;
        this.data = data;
    }
}
