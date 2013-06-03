package lsfusion.server.logics;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMap;
import lsfusion.base.col.implementations.abs.ASet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.Compare;
import lsfusion.server.AlterationScriptLexer;
import lsfusion.server.AlterationScriptParser;
import lsfusion.server.ServerLoggers;
import lsfusion.server.SystemProperties;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.navigator.*;
import lsfusion.server.integration.*;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.property.group.AbstractNode;
import lsfusion.server.logics.table.IDTable;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.mail.NotificationActionProperty;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.*;

import static java.util.Arrays.asList;
import static lsfusion.base.SystemUtils.getRevision;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class DBManager extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = Logger.getLogger(DBManager.class);
    private static final Logger systemLogger = ServerLoggers.systemLogger;

    private static Comparator<DBVersion> dbVersionComparator = new Comparator<DBVersion>() {
        @Override
        public int compare(DBVersion lhs, DBVersion rhs) {
            return lhs.compare(rhs);
        }
    };

    private TreeMap<DBVersion, List<SIDChange>> propertySIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> classSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> tableSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> objectSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);

    private DataAdapter adapter;

    private RestartManager restartManager;

    private BusinessLogics<?> businessLogics;

    private boolean ignoreMigration;

    private BaseLogicsModule<?> LM;

    private ReflectionLogicsModule reflectionLM;

    private EmailLogicsModule emailLM;

    private SystemEventsLogicsModule systemEventsLM;

    private TimeLogicsModule timeLM;

    private int systemUserObject;
    private int systemComputer;

    private final ThreadLocal<SQLSession> threadLocalSql;

    private final Map<List<? extends CalcProperty>, Boolean> indexes = new HashMap<List<? extends CalcProperty>, Boolean>();

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

    public void setIgnoreMigration(boolean ignoreMigration) {
        this.ignoreMigration = ignoreMigration;
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
        this.timeLM = businessLogics.timeLM;
        try {
            systemLogger.info("Synchronizing DB.");
            synchronizeDB();

            systemLogger.info("Setting user logging for properties");
            setUserLoggableProperties();
            systemLogger.info("Setting user not null constraints for properties");
            setNotNullProperties();
            systemLogger.info("Setting user notifications for property changes");
            setupPropertyNotifications();

            if (!SystemProperties.isDebug) {
                systemLogger.info("Synchronizing forms");
                synchronizeForms();
                systemLogger.info("Synchronizing groups of properties");
                synchronizeGroupProperties();
                systemLogger.info("Synchronizing properties");
                synchronizeProperties();
            }

            systemLogger.info("Synchronizing tables");
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
                                     return new DataObject(systemUserObject, businessLogics.authenticationLM.systemUser);
                                 }
                             },
                             new ComputerController() {
                                 public DataObject getCurrentComputer() {
                                     return new DataObject(systemComputer, businessLogics.authenticationLM.computer);
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

        List<List<Object>> dataParents = getRelations(LM.root, getElementsWithParent(LM.root));

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

    protected Set<String> getElementsWithParent(NavigatorElement element) {
        Set<String> parentInfo = new HashSet<String>();
        List<NavigatorElement> children = (List<NavigatorElement>) element.getChildren(false);
        parentInfo.add(element.getSID());
        for (NavigatorElement child : children) {
            parentInfo.add(child.getSID());
            parentInfo.addAll(getElementsWithParent(child));
        }
        return parentInfo;
    }

    protected List<List<Object>> getRelations(NavigatorElement element, Set<String> elementsWithParent) {
        List<List<Object>> parentInfo = new ArrayList<List<Object>>();
        List<NavigatorElement> children = (List<NavigatorElement>) element.getChildren(false);
        int counter = 1;
        for (NavigatorElement child : children) {
            parentInfo.add(BaseUtils.toList((Object) child.getSID(), element.getSID(), counter++));
            parentInfo.addAll(getRelations(child));
        }
        counter = 1;
        for(NavigatorElement navigatorElement : businessLogics.getNavigatorElements()) {
            if(!elementsWithParent.contains(navigatorElement.getSID()))
                parentInfo.add(BaseUtils.toList((Object) navigatorElement.getSID(), "noParentGroup", counter++));
        }
        return parentInfo;
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
        return !LM.isGeneratedSID(property.getSID()) && (property instanceof ActionProperty || (((CalcProperty)property).isFull() && !(((CalcProperty)property).isEmpty())));
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
        ImportField complexityPropertyField = new ImportField(LongClass.instance);

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertySID.getMapping(sidPropertyField));

        List<List<Object>> dataProperty = new ArrayList<List<Object>>();
        for (Property property : businessLogics.getOrderProperties()) {
            if (needsToBeSynchronized(property)) {
                String commonClasses = "";
                String returnClass = "";
                String classProperty = "";
                Long complexityProperty = null;
                try {
                    classProperty = property.getClass().getSimpleName();
                    if(property instanceof CalcProperty) {
                        complexityProperty = ((CalcProperty)property).getExpr(((CalcProperty)property).getMapKeys(), Property.defaultModifier).getComplexity(false);
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
                dataProperty.add(asList((Object) property.getSID(), property.caption, property.loggable ? true : null,
                        property instanceof CalcProperty && ((CalcProperty) property).isStored() ? true : null,
                        property instanceof CalcProperty && ((CalcProperty) property).setNotNull ? true : null,
                        commonClasses, returnClass, classProperty, complexityProperty));
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
        properties.add(new ImportProperty(complexityPropertyField, reflectionLM.complexityProperty.getMapping(keyProperty)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(keyProperty, LM.is(reflectionLM.property).getMapping(keyProperty), false));

        ImportTable table = new ImportTable(asList(sidPropertyField, captionPropertyField, loggablePropertyField,
                storedPropertyField, isSetNotNullPropertyField, signaturePropertyField, returnPropertyField,
                classPropertyField, complexityPropertyField), dataProperty);

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
        ImportKey<?> keyParent = new ImportKey(reflectionLM.propertyGroup, reflectionLM.propertyGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        properties.add(new ImportProperty(parentSidField, reflectionLM.parentProperty.getMapping(keyProperty), LM.object(reflectionLM.propertyGroup).getMapping(keyParent)));
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
        query.and(reflectionLM.userLoggableProperty.getExpr(session.getModifier(), key).getWhere());
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (ImMap<Object, Object> values : result.valueIt()) {
            LM.makeUserLoggable(systemEventsLM, businessLogics.getLCP(values.get("SIDProperty").toString().trim()));
        }
    }

    private void setNotNullProperties() throws SQLException {
        DataSession session = createSession();

        LCP isProperty = LM.is(reflectionLM.property);
        ImRevMap<Object, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("SIDProperty", reflectionLM.SIDProperty.getExpr(session.getModifier(), key));
        query.and(reflectionLM.isSetNotNullProperty.getExpr(session.getModifier(), key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (ImMap<Object, Object> values : result.valueIt()) {
            LCP<?> prop = businessLogics.getLCP(values.get("SIDProperty").toString().trim());
            prop.property.setNotNull = true;
            LM.setNotNull(prop);
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
        List<List<Object>> dataKeys = new ArrayList<List<Object>>();
        List<List<Object>> dataProps = new ArrayList<List<Object>>();
        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            Object tableName = dataTable.name;
            data.add(asList(tableName));
            ImMap<KeyField, ValueClass> classes = dataTable.getClasses().getCommonParent(dataTable.getTableKeys());
            for (KeyField key : dataTable.keys) {
                dataKeys.add(asList(tableName, key.name, tableName + "." + key.name, classes.get(key).getCaption()));
            }
            for (PropertyField property : dataTable.properties) {
                dataProps.add(asList(tableName, property.name));
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
        propertiesColumns.add(new ImportProperty(tableColumnSidField, reflectionLM.sidTableColumn.getMapping(tableColumnKey)));
        propertiesColumns.add(new ImportProperty(null, reflectionLM.tableTableColumn.getMapping(tableColumnKey), reflectionLM.tableSID.getMapping(tableSidField)));

        List<ImportDelete> delete = new ArrayList<ImportDelete>();
        delete.add(new ImportDelete(tableKey, LM.is(reflectionLM.table).getMapping(tableKey), false));

        List<ImportDelete> deleteKeys = new ArrayList<ImportDelete>();
        deleteKeys.add(new ImportDelete(tableKeyKey, LM.is(reflectionLM.tableKey).getMapping(tableKeyKey), false));

        List<ImportDelete> deleteColumns = new ArrayList<ImportDelete>();
        deleteColumns.add(new ImportDelete(tableColumnKey, LM.is(reflectionLM.tableColumn).getMapping(tableColumnKey), false));

        ImportTable table = new ImportTable(asList(tableSidField), data);
        ImportTable tableKeys = new ImportTable(asList(tableSidField, tableKeyNameField, tableKeySidField, tableKeyClassField), dataKeys);
        ImportTable tableColumns = new ImportTable(asList(tableSidField, tableColumnSidField), dataProps);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

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
        systemEventsLM.timeLaunch.change(timeLM.currentDateTime.read(session), session, newLaunch);
        systemEventsLM.revisionLaunch.change(getRevision(), session, newLaunch);

        session.apply(businessLogics);
        session.close();
    }

    public Integer getServerComputer() {
        return getComputer(SystemUtils.getLocalHostName());
    }

    public DataObject getServerComputerObject() {
        return new DataObject(getComputer(SystemUtils.getLocalHostName()), businessLogics.authenticationLM.computer);
    }

    public Integer getComputer(String strHostName) {
        try {
            DataSession session = createSession();

            QueryBuilder<String, Object> q = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            q.and(
                    businessLogics.authenticationLM.hostnameComputer.getExpr(
                            session.getModifier(), q.getMapExprs().get("key")
                    ).compare(new DataObject(strHostName), Compare.EQUALS)
            );

            Integer result;

            ImSet<ImMap<String, Object>> keys = q.execute(session).keys();
            if (keys.size() == 0) {
                DataObject addObject = session.addObject(businessLogics.authenticationLM.computer);
                businessLogics.authenticationLM.hostnameComputer.change(strHostName, session, addObject);

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

        DBStructure oldDBStructure = new DBStructure(inputDB, sql);

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
                    sql.dropIndex(oldTable.name, oldTable.keys, SetFact.fromJavaOrderSet(oldIndexKeys), oldOrder);
                }
            }
        }

        systemLogger.info("Applying migration script");
        alterateDBStructure(oldDBStructure, sql);

        // добавим таблицы которых не было
        systemLogger.info("Creating tables");
        for (Table table : newDBStructure.tables.keySet()) {
            if (oldDBStructure.getTable(table.name) == null)
                sql.createTable(table.name, table.keys);
        }

        // проверяем изменение структуры ключей
        for (Table table : newDBStructure.tables.keySet()) {
            Table oldTable = oldDBStructure.getTable(table.name);
            if (oldTable != null) {
                for (KeyField key : table.keys) {
                    KeyField oldKey = oldTable.findKey(key.name);
                    if (!(key.type.equals(oldKey.type))) {
                        sql.modifyColumn(table.name, key, oldKey.type);
                        systemLogger.info("Changing type of key column " + key.name + " in table " + table.name + " from " + oldKey.type + " to " + key.type);
                    }
                }
            }
        }

        List<AggregateProperty> recalculateProperties = new ArrayList<AggregateProperty>();

        MExclSet<Pair<String, String>> mDropColumns = SetFact.mExclSet(); // вообще pend'ить нужно только classDataProperty, но их тогда надо будет отличать

        // бежим по свойствам
        Map<String, String> columnsToDrop = new HashMap<String, String>();
        List<DBStoredProperty> restNewDBStored = new ArrayList<DBStoredProperty>(newDBStructure.storedProperties);
        for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
            Table oldTable = oldDBStructure.getTable(oldProperty.tableName);

            boolean keep = false, moved = false;
            for (Iterator<DBStoredProperty> is = restNewDBStored.iterator(); is.hasNext(); ) {
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

                            systemLogger.info(getString("logics.info.property.is.transferred.from.table.to.table", newProperty.property.field, newProperty.property.caption, oldProperty.tableName, newProperty.tableName));
                            newProperty.property.mapTable.table.moveColumn(sql, newProperty.property.field, oldTable,
                                                                           foundInterfaces.join((ImMap<PropertyInterface, KeyField>) newProperty.property.mapTable.mapKeys), oldTable.findProperty(oldProperty.sID));
                            systemLogger.info("Done");
                            moved = true;
                        } else { // надо проверить что тип не изменился
                            Type oldType = oldTable.findProperty(oldProperty.sID).type;
                            if(oldDBStructure.version < 8 && newProperty.property.field.type instanceof ConcatenateType) { // вряд ли сможем конвертить пересоздадим
                                sql.dropColumn(newProperty.tableName, newProperty.property.field.name);
                                sql.addColumn(newProperty.tableName, newProperty.property.field);
                                recalculateProperties.add((AggregateProperty) newProperty.property);
                            } else if (!oldType.equals(newProperty.property.field.type)) {
                                systemLogger.info("Changing type of property column " + newProperty.property.field.name + " in table " + newProperty.tableName + " from " + oldType + " to " + newProperty.property.field.type);
                                sql.modifyColumn(newProperty.tableName, newProperty.property.field, oldType);
                            }
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
                        mDropColumns.exclAdd(new Pair<String, String>(oldTable.name, oldProperty.sID));
                    }
                } else
                    mDropColumns.exclAdd(new Pair<String, String>(oldTable.name, oldProperty.sID));
            }
        }

        for (DBStoredProperty property : restNewDBStored) { // добавляем оставшиеся
            sql.addColumn(property.tableName, property.property.field);
            if (struct != null && property.property instanceof AggregateProperty) // если все свойства "новые" то ничего перерасчитывать не надо
                recalculateProperties.add((AggregateProperty) property.property);
        }

        // обработка изменений с классами
        MMap<String, ImMap<String, ImSet<Integer>>> mToCopy = MapFact.mMap(AMap.<String, String, Integer>addMergeMapSets()); // в какое свойство, из какого свойства - какой класс
        for(DBConcreteClass oldClass : oldDBStructure.concreteClasses) {
            for(DBConcreteClass newClass : newDBStructure.concreteClasses) {
                if(oldClass.sID.equals(newClass.sID)) {
                    if(!(oldClass.sDataPropID.equals(newClass.sDataPropID))) // надо пометить перенос, и удаление
                        mToCopy.add(newClass.sDataPropID, MapFact.singleton(oldClass.sDataPropID, SetFact.singleton(oldClass.ID)));
                    break;
                }
            }
        }
        ImMap<String, ImMap<String, ImSet<Integer>>> toCopy = mToCopy.immutable();
        for(int i=0,size=toCopy.size();i<size;i++) { // перенесем классы, которые сохранились но изменили поле
            DBStoredProperty classProp = newDBStructure.getProperty(toCopy.getKey(i));
            Table table = newDBStructure.getTable(classProp.tableName);

            QueryBuilder<KeyField, PropertyField> copyObjects = new QueryBuilder<KeyField, PropertyField>(table);
            Expr keyExpr = copyObjects.getMapExprs().singleValue();
            Where moveWhere = Where.FALSE;
            CaseExprInterface mExpr = Expr.newCases(true);
            ImMap<String, ImSet<Integer>> copyFrom = toCopy.getValue(i);
            MSet<String> mCopyFromTables = SetFact.mSetMax(copyFrom.size());
            for(int j=0,sizeJ=copyFrom.size();j<sizeJ;j++) {
                DBStoredProperty oldClassProp = oldDBStructure.getProperty(copyFrom.getKey(j));
                Table oldTable = oldDBStructure.getTable(oldClassProp.tableName);
                mCopyFromTables.add(oldClassProp.tableName);

                Expr oldExpr = oldTable.join(MapFact.singleton(oldTable.getTableKeys().single(), keyExpr)).getExpr(oldTable.findProperty(oldClassProp.sID));
                for(int prevID : copyFrom.getValue(j))
                    moveWhere = moveWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
                mExpr.add(moveWhere, oldExpr);
            }
            copyObjects.addProperty(table.findProperty(classProp.sID), mExpr.getFinal());
            copyObjects.and(moveWhere);

            systemLogger.info(getString("logics.info.objects.are.transferred.from.tables.to.table", classProp.tableName, mCopyFromTables.immutable().toString()));
            sql.modifyRecords(new ModifyQuery(table, copyObjects.getQuery()));
        }
        ImMap<String, ImSet<Integer>> toClean = MapFact.mergeMaps(toCopy.values(), ASet.<String, Integer>addMergeSet());
        for(int i=0,size=toClean.size();i<size;i++) { // удалим оставшиеся классы
            DBStoredProperty classProp = oldDBStructure.getProperty(toClean.getKey(i));
            Table table = oldDBStructure.getTable(classProp.tableName);

            QueryBuilder<KeyField, PropertyField> dropClassObjects = new QueryBuilder<KeyField, PropertyField>(table);
            Where moveWhere = Where.FALSE;

            PropertyField oldField = table.findProperty(classProp.sID);
            Expr oldExpr = table.join(dropClassObjects.getMapExprs()).getExpr(oldField);
            for(int prevID : toClean.getValue(i))
                moveWhere = moveWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
            dropClassObjects.addProperty(oldField, Expr.NULL);
            dropClassObjects.and(moveWhere);

            systemLogger.info(getString("logics.info.objects.are.removed.from.table", classProp.tableName));
            sql.updateRecords(new ModifyQuery(table, dropClassObjects.getQuery()));
        }

        MSet<ImplementTable> mPackTables = SetFact.mSet();
        for(Pair<String, String> dropColumn : mDropColumns.immutable()) {
            systemLogger.info("Dropping column " + dropColumn.second + " from table " + dropColumn.first);
            sql.dropColumn(dropColumn.first, dropColumn.second);
            ImplementTable table = (ImplementTable) newDBStructure.getTable(dropColumn.first);
            if (table != null) mPackTables.add(table);
        }

        // удаляем таблицы старые
        for (Table table : oldDBStructure.tables.keySet()) {
            if (newDBStructure.getTable(table.name) == null) {
                sql.dropTable(table.name);
            }
        }

        systemLogger.info("Packing tables");
        packTables(sql, mPackTables.immutable()); // упакуем таблицы

        systemLogger.info("Updating stats");
        updateStats();  // пересчитаем статистику

        // создадим индексы в базе
        systemLogger.info("Adding indices");
        for (Map.Entry<Table, Map<List<String>, Boolean>> mapIndex : newDBStructure.tables.entrySet())
            for (Map.Entry<List<String>, Boolean> index : mapIndex.getValue().entrySet())
                sql.addIndex(mapIndex.getKey().name, mapIndex.getKey().keys, SetFact.fromJavaOrderSet(index.getKey()), index.getValue());

        systemLogger.info("Filling static objects ids");
        fillIDs(getChangesAfter(oldDBStructure.dbVersion, classSIDChanges), getChangesAfter(oldDBStructure.dbVersion, objectSIDChanges));

        for(DBConcreteClass newClass : newDBStructure.concreteClasses) {
            newClass.ID = newClass.customClass.ID;
        }

        newDBStructure.writeConcreteClasses(outDB);

        try {
            sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject((Object) outDBStruct.toByteArray(), ByteArrayClass.instance)), true);
        } catch (Exception e) {
            ImMap<PropertyField, ObjectValue> propFields = MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject((Object) new byte[0], ByteArrayClass.instance));
            sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), propFields, true);
        }

        systemLogger.info("Updating class stats");

        updateClassStat(sql);

        systemLogger.info("Recalculating aggregations");
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

    private void fillIDs(Map<String, String> sIDChanges, Map<String, String> objectSIDChanges) throws SQLException {
        DataSession session = createSession();

        LM.baseClass.fillIDs(session, LM.staticCaption, LM.staticName, sIDChanges, objectSIDChanges);

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
        if (ignoreMigration) {
            //todo: добавить возможность задавать расположение для migration.script, чтобы можно было запускать разные логики из одного модуля
            return;
        }

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
        Map<String, String> classChanges = getChangesAfter(data.dbVersion, classSIDChanges);

        for (Map.Entry<String, String> entry : propertyChanges.entrySet()) {
            DBStoredProperty oldProperty = null;
            for (DBStoredProperty property : data.storedProperties)
                if (entry.getKey().equals(property.sID)) {
                    oldProperty = property;
                    break;
                }
            if (oldProperty != null) {
                String newSID = entry.getValue();
                systemLogger.info("Renaming column from " + oldProperty.sID + " to " + newSID + " in table " + oldProperty.tableName);
                sql.renameColumn(oldProperty.tableName, oldProperty.sID, newSID);
                oldProperty.sID = newSID;
            }
        }

        for (DBStoredProperty oldProperty : data.storedProperties) {
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
                systemLogger.info("Renaming table from " + table.name + " to " + newSID);
                sql.renameTable(table.name, newSID);
                table.name = newSID;
            }
        }

        for (DBConcreteClass oldClass : data.concreteClasses) {
            if(classChanges.containsKey(oldClass.sID)) {
                oldClass.sID = classChanges.get(oldClass.sID);
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
        if (!objectSIDChanges.isEmpty() && curVersion.compare(objectSIDChanges.lastKey()) < 0) {
            curVersion = objectSIDChanges.lastKey();
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

    public void addObjectSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(objectSIDChanges, version, oldSID, newSID);
    }

    private Map<String, String> getChangesAfter(DBVersion versionAfter, TreeMap<DBVersion, List<SIDChange>> allChanges) {
        Map<String, String> resultChanges = new OrderedMap<String, String>();

        for (Map.Entry<DBVersion, List<SIDChange>> changesEntry : allChanges.entrySet()) {
            if (changesEntry.getKey().compare(versionAfter) > 0) {
                List<SIDChange> versionChanges = changesEntry.getValue();
                Map<String, String> versionChangesMap = new OrderedMap<String, String>();

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

    public void addIndex(LCP<?>... lps) {
        List<CalcProperty> index = new ArrayList<CalcProperty>();
        for (LCP<?> lp : lps) {
            index.add((CalcProperty) lp.property);
        }
        indexes.put(index, lps[0].property.getType() instanceof DataClass);
    }

    public String backupDB(String dumpFileName) throws IOException, InterruptedException {
        return adapter.backupDB(dumpFileName);
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

    private <T> ImMap<String, T> readStatsFromDB(DataSession session, LCP sIDProp, LCP statsProp, final LCP notNullProp) throws SQLException {
        QueryBuilder<String, String> query = new QueryBuilder<String, String>(SetFact.toSet("key"));
        Expr sidToObject = sIDProp.getExpr(session.getModifier(), query.getMapExprs().singleValue());
        query.and(sidToObject.getWhere());
        query.addProperty("property", statsProp.getExpr(session.getModifier(), sidToObject));
        if(notNullProp!=null)
            query.addProperty("notNull", notNullProp.getExpr(session.getModifier(), sidToObject));
        return query.execute(session).getMap().mapKeyValues(new GetValue<String, ImMap<String, Object>>() {
                                                                public String getMapValue(ImMap<String, Object> key) {
                                                                    return ((String) key.singleValue()).trim();
                                                                }}, new GetValue<T, ImMap<String, Object>>() {
                                                                public T getMapValue(ImMap<String, Object> value) {
                                                                    if(notNullProp!=null) {
                                                                        return (T)new Pair<Integer, Integer>((Integer)value.get("property"), (Integer)value.get("notNull"));
                                                                    } else
                                                                        return (T)value.singleValue();
                                                                }});
    }

    public void updateStats(boolean statDefault) throws SQLException {
        DataSession session = createSession();

        ImMap<String, Integer> tableStats;
        ImMap<String, Integer> keyStats;
        ImMap<String, Pair<Integer, Integer>> propStats;
        if(statDefault) {
            tableStats = MapFact.EMPTY();
            keyStats = MapFact.EMPTY();
            propStats = MapFact.EMPTY();
        } else {
            tableStats = readStatsFromDB(session, reflectionLM.tableSID, reflectionLM.rowsTable, null);
            keyStats = readStatsFromDB(session, reflectionLM.tableKeySID, reflectionLM.quantityTableKey, null);
            propStats = readStatsFromDB(session, reflectionLM.tableColumnSID, reflectionLM.quantityTableColumn, reflectionLM.notNullQuantityTableColumn);
        }

        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            dataTable.updateStat(tableStats, keyStats, propStats, statDefault);
        }
    }

    private void initSystemUser() {
        // считаем системного пользователя
        try {
            DataSession session = createSession();

            QueryBuilder<String, Object> query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(query.getMapExprs().singleValue().isClass(businessLogics.authenticationLM.systemUser));
            ImOrderSet<ImMap<String, Object>> rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                systemUserObject = (Integer) session.addObject(businessLogics.authenticationLM.systemUser).object;
                session.apply(businessLogics);
            } else
                systemUserObject = (Integer) rows.single().get("key");

            query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(businessLogics.authenticationLM.hostnameComputer.getExpr(session.getModifier(), query.getMapExprs().singleValue()).compare(new DataObject("systemhost"), Compare.EQUALS));
            rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                DataObject computerObject = session.addObject(businessLogics.authenticationLM.computer);
                systemComputer = (Integer) computerObject.object;
                businessLogics.authenticationLM.hostnameComputer.change("systemhost", session, computerObject);
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

        @Override
        public String toString() {
            return sID + ' ' + tableName;
        }

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

    private class DBConcreteClass {
        public String sID;
        public String sDataPropID; // в каком ClassDataProperty хранился

        @Override
        public String toString() {
            return sID + ' ' + sDataPropID;
        }

        public Integer ID = null; // только для старых
        public ConcreteCustomClass customClass = null; // только для новых

        private DBConcreteClass(String sID, String sDataPropID, Integer ID) {
            this.sID = sID;
            this.sDataPropID = sDataPropID;
            this.ID = ID;
        }

        private DBConcreteClass(ConcreteCustomClass customClass) {
            sID = customClass.getSID();
            sDataPropID = customClass.dataProperty.getSID();

            this.customClass = customClass;
        }
    }

    private class DBStructure {
        public int version;
        public DBVersion dbVersion;
        public Map<Table, Map<List<String>, Boolean>> tables = new HashMap<Table, Map<List<String>, Boolean>>();
        public List<DBStoredProperty> storedProperties = new ArrayList<DBStoredProperty>();
        public Set<DBConcreteClass> concreteClasses = new HashSet<DBConcreteClass>();

        public DBStructure(DBVersion dbVersion) {
            version = 9;
            this.dbVersion = dbVersion;

            for (Table table : LM.tableFactory.getImplementTablesMap().valueIt()) {
                tables.put(table, new HashMap<List<String>, Boolean>());
            }

            for (Map.Entry<List<? extends CalcProperty>, Boolean> index : indexes.entrySet()) {
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

            for (ConcreteCustomClass customClass : businessLogics.getConcreteCustomClasses()) {
                concreteClasses.add(new DBConcreteClass(customClass));
            }
        }

        public DBStructure(DataInputStream inputDB, SQLSession sql) throws IOException, SQLException {
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

                if(version > 4) {
                    int prevConcreteNum = inputDB.readInt();
                    for(int i = 0; i < prevConcreteNum; i++)
                        concreteClasses.add(new DBConcreteClass(inputDB.readUTF(), inputDB.readUTF(), inputDB.readInt()));
                } else {
                    KeyField objectKey = new KeyField("object", ObjectType.instance);
                    PropertyField classField = new PropertyField("class", ObjectType.instance);
                    SerializedTable objectTable = new SerializedTable("objects", SetFact.singletonOrder(objectKey), SetFact.singleton(classField), LM.baseClass);
                    tables.put(objectTable, new HashMap<List<String>, Boolean>());
                    storedProperties.add(new DBStoredProperty("class", false, "objects", MapFact.singleton(0, objectKey))); // в map'е все равно что будет

                    QueryBuilder<String, String> allClassesQuery = new QueryBuilder<String, String>(SetFact.singleton("key"));
                    Expr key = allClassesQuery.getMapExprs().singleValue();

                    BaseLogicsModule LM = ThreadLocalContext.getBusinessLogics().LM;

                    ImplementTable table = new ImplementTable("sidClass", LM.baseClass);

                    StoredDataProperty dataProperty = new StoredDataProperty("classSID", "classSID", new ValueClass[] {LM.baseClass}, StringClass.get(250));
                    LCP classSID = new LCP<ClassPropertyInterface> (dataProperty);
                    dataProperty.markStored(LM.tableFactory, table);

                    Expr sidExpr = classSID.getExpr(Property.defaultModifier, key);

                    allClassesQuery.and(sidExpr.getWhere()); // вот тут придется напрямую из таблицы читать id'ки для классов, потому как isClass использовать очевидно нельзя
                    allClassesQuery.and(objectTable.join(MapFact.singleton(objectKey, key)).getExpr(classField).compare(new ValueExpr(Integer.MAX_VALUE - 5, LM.baseClass.objectClass), Compare.EQUALS));
                    allClassesQuery.addProperty("sid", sidExpr);
                    ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> qResult = allClassesQuery.execute(sql, QueryEnvironment.empty);

                    for (int i = 0, size = qResult.size(); i < size; i++)
                        concreteClasses.add(new DBConcreteClass(
                                ((String) qResult.getValue(i).get("sid")).trim(), "class", (Integer) qResult.getKey(i).singleValue()));
                }
            }
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

        public void writeConcreteClasses(DataOutputStream outDB) throws IOException { // отдельно от write, так как ID заполняются после fillIDs
            outDB.writeInt(concreteClasses.size());
            for (DBConcreteClass concreteClass : concreteClasses) {
                outDB.writeUTF(concreteClass.sID);
                outDB.writeUTF(concreteClass.sDataPropID);
                outDB.writeInt(concreteClass.ID);
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

        public DBStoredProperty getProperty(String name) {
            for (DBStoredProperty prop : storedProperties) {
                if (prop.sID.equals(name)) {
                    return prop;
                }
            }
            return null;
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
