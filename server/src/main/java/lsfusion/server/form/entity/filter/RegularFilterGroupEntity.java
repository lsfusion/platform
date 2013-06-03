package lsfusion.server.form.entity.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegularFilterGroupEntity extends IdentityObject implements ServerIdentitySerializable {

    // конструктор нельзя удалять - нужен для сериализации
    public RegularFilterGroupEntity() {
    }

    public RegularFilterGroupEntity(int iID) {
        ID = iID;
    }

    public List<RegularFilterEntity> filters = new ArrayList<RegularFilterEntity>();

    public void addFilter(RegularFilterEntity filter) {
        filters.add(filter);
    }

    public int defaultFilterIndex = -1;

    public void addFilter(RegularFilterEntity filter, boolean setDefault) {
        if (setDefault) {
            defaultFilterIndex = filters.size();
        }
        filters.add(filter);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, filters);
        outStream.writeInt(defaultFilterIndex);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        filters = pool.deserializeList(inStream);
        defaultFilterIndex = inStream.readInt();
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        Set<ObjectEntity> groupObjects = new HashSet<ObjectEntity>();

        // ищем самый нижний GroupObjectInstance, к которому применяется фильтр
        for (RegularFilterEntity regFilter : filters) {
            groupObjects.addAll(regFilter.filter.getObjects());
        }

        return form.getApplyObject(groupObjects);
    }
}
