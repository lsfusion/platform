package platform.server.session;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.action.ClientAction;
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
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.ComputerController;
import platform.server.form.navigator.IsServerRestartingController;
import platform.server.form.navigator.UserController;
import platform.server.logics.*;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

public class DataSession extends BaseMutableModifier implements SessionChanges {

    public Map<CustomClass, SingleKeyNoPropertyUsage> add = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    public Map<CustomClass, SingleKeyNoPropertyUsage> remove = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    public Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>();

    public SingleKeyPropertyUsage news = null;

    public static Set<IsClassProperty> getProperties(Set<CustomClass> addClasses, Set<CustomClass> removeClasses) {
        return CustomClass.getProperties(BaseUtils.mergeSet(addClasses, removeClasses));
    }

    protected Collection<Property> calculateProperties() {
        Collection<Property> result = BaseUtils.immutableCast(BaseUtils.mergeSet(getProperties(add.keySet(), remove.keySet()), data.keySet()));
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

    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property) {
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

        public final Set<Property> properties;
        public final Set<CustomClass> addClasses;
        public final Set<CustomClass> removeClasses;

        public UpdateChanges() {
            properties = new HashSet<Property>();
            addClasses = new HashSet<CustomClass>();
            removeClasses = new HashSet<CustomClass>();
        }

        public UpdateChanges(DataSession session, FormInstance<?> form) {
            assert form.session == session;
            addClasses = new HashSet<CustomClass>(session.add.keySet());
            removeClasses = new HashSet<CustomClass>(session.remove.keySet());
            properties = new HashSet<Property>(form.getUpdateProperties());
        }

        public void add(UpdateChanges changes) {
            properties.addAll(changes.properties);
            addClasses.addAll(changes.addClasses);
            removeClasses.addAll(changes.removeClasses);
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
    public final LP<?> name;
    public final AbstractGroup recognizeGroup;
    public final CustomClass transaction;
    public final LP<?> date;
    public final ConcreteCustomClass sessionClass;
    public final LP<?> currentDate;

    // для отладки
    public static boolean reCalculateAggr = false;

    private final List<DerivedChange<?,?>> notDeterministic;

    private final IsServerRestartingController isServerRestarting;
    public final UserController user;
    public final ComputerController computer;

    public DataObject applyObject = null;

    public DataSession(SQLSession sql, final UserController user, final ComputerController computer, IsServerRestartingController isServerRestarting, BaseClass baseClass, CustomClass namedObject, ConcreteCustomClass sessionClass, LP<?> name, AbstractGroup recognizeGroup, CustomClass transaction, LP<?> date, LP<?> currentDate, List<DerivedChange<?,?>> notDeterministic, SQLSession idSession) throws SQLException {
        this.sql = sql;
        this.isServerRestarting = isServerRestarting;

        this.baseClass = baseClass;
        this.namedObject = namedObject;
        this.name = name;
        this.recognizeGroup = recognizeGroup;
        this.transaction = transaction;
        this.date = date;
        this.notDeterministic = notDeterministic;
        this.sessionClass = sessionClass;
        this.currentDate = currentDate;

        this.user = user;
        this.computer = computer;

        this.idSession = idSession;
    }

    public DataSession createSession() throws SQLException {
        return new DataSession(sql, user, computer, isServerRestarting, baseClass, namedObject, sessionClass, name, recognizeGroup, transaction, date, currentDate, notDeterministic, idSession);
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

        applyObject = null; // сбрасываем в том числе когда cancel потому как cancel drop'ает в том числе и добавление объекта
    }

    public DataObject addObject(ConcreteCustomClass customClass, Modifier modifier) throws SQLException {
        return addObject(customClass, modifier, true, true);
    }

    public DataObject addObject() throws SQLException {
        return new DataObject(IDTable.instance.generateID(idSession, IDTable.OBJECT),baseClass.unknown);
    }

    public DataObject addObject(ConcreteCustomClass customClass, Modifier modifier, boolean fillDefault, boolean groupLast) throws SQLException {

        DataObject object = addObject();

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass, groupLast);

        if(fillDefault) {
            if(customClass.isChild(namedObject))
                name.execute(customClass.caption+" "+object.object, this, modifier, object);

            if(customClass.isChild(transaction))
                date.execute(currentDate.read(sql, modifier, env), DataSession.this, modifier, object);
        }

        return object;
    }

    Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        changeClass(change, toClass, true);
    }

