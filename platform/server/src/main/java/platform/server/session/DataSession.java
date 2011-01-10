package platform.server.session;

import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.base.MutableObject;
import platform.interop.action.ClientAction;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.ComputerController;
import platform.server.form.navigator.UserController;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;
import platform.server.caches.MapValues;

import java.sql.SQLException;
import java.util.*;

public class DataSession extends MutableObject implements SessionChanges, ExprChanges {

    public final Modifier<SimpleChanges> modifier = new Modifier<SimpleChanges>() {
        public SimpleChanges newChanges() {
            return SimpleChanges.EMPTY;
        }

        public SimpleChanges newFullChanges() {
            return new SimpleChanges(this);
        }

        public ExprChanges getSession() {
            return DataSession.this;
        }

        public SimpleChanges used(Property property, SimpleChanges usedChanges) {
            return usedChanges;
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof SimpleChanges;
        }
    };
    public final SQLSession sql;

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
            properties = new HashSet<Property>(form.getUpdateProperties(form));
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
    public final CustomClass transaction;
    public final LP<?> date;
    public final ConcreteCustomClass sessionClass;
    public final LP<?> currentDate;

    // для отладки
    public static boolean reCalculateAggr = false;

    private final List<DerivedChange<?,?>> notDeterministic;

    public final UserController user;
    public final ComputerController computer;

    public DataObject applyObject = null;

    public DataSession(SQLSession sql, final UserController user, final ComputerController computer, BaseClass baseClass, CustomClass namedObject, ConcreteCustomClass sessionClass, LP<?> name, CustomClass transaction, LP<?> date, LP<?> currentDate, List<DerivedChange<?,?>> notDeterministic) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.sql = sql;

        this.baseClass = baseClass;
        this.namedObject = namedObject;
        this.name = name;
        this.transaction = transaction;
        this.date = date;
        this.notDeterministic = notDeterministic;
        this.sessionClass = sessionClass;
        this.currentDate = currentDate;

        this.user = user;
        this.computer = computer;
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

    public DataObject addObject(ConcreteCustomClass customClass, Modifier<? extends Changes> modifier) throws SQLException {

        DataObject object = new DataObject(IDTable.instance.generateID(sql, IDTable.OBJECT),baseClass.unknown);

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass);

        if(customClass.isChild(namedObject))
            name.execute(customClass.caption+" "+object.object, this, modifier, object);

        if(customClass.isChild(transaction))
            date.execute(currentDate.read(sql, modifier, env), DataSession.this, modifier, object);

