package lsfusion.server.physics.admin.reflection.controller.manager;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.task.PublicTask;
import lsfusion.server.base.task.TaskRunner;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.utils.time.TimeLogicsModule;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.group.AbstractNode;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.NavigatorAction;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.AlgType;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.integration.service.*;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;

public class ReflectionManager extends LogicsManager implements InitializingBean {

    public static final Logger startLogger = ServerLoggers.startLogger;

    private BusinessLogics businessLogics;

    @Override
    protected BusinessLogics getBusinessLogics() {
        return businessLogics;
    }

    private DBManager dbManager;

    private PublicTask initTask;

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setInitTask(PublicTask initTask) {
        this.initTask = initTask;
    }

    private BaseLogicsModule LM;
    private ReflectionLogicsModule reflectionLM;
    private SystemEventsLogicsModule systemEventsLM;
    private TimeLogicsModule timeLM;

    private String modulesHash;
    private boolean sourceHashChanged;

    public ReflectionManager() {
        super(REFLECTION_ORDER);
    }

    public void afterPropertiesSet() {
        Assert.notNull(initTask, "initTask must be specified");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        this.LM = businessLogics.LM;
        this.reflectionLM = businessLogics.reflectionLM;
        this.systemEventsLM = businessLogics.systemEventsLM;
        this.timeLM = businessLogics.timeLM;
    }
    
    @Override
    protected void onStarted(LifecycleEvent event) {
        try {
            new TaskRunner(businessLogics).runTask(initTask, startLogger);
        } catch (Exception e) {
            throw new RuntimeException("Error starting ReflectionManager: ", e);
        }
    }

    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    private DataSession createSyncSession() throws SQLException {
        DataSession session = createSession();
        if(Settings.get().isStartServerAnyWay())
            session.setNoCancelInTransaction(true);
        return session;
    }