    public void changeClass(DataObject change, ConcreteObjectClass toClass, boolean groupLast) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = (ConcreteObjectClass) getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        assert Collections.disjoint(addClasses,removeClasses);

        changeClass(addClasses, removeClasses, toClass, change, sql, groupLast);

        newClasses.put(change,toClass);

        if(groupLast) {
            // по тем по кому не было restart'а new -> to
            updateProperties(getFilterChanges(addClasses, removeClasses));
            for(UpdateChanges incrementChange : incrementChanges.values()) {
                incrementChange.addClasses.addAll(addClasses);
                incrementChange.removeClasses.addAll(removeClasses);
            }
        }
    }

    public void changeProperty(DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, boolean groupLast) throws SQLException {
        changeProperty(property, keys, newValue, sql, groupLast);

        if(groupLast) // по тем по кому не было restart'а new -> to
            updateProperties(getFilterChanges(property));
    }

    public void updateProperties(PropertyChanges changes) {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((FormInstance<?>) incrementChange.getKey()).getUpdateProperties(changes));
    }

    public <P extends PropertyInterface> List<ClientAction> execute(Property<P> property, PropertyChange<P> change, Modifier modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        if(executeForm!=null) // assert что сама форма modifier и session ее же
            executeForm.form.fireChange(property, change);
        return execute(property.getDataChanges(change, modifier, null), executeForm, mapObjects);
    }

    public <P extends PropertyInterface> List<ClientAction> execute(MapDataChanges<P> mapChanges, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {

        DataChanges dataChanges = mapChanges.changes;

        // если идет изменение и есть недетерменированное производное изменение зависищее от него, то придется его "выполнить"
        for(DerivedChange<?,?> derivedChange : notDeterministic) {
            DataChanges derivedChanges = derivedChange.getDataChanges(dataChanges.add(modifier.getPropertyChanges()));
            if(!derivedChanges.isEmpty())
                mapChanges = mapChanges.add(new MapDataChanges<P>(derivedChanges));
        }

        List<ClientAction> actions = new ArrayList<ClientAction>(); // сначала читаем изменения, чтобы не было каскадных непредсказуемых эффектов, потом изменяем
        for(Map.Entry<UserProperty,Map<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>>> propRow : dataChanges.read(sql, env, baseClass).entrySet()) {
            for (Iterator<Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> iterator = propRow.getValue().entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>> row = iterator.next();
                UserProperty property = propRow.getKey();
                Map<ClassPropertyInterface, P> mapInterfaces = mapChanges.map.get(property);
                property.execute(new ExecutionContext(row.getKey(), row.getValue().get("value"), this, (executeForm == null ? modifier : executeForm.form), actions, executeForm, mapInterfaces == null ? null : BaseUtils.nullJoin(mapInterfaces, mapObjects), !iterator.hasNext()));
            }
        }
        return actions;
    }

    public ConcreteClass getCurrentClass(DataObject value) {
        ConcreteClass newClass;
        if((newClass = newClasses.get(value))==null)
            return value.objectClass;
        else
            return newClass;
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
    public Collection<Property> update(FormInstance<?> form, Collection<CustomClass> updateClasses) throws SQLException {
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

        updateClasses.addAll(incrementChange.addClasses);
        updateClasses.addAll(incrementChange.removeClasses);
        return incrementChange.properties;
    }

    public String apply(BusinessLogics<?> BL) throws SQLException {
        return apply(BL, new ArrayList<ClientAction>());
    }

    public String apply(BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException {
        return apply(BL, actions, false);
    }

    public String check(BusinessLogics BL) throws SQLException {
        return apply(BL, new ArrayList<ClientAction>(), true);
    }

    private <T extends PropertyInterface> boolean fitKeyClasses(Property<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys).means(property.mapTable.table.getClasses());
    }

    private <T extends PropertyInterface> boolean fitClasses(Property<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys, property.field).means(property.getClassWhere(property.field));
    }

    private static boolean needPrevious(Property<?> property, BusinessLogics<?> BL) {
        return BL.getConstraintDerivedDependProperties().contains(property);
    }
    private <T extends PropertyInterface, D extends PropertyInterface> void applySingleStored(Property<T> property, SinglePropertyTableUsage<T> change, BusinessLogics<?> BL, IncrementApply incrementApply) throws SQLException {
        // assert что у change классы совпадают с property
        assert property.isStored();
        assert fitClasses(property, change); // проверяет гипотезу
        assert fitKeyClasses(property, change); // дополнительная проверка, она должна обеспечиваться тем что в change не должно быть замен null на null

        if(change.isEmpty())
            return;

        if(needPrevious(property, BL)) // запоминаем предыдущее значение
            incrementApply.updateApplyStart(property, change, baseClass);

        NoUpdate noUpdate = new NoUpdate(); IncrementProps increment = new IncrementProps(property, change);
        Modifier modifier = new OverrideModifier(noUpdate, increment);

        // true нужно предыдущее значение сохранить
        for(Property<D> depend : BL.getAppliedDependFrom(property)) { // !!! важно в лексикографическом порядке должно быть
            SinglePropertyTableUsage<D> dependChange = depend.readChangeTable(sql, modifier, baseClass, env);
            if(depend.isStored()) { // читаем новое значение, запускаем рекурсию
                applySingleStored((Property)depend, (SinglePropertyTableUsage)dependChange, BL, incrementApply);
                noUpdate.add(depend); // докидываем noUpdate чтобы по нескольку раз одну ветку не отрабатывать
            } else {
                assert needPrevious(depend, BL);
                incrementApply.updateApplyStart(depend, dependChange, baseClass);
            }
        }
        savePropertyChanges(property.mapTable.table, Collections.singleton((Property) property), change.map(property.mapTable.mapKeys, (Property)property));
    }

    private void savePropertyChanges(Table implementTable, Collection<Property> props, SessionTableUsage<KeyField, Property> changeTable) throws SQLException {
        Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(implementTable);
        Join<Property> join = changeTable.join(modifyQuery.mapKeys);
        for (Property property : props)
            modifyQuery.properties.put(property.field, join.getExpr(property));
        modifyQuery.and(join.getWhere());
        sql.modifyRecords(new ModifyQuery(implementTable, modifyQuery, env));
    }

    public String apply(final BusinessLogics<?> BL, List<ClientAction> actions, boolean onlyCheck) throws SQLException {
        resolveFollows(BL, false);

        // до чтения persistent свойств в сессию
        if (applyObject == null) {
            applyObject = addObject(sessionClass, modifier);
        }

        sql.startTransaction();

        Map<ExecuteProperty, List<ExecutionContext>> pendingExecutes = new HashMap<ExecuteProperty, List<ExecutionContext>>();
        IncrementApply incrementApply = new IncrementApply(this);

        // тоже нужен посередине, чтобы он успел dataproperty изменить до того как они обработаны
        for (Property<?> property : BL.getAppliedProperties(onlyCheck)) {
            if(property.isFalse) { // ограничения
                String constraintResult = check(property, incrementApply);
                if (constraintResult != null) {
                    // не надо DROP'ать так как Rollback автоматически drop'ает все temporary таблицы
                    incrementApply.cleanIncrementTables();
                    sql.rollbackTransaction();
                    return constraintResult;
                }
            }
            if(property instanceof ExecuteProperty)
                executeDerived((ExecuteProperty) property, incrementApply, actions, pendingExecutes); // действия
            if(property.isStored()) // постоянно-хранимые свойства
                readStored(property, incrementApply, BL);
        }

        // записываем в базу, то что туда еще не сохранено, приходится сохранять группами, так могут не подходить по классам
        for (Map.Entry<ImplementTable, Collection<Property>> groupTable : incrementApply.groupPropertiesByTables().entrySet())
            savePropertyChanges(groupTable.getKey(), groupTable.getValue(), incrementApply.readSave(groupTable.getKey(), groupTable.getValue()));

        for (Map.Entry<DataObject, ConcreteObjectClass> newClass : newClasses.entrySet())
            newClass.getValue().saveClassChanges(sql, newClass.getKey());

        incrementApply.cleanIncrementTables();
        sql.commitTransaction();
        restart(false);

        for(Map.Entry<ExecuteProperty, List<ExecutionContext>> pendingExecute : pendingExecutes.entrySet())
            for(ExecutionContext context : pendingExecute.getValue())
                executePending(pendingExecute.getKey(), context);

        return null;
    }

    private PropertyChange<ClassPropertyInterface> getStoreChange(DataProperty property, IncrementApply incrementApply) {
        PropertyChange<ClassPropertyInterface> change = PropertyChange.addNull(getDataChange(property), property.getDerivedChange(incrementApply, incrementApply.applyStart));
        if(change!=null)
            return change.correctIncrement(property);
        return null;
    }

    private <P extends PropertyInterface> PropertyChange<P> getStoredChange(Property<P> property, IncrementApply incrementApply) throws SQLException {
        if(property instanceof DataProperty) // data property с derived отдельно отрабатываем из-за single apply
            return (PropertyChange<P>) getStoreChange((DataProperty) property, incrementApply);
        else
            if(property.hasChanges(incrementApply))
                return property.getIncrementChange(incrementApply);
        return null;
    }
    @Message("message.session.apply.write")
    private <P extends PropertyInterface> void readStored(@ParamMessage Property<P> property, IncrementApply incrementApply, BusinessLogics<?> BL) throws SQLException {
        PropertyChange<P> change = getStoredChange(property, incrementApply);
        if(change!=null) {
            SinglePropertyTableUsage<P> changeTable = property.readChangeTable(sql, change, baseClass, env);
            if(Settings.instance.isEnableApplySingleStored() && fitClasses(property, changeTable)) {
                applySingleStored(property, changeTable, BL, incrementApply);
                incrementApply.noUpdate.add(property);
            } else
                incrementApply.increment.add(property, changeTable);
        }
    }

    public void executeDerived(@ParamMessage ExecuteProperty property, IncrementApply incrementApply, List<ClientAction> actions, Map<ExecuteProperty, List<ExecutionContext>> pendingExecute) throws SQLException {
        PropertyChange<ClassPropertyInterface> propertyChange = property.getDerivedChange(incrementApply, incrementApply.applyStart);
        if(propertyChange!=null && !propertyChange.isEmpty()) {
            List<ExecutionContext> pendingPropExecute = null;
            if(property.pendingDerivedExecute()) {
                pendingPropExecute = new ArrayList<ExecutionContext>();
                pendingExecute.put(property, pendingPropExecute);
            }

            for (Iterator<Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> iterator = propertyChange.getQuery().executeClasses(sql, env, baseClass).entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>> executeRow = iterator.next();
                Map<ClassPropertyInterface, DataObject> executeKeys = executeRow.getKey();
                ObjectValue executeValue = executeRow.getValue().get("value");

                if(pendingPropExecute!=null)
                    // иначе "pend'им" выполнение, но уже с новыми классами
                    pendingPropExecute.add(new ExecutionContext(getCurrentObjects(executeKeys), getCurrentValue(executeValue), this, modifier, actions, null, null, !iterator.hasNext()));
                else
                    property.execute(new ExecutionContext(executeKeys, executeValue, this, incrementApply, actions, null, null, !iterator.hasNext()));
            }
        }
    }

    @Message("message.session.apply.check")
    public <T extends PropertyInterface> String check(@ParamMessage Property<T> property, IncrementApply incrementApply) throws SQLException {
        if(property.hasChanges(incrementApply) || property.hasChanges(incrementApply.applyStart)) {
            Query<T,String> changed = new Query<T,String>(property);

            WhereBuilder changedWhere = new WhereBuilder();
            property.getIncrementExpr(changed.mapKeys, incrementApply, incrementApply.applyStart, changedWhere, IncrementType.SET);
            changed.and(changedWhere.toWhere()); // только на измененные смотрим

            // сюда надо name'ы вставить
            List<List<String>> propCaptions = new ArrayList<List<String>>();
            for (int i = 0; i < property.interfaces.size(); i++) {
                propCaptions.add(new ArrayList<String>());
            }

            Property.CommonClasses<?> classes = property.getCommonClasses();
            int interfaceIndex = 0;
            for(T propertyInterface : property.interfaces) {
                ValueClass valueClass = classes.interfaces.get(propertyInterface);
                for (Property nameProp : recognizeGroup.getProperties()) {
                    List<ValueClassWrapper> wrapper = Arrays.asList(new ValueClassWrapper(valueClass));
                    if (!nameProp.getProperties(Arrays.asList(wrapper), true).isEmpty()) {
                        Expr nameExpr = nameProp.getExpr(Collections.singletonMap(BaseUtils.single(nameProp.interfaces), changed.mapKeys.get(propertyInterface)), incrementApply);
                        changed.properties.put("int" + propertyInterface.ID + "_" + propCaptions.get(interfaceIndex).size(), nameExpr);
                        propCaptions.get(interfaceIndex).add(nameProp.caption);
                    }
                }
                interfaceIndex++;
            }

            OrderedMap<Map<T, Object>, Map<String, Object>> result = changed.execute(sql, new OrderedMap<String, Boolean>(), 30, env);
            if (result.size() > 0) {
                String resultString = property.toString() + '\n';
                for (Map.Entry<Map<T,Object>, Map<String,Object>> row : result.entrySet()) {
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

                return resultString;
            }
        }

        return null;
    }

    @Message("message.session.apply.auto.execute")
    public void executePending(@ParamMessage ExecuteProperty property, ExecutionContext context) throws SQLException {
        property.execute(context);
    }

    @Message("message.session.apply.resolve.follows")
    public void resolveFollows(final BusinessLogics<?> BL, boolean recalculate) throws SQLException {

        for(Property<?> property : BL.getFollowProperties()) {
            for(PropertyFollows<?, ?> follow : property.follows)
                follow.resolveFalse(this, recalculate);
            for(PropertyFollows<?, ?> follow : property.followed)
                follow.resolveTrue(this, recalculate);
        }
    }

    public final QueryEnvironment env = new QueryEnvironment() {
        public ParseInterface getSQLUser() {
            return new TypeObject(user.getCurrentUser().object, ObjectType.instance);
        }

        public ParseInterface getID() {
            return applyObject ==null?new TypeObject(0, ObjectType.instance):new TypeObject(applyObject);
        }

        public ParseInterface getSQLComputer() {
            return new TypeObject(computer.getCurrentComputer().object, ObjectType.instance);
        }

        public ParseInterface getIsServerRestarting() {
            return new StringParseInterface() {
                public String getString(SQLSyntax syntax) {
                    return isServerRestarting.isServerRestarting()
                           ? LogicalClass.instance.getString(true, syntax)
                           : SQLSyntax.NULL;
                }
            };
        }
    };

    public void changeClass(Set<CustomClass> addClasses, Set<CustomClass> removeClasses, ConcreteObjectClass toClass, DataObject change, SQLSession session, boolean groupLast) throws SQLException {
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

        add.clear();
        remove.clear();
        data.clear();
        news = null;
    }
}
