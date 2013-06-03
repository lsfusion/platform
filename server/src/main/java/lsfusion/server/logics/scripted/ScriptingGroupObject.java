package lsfusion.server.logics.scripted;

import lsfusion.interop.ClassViewType;
import lsfusion.server.form.entity.ActionPropertyObjectEntity;

import java.util.List;

public class ScriptingGroupObject {
    String groupName;
    List<String> objects;
    List<String> classes;
    List<String> captions;
    List<ActionPropertyObjectEntity> events;
    ClassViewType viewType;
    boolean isInitType;
    Integer pageSize;
    String reportPathPropName;
    List<String> reportPathMapping;

    public ScriptingGroupObject(String name, List<String> objects, List<String> classes, List<String> captions, List<ActionPropertyObjectEntity> events) {
        assert objects.size() == classes.size() && classes.size() == captions.size() && captions.size() == events.size();

        groupName = name;
        this.objects = objects;
        this.classes = classes;
        this.captions = captions;
        this.events = events;
    }

    public ScriptingGroupObject(String name, List<String> objects, List<String> classes, List<String> captions, List<ActionPropertyObjectEntity> events, ClassViewType viewType, boolean isInitType) {
        this(name, objects, classes, captions, events);

        this.viewType = viewType;
        this.isInitType = isInitType;
    }

    public void setViewType(ClassViewType viewType, boolean isInitType) {
        this.viewType = viewType;
        this.isInitType = isInitType;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setReportPathProp(String propName, List<String> mapping) {
        this.reportPathPropName = propName;
        this.reportPathMapping = mapping;
    }
}
