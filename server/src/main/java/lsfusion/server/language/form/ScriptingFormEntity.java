package lsfusion.server.language.form;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.language.form.object.ScriptingObject;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.event.FormChangeEvent;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NeighbourComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.converters.KeyStrokeConverter;
import lsfusion.server.language.form.object.ScriptingGroupObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.language.property.oraction.MappedActionOrProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ColorClass;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.time.TimeSeriesClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.FormEventType;
import lsfusion.server.logics.form.interactive.event.FormServerEvent;
import lsfusion.server.logics.form.interactive.event.ObjectEventObject;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.IdentityEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.*;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.logics.form.interactive.action.edit.FormSessionScope.OLDSESSION;

public class ScriptingFormEntity {
    private ScriptingLogicsModule LM;
    private FormEntity form;
    public String code;

    public ScriptingFormEntity(ScriptingLogicsModule LM, FormEntity form) {
        assert form != null && LM != null;
        this.LM = LM;
        this.form = form;
    }

    public FormEntity getForm() {
        return form;
    }

    public void addScriptingForms(List<String> forms, boolean extend, Version version) throws ScriptingErrorLog.SemanticErrorException {
        for (String f : forms) {
            FormEntity addForm = LM.findForm(f);
            FormView addFormView = addForm.view;

            ObjectMapping mapping = new ObjectMapping(form, extend ? ObjectMapping.getImplicitAdd(version, addForm, form) : MapFact.EMPTYREV(), extend, version);

            if(addFormView != null) {
                FormView formView = mapping.get(addFormView);
                if (formView instanceof DefaultFormView && !extend) {
                    ((DefaultFormView) formView).addForm(addFormView, mapping, version);
                }
            } else
                mapping.get(addForm);
        }
    }


