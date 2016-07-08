package lsfusion.server.logics.scripted;

import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FormEventType;
import lsfusion.interop.ModalityType;
import lsfusion.server.classes.ColorClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.logics.debug.DebugInfo;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.form.instance.FormSessionScope.*;

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

    public List<GroupObjectEntity> addScriptingGroupObjects(List<ScriptingGroupObject> groupObjects, Version version) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groups = new ArrayList<GroupObjectEntity>();
        for (ScriptingGroupObject groupObject : groupObjects) {
            GroupObjectEntity groupObj = new GroupObjectEntity(form.genID());

            for (int j = 0; j < groupObject.objects.size(); j++) {
                String className = groupObject.classes.get(j);
                ValueClass cls = LM.findClass(groupObject.classes.get(j));
                String objectName = nvl(groupObject.objects.get(j), className);
                String objectCaption = nvl(groupObject.captions.get(j), cls.getCaption());

                ObjectEntity obj = new ObjectEntity(form.genID(), cls, objectCaption);
                addObjectEntity(objectName, obj, groupObj, version);

                if (groupObject.events.get(j) != null) {
                    form.addActionsOnEvent(obj, version, groupObject.events.get(j));
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
            
            if (groupObject.updateType != null) {
                groupObj.updateType = groupObject.updateType;
            }

            addGroupObjectEntity(groupName, groupObj, groupObject.neighbourGroupObject, groupObject.isRightNeighbour, version);
            groups.add(groupObj);
        }

        for (int i = 0; i < groupObjects.size(); i++) {
            if (groupObjects.get(i).reportPathPropUsage != null) {
                // todo [dale]: перейти на formPropertyObject?
                groups.get(i).reportPathProp = addCalcPropertyObject(groupObjects.get(i).reportPathPropUsage, groupObjects.get(i).reportPathMapping);
            }
        }
        return groups;
    }

    public void addScriptingTreeGroupObject(String treeSID, List<ScriptingGroupObject> groupObjects, List<List<ScriptingLogicsModule.PropertyUsage>> parentProperties, Version version) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groups = addScriptingGroupObjects(groupObjects, version);
        for (ScriptingGroupObject groupObject : groupObjects) {
            List<ScriptingLogicsModule.PropertyUsage> properties = parentProperties.get(groupObjects.indexOf(groupObject));

            if (properties != null && groupObject.objects.size() != properties.size()) {
                LM.getErrLog().emitDifferentObjsNPropsQuantity(LM.getParser());
            }
            if (properties != null) {
                GroupObjectEntity groupObj = groups.get(groupObjects.indexOf(groupObject));

                List<CalcPropertyObjectEntity> propertyObjects = new ArrayList<CalcPropertyObjectEntity>();
                for (ScriptingLogicsModule.PropertyUsage pUsage : properties) {
                    if (pUsage.name != null) {
                        LCP property = (LCP) findLP(pUsage, groupObj);
                        propertyObjects.add(form.addPropertyObject(property, groupObj.getOrderObjects().toArray(new ObjectEntity[groupObj.getObjects().size()])));
                    }
                }

                if (!propertyObjects.isEmpty())
                    groupObj.setIsParents(propertyObjects.toArray(new CalcPropertyObjectEntity[propertyObjects.size()]));
            }
        }
        form.addTreeGroupObject(treeSID, version, groups.toArray(new GroupObjectEntity[groups.size()]));
    }

    private LP findLP(ScriptingLogicsModule.PropertyUsage property, GroupObjectEntity group) throws ScriptingErrorLog.SemanticErrorException {
        if (property.classNames != null) {
            return LM.findLPByPropertyUsage(property);
        } else {
            ValueClass[] signature = new ValueClass[group.getOrderObjects().size()];
            int index = 0;
            for (ObjectEntity obj : group.getOrderObjects()) {
                signature[index] = obj.baseClass;
                ++index;
            }
            return LM.findLPByNameAndClasses(property.name, signature);
        }
    }
    
    private void addGroupObjectEntity(String groupName, GroupObjectEntity group, GroupObjectEntity neighbour, Boolean isRightNeighbour, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getNFGroupObject(groupName, version) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "group object", groupName);
        }
        if (neighbour != null && neighbour.treeGroup != null) {
            LM.getErrLog().emitGroupObjectInTreeAfterBeforeError(LM.getParser(), neighbour.getSID());    
        }
        group.setSID(groupName);
        form.addGroupObject(group,neighbour, isRightNeighbour, version);
    }

    private void addObjectEntity(String name, ObjectEntity object, GroupObjectEntity group, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getNFObject(name, version) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "object", name);
        }
        object.setSID(name);
        group.add(object);
    }

    private ObjectEntity[] getMappingObjectsArray(List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getMappingObjectsArray(form, mapping);
    }

    private ObjectEntity getSingleMappingObject(List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        checkSingleParam(mapping.size());
        ObjectEntity[] objects = getMappingObjectsArray(mapping);
        return objects[0];
    }

    private ObjectEntity getSingleCustomClassMappingObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity object = getSingleMappingObject(mapping);
        checkCustomClassParam(object, property);
        return object;
    }

    private ObjectEntity getObjectEntity(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getNFObjectEntityByName(form, name);
    }

    public MappedProperty getPropertyWithMapping(ScriptingLogicsModule.PropertyUsage pUsage, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getPropertyWithMapping(form, pUsage, mapping);
    }

    public List<GroupObjectEntity> getGroupObjectsList(List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groupObjects = new ArrayList<GroupObjectEntity>();
        for (String groupName : mapping) {
            GroupObjectEntity groupObject = form.getNFGroupObject(groupName, version);
            if (groupObject == null) {
                LM.getErrLog().emitParamNotFoundError(LM.getParser(), groupName);
            } else {
                groupObjects.add(groupObject);
            }

        }
        return groupObjects;
    }

    public GroupObjectEntity getGroupObjectEntity(String groupSID, Version version) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity groupObject = form.getNFGroupObject(groupSID, version);
        if (groupObject == null) {
            LM.getErrLog().emitNotFoundError(LM.getParser(), "groupObject", groupSID);
        }
        return groupObject;
    }
    
    public void setReportPath(GroupObjectEntity groupObject, ScriptingLogicsModule.PropertyUsage propUsage, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        if (groupObject != null) {
            groupObject.reportPathProp = addCalcPropertyObject(propUsage, mapping);
        } else {
            form.reportPathProp = addCalcPropertyObject(propUsage, mapping);
        }
    }

    public void addScriptedPropertyDraws(List<ScriptingLogicsModule.PropertyUsage> properties, List<String> aliases, List<List<String>> mappings, FormPropertyOptions commonOptions, List<FormPropertyOptions> options, Version version, List<DebugInfo.DebugPoint> points) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();

        boolean reverse = commonOptions.getNeighbourPropertyDraw() != null && commonOptions.isRightNeighbour();
        
        for (int i = reverse ? properties.size() - 1 : 0; (reverse ? i >= 0 : i < properties.size()); i = reverse ? i - 1 : i + 1) {
            List<String> mapping = mappings.get(i);
            ScriptingLogicsModule.PropertyUsage pUsage = properties.get(i);
            String propertyName = pUsage.name;
            String alias = aliases.get(i);

            PropertyDrawEntity property;
            if (propertyName.equals("OBJVALUE")) {
                checkSingleParam(mapping.size());

                //assertion, что создастся только один PropertyDrawEntity
                property = BaseUtils.<PropertyDrawEntity>single(
                        form.addPropertyDraw(LM.baseLM.objectValue, version, getMappingObjectsArray(mapping))
                );
            } else if (propertyName.equals("ADDOBJ")) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                LAP<?> addObjAction = LM.getAddObjectAction(form, obj);
                property = form.addPropertyDraw(addObjAction, version);
            } else if (propertyName.equals("ADDFORM") || propertyName.equals("ADDSESSIONFORM") || propertyName.equals("ADDNESTEDFORM")) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                property = LM.addAddFormAction(form, obj, getAddFormActionScope(propertyName), version);
            } else if (propertyName.equals("EDITFORM") || propertyName.equals("EDITSESSIONFORM") || propertyName.equals("EDITNESTEDFORM")) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                property = LM.addEditFormAction(form, obj, getEditFormActionScope(propertyName), version);
            } else if (propertyName.equals("DELETE") || propertyName.equals("DELETESESSION")) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                property = LM.addFormDeleteAction(form, obj, propertyName.equals("DELETESESSION"), version);
            } else {
                MappedProperty prop = getPropertyWithMapping(pUsage, mapping);

                checkPropertyParameters(prop.property, prop.mapping);
                String formPath = points.get(i).toString();
                property = form.addPropertyDraw(prop.property, version, formPath, prop.mapping);
            }
            FormPropertyOptions propertyOptions = commonOptions.overrideWith(options.get(i));
            applyPropertyOptions(property, propertyOptions, version);

            // Добавляем PropertyDrawView в FormView, если он уже был создан
            form.addPropertyDrawView(property, version);

            movePropertyDraw(property, propertyOptions, version);

            setFinalPropertyDrawSID(property, alias, version);
        }
    }

    private static FormSessionScope getAddFormActionScope(String name) {
        if ("ADDFORM".equals(name)) {
            return NEWSESSION;
        } else if ("ADDSESSIONFORM".equals(name)) {
            return OLDSESSION;
        } else if ("ADDNESTEDFORM".equals(name)) {
            return NESTEDSESSION;
        }
        throw new IllegalStateException("incorrect EDITFORM action name");
    }

    private static FormSessionScope getEditFormActionScope(String name) {
        if ("EDITFORM".equals(name)) {
            return NEWSESSION;
        } else if ("EDITSESSIONFORM".equals(name)) {
            return OLDSESSION;
        } else if ("EDITNESTEDFORM".equals(name)) {
            return NESTEDSESSION;
        }
        throw new IllegalStateException("incorrect EDITFORM action name");
    }

    private void checkSingleParam(int size) throws ScriptingErrorLog.SemanticErrorException {
        if (size != 1) {
            LM.getErrLog().emitParamCountError(LM.getParser(), 1, size);
        }
    }

    private void checkCustomClassParam(ObjectEntity param, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(param.baseClass instanceof CustomClass)) {
            LM.getErrLog().emitCustomClassExpextedError(LM.getParser(), propertyName);
        }
    }

    private void checkPropertyParameters(LP<PropertyInterface, ?> property, PropertyObjectInterfaceEntity[] mapping) throws ScriptingErrorLog.SemanticErrorException {
        ImMap<PropertyInterface, AndClassSet> map = property.listInterfaces.mapList(ListFact.toList(mapping)).mapValues(new GetValue<AndClassSet, PropertyObjectInterfaceEntity>() {
            @Override
            public AndClassSet getMapValue(PropertyObjectInterfaceEntity value) {
                return value.getAndClassSet();
            }
        });

        if (!property.property.isInInterface(map, true)) {
            LM.getErrLog().emitWrongPropertyParametersError(LM.getParser(), property.property.getName());
        }
    }

    public void applyPropertyOptions(PropertyDrawEntity property, FormPropertyOptions options, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (options.getEditType() != null) {
            property.setEditType(options.getEditType());
        }

        FormPropertyOptions.Columns columns = options.getColumns();
        if (columns != null) {
            property.setColumnGroupObjects(columns.columnsName, SetFact.fromJavaOrderSet(columns.columns));
        }

        property.propertyCaption = options.getHeader();
        property.propertyFooter = options.getFooter();

        property.propertyShowIf = options.getShowIf();

        property.quickFilterProperty = options.getQuickFilterPropertyDraw();

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

        Boolean hintNoUpdate = options.getHintNoUpdate();
        if (hintNoUpdate != null && hintNoUpdate) {
            form.addHintsNoUpdate((CalcProperty) property.propertyObject.property, version);
        }
        
        Boolean hintTable = options.getHintTable();
        if (hintTable != null && hintTable) {
            form.addHintsIncrementTable(version, (CalcProperty) property.propertyObject.property);
        }

        Boolean drawToToolbar = options.getDrawToToolbar();
        if (drawToToolbar != null && drawToToolbar) {
            property.setDrawToToolbar(true);
        }

        Boolean optimisticAsync = options.getOptimisticAsync();
        if (optimisticAsync != null && optimisticAsync) {
            property.optimisticAsync = true;
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

    private void movePropertyDraw(PropertyDrawEntity property, FormPropertyOptions options, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (options.getNeighbourPropertyDraw() != null) {
            if (options.getNeighbourPropertyDraw().getNFToDraw(form, version) != property.getNFToDraw(form, version)) {
                LM.getErrLog().emitNeighbourPropertyError(LM.getParser(), options.getNeighbourPropertyText(), property.getSID());
            }
            form.movePropertyDrawTo(property, options.getNeighbourPropertyDraw(), options.isRightNeighbour(), version);
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

    public void setFinalPropertyDrawSID(PropertyDrawEntity property, String alias, Version version) throws ScriptingErrorLog.SemanticErrorException {
        String newSID = (alias == null ? property.getSID() : alias);
        property.setSID(null);
        try {
            if (form.getPropertyDraw(newSID, version) != null) {
                LM.getErrLog().emitAlreadyDefinedPropertyDraw(LM.getParser(), form.getCanonicalName(), newSID);
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            System.err.println(e.getMessage());
        }
        property.setSID(newSID);
    }

    public PropertyDrawEntity getPropertyDraw(String sid, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return getPropertyDraw(LM, form, sid, version);
    }

    public static PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule LM, FormEntity form, String sid, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return checkPropertyDraw(LM, form.getPropertyDraw(sid, version), sid);
    }

    public PropertyDrawEntity getPropertyDraw(String name, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return getPropertyDraw(LM, form, name, mapping, version);
    }

    public static PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule LM, FormEntity form, String name, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return checkPropertyDraw(LM, form.getPropertyDraw(name, mapping, version), name);    
    }

    public PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule.PropertyUsage propUsage, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return getPropertyDraw(LM, form, propUsage, mapping, version);
    }

    static public PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule LM, FormEntity form, ScriptingLogicsModule.PropertyUsage pUsage, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty mappedProp = LM.getPropertyWithMapping(form, pUsage, mapping);
        PropertyDrawEntity property = form.getNFPropertyDraw(mappedProp.property, version, mappedProp.mapping);

        if (property == null) {
            LM.getErrLog().emitPropertyNotFoundError(LM.getParser(), PropertyDrawEntity.createSID(pUsage.name, mapping));
        }
        return property;
    }

    private static PropertyDrawEntity checkPropertyDraw(ScriptingLogicsModule LM, PropertyDrawEntity property, String sid) throws ScriptingErrorLog.SemanticErrorException {
        if (property == null) {
            LM.getErrLog().emitPropertyNotFoundError(LM.getParser(), sid);
        }
        return property;
    }

    public void addScriptedFilters(List<LP> properties, List<List<String>> mappings, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            LCP property = (LCP) properties.get(i);
            checkPropertyParameters(property, getMappingObjectsArray(mappings.get(i)));

            form.addFixedFilter(new NotNullFilterEntity(form.addPropertyObject(property, getMappingObjectsArray(mappings.get(i))), true), version);
        }
    }

    public void addScriptedHints(boolean isHintNoUpdate, List<ScriptingLogicsModule.PropertyUsage> propUsages, Version version) throws ScriptingErrorLog.SemanticErrorException {
        LCP[] properties = new LCP[propUsages.size()];
        for (int i = 0; i < propUsages.size(); i++) {
            properties[i] = (LCP) LM.findLPByPropertyUsage(propUsages.get(i));
        }

        if (isHintNoUpdate) {
            form.addHintsNoUpdate(version, properties);
        } else {
            form.addHintsIncrementTable(version, properties);
        }
    }
    
    public void addScriptedRegularFilterGroup(String sid, List<RegularFilterInfo> filters, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getNFRegularFilterGroup(sid, version) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "filter group", sid);
        }

        RegularFilterGroupEntity regularFilterGroup = new RegularFilterGroupEntity(form.genID(), version);
        regularFilterGroup.setSID(sid);

        addRegularFilters(regularFilterGroup, filters, version, false);

        form.addRegularFilterGroup(regularFilterGroup, version);
    }

    public void extendScriptedRegularFilterGroup(String sid, List<RegularFilterInfo> filters, Version version) throws ScriptingErrorLog.SemanticErrorException {
        RegularFilterGroupEntity filterGroup = form.getNFRegularFilterGroup(sid, version);
        if (filterGroup == null) {
            LM.getErrLog().emitFilterGroupNotFoundError(LM.getParser(), sid);
        }
        
        addRegularFilters(filterGroup, filters, version, true);
    }

    public void addRegularFilters(RegularFilterGroupEntity filterGroup, List<RegularFilterInfo> filters, Version version, boolean extend) throws ScriptingErrorLog.SemanticErrorException {
        for (RegularFilterInfo info : filters) {
            String caption = info.caption;
            KeyStroke keyStroke = (info.keystroke != null ? KeyStroke.getKeyStroke(info.keystroke) : null);
            boolean isDefault = info.isDefault;

            if (info.keystroke != null && keyStroke == null) {
                LM.getErrLog().emitWrongKeyStrokeFormat(LM.getParser(), info.keystroke);
            }

            List<String> mapping = info.mapping;
            LP property = info.property;

            checkPropertyParameters(property, getMappingObjectsArray(mapping));

            RegularFilterEntity filter = new RegularFilterEntity(form.genID(), new NotNullFilterEntity(form.addPropertyObject((LCP) property, getMappingObjectsArray(mapping)), true), caption, keyStroke);
            if (extend) {
                form.addRegularFilter(filterGroup, filter, isDefault, version);
            } else {
                filterGroup.addFilter(filter, isDefault, version);
            }
        }
    }

    public ActionPropertyObjectEntity getActionPropertyObject(List<ScriptingLogicsModule.TypedParameter> context, ScriptingLogicsModule.LPWithParams action) throws ScriptingErrorLog.SemanticErrorException {
        return form.addPropertyObject((LAP) action.property, getMappingObjectsArray(ScriptingLogicsModule.getUsedNames(context, action.usedParams)));
    }

    public void addScriptedFormEvents(List<ActionPropertyObjectEntity> actions, List<Object> types, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert actions.size() == types.size();
        for (int i = 0; i < actions.size(); i++) {
            Object eventType = types.get(i);
            if (eventType instanceof String) {
                form.addActionsOnEvent(getObjectEntity((String) eventType), version, actions.get(i));
            } else {
                ActionPropertyObjectEntity action = actions.get(i);
                form.addActionsOnEvent(eventType, eventType == FormEventType.QUERYOK || eventType == FormEventType.QUERYCLOSE, version, action);
            }
        }
    }

    public CalcPropertyObjectEntity addCalcPropertyObject(ScriptingLogicsModule.PropertyUsage property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return addCalcPropertyObject(LM, form, property, mapping);
    }

    public static CalcPropertyObjectEntity addCalcPropertyObject(ScriptingLogicsModule LM, FormEntity form, ScriptingLogicsModule.PropertyUsage property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty prop = LM.getPropertyWithMapping(form, property, mapping);
        PropertyObjectEntity propObject = form.addPropertyObject(prop.property, prop.mapping);
        if (!(propObject instanceof CalcPropertyObjectEntity)) {
            LM.getErrLog().emitNotCalculationPropertyError(LM.getParser());
        }
        return (CalcPropertyObjectEntity) propObject;
    }

    public ActionPropertyObjectEntity addActionPropertyObject(ScriptingLogicsModule.PropertyUsage property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        PropertyObjectEntity propObject = addPropertyObject(property, mapping);
        if (!(propObject instanceof ActionPropertyObjectEntity)) {
            LM.getErrLog().emitNotActionPropertyError(LM.getParser());
        }
        return (ActionPropertyObjectEntity) propObject;
    }

    public PropertyObjectEntity addPropertyObject(ScriptingLogicsModule.PropertyUsage property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty prop = getPropertyWithMapping(property, mapping);
        return form.addPropertyObject(prop.property, prop.mapping);
    }

    public void addScriptedDefaultOrder(List<PropertyDrawEntity> properties, List<Boolean> orders, Version version) throws ScriptingErrorLog.SemanticErrorException {
        for (int i = 0; i < properties.size(); ++i) {
            form.addDefaultOrder(properties.get(i), orders.get(i), version);
            form.addDefaultOrderView(properties.get(i), orders.get(i), version);
        }
    }

    public void setAsDialogForm(String className, String objectID, Version version) throws ScriptingErrorLog.SemanticErrorException {
        findCustomClassForFormSetup(className).setDialogForm(form, getObjectEntity(objectID), version);
    }

    public void setAsEditForm(String className, String objectID, Version version) throws ScriptingErrorLog.SemanticErrorException {
        findCustomClassForFormSetup(className).setEditForm(form, getObjectEntity(objectID), version);
    }

    public void setAsListForm(String className, String objectID, Version version) throws ScriptingErrorLog.SemanticErrorException {
        findCustomClassForFormSetup(className).setListForm(form, getObjectEntity(objectID), version);
    }

    public void setModalityType(ModalityType modalityType) {
        if (modalityType != null) {
            form.modalityType = modalityType;
        }
    }

    public void setAutoRefresh(int autoRefresh) {
        form.autoRefresh = autoRefresh;
    }

    private CustomClass findCustomClassForFormSetup(String className) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass valueClass = LM.findClass(className);
        if (!(valueClass instanceof CustomClass)) {
            LM.getErrLog().emitBuiltInClassFormSetupError(LM.getParser(), className);
        }

        return (CustomClass) valueClass;
    }

    public List<ScriptingLogicsModule.TypedParameter> getTypedObjectsNames(Version version) {
        List<ValueClass> classes = new ArrayList<ValueClass>();
        List<String> objNames = form.getNFObjectsNamesAndClasses(classes, version);
        List<ScriptingLogicsModule.TypedParameter> typedObjects = new ArrayList<ScriptingLogicsModule.TypedParameter>();
        for (int i = 0; i < classes.size(); ++i) {
            typedObjects.add(LM.new TypedParameter(classes.get(i), objNames.get(i)));
        }
        return typedObjects;
    }
    
    public static class RegularFilterInfo {
        String caption;
        String keystroke;
        LP property;
        List<String> mapping;
        boolean isDefault;

        public RegularFilterInfo(String caption, String keystroke, LP property, List<String> mapping, boolean isDefault) {
            this.caption = caption;
            this.keystroke = keystroke;
            this.property = property;
            this.mapping = mapping;
            this.isDefault = isDefault;
        }
    }
}
