package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.*;
import platform.server.data.expr.*;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.AbstractClassWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.SessionDataProperty;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.table.MapKeysTable;
import platform.server.logics.table.TableFactory;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public abstract class Property<T extends PropertyInterface> extends AbstractNode implements MapKeysInterface<T>, ServerIdentitySerializable {

    public final String sID;

    public String caption;

    public boolean askConfirm = false;

    public String toString() {
        return caption;
    }

    public int ID = 0;

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public final Collection<T> interfaces;

    public boolean check() {
        return !getClassWhere().isFalse();
    }

    public <P extends PropertyInterface> boolean intersect(Property<P> property, Map<P, T> map) {
        return !getClassWhere().and(new ClassWhere<T>(property.getClassWhere(), map)).isFalse();
    }

    public boolean isInInterface(Map<T, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return isAny ? anyInInterface(interfaceClasses) : allInInterface(interfaceClasses);        
    }

    @IdentityLazy
    public boolean allInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).meansCompatible(getClassWhere());
    }

    @IdentityLazy
    public boolean anyInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere().andCompatible(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    @IdentityLazy
    public boolean isFull() {
        boolean result = true;
        for (AbstractClassWhere.And where : getClassWhere().wheres) {
            for (T i : interfaces) {
                result = result && (where.get(i) != null);
            }
            result = result && !(where).containsNullValue();
        }
        return result;
    }

    public Property(String sID, String caption, List<T> interfaces) {
        this.sID = sID;
        this.caption = caption;
        this.interfaces = interfaces;

        changeExpr = new PullExpr(toString() + " value");
    }

    public Map<Time, TimeChangeDataProperty<T>> timeChanges = new HashMap<Time, TimeChangeDataProperty<T>>();

    protected void fillDepends(Set<Property> depends, boolean derived) {
    }

    public boolean notDeterministic() {
        Set<Property> depends = new HashSet<Property>();
        fillDepends(depends, false);
        for (Property property : depends)
            if (property.notDeterministic())
                return true;
        return false;
    }

    public Set<Property> getDepends() {
        Set<Property> depends = new HashSet<Property>();
        fillDepends(depends, true);
        return depends;
    }

    @IdentityLazy
    public Map<T, KeyExpr> getMapKeys() {
        Map<T, KeyExpr> result = new HashMap<T, KeyExpr>();
        for (T propertyInterface : interfaces)
            result.put(propertyInterface, new KeyExpr(propertyInterface.toString()));
        return result;
    }

    protected static class DefaultChanges extends Changes<DefaultChanges> {
        private DefaultChanges() {
        }

        public final static DefaultChanges EMPTY = new DefaultChanges();

        private DefaultChanges(DefaultChanges changes, SessionChanges merge) {
            super(changes, merge);
        }

        public DefaultChanges addChanges(SessionChanges changes) {
            return new DefaultChanges(this, changes);
        }

        private DefaultChanges(DefaultChanges changes, DefaultChanges merge) {
            super(changes, merge);
        }

        public DefaultChanges add(DefaultChanges changes) {
            return new DefaultChanges(this, changes);
        }

        public DefaultChanges(DefaultChanges changes, MapValuesTranslate mapValues) {
            super(changes, mapValues);
        }

        public DefaultChanges translate(MapValuesTranslate mapValues) {
            return new DefaultChanges(this, mapValues);
        }
    }

    public static Modifier<DefaultChanges> defaultModifier = new Modifier<DefaultChanges>() {
        public DefaultChanges newChanges() {
            return DefaultChanges.EMPTY;
        }

        public SessionChanges getSession() {
            return SessionChanges.EMPTY;
        }

        public DefaultChanges fullChanges() {
            return DefaultChanges.EMPTY;
        }

        public DefaultChanges used(Property property, DefaultChanges usedChanges) {
            return usedChanges;
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof DefaultChanges;
        }
    };

    public Expr getExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, defaultModifier, null);
    }

    public Expr getClassExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, SessionDataProperty.modifier, null);
    }

    public <U extends Changes<U>> Expr getExpr(Map<T, ? extends Expr> joinImplement, Modifier<U> modifier, WhereBuilder changedWhere) {

        assert joinImplement.size() == interfaces.size();

        WhereBuilder changedExprWhere = new WhereBuilder();
        Expr changedExpr = modifier.changed(this, joinImplement, changedExprWhere);

        if (changedExpr == null && isStored()) {
            if (!hasChanges(modifier)) // если нету изменений
                return mapTable.table.join(BaseUtils.join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if (usePreviousStored())
                changedExpr = calculateExpr(joinImplement, modifier, changedExprWhere);
        }

        if (changedExpr != null) {
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
        } else
            return calculateExpr(joinImplement, modifier, changedWhere);
    }

    public Expr calculateExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, defaultModifier, null);
    }

    public Expr calculateClassExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, SessionDataProperty.modifier, null);
    }

    protected abstract Expr calculateExpr(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere);

    @IdentityLazy
    public ClassWhere<T> getClassWhere() {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return new Query<T, String>(mapKeys, getClassExpr(mapKeys), "value").getClassWhere(new ArrayList<String>());
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public CustomClass getDialogClass(Map<T, DataObject> mapValues, Map<T, ConcreteClass> mapClasses, Map<T, PropertyObjectInterfaceInstance> mapObjects) {
        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for (Map.Entry<T, DataObject> keyField : mapValues.entrySet())
            mapExprs.put(keyField.getKey(), new ValueExpr(keyField.getValue().object, mapClasses.get(keyField.getKey())));
        return (CustomClass) new Query<String, String>(new HashMap<String, KeyExpr>(), getClassExpr(mapExprs), "value").
                getClassWhere(Collections.singleton("value")).getSingleWhere("value").getOr().getCommonClass();
    }

    public abstract Type getType();

    public Type getEditorType(Map<T, PropertyObjectInterfaceInstance> mapObjects) {
        return getType();
    }

    @IdentityLazy
    public Type getInterfaceType(T propertyInterface) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return mapKeys.get(propertyInterface).getType(getClassExpr(mapKeys).getWhere());
    }

    // возвращает от чего "зависят" изменения - с callback'ов
    protected abstract <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier);

    public <U extends Changes<U>> U aspectGetUsedChanges(Modifier<U> modifier) {
        return modifier.used(this, calculateUsedChanges(modifier));
    }

    public <U extends Changes<U>> U getUsedChanges(Modifier<U> modifier) {
        return aspectGetUsedChanges(modifier);
    }

    public boolean hasChanges(Modifier<? extends Changes> modifier) {
        return getUsedChanges(modifier).hasChanges();
    }

    public boolean isObject() {
        return true;
    }

    public PropertyField field;

    public static class CommonClasses<T extends PropertyInterface> {
        public Map<T, ValueClass> interfaces;
        public ValueClass value;

        public CommonClasses(Map<T, ValueClass> interfaces, ValueClass value) {
            this.interfaces = interfaces;
            this.value = value;
        }
    }

    public Map<T, ValueClass> getMapClasses() {
        return getCommonClasses().interfaces;
    }

    public abstract CommonClasses<T> getCommonClasses();

    protected abstract ClassWhere<Field> getClassWhere(PropertyField storedField);

    public boolean cached = false;

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent

    public void markStored(TableFactory tableFactory) {
        mapTable = tableFactory.getMapTable(getMapClasses());

        PropertyField storedField = new PropertyField(sID, getType());
        mapTable.table.addField(storedField, getClassWhere(storedField));

        // именно после так как высчитали, а то сама себя stored'ом считать будет
        field = storedField;

        assert !cached;
    }

    public abstract boolean isStored();

    public boolean isFalse = false;
    public boolean checkChange = true;

    public Map<T, T> getIdentityInterfaces() {
        return BaseUtils.toMap(new HashSet<T>(interfaces));
    }

    public PropertyMapImplement<?, T> getChangeImplement() {
        return new PropertyMapImplement<T, T>(this, getIdentityInterfaces());
    }

    public Object read(SQLSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier) throws SQLException {
        String readValue = "readvalue";
        Query<T, Object> readQuery = new Query<T, Object>(this);

        readQuery.putKeyWhere(keys);

        readQuery.properties.put(readValue, getExpr(readQuery.mapKeys, modifier, null));
        return BaseUtils.singleValue(readQuery.execute(session)).get(readValue);
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier) throws SQLException {
        return session.getObjectValue(read(session, keys, modifier), getType());
    }

    public Expr getIncrementExpr(Map<KeyField, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<T, ? extends Expr> joinKeys = BaseUtils.join(mapTable.mapKeys, joinImplement);
        WhereBuilder incrementWhere = new WhereBuilder();
        Expr incrementExpr = getExpr(joinKeys, modifier, incrementWhere);
        changedWhere.add(incrementWhere.toWhere().and(incrementExpr.getWhere().or(getExpr(joinKeys).getWhere()))); // если старые или новые изменились
        return incrementExpr;
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean usePreviousStored();

    @IdentityLazy
    public <P extends PropertyInterface> MaxChangeProperty<T, P> getMaxChangeProperty(Property<P> change) {
        return new MaxChangeProperty<T, P>(this, change);
    }

    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        U result = calculateUsedDataChanges(modifier);
        for (TimeChangeDataProperty<T> timeProperty : timeChanges.values())
            result = result.add(timeProperty.getUsedDataChanges(modifier));
        return result;
    }

    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        WhereBuilder calculateChangedWhere = timeChanges.isEmpty() ? changedWhere : new WhereBuilder();
        MapDataChanges<T> dataChanges = calculateDataChanges(change, calculateChangedWhere, modifier);
        for (Map.Entry<Time, TimeChangeDataProperty<T>> timeChange : timeChanges.entrySet()) // обновляем свойства времени изменения
            dataChanges = dataChanges.add(timeChange.getValue().getDataChanges(new PropertyChange<ClassPropertyInterface>(
                    BaseUtils.join(timeChange.getValue().mapInterfaces, change.mapKeys),
                    new TimeExpr(timeChange.getKey()), calculateChangedWhere.toWhere()), null, modifier).map(timeChange.getValue().mapInterfaces));
        if (changedWhere != null && !timeChanges.isEmpty())
            changedWhere.add(calculateChangedWhere.toWhere());
        return dataChanges;
    }

    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
        return modifier.newChanges();
    }

    // для оболочки чтобы всем getDataChanges можно было бы timeChanges вставить
    protected MapDataChanges<T> calculateDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return new MapDataChanges<T>();
    }

    public Map<T, Expr> getChangeExprs() {
        Map<T, Expr> result = new HashMap<T, Expr>();
        for (T propertyInterface : interfaces)
            result.put(propertyInterface, propertyInterface.changeExpr);
        return result;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    public final Expr changeExpr;

    private DataChanges getDataChanges(Modifier<? extends Changes> modifier, boolean toNull) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return getDataChanges(new PropertyChange<T>(mapKeys, toNull ? CaseExpr.NULL : changeExpr, CompareWhere.compare(mapKeys, getChangeExprs())), null, modifier).changes;
    }

    public Modifier<? extends Changes> getChangeModifier(Modifier<? extends Changes> modifier, boolean toNull) {
        // строим Where для изменения
        return new DataChangesModifier(modifier, getDataChanges(modifier, toNull));
    }

    public Collection<DataProperty> getDataChanges() { // не должно быть Action'ов
        return (Collection<DataProperty>) ((Collection<? extends Property>) getDataChanges(defaultModifier, false).keys());
    }

    protected MapDataChanges<T> getJoinDataChanges(Map<T, ? extends Expr> implementExprs, Expr expr, Where where, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder changedImplementWhere = cascadeWhere(changedWhere);
        MapDataChanges<T> result = getDataChanges(new PropertyChange<T>(mapKeys,
                GroupExpr.create(implementExprs, expr, where, true, mapKeys),
                GroupExpr.create(implementExprs, ValueExpr.TRUE, where, true, mapKeys).getWhere()),
                changedImplementWhere, modifier);
        if (changedWhere != null)
            changedWhere.add(new Query<T, Object>(mapKeys, changedImplementWhere.toWhere()).join(implementExprs).getWhere());// нужно перемаппить назад
        return result;
    }

    public PropertyMapImplement<T, T> getImplement() {
        return new PropertyMapImplement<T, T>(this, getIdentityInterfaces());
    }

    public void setConstraint(boolean checkChange) {
        isFalse = true;
        this.checkChange = checkChange;
    }

    // используется если создаваемый WhereBuilder нужен только если задан changed 
    public static WhereBuilder cascadeWhere(WhereBuilder changed) {
        return changed == null ? null : new WhereBuilder();
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier<? extends Changes> modifier) throws SQLException {
        return getChangeImplement().execute(keys, session, value, modifier);
    }

    // по умолчанию заполняет свойства
    // assert что entity этого свойства
    public void proceedDefaultDraw(PropertyDrawEntity<T> entity, FormEntity form) {
    }

    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<T> entity) {
    }

    public boolean hasChild(Property prop) {
        return prop.equals(this);
    }

    public List<ConcreteCustomClass> getClasses() {
        return new ArrayList<ConcreteCustomClass>();
    }

    public List<Property> getProperties() {
        return Collections.singletonList((Property)this);
    }

    @Override
    public List<PropertyClassImplement> getProperties(Collection<List<ValueClassWrapper>> classLists, boolean anyInInterface) {
        List<PropertyClassImplement> resultList = new ArrayList<PropertyClassImplement>();
        if (isFull()) {
            for (List<ValueClassWrapper> classes : classLists) {
                if (interfaces.size() == classes.size()) {
                    for (List<T> mapping : new ListPermutations<T>(interfaces)) {
                        Map<T, AndClassSet> propertyInterface = new HashMap<T, AndClassSet>();
                        int interfaceCount = 0;
                        for (T iface : mapping) {
                            ValueClass propertyClass = classes.get(interfaceCount++).valueClass;
                            propertyInterface.put(iface, propertyClass.getUpSet());
                        }

                        if (isInInterface(propertyInterface, anyInInterface)) {
                            resultList.add(new PropertyClassImplement<T>(this, classes, mapping));
                        }
                    }
                }
            }
        }
        return resultList;
    }

    public T getInterfaceById(int iID) {
        for (T inter : interfaces) {
            if (inter.getID() == iID) {
                return inter;
            }
        }

        return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeUTF(sID);
        outStream.writeUTF(caption);

        pool.serializeCollection(outStream, interfaces);
        pool.serializeObject(outStream, getParent());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        //десериализация не нужна, т.к. вместо создания объекта, происходит поиск в BL
    }
}
