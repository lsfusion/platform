package lsfusion.server.language.form;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.event.FormEventType;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.form.object.ScriptingGroupObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.language.property.oraction.MappedActionOrProperty;
import lsfusion.server.logics.LogicsModule.InsertType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ColorClass;
import lsfusion.server.logics.classes.data.file.ImageClass;
import lsfusion.server.logics.classes.data.time.TimeSeriesClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.logics.LogicsModule.InsertType.*;
import static lsfusion.server.logics.form.interactive.action.edit.FormSessionScope.OLDSESSION;

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

    public void addScriptingGroupObjects(List<ScriptingGroupObject> groupObjects, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        for (ScriptingGroupObject groupObject : groupObjects) {
            GroupObjectEntity neighbour = groupObject.neighbourGroupObject;
            InsertType insertType = groupObject.insertType;
            checkNeighbour(neighbour, insertType);
            addScriptingGroupObject(groupObject, null, neighbour, insertType, version, debugPoint);
        }
    }
    public List<GroupObjectEntity> addScriptingGroupObjects(List<ScriptingGroupObject> groupObjects, TreeGroupEntity treeGroup, GroupObjectEntity neighbourGroupObject, InsertType insertType, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        List<GroupObjectEntity> groups = new ArrayList<>();

        boolean reverseList = neighbourGroupObject != null && insertType == BEFORE || insertType == FIRST;
        for (ScriptingGroupObject groupObject : (reverseList ? BaseUtils.reverse(groupObjects) : groupObjects)) {
            GroupObjectEntity groupObj = addScriptingGroupObject(groupObject, treeGroup, neighbourGroupObject, insertType, version, debugPoint);
            if(neighbourGroupObject != null)
                neighbourGroupObject = groupObj;
            groups.add(groupObj);
        }
        return (reverseList ? BaseUtils.reverse(groups) : groups);
    }

    private GroupObjectEntity addScriptingGroupObject(ScriptingGroupObject groupObject, TreeGroupEntity treeGroup, GroupObjectEntity neighbour, InsertType insertType, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity groupObj = new GroupObjectEntity(form.genID(), treeGroup);
        groupObj.setScriptIndex(Pair.create(debugPoint.line, debugPoint.offset));

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

            if (groupObject.integrationSIDs.get(j) != null) {
                obj.setIntegrationSID(groupObject.integrationSIDs.get(j));
            }
        }

        String groupName = groupObject.groupName;
        if (groupName == null) {
            groupName = "";
            for (ObjectEntity obj : groupObj.getOrderObjects()) {
                groupName = (groupName.length() == 0 ? "" : groupName + ".") + obj.getSID();
            }
        }

        if (groupObject.viewType != null)
            groupObj.setViewType(groupObject.viewType);
        if (groupObject.listViewType != null)
            groupObj.setListViewType(groupObject.listViewType);
        if(groupObject.pivotOptions != null)
            groupObj.setPivotOptions(groupObject.pivotOptions);
        if (groupObject.customRenderFunction != null)
            groupObj.setCustomRenderFunction(groupObject.customRenderFunction);
        if (groupObject.mapTileProvider != null)
            groupObj.setMapTileProvider(groupObject.mapTileProvider);

        if (groupObject.pageSize != null) {
            groupObj.pageSize = groupObject.pageSize;
        }

        if (groupObject.updateType != null) {
            groupObj.updateType = groupObject.updateType;
        }

        String propertyGroupName = groupObject.propertyGroupName;
        Group propertyGroup = (propertyGroupName == null ? null : LM.findGroup(propertyGroupName));
        if(propertyGroup != null)
            groupObj.propertyGroup = propertyGroup;

        String integrationSID = groupObject.integrationSID;
        if(integrationSID == null && groupObject.groupName == null) {
            integrationSID = "";
            for (ObjectEntity obj : groupObj.getOrderObjects()) {
                integrationSID = (integrationSID.length() == 0 ? "" : integrationSID + ".") + obj.getIntegrationSID();
            }
        }
        if(integrationSID != null)
            groupObj.setIntegrationSID(integrationSID);

        groupObj.setIntegrationKey(groupObject.integrationKey);
        addGroupObjectEntity(groupName, groupObj, neighbour, insertType, version);

        if(groupObject.isSubReport)
            setSubReport(groupObj, groupObject.subReportPath);

        if(groupObject.background != null)
            groupObj.setPropertyBackground(addPropertyObject(groupObject.background));
        if(groupObject.foreground != null)
            groupObj.setPropertyForeground(addPropertyObject(groupObject.foreground));

        return groupObj;
    }

    public void addScriptingTreeGroupObject(String treeSID, GroupObjectEntity neighbour, InsertType insertType, List<ScriptingGroupObject> groupObjects, List<List<LP>> parentProperties, List<List<ImOrderSet<String>>> propertyMappings, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        checkNeighbour(neighbour, insertType);

        TreeGroupEntity treeGroup = new TreeGroupEntity(form.genID());
        List<GroupObjectEntity> groups = addScriptingGroupObjects(groupObjects, treeGroup, neighbour, insertType, version, debugPoint);
        for (ScriptingGroupObject groupObject : groupObjects) {
            int groupIndex = groupObjects.indexOf(groupObject);
            List<LP> properties = parentProperties.get(groupIndex);
            List<ImOrderSet<String>> propertyMapping = propertyMappings.get(groupIndex);

            if (properties != null && groupObject.objects.size() != properties.size()) {
                LM.getErrLog().emitDifferentObjsNPropsQuantityError(LM.getParser(), groupObject.objects.size());
            }
            if (properties != null) {
                GroupObjectEntity groupObj = groups.get(groupObjects.indexOf(groupObject));

                List<PropertyObjectEntity> propertyObjects = new ArrayList<>();
                for (int i = 0; i < properties.size(); i++) {
                    LP property = properties.get(i);
                    if (propertyMapping != null && property.property.getName() != null) {
                        ImOrderSet<ObjectEntity> mappingObjects = getMappingObjects(propertyMapping.get(i));
                        checkPropertyParameters(property, mappingObjects);
                        propertyObjects.add(form.addPropertyObject(property, mappingObjects));
                    }
                }

                if (!propertyObjects.isEmpty())
                    groupObj.setIsParents(propertyObjects.toArray(new PropertyObjectEntity[propertyObjects.size()]));
            }
        }

        form.addTreeGroupObject(treeGroup, neighbour, insertType, treeSID, version, groups.toArray(new GroupObjectEntity[groups.size()]));
    }

    private LP findLPByPropertyUsage(ScriptingLogicsModule.NamedPropertyUsage property, GroupObjectEntity group) throws ScriptingErrorLog.SemanticErrorException {
        if (property.classNames != null) {
            return LM.findLPByPropertyUsage(property);
        } else {
            List<ResolveClassSet> classSets = new ArrayList<>();
            for (ObjectEntity obj : group.getOrderObjects()) {
                classSets.add(obj.baseClass.getResolveSet());
            }
            return LM.findLPByNameAndClasses(property.name, property.getSourceName(), classSets);
        }
    }

    public void checkNeighbour(GroupObjectEntity neighbour, InsertType insertType) throws ScriptingErrorLog.SemanticErrorException {
        if (neighbour != null && neighbour.isInTree()) {
            if (insertType == AFTER) {
                if (!neighbour.equals(neighbour.treeGroup.getGroups().last()))
                    LM.getErrLog().emitGroupObjectInTreeAfterNotLastError(LM.getParser(), neighbour.getSID());
            } else if (insertType == BEFORE) {
                if (!neighbour.equals(neighbour.treeGroup.getGroups().get(0)))
                    LM.getErrLog().emitGroupObjectInTreeBeforeNotFirstError(LM.getParser(), neighbour.getSID());
            }
        }
    }
    private void addGroupObjectEntity(String groupName, GroupObjectEntity group, GroupObjectEntity neighbour, InsertType insertType, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getNFGroupObject(groupName, version) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "group object", groupName);
        }
        group.setSID(groupName);
        form.addGroupObject(group, neighbour, insertType, version);
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
        checkParamCount(mapping.size(), 1);
        return getMappingObjects(SetFact.singletonOrder(BaseUtils.single(mapping))).single();
    }

    private ObjectEntity getSingleCustomClassMappingObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity object = getSingleMappingObject(mapping);
        checkCustomClassParam(object, property);
        return object;
    }

    private ImOrderSet<ObjectEntity> getTwinTimeSeriesMappingObject(String property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        checkParamCount(mapping.size(), 2);
        ImOrderSet<ObjectEntity> mappingObjects = getMappingObjects(SetFact.fromJavaOrderSet(mapping));
        checkTimeSeriesParam(mappingObjects.get(0), property);
        checkTimeSeriesParam(mappingObjects.get(1), property);
        checkEqualParamClasses(mappingObjects.get(0), mappingObjects.get(1), property);
        return mappingObjects;
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
    
    public void setReportPath(GroupObjectEntity groupObject, PropertyObjectEntity property) {
        if (groupObject != null)
            setSubReport(groupObject, property);
        else
            setReportPath(property);
    }

    public void setIntegrationSID(String sID) {
        form.setIntegrationSID(sID);
    }

    public void setReportPath(PropertyObjectEntity property) {
        form.reportPathProp = property;
    }

    public void setSubReport(GroupObjectEntity groupObject, PropertyObjectEntity property) {
        groupObject.isSubReport = true;
        groupObject.reportPathProp = property;
    }
    
    private CustomClass getSingleAddClass(ScriptingLogicsModule.NamedPropertyUsage propertyUsage) throws ScriptingErrorLog.SemanticErrorException {
        List<ValueClass> valueClasses = LM.getValueClasses(propertyUsage);
        if(valueClasses != null) {
            ValueClass valueClass = BaseUtils.single(valueClasses);
            checkCustomClassParam(valueClass, propertyUsage.name);
            return (CustomClass) valueClass;
        }
        return null; 
    }

    public void addScriptedPropertyDraws(List<? extends ScriptingLogicsModule.AbstractFormActionOrPropertyUsage> properties, List<String> aliases, List<LocalizedString> captions, FormPropertyOptions commonOptions, List<FormPropertyOptions> options, Version version, List<DebugInfo.DebugPoint> points) throws ScriptingErrorLog.SemanticErrorException {
        boolean reverse = commonOptions.getInsertType() == FIRST || commonOptions.getNeighbourPropertyDraw() != null && commonOptions.getInsertType() == AFTER;
        
        for (int i = reverse ? properties.size() - 1 : 0; (reverse ? i >= 0 : i < properties.size()); i = reverse ? i - 1 : i + 1) {
            ScriptingLogicsModule.AbstractFormActionOrPropertyUsage pDrawUsage = properties.get(i);
            String alias = aliases.get(i);
            LocalizedString caption = captions.get(i);

            FormPropertyOptions propertyOptions = commonOptions.overrideWith(options.get(i));

            FormSessionScope scope = propertyOptions.getFormSessionScope();
            
            LAP<?, ?> property = null;
            ImOrderSet<ObjectEntity> objects = null;
            String forceIntegrationSID = null;
            ActionObjectSelector forceChangeAction = null;
            if(pDrawUsage instanceof ScriptingLogicsModule.FormPredefinedUsage) {
                ScriptingLogicsModule.FormPredefinedUsage prefefUsage = (ScriptingLogicsModule.FormPredefinedUsage) pDrawUsage;
                ScriptingLogicsModule.NamedPropertyUsage pUsage = prefefUsage.property;
                List<String> mapping = prefefUsage.mapping;
                String propertyName = pUsage.name;
                if (propertyName.equals("VALUE")) {
                    ObjectEntity obj = getSingleMappingObject(mapping);
                    Pair<LP, ActionObjectSelector> valueProp = LM.getObjValueProp(form, obj);
                    property = valueProp.first;
                    forceChangeAction = valueProp.second;
                    objects = SetFact.singletonOrder(obj);
                } else if (propertyName.equals("NEW") && nvl(scope, PropertyDrawEntity.DEFAULT_ACTION_EVENTSCOPE) == OLDSESSION) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    CustomClass explicitClass = getSingleAddClass(pUsage);
                    property = LM.getAddObjectAction(form, obj, explicitClass);
                    objects = SetFact.EMPTYORDER();
                    forceIntegrationSID = propertyName;
                } else if (propertyName.equals("NEWEDIT") || propertyName.equals("NEW")) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    CustomClass explicitClass = getSingleAddClass(pUsage);
                    property = LM.getAddFormAction(form, obj, explicitClass);
                    objects = SetFact.EMPTYORDER();
                } else if (propertyName.equals("EDIT")) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    CustomClass explicitClass = getSingleAddClass(pUsage);
                    property = LM.getEditFormAction(obj, explicitClass);
                    objects = SetFact.singletonOrder(obj);
                } else if (propertyName.equals("DELETE")) {
                    ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                    property = LM.getDeleteAction(obj);
                    objects = SetFact.singletonOrder(obj);
                    forceIntegrationSID = propertyName;
                } else if (propertyName.equals("INTERVAL")) {
                    objects = getTwinTimeSeriesMappingObject(propertyName, mapping);
                    Iterator<ObjectEntity> iterator = objects.iterator();
                    ObjectEntity objectFrom = iterator.next();
                    ObjectEntity objectTo = iterator.next();
                    TimeSeriesClass timeClass = (TimeSeriesClass) objectFrom.baseClass;
                    LP<?>[] intProps = LM.findProperties(timeClass.getIntervalProperty(), timeClass.getFromIntervalProperty(), timeClass.getToIntervalProperty());
                    Pair<LP, ActionObjectSelector> intervalProp = LM.getObjIntervalProp(form, objectFrom, objectTo, intProps[0], intProps[1], intProps[2]);
                    property = intervalProp.first;
                    forceChangeAction = intervalProp.second;
                }
            }
            Result<Pair<ActionOrProperty, String>> inherited = new Result<>();
            if(property == null) {
                MappedActionOrProperty prop = LM.getPropertyWithMapping(form, pDrawUsage, inherited);
                checkPropertyParameters(prop.property, prop.mapping);
                property = prop.property;
                objects = prop.mapping;

                if (alias != null && pDrawUsage instanceof ScriptingLogicsModule.FormLAPUsage) {
                    LM.makeActionOrPropertyPublic(form, alias, ((ScriptingLogicsModule.FormLAPUsage) pDrawUsage));
                }
            }

            DebugInfo.DebugPoint debugPoint = points.get(i);
            String formPath = debugPoint.toString();
            PropertyDrawEntity propertyDraw;
            ActionOrPropertyObjectEntity propertyObject = property.createObjectEntity(objects);
            if(inherited.result != null)
                propertyDraw = form.addPropertyDraw(propertyObject, formPath, inherited.result.second, inherited.result.first, propertyOptions.getInsertType() == FIRST, version);
            else
                propertyDraw = form.addPropertyDraw(propertyObject, formPath, property.listInterfaces, propertyOptions.getInsertType() == FIRST, version);
            propertyDraw.setScriptIndex(Pair.create(debugPoint.line, debugPoint.offset));

            if(forceChangeAction != null)
                propertyDraw.setEventAction(ServerResponse.CHANGE, forceChangeAction);

            // temporary check
            if(nvl(scope, PropertyDrawEntity.DEFAULT_ACTION_EVENTSCOPE) != OLDSESSION && !(pDrawUsage instanceof ScriptingLogicsModule.FormPredefinedUsage) && (propertyOptions.getEventActions() == null || !propertyOptions.getEventActions().containsKey(ServerResponse.CHANGE)))
                ServerLoggers.startLogger.info("WARNING! Now default change event action will work in new session " + propertyDraw);

            propertyDraw.defaultChangeEventScope = scope;

            if(forceIntegrationSID != null) // for NEW, DELETE will set integration SID for js integration
                propertyDraw.setIntegrationSID(forceIntegrationSID);

            try {
                form.setFinalPropertyDrawSID(propertyDraw, alias);
            } catch (FormEntity.AlreadyDefined alreadyDefined) {
                LM.throwAlreadyDefinePropertyDraw(alreadyDefined);
            }

            applyPropertyOptions(propertyDraw, propertyOptions, version);

            // Добавляем PropertyDrawView в FormView, если он уже был создан
            PropertyDrawView view = form.addPropertyDrawView(propertyDraw, propertyOptions.getInsertType() == FIRST, version);
            if(view != null)
                view.caption = caption;
            else
                propertyDraw.initCaption = caption; 

            movePropertyDraw(propertyDraw, propertyOptions, version);
        }
    }

    private void checkParamCount(int size, int expectedSize) throws ScriptingErrorLog.SemanticErrorException {
        if (size != expectedSize)
            LM.getErrLog().emitParamCountError(LM.getParser(), expectedSize, size);
    }

    private void checkCustomClassParam(ObjectEntity param, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        checkCustomClassParam(param.baseClass, propertyName);
    }

    private void checkTimeSeriesParam(ObjectEntity param, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        checkTimeSeriesParam(param.baseClass, propertyName);
    }

    private void checkEqualParamClasses(ObjectEntity param1, ObjectEntity param2, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        checkEqualParamClasses(param1.baseClass, param2.baseClass, propertyName);
    }

    private void checkCustomClassParam(ValueClass cls, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            LM.getErrLog().emitCustomClassExpectedError(LM.getParser(), propertyName);
        }
    }

    private void checkTimeSeriesParam(ValueClass cls, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof TimeSeriesClass)) {
            LM.getErrLog().emitTimeSeriesExpectedError(LM.getParser(), propertyName);
        }
    }

    private void checkEqualParamClasses(ValueClass cls1, ValueClass cls2, String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(BaseUtils.hashEquals(cls1, cls2))) {
            LM.getErrLog().emitEqualParamClassesExpectedError(LM.getParser(), propertyName);
        }
    }

    private <P extends PropertyInterface> void checkPropertyParameters(LAP<P, ?> property, ImOrderSet<ObjectEntity> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ImMap<P, AndClassSet> map = property.listInterfaces.mapList(mapping).mapValues(ObjectEntity::getAndClassSet);

        if (!property.getActionOrProperty().isInInterface(map, true)) {
            LM.getErrLog().emitWrongPropertyParametersError(LM.getParser(), property.getActionOrProperty().getName());
        }
    }

    public void applyPropertyOptions(PropertyDrawEntity<?> property, FormPropertyOptions options, Version version) throws ScriptingErrorLog.SemanticErrorException {
        FormPropertyOptions.Columns columns = options.getColumns();
        if (columns != null) {
            property.setColumnGroupObjects(columns.columnsName, SetFact.fromJavaOrderSet(columns.columns));
        }

        property.setPropertyExtra(options.getHeader(), PropertyDrawExtraType.CAPTION);
        property.setPropertyExtra(options.getFooter(), PropertyDrawExtraType.FOOTER);

        property.setPropertyExtra(options.getShowIf(), PropertyDrawExtraType.SHOWIF);

        property.formula = options.formula;
        property.formulaOperands = options.formulaOperands;
        property.aggrFunc = options.aggrFunc;
        property.lastAggrColumns = nvl(options.lastAggrColumns, ListFact.EMPTY());
        property.lastAggrDesc = nvl(options.lastAggrDesc, false);

        property.quickFilterProperty = options.getQuickFilterPropertyDraw();

        PropertyObjectEntity backgroundProperty = options.getBackground();
        if (backgroundProperty != null && !((PropertyObjectEntity<?>)backgroundProperty).property.getType().equals(ColorClass.instance)) {
            property.setPropertyExtra(addGroundPropertyObject(backgroundProperty, true), PropertyDrawExtraType.BACKGROUND);
        } else {
            property.setPropertyExtra(backgroundProperty, PropertyDrawExtraType.BACKGROUND);
        }

        PropertyObjectEntity foregroundProperty = options.getForeground();
        if (foregroundProperty != null && !((PropertyObjectEntity<?>)foregroundProperty).property.getType().equals(ColorClass.instance)) {
            property.setPropertyExtra(addGroundPropertyObject(foregroundProperty, false), PropertyDrawExtraType.FOREGROUND);
        } else {
            property.setPropertyExtra(foregroundProperty, PropertyDrawExtraType.FOREGROUND);
        }

        PropertyObjectEntity imageProperty = options.getImage();
        if (imageProperty != null && ((PropertyObjectEntity<?>) imageProperty).property.getType().equals(ImageClass.get())) {
            property.setPropertyExtra(imageProperty, PropertyDrawExtraType.IMAGE);
            property.hasDynamicImage = true;
        }

        property.setPropertyExtra(options.getReadOnlyIf(), PropertyDrawExtraType.READONLYIF);
        if (options.getViewType() != null) {
            property.viewType = options.getViewType();
        }
        String customRenderFunction = options.getCustomRenderFunction();
        if (customRenderFunction != null) {
            if (!customRenderFunction.isEmpty()) {
                property.customRenderFunction = customRenderFunction;
            } else {
                LM.getErrLog().emitCustomPropertyViewFunctionError(LM.getParser(), property.getSID(), customRenderFunction, true);
            }
        }
        String customEditorFunction = options.getCustomEditorFunction();
        if (customEditorFunction != null) {
            if (!customEditorFunction.isEmpty()) {
                property.customEditorFunction = customEditorFunction;
            } else {
                LM.getErrLog().emitCustomPropertyViewFunctionError(LM.getParser(), property.getSID(), customRenderFunction, false);
            }
        }

        if (options.getToDraw() != null) {
            property.toDraw = options.getToDraw();
        }

        Boolean hintNoUpdate = options.getHintNoUpdate();
        if (hintNoUpdate != null && hintNoUpdate) {
            form.addHintsNoUpdate(property.getValueProperty().property, version);
        }
        
        Boolean hintTable = options.getHintTable();
        if (hintTable != null && hintTable) {
            form.addHintsIncrementTable(version, property.getValueProperty().property);
        }

        Boolean optimisticAsync = options.getOptimisticAsync();
        if (optimisticAsync != null && optimisticAsync) {
            property.optimisticAsync = true;
        }

        Boolean isSelector = options.getSelector();
        if(isSelector != null && isSelector)
            property.setEventAction(ServerResponse.CHANGE, property::getSelectorAction);

        Map<String, ActionObjectEntity> eventActions = options.getEventActions();
        if (eventActions != null) {
            for (Map.Entry<String, ActionObjectEntity> e : eventActions.entrySet()) {
                property.setEventAction(e.getKey(), e.getValue());
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

        String eventID = options.getEventId();
        if (eventID != null)
            property.eventID = eventID;

        String integrationSID = options.getIntegrationSID();
        if (integrationSID != null)
            property.setIntegrationSID(integrationSID);
        
        String groupName = options.getGroupName();
        Group group = (groupName == null ? null : LM.findGroup(groupName));
        if(group != null)
            property.group = group;

        Boolean attr = options.getAttr();
        if(attr != null)
            property.attr = attr;

        Boolean extNull = options.getExtNull();
        if(extNull != null)
            property.extNull = extNull;

        Boolean order = options.getOrder();
        if(order != null)
            form.addDefaultOrder(property, order, version);

        Boolean filter = options.getFilter();
        if(filter != null && filter)
            form.addFixedFilter(new FilterEntity(property.getPropertyObjectEntity()), version);

        Boolean pivotColumn = options.getPivotColumn();
        if(pivotColumn != null && pivotColumn)
            form.addPivotColumn(property, version);
        Boolean pivotRow = options.getPivotRow();
        if(pivotRow != null && pivotRow)
            form.addPivotRow(property, version);
        Boolean pivotMeasure = options.getPivotMeasure();
        if(pivotMeasure != null && pivotMeasure)
            form.addPivotMeasure(property, version);

        Boolean sticky = options.getSticky();
        if(sticky != null)
            property.sticky = sticky;
    }

    private void movePropertyDraw(PropertyDrawEntity<?> property, FormPropertyOptions options, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (options.getNeighbourPropertyDraw() != null) {
            if (options.getNeighbourPropertyDraw().getNFToDraw(form, version) != property.getNFToDraw(form, version)) {
                LM.getErrLog().emitNeighbourPropertyError(LM.getParser(), options.getNeighbourPropertyText(), property.getSID());
            }
            form.movePropertyDrawTo(property, options.getNeighbourPropertyDraw(), options.getInsertType() == AFTER, version);
        }
    }

    private <P extends PropertyInterface, C extends PropertyInterface> PropertyObjectEntity addGroundPropertyObject(PropertyObjectEntity<P> groundProperty, boolean back) {
        LP<C> defaultColorProp = back ? LM.baseLM.defaultOverrideBackgroundColor : LM.baseLM.defaultOverrideForegroundColor;
        PropertyMapImplement<P, P> groupImplement = groundProperty.property.getImplement();
        PropertyMapImplement<?, P> mapImpl = PropertyFact.createAnd(groundProperty.property.interfaces,
                new PropertyMapImplement<>(defaultColorProp.property, MapFact.<C, P>EMPTYREV()), groupImplement);
        return new PropertyObjectEntity(
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

    public void addScriptedFilters(List<LP> properties, List<ImOrderSet<String>> mappings, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == mappings.size();
        for (int i = 0; i < properties.size(); i++) {
            LP property = properties.get(i);
            ImOrderSet<ObjectEntity> mappingObjects = getMappingObjects(mappings.get(i));
            checkPropertyParameters(property, mappingObjects);

            form.addFixedFilter(new FilterEntity(form.addPropertyObject(property, mappingObjects), true), version);
        }
    }

    public void addScriptedHints(boolean isHintNoUpdate, List<ScriptingLogicsModule.NamedPropertyUsage> propUsages, Version version) throws ScriptingErrorLog.SemanticErrorException {
        LP[] properties = new LP[propUsages.size()];
        for (int i = 0; i < propUsages.size(); i++) {
            properties[i] = LM.findLPByPropertyUsage(propUsages.get(i));
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
                LM.getErrLog().emitWrongKeyStrokeFormatError(LM.getParser(), info.keystroke);
            }

            ImOrderSet<String> mapping = info.mapping;
            LP<?> property = info.property;

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

    public void addScriptedFormEvents(List<ActionObjectEntity> actions, List<Object> types, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert actions.size() == types.size();
        for (int i = 0; i < actions.size(); i++) {
            Object eventType = types.get(i);
            if (eventType instanceof String) {
                form.addActionsOnEvent(getObjectEntity((String) eventType), version, actions.get(i));
            } else {
                ActionObjectEntity action = actions.get(i);
                form.addActionsOnEvent(eventType, eventType == FormEventType.QUERYCLOSE, version, action);
            }
        }
    }

    public PropertyObjectEntity addPropertyObject(ScriptingLogicsModule.AbstractFormPropertyUsage property) throws ScriptingErrorLog.SemanticErrorException {
        return addPropertyObject(LM, form, property);
    }

    public static PropertyObjectEntity addPropertyObject(ScriptingLogicsModule LM, FormEntity form, ScriptingLogicsModule.AbstractFormPropertyUsage property) throws ScriptingErrorLog.SemanticErrorException {
        MappedActionOrProperty prop = LM.getPropertyWithMapping(form, property, null);
        return form.addPropertyObject((LP)prop.property, prop.mapping);
    }

    public ActionObjectEntity addActionObject(ScriptingLogicsModule.AbstractFormActionUsage property) throws ScriptingErrorLog.SemanticErrorException {
        MappedActionOrProperty prop = LM.getPropertyWithMapping(form, property, null);
        return form.addPropertyObject((LA)prop.property, prop.mapping);
    }
    
    public void addScriptedUserFilters(List<PropertyDrawEntity> properties, Version version) {
        for (PropertyDrawEntity property : properties) {
            form.addUserFilter(property, version);
        }
    }

    public void addScriptedDefaultOrder(List<PropertyDrawEntity> properties, List<Boolean> orders, boolean first, Version version) {
        if(first) {
            for (int i = properties.size() - 1; i >= 0; --i) {
                form.addDefaultOrderFirst(properties.get(i), orders.get(i), version);
                form.addDefaultOrderView(properties.get(i), orders.get(i), version);
            }
        } else {
            for (int i = 0; i < properties.size(); ++i) {
                form.addDefaultOrder(properties.get(i), orders.get(i), version);
                form.addDefaultOrderView(properties.get(i), orders.get(i), version);
            }
        }
    }

    public void addPivotOptions(List<Pair<String, PivotOptions>> pivotOptionsList, List<List<PropertyDrawEntity>> pivotColumns,
                                List<List<PropertyDrawEntity>> pivotRows, List<PropertyDrawEntity> pivotMeasures, Version version) {
        for(Pair<String, PivotOptions> entry : pivotOptionsList) {
            GroupObjectEntity groupObject = form.getNFGroupObject(entry.first, version);
            if (groupObject != null) {
                groupObject.setPivotOptions(entry.second);
            }
        }
        form.addPivotColumns(pivotColumns, version);
        form.addPivotRows(pivotRows, version);
        form.addPivotMeasures(pivotMeasures, version);
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

    public void setLocalAsync(boolean localAsync) {
        form.localAsync = localAsync;
    }

    private CustomClass findCustomClassForFormSetup(String className) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass valueClass = LM.findClass(className);
        if (!(valueClass instanceof CustomClass)) {
            LM.getErrLog().emitBuiltInClassFormSetupError(LM.getParser(), className);
        }

        return (CustomClass) valueClass;
    }

    public static List<ScriptingLogicsModule.TypedParameter> getTypedObjectsNames(ScriptingLogicsModule LM, FormEntity form, Version version) {
        List<ValueClass> classes = new ArrayList<>();
        List<String> objNames = form.getNFObjectsNamesAndClasses(classes, version);
        List<ScriptingLogicsModule.TypedParameter> typedObjects = new ArrayList<>();
        for (int i = 0; i < classes.size(); ++i) {
            typedObjects.add(LM.new TypedParameter(classes.get(i), objNames.get(i)));
        }
        return typedObjects;
    }
    public List<ScriptingLogicsModule.TypedParameter> getTypedObjectsNames(Version version) {
        return getTypedObjectsNames(LM, form, version);
    }
    
    public static class RegularFilterInfo {
        LocalizedString caption;
        String keystroke;
        LP property;
        ImOrderSet<String> mapping;
        boolean isDefault;

        public RegularFilterInfo(LocalizedString caption, String keystroke, LP property, ImOrderSet<String> mapping, boolean isDefault) {
            this.caption = caption;
            this.keystroke = keystroke;
            this.property = property;
            this.mapping = mapping;
            this.isDefault = isDefault;
        }
    }
}