    public void readModulesHash() {
        try(DataSession session = createSession()){
            List<Integer> moduleHashCodes = new ArrayList<>();
            for (LogicsModule module : businessLogics.getLogicModules()) {
                if (module instanceof ScriptingLogicsModule) {
                    moduleHashCodes.add(((ScriptingLogicsModule) module).getCode().hashCode());
                }
            }
            moduleHashCodes.add((SystemProperties.lightStart ? "light" : "full").hashCode());
            modulesHash = Integer.toHexString(moduleHashCodes.hashCode());

            String oldModulesHash = (String) LM.findProperty("hashModules[]").read(session);
            sourceHashChanged = checkModulesHashChanged(oldModulesHash, modulesHash);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean checkModulesHashChanged(String oldHash, String newHash) {
        startLogger.info(String.format("Comparing modulesHash: old %s, new %s", oldHash, newHash));
        return (oldHash == null || newHash == null) || !oldHash.equals(newHash);
    }

    public void writeModulesHash() {
        try(DataSession session = createSession()) {
            startLogger.info("Writing modulesHash " + modulesHash);
            LM.findProperty("hashModules[]").change(modulesHash, session);
            apply(session);
            startLogger.info("Writing modulesHash finished successfully");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean isSourceHashChanged() {
        return sourceHashChanged;
    }

    public void synchronizeNavigatorElements() {
        migrateNavigatorElements();
        synchronizeNavigatorElements(reflectionLM.navigatorFolder, false, reflectionLM.isNavigatorFolder);
        synchronizeNavigatorElements(reflectionLM.navigatorAction, true, reflectionLM.isNavigatorAction);
    }

    private void synchronizeNavigatorElements(ConcreteCustomClass elementCustomClass, boolean actions, LP deleteLP) {
        startLogger.info("synchronizeNavigatorElements collecting data started");
        
        ImportField nameField = new ImportField(reflectionLM.navigatorElementCanonicalNameClass);
        ImportField captionField = new ImportField(reflectionLM.navigatorElementCaptionClass);
        ImportField formCNField = null;
        ImportField actionCNField = null;
        if(actions) {
            formCNField = new ImportField(reflectionLM.formCanonicalNameClass);
            actionCNField = new ImportField(reflectionLM.actionCanonicalNameClass);
        }
        
        ImportKey<?> keyNavigatorElement = new ImportKey(elementCustomClass, reflectionLM.navigatorElementCanonicalName.getMapping(nameField));
        ImportKey<?> keyForm = null;
        ImportKey<?> keyAction = null;
        if(actions) {
            keyForm = new ImportKey(reflectionLM.form, reflectionLM.formByCanonicalName.getMapping(formCNField));
            keyAction = new ImportKey(reflectionLM.action, reflectionLM.actionCanonicalName.getMapping(actionCNField));
        }

        List<List<Object>> elementsData = new ArrayList<>();
        for (NavigatorElement e : businessLogics.getNavigatorElements()) {
            if (e instanceof NavigatorAction == actions){
                List<Object> row = new ArrayList<>();
                row.add(e.getCanonicalName());
                row.add(ThreadLocalContext.localize(e.caption));
                if(actions) {
                    FormEntity form = ((NavigatorAction) e).getForm();
                    row.add(form != null ? form.getCanonicalName() : null);
                    row.add(((NavigatorAction) e).getAction().getCanonicalName());
                }
                elementsData.add(row);
            }
        }
//        elementsData.add(asList((Object) "noParentGroup", "Без родительской группы"));

        startLogger.info("synchronizeNavigatorElements integration service started");
        List<ImportProperty<?>> propsNavigatorElement = new ArrayList<>();
        propsNavigatorElement.add(new ImportProperty(nameField, reflectionLM.canonicalNameNavigatorElement.getMapping(keyNavigatorElement)));
        propsNavigatorElement.add(new ImportProperty(captionField, reflectionLM.captionNavigatorElement.getMapping(keyNavigatorElement)));
        if(actions) {
            propsNavigatorElement.add(new ImportProperty(formCNField, reflectionLM.formNavigatorAction.getMapping(keyNavigatorElement), LM.object(reflectionLM.form).getMapping(keyForm)));
            propsNavigatorElement.add(new ImportProperty(actionCNField, reflectionLM.actionNavigatorAction.getMapping(keyNavigatorElement), LM.object(reflectionLM.action).getMapping(keyAction)));
        }

        List<ImportDelete> deletes = Collections.singletonList(
                new ImportDelete(keyNavigatorElement, deleteLP.getMapping(keyNavigatorElement), false)
        );
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();
        fields.add(nameField);
        fields.add(captionField);
        keys.add(keyNavigatorElement);
        if(actions) {
            fields.add(formCNField);
            fields.add(actionCNField);
            keys.add(keyForm);
            keys.add(keyAction);            
        }
        ImportTable table = new ImportTable(fields, elementsData);

        try {
            try (DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_NE");
                IntegrationService service = new IntegrationService(session, table, keys, propsNavigatorElement, deletes);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronizeNavigatorElements finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void migrateNavigatorElements() {
        startLogger.info("migrateNavigatorElements collecting data started");
        Map<String, String> nameChanges = dbManager.getNavigatorElementNameChanges();

        ImportField oldNavigatorElementCNField = new ImportField(reflectionLM.navigatorElementCanonicalNameClass);
        ImportField newNavigatorElementCNField = new ImportField(reflectionLM.navigatorElementCanonicalNameClass);

        ImportKey<?> keyNE = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementCanonicalName.getMapping(oldNavigatorElementCNField));

        try {
            List<List<Object>> data = new ArrayList<>();
            for (String oldName : nameChanges.keySet()) {
                data.add(Arrays.asList(oldName, nameChanges.get(oldName)));
            }

            startLogger.info("migrateNavigatorElements integration service started");
            List<ImportProperty<?>> properties = new ArrayList<>();
            properties.add(new ImportProperty(newNavigatorElementCNField, reflectionLM.canonicalNameNavigatorElement.getMapping(keyNE)));

            ImportTable table = new ImportTable(asList(oldNavigatorElementCNField, newNavigatorElementCNField), data);

            try (DataSession session = createSyncSession()) {
                IntegrationService service = new IntegrationService(session, table, Collections.singletonList(keyNE), properties);
                service.synchronize(false, false);
                apply(session);
                startLogger.info("migrateNavigatorElements finished");
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
    
    public void synchronizeForms() {
        startLogger.info("synchronizeForms collecting data started");
        ImportField nameField = new ImportField(reflectionLM.formCanonicalNameClass);
        ImportField captionField = new ImportField(reflectionLM.formCaptionClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.formByCanonicalName.getMapping(nameField));
        
        List<List<Object>> formsData = new ArrayList<>();
        for (FormEntity form : businessLogics.getFormEntities()) {
            formsData.add(asList(form.getCanonicalName(), ThreadLocalContext.localize(form.getCaption())));
        }
        formsData.add(asList("_NOFORM", ThreadLocalContext.localize(reflectionLM.noForm.getObjectCaption("instance"))));

        startLogger.info("synchronizeForms integration service started");
        List<ImportProperty<?>> props = new ArrayList<>();
        props.add(new ImportProperty(nameField, reflectionLM.formCanonicalName.getMapping(keyForm)));
        props.add(new ImportProperty(captionField, reflectionLM.formCaption.getMapping(keyForm)));

        List<ImportDelete> deletes = Collections.singletonList(
                new ImportDelete(keyForm, reflectionLM.isForm.getMapping(keyForm), false)
        );
        ImportTable table = new ImportTable(asList(nameField, captionField), formsData);

        try {
            try (DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_NE");
                IntegrationService service = new IntegrationService(session, table, Collections.singletonList(keyForm), props, deletes);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronizeForms finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    
    public void synchronizeParents() {

        startLogger.info("synchronizeParents collecting data started");
        ImportField nameField = new ImportField(reflectionLM.navigatorElementCanonicalNameClass);
        ImportField parentNameField = new ImportField(reflectionLM.navigatorElementCanonicalNameClass);
        ImportField numberField = new ImportField(reflectionLM.numberNavigatorElement);

        List<List<Object>> dataParents = getRelations(LM.root, getElementWithChildren(LM.root));

        startLogger.info("synchronizeParents integration service started");
        ImportKey<?> keyElement = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementCanonicalName.getMapping(nameField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementCanonicalName.getMapping(parentNameField));
        List<ImportProperty<?>> propsParent = new ArrayList<>();
        propsParent.add(new ImportProperty(parentNameField, reflectionLM.parentNavigatorElement.getMapping(keyElement), LM.object(reflectionLM.navigatorElement).getMapping(keyParent)));
        propsParent.add(new ImportProperty(numberField, reflectionLM.numberNavigatorElement.getMapping(keyElement), GroupType.MIN));
        ImportTable table = new ImportTable(asList(nameField, parentNameField, numberField), dataParents);
        try {
            try (DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_PT");
                IntegrationService service = new IntegrationService(session, table, asList(keyElement, keyParent), propsParent);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronizeParents finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private Set<String> getElementWithChildren(NavigatorElement element) {
        Set<String> parentInfo = new HashSet<>();
        parentInfo.add(element.getCanonicalName());
        
        for (NavigatorElement child : element.getChildren()) {
            parentInfo.addAll(getElementWithChildren(child));
        }
        return parentInfo;
    }

    private List<List<Object>> getRelations(NavigatorElement element, Set<String> elementsWithParent) {
        List<List<Object>> parentInfo = new ArrayList<>();
        
        int counter = 1;
        for (NavigatorElement child : element.getChildrenList()) {
            parentInfo.add(BaseUtils.toList(child.getCanonicalName(), element.getCanonicalName(), counter++));
            parentInfo.addAll(getRelations(child));
        }
        
//        counter = 1;
//        // todo [dale]: Будут ли вообще элементы навигатора без парента?
//        for(NavigatorElement navigatorElement : businessLogics.getNavigatorElements()) {
//            if(!elementsWithParent.contains(navigatorElement.getCanonicalName()))
//                parentInfo.add(BaseUtils.toList((Object) navigatorElement.getCanonicalName(), "noParentGroup", counter++));
//        }
        return parentInfo;
    }

    private List<List<Object>> getRelations(NavigatorElement element) {
        List<List<Object>> parentInfo = new ArrayList<>();
        
        int counter = 1;
        for (NavigatorElement child : element.getChildrenList()) {
            parentInfo.add(BaseUtils.toList(child.getCanonicalName(), element.getCanonicalName(), counter++));
            parentInfo.addAll(getRelations(child));
        }
        return parentInfo;
    }

    private void migratePropertyDraws() {
        startLogger.info("migratePropertyDraws collecting data started");
        Map<String, String> nameChanges = dbManager.getPropertyDrawNamesChanges();
        
        ImportField oldPropertyDrawSIDField = new ImportField(reflectionLM.propertyDrawSIDClass);
        ImportField oldFormCanonicalNameField = new ImportField(reflectionLM.formCanonicalNameClass);
        ImportField newPropertyDrawSIDField = new ImportField(reflectionLM.propertyDrawSIDClass);
        ImportField newFormCanonicalNameField = new ImportField(reflectionLM.formCanonicalNameClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.formByCanonicalName.getMapping(newFormCanonicalNameField));
        ImportKey<?> keyProperty = new ImportKey(reflectionLM.propertyDraw, reflectionLM.propertyDrawByFormNameAndPropertyDrawSid.getMapping(oldFormCanonicalNameField, oldPropertyDrawSIDField));

        try {
            List<List<Object>> data = new ArrayList<>();
            for (String oldName : nameChanges.keySet()) {
                String newName = nameChanges.get(oldName);
                String oldFormName = oldName.substring(0, oldName.lastIndexOf('.'));
                String newFormName = newName.substring(0, newName.lastIndexOf('.'));
                data.add(Arrays.asList(oldName.substring(oldFormName.length() + 1), oldFormName, newName.substring(newFormName.length() + 1), newFormName));
            }

            startLogger.info("migratePropertyDraws integration service started");
            List<ImportProperty<?>> properties = new ArrayList<>();
            properties.add(new ImportProperty(newPropertyDrawSIDField, reflectionLM.sidPropertyDraw.getMapping(keyProperty)));
            properties.add(new ImportProperty(newFormCanonicalNameField, reflectionLM.formPropertyDraw.getMapping(keyProperty), LM.object(reflectionLM.form).getMapping(keyForm)));

            ImportTable table = new ImportTable(asList(oldPropertyDrawSIDField, oldFormCanonicalNameField, newPropertyDrawSIDField, newFormCanonicalNameField), data);

            try (DataSession session = createSyncSession()) {
                IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyProperty), properties);
                service.synchronize(false, false);
                apply(session);
                startLogger.info("migratePropertyDraws finished");
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
    
    public void synchronizePropertyDraws() {
        startLogger.info("synchronizePropertyDraws collecting data started");
        migratePropertyDraws();
        
        List<List<Object>> dataPropertyDraws = new ArrayList<>();
        for (FormEntity formElement : businessLogics.getFormEntities()) {
            String canonicalName = formElement.getCanonicalName();
            if (canonicalName != null && formElement.needsToBeSynchronized()) {
                for (PropertyDrawEntity drawEntity : formElement.getPropertyDrawsListIt()) {
                    GroupObjectEntity groupObjectEntity = drawEntity.getToDraw(formElement);
                    dataPropertyDraws.add(asList(drawEntity.getCaption().toString(), drawEntity.getSID(), canonicalName, groupObjectEntity == null ? null : groupObjectEntity.getSID()));
                }
            }
        }

        startLogger.info("synchronizePropertyDraws integration service started");
        ImportField captionPropertyDrawField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField sidPropertyDrawField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField nameFormField = new ImportField(reflectionLM.formCanonicalNameClass);
        ImportField sidGroupObjectField = new ImportField(reflectionLM.propertySIDValueClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.formByCanonicalName.getMapping(nameFormField));
        ImportKey<?> keyPropertyDraw = new ImportKey(reflectionLM.propertyDraw, reflectionLM.propertyDrawByFormNameAndPropertyDrawSid.getMapping(nameFormField, sidPropertyDrawField));
        ImportKey<?> keyGroupObject = new ImportKey(reflectionLM.groupObject, reflectionLM.groupObjectSIDFormNameGroupObject.getMapping(sidGroupObjectField, nameFormField));

        List<ImportProperty<?>> propsPropertyDraw = new ArrayList<>();
        propsPropertyDraw.add(new ImportProperty(captionPropertyDrawField, reflectionLM.captionPropertyDraw.getMapping(keyPropertyDraw)));
        propsPropertyDraw.add(new ImportProperty(sidPropertyDrawField, reflectionLM.sidPropertyDraw.getMapping(keyPropertyDraw)));
        propsPropertyDraw.add(new ImportProperty(nameFormField, reflectionLM.formPropertyDraw.getMapping(keyPropertyDraw), LM.object(reflectionLM.form).getMapping(keyForm)));
        propsPropertyDraw.add(new ImportProperty(sidGroupObjectField, reflectionLM.groupObjectPropertyDraw.getMapping(keyPropertyDraw), LM.object(reflectionLM.groupObject).getMapping(keyGroupObject)));


        List<ImportDelete> deletes = new ArrayList<>();
        deletes.add(new ImportDelete(keyPropertyDraw, LM.is(reflectionLM.propertyDraw).getMapping(keyPropertyDraw), false));

        ImportTable table = new ImportTable(asList(captionPropertyDrawField, sidPropertyDrawField, nameFormField, sidGroupObjectField), dataPropertyDraws);

        try {
            try (DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_PD");
                IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyPropertyDraw, keyGroupObject), propsPropertyDraw, deletes);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronizePropertyDraws finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void synchronizeGroupObjects() {
        startLogger.info("synchronizeGroupObjects collecting data started");
        List<List<Object>> dataGroupObjectList = new ArrayList<>();
        for (FormEntity formElement : businessLogics.getFormEntities()) {
            String formCanonicalName = formElement.getCanonicalName();
            if (formCanonicalName != null && formElement.needsToBeSynchronized()) { //formSID - sidGroupObject
                for (PropertyDrawEntity property : formElement.getPropertyDrawsListIt()) {
                    GroupObjectEntity groupObjectEntity = property.getToDraw(formElement);
                    if (groupObjectEntity != null) {
                        dataGroupObjectList.add(
                                Arrays.asList(formCanonicalName, groupObjectEntity.getSID()));
                    }
                }
            }
        }

        startLogger.info("synchronizeGroupObjects integration service started");
        ImportField canonicalNameFormField = new ImportField(reflectionLM.formCanonicalNameClass);
        ImportField sidGroupObjectField = new ImportField(reflectionLM.propertySIDValueClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.formByCanonicalName.getMapping(canonicalNameFormField));
        ImportKey<?> keyGroupObject = new ImportKey(reflectionLM.groupObject, reflectionLM.groupObjectSIDFormNameGroupObject.getMapping(sidGroupObjectField, canonicalNameFormField));

        List<ImportProperty<?>> propsGroupObject = new ArrayList<>();
        propsGroupObject.add(new ImportProperty(sidGroupObjectField, reflectionLM.sidGroupObject.getMapping(keyGroupObject)));
        propsGroupObject.add(new ImportProperty(canonicalNameFormField, reflectionLM.formGroupObject.getMapping(keyGroupObject), LM.object(reflectionLM.form).getMapping(keyForm)));

        List<ImportDelete> deletes = new ArrayList<>();
        deletes.add(new ImportDelete(keyGroupObject, LM.is(reflectionLM.groupObject).getMapping(keyGroupObject), false));

        ImportTable table = new ImportTable(asList(canonicalNameFormField, sidGroupObjectField), dataGroupObjectList);

        try {
            try(DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_GO");
                IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyGroupObject), propsGroupObject, deletes);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronizeGroupObjects finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean needsToBeSynchronized(ActionOrProperty property) {
        return property.isNamed() && (property instanceof Action || !((Property)property).isEmpty(AlgType.syncType));
    }

    public void synchronizePropertyEntities() {
        synchronizePropertyEntities(true);
        synchronizePropertyEntities(false);
    }
    public void synchronizePropertyEntities(boolean actions) {

        startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Entities collecting data started");
        ImportField canonicalNamePropertyField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField dbNamePropertyField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField captionPropertyField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField loggablePropertyField = new ImportField(LogicalClass.instance);
        ImportField storedPropertyField = new ImportField(LogicalClass.instance);
        ImportField isSetNotNullPropertyField = new ImportField(LogicalClass.instance);
        ImportField disableInputListPropertyField = new ImportField(LogicalClass.instance);
        ImportField returnPropertyField = new ImportField(reflectionLM.propertyClassValueClass);
        ImportField classPropertyField = new ImportField(reflectionLM.propertyClassValueClass);
        ImportField complexityPropertyField = new ImportField(LongClass.instance);
        ImportField tableSIDPropertyField = new ImportField(reflectionLM.propertyTableValueClass);
        ImportField annotationPropertyField = new ImportField(reflectionLM.propertyTableValueClass);
        ImportField statsPropertyField = new ImportField(ValueExpr.COUNTCLASS);

        ConcreteCustomClass customClass = actions ? reflectionLM.action : reflectionLM.property;
        LP objectByName = actions ? reflectionLM.actionCanonicalName : reflectionLM.propertyCanonicalName;
        LP nameByObject = actions ? reflectionLM.canonicalNameAction : reflectionLM.canonicalNameProperty;
        ImportKey<?> keyProperty = new ImportKey(customClass, objectByName.getMapping(canonicalNamePropertyField));

        try {
            List<List<Object>> dataProperty = new ArrayList<>();
            for (ActionOrProperty actionOrProperty : businessLogics.getOrderActionOrProperties()) {
                if (needsToBeSynchronized(actionOrProperty)) {
                    if((actionOrProperty instanceof Action) != actions)
                        continue;
                    
                    String returnClass = null;
                    String classProperty = "";
                    String tableSID = "";
                    String fieldSID = "";
                    Long complexityProperty = null;

                    try {
                        classProperty = actionOrProperty.getClass().getSimpleName();
                        
                        if(actionOrProperty instanceof Property) {
                            Property property = (Property)actionOrProperty;
                            complexityProperty = property.getComplexity();
                            if (property.mapTable != null) {
                                tableSID = property.mapTable.table.getName();
                                fieldSID = property.field.getName();
                            } else {
                                tableSID = "";
                                fieldSID = "";
                            }

                            returnClass = ((Property)actionOrProperty).getValueClass(ClassType.syncPolicy).getSID();
                        }
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException ignored) {
                    }
                    
                    dataProperty.add(asList(actionOrProperty.getCanonicalName(), fieldSID, ThreadLocalContext.localize(actionOrProperty.caption),
                            actionOrProperty instanceof Property && ((Property) actionOrProperty).userLoggable ? true : null,
                            actionOrProperty instanceof Property && ((Property) actionOrProperty).isStored() ? true : null,
                            actionOrProperty instanceof Property && ((Property) actionOrProperty).userNotNull ? true : null,
                            actionOrProperty instanceof Property && ((Property) actionOrProperty).disableInputList ? true : null,
                            returnClass, classProperty, complexityProperty, tableSID, actionOrProperty.annotation,
                            (Settings.get().isDisableSyncStatProps() || !(actionOrProperty instanceof Property) ? (Integer)Stat.DEFAULT.getCount() : DBManager.getPropertyInterfaceStat((Property)actionOrProperty))));
                }
            }

            startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Entities integration service started");
            List<ImportProperty<?>> properties = new ArrayList<>();
            properties.add(new ImportProperty(canonicalNamePropertyField, nameByObject.getMapping(keyProperty)));
            properties.add(new ImportProperty(dbNamePropertyField, reflectionLM.dbNameProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(captionPropertyField, reflectionLM.captionProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(loggablePropertyField, reflectionLM.loggableProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(storedPropertyField, reflectionLM.storedProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(isSetNotNullPropertyField, reflectionLM.isSetNotNullProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(disableInputListPropertyField, reflectionLM.disableInputListProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(returnPropertyField, reflectionLM.returnProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(classPropertyField, reflectionLM.classProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(complexityPropertyField, reflectionLM.complexityProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(tableSIDPropertyField, reflectionLM.tableSIDProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(annotationPropertyField, reflectionLM.annotationProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(statsPropertyField, reflectionLM.statsProperty.getMapping(keyProperty)));

            List<ImportDelete> deletes = new ArrayList<>();
            deletes.add(new ImportDelete(keyProperty, LM.is(customClass).getMapping(keyProperty), false));

            ImportTable table = new ImportTable(asList(canonicalNamePropertyField, dbNamePropertyField, captionPropertyField, loggablePropertyField,
                    storedPropertyField, isSetNotNullPropertyField, disableInputListPropertyField, returnPropertyField,
                    classPropertyField, complexityPropertyField, tableSIDPropertyField, annotationPropertyField, statsPropertyField), dataProperty);

            try (DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_PE");
                IntegrationService service = new IntegrationService(session, table, Collections.singletonList(keyProperty), properties, deletes);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Entities finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void synchronizePropertyParents() {
        synchronizePropertyParents(true);
        synchronizePropertyParents(false);
    }
    public void synchronizePropertyParents(boolean actions) {
        startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Parents collecting data started");

        ImportField canonicalNamePropertyField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField numberPropertyField = new ImportField(reflectionLM.numberProperty);
        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);

        List<List<Object>> dataParent = new ArrayList<>();
        for (ActionOrProperty property : businessLogics.getOrderActionOrProperties()) {
            if (needsToBeSynchronized(property)) {
                if((property instanceof Action) != actions)
                    continue;
                dataParent.add(asList(property.getCanonicalName(), property.getParent().getSID(), getNumberInListOfChildren(property)));
            }
        }

        startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Parents integration service started");
        ConcreteCustomClass customClass = actions ? reflectionLM.action : reflectionLM.property;
        LP objectByName = actions ? reflectionLM.actionCanonicalName : reflectionLM.propertyCanonicalName;
        ImportKey<?> keyProperty = new ImportKey(customClass, objectByName.getMapping(canonicalNamePropertyField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.propertyGroup, reflectionLM.propertyGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> properties = new ArrayList<>();

        properties.add(new ImportProperty(parentSidField, reflectionLM.parentProperty.getMapping(keyProperty), LM.object(reflectionLM.propertyGroup).getMapping(keyParent)));
        properties.add(new ImportProperty(numberPropertyField, reflectionLM.numberProperty.getMapping(keyProperty)));
        ImportTable table = new ImportTable(asList(canonicalNamePropertyField, parentSidField, numberPropertyField), dataParent);

        try {
            try (DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_PP");
                IntegrationService service = new IntegrationService(session, table, asList(keyProperty, keyParent), properties);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Parents finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void synchronizeGroupProperties() {

        startLogger.info("synchronizeGroupProperties collecting data started");
        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField captionField = new ImportField(reflectionLM.navigatorElementCaptionClass);
        ImportField numberField = new ImportField(reflectionLM.numberPropertyGroup);

        ImportKey<?> key = new ImportKey(reflectionLM.propertyGroup, reflectionLM.propertyGroupSID.getMapping(sidField));

        List<List<Object>> data = new ArrayList<>();

        for (Group group : businessLogics.getChildGroups()) {
            data.add(asList(group.getSID(), ThreadLocalContext.localize(group.caption)));
        }

        startLogger.info("synchronizeGroupProperties integration service started");
        List<ImportProperty<?>> props = new ArrayList<>();
        props.add(new ImportProperty(sidField, reflectionLM.SIDPropertyGroup.getMapping(key)));
        props.add(new ImportProperty(captionField, reflectionLM.captionPropertyGroup.getMapping(key)));

        List<ImportDelete> deletes = new ArrayList<>();
        deletes.add(new ImportDelete(key, LM.is(reflectionLM.propertyGroup).getMapping(key), false));

        ImportTable table = new ImportTable(asList(sidField, captionField), data);

        List<List<Object>> data2 = new ArrayList<>();

        for (Group group : businessLogics.getChildGroups()) {
            if (group.getParent() != null) {
                data2.add(asList(group.getSID(), group.getParent().getSID(), getNumberInListOfChildren(group)));
            }
        }

        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportKey<?> key2 = new ImportKey(reflectionLM.propertyGroup, reflectionLM.propertyGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> props2 = new ArrayList<>();
        props2.add(new ImportProperty(parentSidField, reflectionLM.parentPropertyGroup.getMapping(key), LM.object(reflectionLM.propertyGroup).getMapping(key2)));
        props2.add(new ImportProperty(numberField, reflectionLM.numberPropertyGroup.getMapping(key)));
        ImportTable table2 = new ImportTable(asList(sidField, parentSidField, numberField), data2);

        try {
            try (DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_GP");
                IntegrationService service = new IntegrationService(session, table, Collections.singletonList(key), props, deletes);
                service.synchronize(true, false);
                service = new IntegrationService(session, table2, asList(key, key2), props2);
                service.synchronize(true, false);
                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronizeGroupProperties finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


    private Integer getNumberInListOfChildren(AbstractNode abstractNode) {
        Group nodeParent = abstractNode.getParent();
        int counter = 0;
        for (AbstractNode node : nodeParent.getChildrenIt()) {
            if(abstractNode instanceof ActionOrProperty && counter > 20)  // оптимизация
                return nodeParent.getIndexedPropChildren().get((ActionOrProperty) abstractNode);
            counter++;
            if (abstractNode instanceof ActionOrProperty) {
                if (node instanceof ActionOrProperty)
                    if (node == abstractNode) {
                        return counter;
                    }
            } else {
                if (node instanceof Group)
                    if (((Group) node).getCanonicalName().equals(((Group) abstractNode).getCanonicalName())) {
                        return counter;
                    }
            }
        }
        return 0;
    }

    public void synchronizeTables() {

        startLogger.info("synchronizeTables collecting data started");
        ImportField tableSidField = new ImportField(reflectionLM.sidTable);
        ImportField tableKeySidField = new ImportField(reflectionLM.sidTableKey);
        ImportField tableKeyNameField = new ImportField(reflectionLM.nameTableKey);
        ImportField tableKeyClassField = new ImportField(reflectionLM.classTableKey);
        ImportField tableKeyClassSIDField = new ImportField(reflectionLM.classSIDTableKey);
        ImportField tableColumnSidField = new ImportField(reflectionLM.sidTableColumn);
        ImportField tableColumnLongSIDField = new ImportField(reflectionLM.longSIDTableColumn); 

        ImportKey<?> tableKey = new ImportKey(reflectionLM.table, reflectionLM.tableSID.getMapping(tableSidField));
        ImportKey<?> tableKeyKey = new ImportKey(reflectionLM.tableKey, reflectionLM.tableKeySID.getMapping(tableKeySidField));
        ImportKey<?> tableColumnKey = new ImportKey(reflectionLM.tableColumn, reflectionLM.tableColumnLongSID.getMapping(tableColumnLongSIDField));

        List<List<Object>> data = new ArrayList<>();
        List<List<Object>> dataKeys = new ArrayList<>();
        List<List<Object>> dataProps = new ArrayList<>();
        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            Object tableName = dataTable.getName();
            data.add(Collections.singletonList(tableName));
            ImMap<KeyField, ValueClass> classes = dataTable.getClasses().getCommonParent(dataTable.getTableKeys());
            for (KeyField key : dataTable.keys) {
                dataKeys.add(asList(tableName, key.getName(), tableName + "." + key.getName(), ThreadLocalContext.localize(classes.get(key).getCaption()), classes.get(key).getSID()));
            }
            for (PropertyField property : dataTable.properties) {
                dataProps.add(asList(tableName, property.getName(), tableName + "." + property.getName()));
            }
        }

        startLogger.info("synchronizeTables integration service started");
        List<ImportProperty<?>> properties = new ArrayList<>();
        properties.add(new ImportProperty(tableSidField, reflectionLM.sidTable.getMapping(tableKey)));

        List<ImportProperty<?>> propertiesKeys = new ArrayList<>();
        propertiesKeys.add(new ImportProperty(tableKeySidField, reflectionLM.sidTableKey.getMapping(tableKeyKey)));
        propertiesKeys.add(new ImportProperty(tableKeyNameField, reflectionLM.nameTableKey.getMapping(tableKeyKey)));
        propertiesKeys.add(new ImportProperty(tableKeyClassField, reflectionLM.classTableKey.getMapping(tableKeyKey)));
        propertiesKeys.add(new ImportProperty(tableKeyClassSIDField, reflectionLM.classSIDTableKey.getMapping(tableKeyKey)));
        propertiesKeys.add(new ImportProperty(null, reflectionLM.tableTableKey.getMapping(tableKeyKey), reflectionLM.tableSID.getMapping(tableSidField)));

        List<ImportProperty<?>> propertiesColumns = new ArrayList<>();
        propertiesColumns.add(new ImportProperty(null, reflectionLM.tableTableColumn.getMapping(tableColumnKey), reflectionLM.tableSID.getMapping(tableSidField)));
        propertiesColumns.add(new ImportProperty(tableColumnSidField, reflectionLM.sidTableColumn.getMapping(tableColumnKey)));
        propertiesColumns.add(new ImportProperty(tableColumnLongSIDField, reflectionLM.longSIDTableColumn.getMapping(tableColumnKey)));

        List<ImportDelete> delete = new ArrayList<>();
        delete.add(new ImportDelete(tableKey, LM.is(reflectionLM.table).getMapping(tableKey), false));

        List<ImportDelete> deleteKeys = new ArrayList<>();
        deleteKeys.add(new ImportDelete(tableKeyKey, LM.is(reflectionLM.tableKey).getMapping(tableKeyKey), false));

        List<ImportDelete> deleteColumns = new ArrayList<>();
        deleteColumns.add(new ImportDelete(tableColumnKey, LM.is(reflectionLM.tableColumn).getMapping(tableColumnKey), false));

        ImportTable table = new ImportTable(Collections.singletonList(tableSidField), data);
        ImportTable tableKeys = new ImportTable(asList(tableSidField, tableKeyNameField, tableKeySidField, tableKeyClassField, tableKeyClassSIDField), dataKeys);
        ImportTable tableColumns = new ImportTable(asList(tableSidField, tableColumnSidField, tableColumnLongSIDField), dataProps);

        try {
            try(DataSession session = createSyncSession()) {
                session.pushVolatileStats("RM_TE");

                IntegrationService service = new IntegrationService(session, table, Collections.singletonList(tableKey), properties, delete);
                service.synchronize(true, false);

                service = new IntegrationService(session, tableKeys, Collections.singletonList(tableKeyKey), propertiesKeys, deleteKeys);
                service.synchronize(true, false);

                service = new IntegrationService(session, tableColumns, Collections.singletonList(tableColumnKey), propertiesColumns, deleteColumns);
                service.synchronize(true, false);

                session.popVolatileStats();
                apply(session);
                startLogger.info("synchronizeTables finished");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
