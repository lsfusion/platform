package platform.server.session;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.interop.action.ClientAction;
import platform.interop.action.LogMessageClientAction;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.Settings;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.*;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.navigator.ComputerController;
import platform.server.form.navigator.IsServerRestartingController;
import platform.server.form.navigator.UserController;
import platform.server.logics.*;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

public class DataSession extends BaseMutableModifier implements SessionChanges, ExecutionEnvironmentInterface {

    private Map<CustomClass, SingleKeyNoPropertyUsage> add = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    private Map<CustomClass, SingleKeyNoPropertyUsage> remove = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    private Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>();

    private SingleKeyPropertyUsage news = null;
    private Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    private class Transaction {
        private final Map<CustomClass, SessionData> add;
        private final Map<CustomClass, SessionData> remove;
        private final Map<DataProperty, SessionData> data;
        
        private final SessionData news;
        private final Map<DataObject, ConcreteObjectClass> newClasses;
        
        private Transaction() {
            add = SessionTableUsage.saveData(DataSession.this.add);
            remove = SessionTableUsage.saveData(DataSession.this.remove);
            data = SessionTableUsage.saveData(DataSession.this.data);

            if(DataSession.this.news!=null)
                news = DataSession.this.news.saveData();
            else
                news = null;
            newClasses = new HashMap<DataObject, ConcreteObjectClass>(DataSession.this.newClasses);
        }

        private void rollback() throws SQLException {
            dropTables(sql); // старые вернем

            // assert что новые включают старые
            DataSession.this.add = SessionTableUsage.rollData(sql, DataSession.this.add, add);
            DataSession.this.remove = SessionTableUsage.rollData(sql, DataSession.this.remove, remove);
            DataSession.this.data = SessionTableUsage.rollData(sql, DataSession.this.data, data);

            if(news!=null)
                DataSession.this.news.rollData(sql, news);
            else
                DataSession.this.news = null;
            DataSession.this.newClasses = newClasses;
        }
    }
    private Transaction applyTransaction; // restore point

    private void startTransaction() throws SQLException {
        if(Settings.instance.isApplyVolatileStats())
            sql.pushVolatileStats(null);
        sql.startTransaction();
    }
    private void checkTransaction() {
        if(sql.isInTransaction() && applyTransaction==null)
            applyTransaction = new Transaction();
    }
    public void rollbackTransaction() throws SQLException {
        if(applyTransaction!=null) {
            applyTransaction.rollback();
            applyTransaction = null;
        }
        sql.rollbackTransaction();
        if(Settings.instance.isApplyVolatileStats())
            sql.popVolatileStats(null);

//        checkSessionTableMap();
    }
/*    private void checkSessionTableMap() {
        checkSessionTableMap(add);
        checkSessionTableMap(remove);
        checkSessionTableMap(data);
        checkSessionTableMap(news);
    }
    private void checkSessionTableMap(Map<?, ? extends SessionTableUsage> usages) {
        for(SessionTableUsage usage : usages.values())
            checkSessionTableMap(usage);
    }
    private void checkSessionTableMap(SessionTableUsage usage) {
        if(usage!=null && usage.table instanceof SessionDataTable)
            sql.checkSessionTableMap(((SessionDataTable)usage.table).getTable(), usage);
    }*/
    

    private void commitTransaction() throws SQLException {
        applyTransaction = null;
        sql.commitTransaction();
        if(Settings.instance.isApplyVolatileStats())
            sql.popVolatileStats(null);
    }

    public static Set<IsClassProperty> getProperties(Set<CustomClass> addClasses, Set<CustomClass> removeClasses) {
        return CustomClass.getProperties(BaseUtils.mergeSet(addClasses, removeClasses));
    }

    protected Collection<CalcProperty> calculateProperties() {
        Collection<CalcProperty> result = BaseUtils.immutableCast(BaseUtils.mergeSet(getProperties(add.keySet(), remove.keySet()), data.keySet()));
        if(news!=null)
            return BaseUtils.add(result, baseClass.getObjectClassProperty());
        else
            return result;
    }

