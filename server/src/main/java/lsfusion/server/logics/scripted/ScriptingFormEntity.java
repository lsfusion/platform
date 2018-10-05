package lsfusion.server.logics.scripted;

import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FormEventType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.ServerResponse;
import lsfusion.server.classes.ColorClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.debug.DebugInfo;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.group.AbstractGroup;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.form.instance.FormSessionScope.OLDSESSION;

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

    public void addScriptingGroupObjects(List<ScriptingGroupObject> groupObjects, Version version) throws ScriptingErrorLog.SemanticErrorException {
        for (ScriptingGroupObject groupObject : groupObjects) {
            GroupObjectEntity neighbour = groupObject.neighbourGroupObject;
            Boolean isRightNeighbour = groupObject.isRightNeighbour;
            checkNeighbour(neighbour, isRightNeighbour);
            addScriptingGroupObject(groupObject, null, neighbour, isRightNeighbour, version);
        }
    }
    public List<GroupObjectEntity> addScriptingGroupObjects(List<ScriptingGroupObject> groupObjects, TreeGroupEntity treeGroup, GroupObjectEntity neighbourGroupObject, boolean isRightNeighbour, Version version) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groups = new ArrayList<>();

        boolean reverseList = neighbourGroupObject != null && !isRightNeighbour;
        for (ScriptingGroupObject groupObject : (reverseList ? BaseUtils.reverse(groupObjects) : groupObjects)) {
            GroupObjectEntity groupObj = addScriptingGroupObject(groupObject, treeGroup, neighbourGroupObject, isRightNeighbour, version);
            if(neighbourGroupObject != null)
                neighbourGroupObject = groupObj;
            groups.add(groupObj);
        }
        return (reverseList ? BaseUtils.reverse(groups) : groups);
    }

    private GroupObjectEntity addScriptingGroupObject(ScriptingGroupObject groupObject, TreeGroupEntity treeGroup, GroupObjectEntity neighbour, Boolean isRightNeighbour, Version version) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity groupObj = new GroupObjectEntity(form.genID(), treeGroup);

        for (int j = 0; j < groupObject.objects.size(); j++) {
            String className = groupObject.classes.get(j);
            ValueClass cls = LM.findClass(groupObject.classes.get(j));
            String objectName = nvl(groupObject.objects.get(j), className);
            LocalizedString objectCaption = nvl(groupObject.captions.get(j), cls.getCaption());

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
        if (viewType != null)
            groupObj.setInitClassView(viewType);
        if(!groupObject.isInitType)
            groupObj.setSingleClassView();

        if (groupObject.pageSize != null) {
            groupObj.pageSize = groupObject.pageSize;
        }

        if (groupObject.updateType != null) {
            groupObj.updateType = groupObject.updateType;
        }

        String propertyGroupName = groupObject.propertyGroupName;
        AbstractGroup propertyGroup = (propertyGroupName == null ? null : LM.findGroup(propertyGroupName));
        if(propertyGroup != null)
            groupObj.propertyGroup = propertyGroup;
            
        if(groupObject.integrationSID != null)
            groupObj.setIntegrationSID(groupObject.integrationSID);

        groupObj.setIntegrationKey(groupObject.integrationKey);
        addGroupObjectEntity(groupName, groupObj, neighbour,isRightNeighbour, version);

        return groupObj;
    }

    public void addScriptingTreeGroupObject(String treeSID, GroupObjectEntity neighbour, boolean isRightNeighbour, List<ScriptingGroupObject> groupObjects, List<List<ScriptingLogicsModule.PropertyUsage>> parentProperties, Version version) throws ScriptingErrorLog.SemanticErrorException {
        checkNeighbour(neighbour, isRightNeighbour);

        TreeGroupEntity treeGroup = new TreeGroupEntity(form.genID());
        List<GroupObjectEntity> groups = addScriptingGroupObjects(groupObjects, treeGroup, neighbour, isRightNeighbour, version);
        for (ScriptingGroupObject groupObject : groupObjects) {
            List<ScriptingLogicsModule.PropertyUsage> properties = parentProperties.get(groupObjects.indexOf(groupObject));

            if (properties != null && groupObject.objects.size() != properties.size()) {
                LM.getErrLog().emitDifferentObjsNPropsQuantity(LM.getParser());
            }
            if (properties != null) {
                GroupObjectEntity groupObj = groups.get(groupObjects.indexOf(groupObject));

                List<CalcPropertyObjectEntity> propertyObjects = new ArrayList<>();
                for (ScriptingLogicsModule.PropertyUsage pUsage : properties) {
                    if (pUsage.name != null) {
                        LCP property = findLCPByPropertyUsage(pUsage, groupObj);
                        propertyObjects.add(form.addPropertyObject(property, groupObj.getOrderObjects()));
                    }
                }

                if (!propertyObjects.isEmpty())
                    groupObj.setIsParents(propertyObjects.toArray(new CalcPropertyObjectEntity[propertyObjects.size()]));
            }
        }

        form.addTreeGroupObject(treeGroup, neighbour, isRightNeighbour, treeSID, version, groups.toArray(new GroupObjectEntity[groups.size()]));
    }

    private LCP findLCPByPropertyUsage(ScriptingLogicsModule.PropertyUsage property, GroupObjectEntity group) throws ScriptingErrorLog.SemanticErrorException {
        if (property.classNames != null) {
            return LM.findLCPByPropertyUsage(property);
        } else {
            List<ResolveClassSet> classSets = new ArrayList<>();
            for (ObjectEntity obj : group.getOrderObjects()) {
                classSets.add(obj.baseClass.getResolveSet());
            }
            return LM.findLCPByNameAndClasses(property.name, property.getSourceName(), classSets);
        }
    }

    public void checkNeighbour(GroupObjectEntity neighbour, Boolean isRightNeighbour) throws ScriptingErrorLog.SemanticErrorException {
        if (neighbour != null && neighbour.isInTree()) {
            if(isRightNeighbour != null && isRightNeighbour) {
                if(!neighbour.equals(neighbour.treeGroup.getGroups().last()))
                    LM.getErrLog().emitGroupObjectInTreeAfterNotLastError(LM.getParser(), neighbour.getSID());
            } else {
                if(!neighbour.equals(neighbour.treeGroup.getGroups().get(0)))
                    LM.getErrLog().emitGroupObjectInTreeBeforeNotFirstError(LM.getParser(), neighbour.getSID());
            }
        }
    }

    private void addGroupObjectEntity(String groupName, GroupObjectEntity group, GroupObjectEntity neighbour, Boolean isRightNeighbour, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getNFGroupObject(groupName, version) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "group object", groupName);
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

    private ImOrderSet<ObjectEntity> getMappingObjects(ImOrderSet<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getMappingObjectsArray(form, mapping);
    }

    private ObjectEntity getSingleMappingObject(List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        checkSingleParam(mapping.size());
        return getMappingObjects(SetFact.singletonOrder(BaseUtils.single(mapping))).single();
    }

    private ObjectEntity getSingleCustomClassMappingObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity object = getSingleMappingObject(mapping);
        checkCustomClassParam(object, property);
        return object;
    }

    private ObjectEntity getObjectEntity(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.getNFObjectEntityByName(form, name);
    }

    public List<GroupObjectEntity> getGroupObjectsList(List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groupObjects = new ArrayList<>();
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
    
    public void setReportPath(GroupObjectEntity groupObject, CalcPropertyObjectEntity property) throws ScriptingErrorLog.SemanticErrorException {
        if (groupObject != null) {
            groupObject.reportPathProp = property;
        } else {
            form.reportPathProp = property;
        }
    }
    
    private CustomClass getSingleAddClass(ScriptingLogicsModule.PropertyUsage propertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        List<ValueClass> valueClasses = LM.getValueClasses(propertyUsage);
        if(valueClasses != null) {
            ValueClass valueClass = BaseUtils.single(valueClasses);
            checkCustomClassParam(valueClass, propertyUsage.name);
            return (CustomClass) valueClass;
        }
        return null; 
    }

    private FormSessionScope override(FormPropertyOptions options, FormSessionScope scope) {
        Boolean newSession = options.isNewSession();
        if(newSession != null && newSession) {
            Boolean nested = options.isNested();
            return (nested != null && nested ? FormSessionScope.NESTEDSESSION : FormSessionScope.NEWSESSION);
        }
        return scope;
    }

    public void addScriptedPropertyDraws(List<? extends ScriptingLogicsModule.AbstractFormPropertyUsage> properties, List<String> aliases, List<LocalizedString> captions, FormPropertyOptions commonOptions, List<FormPropertyOptions> options, Version version, List<DebugInfo.DebugPoint> points) throws ScriptingErrorLog.SemanticErrorException {
        boolean reverse = commonOptions.getNeighbourPropertyDraw() != null && commonOptions.isRightNeighbour();
        
        for (int i = reverse ? properties.size() - 1 : 0; (reverse ? i >= 0 : i < properties.size()); i = reverse ? i - 1 : i + 1) {
            ScriptingLogicsModule.AbstractFormPropertyUsage pDrawUsage = properties.get(i);
            String alias = aliases.get(i);
            LocalizedString caption = captions.get(i);

            FormPropertyOptions propertyOptions = commonOptions.overrideWith(options.get(i));

            FormSessionScope scope = override(propertyOptions, FormSessionScope.OLDSESSION);
            
            LP property = null;
            ImOrderSet<ObjectEntity> objects = null;
            if(pDrawUsage instanceof ScriptingLogicsModule.FormPredefinedUsage) {
                ScriptingLogicsModule.FormPredefinedUsage prefefUsage = (ScriptingLogicsModule.FormPredefinedUsage) pDrawUsage;
                ScriptingLogicsModule.PropertyUsage pUsage = prefefUsage.property;
                List<String> mapping = prefefUsage.mapping;
                String propertyName = pUsage.name;
                if (propertyName.equals("VALUE")) {
                    ObjectEntity obj = getSingleMappingObject(mapping);
                    property = LM.getObjValueProp(form, obj);
                    objects = SetFact.singletonOrder(obj);
                } else if (propertyName.equals("NEW") && scope == OLDSESSION) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    CustomClass explicitClass = getSingleAddClass(pUsage);
                    property = LM.getAddObjectAction(form, obj, explicitClass);
                    objects = SetFact.EMPTYORDER();
                } else if (propertyName.equals("NEWEDIT") || (propertyName.equals("NEW") && scope != OLDSESSION)) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    CustomClass explicitClass = getSingleAddClass(pUsage);
                    property = LM.getAddFormAction(form, obj, explicitClass, scope, version);
                    objects = SetFact.EMPTYORDER();
                } else if (propertyName.equals("EDIT")) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    CustomClass explicitClass = getSingleAddClass(pUsage);
                    property = LM.getEditFormAction(obj, explicitClass, scope, version);
                    objects = SetFact.singletonOrder(obj);
                } else if (propertyName.equals("DELETE")) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    property = LM.getDeleteAction(obj, scope);
                    objects = SetFact.singletonOrder(obj);
                }
            }
            Result<Pair<Property, String>> inherited = new Result<>();
            if(property == null) {
                MappedProperty prop = LM.getPropertyWithMapping(form, pDrawUsage, inherited);
                checkPropertyParameters(prop.property, prop.mapping);
                property = prop.property;
                objects = prop.mapping;

                if (alias != null && pDrawUsage instanceof ScriptingLogicsModule.FormLPUsage) {
                    property = LM.makeActionOrPropertyPublic(form, alias, ((ScriptingLogicsModule.FormLPUsage) pDrawUsage));
                }
            }

            String formPath = points.get(i).toString();
            PropertyDrawEntity propertyDraw;
            PropertyObjectEntity propertyObject = property.createObjectEntity(objects);
            if(inherited.result != null)
                propertyDraw = form.addPropertyDraw(null, propertyObject, formPath, inherited.result.second, inherited.result.first, version);
            else
                propertyDraw = form.addPropertyDraw(null, propertyObject, formPath, property.listInterfaces, version);

            try {
                form.setFinalPropertyDrawSID(propertyDraw, alias);
            } catch (FormEntity.AlreadyDefined alreadyDefined) {
                LM.throwAlreadyDefinePropertyDraw(alreadyDefined);
            }

            applyPropertyOptions(propertyDraw, propertyOptions, version);

            // Добавляем PropertyDrawView в FormView, если он уже был создан
            PropertyDrawView view = form.addPropertyDrawView(propertyDraw, version);
            if(view != null)
                view.caption = caption;
            else
                propertyDraw.initCaption = caption; 

            movePropertyDraw(propertyDraw, propertyOptions, version);
        }
    }

    private static FormSessionScope getAddFormActionScope(String name) {
        return OLDSESSION;
    }

    private static FormSessionScope getEditFormActionScope(String name) {
        return OLDSESSION;
    }

    private void checkSingleParam(int size) throws ScriptingErrorLog.SemanticErrorException {
        if (size != 1) {
            LM.getErrLog().emitParamCountError(LM.getParser(), 1, size);
        }
    }

    private void checkCustomClassParam(ObjectEntity param, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        checkCustomClassParam(param.baseClass, propertyName);
    }

    private void checkCustomClassParam(ValueClass cls, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            LM.getErrLog().emitCustomClassExpectedError(LM.getParser(), propertyName);
        }
    }

    private <P extends PropertyInterface> void checkPropertyParameters(LP<P, ?> property, ImOrderSet<ObjectEntity> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ImMap<P, AndClassSet> map = property.listInterfaces.mapList(mapping).mapValues(new GetValue<AndClassSet, ObjectEntity>() {
            public AndClassSet getMapValue(ObjectEntity value) {
                return value.getAndClassSet();
            }
        });

        if (!property.property.isInInterface(map, true)) {
            LM.getErrLog().emitWrongPropertyParametersError(LM.getParser(), property.property.getName());
        }
    }

    public void applyPropertyOptions(PropertyDrawEntity property, FormPropertyOptions options, Version version) throws ScriptingErrorLog.SemanticErrorException {
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
            form.addHintsNoUpdate((CalcProperty) property.getValueProperty().property, version);
        }
        
        Boolean hintTable = options.getHintTable();
        if (hintTable != null && hintTable) {
            form.addHintsIncrementTable(version, (CalcProperty) property.getValueProperty().property);
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

        OrderedMap<String, LocalizedString> contextMenuBindings = options.getContextMenuBindings();
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                property.setContextMenuAction(contextMenuBindings.getKey(i), contextMenuBindings.getValue(i));
            }
        }
        
        Map<KeyStroke, String> keyBindings = options.getKeyBindings();
        if (keyBindings != null) {
            for (KeyStroke key : keyBindings.keySet()) {
                property.setKeyAction(key, keyBindings.get(key));
            }
        }

        PropertyEditType editType = options.getEditType();
        if (editType != null)
            property.setEditType(editType);

        Boolean isSelector = options.getSelector();
        ActionPropertyObjectEntity selectorAction;
        if(isSelector != null && isSelector && (selectorAction = property.getSelectorAction(form, version)) != null)
            property.setEditAction(ServerResponse.CHANGE, selectorAction);

        String eventID = options.getEventId();
        if (eventID != null)
            property.eventID = eventID;

        String integrationSID = options.getIntegrationSID();
        if (integrationSID != null)
            property.setIntegrationSID(integrationSID);
        
        String groupName = options.getGroupName();
        AbstractGroup group = (groupName == null ? null : LM.findGroup(groupName));
        if(group != null)
            property.group = group;

        Boolean attr = options.getAttr();
        if(attr != null)
            property.attr = attr;
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
                new CalcPropertyMapImplement<>(defaultColorProp.property, MapFact.<C, P>EMPTYREV()), groupImplement);
        return new CalcPropertyObjectEntity(
                mapImpl.property,
                mapImpl.mapping.join(groundProperty.mapping));
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

    private static PropertyDrawEntity checkPropertyDraw(ScriptingLogicsModule LM, PropertyDrawEntity property, String sid) throws ScriptingErrorLog.SemanticErrorException {
        if (property == null) {
            LM.getErrLog().emitPropertyNotFoundError(LM.getParser(), sid);
        }
        return property;
    }

    public void addScriptedFilters(List<LCP> properties, List<ImOrderSet<String>> mappings, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            LCP property = properties.get(i);
            ImOrderSet<ObjectEntity> mappingObjects = getMappingObjects(mappings.get(i));
            checkPropertyParameters(property, mappingObjects);

            form.addFixedFilter(new FilterEntity(form.addPropertyObject(property, mappingObjects), true), version);
        }
    }

    public void addScriptedHints(boolean isHintNoUpdate, List<ScriptingLogicsModule.PropertyUsage> propUsages, Version version) throws ScriptingErrorLog.SemanticErrorException {
        LCP[] properties = new LCP[propUsages.size()];
        for (int i = 0; i < propUsages.size(); i++) {
            properties[i] = LM.findLCPByPropertyUsage(propUsages.get(i));
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
            LocalizedString caption = info.caption;
            KeyStroke keyStroke = (info.keystroke != null ? KeyStroke.getKeyStroke(info.keystroke) : null);
            boolean isDefault = info.isDefault;

            if (info.keystroke != null && keyStroke == null) {
                LM.getErrLog().emitWrongKeyStrokeFormat(LM.getParser(), info.keystroke);
            }

            ImOrderSet<String> mapping = info.mapping;
            LCP<?> property = info.property;

            ImOrderSet<ObjectEntity> mappingObjects = getMappingObjects(mapping);
            checkPropertyParameters(property, mappingObjects);

            RegularFilterEntity filter = new RegularFilterEntity(form.genID(), new FilterEntity(form.addPropertyObject(property, mappingObjects), true), caption, keyStroke);
            if (extend) {
                form.addRegularFilter(filterGroup, filter, isDefault, version);
            } else {
                filterGroup.addFilter(filter, isDefault, version);
            }
        }
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

    public CalcPropertyObjectEntity addCalcPropertyObject(ScriptingLogicsModule.AbstractFormCalcPropertyUsage property) throws ScriptingErrorLog.SemanticErrorException {
        return addCalcPropertyObject(LM, form, property);
    }

    public static CalcPropertyObjectEntity addCalcPropertyObject(ScriptingLogicsModule LM, FormEntity form, ScriptingLogicsModule.AbstractFormCalcPropertyUsage property) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty prop = LM.getPropertyWithMapping(form, property, null);
        return form.addPropertyObject((LCP)prop.property, prop.mapping);
    }

    public ActionPropertyObjectEntity addActionPropertyObject(ScriptingLogicsModule.AbstractFormActionPropertyUsage property) throws ScriptingErrorLog.SemanticErrorException {
        MappedProperty prop = LM.getPropertyWithMapping(form, property, null);
        return form.addPropertyObject((LAP)prop.property, prop.mapping);
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
        List<ValueClass> classes = new ArrayList<>();
        List<String> objNames = form.getNFObjectsNamesAndClasses(classes, version);
        List<ScriptingLogicsModule.TypedParameter> typedObjects = new ArrayList<>();
        for (int i = 0; i < classes.size(); ++i) {
            typedObjects.add(LM.new TypedParameter(classes.get(i), objNames.get(i)));
        }
        return typedObjects;
    }
    
    public static class RegularFilterInfo {
        LocalizedString caption;
        String keystroke;
        LCP property;
        ImOrderSet<String> mapping;
        boolean isDefault;

        public RegularFilterInfo(LocalizedString caption, String keystroke, LCP property, ImOrderSet<String> mapping, boolean isDefault) {
            this.caption = caption;
            this.keystroke = keystroke;
            this.property = property;
            this.mapping = mapping;
            this.isDefault = isDefault;
        }
    }
}
