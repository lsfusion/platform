package platform.server.logics.scripted;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.navigator.FormShowType;
import platform.server.classes.ColorClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.linear.LP;

import javax.swing.*;
import java.util.*;

import static platform.base.BaseUtils.nvl;

/**
 * User: DAle
 * Date: 26.07.11
 * Time: 19:27
 */

public class ScriptingFormEntity {
    private ScriptingLogicsModule LM;
    private FormEntity form;

    private Map<String, PropertyDrawEntity> aliasToPropertyMap = new HashMap<String, PropertyDrawEntity>();

    public ScriptingFormEntity(ScriptingLogicsModule LM, FormEntity form) {
        assert form != null && LM != null;
        this.LM = LM;
        this.form = form;
    }

    public FormEntity getForm() {
        return form;
    }

    public List<GroupObjectEntity> addScriptingGroupObjects(List<ScriptingGroupObject> groupObjects) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groups = new ArrayList<GroupObjectEntity>();
        for (ScriptingGroupObject groupObject : groupObjects) {
            GroupObjectEntity groupObj = new GroupObjectEntity(form.genID());

            for (int j = 0; j < groupObject.objects.size(); j++) {
                String className = groupObject.classes.get(j);
                ValueClass cls = LM.findClassByCompoundName(groupObject.classes.get(j));
                String objectName = nvl(groupObject.objects.get(j), className);
                String objectCaption = nvl(groupObject.captions.get(j), cls.getCaption());

                addObjectEntity(objectName, new ObjectEntity(form.genID(), cls, objectCaption), groupObj);
            }

            String groupName = groupObject.groupName;
            if (groupName == null) {
                groupName = "";
                for (ObjectEntity obj : groupObj.objects) {
                    groupName = (groupName.length() == 0 ? "" : groupName + ".") + obj.getSID();
                }
            }

            ClassViewType viewType = groupObject.viewType;
            if (viewType != null) {
                if (groupObject.isInitType) {
                    groupObj.setInitClassView(viewType);
                } else {
                    groupObj.setSingleClassView(viewType);
                }
            }

            if (groupObject.pageSize != null) {
                groupObj.pageSize = groupObject.pageSize;
            }

            addGroupObjectEntity(groupName, groupObj);
            groups.add(groupObj);
        }

