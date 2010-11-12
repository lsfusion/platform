package platform.server.form.instance;

import platform.server.form.entity.TreeGroupEntity;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

public class TreeGroupInstance {
    public TreeGroupEntity entity;
    public final List<GroupObjectInstance> groups;

    public TreeGroupInstance(TreeGroupEntity entity, List<GroupObjectInstance> groups) {
        this.entity = entity;
        this.groups = groups;

        List<GroupObjectInstance> upGroups = new ArrayList<GroupObjectInstance>();
        for(GroupObjectInstance group : groups) {
            group.upTreeGroups.addAll(upGroups);
            upGroups.add(group);
        }
    }

    public int getID() {
        return entity.getID();
    }
}
