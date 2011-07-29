package platform.server.logics;

import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
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

    public void addScriptedGroupObjects(List<List<String>> names, List<List<String>> classes) {
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
            addGroup(groupObj);
        }
    }

    public void addScriptedPropertyDraws(List<String> properties, List<List<String>> mappings) {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            LP<?> property = LM.getLPByName(properties.get(i));
            assert property != null;

            List<String> mappingObjects = mappings.get(i);
            assert property.property.interfaces.size() == mappingObjects.size();
            List<PropertyObjectInterfaceEntity> objects = new ArrayList<PropertyObjectInterfaceEntity>();
            for (String objectName : mappingObjects) {
                objects.add(objectEntities.get(objectName));
            }
            addPropertyDraw(property, objects.toArray(new PropertyObjectInterfaceEntity[mappingObjects.size()]));
        }
    }
}
