package platform.server.logics.scripted;

import platform.interop.ClassViewType;

import java.util.List;

public class ScriptingGroupObject {
    String groupName;
    List<String> objects;
    List<String> classes;
    List<String> captions;
    ClassViewType viewType;
    boolean isInitType;

    public ScriptingGroupObject(String name, List<String> objects, List<String> classes, List<String> captions) {
        assert objects.size() == classes.size() && classes.size() == captions.size();

        groupName = name;
        this.objects = objects;
        this.classes = classes;
        this.captions = captions;
    }

    public ScriptingGroupObject(String name, List<String> objects, List<String> classes, List<String> captions, ClassViewType viewType, boolean isInitType) {
        this(name, objects, classes, captions);

        this.viewType = viewType;
        this.isInitType = isInitType;
    }

    public void setViewType(ClassViewType viewType, boolean isInitType) {
        this.viewType = viewType;
        this.isInitType = isInitType;
    }
}
