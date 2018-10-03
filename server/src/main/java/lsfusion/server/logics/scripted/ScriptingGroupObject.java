package lsfusion.server.logics.scripted;

import lsfusion.interop.ClassViewType;
import lsfusion.server.form.entity.ActionPropertyObjectEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.UpdateType;
import lsfusion.server.logics.i18n.LocalizedString;

import java.util.List;

public class ScriptingGroupObject {
    public String groupName;
    public List<String> objects;
    public List<String> classes;
    public List<LocalizedString> captions;
    public List<ActionPropertyObjectEntity> events;
    public ClassViewType viewType;
    public boolean isInitType;
    public Integer pageSize;
    public UpdateType updateType;
    public String propertyGroupName;

    public String integrationSID;

    public GroupObjectEntity neighbourGroupObject;
    public Boolean isRightNeighbour;

    public ScriptingGroupObject(String name, List<String> objects, List<String> classes, List<LocalizedString> captions, List<ActionPropertyObjectEntity> events) {
        assert objects.size() == classes.size() && classes.size() == captions.size() && captions.size() == events.size();

        groupName = name;
        this.objects = objects;
        this.classes = classes;
        this.captions = captions;
        this.events = events;
    }

    public void setViewType(ClassViewType viewType) {
        this.viewType = viewType;
    }

    public void setInitType(boolean isInitType) {
        this.isInitType = isInitType;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setUpdateType(UpdateType updateType) {
        this.updateType = updateType; 
    }
    
    public void setNeighbourGroupObject(GroupObjectEntity neighbourGroupObject, boolean isRightNeighbour) {
        this.neighbourGroupObject = neighbourGroupObject;
        this.isRightNeighbour = isRightNeighbour;
    }

    public void setPropertyGroupName(String propertyGroupName) {
        this.propertyGroupName = propertyGroupName;
    }

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }
}
