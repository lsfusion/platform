package platform.server.logics;

import platform.interop.ClassViewType;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.linear.LP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 26.07.11
 * Time: 19:27
 */

public class ScriptingFormEntity extends FormEntity {
    private ScriptingLogicsModule LM;
    private Map<String, ObjectEntity> objectEntities = new HashMap<String, ObjectEntity>();

    public ScriptingFormEntity(NavigatorElement parent, ScriptingLogicsModule LM, String sID, String caption) {
        super(parent, sID, caption);
        this.LM = LM;
    }

    public void addScriptedGroupObjects(List<List<String>> names, List<List<String>> classes, List<ClassViewType> viewTypes, List<Boolean> isInitType) {
        assert names.size() == classes.size();
        for (int i = 0; i < names.size();  i++) {
            List<String> groupObjectNames = names.get(i);
            List<String> groupClassIds = classes.get(i);
            assert groupObjectNames.size() == groupClassIds.size();
            GroupObjectEntity groupObj = new GroupObjectEntity(genID());
            for (int j = 0; j < groupObjectNames.size(); j++) {
                String objectName = groupObjectNames.get(j);
                ValueClass cls = LM.getClassByName(groupClassIds.get(j));
                if (objectName == null) {
                    objectName = cls.getSID();
                }
                ObjectEntity obj = new ObjectEntity(genID(), objectName, cls, objectName);
                groupObj.add(obj);
                objectEntities.put(objectName, obj);
            }

            ClassViewType viewType = viewTypes.get(i);
            if (viewType != null) {
                if (isInitType.get(i)) {
                    groupObj.setInitClassView(viewType);
                } else {
                    groupObj.setSingleClassView(viewType);
                }
            }
            addGroup(groupObj);
        }
    }

    private class MappedProperty {
        public LP<?> property;
        public PropertyObjectInterfaceEntity[] mapping;

        public MappedProperty(LP<?> property, List<PropertyObjectInterfaceEntity> mapping) {
            this.property = property;
            this.mapping = mapping.toArray(new PropertyObjectInterfaceEntity[mapping.size()]);
        }
    }

    private MappedProperty getPropertyWithMapping(String name, List<String> mapping) {
        LP<?> property = LM.getLPByName(name);
        assert property != null;
        assert property.property.interfaces.size() == mapping.size();

        List<PropertyObjectInterfaceEntity> objects = new ArrayList<PropertyObjectInterfaceEntity>();
        for (String objectName : mapping) {
            objects.add(objectEntities.get(objectName));
        }
        return new MappedProperty(property, objects);
    }

    public void addScriptedPropertyDraws(List<String> properties, List<List<String>> mappings) {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            MappedProperty prop = getPropertyWithMapping(properties.get(i), mappings.get(i));
            addPropertyDraw(prop.property, prop.mapping);
        }
    }

    public void addScriptedFilters(List<String> properties, List<List<String>> mappings) {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            MappedProperty prop = getPropertyWithMapping(properties.get(i), mappings.get(i));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(prop.property, prop.mapping)));
        }
    }
}