    @NotNull
    public ObjectEntity addScriptingObject(boolean extend, String objectName, String className, Version version) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity object;
        if(extend) {
            object = form.getNFObject(objectName, version);
            if(object == null)
                LM.getErrLog().emitGroupObjectNotFoundError(LM.getParser(), objectName);
        } else {
            ValueClass cls = LM.findClass(className);
            object = new ObjectEntity(form.genID, objectName, cls, false);
            form.addObject(object, version);
        }
        return object;
    }

    public GroupObjectEntity addScriptingGroupObject(boolean extend, String groupName, List<ObjectEntity> lobjects, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        int size = lobjects.size();
        if (groupName == null) {
            groupName = "";
            for (int j = 0; j < size; j++) {
                groupName = (groupName.isEmpty() ? "" : groupName + ".") + lobjects.get(j).getSID();
            }
        }

        GroupObjectEntity groupObj;
        if(extend) {
            groupObj = form.getNFGroupObject(groupName, version);
            if(groupObj == null) {
                if(size == 1) { // looking for object
                    ObjectEntity object = form.getNFObject(groupName, version);
                    if(object != null)
                        groupObj = object.groupTo;
                }
                if(groupObj == null)
                    LM.getErrLog().emitGroupObjectNotFoundError(LM.getParser(), groupName);
            }
        } else {
            ImOrderSet<ObjectEntity> objects = SetFact.fromJavaOrderSet(lobjects);

            groupObj = new GroupObjectEntity(form.genID, groupName, objects, LM.baseLM, debugPoint);
            form.addGroupObject(groupObj, version);

            checkAlreadyDefined(groupObj, version);
        }

        return groupObj;
    }

    public void applyGroupObjectOptions(GroupObjectEntity groupObj, ScriptingGroupObject groupObject, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (groupObject.viewType != null)
            groupObj.setViewType(groupObject.viewType);
        if (groupObject.listViewType != null)
            groupObj.setListViewType(groupObject.listViewType);
        if(groupObject.pivotOptions != null)
            groupObj.setPivotOptions(groupObject.pivotOptions);
        if (groupObject.customRenderFunction != null)
            groupObj.setCustomRenderFunction(groupObject.customRenderFunction);
        if(groupObject.customOptions != null)
            groupObj.setCustomOptions(addPropertyObject(groupObject.customOptions));
        if (groupObject.mapTileProvider != null) {
            checkMapTileProvider(groupObject.mapTileProvider);
            groupObj.setMapTileProvider(groupObject.mapTileProvider);
        }

        if (groupObject.pageSize != null) {
            groupObj.pageSize = groupObject.pageSize;
        }

        if (groupObject.updateType != null) {
            groupObj.updateType = groupObject.updateType;
        }

        String propertyGroupName = groupObject.propertyGroupName;
        Group propertyGroup = (propertyGroupName == null ? null : LM.findGroup(propertyGroupName));
        if(propertyGroup != null)
            groupObj.setPropertyGroup(propertyGroup, version);

        String integrationSID = groupObject.integrationSID;
        if(integrationSID != null)
            groupObj.setIntegrationSID(integrationSID, version);

        groupObj.setIntegrationKey(groupObject.integrationKey, version);

        if(groupObject.isSubReport)
            setSubReport(groupObj, groupObject.subReportPath, version);

        if(groupObject.background != null)
            groupObj.setPropertyBackground(addPropertyObject(groupObject.background));
        if(groupObject.foreground != null)
            groupObj.setPropertyForeground(addPropertyObject(groupObject.foreground));

        if(groupObject.location != null) {
            form.moveGroupObject(groupObj, groupObject.location, version);

            checkNeighbour(groupObject.location);
        }
    }

    public void applyObjectOptions(ObjectEntity obj, ScriptingObject object, Version version) {
        if (object.event != null) {
            form.addActionsOnEvent(new ObjectEventObject(obj.getSID()), version, object.event);
        }

        if (object.integrationSID != null) {
            obj.setIntegrationSID(object.integrationSID);
        }

        obj.setCaption(object.caption);
    }

    private static List<String> supportedMapTileProviders = Arrays.asList("openStreetMap", "google", "yandex");
    private void checkMapTileProvider(String mapTileProvider) throws ScriptingErrorLog.SemanticErrorException {
        if(!supportedMapTileProviders.contains(mapTileProvider)) {
            LM.getErrLog().emitMapTileProviderNotSupportedError(LM.getParser(), mapTileProvider, supportedMapTileProviders);
        }
    }

    public TreeGroupEntity addScriptingTreeGroupObject(boolean extend, String treeSID, List<GroupObjectEntity> groupObjects, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        TreeGroupEntity treeGroup = null;

        if(extend) {
            if(treeSID != null) {
                treeGroup = form.getNFTreeGroupObject(treeSID, version, null);
            }
            if(treeGroup == null) {
                LM.getErrLog().emitTreeGroupObjectNotFoundError(LM.getParser(), treeSID);
            }
        } else {
            treeGroup = new TreeGroupEntity(form.genID, treeSID, SetFact.fromJavaOrderSet(groupObjects), debugPoint);
            form.addTreeGroupObject(treeGroup, version);

            checkAlreadyDefined(treeGroup, version);
        }

        return treeGroup;
    }

    public void applyTreeGroupObjectOptions(TreeGroupEntity treeGroup, ComplexLocation<GroupObjectEntity> location, List<List<LP>> parentProperties, List<List<ImOrderSet<String>>> propertyMappings, Version version) throws ScriptingErrorLog.SemanticErrorException {
        ImOrderSet<GroupObjectEntity> groups = treeGroup.getGroups();
        for (int j = 0; j < groups.size(); j++) {
            GroupObjectEntity groupObj = groups.get(j);

            List<LP> properties = parentProperties.get(j);
            List<ImOrderSet<String>> propertyMapping = propertyMappings.get(j);

            if (properties != null && groupObj.getObjects().size() != properties.size()) {
                LM.getErrLog().emitDifferentObjsNPropsQuantityError(LM.getParser(), groupObj.getObjects().size());
            }
            if (properties != null) {

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

        if(location != null) {
            form.moveTreeGroupObject(treeGroup, location, version);

            checkNeighbour(location);
        }
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
    
    public void setReportPath(GroupObjectEntity groupObject, PropertyObjectEntity property, Version version) {
        if (groupObject != null)
            setSubReport(groupObject, property, version);
        else
            setReportPath(property, version);
    }

    public void setIntegrationSID(String sID) {
        form.setIntegrationSID(sID);
    }

    public void setReportPath(PropertyObjectEntity property, Version version) {
        form.reportPathProp.set(property, version);
    }

    public void setSubReport(GroupObjectEntity groupObject, PropertyObjectEntity property, Version version) {
        groupObject.setIsSubReport(true, version);
        groupObject.setReportPathProp(property, version);
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

    public void addScriptingPropertyDraws(boolean extend, List<? extends ScriptingLogicsModule.AbstractFormActionOrPropertyUsage> properties, List<String> aliases, List<LocalizedString> captions, FormPropertyOptions commonOptions, List<FormPropertyOptions> options, Version version, List<DebugInfo.DebugPoint> points) throws ScriptingErrorLog.SemanticErrorException {
        ComplexLocation<PropertyDrawEntity> commonLocation = commonOptions.getLocation();
        boolean reverseList = commonLocation != null && commonLocation.isReverseList();
        
        for (int i = reverseList ? properties.size() - 1 : 0; (reverseList ? i >= 0 : i < properties.size()); i = reverseList ? i - 1 : i + 1) {
            ScriptingLogicsModule.AbstractFormActionOrPropertyUsage pDrawUsage = properties.get(i);
            String alias = aliases.get(i);
            DebugInfo.DebugPoint debugPoint = points.get(i);

            PropertyDrawEntity propertyDraw = addScriptingPropertyDraw(extend, alias, pDrawUsage, commonOptions, version, debugPoint);

            FormPropertyOptions propertyOptions = commonOptions.overrideWith(options.get(i));
            applyPropertyOptions(propertyDraw, propertyOptions, captions.get(i), version);
        }
    }

    private PropertyDrawEntity addScriptingPropertyDraw(boolean extend, String alias, ScriptingLogicsModule.AbstractFormActionOrPropertyUsage pDrawUsage, FormPropertyOptions commonOptions, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        Result<Pair<ActionOrProperty, List<String>>> inherited = new Result<>();
        LAP<?, ?> property;
        ImOrderSet<ObjectEntity> objects;
        if(pDrawUsage instanceof ScriptingLogicsModule.FormPredefinedUsage) {
            ScriptingLogicsModule.FormPredefinedUsage prefefUsage = (ScriptingLogicsModule.FormPredefinedUsage) pDrawUsage;
            ScriptingLogicsModule.NamedPropertyUsage pUsage = prefefUsage.property;
            List<String> mapping = prefefUsage.mapping;
            String propertyName = pUsage.name;
            if (propertyName.equals("VALUE")) {
                ObjectEntity obj = getSingleMappingObject(mapping);
                property = LM.getObjValueProp(form, obj);
                objects = SetFact.singletonOrder(obj);
            } else if (propertyName.equals("NEW") && nvl(commonOptions.getFormSessionScope(), PropertyDrawEntity.DEFAULT_ACTION_EVENTSCOPE) == OLDSESSION) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                property = LM.getAddObjectAction(form, obj, getSingleAddClass(pUsage));
                objects = SetFact.EMPTYORDER();
            } else if (propertyName.equals("NEWEDIT") || propertyName.equals("NEW")) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                property = LM.getAddFormAction(form, obj, getSingleAddClass(pUsage));
                objects = SetFact.EMPTYORDER();
            } else if (propertyName.equals("EDIT")) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                property = LM.getEditFormAction(obj, getSingleAddClass(pUsage));
                objects = SetFact.singletonOrder(obj);
            } else if (propertyName.equals("DELETE")) {
                ObjectEntity obj = getSingleCustomClassMappingObject(propertyName, mapping);
                property = LM.getDeleteAction(obj);
                objects = SetFact.singletonOrder(obj);
            } else if (propertyName.equals("INTERVAL")) {
                objects = getTwinTimeSeriesMappingObject(propertyName, mapping);
                Iterator<ObjectEntity> iterator = objects.iterator();
                ObjectEntity objectFrom = iterator.next();
                ObjectEntity objectTo = iterator.next();
                TimeSeriesClass timeClass = (TimeSeriesClass) objectFrom.baseClass;
                LP<?>[] intProps = LM.findProperties(timeClass.getIntervalProperty(), timeClass.getFromIntervalProperty(), timeClass.getToIntervalProperty());
                property = LM.getObjIntervalProp(form, objectFrom, objectTo, intProps[0], intProps[1], intProps[2]);
            } else
                throw new UnsupportedOperationException();
        } else {
            MappedActionOrProperty prop = LM.getPropertyWithMapping(form, pDrawUsage, inherited);
            checkPropertyParameters(prop.property, prop.mapping);
            property = prop.property;
            objects = prop.mapping;

            if (alias != null && pDrawUsage instanceof ScriptingLogicsModule.FormLAPUsage) { // it is named expression
                LM.makeActionOrPropertyPublic(form, alias, ((ScriptingLogicsModule.FormLAPUsage) pDrawUsage));
            }
        }
        ActionOrPropertyObjectEntity actionOrPropertyEntity = property.createObjectEntity(objects);

        PropertyDrawEntity propertyDraw = form.addPropertyDraw(actionOrPropertyEntity, property.listInterfaces, inherited.result,
                extend, version, debugPoint, alias); // with location it is sort of optimization not to add and then move
        if(!extend)
            checkAlreadyDefined(propertyDraw, version);
        return propertyDraw;
    }

    private Version getCheckVersion(Version version) {
        return version;
    }

    private void checkAlreadyDefined(PropertyDrawEntity propertyDraw, Version version) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity definedEntity;
        if ((definedEntity = form.getNFPropertyDraw(propertyDraw.getSID(), getCheckVersion(version), propertyDraw)) != null) {
            throwAlreadyDefinedError("property", definedEntity);
        }
    }

    private void throwAlreadyDefinedError(String type, IdentityEntity entity) throws ScriptingErrorLog.SemanticErrorException {
        LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), type, form.getCanonicalName(), entity.getSID(), entity.getFormPath());
    }

    private void checkAlreadyDefined(GroupObjectEntity groupObject, Version version) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity definedEntity;
        if ((definedEntity = form.getNFGroupObject(groupObject.getSID(), getCheckVersion(version), groupObject)) != null)
            throwAlreadyDefinedError("group object", definedEntity);

        ImOrderSet<ObjectEntity> objects = groupObject.getOrderObjects();
        for (int j = 0, size = objects.size(); j < size; j++)
            checkAlreadyDefined(objects.get(j), version);
    }

    private void checkAlreadyDefined(ObjectEntity object, Version version) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity definedEntity;
        if ((definedEntity = form.getNFObject(object.getSID(), getCheckVersion(version), object)) != null)
            throwAlreadyDefinedError("object", definedEntity);
    }

    private void checkAlreadyDefined(TreeGroupEntity tree, Version version) throws ScriptingErrorLog.SemanticErrorException {
        TreeGroupEntity definedEntity;
        if ((definedEntity = form.getNFTreeGroupObject(tree.getSID(), getCheckVersion(version), tree)) != null)
            throwAlreadyDefinedError("tree", definedEntity);
    }

    // containers neighbours are checked in the addOrMoveComponent
    private void checkNeighbour(PropertyDrawEntity<?, ?> propertyDraw, ComplexLocation<PropertyDrawEntity> location, String neighbourText, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if(location instanceof NeighbourComplexLocation) {
            NeighbourComplexLocation<PropertyDrawEntity> neighbourLocation = (NeighbourComplexLocation<PropertyDrawEntity>) location;

            PropertyDrawEntity neighbour = neighbourLocation.element;
            if (neighbour.getNFToDraw(form, version) != propertyDraw.getNFToDraw(form, version)) {
                LM.getErrLog().emitNeighbourPropertyError(LM.getParser(), neighbourText, propertyDraw.getSID());
            }
        }
    }

    public void checkNeighbour(ComplexLocation<GroupObjectEntity> location) throws ScriptingErrorLog.SemanticErrorException {
        if(location instanceof NeighbourComplexLocation) {
            NeighbourComplexLocation<GroupObjectEntity> neighbourLocation = (NeighbourComplexLocation<GroupObjectEntity>) location;

            GroupObjectEntity neighbour = neighbourLocation.element;
            if (neighbour.isInTree()) {
                if (neighbourLocation.isAfter) {
                    if (!neighbour.equals(neighbour.treeGroup.getGroups().last()))
                        LM.getErrLog().emitGroupObjectInTreeAfterNotLastError(LM.getParser(), neighbour.getSID());
                } else {
                    if (!neighbour.equals(neighbour.treeGroup.getGroups().get(0)))
                        LM.getErrLog().emitGroupObjectInTreeBeforeNotFirstError(LM.getParser(), neighbour.getSID());
                }
            }
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

    public void applyPropertyOptions(PropertyDrawEntity<?, ?> property, FormPropertyOptions options, LocalizedString caption, Version version) throws ScriptingErrorLog.SemanticErrorException {
        property.setCaption(caption, version);
        String appImage = options.getAppImage();
        if(appImage != null)
            property.setImage(appImage, version);

        FormPropertyOptions.Columns columns = options.getColumns();
        if (columns != null) {
            property.setColumnGroupObjects(columns.columnsName, SetFact.fromJavaOrderSet(columns.columns), form, version);
        }

        property.setPropertyExtra(options.getHeader(), PropertyDrawExtraType.CAPTION, version);
        property.setPropertyExtra(options.getFooter(), PropertyDrawExtraType.FOOTER, version);

        property.setPropertyExtra(options.getShowIf(), PropertyDrawExtraType.SHOWIF, version);

        property.setFormula(options.formula, version);
        property.setFormulaOperands(options.formulaOperands, version);
        property.setAggrFunc(options.aggrFunc, version);
        property.setLastAggrColumns(nvl(options.lastAggrColumns, ListFact.EMPTY()), version);
        property.setLastAggrDesc(nvl(options.lastAggrDesc, false), version);

        property.setDefaultChangeEventScope(options.getFormSessionScope(), version);

        property.setQuickFilterProperty(options.getQuickFilterPropertyDraw(), version);

        PropertyObjectEntity valueElementClassProperty = options.getValueElementClass();
        if(valueElementClassProperty != null) {
            property.setPropertyExtra(valueElementClassProperty, PropertyDrawExtraType.VALUEELEMENTCLASS, version);
        }

        PropertyObjectEntity backgroundProperty = options.getBackground();
        if (backgroundProperty != null && !((PropertyObjectEntity<?>)backgroundProperty).property.getType().equals(ColorClass.instance)) {
            property.setPropertyExtra(addGroundPropertyObject(backgroundProperty, true), PropertyDrawExtraType.BACKGROUND, version);
        } else {
            property.setPropertyExtra(backgroundProperty, PropertyDrawExtraType.BACKGROUND, version);
        }

        PropertyObjectEntity foregroundProperty = options.getForeground();
        if (foregroundProperty != null && !((PropertyObjectEntity<?>)foregroundProperty).property.getType().equals(ColorClass.instance)) {
            property.setPropertyExtra(addGroundPropertyObject(foregroundProperty, false), PropertyDrawExtraType.FOREGROUND, version);
        } else {
            property.setPropertyExtra(foregroundProperty, PropertyDrawExtraType.FOREGROUND, version);
        }

        PropertyObjectEntity imageProperty = options.getImage();
        if (imageProperty != null) {
            property.setPropertyExtra(imageProperty, PropertyDrawExtraType.IMAGE, version);
        }

        PropertyObjectEntity disableIf = options.getDisableIf();
        PropertyObjectEntity readOnlyIf = options.getReadOnlyIf();
        if(disableIf != null || readOnlyIf != null) {
            property.setPropertyExtra(addReadonlyIfPropertyObject(disableIf, readOnlyIf), PropertyDrawExtraType.READONLYIF, version);
        }
        if (options.getViewType() != null) {
            property.setViewType(options.getViewType(), form, version);
        }
        String customRenderFunction = options.getCustomRenderFunction();
        if (customRenderFunction != null) {
            property.setCustomRenderFunction(customRenderFunction, version);
        }
        String customEditorFunction = options.getCustomEditorFunction();
        if (customEditorFunction != null) {
            property.setCustomChangeFunction(customEditorFunction, version);
        }

        if (options.getToDraw() != null) {
            property.setToDraw(options.getToDraw(), form, version);
        }

        Boolean hintNoUpdate = options.getHintNoUpdate();
        if (hintNoUpdate != null && hintNoUpdate) {
            form.addHintsNoUpdate((Property) property.getInheritedProperty(), version);
        }

        Boolean hintTable = options.getHintTable();
        if (hintTable != null && hintTable) {
            form.addHintsIncrementTable(version, (Property) property.getInheritedProperty());
        }

        Boolean optimisticAsync = options.getOptimisticAsync();
        if (optimisticAsync != null && optimisticAsync) {
            property.setOptimisticAsync(true, version);
        }

        Boolean isSelector = options.getSelector();
        boolean hasSelector = isSelector != null && isSelector;
        if (hasSelector)
            property.setSelectorAction(new PropertyDrawEntity.SelectorAction(property::getSelectorAction), version);

        Map<String, ActionObjectEntity> eventActions = options.getEventActions();
        if (eventActions != null)
            for (Map.Entry<String, ActionObjectEntity> e : eventActions.entrySet())
                property.setEventAction(e.getKey(), e.getValue(), version);

        List<Pair<ActionObjectEntity, Boolean>> formChangeEventActions = options.getFormChangeEventActions();
        if (formChangeEventActions != null) {
            for (Pair<ActionObjectEntity, Boolean> entry : formChangeEventActions) {
                form.addActionsOnEvent(new FormChangeEvent(property, entry.second), version, entry.first);
            }
        }

        OrderedMap<String, LocalizedString> contextMenuBindings = options.getContextMenuBindings();
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                property.setContextMenuAction(contextMenuBindings.getKey(i), contextMenuBindings.getValue(i), version);
            }
        }
        
        PropertyEditType editType = options.getEditType();
        if (editType != null)
            property.setEditType(editType, version);

        String eventID = options.getEventId();
        if (eventID != null)
            property.setEventID(eventID, version);

        String integrationSID = options.getIntegrationSID();
        if (integrationSID != null)
            property.setIntegrationSID(integrationSID, version);
        
        String groupName = options.getGroupName();
        Group group = (groupName == null ? null : LM.findGroup(groupName));
        if(group != null)
            property.setGroup(group, form, version);

        Boolean attr = options.getAttr();
        if(attr != null)
            property.setAttr(attr, version);

        Boolean extNull = options.getExtNull();
        if(extNull != null)
            property.setExtNull(extNull, version);

        Boolean descending = options.getDescending();
        if(descending != null)
            form.addDefaultOrder(property, descending, version);

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
            property.setSticky(sticky, version);
        Boolean sync = options.getSync();
        if(sync != null)
            property.setSync(sync, version);

        PropertyObjectEntity propertyCustomOptions = options.getPropertyCustomOptions();
        if(propertyCustomOptions != null && ((PropertyObjectEntity<?>)propertyCustomOptions).property.getType().equals(JSONClass.instance))
            property.setPropertyExtra(propertyCustomOptions, PropertyDrawExtraType.PROPERTY_CUSTOM_OPTIONS, version);

        Boolean filter = options.getFilter();
        if(filter != null && filter)
            form.addUserFilter(property, version);

        ComplexLocation<PropertyDrawEntity> location = options.getLocation();
        if(location != null) {
            form.movePropertyDraw(property, location, version);

            // has to be later than applyPropertyOptions (because it uses getPropertyExtra)
            checkNeighbour((PropertyDrawEntity<?, ?>) property, location, options.getNeighbourPropertyText(), version);
        }
    }

    private <A extends PropertyInterface, B extends PropertyInterface, C extends PropertyInterface> PropertyObjectEntity addReadonlyIfPropertyObject(PropertyObjectEntity<A> disableIf, PropertyObjectEntity<B> readOnlyIf) {

        ImSet<ObjectEntity> allObjects = SetFact.EMPTY();
        if (disableIf != null) {
            allObjects = allObjects.merge(disableIf.mapping.valuesSet());
        }
        if(readOnlyIf != null) {
            allObjects = allObjects.merge(readOnlyIf.mapping.valuesSet());
        }

        ImRevMap<ObjectEntity, C> objectInterfaces = (ImRevMap<ObjectEntity, C>) allObjects.mapRevValues(UnionProperty.genInterface);

        MList<CalcCase<C>> mListCases = ListFact.mList();
        if(disableIf != null) {
            mListCases.add(new CalcCase<>(disableIf.getImplement(objectInterfaces), PropertyFact.createTTrue()));
        }
        if(readOnlyIf != null) {
            mListCases.add(new CalcCase<>(readOnlyIf.getImplement(objectInterfaces), PropertyFact.createTFalse()));
        }

        return PropertyFact.createCaseProperty(objectInterfaces.valuesSet(), false, mListCases.immutableList()).mapEntityObjects(objectInterfaces.reverse());
    }

    private <P extends PropertyInterface, C extends PropertyInterface> PropertyObjectEntity addGroundPropertyObject(PropertyObjectEntity<P> groundProperty, boolean back) {
        LP<C> defaultColorProp = back ? LM.baseLM.defaultOverrideBackgroundColor : LM.baseLM.defaultOverrideForegroundColor;
        return PropertyFact.createAnd(groundProperty.property.interfaces, // default IF property()
                defaultColorProp.getImplement(), groundProperty.property.getImplement()).mapEntityObjects(groundProperty.mapping);
    }

    public PropertyDrawEntity getPropertyDraw(String sid, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return getPropertyDraw(LM, form, sid, version);
    }

    public static PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule LM, FormEntity form, String sid, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return checkPropertyDraw(LM, form.getNFPropertyDraw(sid, version), sid);
    }

    public PropertyDrawEntity getPropertyDraw(String name, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return getPropertyDraw(LM, form, name, mapping, version);
    }

    public static PropertyDrawEntity getPropertyDraw(ScriptingLogicsModule LM, FormEntity form, String name, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return checkPropertyDraw(LM, form.getNFPropertyDraw(name, mapping, version), name);
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
    
    public void addScriptedRegularFilterGroup(String sid, boolean noNull, List<RegularFilterInfo> filters, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (form.getNFRegularFilterGroup(sid, version) != null) {
            LM.getErrLog().emitAlreadyDefinedError(LM.getParser(), "filter group", sid);
        }

        RegularFilterGroupEntity regularFilterGroup = new RegularFilterGroupEntity(form.genID, sid, noNull, version);

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

            InputBindingEvent keyInputEvent = KeyStrokeConverter.parseInputBindingEvent(info.keyEvent, false);
            if (info.keyEvent != null && keyInputEvent != null && keyInputEvent.inputEvent != null && ((KeyInputEvent)keyInputEvent.inputEvent).keyStroke == null) {
                LM.getErrLog().emitWrongKeyStrokeFormatError(LM.getParser(), info.keyEvent);
            }

            InputBindingEvent mouseInputEvent = KeyStrokeConverter.parseInputBindingEvent(info.mouseEvent, true);

            ImOrderSet<ObjectEntity> mappingObjects = getMappingObjects(info.mapping);
            checkPropertyParameters(info.property, mappingObjects);
            RegularFilterEntity filter = new RegularFilterEntity(form.genID, null,
                    new FilterEntity(form.addPropertyObject(info.property, mappingObjects), true),
                    info.caption, keyInputEvent, info.showKey, mouseInputEvent, info.showMouse);
            if (extend) {
                form.addRegularFilter(filterGroup, filter, info.isDefault, version);
            } else {
                filterGroup.addFilter(filter, info.isDefault, version);
            }
        }
    }

    public void addScriptedFormEvents(List<ActionObjectEntity> actions, List<FormServerEvent> types, List<Boolean> replaces, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert actions.size() == types.size();
        for (int i = 0; i < actions.size(); i++) {
            FormServerEvent eventType = types.get(i);
            if (eventType instanceof FormChangeEvent && ((FormChangeEvent) eventType).before == null) {
                PropertyDrawEntity propertyDrawEntity = ((FormChangeEvent) eventType).propertyDrawEntity;
                propertyDrawEntity.setEventAction(ServerResponse.CHANGE, actions.get(i), version);
            } else {
                Boolean replace = replaces.get(i);
                if(replace == null)
                    replace = eventType == FormEventType.QUERYCLOSE || eventType == FormEventType.QUERYOK;
                if(replace)
                    form.removeActionsOnEvent(eventType, version);

                form.addActionsOnEvent(eventType, version, actions.get(i));
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
            }
        } else {
            for (int i = 0; i < properties.size(); ++i) {
                form.addDefaultOrder(properties.get(i), orders.get(i), version);
            }
        }
    }

    public void addPivotOptions(List<Pair<String, PivotOptions>> pivotOptionsList, List<List<PropertyDrawEntityOrPivotColumn>> pivotColumns,
                                List<List<PropertyDrawEntityOrPivotColumn>> pivotRows, List<PropertyDrawEntity> pivotMeasures, Version version) {
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

    public void setLocalAsync(boolean localAsync, Version version) {
        form.setLocalAsync(localAsync, version);
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
        String keyEvent;
        boolean showKey;
        String mouseEvent;
        boolean showMouse;
        LP property;
        ImOrderSet<String> mapping;
        boolean isDefault;

        public RegularFilterInfo(LocalizedString caption, String keyEvent, boolean showKey, String mouseEvent, boolean showMouse,
                                 LP property, ImOrderSet<String> mapping, boolean isDefault) {
            this.caption = caption;
            this.keyEvent = keyEvent;
            this.showKey = showKey;
            this.mouseEvent = mouseEvent;
            this.showMouse = showMouse;
            this.property = property;
            this.mapping = mapping;
            this.isDefault = isDefault;
        }
    }
}