    public PropertyChanges getFilterChanges(Set<CustomClass> addClasses, Set<CustomClass> removeClasses) {
        return getPropertyChanges().filter(BaseUtils.add(getProperties(addClasses, removeClasses), baseClass.getObjectClassProperty()));
    }

    public PropertyChanges getFilterChanges(DataProperty property) {
        return getPropertyChanges().filter(Collections.singleton(property));
    }

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty() || news!=null;
    }

    public boolean hasStoredChanges() {
        if (!add.isEmpty() || !remove.isEmpty() || news != null)
            return true;

        for (DataProperty property : data.keySet())
            if (property.isStored())
                return true;

        return false;
    }

    protected boolean isFinal() {
        return false;
    }

    private PropertyChange<ClassPropertyInterface> getObjectClassChange(ObjectClassProperty property) {
        if(news!=null)
            return SingleKeyPropertyUsage.getChange(news, BaseUtils.single(property.interfaces));
        return null;
    }

    private PropertyChange<ClassPropertyInterface> getClassChange(IsClassProperty property) {
        ValueClass isClass = property.getInterfaceClass();
        if(isClass instanceof CustomClass) {
            SingleKeyNoPropertyUsage addTable = add.get((CustomClass)isClass);
            SingleKeyNoPropertyUsage removeTable = remove.get((CustomClass) isClass);
            if(addTable!=null || removeTable!=null) {
                Map<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
                KeyExpr key = BaseUtils.singleValue(mapKeys);

                Where changedWhere = Where.FALSE;
                Expr changeExpr;
                if(addTable!=null) {
                    Where addWhere = addTable.getWhere(key);
                    changeExpr = ValueExpr.get(addWhere);
                    changedWhere = changedWhere.or(addWhere);
                } else
                    changeExpr = Expr.NULL;

                if(removeTable!=null)
                    changedWhere = changedWhere.or(removeTable.getWhere(key));
                return new PropertyChange<ClassPropertyInterface>(mapKeys, changeExpr, changedWhere);
            }
        }
        return null;
    }

    public PropertyChange<ClassPropertyInterface> getDataChange(DataProperty property) {
        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange!=null)
            return SinglePropertyTableUsage.getChange(dataChange);
        return null;
    }

    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        if(property instanceof ObjectClassProperty)
            return (PropertyChange<P>) getObjectClassChange((ObjectClassProperty) property);

        if(property instanceof IsClassProperty)
            return (PropertyChange<P>) getClassChange((IsClassProperty) property);

        if(property instanceof DataProperty)
            return (PropertyChange<P>) getDataChange((DataProperty) property);
        return null;
    }

    private SingleKeyNoPropertyUsage getRemoveTable(ValueClass valueClass) {
        SingleKeyNoPropertyUsage removeTable;
        if(valueClass instanceof CustomClass)
            return remove.get((CustomClass)valueClass);
        return null;
    }

    public final Modifier modifier = this;
    public final SQLSession sql;
    public final SQLSession idSession;

    public void close() throws SQLException {
    }

    public static class UpdateChanges {

        public final Set<CalcProperty> properties;

        public UpdateChanges() {
            properties = new HashSet<CalcProperty>();
        }

        public UpdateChanges(DataSession session, FormInstance<?> form) {
            assert form.session == session;
            properties = new HashSet<CalcProperty>(form.getUpdateProperties());
        }

        public void add(UpdateChanges changes) {
            properties.addAll(changes.properties);
        }
    }

    // формы, для которых с момента последнего update уже был restart, соотвественно в значениях - изменения от посл. update (prev) до посл. apply
    public IdentityHashMap<FormInstance, UpdateChanges> appliedChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    // формы для которых с момента последнего update не было restart, соответственно в значениях - изменения от посл. update (prev) до посл. изменения
    public IdentityHashMap<FormInstance, UpdateChanges> incrementChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    // assert что те же формы что и в increment, соответственно в значениях - изменения от посл. apply до посл. update (prev)
    public IdentityHashMap<FormInstance, UpdateChanges> updateChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    public final BaseClass baseClass;
    public final CustomClass namedObject;
    public final LCP<?> name;
    public final AbstractGroup recognizeGroup;
    public final CustomClass transaction;
    public final LCP<?> date;
    public final ConcreteCustomClass sessionClass;
    public final LCP<?> currentDate;
    public final LCP<?> currentSession;

    // для отладки
    public static boolean reCalculateAggr = false;

    private final IsServerRestartingController isServerRestarting;
    public final UserController user;
    public final ComputerController computer;

    public DataObject applyObject = null;

    public DataSession(SQLSession sql, final UserController user, final ComputerController computer, IsServerRestartingController isServerRestarting, BaseClass baseClass, CustomClass namedObject, ConcreteCustomClass sessionClass, LCP name, AbstractGroup recognizeGroup, CustomClass transaction, LCP date, LCP currentDate, LCP currentSession, SQLSession idSession) throws SQLException {
        this.sql = sql;
        this.isServerRestarting = isServerRestarting;

        this.baseClass = baseClass;
        this.namedObject = namedObject;
        this.name = name;
        this.recognizeGroup = recognizeGroup;
        this.transaction = transaction;
        this.date = date;
        this.sessionClass = sessionClass;
        this.currentDate = currentDate;
        this.currentSession = currentSession;

        this.user = user;
        this.computer = computer;

        this.idSession = idSession;
    }

    public DataSession createSession() throws SQLException {
        return new DataSession(sql, user, computer, isServerRestarting, baseClass, namedObject, sessionClass, name, recognizeGroup, transaction, date, currentDate, currentSession, idSession);
    }

    public void restart(boolean cancel) throws SQLException {

        // apply
        //      по кому был restart : добавляем changes -> applied
        //      по кому не было restart : to -> applied (помечая что был restart)

        // cancel
        //    по кому не было restart :  from -> в applied (помечая что был restart)

        if(!cancel)
            for(Map.Entry<FormInstance,UpdateChanges> appliedChange : appliedChanges.entrySet())
                appliedChange.getValue().add(new UpdateChanges(this, (FormInstance<?>) appliedChange.getKey()));

        assert Collections.disjoint(appliedChanges.keySet(),(cancel?updateChanges:incrementChanges).keySet());
        appliedChanges.putAll(cancel?updateChanges:incrementChanges);
        incrementChanges = new IdentityHashMap<FormInstance, UpdateChanges>();
        updateChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

        newClasses = new HashMap<DataObject, ConcreteObjectClass>();

        dropTables(sql);
        add.clear();
        remove.clear();
        data.clear();
        news = null;

        applyObject = null; // сбрасываем в том числе когда cancel потому как cancel drop'ает в том числе и добавление объекта
    }

    public DataObject addObject() throws SQLException {
        return new DataObject(IDTable.instance.generateID(idSession, IDTable.OBJECT),baseClass.unknown);
    }

    // с fill'ами addObject'ы

    public DataObject addObject(ConcreteCustomClass customClass) throws SQLException {
        return addObject(customClass, true, true);
    }

    public DataObject addObject(ConcreteCustomClass customClass, boolean fillDefault, boolean groupLast) throws SQLException {
        DataObject object = addObject();

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass, groupLast);

        if(fillDefault) {
            if(customClass.isChild(namedObject))
                name.change(customClass.caption + " " + object.object, new ExecutionEnvironment(this), object);

            if(customClass.isChild(transaction))
                date.change(currentDate.read(this), new ExecutionEnvironment(this), object);
        }

        return object;
    }

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        changeClass(change, toClass, true);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass cls, boolean groupLast) throws SQLException {
        changeClass(object, cls, groupLast);
    }

    public void changeClass(DataObject change, ConcreteObjectClass toClass, boolean groupLast) throws SQLException {
        boolean hadStoredChanges = hasStoredChanges();

        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = (ConcreteObjectClass) getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        assert Collections.disjoint(addClasses,removeClasses);

        changeClass(addClasses, removeClasses, toClass, change, sql, groupLast);

        newClasses.put(change, toClass);

        if(groupLast) // по тем по кому не было restart'а new -> to
            updateProperties(getFilterChanges(addClasses, removeClasses));

        aspectChange(hadStoredChanges);
    }

    public void changeProperty(DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, boolean groupLast) throws SQLException {
        boolean hadStoredChanges = hasStoredChanges();
        
        changeProperty(property, keys, newValue, sql, groupLast);
        
        if(groupLast) // по тем по кому не было restart'а new -> to
            updateProperties(getFilterChanges(property));

        aspectChange(hadStoredChanges);
    }

    public static final SessionDataProperty isDataChanged = new SessionDataProperty("isDataChanged", "Is data changed", LogicalClass.instance);
    private void aspectChange(boolean hadStoredChanges) throws SQLException {
        if(!hadStoredChanges) {
            changeProperty(isDataChanged, new HashMap<ClassPropertyInterface, DataObject>(), new DataObject(true, LogicalClass.instance), true);
            updateProperties(getFilterChanges(isDataChanged));
        }
    }

    public void updateProperties(PropertyChanges changes) throws SQLException {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet()) {
            FormInstance<?> formInstance = (FormInstance<?>) incrementChange.getKey();
            incrementChange.getValue().properties.addAll(formInstance.getUpdateProperties(changes));
            formInstance.dropIncrement(changes);
            formInstance.dataChanged = true;
        }
    }

    
    // для оптимизации
    public DataChanges getUserDataChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
        Pair<Map<ClassPropertyInterface, DataObject>, ObjectValue> simple;
        if((simple = change.getSimple())!=null) {
            if(IsClassProperty.fitClasses(getCurrentClasses(simple.first), property.value,
                    simple.second instanceof DataObject ? getCurrentClass((DataObject) simple.second) : null))
                return new DataChanges(property, change);
            else
                return new DataChanges();
        }
        return null;
    }

    public ConcreteClass getCurrentClass(DataObject value) {
        ConcreteClass newClass;
        if((newClass = newClasses.get(value))==null)
            return value.objectClass;
        else
            return newClass;
    }

    public <K> Map<K, ConcreteClass> getCurrentClasses(Map<K, DataObject> map) {
        Map<K, ConcreteClass> result = new HashMap<K, ConcreteClass>();
        for(Map.Entry<K, DataObject> entry : map.entrySet())
            result.put(entry.getKey(), getCurrentClass(entry.getValue()));
        return result;
    }

    public ObjectValue getCurrentValue(ObjectValue value) {
        if(value instanceof NullValue)
            return value;
        else {
            DataObject dataObject = (DataObject)value;
            return new DataObject(dataObject.object, getCurrentClass(dataObject));
        }
    }

    public <K, V extends ObjectValue> Map<K, V> getCurrentObjects(Map<K, V> map) {
        Map<K, V> result = new HashMap<K, V>();
        for(Map.Entry<K, V> entry : map.entrySet())
            result.put(entry.getKey(), (V) getCurrentValue(entry.getValue()));
        return result;
    }

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        return baseClass.getDataObject(sql, value, type);
    }

    public ObjectValue getObjectValue(Object value, Type type) throws SQLException {
        return baseClass.getObjectValue(sql, value, type);
    }

    // узнает список изменений произошедших без него
    public Collection<CalcProperty> update(FormInstance<?> form) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        UpdateChanges incrementChange = incrementChanges.get(form);
        if(incrementChange!=null) // если не было restart
            //    to -> from или from = changes, to = пустому
            updateChanges.get(form).add(incrementChange);
            //    возвращаем to
        else { // иначе
            incrementChange = appliedChanges.remove(form);
            if(incrementChange==null) // совсем не было
                incrementChange = new UpdateChanges();
            UpdateChanges formChanges = new UpdateChanges(this, form);
            // from = changes (сбрасываем пометку что не было restart'а)
            updateChanges.put(form, formChanges);
            // возвращаем applied + changes
            incrementChange.add(formChanges);
        }
        incrementChanges.put(form,new UpdateChanges());

        return incrementChange.properties;
    }

    public String apply(BusinessLogics<?> BL) throws SQLException {
        ArrayList<ClientAction> actions = new ArrayList<ClientAction>();
        if(apply(BL, actions))
            return null;
        else
            return ((LogMessageClientAction)BaseUtils.single(actions)).message;
    }

    public boolean apply(BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException {
        return apply(BL, actions, false);
    }

    public boolean check(BusinessLogics BL, List<ClientAction> actions) throws SQLException {
        return apply(BL, actions, true);
    }

    public static <T extends PropertyInterface> boolean fitKeyClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys).means(property.mapTable.table.getClasses());
    }

    public static <T extends PropertyInterface> boolean fitClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys, property.field).means(property.fieldClassWhere);
    }

    public static <T extends PropertyInterface> boolean notFitKeyClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys).and(property.mapTable.table.getClasses()).isFalse();
    }

    public static <T extends PropertyInterface> boolean notFitClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys, property.field).and(property.fieldClassWhere).isFalse();
    }

    private <T extends PropertyInterface, D extends PropertyInterface> void applySingleStored(CalcProperty<T> property, SinglePropertyTableUsage<T> change, BusinessLogics<?> BL, IncrementApply incrementApply) throws SQLException {
        // assert что у change классы совпадают с property
        assert property.isStored();
        assert fitClasses(property, change); // проверяет гипотезу
        assert fitKeyClasses(property, change); // дополнительная проверка, она должна обеспечиваться тем что в change не должно быть замен null на null

        if(change.isEmpty())
            return;

        NoUpdate noUpdate = new NoUpdate(); IncrementProps increment = new IncrementProps(property, change);
        Modifier modifier = new OverrideModifier(noUpdate, increment);

        // true нужно предыдущее значение сохранить
        for(CalcProperty<D> depend : BL.getAppliedDependFrom(property)) { // !!! важно в лексикографическом порядке должно быть
            assert depend.isStored() || depend instanceof OldProperty;
            if(depend.isStored()) { // читаем новое значение, запускаем рекурсию
               assert !(depend instanceof OldProperty);
                SinglePropertyTableUsage<D> dependChange = depend.readChangeTable(sql, modifier, baseClass, env);
                applySingleStored((CalcProperty)depend, (SinglePropertyTableUsage)dependChange, BL, incrementApply);
                noUpdate.add(depend); // докидываем noUpdate чтобы по нескольку раз одну ветку не отрабатывать
            } else {
                SinglePropertyTableUsage<D> dependChange = ((OldProperty<D>) depend).property.readChangeTable(sql, modifier, baseClass, env);
                incrementApply.updateApplyStart((OldProperty<D>)depend, dependChange, baseClass);
            }
        }
        savePropertyChanges(property.mapTable.table, Collections.singletonMap("value", (CalcProperty) property), property.mapTable.mapKeys, change);
    }

    private void savePropertyChanges(Table implementTable, SessionTableUsage<KeyField, CalcProperty> changeTable) throws SQLException {
        savePropertyChanges(implementTable, BaseUtils.toMap(changeTable.getValues()), BaseUtils.toMap(changeTable.getKeys()), changeTable);
    }

    private <K,V> void savePropertyChanges(Table implementTable, Map<V, CalcProperty> props, Map<K, KeyField> mapKeys, SessionTableUsage<K, V> changeTable) throws SQLException {
        Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(implementTable);
        Join<V> join = changeTable.join(BaseUtils.join(mapKeys, modifyQuery.mapKeys));
        for (Map.Entry<V, CalcProperty> property : props.entrySet())
            modifyQuery.properties.put(property.getValue().field, join.getExpr(property.getKey()));
        modifyQuery.and(join.getWhere());
        sql.modifyRecords(new ModifyQuery(implementTable, modifyQuery, env));
        changeTable.drop(sql);
    }

    public boolean apply(final BusinessLogics<?> BL, List<ClientAction> actions, boolean onlyCheck) throws SQLException {
        // до чтения persistent свойств в сессию
        if (applyObject == null) {
            applyObject = addObject(sessionClass);
            currentSession.change(applyObject.object, DataSession.this);
        }

        startTransaction();

        Map<ActionProperty, List<Map<ClassPropertyInterface, DataObject>>> pendingExecutes = new HashMap<ActionProperty, List<Map<ClassPropertyInterface, DataObject>>>();
        IncrementApply incrementApply = new IncrementApply(this);

        // тоже нужен посередине, чтобы он успел dataproperty изменить до того как они обработаны
        for (Property<?> property : BL.getAppliedProperties(onlyCheck)) {
            if(property instanceof ActionProperty && ((ActionProperty)property).event!=null)
                if(!executeEventAction((ActionProperty) property, incrementApply, actions, pendingExecutes)) // действия
                    return false;
            if(property instanceof CalcProperty && ((CalcProperty)property).isStored()) // постоянно-хранимые свойства
                readStored((CalcProperty<PropertyInterface>) property, incrementApply, BL);
        }

        if (onlyCheck) {
            incrementApply.cancel(actions);
            return true;
        }

        // записываем в базу, то что туда еще не сохранено, приходится сохранять группами, так могут не подходить по классам
        for (Map.Entry<ImplementTable, Collection<CalcProperty>> groupTable : incrementApply.groupPropertiesByTables().entrySet())
            savePropertyChanges(groupTable.getKey(), incrementApply.readSave(groupTable.getKey(), groupTable.getValue()));

        for (Map.Entry<DataObject, ConcreteObjectClass> newClass : newClasses.entrySet())
            newClass.getValue().saveClassChanges(sql, newClass.getKey());
        
        // проводим "мини-паковку", то есть удаляем все записи, у которых ключем является удаляем объект
        for (Map.Entry<CustomClass, SingleKeyNoPropertyUsage> remClass : remove.entrySet()) {
            BL.LM.tableFactory.removeKeys(this, remClass.getKey(), remClass.getValue());
        }

        incrementApply.cleanIncrementTables();
        commitTransaction();
        restart(false);

        for(Map.Entry<ActionProperty, List<Map<ClassPropertyInterface, DataObject>>> pendingExecute : pendingExecutes.entrySet())
            for (Iterator<Map<ClassPropertyInterface, DataObject>> iterator = pendingExecute.getValue().iterator(); iterator.hasNext(); ) {
                Map<ClassPropertyInterface, DataObject> context = iterator.next();
                executePending(pendingExecute.getKey(), context, actions, !iterator.hasNext());
            }

        return true;
    }

    @Message("message.session.apply.write")
    private <P extends PropertyInterface> void readStored(@ParamMessage CalcProperty<P> property, IncrementApply incrementApply, BusinessLogics<?> BL) throws SQLException {
        if(property.hasChanges(incrementApply)) {
            SinglePropertyTableUsage<P> changeTable = property.readChangeTable(sql, incrementApply, baseClass, env);

            Pair<SinglePropertyTableUsage<P>,SinglePropertyTableUsage<P>> result = property.splitFitClasses(changeTable, this);

            applySingleStored(property, result.first, BL, incrementApply);
            incrementApply.increment.add(property, result.second);
        }
    }

    public boolean executeEventAction(@ParamMessage ActionProperty property, IncrementApply incrementApply, List<ClientAction> actions,
                                      Map<ActionProperty, List<Map<ClassPropertyInterface, DataObject>>> pendingExecute) throws SQLException {
        ExecutionEnvironment transactEnv = new ExecutionEnvironment(incrementApply);
        assert transactEnv.isInTransaction();
        
        PropertySet<ClassPropertyInterface> propertyChange = property.getEventAction(incrementApply);
        if(propertyChange!=null && !propertyChange.isEmpty()) {
            List<Map<ClassPropertyInterface, DataObject>> pendingPropExecute = null;
            if(property.pendingEventExecute()) {
                pendingPropExecute = new ArrayList<Map<ClassPropertyInterface, DataObject>>();
                pendingExecute.put(property, pendingPropExecute);
            }

            for (Iterator<Map<ClassPropertyInterface, DataObject>> iterator = propertyChange.executeClasses(transactEnv).iterator(); iterator.hasNext();) {
                Map<ClassPropertyInterface, DataObject> executeRow = iterator.next();

                if(pendingPropExecute!=null)
                    // иначе "pend'им" выполнение, но уже с новыми классами
                    pendingPropExecute.add(getCurrentObjects(executeRow));
                else
                    property.execute(new ExecutionContext(executeRow, null, transactEnv, actions, null, !iterator.hasNext()));
            }
        }

        return transactEnv.isInTransaction();
    }
