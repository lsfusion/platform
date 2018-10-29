package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MAddExclSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.form.entity.ObjectEntity;

import java.util.ArrayList;
import java.util.List;

public class StaticKeyData {

    public final ImOrderSet<ObjectEntity> objects;
    public final ImOrderSet<ImMap<ObjectEntity, Object>> data;
    
    public StaticKeyData(ImOrderSet<ObjectEntity> objects, ImOrderSet<ImMap<ObjectEntity, Object>> data) {
        this.objects = objects;
        this.data = data;
    }
}
