package platform.server.form.view;

import platform.base.IdentityObject;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.TreeGroupEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupView extends ComponentView implements ServerIdentitySerializable {
    public List<GroupObjectView> groups = new ArrayList<GroupObjectView>();

    public TreeGroupEntity entity;

    public TreeGroupView() {
        
    }

    public TreeGroupView(FormView form, TreeGroupEntity entity) {
        super(entity.getID());

        this.entity = entity;

        for (GroupObjectEntity group : entity.groups) {
            groups.add(form.getGroupObject(group));
        }
    }

    public void add(GroupObjectView group) {
        groups.add(group);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, groups, serializationType);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
        
        groups = pool.deserializeList(inStream);

        entity = pool.context.entity.getTreeGroup(ID);
    }
}
