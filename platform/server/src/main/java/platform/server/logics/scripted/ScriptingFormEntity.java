package platform.server.logics.scripted;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.linear.LP;

import javax.swing.*;
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

    public void addScriptedGroupObjects(List<List<String>> names, List<List<String>> classes, List<ClassViewType> viewTypes, List<Boolean> isInitType) throws ScriptingErrorLog.SemanticErrorException {
        assert names.size() == classes.size();
        for (int i = 0; i < names.size();  i++) {
            List<String> groupObjectNames = names.get(i);
            List<String> groupClassIds = classes.get(i);
            assert groupObjectNames.size() == groupClassIds.size();
            GroupObjectEntity groupObj = new GroupObjectEntity(genID());
            String groupObjectSID = "";
            for (int j = 0; j < groupObjectNames.size(); j++) {
                String objectName = groupObjectNames.get(j);
                String objectSID = objectName;
                ValueClass cls = LM.findClassByCompoundName(groupClassIds.get(j));
                if (objectSID == null) {
                    objectSID = cls.getSID();
                    objectName = groupClassIds.get(j);
                }
                ObjectEntity obj = new ObjectEntity(genID(), objectSID, cls, objectName);
                groupObj.add(obj);
                assert !objectEntities.containsKey(objectName);
                objectEntities.put(objectName, obj);

                groupObjectSID = (groupObjectSID.length() == 0 ? "" : groupObjectSID + ".") + objectSID;
            }

            ClassViewType viewType = viewTypes.get(i);
            if (viewType != null) {
                if (isInitType.get(i)) {
                    groupObj.setInitClassView(viewType);
                } else {
                    groupObj.setSingleClassView(viewType);
                }
            }

            groupObj.setSID(groupObjectSID);
            addGroup(groupObj);
        }
    }

    public static final class MappedProperty {
        public LP<?> property;
        public PropertyObjectInterfaceEntity[] mapping;

        public MappedProperty(LP<?> property, PropertyObjectInterfaceEntity[] mapping) {
            this.property = property;
            this.mapping = mapping;
        }
    }

    private ObjectEntity[] getMappingObjectsArray(List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity[] objects = new ObjectEntity[mapping.size()];
        for (int i = 0; i < mapping.size(); i++) {
            objects[i] = objectEntities.get(mapping.get(i));
            if (objects[i] == null) {
                LM.getErrLog().emitParamNotFoundError(LM.getParser(), mapping.get(i));
            }
        }
        return objects;
    }

    public List<GroupObjectEntity> getGroupObjectsList(List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groupObjects = new ArrayList<GroupObjectEntity>();
        for (int i = 0; i < mapping.size(); i++) {
            GroupObjectEntity groupObject = getGroupObject(mapping.get(i));
            if (groupObject == null) {
                LM.getErrLog().emitParamNotFoundError(LM.getParser(), mapping.get(i));
            } else {
                groupObjects.add(groupObject);
            }

        }
        return groupObjects;
    }

    public MappedProperty getPropertyWithMapping(String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> property = LM.findLPByCompoundName(name);
        if (property.property.interfaces.size() != mapping.size()) {
            LM.getErrLog().emitParamCountError(LM.getParser(), property, mapping.size());
        }
        return new MappedProperty(property, getMappingObjectsArray(mapping));
    }

    public void addScriptedPropertyDraws(List<String> properties, List<List<String>> mappings, FormPropertyOptions commonOptions, List<FormPropertyOptions> options) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            List<String> mapping = mappings.get(i);
            PropertyDrawEntity property;
            if (properties.get(i).equals("OBJVALUE")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }
                property = addPropertyDraw(LM.baseLM.objectValue, false, getMappingObjectsArray(mapping));
            } else if (properties.get(i).equals("SELECTION")) {
                property = addPropertyDraw(LM.baseLM.sessionGroup, false, getMappingObjectsArray(mapping));
            } else if (properties.get(i).equals("ADDOBJ")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                LP<?> addObjAction = LM.getAddObjectAction(obj[0].baseClass);
                property = addPropertyDraw(addObjAction);
            } else if (properties.get(i).equals("ADDFORM")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                LP<?> addObjAction = LM.getAddFormAction((CustomClass)obj[0].baseClass);
                property = addPropertyDraw(addObjAction);
            } else if (properties.get(i).equals("EDITFORM")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                LP<?> editObjAction = LM.getEditFormAction((CustomClass)obj[0].baseClass);
                property = addPropertyDraw(editObjAction, obj[0]);
            } else {
                MappedProperty prop = getPropertyWithMapping(properties.get(i), mapping);
                property = addPropertyDraw(prop.property, prop.mapping);
            }
            applyPropertyOptions(property, commonOptions.overrideWith(options.get(i)));
        }
    }

    public void applyPropertyOptions(PropertyDrawEntity property, FormPropertyOptions options) {
        if (options.getReadOnly() != null) {
            property.readOnly = options.getReadOnly();
        }

        if (options.getColumns() != null) {
            property.columnGroupObjects = options.getColumns();
        }

        property.propertyCaption = options.getHeader();
        property.propertyFooter = options.getFooter();
        property.propertyHighlight = options.getHighlightIf();
        MappedProperty showIf = options.getShowIf();
        if (showIf != null) {
            LM.showIf(this, property, showIf.property, showIf.mapping);
        }
    }

    public void addScriptedFilters(List<String> properties, List<List<String>> mappings) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            MappedProperty prop = getPropertyWithMapping(properties.get(i), mappings.get(i));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(prop.property, prop.mapping)));
        }
    }

    public void addScriptedRegularFilterGroup(String sid, List<String> captions, List<String> keystrokes, List<String> properties, List<List<String>> mappings) throws ScriptingErrorLog.SemanticErrorException {
        assert captions.size() == mappings.size() && keystrokes.size() == mappings.size() && properties.size() == mappings.size();

        RegularFilterGroupEntity regularFilterGroup = new RegularFilterGroupEntity(genID());
        regularFilterGroup.setSID(sid);

        for (int i = 0; i < properties.size(); i++) {
            String caption = captions.get(i);
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keystrokes.get(i));
            MappedProperty property = getPropertyWithMapping(properties.get(i), mappings.get(i));

            if (keyStroke == null) {
                LM.getErrLog().emitWrongKeyStrokeFormat(LM.getParser(), keystrokes.get(i));
            }

            regularFilterGroup.addFilter(
                    new RegularFilterEntity(genID(), new NotNullFilterEntity(addPropertyObject(property.property, property.mapping)), caption, keyStroke)
            );
        }

        addRegularFilterGroup(regularFilterGroup);
    }

    public PropertyObjectEntity addPropertyObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty prop = getPropertyWithMapping(property, mapping);
        return addPropertyObject(prop.property, prop.mapping);
    }

    public void addScriptedDefaultOrder(List<String> properties, List<Boolean> orders) throws ScriptingErrorLog.SemanticErrorException {
        for (int i = 0; i < properties.size(); ++i) {
            String alias = properties.get(i);
            Boolean order = orders.get(i);

            addDefaultOrder(getPropertyDrawByAlias(alias), order);
        }
    }

    private PropertyDrawEntity getPropertyDrawByAlias(String alias) throws ScriptingErrorLog.SemanticErrorException {
        //todo: переделать, когда будут реализованы алиасы для свойств в форме

        PropertyDrawEntity property = getPropertyDraw(LM.findLPByCompoundName(alias));
        if (property == null) {
            LM.getErrLog().emitPropertyNotFoundError(LM.getParser(), alias);
        }

        return property;
    }
}
