package platform.server.logics.property;

import platform.base.*;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.server.Message;
import platform.server.Settings;
import platform.server.ThisMessage;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.PackComplex;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.*;
import platform.server.data.expr.*;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.AbstractClassWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.panellocation.*;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.*;
import platform.server.logics.linear.LP;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.panellocation.ShortcutPanelLocation;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.table.MapKeysTable;
import platform.server.logics.table.TableFactory;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.*;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.join;

public abstract class Property<T extends PropertyInterface> extends AbstractNode implements MapKeysInterface<T>, ServerIdentitySerializable {

    private String sID;

    // вот отсюда идут свойства, которые отвечают за логику представлений и подставляются автоматически для PropertyDrawEntity и PropertyDrawView

    public String caption;
    public String toolTip;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    public boolean loggable;
    public boolean setNotNull;
    private LP logProperty;
    private LP logFormProperty;

    public void setFixedCharWidth(int charWidth) {
        minimumCharWidth = charWidth;
        maximumCharWidth = charWidth;
        preferredCharWidth = charWidth;
    }

    public void inheritFixedCharWidth(Property property) {
        minimumCharWidth = property.minimumCharWidth;
        maximumCharWidth = property.maximumCharWidth;
        preferredCharWidth = property.preferredCharWidth;
    }

    private ImageIcon image;
    private String iconPath;

    public void inheritImage(Property property) {
        image = property.image;
        iconPath = property.iconPath;
    }

    public void setImage(String iconPath) {
        this.iconPath = iconPath;
        this.image = new ImageIcon(Property.class.getResource("/images/" + iconPath));
    }

    public KeyStroke editKey;
    public Boolean showEditKey;

    public String regexp;
    public String regexpMessage;
    public Boolean echoSymbols;

    public PanelLocation panelLocation;

    public Boolean shouldBeLast;

    public ClassViewType forceViewType;

    public Boolean askConfirm;

    public boolean autoset;

    public String toString() {
        return caption;
    }

    public int ID = 0;

    public String getCode() {
        return getSID();
    }