        for (int i = 0; i < groupObjects.size(); i++) {
            if (groupObjects.get(i).reportPathPropName != null) {
                groups.get(i).reportPathProp = addPropertyObject(groupObjects.get(i).reportPathPropName, groupObjects.get(i).reportPathMapping);
            }
        }
        return groups;
    }

    public void addScriptingTreeGroupObject(String treeSID, List<ScriptingGroupObject> groupObjects,
                                            List<List<String>> parentProperties) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groups = addScriptingGroupObjects(groupObjects);
        for (ScriptingGroupObject groupObject : groupObjects) {
            List<String> properties = parentProperties.get(groupObjects.indexOf(groupObject));

            if (properties != null && groupObject.objects.size() != properties.size()) {
                LM.getErrLog().emitDifferentObjsNPropsQuantity(LM.getParser());
            }
            if (properties != null) {
                assert groupObject.objects.size() == properties.size();
                GroupObjectEntity groupObj = groups.get(groupObjects.indexOf(groupObject));

                List<PropertyObjectEntity> propertyObjects = new ArrayList<PropertyObjectEntity>();
                for (String sid : properties) {
                    if (sid != null)
                        propertyObjects.add(form.addPropertyObject(LM.findLPByCompoundName(sid), groupObj.objects.toArray(new ObjectEntity[groupObj.objects.size()])));
                }

                if (!propertyObjects.isEmpty())
                    groupObj.setIsParents(propertyObjects.toArray(new PropertyObjectEntity[propertyObjects.size()]));
            }
        }
        TreeGroupEntity tree = form.addTreeGroupObject(groups.toArray(new GroupObjectEntity[groups.size()]));
        if (treeSID != null)
            tree.setSID(treeSID);
    }

    private void addGroupObjectEntity(String groupName, GroupObjectEntity group) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getGroupObject(groupName) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "group object", groupName);
        }
        group.setSID(groupName);
        form.addGroup(group);
    }

    private void addObjectEntity(String name, ObjectEntity object, GroupObjectEntity group) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getObject(name) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "object", name);
        }
        object.setSID(name);
        group.add(object);
    }

    private ObjectEntity[] getMappingObjectsArray(List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getMappingObjectsArray(form, mapping);
    }

    private ObjectEntity getObjectEntity(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getObjectEntityByName(form, name);
    }

    public MappedProperty getPropertyWithMapping(String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getPropertyWithMapping(form, name, mapping);
    }

    public List<GroupObjectEntity> getGroupObjectsList(List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groupObjects = new ArrayList<GroupObjectEntity>();
        for (String groupName : mapping) {
            GroupObjectEntity groupObject = form.getGroupObject(groupName);
            if (groupObject == null) {
                LM.getErrLog().emitParamNotFoundError(LM.getParser(), groupName);
            } else {
                groupObjects.add(groupObject);
            }

        }
        return groupObjects;
    }

    public GroupObjectEntity getGroupObjectEntity(String objectSID) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity groupObject = form.getGroupObject(objectSID);
        if (groupObject != null)
            return groupObject;
        ObjectEntity objectEntity = form.getObject(objectSID);
        GroupObjectEntity groupObjectEntity = null;
        if (objectEntity == null) {
            LM.getErrLog().emitComponentNotFoundError(LM.getParser(), objectSID);
        } else {
            groupObjectEntity = objectEntity.groupTo;
        }
        return groupObjectEntity;
    }

    public void addScriptedPropertyDraws(List<String> properties, List<String> aliases, List<List<String>> mappings, FormPropertyOptions commonOptions, List<FormPropertyOptions> options) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();

        for (int i = 0; i < properties.size(); i++) {
            List<String> mapping = mappings.get(i);
            String propertyName = properties.get(i);
            String alias = aliases.get(i);

            PropertyDrawEntity property;
            if (propertyName.equals("OBJVALUE")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }

                //assertion, что создастся только один PropertyDrawEntity
                property = BaseUtils.<PropertyDrawEntity>single(
                        form.addPropertyDraw(LM.baseLM.objectValue, false, getMappingObjectsArray(mapping))
                );
            } else if (propertyName.equals("SELECTION")) {
                //assertion, что создастся только один PropertyDrawEntity
                property = BaseUtils.<PropertyDrawEntity>single(
                        form.addPropertyDraw(LM.baseLM.sessionGroup, false, getMappingObjectsArray(mapping))
                );
            } else if (propertyName.equals("ADDOBJ")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                LP<?> addObjAction = LM.getSimpleAddObjectAction((CustomClass)obj[0].baseClass);
                property = form.addPropertyDraw(addObjAction);
            } else if (propertyName.equals("ADDFORM") || propertyName.equals("ADDSESSIONFORM")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                property = LM.addAddFormAction(form, obj[0], propertyName.equals("ADDSESSIONFORM"));
            } else if (propertyName.equals("EDITFORM") || propertyName.equals("EDITSESSIONFORM")) {
                if (mapping.size() != 1) {
                    LM.getErrLog().emitParamCountError(LM.getParser(), 1, mapping.size());
                }

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                property = LM.addEditFormAction(form, obj[0], propertyName.equals("EDITSESSIONFORM"));
            } else {
                MappedProperty prop = getPropertyWithMapping(propertyName, mapping);
                property = form.addPropertyDraw(prop.property, prop.mapping);
            }
            applyPropertyOptions(property, commonOptions.overrideWith(options.get(i)));

            setPropertDrawAlias(alias, property);
        }
    }

    public void applyPropertyOptions(PropertyDrawEntity property, FormPropertyOptions options) {
        if (options.getEditType() != null) {
            property.setEditType(options.getEditType());
        }

        if (options.getColumns() != null) {
            property.columnGroupObjects = options.getColumns();
        }

        property.propertyCaption = options.getHeader();
        property.propertyFooter = options.getFooter();

        PropertyObjectEntity backgroundProperty = options.getBackground();
        if (backgroundProperty != null && !backgroundProperty.property.getType().equals(ColorClass.instance)) {
            property.propertyBackground = addGroundPropertyObject(backgroundProperty, true);
        } else {
            property.propertyBackground = backgroundProperty;
        }

        PropertyObjectEntity foregroundProperty = options.getForeground();
        if (foregroundProperty != null && !foregroundProperty.property.getType().equals(ColorClass.instance)) {
            property.propertyForeground = addGroundPropertyObject(foregroundProperty, false);
        } else {
            property.propertyForeground = foregroundProperty;
        }

        property.propertyReadOnly = options.getReadOnlyIf();
        if (options.getForceViewType() != null) {
            property.forceViewType = options.getForceViewType();
        }
        if (options.getToDraw() != null) {
            property.toDraw = options.getToDraw();
        }
        MappedProperty showIf = options.getShowIf();
        if (showIf != null) {
            LM.showIf(form, property, showIf.property, showIf.mapping);
        }

        Boolean hintNoUpdate = options.getHintNoUpdate();
        if (hintNoUpdate != null && hintNoUpdate) {
            form.addHintsNoUpdate(property.propertyObject.property);
        }
        
        Boolean hintTable = options.getHintTable();
        if (hintTable != null && hintTable) {
            form.addHintsIncrementTable(property.propertyObject.property);
        }
    }

    private PropertyObjectEntity addGroundPropertyObject(PropertyObjectEntity groundProperty, boolean back) {
        LP backgroundLP = LM.getLPBySID(groundProperty.property.getSID());
        Object[] params = new Object[backgroundLP.listInterfaces.size() + 2];
        params[0] = back ? LM.baseLM.defaultOverrideBackgroundColor : LM.baseLM.defaultOverrideForegroundColor;
        params[1] = backgroundLP;
        for (int i = 0; i < backgroundLP.listInterfaces.size(); i++) {
            params[i + 2] = i + 1;
        }
        Collection<ObjectEntity> objects = groundProperty.getObjectInstances();
        return form.addPropertyObject(LM.addJProp(LM.baseLM.and1, params), objects.toArray(new ObjectEntity[objects.size()]));
    }

    private void setPropertDrawAlias(String alias, PropertyDrawEntity property) throws ScriptingErrorLog.SemanticErrorException {
        assert property != null;

        if (alias == null) {
            return;
        }

        if (aliasToPropertyMap.containsKey(alias)) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "alias", alias);
        }

        aliasToPropertyMap.put(alias, property);

        PropertyDrawEntity oldSIDOwner = form.getPropertyDraw(alias);

        property.setSID(alias);

        if (oldSIDOwner != null && oldSIDOwner != property) {
            form.setPropertyDrawGeneratedSID(oldSIDOwner, alias);
        }
    }

    public PropertyDrawEntity getPropertyDrawByName(String alias) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity property = aliasToPropertyMap.get(alias);
        if (property != null) {
            return property;
        }

        property = form.getPropertyDraw(LM.findLPByCompoundName(alias));
        if (property == null) {
            LM.getErrLog().emitPropertyNotFoundError(LM.getParser(), alias);
        }

        return property;
    }

    public void addScriptedFilters(List<LP<?>> properties, List<List<String>> mappings) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            form.addFixedFilter(new NotNullFilterEntity(form.addPropertyObject(properties.get(i), getMappingObjectsArray(mappings.get(i))), true));
        }
    }

    public void addScriptedHints(boolean isHintNoUpdate, List<String> propNames) throws ScriptingErrorLog.SemanticErrorException {
        LP<?>[] properties = new LP<?>[propNames.size()];
        for (int i = 0; i < propNames.size(); i++) {
            properties[i] = LM.findLPByCompoundName(propNames.get(i));
        }

        if (isHintNoUpdate) {
            form.addHintsNoUpdate(properties);
        } else {
            form.addHintsIncrementTable(properties);
        }
    }
    
    public void addScriptedRegularFilterGroup(String sid, List<String> captions, List<String> keystrokes, List<LP<?>> properties, List<List<String>> mappings, List<Boolean> defaults) throws ScriptingErrorLog.SemanticErrorException {
        assert captions.size() == mappings.size() && keystrokes.size() == mappings.size() && properties.size() == mappings.size();

        RegularFilterGroupEntity regularFilterGroup = new RegularFilterGroupEntity(form.genID());
        regularFilterGroup.setSID(sid);

        for (int i = 0; i < properties.size(); i++) {
            String caption = captions.get(i);
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keystrokes.get(i));
            Boolean setDefault = defaults.get(i);

            if (keyStroke == null) {
                LM.getErrLog().emitWrongKeyStrokeFormat(LM.getParser(), keystrokes.get(i));
            }

            regularFilterGroup.addFilter(
                    new RegularFilterEntity(form.genID(), new NotNullFilterEntity(form.addPropertyObject(properties.get(i), getMappingObjectsArray(mappings.get(i))), true), caption, keyStroke),
                    setDefault
            );
        }

        form.addRegularFilterGroup(regularFilterGroup);
    }

    public PropertyObjectEntity addPropertyObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty prop = getPropertyWithMapping(property, mapping);
        return form.addPropertyObject(prop.property, prop.mapping);
    }

    public void addScriptedDefaultOrder(List<String> properties, List<Boolean> orders) throws ScriptingErrorLog.SemanticErrorException {
        for (int i = 0; i < properties.size(); ++i) {
            String alias = properties.get(i);
            Boolean order = orders.get(i);

            form.addDefaultOrder(getPropertyDrawByName(alias), order);
        }
    }

    public void setAsDialogForm(String className, String objectID) throws ScriptingErrorLog.SemanticErrorException {
        findCustomClassForFormSetup(className).setDialogForm(form, getObjectEntity(objectID));
    }

    public void setAsEditForm(String className, String objectID) throws ScriptingErrorLog.SemanticErrorException {
        findCustomClassForFormSetup(className).setEditForm(form, getObjectEntity(objectID));
    }

    public void setAsListForm(String className, String objectID) throws ScriptingErrorLog.SemanticErrorException {
        findCustomClassForFormSetup(className).setListForm(form, getObjectEntity(objectID));
    }

    public void setIsPrintForm(boolean isPrintForm) {
        form.isPrintForm = isPrintForm;
    }

    public void setShowType(FormShowType showType) {
        if (showType != null)
            form.showType = showType;
    }

    private CustomClass findCustomClassForFormSetup(String className) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass valueClass = LM.findClassByCompoundName(className);
        if (!(valueClass instanceof CustomClass)) {
            LM.getErrLog().emitBuiltInClassFormSetupError(LM.getParser(), className);
        }

        return (CustomClass) valueClass;
    }

    public List<String> getObjectsNames() {
        return form.getObjectsNames();
    }
}
