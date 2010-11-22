package platform.server.form.instance;

import platform.server.form.entity.TreeGroupEntity;
import platform.server.caches.IdentityLazy;
import platform.base.BaseUtils;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

public class TreeGroupInstance {
    public TreeGroupEntity entity;
    public final List<GroupObjectInstance> groups;

    @IdentityLazy
    public List<GroupObjectInstance> getDownTreeGroups(GroupObjectInstance group) {
        List<GroupObjectInstance> downGroups = new ArrayList<GroupObjectInstance>();
        for(GroupObjectInstance downGroup : groups) {
            if(downGroup.equals(group))
                return downGroups;
            downGroups.add(downGroup);
        }
        throw new RuntimeException("should not be");
    }

    @IdentityLazy
    public GroupObjectInstance getUpTreeGroup(GroupObjectInstance group) {
        GroupObjectInstance result = null;
        for(GroupObjectInstance upGroup : groups) {
            if(upGroup.equals(group))
                return result;
            result = upGroup;
        }
        throw new RuntimeException("should not be");
    }

    @IdentityLazy
    public List<GroupObjectInstance> getUpTreeGroups(GroupObjectInstance group) {
        List<GroupObjectInstance> upGroups = new ArrayList<GroupObjectInstance>();
        for(GroupObjectInstance upGroup : groups) {
            upGroups.add(upGroup);
            if(upGroup.equals(group))
                return upGroups;
        }
        throw new RuntimeException("should not be");
    }

    public TreeGroupInstance(TreeGroupEntity entity, List<GroupObjectInstance> groups) {
        this.entity = entity;
        this.groups = groups;

        for(GroupObjectInstance group : groups)
            group.treeGroup = this;
    }

    public int getID() {
        return entity.getID();
    }
}