    public boolean isField() {
        return false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public LP getLogProperty() {
        return logProperty;
    }

    public void setLogProperty(LP logProperty) {
        this.logProperty = logProperty;
    }

    public LP getLogFormProperty() {
        return logFormProperty;
    }

    public void setLogFormProperty(LP logFormProperty) {
        this.logFormProperty = logFormProperty;
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

    
    private boolean calculateIsFull() {
        ClassWhere<T> classWhere = getClassWhere();
        if(classWhere.isFalse())
            return false;
        for (AbstractClassWhere.And where : classWhere.wheres) {
            for (T i : interfaces)
                if(where.get(i)==null)
                    return false;
            if(where.containsNullValue())
                return false;
        }
        return true;
    }
    private Boolean isFull;
    private static ThreadLocal<Boolean> isFullRunning = new ThreadLocal<Boolean>();
    @ManualLazy
    public boolean isFull() {
        if(isFull==null) {
            if(isFullRunning.get()!=null)
                return false;
            isFullRunning.set(true);

            isFull = calculateIsFull();

            isFullRunning.set(null);
        }
        return isFull;
    }

    public Property(String sID, String caption, List<T> interfaces) {
        this.setSID(sID);
        this.caption = caption;
        this.interfaces = interfaces;

        changeExpr = new PullExpr(toString() + " value");
    }

    public Map<Time, TimePropertyChange<T>> timeChanges = new HashMap<Time, TimePropertyChange<T>>();

    protected void fillDepends(Set<Property> depends, boolean derived) {
    }

    public boolean notDeterministic() {
        for (Property property : getDepends(false))
            if (property.notDeterministic())
                return true;
        return false;
    }

    public Set<Property> getDepends(boolean derived) {
        Set<Property> depends = new HashSet<Property>();
        fillDepends(depends, derived);
        return depends;
    }

    public Set<Property> getDepends() {
        return getDepends(true);
    }

    @IdentityLazy
    public Map<T, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(interfaces);
    }

    public static Modifier defaultModifier = new Modifier() {
        public PropertyChanges getPropertyChanges() {
            return PropertyChanges.EMPTY;
        }
    };

    public Expr getExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, PropertyChanges.EMPTY);
    }

    public Expr getClassExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, SessionDataProperty.modifier);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getExpr(joinImplement, modifier.getPropertyChanges());
    }
    public Expr getExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return getExpr(joinImplement, propChanges, null);
    }

    public PropertyChange<T> getIncrementChange(Modifier modifier) {
        return getIncrementChange(modifier.getPropertyChanges());
    }

    public PropertyChange<T> getIncrementChange(PropertyChanges propChanges) {
        IQuery<T, String> incrementQuery = getQuery(propChanges, PropertyQueryType.FULLCHANGED, new HashMap<T, Expr>());
        return new PropertyChange<T>(incrementQuery.getMapKeys(), incrementQuery.getExpr("value"), incrementQuery.getExpr("changed").getWhere());
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier, WhereBuilder resultChanged) {
        return getIncrementExpr(joinImplement, modifier.getPropertyChanges(), resultChanged);
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier, Modifier prevModifier, WhereBuilder resultChanged, IncrementType incrementType) {
        return getIncrementExpr(joinImplement, modifier.getPropertyChanges(), prevModifier.getPropertyChanges(), resultChanged, incrementType);
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder resultChanged) {
        return getIncrementExpr(joinImplement, propChanges, resultChanged, IncrementType.SUSPICION);
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder resultChanged, IncrementType incrementType) {
        return getIncrementExpr(joinImplement, propChanges, PropertyChanges.EMPTY, resultChanged, incrementType);
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges newChanges, PropertyChanges prevChanges, WhereBuilder resultChanged, IncrementType incrementType) {
        WhereBuilder incrementWhere = new WhereBuilder();
        Expr newExpr = getExpr(joinImplement, newChanges, incrementWhere);
        Expr prevExpr = getExpr(joinImplement, prevChanges, incrementWhere);

        Where forceWhere;
        switch(incrementType) {
            case CHANGESET:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere()).and(newExpr.getWhere().and(prevExpr.getWhere()).not());
                break;
            case SET:
                forceWhere = newExpr.getWhere().and(prevExpr.getWhere().not());
                break;
            case DROP:
                forceWhere = newExpr.getWhere().not().and(prevExpr.getWhere());
                break;
            case CHANGE:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere()).and(newExpr.compare(prevExpr, Compare.EQUALS).not());
                break;
            case SUSPICION:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere());
                break;
            default:
                throw new RuntimeException("should not be");
        }
        resultChanged.add(incrementWhere.toWhere().and(forceWhere));
        return newExpr;
    }

    public Expr aspectGetExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {

        assert joinImplement.size() == interfaces.size();

        WhereBuilder changedExprWhere = new WhereBuilder();
        Expr changedExpr = propChanges.getChangeExpr(this, joinImplement, changedExprWhere);

        if (changedExpr == null && isStored()) {
            if (!hasChanges(propChanges)) // если нету изменений
                return mapTable.table.join(join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if (useSimpleIncrement())
                changedExpr = calculateExpr(joinImplement, propChanges, changedExprWhere);
        }

        if (changedExpr != null) {
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
        } else
            return calculateExpr(joinImplement, propChanges, changedWhere);
    }

    @PackComplex
    @Message("message.core.property.get.expr")
    @ThisMessage
    public IQuery<T, String> getQuery(PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            Query<T, String> fullQuery = new Query<T, String>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.properties.put("value", newExpr);
            fullQuery.properties.put("changed", query.getExpr("changed").and(newExpr.getWhere().or(getExpr(fullQuery.mapKeys).getWhere())));
            return fullQuery;
        }
            
        Query<T, String> query = new Query<T,String>(BaseUtils.filterNotKeys(getMapKeys(), interfaceValues.keySet()));
        Map<T, Expr> allKeys = BaseUtils.merge(interfaceValues, query.mapKeys);
        WhereBuilder queryWheres = queryType.needChange() ? new WhereBuilder():null;
        query.properties.put("value", aspectGetExpr(allKeys, propChanges, queryWheres));
        if(queryType.needChange())
            query.properties.put("changed", ValueExpr.get(queryWheres.toWhere()));
        return query;
    }

    public Expr getQueryExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWheres) {

        Map<T, Expr> interfaceValues = new HashMap<T, Expr>(); Map<T, Expr> interfaceExprs = new HashMap<T, Expr>();
        for(Map.Entry<T, ? extends Expr> entry : joinImplement.entrySet())
            if(entry.getValue().isValue())
                interfaceValues.put(entry.getKey(), entry.getValue());
            else
                interfaceExprs.put(entry.getKey(), entry.getValue());

        IQuery<T, String> query = getQuery(propChanges, changedWheres!=null?PropertyQueryType.CHANGED:PropertyQueryType.NOCHANGE, interfaceValues);

        Join<String> queryJoin = query.join(interfaceExprs);
        if(changedWheres!=null)
            changedWheres.add(queryJoin.getExpr("changed").getWhere());
        return queryJoin.getExpr("value");
    }

    @Message("message.core.property.get.expr")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, propChanges, changedWhere);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (isFull() && (Settings.instance.isUseQueryExpr() || Query.getMapKeys(joinImplement)!=null))
            return getQueryExpr(joinImplement, propChanges, changedWhere);
        else
            return getJoinExpr(joinImplement, propChanges, changedWhere);
    }

    public Expr calculateExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, PropertyChanges.EMPTY, null);
    }

    public Expr calculateClassExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, SessionDataProperty.modifier.getPropertyChanges(), null);
    }

    protected abstract Expr calculateExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere);

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
    protected abstract QuickSet<Property> calculateUsedChanges(StructChanges propChanges);

    public QuickSet<Property> getUsedChanges(StructChanges propChanges) {
        if(propChanges.isEmpty()) // чтобы рекурсию разбить
            return QuickSet.EMPTY();

        QuickSet<Property> usedChanges;
        QuickSet<Property> modifyChanges = propChanges.getUsedChanges(this);
        if(propChanges.hasChanges(modifyChanges) || (propChanges.hasChanges(usedChanges  = calculateUsedChanges(propChanges)) && !modifyChanges.isEmpty()))
            return modifyChanges;
        return usedChanges;
    }

    public PropertyChanges getUsedChanges(PropertyChanges propChanges) {
        return propChanges.filter(getUsedChanges(propChanges.getStruct()));
    }

    public PropertyChanges getUsedDataChanges(PropertyChanges propChanges) {
        return propChanges.filter(getUsedDataChanges(propChanges.getStruct()));
    }

    public boolean hasChanges(Modifier modifier) {
        return hasChanges(modifier.getPropertyChanges());
    }
    public boolean hasChanges(PropertyChanges propChanges) {
        StructChanges struct = propChanges.getStruct();
        return struct.hasChanges(getUsedChanges(struct));
    }
    public static Set<Property> hasChanges(Collection<Property> properties, PropertyChanges propChanges) {
        Set<Property> result = new HashSet<Property>();
        for (Property<?> updateProperty : properties)
            if (updateProperty.hasChanges(propChanges))
                result.add(updateProperty);
        return result;
    }

    public boolean isObject() {
        return true;
    }

    public PropertyField field;
    
    public boolean aggProp;

    public String getSID() {
        return sID;
    }

    private boolean canChangeSID = true;

    public void setSID(String sID) {
        if (canChangeSID) {
            this.sID = sID;
        } else {
            throw new RuntimeException(String.format("Can't change property SID [%s] after freezing", sID));
        }
    }

    public void freezeSID() {     // todo [dale]: Отрефакторить установку SID
        canChangeSID = false;
    }

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

    public abstract ClassWhere<Field> getClassWhere(PropertyField storedField);

    public boolean cached = false;

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent

    public void markStored(TableFactory tableFactory) {
        mapTable = tableFactory.getMapTable(getMapClasses());

        PropertyField storedField = new PropertyField(getSID(), getType());
        mapTable.table.addField(storedField, getClassWhere(storedField));

        // именно после так как высчитали, а то сама себя stored'ом считать будет
        field = storedField;

        assert !cached;
    }

    public String outputStored(boolean outputTable) {
        assert isStored() && field!=null;
        return (this instanceof UserProperty? ServerResourceBundle.getString("logics.property.primary"):ServerResourceBundle.getString("logics.property.calculated")) + " "+ServerResourceBundle.getString("logics.property")+" : " + caption+", "+mapTable.table.outputField(field, outputTable);
    }

    public abstract boolean isStored();

    public boolean isFalse = false;
    public boolean checkChange = true;

    public Map<T, T> getIdentityInterfaces() {
        return BaseUtils.toMap(new HashSet<T>(interfaces));
    }

    public PropertyMapImplement<?, T> modifyChangeImplement(Result<Property> aggProp, Map<T, DataObject> interfaceValues, DataSession session, Modifier modifier) throws SQLException {
        if (new ClassWhere<T>(DataObject.getMapClasses(session.getCurrentObjects(interfaceValues))).means(getClassWhere()))
            return new PropertyMapImplement<T, T>(this, getIdentityInterfaces());
        return null;
    }
    // assert пока что aggrProps со свойствами с одним входом
    public PropertyMapImplement<?, T> getChangeImplement(Result<Property> aggProp, Map<T, DataObject> interfaceValues, DataSession session, Modifier modifier) throws SQLException {
        PropertyMapImplement<?, T> changedImplement;
        if(interfaceValues==null || (changedImplement = modifyChangeImplement(aggProp, interfaceValues, session, modifier)) == null)
            return new PropertyMapImplement<T, T>(this, getIdentityInterfaces());
        return changedImplement;
    }

    public boolean checkEquals() {
        return !(this instanceof ExecuteProperty);
    }

    public Object read(DataSession session) throws SQLException {
        return read(session.sql, new HashMap(), session.modifier, session.env);
    }

    public Object read(ExecutionContext context) throws SQLException {
        return read(context.getSession(), context.getModifier());
    }

    public Object read(DataSession session, Modifier modifier) throws SQLException {
        return read(session.sql, modifier, session.env);
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env) throws SQLException {
        return read(session, new HashMap(), modifier, env);
    }

    public Object read(SQLSession session, Map<T, DataObject> keys, Modifier modifier, QueryEnvironment env) throws SQLException {
        String readValue = "readvalue";
        Query<T, Object> readQuery = new Query<T, Object>(this);

        readQuery.putKeyWhere(keys);

        readQuery.properties.put(readValue, getExpr(readQuery.mapKeys, modifier));
        return BaseUtils.singleValue(readQuery.execute(session, env)).get(readValue);
    }

    public Object read(DataSession session, Map<T, DataObject> keys) throws SQLException {
        return read(session.sql, keys, session.modifier, session.env);
    }

    public Object read(ExecutionContext context, Map<T, DataObject> keys) throws SQLException {
        return read(context.getSession(), keys, context.getModifier());
    }

    public Object read(DataSession session, Map<T, DataObject> keys, Modifier modifier) throws SQLException {
        return read(session.sql, keys, modifier, session.env);
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> keys, Modifier modifier) throws SQLException {
        return readClasses(session, keys, modifier, session.env);
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> keys, Modifier modifier, QueryEnvironment env) throws SQLException {
        return session.getObjectValue(read(session.sql, keys, modifier, env), getType());
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean useSimpleIncrement();

    @IdentityLazy
    public <P extends PropertyInterface> MaxChangeProperty<T, P> getMaxChangeProperty(Property<P> change) {
        return new MaxChangeProperty<T, P>(this, change);
    }
    @IdentityLazy
    public <P extends PropertyInterface> OnChangeProperty<T, P> getOnChangeProperty(Property<P> change) {
        return new OnChangeProperty<T, P>(this, change);
    }

    public static boolean depends(Property<?> property, Property check) { // пока только для getChangeConstrainedProperties
        if (property.equals(check))
            return true;
        for (Property depend : property.getDepends())
            if (depends(depend, check))
                return true;
        return false;
    }

    public Collection<MaxChangeProperty<?, T>> getMaxChangeProperties(Collection<Property> properties) {
        Collection<MaxChangeProperty<?, T>> result = new ArrayList<MaxChangeProperty<?, T>>();
        for (Property<?> property : properties)
            if (depends(property, this))
                result.add(property.getMaxChangeProperty(this));
        return result;
    }

    public QuickSet<Property> getUsedDataChanges(StructChanges propChanges) {
        QuickSet<Property> result = new QuickSet<Property>(calculateUsedDataChanges(propChanges));
        for (TimePropertyChange<T> timeChange : timeChanges.values())
            result.addAll(timeChange.property.getUsedDataChanges(propChanges));
        return result;
    }

    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, Modifier modifier, WhereBuilder changedWhere) {
        return getDataChanges(change, modifier.getPropertyChanges(), changedWhere);
    }
    @Message("message.core.property.data.changes")
    @PackComplex
    @ThisMessage
    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (!change.where.isFalse()) {
            // для оптимизации, если не было изменений
            WhereBuilder calculateChangedWhere = timeChanges.isEmpty() ? changedWhere : new WhereBuilder();
            MapDataChanges<T> dataChanges = calculateDataChanges(change, calculateChangedWhere, propChanges);
            // обновляем свойства времени изменения
            for (Map.Entry<Time, TimePropertyChange<T>> timeChange : timeChanges.entrySet()) {
                TimePropertyChange<T> timeProperty = timeChange.getValue();
                dataChanges = dataChanges.add(
                        timeProperty.property.getDataChanges(
                                new PropertyChange<ClassPropertyInterface>(
                                        join(timeProperty.mapInterfaces, change.mapKeys),
                                        new TimeExpr(timeChange.getKey()), calculateChangedWhere.toWhere()
                                ), propChanges, null
                        ).map(timeProperty.mapInterfaces)
                );
            }
            if (changedWhere != null && !timeChanges.isEmpty()) {
                changedWhere.add(calculateChangedWhere.toWhere());
            }
            return dataChanges;
        }
        return new MapDataChanges<T>();
    }

    private Set<Property> actionChangeProps = new HashSet<Property>();
    public Set<Property> getChangeDepends() {
        return actionChangeProps;
    }

    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        return QuickSet.EMPTY();
    }

    // для оболочки чтобы всем getDataChanges можно было бы timeChanges вставить
    protected MapDataChanges<T> calculateDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
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

    private DataChanges getDataChanges(PropertyChanges changes, boolean toNull) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return getDataChanges(new PropertyChange<T>(mapKeys, toNull ? CaseExpr.NULL : changeExpr, CompareWhere.compare(mapKeys, getChangeExprs())), changes, null).changes;
    }

    public PropertyChanges getChangeModifier(PropertyChanges changes, boolean toNull) {
        // строим Where для изменения
        return getDataChanges(changes, toNull).add(changes);
    }

    public Collection<UserProperty> getDataChanges() { // не должно быть Action'ов
        return getDataChanges(PropertyChanges.EMPTY, false).getProperties();
    }

    protected MapDataChanges<T> getJoinDataChanges(Map<T, ? extends Expr> implementExprs, Expr expr, Where where, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder changedImplementWhere = cascadeWhere(changedWhere);
        MapDataChanges<T> result = getDataChanges(new PropertyChange<T>(mapKeys,
                GroupExpr.create(implementExprs, expr, where, GroupType.ANY, mapKeys),
                GroupExpr.create(implementExprs, where, mapKeys).getWhere()),
                propChanges, changedImplementWhere);
        if (changedWhere != null)
            changedWhere.add(new Query<T, Object>(mapKeys, changedImplementWhere.toWhere()).join(implementExprs).getWhere());// нужно перемаппить назад
        return result;
    }

    public void setJoinNotNull(Map<T, KeyExpr> implementKeys, Where where, DataSession session) throws SQLException {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        setNotNull(mapKeys, GroupExpr.create(implementKeys, where, mapKeys).getWhere(), session);
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

    public List<ClientAction> execute(ExecutionContext context, Object value) throws SQLException {
        return execute(context.getSession(), value, context.getModifier());
    }

    public List<ClientAction> execute(DataSession session, Object value, Modifier modifier) throws SQLException {
        return execute(new HashMap(), session, value, modifier);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, ExecutionContext context, Object value) throws SQLException {
        return execute(keys, context.getSession(), value, context.getModifier());
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier modifier) throws SQLException {
        return getImplement().execute(keys, session, value, modifier);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier modifier, RemoteForm executeForm, Map<T, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        return getImplement().execute(keys, session, value, modifier, executeForm, mapObjects);
    }

    // по умолчанию заполняет свойства
    // assert что entity этого свойства
    public void proceedDefaultDraw(PropertyDrawEntity<T> entity, FormEntity<?> form) {
        if (loggable && logFormProperty != null) {
            form.addPropertyDraw(logFormProperty, BaseUtils.orderMap(entity.propertyObject.mapping, interfaces).values().toArray(new PropertyObjectInterfaceEntity[0]));
            form.setForceViewType(logFormProperty, ClassViewType.PANEL);
        }

        if (shouldBeLast != null)
            entity.shouldBeLast = shouldBeLast;

        if (forceViewType != null)
            entity.forceViewType = forceViewType;

        //перемещаем свойство в контекстном меню в тот же groupObject, что и свойство, к которому оно привязано
        if (panelLocation != null && panelLocation.isShortcutLocation() && ((ShortcutPanelLocation) panelLocation).getOnlyProperty() != null) {
            Property onlyProperty = ((ShortcutPanelLocation) panelLocation).getOnlyProperty();
            for (PropertyDrawEntity drawEntity : form.getProperties(onlyProperty)) {
                if (drawEntity.toDraw != null) {
                    entity.toDraw = drawEntity.toDraw;
                }
            }
        }
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        if (iconPath != null) {
            propertyView.design.iconPath = iconPath;
            propertyView.design.setImage(image);
        }

        if (editKey != null)
            propertyView.editKey = editKey;
        if (showEditKey != null)
            propertyView.showEditKey = showEditKey;
        if (regexp != null)
            propertyView.regexp = regexp;
        if (regexpMessage != null)
            propertyView.regexpMessage = regexpMessage;
        if (echoSymbols != null)
            propertyView.echoSymbols = echoSymbols;
        if (askConfirm != null)
            propertyView.askConfirm = askConfirm;

        if (panelLocation != null) {
            PanelLocationView panelLocationView = panelLocation.convertToView();
            if (panelLocationView.isShortcutLocation()) {
                Property onlyProperty = ((ShortcutPanelLocation) panelLocation).getOnlyProperty();
                if (onlyProperty != null) {
                    for (PropertyDrawView prop : view.properties) {
                        if (prop.entity.propertyObject.property.equals(onlyProperty) &&
                        (view.getGroupObject(propertyView.entity.toDraw) == null || view.getGroupObject(propertyView.entity.toDraw).equals(view.getGroupObject(prop.entity.toDraw)))) {
                            ((ShortcutPanelLocationView) panelLocationView).setOnlyProperty(prop);
                            break;
                        }
                    }
                    if (((ShortcutPanelLocationView) panelLocationView).getOnlyProperty() == null)
                        panelLocationView = null;
                }
            }
            if (panelLocationView != null) {
                propertyView.entity.forceViewType = ClassViewType.PANEL;
                propertyView.setPanelLocation(panelLocationView);
            }
        }
        
        if(propertyView.getType() instanceof LogicalClass)
            propertyView.editOnSingleClick = Settings.instance.getEditLogicalOnSingleClick();
        if(propertyView.getType() instanceof ActionClass)
            propertyView.editOnSingleClick = Settings.instance.getEditActionClassOnSingleClick();

        if (loggable && logFormProperty != null) {
            PropertyDrawView logPropertyView = view.get(view.entity.getPropertyDraw(logFormProperty));
            GroupObjectEntity groupObject = propertyView.entity.getToDraw(view.entity);
            if (groupObject != null) {
                logPropertyView = BaseUtils.nvl(view.get(view.entity.getPropertyDraw(logFormProperty.property, groupObject)), logPropertyView);
            }
            if (logPropertyView != null) {
                logPropertyView.entity.readOnly = false; //бывает, что проставляют true для всего groupObject'а
                logPropertyView.setPanelLocation(new ShortcutPanelLocationView(propertyView));
            }
        }
    }

    public boolean hasChild(Property prop) {
        return prop.equals(this);
    }

    public List<Property> getProperties() {
        return Collections.singletonList((Property) this);
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

    @Override
    public Property getProperty(String sid) {
        return this.getSID().equals(sid) ? this : null;
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
        outStream.writeUTF(getSID());
        outStream.writeUTF(caption);
        outStream.writeBoolean(toolTip != null);
        if (toolTip != null)
            outStream.writeUTF(toolTip);
        outStream.writeUTF(getCode());
        outStream.writeBoolean(isField());

        pool.serializeCollection(outStream, interfaces);
        pool.serializeObject(outStream, getParent());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        //десериализация не нужна, т.к. вместо создания объекта, происходит поиск в BL
    }

    public final Type.Getter<T> interfaceTypeGetter = new Type.Getter<T>() {
            public Type getType(T key) {
                return getInterfaceType(key);
            }
        };

    public SinglePropertyTableUsage<T> createChangeTable() {
        return new SinglePropertyTableUsage<T>(new ArrayList<T>(interfaces), interfaceTypeGetter, getType());
    }

    @Message("message.increment.read.properties")
    @ThisMessage
    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, Modifier modifier, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        return readChangeTable(session, getIncrementChange(modifier), baseClass, env);
    }

    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        SinglePropertyTableUsage<T> changeTable = createChangeTable();
        changeTable.writeRows(session, change.getQuery(), baseClass, env);
        return changeTable;
    }

    // дебилизм конечно, но это самый простой обход DistrGroupProperty
    public boolean isOnlyNotZero = false;

    public Set<PropertyFollows<?, T>> followed = new HashSet<PropertyFollows<?, T>>();
    public Set<PropertyFollows<T, ?>> follows = new HashSet<PropertyFollows<T, ?>>();

    public <L extends PropertyInterface> Property addFollows(PropertyMapImplement<L, T> implement) {
        return addFollows(implement, PropertyFollows.RESOLVE_ALL);
    }

    public <L extends PropertyInterface> Property addFollows(PropertyMapImplement<L, T> implement, int options) {
        return addFollows(implement, ServerResourceBundle.getString("logics.property.violated.consequence.from") + "(" + this + ") => (" + implement.property + ")", options);
    }

    public <L extends PropertyInterface> Property addFollows(PropertyMapImplement<L, T> implement, String caption, int options) {
        PropertyFollows<T, L> propertyFollows = new PropertyFollows<T, L>(this, implement, options);
        follows.add(propertyFollows);
        implement.property.followed.add(propertyFollows);

        Property constraint = DerivedProperty.createAndNot(this, implement).property;
        constraint.caption = caption;
        constraint.setConstraint(false);
        return constraint;
    }

    public Collection<Property> getFollows() {
        Collection<Property> result = new ArrayList<Property>();
        for(PropertyFollows<T, ?> follow : follows)
            result.add(follow.getFollow());
        return result;
    }

    public boolean isFollow() {
        return !(followed.isEmpty() && follows.isEmpty());
    }

    protected Expr getDefaultExpr(Map<T, ? extends Expr> mapExprs) {
        Type type = getType();
        if(type instanceof DataClass)
            return ((DataClass) type).getDefaultExpr();
        else
            return null;
    }

    public void setNotNull(Map<T, DataObject> values, DataSession session) throws SQLException {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        setNotNull(mapKeys, EqualsWhere.compareValues(mapKeys, values), session);
    }

    public void setNotNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        proceedNotNull(mapKeys, where.and(getExpr(mapKeys, session.modifier).getWhere().not()), session);
    }

    public void setNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        proceedNull(mapKeys, where.and(getExpr(mapKeys, session.modifier).getWhere()), session);
    }

    // assert что where содержит getWhere().not
    protected void proceedNotNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        Expr defaultExpr = getDefaultExpr(mapKeys);
        if(defaultExpr!=null)
            session.execute(this, new PropertyChange<T>(mapKeys, defaultExpr, where), session.modifier, null, null);
    }

    // assert что where содержит getWhere()
    protected void proceedNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        session.execute(this, new PropertyChange<T>(mapKeys, CaseExpr.NULL, where), session.modifier, null, null);
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }
    
    public boolean isDerived() {
        return this instanceof UserProperty && ((UserProperty)this).derivedChange != null;
    }

    public boolean isExecuteDerived() {
        return this instanceof ExecuteProperty && ((ExecuteProperty)this).derivedChange != null;
    }

    public Set<Property> getChangeProps() {
        return new HashSet<Property>();
    }

    private boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
        for(Property property : getChangeProps())
            property.actionChangeProps.add(this);
    }

    public QuickSet<Property> getUsedDerivedChange(StructChanges propChanges) {
        return QuickSet.EMPTY();
    }

    public PropertyChange<T> getDerivedChange(PropertyChanges propChanges) {
        return null;
    }
    
    @IdentityLazy
    public PropertyChange<T> getNoChange() {
        return new PropertyChange<T>(getMapKeys(), CaseExpr.NULL);
    }
    
    public void prereadCaches() {
        getClassWhere();
        if(isFull())
            getQuery(PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, new HashMap<T, Expr>()).pack();
    }

    private Collection<Pair<Property<?>, LinkType>> links; 
    @ManualLazy
    public Collection<Pair<Property<?>, LinkType>> getLinks() {
        if(links==null) {
            Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
            for(Property depend : getDepends())
                result.add(new Pair<Property<?>, LinkType>(depend, LinkType.DEPEND));
            for(Property depend : getFollows())
                result.add(new Pair<Property<?>, LinkType>(depend, LinkType.FOLLOW));
            for(Property depend : getChangeDepends())
                result.add(new Pair<Property<?>, LinkType>(depend, LinkType.CHANGE));
            links = result;
        }
        return links;
    }
}
