package platform.server.logics.scripted;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.interop.ClassViewType;
import platform.interop.FormEventType;
import platform.interop.ModalityType;
import platform.server.classes.ColorClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.derived.DerivedProperty;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.nvl;

/**
 * User: DAle
 * Date: 26.07.11
 * Time: 19:27
 */

public class ScriptingFormEntity {
    private ScriptingLogicsModule LM;
    private FormEntity form;

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

                ObjectEntity obj = new ObjectEntity(form.genID(), cls, objectCaption);
                addObjectEntity(objectName, obj, groupObj);

                if (groupObject.events.get(j) != null) {
                    form.addActionsOnEvent(obj, groupObject.events.get(j));
                }
            }

            String groupName = groupObject.groupName;
            if (groupName == null) {
                groupName = "";
                for (ObjectEntity obj : groupObj.getOrderObjects()) {
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
                // todo [dale]: перейти на formPropertyObject?
                groups.get(i).reportPathProp = addCalcPropertyObject(groupObjects.get(i).reportPathPropName, groupObjects.get(i).reportPathMapping);
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
                GroupObjectEntity groupObj = groups.get(groupObjects.indexOf(groupObject));

                List<CalcPropertyObjectEntity> propertyObjects = new ArrayList<CalcPropertyObjectEntity>();
                for (String sid : properties) {
                    if (sid != null)
                        propertyObjects.add(form.addPropertyObject((LCP)LM.findLPByCompoundName(sid), groupObj.getOrderObjects().toArray(new ObjectEntity[groupObj.getObjects().size()])));
                }

                if (!propertyObjects.isEmpty())
                    groupObj.setIsParents(propertyObjects.toArray(new CalcPropertyObjectEntity[propertyObjects.size()]));
            }
        }
        form.addTreeGroupObject(treeSID, groups.toArray(new GroupObjectEntity[groups.size()]));
    }

    private void addGroupObjectEntity(String groupName, GroupObjectEntity group) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getGroupObject(groupName) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "group object", groupName);
        }
        group.setSID(groupName);
        form.addGroupObject(group);
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

    public List<String> getUsedObjectNames(List<String> context, List<Integer> usedParams) {
        List<String> usedNames = new ArrayList<String>();
        for (int usedIndex : usedParams) {
            usedNames.add(context.get(usedIndex));
        }
        return usedNames;
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
                checkSingleParam(mapping.size());

                //assertion, что создастся только один PropertyDrawEntity
                property = BaseUtils.<PropertyDrawEntity>single(
                        form.addPropertyDraw(LM.baseLM.objectValue, false, getMappingObjectsArray(mapping))
                );
            } else if (propertyName.equals("SELECTION")) {
                //assertion, что создастся только один PropertyDrawEntity
                property = BaseUtils.<PropertyDrawEntity>single(
                        form.addPropertyDraw(LM.baseLM.selection, false, getMappingObjectsArray(mapping))
                );
            } else if (propertyName.equals("ADDOBJ")) {
                checkSingleParam(mapping.size());

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                LAP<?> addObjAction = LM.getFormAddObjectAction(form, obj[0]);
                property = form.addPropertyDraw(addObjAction);
            } else if (propertyName.equals("ADDFORM") || propertyName.equals("ADDSESSIONFORM")) {
                checkSingleParam(mapping.size());

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                property = LM.addAddFormAction(form, obj[0], propertyName.equals("ADDSESSIONFORM"));
            } else if (propertyName.equals("EDITFORM") || propertyName.equals("EDITSESSIONFORM")) {
                checkSingleParam(mapping.size());

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                property = LM.addEditFormAction(form, obj[0], propertyName.equals("EDITSESSIONFORM"));
            } else if (propertyName.equals("DELETE") || propertyName.equals("DELETESESSION")) {
                checkSingleParam(mapping.size());

                ObjectEntity[] obj = getMappingObjectsArray(mapping);
                property = LM.addFormDeleteAction(form, obj[0], propertyName.equals("DELETESESSION"));
            } else {
                MappedProperty prop = getPropertyWithMapping(propertyName, mapping);
                property = form.addPropertyDraw(prop.property, prop.mapping);
            }
            FormPropertyOptions propertyOptions = commonOptions.overrideWith(options.get(i));
            applyPropertyOptions(property, propertyOptions);

            // Добавляем PropertyDrawView в FormView, если он уже был создан
            form.addPropertyDrawView(property);

            movePropertyDraw(property, propertyOptions);

            setPropertyDrawAlias(alias, property);
        }
    }

    private void checkSingleParam(int size) throws ScriptingErrorLog.SemanticErrorException {
        if (size != 1) {
            LM.getErrLog().emitParamCountError(LM.getParser(), 1, size);
        }
    }

    public void applyPropertyOptions(PropertyDrawEntity property, FormPropertyOptions options) throws ScriptingErrorLog.SemanticErrorException {
        if (options.getEditType() != null) {
            property.setEditType(options.getEditType());
        }

        if (options.getColumns() != null) {
            property.setColumnGroupObjects(SetFact.fromJavaOrderSet(options.getColumns()));
        }

        property.propertyCaption = options.getHeader();
        property.propertyFooter = options.getFooter();

        CalcPropertyObjectEntity backgroundProperty = options.getBackground();
        if (backgroundProperty != null && !backgroundProperty.property.getType().equals(ColorClass.instance)) {
            property.propertyBackground = addGroundPropertyObject(backgroundProperty, true);
        } else {
            property.propertyBackground = backgroundProperty;
        }

        CalcPropertyObjectEntity foregroundProperty = options.getForeground();
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
            // todo [dale]: нужна проверка на LCP
            LM.showIf(form, property, (LCP) showIf.property, showIf.mapping);
        }

        Boolean hintNoUpdate = options.getHintNoUpdate();
        if (hintNoUpdate != null && hintNoUpdate) {
            form.addHintsNoUpdate((CalcProperty) property.propertyObject.property);
        }
        
        Boolean hintTable = options.getHintTable();
        if (hintTable != null && hintTable) {
            form.addHintsIncrementTable((CalcProperty) property.propertyObject.property);
        }

        Boolean drawToToolbar = options.getDrawToToolbar();
        if (drawToToolbar != null && drawToToolbar) {
            property.setDrawToToolbar(true);
        }

        Map<String, ActionPropertyObjectEntity> editActions = options.getEditActions();
        if (editActions != null) {
            for (Map.Entry<String, ActionPropertyObjectEntity> e : editActions.entrySet()) {
                property.setEditAction(e.getKey(), e.getValue());
            }
        }

        OrderedMap<String, String> contextMenuBindings = options.getContextMenuBindings();
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                property.setContextMenuAction(contextMenuBindings.getKey(i), contextMenuBindings.getValue(i));
            }
        }

        String eventID = options.getEventId();
        if (eventID != null)
            property.eventID = eventID;
    }

    private void movePropertyDraw(PropertyDrawEntity property, FormPropertyOptions options) throws ScriptingErrorLog.SemanticErrorException {
        if (options.getNeighbourPropertyDraw() != null) {
            if (options.getNeighbourPropertyDraw().getToDraw(form) != property.getToDraw(form)) {
                LM.getErrLog().emitNeighbourPropertyError(LM.getParser(), options.getNeighbourPropertyText(), property.getSID());
            }
            form.movePropertyDrawTo(property, options.getNeighbourPropertyDraw(), options.isRightNeighbour());
        }
    }

    private <P extends PropertyInterface, C extends PropertyInterface> CalcPropertyObjectEntity addGroundPropertyObject(CalcPropertyObjectEntity<P> groundProperty, boolean back) {
        LCP<C> defaultColorProp = back ? LM.baseLM.defaultOverrideBackgroundColor : LM.baseLM.defaultOverrideForegroundColor;
        CalcPropertyMapImplement<P, P> groupImplement = groundProperty.property.getImplement();
        CalcPropertyMapImplement<?, P> mapImpl = DerivedProperty.createAnd(groundProperty.property.interfaces,
                new CalcPropertyMapImplement<C, P>(defaultColorProp.property, MapFact.<C, P>EMPTYREV()), groupImplement);
        return new CalcPropertyObjectEntity(
                mapImpl.property,
                mapImpl.mapping.join(groundProperty.mapping));
    }

    private void setPropertyDrawAlias(String alias, PropertyDrawEntity property) {
        assert property != null;

        if (alias == null) {
            return;
        }

        PropertyDrawEntity oldSIDOwner = form.getPropertyDraw(alias);

        property.setSID(alias);

        if (oldSIDOwner != null && oldSIDOwner != property) {
            form.setPropertyDrawGeneratedSID(oldSIDOwner, alias);
        }
    }

    public PropertyDrawEntity getPropertyDraw(String sid) throws ScriptingErrorLog.SemanticErrorException {
        return getPropertyDraw(LM, form, sid);
    }

    static public PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule LM, FormEntity form, String sid) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity property = form.getPropertyDraw(sid);
        if (property == null) {
            LM.getErrLog().emitPropertyNotFoundError(LM.getParser(), sid);
        }
        return property;
    }

    public PropertyDrawEntity getPropertyDraw(String sid, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return getPropertyDraw(LM, form, sid, mapping);
    }

    static public PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule LM, FormEntity form, String sid, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty mappedProp = LM.getPropertyWithMapping(form, sid, mapping);
        PropertyDrawEntity property = form.getPropertyDraw(mappedProp.property, mappedProp.mapping);

        if (property == null) {
            String params = "(";
            for (int i = 0; i < mapping.size(); i++) {
                if (i > 0) {
                    params = params + ", ";
                }
                params = params + mapping.get(i);
            }
            params = params + ")";
            LM.getErrLog().emitPropertyNotFoundError(LM.getParser(), sid + params);
        }

        return property;
    }

    public void addScriptedFilters(List<LP> properties, List<List<String>> mappings) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            form.addFixedFilter(new NotNullFilterEntity(form.addPropertyObject((LCP) properties.get(i), getMappingObjectsArray(mappings.get(i))), true));
        }
    }

    public void addScriptedHints(boolean isHintNoUpdate, List<String> propNames) throws ScriptingErrorLog.SemanticErrorException {
        LCP[] properties = new LCP[propNames.size()];
        for (int i = 0; i < propNames.size(); i++) {
            properties[i] = (LCP) LM.findLPByCompoundName(propNames.get(i));
        }

        if (isHintNoUpdate) {
            form.addHintsNoUpdate(properties);
        } else {
            form.addHintsIncrementTable(properties);
        }
    }
    
    public void addScriptedRegularFilterGroup(String sid, List<String> captions, List<String> keystrokes, List<LP> properties, List<List<String>> mappings, List<Boolean> defaults) throws ScriptingErrorLog.SemanticErrorException {
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
                    new RegularFilterEntity(form.genID(), new NotNullFilterEntity(form.addPropertyObject((LCP) properties.get(i), getMappingObjectsArray(mappings.get(i))), true), caption, keyStroke),
                    setDefault
            );
        }

        form.addRegularFilterGroup(regularFilterGroup);
    }

    public ActionPropertyObjectEntity getActionPropertyObject(List<String> context, ScriptingLogicsModule.LPWithParams action) throws ScriptingErrorLog.SemanticErrorException {
        return form.addPropertyObject((LAP) action.property, getMappingObjectsArray(getUsedObjectNames(context, action.usedParams)));
    }

    public void addScriptedFormEvents(List<ActionPropertyObjectEntity> actions, List<Object> types) throws ScriptingErrorLog.SemanticErrorException {
        assert actions.size() == types.size();
        for (int i = 0; i < actions.size(); i++) {
            Object eventType = types.get(i);
            if (eventType instanceof String) {
                form.addActionsOnEvent(getObjectEntity((String) eventType), actions.get(i));
            } else {
                ActionPropertyObjectEntity action = actions.get(i);
                form.addActionsOnEvent(eventType, eventType == FormEventType.QUERYOK || eventType == FormEventType.QUERYCLOSE, action);
            }
        }
    }

    public CalcPropertyObjectEntity addCalcPropertyObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        PropertyObjectEntity propObject = addPropertyObject(property, mapping);
        if (!(propObject instanceof CalcPropertyObjectEntity)) {
            LM.getErrLog().emitNotCalculationPropertyError(LM.getParser());
        }
        return (CalcPropertyObjectEntity) propObject;
    }

    public ActionPropertyObjectEntity addActionPropertyObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        PropertyObjectEntity propObject = addPropertyObject(property, mapping);
        if (!(propObject instanceof ActionPropertyObjectEntity)) {
            LM.getErrLog().emitNotActionPropertyError(LM.getParser());
        }
        return (ActionPropertyObjectEntity) propObject;
    }

    public PropertyObjectEntity addPropertyObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty prop = getPropertyWithMapping(property, mapping);
        return form.addPropertyObject(prop.property, prop.mapping);
    }

    public void addScriptedDefaultOrder(List<PropertyDrawEntity> properties, List<Boolean> orders) throws ScriptingErrorLog.SemanticErrorException {
        for (int i = 0; i < properties.size(); ++i) {
            form.addDefaultOrder(properties.get(i), orders.get(i));
            form.addDefaultOrderView(properties.get(i), orders.get(i));
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

    public void setModalityType(ModalityType modalityType) {
        if (modalityType != null) {
            form.modalityType = modalityType;
        }
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