/*
    @Message("message.session.apply.check")
    public <T extends PropertyInterface> boolean check(@ParamMessage CalcProperty<T> property, IncrementApply incrementApply, List<ClientAction> actions) throws SQLException {
        assert property.noDB();
        if(property.hasChanges(incrementApply)) {
            Query<T,String> changed = new Query<T,String>(property);
            changed.and(property.getExpr(changed.mapKeys, incrementApply).getWhere()); // только на измененные смотрим
            OrderedMap<Map<T, DataObject>, Map<String, ObjectValue>> result = changed.executeClasses(sql, new OrderedMap<String, Boolean>(), 30, baseClass, env);
            if (result.size() > 0) {
                // для constraint'ов
                assert property.isFull();

                NoPropertyTableUsage<T> keysTable = new NoPropertyTableUsage<T>(new ArrayList<T>(property.interfaces), property.interfaceTypeGetter);
                keysTable.writeKeys(sql, result.keyList());
                Map<T, KeyExpr> keysMap = keysTable.getMapKeys();

                // сюда надо name'ы вставить
                List<List<String>> propCaptions = new ArrayList<List<String>>();
                for (int i = 0; i < property.interfaces.size(); i++) {
                    propCaptions.add(new ArrayList<String>());
                }

                Query<T,String> detailed = new Query<T,String>(keysMap);
                detailed.and(keysTable.getWhere(keysMap));

                int interfaceIndex = 0;
                for(T propertyInterface : property.interfaces) {
                    ValueClass valueClass = property.getInterfaceClasses().get(propertyInterface);
                    for (Property nameProp : recognizeGroup.getProperties()) {
                        List<ValueClassWrapper> wrapper = Arrays.asList(new ValueClassWrapper(valueClass));
                        if (!nameProp.getProperties(Arrays.asList(wrapper), true).isEmpty()) {
                            Expr nameExpr = nameProp.getExpr(Collections.singletonMap(BaseUtils.single(nameProp.interfaces), detailed.mapKeys.get(propertyInterface)), incrementApply);
                            detailed.properties.put("int" + propertyInterface.ID + "_" + propCaptions.get(interfaceIndex).size(), nameExpr);
                            propCaptions.get(interfaceIndex).add(nameProp.caption);
                        }
                    }
                    interfaceIndex++;
                }
                OrderedMap<Map<T, Object>, Map<String, Object>> detailedResult = detailed.execute(this);

                String resultString = property.toString() + '\n';
                for (Map.Entry<Map<T,Object>, Map<String,Object>> row : detailedResult.entrySet()) {
                    String infoStr = "";
                    int ifaceIndex = 0;
                    for (T propertyInterface : property.interfaces) {
                        if (ifaceIndex > 0) {
                            infoStr += ", ";
                        }
                        infoStr += "[id=" + row.getKey().get(propertyInterface).toString().trim();
                        for (int i = 0; i < propCaptions.get(ifaceIndex).size(); i++) {
                            Object value = row.getValue().get("int" + propertyInterface.ID + "_" + i);
                            if (value != null) {
                                infoStr += ", " + propCaptions.get(ifaceIndex).get(i) + '=';
                                infoStr += value.toString().trim();
                            }
                        }
                        infoStr += "]";
                        ifaceIndex++;
                    }
                    resultString += "    " + infoStr + '\n';
                }

                actions.add(new LogMessageClientAction(resultString, true));
                keysTable.drop(sql);
                return false;
            }
        }

        return true;
    }
  */
    @Message("message.session.apply.auto.execute")
    public void executePending(@ParamMessage ActionProperty property, Map<ClassPropertyInterface, DataObject> context, List<ClientAction> actions, boolean groupLast) throws SQLException {
        property.execute(new ExecutionContext(context, null, new ExecutionEnvironment(this), actions, null, groupLast));
    }

    public QueryEnvironment getQueryEnv() {
        return env;
    }

    public final QueryEnvironment env = new QueryEnvironment() {
        public ParseInterface getSQLUser() {
            return new TypeObject(user.getCurrentUser().object, ObjectType.instance);
        }

        public ParseInterface getIsFullClient() {
            return new LogicalParseInterface() {
                public boolean isTrue() {
                    return computer.isFullClient();
                }
            };
        }

        public ParseInterface getSQLComputer() {
            return new TypeObject(computer.getCurrentComputer().object, ObjectType.instance);
        }

        public ParseInterface getIsServerRestarting() {
            return new LogicalParseInterface() {
                public boolean isTrue() {
                    return isServerRestarting.isServerRestarting();
                }
            };
        }
    };

    public void changeClass(Set<CustomClass> addClasses, Set<CustomClass> removeClasses, ConcreteObjectClass toClass, DataObject change, SQLSession session, boolean groupLast) throws SQLException {
        checkTransaction(); // важно что, вначале

        for(CustomClass addClass : addClasses) {
            SingleKeyNoPropertyUsage addTable = add.get(addClass);
            if(addTable==null) { // если нету таблицы создаем
                addTable = new SingleKeyNoPropertyUsage(ObjectType.instance);
                add.put(addClass, addTable);
            }
            addTable.insertRecord(session, change, false, groupLast);

            SingleKeyNoPropertyUsage removeTable = remove.get(addClass);
            if(removeTable!=null)
                removeTable.deleteRecords(session,change);
        }
        for(CustomClass removeClass : removeClasses) {
            SingleKeyNoPropertyUsage removeTable = remove.get(removeClass);
            if(removeTable==null) { // если нету таблицы создаем
                removeTable = new SingleKeyNoPropertyUsage(ObjectType.instance);
                remove.put(removeClass, removeTable);
            }
            removeTable.insertRecord(session, change, false, groupLast);

            SingleKeyNoPropertyUsage addTable = add.get(removeClass);
            if(addTable!=null)
                addTable.deleteRecords(session,change);
        }

        addChanges(getProperties(addClasses, removeClasses));

        for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> dataChange : data.entrySet()) { // удаляем существующие изменения
            DataProperty property = dataChange.getKey();
            for(ClassPropertyInterface propertyInterface : property.interfaces)
                if(propertyInterface.interfaceClass instanceof CustomClass && removeClasses.contains((CustomClass)propertyInterface.interfaceClass)) {
                    dataChange.getValue().deleteKey(session, propertyInterface, change);
                    addChange(property);
                }
            if(property.value instanceof CustomClass && removeClasses.contains((CustomClass) property.value)) {
                dataChange.getValue().deleteProperty(session, "value", change);
                addChange(property);
            }
        }

        if(news ==null)
            news = new SingleKeyPropertyUsage(ObjectType.instance, ObjectType.instance);
        news.insertRecord(session, change, toClass.getClassObject(), true, groupLast);
        addChange(baseClass.getObjectClassProperty());
    }

    public void changeProperty(final DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, SQLSession session, boolean groupLast) throws SQLException {
        checkTransaction();

        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange == null) { // создадим таблицу, если не было
            dataChange = property.createChangeTable();
            data.put(property, dataChange);
        }
        dataChange.insertRecord(session, keys, newValue, true, groupLast);

        addChange(property);
    }

    public void dropTables(SQLSession session) throws SQLException {
        for(SingleKeyNoPropertyUsage addTable : add.values())
            addTable.drop(session);
        for(SingleKeyNoPropertyUsage removeTable : remove.values())
            removeTable.drop(session);
        for(SinglePropertyTableUsage<ClassPropertyInterface> dataTable : data.values())
            dataTable.drop(session);
        if(news !=null)
            news.drop(session);

        addChanges(getProperties(add.keySet(), remove.keySet()));
        addChanges(data.keySet());
        if(news!=null)
            addChange(baseClass.getObjectClassProperty());
    }

    public DataSession getSession() {
        return this;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public FormInstance getFormInstance() {
        return null;
    }

    public boolean isInTransaction() {
        return false;
    }

    public <P extends PropertyInterface> void fireChange(CalcProperty<P> property, PropertyChange<P> change) throws SQLException {
    }

    public ExecutionEnvironmentInterface cancel(List<ClientAction> actions) throws SQLException {
        restart(true);
        return this;
    }
}
