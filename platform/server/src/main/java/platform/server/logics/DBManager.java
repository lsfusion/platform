package platform.server.logics;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import platform.base.BaseUtils;
import platform.base.SystemUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.Compare;
import platform.server.AlterationScriptLexer;
import platform.server.AlterationScriptParser;
import platform.server.SystemProperties;
import platform.server.caches.IdentityStrongLazy;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.QueryBuilder;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.navigator.*;
import platform.server.integration.*;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.table.DataTable;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;
import platform.server.mail.NotificationActionProperty;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.*;

import static java.util.Arrays.asList;
import static platform.base.SystemUtils.getRevision;
import static platform.server.logics.ServerResourceBundle.getString;

public class DBManager extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = Logger.getLogger(DBManager.class);

    private static Comparator<DBVersion> dbVersionComparator = new Comparator<DBVersion>() {
        @Override
        public int compare(DBVersion lhs, DBVersion rhs) {
            return lhs.compare(rhs);
        }
    };

    private TreeMap<DBVersion, List<SIDChange>> propertySIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> classSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> tableSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);

    private DataAdapter adapter;

    private BusinessLogics<?> businessLogics;

    private BaseLogicsModule<?> LM;

    private ReflectionLogicsModule reflectionLM;

    private EmailLogicsModule emailLM;

    private SystemEventsLogicsModule systemEventsLM;

    private int systemUserObject;
    private int systemComputer;

    private RestartManager restartManager;

    private final ThreadLocal<SQLSession> threadLocalSql;

    public DBManager() {
        super(DBMANAGER_ORDER);

        threadLocalSql = new ThreadLocal<SQLSession>() {
            @Override
            public SQLSession initialValue() {
                try {
                    return createSQL();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public void setAdapter(DataAdapter adapter) {
        this.adapter = adapter;
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(adapter, "adapter must be specified");
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        this.LM = businessLogics.LM;
        this.reflectionLM = businessLogics.reflectionLM;
        this.emailLM = businessLogics.emailLM;
        this.systemEventsLM = businessLogics.systemEventsLM;
        try {
            logger.info("Synchronizing DB.");
            synchronizeDB();

            setUserLoggableProperties();
            setNotNullProperties();
            setupPropertyNotifications();

            if (!SystemProperties.isDebug) {
                synchronizeForms();
                synchronizeGroupProperties();
                synchronizeProperties();
            }

            synchronizeTables();

            resetConnectionStatus();
            logLaunch();
        } catch (Exception e) {
            throw new RuntimeException("Error synchronizing DB: ", e);
        }
    }

    @IdentityStrongLazy // ресурсы потребляет
    private SQLSession getIDSql() throws SQLException { // подразумевает synchronized использование
        try {
            return createSQL(Connection.TRANSACTION_REPEATABLE_READ);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SQLSession createSQL() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return createSQL(-1);
    }

    public SQLSession createSQL(int isolationLevel) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return new SQLSession(adapter, isolationLevel);
    }

    public SQLSession getThreadLocalSql() {
        return threadLocalSql.get();
    }

    public int generateID() throws RemoteException {
        try {
            return IDTable.instance.generateID(getIDSql(), IDTable.OBJECT);
        } catch (SQLException e) {
            throw new RuntimeException(getString("logics.info.error.reading.user.data"), e);
        }
    }

    public DataSession createSession() throws SQLException {
        return createSession(getThreadLocalSql(),
                             new UserController() {
                                 public void changeCurrentUser(DataObject user) {
                                     throw new RuntimeException("not supported");
                                 }

                                 public DataObject getCurrentUser() {
                                     return new DataObject(systemUserObject, LM.systemUser);
                                 }
                             },
                             new ComputerController() {
                                 public DataObject getCurrentComputer() {
                                     return new DataObject(systemComputer, LM.computer);
                                 }

                                 public boolean isFullClient() {
                                     return false;
                                 }
                             }
        );
    }

    public DataSession createSession(SQLSession sql, UserController userController, ComputerController computerController) throws SQLException {
        //todo: неплохо бы избавиться от зависимости на restartManager, а то она неестественна
        return new DataSession(sql, userController, computerController,
                               new IsServerRestartingController() {
                                   public boolean isServerRestarting() {
                                       return restartManager.isPendingRestart();
                                   }
                               },
                               LM.baseClass, businessLogics.systemEventsLM.session, businessLogics.systemEventsLM.currentSession, getIDSql(), businessLogics.getSessionEvents());
    }


    private void synchronizeForms() {
        synchronizeNavigatorElements(reflectionLM.form, FormEntity.class, false, reflectionLM.isForm);
        synchronizeNavigatorElements(reflectionLM.navigatorAction, NavigatorAction.class, true, reflectionLM.isNavigatorAction);
        synchronizeNavigatorElements(reflectionLM.navigatorElement, NavigatorElement.class, true, reflectionLM.isNavigatorElement);
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
            if (exactJavaClass ? filterJavaClass == element.getClass() : filterJavaClass.isInstance(element)) {
                elementsData.add(asList((Object) element.getSID(), element.caption));
            }
        }

        List<ImportProperty<?>> propsNavigatorElement = new ArrayList<ImportProperty<?>>();
        propsNavigatorElement.add(new ImportProperty(sidField, reflectionLM.sidNavigatorElement.getMapping(keyNavigatorElement)));
        propsNavigatorElement.add(new ImportProperty(captionField, reflectionLM.captionNavigatorElement.getMapping(keyNavigatorElement)));

        List<ImportDelete> deletes = asList(
                new ImportDelete(keyNavigatorElement, deleteLP.getMapping(keyNavigatorElement), false)
        );
        ImportTable table = new ImportTable(asList(sidField, captionField), elementsData);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyNavigatorElement), propsNavigatorElement, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizeParents() {
        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField numberField = new ImportField(reflectionLM.numberNavigatorElement);

        List<List<Object>> dataParents = getRelations(LM.root);

        ImportKey<?> keyElement = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementSID.getMapping(sidField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementSID.getMapping(parentSidField));
        List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
        propsParent.add(new ImportProperty(parentSidField, reflectionLM.parentNavigatorElement.getMapping(keyElement), LM.object(reflectionLM.navigatorElement).getMapping(keyParent)));
        propsParent.add(new ImportProperty(numberField, reflectionLM.numberNavigatorElement.getMapping(keyElement), GroupType.MIN));
        ImportTable table = new ImportTable(asList(sidField, parentSidField, numberField), dataParents);
        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyElement, keyParent), propsParent);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<List<Object>> getRelations(NavigatorElement element) {
        List<List<Object>> parentInfo = new ArrayList<List<Object>>();
        List<NavigatorElement> children = (List<NavigatorElement>) element.getChildren(false);
        int counter = 1;
        for (NavigatorElement child : children) {
            parentInfo.add(BaseUtils.toList((Object) child.getSID(), element.getSID(), counter++));
            parentInfo.addAll(getRelations(child));
        }
        return parentInfo;
    }

    private void synchronizePropertyDraws() {

        List<List<Object>> dataPropertyDraws = new ArrayList<List<Object>>();
        for (FormEntity formElement : businessLogics.getFormEntities()) {
            List<PropertyDrawEntity> propertyDraws = formElement.propertyDraws;
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
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyPropertyDraw, keyGroupObject), propsPropertyDraw, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizeGroupObjects() {

        List<List<Object>> dataGroupObjectList = new ArrayList<List<Object>>();
        for (FormEntity<?> formElement : businessLogics.getFormEntities()) { //formSID - sidGroupObject
            for (PropertyDrawEntity property : formElement.propertyDraws) {
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
            session.pushVolatileStats();

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
        return !LM.isGeneratedSID(property.getSID()) && (property instanceof ActionProperty || property.isFull());
    }

    private void synchronizeProperties() {
        synchronizePropertyEntities();
        synchronizePropertyParents();
    }

    private void synchronizePropertyEntities() {
        ImportField sidPropertyField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField captionPropertyField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField loggablePropertyField = new ImportField(reflectionLM.propertyLoggableValueClass);
        ImportField storedPropertyField = new ImportField(reflectionLM.propertyStoredValueClass);
        ImportField isSetNotNullPropertyField = new ImportField(reflectionLM.propertyIsSetNotNullValueClass);
        ImportField signaturePropertyField = new ImportField(reflectionLM.propertySignatureValueClass);
        ImportField returnPropertyField = new ImportField(reflectionLM.propertySignatureValueClass);
        ImportField classPropertyField = new ImportField(reflectionLM.propertySignatureValueClass);

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertySID.getMapping(sidPropertyField));

        List<List<Object>> dataProperty = new ArrayList<List<Object>>();
        for (Property property : businessLogics.getOrderProperties()) {
            if (needsToBeSynchronized(property)) {
                String commonClasses = "";
                String returnClass = "";
                String classProperty = "";
                try {
                    classProperty = property.getClass().getSimpleName();
                    returnClass = property.getValueClass().getSID();
                    for (Object cc : property.getInterfaceClasses().valueIt()) {
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
                dataProperty.add(asList((Object) property.getSID(), property.caption, property.loggable ? true : null,
                                        property instanceof CalcProperty && ((CalcProperty) property).isStored() ? true : null, property instanceof CalcProperty && ((CalcProperty) property).setNotNull ? true : null, commonClasses, returnClass, classProperty));
            }
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(sidPropertyField, reflectionLM.SIDProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(captionPropertyField, reflectionLM.captionProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(loggablePropertyField, reflectionLM.loggableProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(storedPropertyField, reflectionLM.storedProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(isSetNotNullPropertyField, reflectionLM.isSetNotNullProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(signaturePropertyField, reflectionLM.signatureProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(returnPropertyField, reflectionLM.returnProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(classPropertyField, reflectionLM.classProperty.getMapping(keyProperty)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(keyProperty, LM.is(reflectionLM.property).getMapping(keyProperty), false));

        ImportTable table = new ImportTable(asList(sidPropertyField, captionPropertyField, loggablePropertyField, storedPropertyField, isSetNotNullPropertyField, signaturePropertyField, returnPropertyField, classPropertyField), dataProperty);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyProperty), properties, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizePropertyParents() {
        ImportField sidPropertyField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField numberPropertyField = new ImportField(reflectionLM.numberProperty);
        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);

        List<List<Object>> dataParent = new ArrayList<List<Object>>();
        for (Property property : businessLogics.getOrderProperties()) {
            if (needsToBeSynchronized(property))
                dataParent.add(asList(property.getSID(), (Object) property.getParent().getSID(), getNumberInListOfChildren(property)));
        }

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertySID.getMapping(sidPropertyField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.abstractGroup, reflectionLM.abstractGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        properties.add(new ImportProperty(parentSidField, reflectionLM.parentProperty.getMapping(keyProperty), LM.object(reflectionLM.abstractGroup).getMapping(keyParent)));
        properties.add(new ImportProperty(numberPropertyField, reflectionLM.numberProperty.getMapping(keyProperty)));
        ImportTable table = new ImportTable(asList(sidPropertyField, parentSidField, numberPropertyField), dataParent);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyProperty, keyParent), properties);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void synchronizeGroupProperties() {
        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField captionField = new ImportField(reflectionLM.navigatorElementCaptionClass);
        ImportField numberField = new ImportField(reflectionLM.numberAbstractGroup);

        ImportKey<?> key = new ImportKey(reflectionLM.abstractGroup, reflectionLM.abstractGroupSID.getMapping(sidField));

        List<List<Object>> data = new ArrayList<List<Object>>();

        for (AbstractGroup group : businessLogics.getParentGroups()) {
            data.add(asList(group.getSID(), (Object) group.caption));
        }

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        props.add(new ImportProperty(sidField, reflectionLM.SIDAbstractGroup.getMapping(key)));
        props.add(new ImportProperty(captionField, reflectionLM.captionAbstractGroup.getMapping(key)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(key, LM.is(reflectionLM.abstractGroup).getMapping(key), false));

        ImportTable table = new ImportTable(asList(sidField, captionField), data);

        List<List<Object>> data2 = new ArrayList<List<Object>>();

        for (AbstractGroup group : businessLogics.getParentGroups()) {
            if (group.getParent() != null) {
                data2.add(asList(group.getSID(), (Object) group.getParent().getSID(), getNumberInListOfChildren(group)));
            }
        }

        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportKey<?> key2 = new ImportKey(reflectionLM.abstractGroup, reflectionLM.abstractGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> props2 = new ArrayList<ImportProperty<?>>();
        props2.add(new ImportProperty(parentSidField, reflectionLM.parentAbstractGroup.getMapping(key), LM.object(reflectionLM.abstractGroup).getMapping(key2)));
        props2.add(new ImportProperty(numberField, reflectionLM.numberAbstractGroup.getMapping(key)));
        ImportTable table2 = new ImportTable(asList(sidField, parentSidField, numberField), data2);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

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
        Set<AbstractNode> siblings = abstractNode.getParent().children;
        if(abstractNode instanceof Property && siblings.size() > 20) // оптимизация
            return abstractNode.getParent().getIndexedPropChildren().get(((Property) abstractNode).getSID());

        int counter = 0;
        for (AbstractNode node : siblings) {
            counter++;
            if (abstractNode instanceof Property) {
                if (node instanceof Property)
                    if (((Property) node).getSID().equals(((Property) abstractNode).getSID())) {
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

    private void setUserLoggableProperties() throws SQLException {

        DataSession session = createSession();

        LCP<PropertyInterface> isProperty = LM.is(reflectionLM.property);
        ImRevMap<PropertyInterface, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
        query.addProperty("SIDProperty", reflectionLM.SIDProperty.getExpr(session.getModifier(), key));
        query.addProperty("userLoggableProperty", reflectionLM.userLoggableProperty.getExpr(session.getModifier(), key));
        query.and(isProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (ImMap<Object, Object> values : result.valueIt()) {
            Object userLoggable = values.get("userLoggableProperty");
            if (userLoggable != null) {
                LM.makeUserLoggable(businessLogics.getLCP(values.get("SIDProperty").toString().trim()));
            }
        }
    }

    private void setNotNullProperties() throws SQLException {
        DataSession session = createSession();

        LCP isProperty = LM.is(reflectionLM.property);
        ImRevMap<Object, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("SIDProperty", reflectionLM.SIDProperty.getExpr(session.getModifier(), key));
        query.addProperty("isSetNotNullProperty", reflectionLM.isSetNotNullProperty.getExpr(session.getModifier(), key));
        query.and(isProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (ImMap<Object, Object> values : result.valueIt()) {
            Object isSetNotNull = values.get("isSetNotNullProperty");
            if (isSetNotNull != null) {
                LCP<?> prop = businessLogics.getLCP(values.get("SIDProperty").toString().trim());
                prop.property.setNotNull = true;
                LM.setNotNull(prop);
            }
        }
    }

    public void synchronizeTables() {
        ImportField tableSidField = new ImportField(reflectionLM.sidTable);
        ImportField tableKeySidField = new ImportField(reflectionLM.sidTableKey);
        ImportField tableKeyNameField = new ImportField(reflectionLM.nameTableKey);
        ImportField tableKeyClassField = new ImportField(reflectionLM.classTableKey);
        ImportField tableColumnSidField = new ImportField(reflectionLM.sidTableColumn);

        ImportKey<?> tableKey = new ImportKey(reflectionLM.table, reflectionLM.tableSID.getMapping(tableSidField));
        ImportKey<?> tableKeyKey = new ImportKey(reflectionLM.tableKey, reflectionLM.tableKeySID.getMapping(tableKeySidField));
        ImportKey<?> tableColumnKey = new ImportKey(reflectionLM.tableColumn, reflectionLM.tableColumnSID.getMapping(tableColumnSidField));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            Object tableName = dataTable.name;
            ImMap<KeyField, ValueClass> classes = dataTable.getClasses().getCommonParent(dataTable.getTableKeys());
            for (KeyField key : dataTable.keys) {
                data.add(asList(tableName, key.name, tableName + "." + key.name, classes.get(key).getCaption()));
            }
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(tableSidField, reflectionLM.sidTable.getMapping(tableKey)));
        properties.add(new ImportProperty(tableKeySidField, reflectionLM.sidTableKey.getMapping(tableKeyKey)));
        properties.add(new ImportProperty(tableKeyNameField, reflectionLM.nameTableKey.getMapping(tableKeyKey)));
        properties.add(new ImportProperty(tableSidField, reflectionLM.tableTableKey.getMapping(tableKeyKey), LM.object(reflectionLM.table).getMapping(tableKey)));
        properties.add(new ImportProperty(tableKeyClassField, reflectionLM.classTableKey.getMapping(tableKeyKey)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(tableKey, LM.is(reflectionLM.table).getMapping(tableKey), false));
        deletes.add(new ImportDelete(tableKeyKey, LM.is(reflectionLM.tableKey).getMapping(tableKeyKey), false));

        ImportTable table = new ImportTable(asList(tableSidField, tableKeyNameField, tableKeySidField, tableKeyClassField), data);

        List<List<Object>> data2 = new ArrayList<List<Object>>();
        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            Object tableName = dataTable.name;
            for (PropertyField property : dataTable.properties) {
                data2.add(asList(tableName, property.name));
            }
        }

        List<ImportProperty<?>> properties2 = new ArrayList<ImportProperty<?>>();
        properties2.add(new ImportProperty(tableSidField, reflectionLM.sidTable.getMapping(tableKey)));
        properties2.add(new ImportProperty(tableColumnSidField, reflectionLM.sidTableColumn.getMapping(tableColumnKey)));
        properties2.add(new ImportProperty(tableSidField, reflectionLM.tableTableColumn.getMapping(tableColumnKey), LM.object(reflectionLM.table).getMapping(tableKey)));

        List<ImportDelete> deletes2 = new ArrayList<ImportDelete>();
        deletes2.add(new ImportDelete(tableColumnKey, LM.is(reflectionLM.tableColumn).getMapping(tableColumnKey), false));

        ImportTable table2 = new ImportTable(asList(tableSidField, tableColumnSidField), data2);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(tableKey, tableKeyKey), properties, deletes);
            service.synchronize(true, false);

            service = new IntegrationService(session, table2, asList(tableKey, tableColumnKey), properties2, deletes2);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupPropertyNotifications() throws SQLException {
        DataSession session = createSession();

        LCP isNotification = LM.is(emailLM.notification);
        ImRevMap<Object, KeyExpr> keys = isNotification.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("isDerivedChange", emailLM.isEventNotification.getExpr(session.getModifier(), key));
        query.addProperty("subject", emailLM.subjectNotification.getExpr(session.getModifier(), key));
        query.addProperty("text", emailLM.textNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailFrom", emailLM.emailFromNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailTo", emailLM.emailToNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailToCC", emailLM.emailToCCNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailToBC", emailLM.emailToBCNotification.getExpr(session.getModifier(), key));
        query.and(isNotification.getExpr(key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (int i=0,size=result.size();i<size;i++) {
            DataObject notificationObject = new DataObject(result.getKey(i).getValue(0), emailLM.notification);
            KeyExpr propertyExpr2 = new KeyExpr("property");
            KeyExpr notificationExpr2 = new KeyExpr("notification");
            ImRevMap<String, KeyExpr> newKeys2 = MapFact.toRevMap("property", propertyExpr2, "notification", notificationExpr2);

            QueryBuilder<String, String> query2 = new QueryBuilder<String, String>(newKeys2);
            query2.addProperty("SIDProperty", reflectionLM.SIDProperty.getExpr(session.getModifier(), propertyExpr2));
            query2.and(emailLM.inNotificationProperty.getExpr(session.getModifier(), notificationExpr2, propertyExpr2).getWhere());
            query2.and(notificationExpr2.compare(notificationObject, Compare.EQUALS));
            ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result2 = query2.execute(session.sql);
            List<LCP> listInNotificationProperty = new ArrayList();
            for (int j=0,size2=result2.size();j<size2;j++) {
                listInNotificationProperty.add(businessLogics.getLCP(result2.getValue(i).get("SIDProperty").toString().trim()));
            }
            ImMap<Object, Object> rowValue = result.getValue(i);

            for (LCP prop : listInNotificationProperty) {
                boolean isDerivedChange = rowValue.get("isDerivedChange") == null ? false : true;
                String subject = rowValue.get("subject") == null ? "" : rowValue.get("subject").toString().trim();
                String text = rowValue.get("text") == null ? "" : rowValue.get("text").toString().trim();
                String emailFrom = rowValue.get("emailFrom") == null ? "" : rowValue.get("emailFrom").toString().trim();
                String emailTo = rowValue.get("emailTo") == null ? "" : rowValue.get("emailTo").toString().trim();
                String emailToCC = rowValue.get("emailToCC") == null ? "" : rowValue.get("emailToCC").toString().trim();
                String emailToBC = rowValue.get("emailToBC") == null ? "" : rowValue.get("emailToBC").toString().trim();
                LAP emailNotificationProperty = LM.addProperty(LM.actionGroup, new LAP(new NotificationActionProperty(prop.property.getSID() + "emailNotificationProperty", "emailNotificationProperty", prop, subject, text, emailFrom, emailTo, emailToCC, emailToBC, businessLogics.emailLM)));

                Integer[] params = new Integer[prop.listInterfaces.size()];
                for (int j = 0; j < prop.listInterfaces.size(); j++)
                    params[j] = j + 1;
                if (isDerivedChange)
                    emailNotificationProperty.setEventAction(LM, prop, params);
                else
                    emailNotificationProperty.setEventSetAction(LM, prop, params);
            }
        }
    }

    private void resetConnectionStatus() {
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

    private void logLaunch() throws SQLException {
        DataSession session = createSession();

        DataObject newLaunch = session.addObject(systemEventsLM.launch);
        systemEventsLM.computerLaunch.change(getServerComputer(), session, newLaunch);
        systemEventsLM.timeLaunch.change(LM.currentDateTime.read(session), session, newLaunch);
        systemEventsLM.revisionLaunch.change(getRevision(), session, newLaunch);

        session.apply(businessLogics);
        session.close();
    }

    public Integer getServerComputer() {
        return getComputer(SystemUtils.getLocalHostName());
    }

    public DataObject getServerComputerObject() {
        return new DataObject(getComputer(SystemUtils.getLocalHostName()), businessLogics.LM.computer);
    }

    public Integer getComputer(String strHostName) {
        try {
            DataSession session = createSession();

            QueryBuilder<String, Object> q = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            q.and(
                    LM.hostname.getExpr(
                            session.getModifier(), q.getMapExprs().get("key")
                    ).compare(new DataObject(strHostName), Compare.EQUALS)
            );

            Integer result;

            ImSet<ImMap<String, Object>> keys = q.execute(session).keys();
            if (keys.size() == 0) {
                DataObject addObject = session.addObject(LM.computer);
                LM.hostname.change(strHostName, session, addObject);

                result = (Integer) addObject.object;
                session.apply(businessLogics);
            } else {
                result = (Integer) keys.iterator().next().get("key");
            }

            session.close();
            logger.debug("Begin user session " + strHostName + " " + result);
            return result;
        } catch (Exception e) {
            logger.error("Error reading computer: ", e);
            throw new RuntimeException(e);
        }
    }

    private void synchronizeDB() throws SQLException, IOException {
        SQLSession sql = getThreadLocalSql();

        // инициализируем таблицы
        LM.tableFactory.fillDB(sql, LM.baseClass);

        // "старое" состояние базы
        DataInputStream inputDB = null;
        byte[] struct = (byte[]) sql.readRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), StructTable.instance.struct);
        if (struct != null)
            inputDB = new DataInputStream(new ByteArrayInputStream(struct));

        DBStructure oldDBStructure = new DBStructure(inputDB);

        runAlterationScript();

        sql.startTransaction();

        // новое состояние базы
        ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
        DataOutputStream outDB = new DataOutputStream(outDBStruct);

        DBVersion newDBVersion = getCurrentDBVersion(oldDBStructure.dbVersion);
        DBStructure newDBStructure = new DBStructure(newDBVersion);
        // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
        newDBStructure.write(outDB);

        for (Map.Entry<Table, Map<List<String>, Boolean>> oldTableIndices : oldDBStructure.tables.entrySet()) {
            Table oldTable = oldTableIndices.getKey();
            Table newTable = newDBStructure.getTable(oldTableIndices.getKey().name);
            Map<List<String>, Boolean> newTableIndices = newDBStructure.tables.get(newTable);

            for (Map.Entry<List<String>, Boolean> oldIndex : oldTableIndices.getValue().entrySet()) {
                List<String> oldIndexKeys = oldIndex.getKey();
                boolean oldOrder = oldIndex.getValue();
                boolean drop = (newTable == null); // ушла таблица
                if (!drop) {
                    Boolean newOrder = newTableIndices.get(oldIndexKeys);
                    if (newOrder != null && newOrder.equals(oldOrder)) {
                        newTableIndices.remove(oldIndexKeys); // не трогаем индекс
                    } else {
                        drop = true;
                    }
                }
                if (drop) {
//                    sql.dropIndex(oldTable.name, oldTable.keys, SetFact.fromJavaOrderSet(oldIndexKeys), oldOrder);
                }
            }
        }

        alterateDBStructure(oldDBStructure, sql);

        // добавим таблицы которых не было
        for (Table table : newDBStructure.tables.keySet()) {
            if (oldDBStructure.getTable(table.name) == null)
                sql.createTable(table.name, table.keys);
        }

        MSet<ImplementTable> mPackTables = SetFact.mSet();

        // бежим по свойствам
        Map<String, String> columnsToDrop = new HashMap<String, String>();
        for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
            Table oldTable = oldDBStructure.getTable(oldProperty.tableName);

            boolean keep = false, moved = false;
            for (Iterator<DBStoredProperty> is = newDBStructure.storedProperties.iterator(); is.hasNext(); ) {
                DBStoredProperty newProperty = is.next();

                if (newProperty.sID.equals(oldProperty.sID)) {
                    MExclMap<KeyField, PropertyInterface> mFoundInterfaces = MapFact.mExclMapMax(newProperty.property.interfaces.size());
                    for (PropertyInterface propertyInterface : newProperty.property.interfaces) {
                        KeyField mapKeyField = oldProperty.mapKeys.get(propertyInterface.ID);
                        if (mapKeyField != null)
                            mFoundInterfaces.exclAdd(mapKeyField, propertyInterface);
                    }
                    ImMap<KeyField, PropertyInterface> foundInterfaces = mFoundInterfaces.immutable();

                    if (foundInterfaces.size() == oldProperty.mapKeys.size()) { // если все нашли
                        if (!(keep = newProperty.tableName.equals(oldProperty.tableName))) { // если в другой таблице
                            sql.addColumn(newProperty.tableName, newProperty.property.field);
                            // делаем запрос на перенос

                            logger.info(getString("logics.info.property.is.transferred.from.table.to.table", newProperty.property.field, newProperty.property.caption, oldProperty.tableName, newProperty.tableName));
                            newProperty.property.mapTable.table.moveColumn(sql, newProperty.property.field, oldTable,
                                                                           foundInterfaces.join((ImMap<PropertyInterface, KeyField>) newProperty.property.mapTable.mapKeys), oldTable.findProperty(oldProperty.sID));
                            logger.info("Done");
                            moved = true;
                        } else { // надо проверить что тип не изменился
                            if (!oldTable.findProperty(oldProperty.sID).type.equals(newProperty.property.field.type))
                                sql.modifyColumn(newProperty.property.mapTable.table.name, newProperty.property.field, oldTable.findProperty(oldProperty.sID).type);
                        }
                        is.remove();
                    }
                    break;
                }
            }
            if (!keep) {
                if (oldProperty.isDataProperty && !moved) {
                    String newName = oldProperty.sID + "_deleted";
                    Savepoint savepoint = sql.getConnection().setSavepoint();
                    try {
                        savepoint = sql.getConnection().setSavepoint();
                        sql.renameColumn(oldProperty.tableName, oldProperty.sID, newName);
                        columnsToDrop.put(newName, oldProperty.tableName);
                    } catch (PSQLException e) { // колонка с новым именем (с '_deleted') уже существует
                        sql.getConnection().rollback(savepoint);
                        sql.dropColumn(oldTable.name, oldProperty.sID);
                        ImplementTable table = (ImplementTable) newDBStructure.getTable(oldTable.name);
                        if (table != null) mPackTables.add(table);
                    }
                } else {
                    sql.dropColumn(oldTable.name, oldProperty.sID);
                    ImplementTable table = (ImplementTable) newDBStructure.getTable(oldTable.name); // надо упаковать таблицу если удалили колонку
                    if (table != null) mPackTables.add(table);
                }
            }
        }

        List<AggregateProperty> recalculateProperties = new ArrayList<AggregateProperty>();
        for (DBStoredProperty property : newDBStructure.storedProperties) { // добавляем оставшиеся
            sql.addColumn(property.tableName, property.property.field);
            if (struct != null && property.property instanceof AggregateProperty) // если все свойства "новые" то ничего перерасчитывать не надо
                recalculateProperties.add((AggregateProperty) property.property);
        }

        // удаляем таблицы старые
        for (Table table : oldDBStructure.tables.keySet()) {
            if (newDBStructure.getTable(table.name) == null) {
                sql.dropTable(table.name);
            }
        }

        packTables(sql, mPackTables.immutable()); // упакуем таблицы

        updateStats();  // пересчитаем статистику

        // создадим индексы в базе
        for (Map.Entry<Table, Map<List<String>, Boolean>> mapIndex : newDBStructure.tables.entrySet())
            for (Map.Entry<List<String>, Boolean> index : mapIndex.getValue().entrySet())
                sql.addIndex(mapIndex.getKey().name, mapIndex.getKey().keys, SetFact.fromJavaOrderSet(index.getKey()), index.getValue());

        try {
            sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject((Object) outDBStruct.toByteArray(), ByteArrayClass.instance)), true);
        } catch (Exception e) {
            ImMap<PropertyField, ObjectValue> propFields = MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject((Object) new byte[0], ByteArrayClass.instance));
            sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), propFields, true);
        }

        fillIDs(getChangesAfter(oldDBStructure.dbVersion, classSIDChanges));

        updateClassStat(sql);

        recalculateAggregations(sql, recalculateProperties); // перерасчитаем агрегации
//        recalculateAggregations(sql, getAggregateStoredProperties());

        sql.commitTransaction();

        DataSession session = createSession();
        for (String sid : columnsToDrop.keySet()) {
            DataObject object = session.addObject(reflectionLM.dropColumn);
            reflectionLM.sidDropColumn.change(sid, session, object);
            reflectionLM.sidTableDropColumn.change(columnsToDrop.get(sid), session, object);
            reflectionLM.timeDropColumn.change(new Timestamp(Calendar.getInstance().getTimeInMillis()), session, object);
            reflectionLM.revisionDropColumn.change(getRevision(), session, object);
        }
        session.apply(businessLogics);
        session.close();

        initSystemUser();
    }

    private void fillIDs(Map<String, String> sIDChanges) throws SQLException {
        DataSession session = createSession();

        LM.baseClass.fillIDs(session, LM.name, LM.classSID, sIDChanges);

        session.apply(businessLogics);

        session.close();
    }

    private void updateClassStat(SQLSession session) throws SQLException {
        LM.baseClass.updateClassStat(session);
    }

    public String checkAggregations(SQLSession session) throws SQLException {
        String message = "";
        for (AggregateProperty property : businessLogics.getAggregateStoredProperties())
            message += property.checkAggregation(session);
        return message;
    }

    public void recalculateAggregations(SQLSession session) throws SQLException {
        recalculateAggregations(session, businessLogics.getAggregateStoredProperties());
    }

    public void recalculateAggregations(SQLSession session, List<AggregateProperty> recalculateProperties) throws SQLException {
        for (AggregateProperty property : recalculateProperties)
            property.recalculateAggregation(session);
    }

    public void recalculateAggregationTableColumn(SQLSession session, String propertySID) throws SQLException {
        for (CalcProperty property : businessLogics.getAggregateStoredProperties())
            if (property.getSID().equals(propertySID)) {
                AggregateProperty aggregateProperty = (AggregateProperty) property;
                aggregateProperty.recalculateAggregation(session);
            }
    }

    private void runAlterationScript() {
        try {
            InputStream scriptStream = getClass().getResourceAsStream("/migration.script");
            if (scriptStream != null) {
                ANTLRInputStream stream = new ANTLRInputStream(scriptStream);
                AlterationScriptLexer lexer = new AlterationScriptLexer(stream);
                AlterationScriptParser parser = new AlterationScriptParser(new CommonTokenStream(lexer));

                parser.self = this;

                parser.script();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void alterateDBStructure(DBStructure data, SQLSession sql) throws SQLException {
        Map<String, String> propertyChanges = getChangesAfter(data.dbVersion, propertySIDChanges);
        Map<String, String> tableChanges = getChangesAfter(data.dbVersion, tableSIDChanges);

        for (DBStoredProperty oldProperty : data.storedProperties) {
            if (propertyChanges.containsKey(oldProperty.sID)) {
                String newSID = propertyChanges.get(oldProperty.sID);
                sql.renameColumn(oldProperty.tableName, oldProperty.sID, newSID);
                oldProperty.sID = newSID;
            }

            if (tableChanges.containsKey(oldProperty.tableName)) {
                oldProperty.tableName = tableChanges.get(oldProperty.tableName);
            }
        }

        for (Table table : data.tables.keySet()) {
            for (PropertyField field : table.properties) {
                if (propertyChanges.containsKey(field.name)) {
                    field.name = propertyChanges.get(field.name);
                }
            }

            if (tableChanges.containsKey(table.name)) {
                String newSID = tableChanges.get(table.name);
                sql.renameTable(table.name, newSID);
                table.name = newSID;
            }
        }
    }

    private DBVersion getCurrentDBVersion(DBVersion oldVersion) {
        DBVersion curVersion = oldVersion;
        if (!propertySIDChanges.isEmpty() && curVersion.compare(propertySIDChanges.lastKey()) < 0) {
            curVersion = propertySIDChanges.lastKey();
        }
        if (!classSIDChanges.isEmpty() && curVersion.compare(classSIDChanges.lastKey()) < 0) {
            curVersion = classSIDChanges.lastKey();
        }
        if (!tableSIDChanges.isEmpty() && curVersion.compare(tableSIDChanges.lastKey()) < 0) {
            curVersion = tableSIDChanges.lastKey();
        }
        return curVersion;
    }

    private void addSIDChange(TreeMap<DBVersion, List<SIDChange>> sidChanges, String version, String oldSID, String newSID) {
        DBVersion dbVersion = new DBVersion(version);
        if (!sidChanges.containsKey(dbVersion)) {
            sidChanges.put(dbVersion, new ArrayList<SIDChange>());
        }
        sidChanges.get(dbVersion).add(new SIDChange(oldSID, newSID));
    }

    public void addPropertySIDChange(String version, String oldSID, String newSID) {
        addSIDChange(propertySIDChanges, version, oldSID, newSID);
    }

    public void addClassSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(classSIDChanges, version, oldSID, newSID);
    }

    public void addTableSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(tableSIDChanges, version, oldSID, newSID);
    }

    private Map<String, String> getChangesAfter(DBVersion versionAfter, TreeMap<DBVersion, List<SIDChange>> allChanges) {
        Map<String, String> resultChanges = new HashMap<String, String>();

        for (Map.Entry<DBVersion, List<SIDChange>> changesEntry : allChanges.entrySet()) {
            if (changesEntry.getKey().compare(versionAfter) > 0) {
                List<SIDChange> versionChanges = changesEntry.getValue();
                Map<String, String> versionChangesMap = new HashMap<String, String>();

                for (SIDChange change : versionChanges) {
                    if (versionChangesMap.containsKey(change.oldSID)) {
                        throw new RuntimeException(String.format("Renaming '%s' twice in version %s.", change.oldSID, changesEntry.getKey()));
                    } else if (resultChanges.containsKey(change.oldSID)) {
                        throw new RuntimeException(String.format("Renaming '%s' twice.", change.oldSID)); // todo [dale]: временно
                    }
                    versionChangesMap.put(change.oldSID, change.newSID);
                }

                for (Map.Entry<String, String> currentChanges : resultChanges.entrySet()) {
                    String renameTo = currentChanges.getValue();
                    if (versionChangesMap.containsKey(renameTo)) {
                        currentChanges.setValue(versionChangesMap.get(renameTo));
                        versionChangesMap.remove(renameTo);
                    }
                }

                resultChanges.putAll(versionChangesMap);

                Set<String> renameToSIDs = new HashSet<String>();
                for (String renameTo : resultChanges.values()) {
                    if (renameToSIDs.contains(renameTo)) {
                        throw new RuntimeException(String.format("Renaming to '%s' twice.", renameTo));
                    }
                    renameToSIDs.add(renameTo);
                }
            }
        }
        return resultChanges;
    }

    public boolean backupDB(String binPath, String dumpDir) throws IOException, InterruptedException {
        return adapter.backupDB(binPath, dumpDir);
    }

    public void analyzeDB(SQLSession session) throws SQLException {
        session.executeDDL(adapter.getAnalyze());
    }

    public void packTables(SQLSession session, ImCol<ImplementTable> tables) throws SQLException {
        for (Table table : tables) {
            logger.debug(getString("logics.info.packing.table") + " (" + table + ")... ");
            session.packTable(table);
            logger.debug("Done");
        }
    }

    public void dropColumn(String tableName, String columnName) throws SQLException {
        SQLSession sql = getThreadLocalSql();
        sql.startTransaction();
        try {
            sql.dropColumn(tableName, columnName);
            ImplementTable table = LM.tableFactory.getImplementTablesMap().get(tableName); // надо упаковать таблицу, если удалили колонку
            if (table != null)
                sql.packTable(table);
            sql.commitTransaction();
        } catch(SQLException e) {
            sql.rollbackTransaction();
            throw e;
        }
    }

    private void updateStats() throws SQLException {
        updateStats(true); // чтобы сами таблицы статистики получили статистику
        if (!SystemProperties.doNotCalculateStats)
            updateStats(false);
    }

    private ImMap<String, Integer> readStatsFromDB(DataSession session, LCP sIDProp, LCP statsProp) throws SQLException {
        QueryBuilder<String, String> query = new QueryBuilder<String, String>(SetFact.toSet("key"));
        Expr sidToObject = sIDProp.getExpr(session.getModifier(), query.getMapExprs().singleValue());
        query.and(sidToObject.getWhere());
        query.addProperty("property", statsProp.getExpr(session.getModifier(), sidToObject));
        return query.execute(session).getMap().mapKeyValues(new GetValue<String, ImMap<String, Object>>() {
                                                                public String getMapValue(ImMap<String, Object> key) {
                                                                    return ((String) key.singleValue()).trim();
                                                                }}, new GetValue<Integer, ImMap<String, Object>>() {
                                                                public Integer getMapValue(ImMap<String, Object> value) {
                                                                    return (Integer)value.singleValue();
                                                                }});
    }

    public void updateStats(boolean statDefault) throws SQLException {
        DataSession session = createSession();

        ImMap<String, Integer> tableStats;
        ImMap<String, Integer> keyStats;
        ImMap<String, Integer> propStats;
        if(statDefault) {
            tableStats = MapFact.EMPTY();
            keyStats = MapFact.EMPTY();
            propStats = MapFact.EMPTY();
        } else {
            tableStats = readStatsFromDB(session, reflectionLM.tableSID, reflectionLM.rowsTable);
            keyStats = readStatsFromDB(session, reflectionLM.tableKeySID, reflectionLM.quantityTableKey);
            propStats = readStatsFromDB(session, reflectionLM.tableColumnSID, reflectionLM.quantityTableColumn);
        }

        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            dataTable.updateStat(tableStats, keyStats, propStats, statDefault);
        }
    }

    private void initSystemUser() {
        // считаем системного пользователя
        try {
            DataSession session = createSession();

            QueryBuilder<String, Object> query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(query.getMapExprs().singleValue().isClass(LM.systemUser));
            ImOrderSet<ImMap<String, Object>> rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                systemUserObject = (Integer) session.addObject(LM.systemUser).object;
                session.apply(businessLogics);
            } else
                systemUserObject = (Integer) rows.single().get("key");

            query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(LM.hostname.getExpr(session.getModifier(), query.getMapExprs().singleValue()).compare(new DataObject("systemhost"), Compare.EQUALS));
            rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                DataObject computerObject = session.addObject(LM.computer);
                systemComputer = (Integer) computerObject.object;
                LM.hostname.change("systemhost", session, computerObject);
                session.apply(businessLogics);
            } else
                systemComputer = (Integer) rows.single().get("key");

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class DBStoredProperty {
        public String sID;
        public Boolean isDataProperty;
        public String tableName;
        public ImMap<Integer, KeyField> mapKeys;
        public CalcProperty<?> property = null;

        public DBStoredProperty(CalcProperty<?> property) {
            this.sID = property.getSID();
            this.isDataProperty = property instanceof DataProperty;
            this.tableName = property.mapTable.table.name;
            mapKeys = ((CalcProperty<PropertyInterface>)property).mapTable.mapKeys.mapKeys(new GetValue<Integer, PropertyInterface>() {
                public Integer getMapValue(PropertyInterface value) {
                    return value.ID;
                }});
            this.property = property;
        }

        public DBStoredProperty(String sID, Boolean isDataProperty, String tableName, ImMap<Integer, KeyField> mapKeys) {
            this.sID = sID;
            this.isDataProperty = isDataProperty;
            this.tableName = tableName;
            this.mapKeys = mapKeys;
        }
    }

    private class DBStructure {
        public int version;
        public DBVersion dbVersion;
        public Map<Table, Map<List<String>, Boolean>> tables = new HashMap<Table, Map<List<String>, Boolean>>();
        public List<DBStoredProperty> storedProperties = new ArrayList<DBStoredProperty>();

        public DBStructure(DBVersion dbVersion) {
            version = 3;
            this.dbVersion = dbVersion;

            for (Table table : LM.tableFactory.getImplementTablesMap().valueIt()) {
                tables.put(table, new HashMap<List<String>, Boolean>());
            }

            for (Map.Entry<List<? extends CalcProperty>, Boolean> index : LM.indexes.entrySet()) {
                Iterator<? extends CalcProperty> i = index.getKey().iterator();
                if (!i.hasNext())
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.empty.indexes"));
                CalcProperty baseProperty = i.next();
                if (!baseProperty.isStored())
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties") + " (" + baseProperty + ")");

                ImplementTable indexTable = baseProperty.mapTable.table;

                List<String> tableIndex = new ArrayList<String>();
                tableIndex.add(baseProperty.field.name);

                while (i.hasNext()) {
                    CalcProperty property = i.next();
                    if (!property.isStored())
                        throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties") + " (" + baseProperty + ")");
                    if (indexTable.findProperty(property.field.name) == null)
                        throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.properties.in.different.tables", baseProperty, property));
                    tableIndex.add(property.field.name);
                }
                tables.get(indexTable).put(tableIndex, index.getValue());
            }

            for (CalcProperty<?> property : businessLogics.getStoredProperties()) {
                storedProperties.add(new DBStoredProperty(property));
            }
        }

        public DBStructure(DataInputStream inputDB) throws IOException {
            if (inputDB == null) {
                version = -2;
                dbVersion = new DBVersion("0.0");
            } else {
                version = inputDB.read() - 'v';
                if (version < 0) {
                    inputDB.reset();
                }

                if (version > 2) {
                    dbVersion = new DBVersion(inputDB.readUTF());
                } else {
                    dbVersion = new DBVersion("0.0");
                }

                for (int i = inputDB.readInt(); i > 0; i--) {
                    SerializedTable prevTable = new SerializedTable(inputDB, LM.baseClass, version);
                    Map<List<String>, Boolean> indices = new HashMap<List<String>, Boolean>();
                    for (int j = inputDB.readInt(); j > 0; j--) {
                        List<String> index = new ArrayList<String>();
                        for (int k = inputDB.readInt(); k > 0; k--) {
                            index.add(inputDB.readUTF());
                        }
                        boolean prevOrdered = version >= 1 && inputDB.readBoolean();
                        indices.put(index, prevOrdered);
                    }
                    tables.put(prevTable, indices);
                }

                int prevStoredNum = inputDB.readInt();
                for (int i = 0; i < prevStoredNum; i++) {
                    String sID = inputDB.readUTF();
                    boolean isDataProperty = true;
                    if (version >= 0) {
                        isDataProperty = inputDB.readBoolean();
                    }
                    String tableName = inputDB.readUTF();
                    Table prevTable = getTable(tableName);
                    MExclMap<Integer, KeyField> mMapKeys = MapFact.mExclMap(prevTable.getTableKeys().size());
                    for (int j = 0; j < prevTable.getTableKeys().size(); j++) {
                        mMapKeys.exclAdd(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));
                    }
                    storedProperties.add(new DBStoredProperty(sID, isDataProperty, tableName, mMapKeys.immutable()));
                }
            }
        }

        public Table getTable(String name) {
            for (Table table : tables.keySet()) {
                if (table.name.equals(name)) {
                    return table;
                }
            }
            return null;
        }

        public void write(DataOutputStream outDB) throws IOException {
            outDB.write('v' + version);  //для поддержки обратной совместимости
            if (version > 2) {
                outDB.writeUTF(dbVersion.toString());
            }

            outDB.writeInt(tables.size());
            for (Map.Entry<Table, Map<List<String>, Boolean>> tableIndices : tables.entrySet()) {
                tableIndices.getKey().serialize(outDB);
                outDB.writeInt(tableIndices.getValue().size());
                for (Map.Entry<List<String>, Boolean> index : tableIndices.getValue().entrySet()) {
                    outDB.writeInt(index.getKey().size());
                    for (String indexField : index.getKey()) {
                        outDB.writeUTF(indexField);
                    }
                    outDB.writeBoolean(index.getValue());
                }
            }

            outDB.writeInt(storedProperties.size());
            for (DBStoredProperty property : storedProperties) {
                outDB.writeUTF(property.sID);
                outDB.writeBoolean(property.isDataProperty);
                outDB.writeUTF(property.tableName);
                for (int i=0,size=property.mapKeys.size();i<size;i++) {
                    outDB.writeInt(property.mapKeys.getKey(i));
                    outDB.writeUTF(property.mapKeys.getValue(i).name);
                }
            }
        }
    }

    public static class SIDChange {
        public String oldSID;
        public String newSID;

        public SIDChange(String oldSID, String newSID) {
            this.oldSID = oldSID;
            this.newSID = newSID;
        }
    }

    public static class DBVersion {
        private List<Integer> version;

        public DBVersion(String version) {
            this.version = versionToList(version);
        }

        public static List<Integer> versionToList(String version) {
            String[] splittedArr = version.split("\\.");
            List<Integer> intVersion = new ArrayList<Integer>();
            for (String part : splittedArr) {
                intVersion.add(Integer.parseInt(part));
            }
            return intVersion;
        }

        public int compare(DBVersion rhs) {
            return compareVersions(version, rhs.version);
        }

        public static int compareVersions(List<Integer> lhs, List<Integer> rhs) {
            for (int i = 0; i < Math.max(lhs.size(), rhs.size()); i++) {
                int left = (i < lhs.size() ? lhs.get(i) : 0);
                int right = (i < rhs.size() ? rhs.get(i) : 0);
                if (left < right) return -1;
                if (left > right) return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < version.size(); i++) {
                if (i > 0) {
                    buf.append(".");
                }
                buf.append(version.get(i));
            }
            return buf.toString();
        }
    }
}
