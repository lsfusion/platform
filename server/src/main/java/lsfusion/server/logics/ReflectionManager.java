package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.*;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.navigator.NavigatorAction;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.integration.*;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.property.group.AbstractNode;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.PublicTask;
import lsfusion.server.logics.tasks.TaskRunner;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;
import static lsfusion.base.SystemUtils.getRevision;

public class ReflectionManager extends LifecycleAdapter implements InitializingBean {

    public static final Logger systemLogger = ServerLoggers.systemLogger;

    private BusinessLogics<?> businessLogics;
    
    private DBManager dbManager;

    private PublicTask initTask;

    public void setBusinessLogics(BusinessLogics<?> businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setInitTask(PublicTask initTask) {
        this.initTask = initTask;
    }

    private BaseLogicsModule<?> LM;
    private ReflectionLogicsModule reflectionLM;
    private SystemEventsLogicsModule systemEventsLM;
    private TimeLogicsModule timeLM;

    public void afterPropertiesSet() throws Exception {
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
            TaskRunner.runTask(initTask, systemLogger);
        } catch (Exception e) {
            throw new RuntimeException("Error starting ReflectionManager: ", e);
        }
    }

    public Integer getServerComputer() {
        return dbManager.getComputer(SystemUtils.getLocalHostName());
    }
    
    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }
    
    public boolean isSourceHashChanged() {
        return dbManager.sourceHashChanged;
    }

    public void synchronizeNavigatorElements() {
        synchronizeNavigatorElements(reflectionLM.form, FormEntity.class, false, reflectionLM.isForm);
        synchronizeNavigatorElements(reflectionLM.navigatorAction, NavigatorAction.class, true, reflectionLM.isNavigatorAction);
        synchronizeNavigatorElements(reflectionLM.navigatorElement, NavigatorElement.class, true, reflectionLM.isNavigatorElement);
    }
    
    private void synchronizeForms() {
        synchronizeNavigatorElements();
        synchronizeParents();
        synchronizeGroupObjects();
        synchronizePropertyDraws();
    }

    private void synchronizeNavigatorElements(ConcreteCustomClass elementCustomClass, Class<? extends NavigatorElement> filterJavaClass, boolean exactJavaClass, LCP deleteLP) {

        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField captionField = new ImportField(reflectionLM.navigatorElementCaptionClass);

        ImportKey<?> keyNavigatorElement = new ImportKey(elementCustomClass, reflectionLM.navigatorElementSID.getMapping(sidField));

        List<List<Object>> elementsData = new ArrayList<List<Object>>();
        for (NavigatorElement element : businessLogics.getNavigatorElements()) {
            if (element.needsToBeSynchronized() && (exactJavaClass ? filterJavaClass == element.getClass() : filterJavaClass.isInstance(element))) {
                elementsData.add(asList((Object) element.getSID(), element.caption));
            }
        }
        elementsData.add(asList((Object) "noParentGroup", "Без родительской группы"));

        List<ImportProperty<?>> propsNavigatorElement = new ArrayList<ImportProperty<?>>();
        propsNavigatorElement.add(new ImportProperty(sidField, reflectionLM.sidNavigatorElement.getMapping(keyNavigatorElement)));
        propsNavigatorElement.add(new ImportProperty(captionField, reflectionLM.captionNavigatorElement.getMapping(keyNavigatorElement)));

        List<ImportDelete> deletes = asList(
                new ImportDelete(keyNavigatorElement, deleteLP.getMapping(keyNavigatorElement), false)
        );
        ImportTable table = new ImportTable(asList(sidField, captionField), elementsData);

        try {
            DataSession session = createSession();
            session.pushVolatileStats("RM_NE");

            IntegrationService service = new IntegrationService(session, table, asList(keyNavigatorElement), propsNavigatorElement, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void synchronizeParents() {

        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField numberField = new ImportField(reflectionLM.numberNavigatorElement);

        List<List<Object>> dataParents = getRelations(LM.root, getElementsWithParent(LM.root));

        ImportKey<?> keyElement = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementSID.getMapping(sidField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementSID.getMapping(parentSidField));
        List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
        propsParent.add(new ImportProperty(parentSidField, reflectionLM.parentNavigatorElement.getMapping(keyElement), LM.object(reflectionLM.navigatorElement).getMapping(keyParent)));
        propsParent.add(new ImportProperty(numberField, reflectionLM.numberNavigatorElement.getMapping(keyElement), GroupType.MIN));
        ImportTable table = new ImportTable(asList(sidField, parentSidField, numberField), dataParents);
        try {
            DataSession session = createSession();
            session.pushVolatileStats("RM_PT");

            IntegrationService service = new IntegrationService(session, table, asList(keyElement, keyParent), propsParent);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Set<String> getElementsWithParent(NavigatorElement element) {
        Set<String> parentInfo = new HashSet<String>();
        ImSet<NavigatorElement> children = (ImSet<NavigatorElement>) element.getChildren();
        parentInfo.add(element.getSID());
        for (NavigatorElement child : children) 
            if(child.needsToBeSynchronized()) {
                parentInfo.add(child.getSID());
                parentInfo.addAll(getElementsWithParent(child));
            }
        return parentInfo;
    }

    protected List<List<Object>> getRelations(NavigatorElement element, Set<String> elementsWithParent) {
        List<List<Object>> parentInfo = new ArrayList<List<Object>>();
        ImSet<NavigatorElement> children = (ImSet<NavigatorElement>) element.getChildren();
        int counter = 1;
        for (NavigatorElement child : children) 
            if(child.needsToBeSynchronized()) {
                parentInfo.add(BaseUtils.toList((Object) child.getSID(), element.getSID(), counter++));
                parentInfo.addAll(getRelations(child));
            }
        counter = 1;
        for(NavigatorElement navigatorElement : businessLogics.getNavigatorElements()) {
            if(navigatorElement.needsToBeSynchronized() && !elementsWithParent.contains(navigatorElement.getSID()))
                parentInfo.add(BaseUtils.toList((Object) navigatorElement.getSID(), "noParentGroup", counter++));
        }
        return parentInfo;
    }

    protected List<List<Object>> getRelations(NavigatorElement element) {
        List<List<Object>> parentInfo = new ArrayList<List<Object>>();
        ImSet<NavigatorElement> children = (ImSet<NavigatorElement>) element.getChildren();
        int counter = 1;
        for (NavigatorElement child : children)
            if(child.needsToBeSynchronized()) {
                parentInfo.add(BaseUtils.toList((Object) child.getSID(), element.getSID(), counter++));
                parentInfo.addAll(getRelations(child));
            }
        return parentInfo;
    }

    public void synchronizePropertyDraws() {

        List<List<Object>> dataPropertyDraws = new ArrayList<List<Object>>();
        for (FormEntity formElement : businessLogics.getFormEntities()) 
            if(formElement.needsToBeSynchronized()) {
                ImList<PropertyDrawEntity> propertyDraws = formElement.getPropertyDrawsList();
                for (PropertyDrawEntity drawEntity : propertyDraws) {
                    GroupObjectEntity groupObjectEntity = drawEntity.getToDraw(formElement);
                    dataPropertyDraws.add(asList(drawEntity.propertyObject.toString(), drawEntity.getSID(), (Object) formElement.getSID(), groupObjectEntity == null ? null : groupObjectEntity.getSID()));
                }
            }

        ImportField captionPropertyDrawField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField sidPropertyDrawField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField sidNavigatorElementField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField sidGroupObjectField = new ImportField(reflectionLM.propertySIDValueClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.navigatorElementSID.getMapping(sidNavigatorElementField));
        ImportKey<?> keyPropertyDraw = new ImportKey(reflectionLM.propertyDraw, reflectionLM.propertyDrawSIDNavigatorElementSIDPropertyDraw.getMapping(sidNavigatorElementField, sidPropertyDrawField));
        ImportKey<?> keyGroupObject = new ImportKey(reflectionLM.groupObject, reflectionLM.groupObjectSIDGroupObjectSIDNavigatorElementGroupObject.getMapping(sidGroupObjectField, sidNavigatorElementField));

        List<ImportProperty<?>> propsPropertyDraw = new ArrayList<ImportProperty<?>>();
        propsPropertyDraw.add(new ImportProperty(captionPropertyDrawField, reflectionLM.captionPropertyDraw.getMapping(keyPropertyDraw)));
        propsPropertyDraw.add(new ImportProperty(sidPropertyDrawField, reflectionLM.sidPropertyDraw.getMapping(keyPropertyDraw)));
        propsPropertyDraw.add(new ImportProperty(sidNavigatorElementField, reflectionLM.formPropertyDraw.getMapping(keyPropertyDraw), LM.object(reflectionLM.navigatorElement).getMapping(keyForm)));
        propsPropertyDraw.add(new ImportProperty(sidGroupObjectField, reflectionLM.groupObjectPropertyDraw.getMapping(keyPropertyDraw), LM.object(reflectionLM.groupObject).getMapping(keyGroupObject)));


        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(keyPropertyDraw, LM.is(reflectionLM.propertyDraw).getMapping(keyPropertyDraw), false));

        ImportTable table = new ImportTable(asList(captionPropertyDrawField, sidPropertyDrawField, sidNavigatorElementField, sidGroupObjectField), dataPropertyDraws);

        try {
            DataSession session = createSession();
            session.pushVolatileStats("RM_PD");

            IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyPropertyDraw, keyGroupObject), propsPropertyDraw, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void synchronizeGroupObjects() {

        List<List<Object>> dataGroupObjectList = new ArrayList<List<Object>>();
        for (FormEntity<?> formElement : businessLogics.getFormEntities()) 
            if(formElement.needsToBeSynchronized()) { //formSID - sidGroupObject
                for (PropertyDrawEntity property : formElement.getPropertyDrawsList()) {
                    GroupObjectEntity groupObjectEntity = property.getToDraw(formElement);
                    if (groupObjectEntity != null)
                        dataGroupObjectList.add(Arrays.asList((Object) formElement.getSID(),
                                groupObjectEntity.getSID()));
                }
            }

        ImportField sidNavigatorElementField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField sidGroupObjectField = new ImportField(reflectionLM.propertySIDValueClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.navigatorElementSID.getMapping(sidNavigatorElementField));
        ImportKey<?> keyGroupObject = new ImportKey(reflectionLM.groupObject, reflectionLM.groupObjectSIDGroupObjectSIDNavigatorElementGroupObject.getMapping(sidGroupObjectField, sidNavigatorElementField));

        List<ImportProperty<?>> propsGroupObject = new ArrayList<ImportProperty<?>>();
        propsGroupObject.add(new ImportProperty(sidGroupObjectField, reflectionLM.sidGroupObject.getMapping(keyGroupObject)));
        propsGroupObject.add(new ImportProperty(sidNavigatorElementField, reflectionLM.navigatorElementGroupObject.getMapping(keyGroupObject), LM.object(reflectionLM.navigatorElement).getMapping(keyForm)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(keyGroupObject, LM.is(reflectionLM.groupObject).getMapping(keyGroupObject), false));

        ImportTable table = new ImportTable(asList(sidNavigatorElementField, sidGroupObjectField), dataGroupObjectList);

        try {
            DataSession session = createSession();
            session.pushVolatileStats("RM_GO");

            IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyGroupObject), propsGroupObject, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean needsToBeSynchronized(Property property) {
        return property.isNamed() && (property instanceof ActionProperty || (((CalcProperty)property).isFull() && !(((CalcProperty)property).isEmpty())));
    }

    private void synchronizeProperties() {
        synchronizePropertyEntities();
        synchronizePropertyParents();
    }

    public void synchronizePropertyEntities() {
        ImportField canonicalNamePropertyField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField sidPropertyField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField captionPropertyField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField loggablePropertyField = new ImportField(reflectionLM.propertyLoggableValueClass);
        ImportField storedPropertyField = new ImportField(reflectionLM.propertyStoredValueClass);
        ImportField isSetNotNullPropertyField = new ImportField(reflectionLM.propertyIsSetNotNullValueClass);
        ImportField signaturePropertyField = new ImportField(reflectionLM.propertySignatureValueClass);
        ImportField returnPropertyField = new ImportField(reflectionLM.propertySignatureValueClass);
        ImportField classPropertyField = new ImportField(reflectionLM.propertySignatureValueClass);
        ImportField complexityPropertyField = new ImportField(LongClass.instance);
        ImportField tableSIDPropertyField = new ImportField(reflectionLM.propertyTableValueClass);

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertyCanonicalName.getMapping(canonicalNamePropertyField));

        try {
            List<List<Object>> dataProperty = new ArrayList<List<Object>>();
            for (Property property : businessLogics.getOrderProperties()) {
                if (needsToBeSynchronized(property)) {
                    String commonClasses = "";
                    String returnClass = "";
                    String classProperty = "";
                    String tableSID = "";
                    Long complexityProperty = null;
                    try {
                        classProperty = property.getClass().getSimpleName();
                        if(property instanceof CalcProperty) {
                            CalcProperty calcProperty = (CalcProperty)property;
                            complexityProperty = calcProperty.getComplexity();
                            if (calcProperty.mapTable != null) {
                                tableSID = calcProperty.mapTable.table.getName();
                            } else {
                                tableSID = "";
                            }
                        }
                        returnClass = property.getValueClass().getSID();
                        for (Object cc : property.getInterfaceClasses(property instanceof ActionProperty ? ClassType.FULL : ClassType.ASSERTFULL).valueIt()) {
                            if (cc instanceof CustomClass)
                                commonClasses += ((CustomClass) cc).getSID() + ", ";
                            else if (cc instanceof DataClass)
                                commonClasses += ((DataClass) cc).getSID() + ", ";
                        }
                        if (!"".equals(commonClasses))
                            commonClasses = commonClasses.substring(0, commonClasses.length() - 2);
                    } catch (NullPointerException e) {
                        commonClasses = "";
                    } catch (ArrayIndexOutOfBoundsException e) {
                        commonClasses = "";
                    }
                    dataProperty.add(asList(property.getCanonicalName(),(Object) property.getSID(), property.caption, property.loggable ? true : null,
                            property instanceof CalcProperty && ((CalcProperty) property).isStored() ? true : null,
                            property instanceof CalcProperty && ((CalcProperty) property).setNotNull ? true : null,
                            commonClasses, returnClass, classProperty, complexityProperty, tableSID));
                }
            }

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            properties.add(new ImportProperty(canonicalNamePropertyField, reflectionLM.canonicalNameProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(sidPropertyField, reflectionLM.SIDProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(captionPropertyField, reflectionLM.captionProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(loggablePropertyField, reflectionLM.loggableProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(storedPropertyField, reflectionLM.storedProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(isSetNotNullPropertyField, reflectionLM.isSetNotNullProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(signaturePropertyField, reflectionLM.signatureProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(returnPropertyField, reflectionLM.returnProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(classPropertyField, reflectionLM.classProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(complexityPropertyField, reflectionLM.complexityProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(tableSIDPropertyField, reflectionLM.tableSIDProperty.getMapping(keyProperty)));

            List<ImportDelete> deletes = new ArrayList<ImportDelete>();
            deletes.add(new ImportDelete(keyProperty, LM.is(reflectionLM.property).getMapping(keyProperty), false));

            ImportTable table = new ImportTable(asList(canonicalNamePropertyField, sidPropertyField, captionPropertyField, loggablePropertyField,
                    storedPropertyField, isSetNotNullPropertyField, signaturePropertyField, returnPropertyField,
                    classPropertyField, complexityPropertyField, tableSIDPropertyField), dataProperty);

            DataSession session = createSession();
            session.pushVolatileStats("RM_PE");

            IntegrationService service = new IntegrationService(session, table, asList(keyProperty), properties, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void synchronizePropertyParents() {

        ImportField canonicalNamePropertyField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField numberPropertyField = new ImportField(reflectionLM.numberProperty);
        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);

        List<List<Object>> dataParent = new ArrayList<List<Object>>();
        for (Property property : businessLogics.getOrderProperties()) {
            if (needsToBeSynchronized(property))
                dataParent.add(asList(property.getCanonicalName(), (Object) property.getParent().getSID(), getNumberInListOfChildren(property)));
        }

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertyCanonicalName.getMapping(canonicalNamePropertyField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.propertyGroup, reflectionLM.propertyGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        properties.add(new ImportProperty(parentSidField, reflectionLM.parentProperty.getMapping(keyProperty), LM.object(reflectionLM.propertyGroup).getMapping(keyParent)));
        properties.add(new ImportProperty(numberPropertyField, reflectionLM.numberProperty.getMapping(keyProperty)));
        ImportTable table = new ImportTable(asList(canonicalNamePropertyField, parentSidField, numberPropertyField), dataParent);

        try {
            DataSession session = createSession();
            session.pushVolatileStats("RM_PP");

            IntegrationService service = new IntegrationService(session, table, asList(keyProperty, keyParent), properties);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void synchronizeGroupProperties() {

        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField captionField = new ImportField(reflectionLM.navigatorElementCaptionClass);
        ImportField numberField = new ImportField(reflectionLM.numberPropertyGroup);

        ImportKey<?> key = new ImportKey(reflectionLM.propertyGroup, reflectionLM.propertyGroupSID.getMapping(sidField));

        List<List<Object>> data = new ArrayList<List<Object>>();

        for (AbstractGroup group : businessLogics.getParentGroups()) {
            data.add(asList(group.getSID(), (Object) group.caption));
        }

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        props.add(new ImportProperty(sidField, reflectionLM.SIDPropertyGroup.getMapping(key)));
        props.add(new ImportProperty(captionField, reflectionLM.captionPropertyGroup.getMapping(key)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(key, LM.is(reflectionLM.propertyGroup).getMapping(key), false));

        ImportTable table = new ImportTable(asList(sidField, captionField), data);

        List<List<Object>> data2 = new ArrayList<List<Object>>();

        for (AbstractGroup group : businessLogics.getParentGroups()) {
            if (group.getParent() != null) {
                data2.add(asList(group.getSID(), (Object) group.getParent().getSID(), getNumberInListOfChildren(group)));
            }
        }

        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportKey<?> key2 = new ImportKey(reflectionLM.propertyGroup, reflectionLM.propertyGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> props2 = new ArrayList<ImportProperty<?>>();
        props2.add(new ImportProperty(parentSidField, reflectionLM.parentPropertyGroup.getMapping(key), LM.object(reflectionLM.propertyGroup).getMapping(key2)));
        props2.add(new ImportProperty(numberField, reflectionLM.numberPropertyGroup.getMapping(key)));
        ImportTable table2 = new ImportTable(asList(sidField, parentSidField, numberField), data2);

        try {
            DataSession session = createSession();
            session.pushVolatileStats("RM_GP");
            
            IntegrationService service = new IntegrationService(session, table, asList(key), props, deletes);
            service.synchronize(true, false);

            service = new IntegrationService(session, table2, asList(key, key2), props2);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private Integer getNumberInListOfChildren(AbstractNode abstractNode) {
        AbstractGroup nodeParent = abstractNode.getParent();
        int counter = 0;
        for (AbstractNode node : nodeParent.getChildrenIt()) {
            if(abstractNode instanceof Property && counter > 20)  // оптимизация
                return nodeParent.getIndexedPropChildren().get((Property) abstractNode);
            counter++;
            if (abstractNode instanceof Property) {
                if (node instanceof Property)
                    if (node == abstractNode) {
                        return counter;
                    }
            } else {
                if (node instanceof AbstractGroup)
                    if (((AbstractGroup) node).getSID().equals(((AbstractGroup) abstractNode).getSID())) {
                        return counter;
                    }
            }
        }
        return 0;
    }

    public void synchronizeTables() {

        ImportField tableSidField = new ImportField(reflectionLM.sidTable);
        ImportField tableKeySidField = new ImportField(reflectionLM.sidTableKey);
        ImportField tableKeyNameField = new ImportField(reflectionLM.nameTableKey);
        ImportField tableKeyClassField = new ImportField(reflectionLM.classTableKey);
        ImportField tableColumnSidField = new ImportField(reflectionLM.sidTableColumn);
        ImportField tableColumnLongSIDField = new ImportField(reflectionLM.longSIDTableColumn); 

        ImportKey<?> tableKey = new ImportKey(reflectionLM.table, reflectionLM.tableSID.getMapping(tableSidField));
        ImportKey<?> tableKeyKey = new ImportKey(reflectionLM.tableKey, reflectionLM.tableKeySID.getMapping(tableKeySidField));
        ImportKey<?> tableColumnKey = new ImportKey(reflectionLM.tableColumn, reflectionLM.tableColumnSID.getMapping(tableColumnLongSIDField));

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<List<Object>> dataKeys = new ArrayList<List<Object>>();
        List<List<Object>> dataProps = new ArrayList<List<Object>>();
        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            Object tableName = dataTable.getName();
            data.add(asList(tableName));
            ImMap<KeyField, ValueClass> classes = dataTable.getClasses().getCommonParent(dataTable.getTableKeys());
            for (KeyField key : dataTable.keys) {
                dataKeys.add(asList(tableName, key.getName(), tableName + "." + key.getName(), classes.get(key).getCaption()));
            }
            for (PropertyField property : dataTable.properties) {
                dataProps.add(asList(tableName, property.getName(), tableName + "." + property.getName()));
            }
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(tableSidField, reflectionLM.sidTable.getMapping(tableKey)));

        List<ImportProperty<?>> propertiesKeys = new ArrayList<ImportProperty<?>>();
        propertiesKeys.add(new ImportProperty(tableKeySidField, reflectionLM.sidTableKey.getMapping(tableKeyKey)));
        propertiesKeys.add(new ImportProperty(tableKeyNameField, reflectionLM.nameTableKey.getMapping(tableKeyKey)));
        propertiesKeys.add(new ImportProperty(tableKeyClassField, reflectionLM.classTableKey.getMapping(tableKeyKey)));
        propertiesKeys.add(new ImportProperty(null, reflectionLM.tableTableKey.getMapping(tableKeyKey), reflectionLM.tableSID.getMapping(tableSidField)));

        List<ImportProperty<?>> propertiesColumns = new ArrayList<ImportProperty<?>>();
        propertiesColumns.add(new ImportProperty(null, reflectionLM.tableTableColumn.getMapping(tableColumnKey), reflectionLM.tableSID.getMapping(tableSidField)));
        propertiesColumns.add(new ImportProperty(tableColumnSidField, reflectionLM.sidTableColumn.getMapping(tableColumnKey)));
        propertiesColumns.add(new ImportProperty(tableColumnLongSIDField, reflectionLM.longSIDTableColumn.getMapping(tableColumnKey)));

        List<ImportDelete> delete = new ArrayList<ImportDelete>();
        delete.add(new ImportDelete(tableKey, LM.is(reflectionLM.table).getMapping(tableKey), false));

        List<ImportDelete> deleteKeys = new ArrayList<ImportDelete>();
        deleteKeys.add(new ImportDelete(tableKeyKey, LM.is(reflectionLM.tableKey).getMapping(tableKeyKey), false));

        List<ImportDelete> deleteColumns = new ArrayList<ImportDelete>();
        deleteColumns.add(new ImportDelete(tableColumnKey, LM.is(reflectionLM.tableColumn).getMapping(tableColumnKey), false));

        ImportTable table = new ImportTable(asList(tableSidField), data);
        ImportTable tableKeys = new ImportTable(asList(tableSidField, tableKeyNameField, tableKeySidField, tableKeyClassField), dataKeys);
        ImportTable tableColumns = new ImportTable(asList(tableSidField, tableColumnSidField, tableColumnLongSIDField), dataProps);

        try {
            DataSession session = createSession();
            session.pushVolatileStats("RM_TE");

            IntegrationService service = new IntegrationService(session, table, asList(tableKey), properties, delete);
            service.synchronize(true, false);

            service = new IntegrationService(session, tableKeys, asList(tableKeyKey), propertiesKeys, deleteKeys);
            service.synchronize(true, false);

            service = new IntegrationService(session, tableColumns, asList(tableColumnKey), propertiesColumns, deleteColumns);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void resetConnectionStatus() {
        try {
            DataSession session = createSession();

            PropertyChange statusChanges = new PropertyChange(systemEventsLM.connectionStatus.getDataObject("disconnectedConnection"),
                    systemEventsLM.connectionStatusConnection.property.interfaces.single(),
                    systemEventsLM.connectionStatus.getDataObject("connectedConnection"));

            session.change((CalcProperty) systemEventsLM.connectionStatusConnection.property, statusChanges);

            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void logLaunch() {
        try {
            DataSession session = createSession();
    
            DataObject newLaunch = session.addObject(systemEventsLM.launch);
            systemEventsLM.computerLaunch.change(getServerComputer(), session, newLaunch);
            systemEventsLM.timeLaunch.change(timeLM.currentDateTime.read(session), session, newLaunch);
            systemEventsLM.revisionLaunch.change(getRevision(), session, newLaunch);
    
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