        return object;
    }

    Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = (ConcreteObjectClass) getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        assert Collections.disjoint(addClasses,removeClasses);

        changeClass(addClasses, removeClasses, toClass, change, sql);

        newClasses.put(change,toClass);

        // по тем по кому не было restart'а new -> to
        updateProperties(new UsedSimpleChanges(new SimpleChanges(getUsedChanges(), addClasses, removeClasses, true)));
        for(UpdateChanges incrementChange : incrementChanges.values()) {
            incrementChange.addClasses.addAll(addClasses);
            incrementChange.removeClasses.addAll(removeClasses);
        }
    }

    public void changeProperty(DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue) throws SQLException {
        changeProperty(property, keys, newValue, sql);

        // по тем по кому не было restart'а new -> to
        updateProperties(new UsedSimpleChanges(new SimpleChanges(getUsedChanges(), property)));
    }

    private void updateProperties(ExprChanges changes) {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((FormInstance<?>) incrementChange.getKey()).getUpdateProperties(changes));
    }

    public void updateProperties(Modifier<? extends Changes> modifier) {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((FormInstance<?>) incrementChange.getKey()).getUpdateProperties(modifier));
    }

    public <P extends PropertyInterface> List<ClientAction> execute(Property<P> property, PropertyChange<P> change, Modifier<? extends Changes> modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        return execute(property.getDataChanges(change, null, modifier), executeForm, mapObjects);
    }


    public <P extends PropertyInterface> List<ClientAction> execute(MapDataChanges<P> mapChanges, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {

        DataChanges dataChanges = mapChanges.changes;

        // если идет изменение и есть недетерменированное производное изменение зависищее от него, то придется его "выполнить"
        for(DerivedChange<?,?> derivedChange : notDeterministic) {
            DataChanges derivedChanges = derivedChange.getDataChanges(new DataChangesModifier(modifier, dataChanges));
            if(!derivedChanges.isEmpty())
                mapChanges = mapChanges.add(new MapDataChanges<P>(derivedChanges));
        }

        // сначала читаем изменения, чтобы не было каскадных непредсказуемых эффектов
        Map<UserProperty, Map<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> propRows = new HashMap<UserProperty, Map<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>>();
        for(int i=0;i<dataChanges.size;i++)
            propRows.put(dataChanges.getKey(i), dataChanges.getValue(i).getQuery().executeClasses(sql, env, baseClass));

        // потом изменяем
        List<ClientAction> actions = new ArrayList<ClientAction>();
        for(Map.Entry<UserProperty,Map<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>>> propRow : propRows.entrySet()) 
            for(Map.Entry<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>> row : propRow.getValue().entrySet()) {
                UserProperty property = propRow.getKey();
                Map<ClassPropertyInterface, P> mapInterfaces = mapChanges.map.get(property);
                property.execute(row.getKey(), row.getValue().get("value"), DataSession.this, actions, executeForm, mapInterfaces==null?null:BaseUtils.nullJoin(mapInterfaces, mapObjects));
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

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        return new DataObject(value,type.getDataClass(value, sql, baseClass));
    }

    public ObjectValue getObjectValue(Object value, Type type) throws SQLException {
        if(value==null)
            return NullValue.instance;
        else
            return getDataObject(value, type);
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

    public String apply(final BusinessLogics<?> BL) throws SQLException {
        String check = check(BL);
        if(check!=null)
            return check;

        write(BL, new ArrayList<ClientAction>());
        return null;
    }

    public String check(final BusinessLogics<?> BL) throws SQLException {

        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(Property<?> property : BL.getAppliedProperties())
            if(property.isFalse && property.hasChanges(modifier)) {
                String constraintResult = check(property);
                if(constraintResult!=null) {
                    // не надо DROP'ать так как Rollback автоматически drop'ает все temporary таблицы
                    return constraintResult;
                }
            }
        return null;
    }

    public <T extends PropertyInterface> String check(Property<T> property) throws SQLException {
        if(property.isFalse) {
            Query<T,String> changed = new Query<T,String>(property);

            WhereBuilder changedWhere = new WhereBuilder();
            Expr valueExpr = property.getExpr(changed.mapKeys, modifier, changedWhere);
            changed.and(valueExpr.getWhere());
            changed.and(changedWhere.toWhere()); // только на измененные смотрим

            // сюда надо name'ы вставить
            for(T propertyInterface : property.interfaces) {
                Expr nameExpr;
                if(property.getInterfaceType(propertyInterface) instanceof ObjectType) // иначе assert'ионы с compatible'ами нарушатся, если ключ скажем число
                    nameExpr = name.getExpr(modifier, changed.mapKeys.get(propertyInterface));
                else
                    nameExpr = CaseExpr.NULL;
                changed.properties.put("int"+propertyInterface.ID, nameExpr);
            }

            OrderedMap<Map<T, Object>, Map<String, Object>> result = changed.execute(sql, env);
            if(result.size()>0) {
                String resultString = property.toString() + '\n';
                for(Map.Entry<Map<T,Object>,Map<String,Object>> row : result.entrySet()) {
                    String objects = "";
                    for(T propertyInterface : property.interfaces)
                        objects = (objects.length()==0?"":objects+", ") + BaseUtils.nvl((String)row.getValue().get("int"+propertyInterface.ID),row.getKey().get(propertyInterface).toString()).trim();
                    resultString += "    " + objects + '\n';
                }

                return resultString;
            }
        }

        return null;
    }

    public void executeDerived(final BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException {

        for(ExecuteProperty property : BL.getExecuteDerivedProperties()) {
            PropertyChange<ClassPropertyInterface> propertyChange = property.derivedChange.getDataChanges(modifier).get(property);
            if(propertyChange!=null)
                for(Map.Entry<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>> executeRow :
                        propertyChange.getQuery().executeClasses(sql, env, baseClass).entrySet())
                    property.execute(executeRow.getKey(), executeRow.getValue().get("value"), this, actions, null, null);
        }        
    }

    public void write(final BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException {
        // до start transaction
        if(applyObject==null)
            applyObject = addObject(sessionClass, modifier);

        executeDerived(BL, actions);

        sql.startTransaction();

        Collection<SessionTableUsage<KeyField, Property>> temporary = new ArrayList<SessionTableUsage<KeyField, Property>>();

        IncrementApply increment = new IncrementApply(DataSession.this);

        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(Property<?> property : BL.getAppliedProperties())
            if (property.isStored() && property.hasChanges(increment))
                temporary.add(increment.read(property.mapTable.table, Collections.<Property>singleton(property), baseClass));

        // записываем в базу
        for(Map.Entry<ImplementTable,Collection<Property>> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable,Property>(){public ImplementTable group(Property key) {return key.mapTable.table;}},
                increment.tables.keySet()).entrySet()) {
            SessionTableUsage<KeyField, Property> changeTable;
            if(groupTable.getValue().size()==1) // временно так - если одна берем старую иначе группой
                changeTable = increment.tables.get(BaseUtils.single(groupTable.getValue()));
            else {
                changeTable = increment.read(groupTable.getKey(), groupTable.getValue(), baseClass);
                temporary.add(changeTable);
            }

            Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(groupTable.getKey());
            Join<Property> join = changeTable.join(modifyQuery.mapKeys);
            for(Property property : groupTable.getValue())
                modifyQuery.properties.put(property.field,join.getExpr(property));
            modifyQuery.and(join.getWhere());
            sql.modifyRecords(new ModifyQuery(groupTable.getKey(), modifyQuery, env));
        }

        for(Map.Entry<DataObject, ConcreteObjectClass> newClass : newClasses.entrySet())
            newClass.getValue().saveClassChanges(sql,newClass.getKey());

        for(SessionTableUsage<KeyField, Property> addTable : temporary)
            addTable.drop(sql);

        sql.commitTransaction();

        restart(false);
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
    };

    public Map<CustomClass, SingleKeyNoPropertyUsage> add = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    public Map<CustomClass, SingleKeyNoPropertyUsage> remove = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    public Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>();

    public SingleKeyPropertyUsage news = null;

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty() || news!=null;
    }

    public void changeClass(Set<CustomClass> addClasses, Set<CustomClass> removeClasses, ConcreteObjectClass toClass, DataObject change, SQLSession session) throws SQLException {
        for(CustomClass addClass : addClasses) {
            SingleKeyNoPropertyUsage addTable = add.get(addClass);
            if(addTable==null) { // если нету таблицы создаем
                addTable = new SingleKeyNoPropertyUsage(ObjectType.instance);
                add.put(addClass, addTable);
            }
            addTable.insertRecord(session, change, false);

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
            removeTable.insertRecord(session, change, false);

            SingleKeyNoPropertyUsage addTable = add.get(removeClass);
            if(addTable!=null)
                addTable.deleteRecords(session,change);
        }

        for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> dataChange : data.entrySet()) { // удаляем существующие изменения
            DataProperty property = dataChange.getKey();
            for(ClassPropertyInterface propertyInterface : property.interfaces)
                if(propertyInterface.interfaceClass instanceof CustomClass && removeClasses.contains((CustomClass)propertyInterface.interfaceClass))
                    dataChange.getValue().deleteKey(session, propertyInterface, change);
            if(property.value instanceof CustomClass && removeClasses.contains((CustomClass) property.value))
                dataChange.getValue().deleteProperty(session, "value", change);
        }

        if(news ==null)
            news = new SingleKeyPropertyUsage(ObjectType.instance, ObjectType.instance);
        news.insertRecord(session, change, toClass.getClassObject(), true);
    }

    public void changeProperty(final DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, SQLSession session) throws SQLException {
        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange == null) { // создадим таблицу, если не было
            dataChange = property.createChangeTable();
            data.put(property, dataChange);
        }
        dataChange.insertRecord(session, keys, newValue, true);
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

        add.clear();
        remove.clear();
        data.clear();
        news = null;
    }

    public Where getIsClassWhere(Expr expr, ValueClass isClass, WhereBuilder changedWheres) {
        Where isClassWhere = expr.isClass(isClass.getUpSet());
        
        if(isClass instanceof CustomClass) {
            SingleKeyNoPropertyUsage removeTable = remove.get((CustomClass)isClass);
            if(removeTable!=null) {
                Where removeWhere = removeTable.getWhere(expr);
                isClassWhere = isClassWhere.and(removeWhere.not());
                if(changedWheres!=null) changedWheres.add(removeWhere);
            }

            SingleKeyNoPropertyUsage addTable = add.get((CustomClass)isClass);
            if(addTable!=null) {
                Where addWhere = addTable.getWhere(expr);
                isClassWhere = isClassWhere.or(addWhere);
                if(changedWheres!=null) changedWheres.add(addWhere);
            }
        }
        return isClassWhere;
    }

    public Expr getIsClassExpr(Expr expr, BaseClass baseClass, WhereBuilder changedWheres) {
        Expr isClassExpr = expr.classExpr(baseClass);

        if(news !=null) {
            Join<String> newJoin = news.join(expr);
            Where newWhere = newJoin.getWhere();
            isClassExpr = newJoin.getExpr("value").ifElse(newWhere, isClassExpr);
            if(changedWheres!=null) changedWheres.add(newWhere);
        }

        return isClassExpr;
    }

    public Join<String> getDataChange(DataProperty property, Map<ClassPropertyInterface, ? extends Expr> joinImplement) {
        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange!=null)
            return dataChange.join(joinImplement);
        else
            return null;
    }

    public Where getRemoveWhere(ValueClass valueClass, Expr expr) {
        SingleKeyNoPropertyUsage removeTable;
        if(valueClass instanceof CustomClass && ((removeTable = remove.get((CustomClass)valueClass))!=null))
            return removeTable.getWhere(expr);
        else
            return Where.FALSE;
    }

    public ExprChanges getSessionChanges(DataProperty property) {
        return new UsedSimpleChanges(new SimpleChanges(getUsedChanges(), property));
    }

    // IMMUTABLE
    public SimpleChanges getUsedChanges() {
        Map<CustomClass, MapValues> usedAdd = new HashMap<CustomClass, MapValues>();
        for(Map.Entry<CustomClass, SingleKeyNoPropertyUsage> change : add.entrySet())
            usedAdd.put(change.getKey(), change.getValue().getUsage());
        Map<CustomClass, MapValues> usedRemove = new HashMap<CustomClass, MapValues>();
        for(Map.Entry<CustomClass, SingleKeyNoPropertyUsage> change : remove.entrySet())
            usedRemove.put(change.getKey(), change.getValue().getUsage());
        Map<DataProperty, MapValues> usedData = new HashMap<DataProperty, MapValues>();
        for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> change : data.entrySet())
            usedData.put(change.getKey(), change.getValue().getUsage());
        return new SimpleChanges(usedAdd, usedRemove, usedData, news != null ? news.getUsage() : null);
    }
}
