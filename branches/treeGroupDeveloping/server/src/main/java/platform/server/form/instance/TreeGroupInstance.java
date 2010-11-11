package platform.server.form.instance;

import platform.server.form.entity.TreeGroupEntity;

import java.util.List;

public class TreeGroupInstance {
    public TreeGroupEntity entity;
    public final List<GroupObjectInstance> groups;

    public TreeGroupInstance(TreeGroupEntity entity, List<GroupObjectInstance> groups) {
        this.entity = entity;
        this.groups = groups;
    }

    public int getID() {
        return entity.getID();
    }
}
