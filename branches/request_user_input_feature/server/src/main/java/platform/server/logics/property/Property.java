package platform.server.logics.property;

import platform.base.*;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.PropertyEditType;
import platform.interop.action.ClientAction;
import platform.interop.form.ServerResponse;
import platform.server.Message;
import platform.server.Settings;
import platform.server.ThisMessage;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.PackComplex;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.AbstractClassWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.view.panellocation.PanelLocationView;
import platform.server.form.view.panellocation.ShortcutPanelLocationView;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.panellocation.ShortcutPanelLocation;
import platform.server.logics.property.actions.edit.DefaultChangeActionProperty;
import platform.server.logics.property.change.ActionPropertyChangeListener;
import platform.server.logics.property.change.PropertyChangeListener;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.table.ImplementTable;
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

import static platform.base.BaseUtils.*;

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
    public LP logFormProperty;

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

    public boolean isFull(Collection<T> checkInterfaces) {
        ClassWhere<T> classWhere = getClassWhere();
        if(classWhere.isFalse())
            return false;
        for (AbstractClassWhere.And<T> where : classWhere.wheres) {
            for (T i : checkInterfaces)
                if(where.get(i)==null)
                    return false;
        }
        return true;
    }
    
    private boolean calculateIsFull() {
        return isFull(interfaces);
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

    protected void fillDepends(Set<Property> depends, boolean events) {
    }

    public Set<Property> getDepends(boolean events) {
        Set<Property> depends = new HashSet<Property>();
        fillDepends(depends, events);
        return depends;
    }

    public Set<Property> getDepends() {
        return getDepends(true);
    }
    
    public Map<Property, List<Property>> getRecDepends(Stack<Property> current) {
        
        current.push(this);
        Map<Property,List<Property>> result = new HashMap<Property, List<Property>>();
        for(Property<?> depend : getDepends()) {
            result.put(depend, new ArrayList<Property>(current));
            result.putAll(depend.getRecDepends(current));
        }
        current.pop();
        return result;
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
        return getExpr(joinImplement, true, PropertyChanges.EMPTY, null);
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
        return getIncrementExpr(joinImplement, resultChanged, false, modifier.getPropertyChanges(), IncrementType.SUSPICION);
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, WhereBuilder resultChanged, boolean propClasses, PropertyChanges propChanges, IncrementType incrementType) {
        WhereBuilder incrementWhere = propClasses ? null : new WhereBuilder();
        Expr newExpr = getExpr(joinImplement, propClasses, propChanges, incrementWhere);
        Expr prevExpr = getOld().getExpr(joinImplement, propClasses, propChanges, incrementWhere);

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
            case LEFTCHANGE:
                forceWhere = newExpr.getWhere().and(newExpr.compare(prevExpr, Compare.EQUALS).not());
                break;
            case SUSPICION:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere());
                break;
            default:
                throw new RuntimeException("should not be");
        }
        if(!propClasses)
            forceWhere = forceWhere.and(incrementWhere.toWhere());
        resultChanged.add(forceWhere);
        return newExpr;
    }

    public Expr aspectGetExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert joinImplement.size() == interfaces.size();

        WhereBuilder changedExprWhere = new WhereBuilder();
        Expr changedExpr = null;
        if(!propClasses) // чтобы не вызывать derived'ы
            changedExpr = propChanges.getChangeExpr(this, joinImplement, changedExprWhere);

        if (changedExpr == null && isStored()) {
            if (!hasChanges(propChanges)) // если нету изменений
                return mapTable.table.join(join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if (useSimpleIncrement())
                changedExpr = calculateExpr(joinImplement, propClasses, propChanges, changedExprWhere);
        }

        if (changedExpr != null) {
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
        } else
            return calculateExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public IQuery<T, String> getQuery(PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) {
        return getQuery(false, propChanges, queryType, interfaceValues);
    }

    @PackComplex
    @Message("message.core.property.get.expr")
    @ThisMessage
    public IQuery<T, String> getQuery(boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(propClasses, propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            Query<T, String> fullQuery = new Query<T, String>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.properties.put("value", newExpr);
            fullQuery.properties.put("changed", query.getExpr("changed").and(newExpr.getWhere().or(getExpr(fullQuery.mapKeys).getWhere())));
            return fullQuery;
        }
            
        Query<T, String> query = new Query<T,String>(BaseUtils.filterNotKeys(getMapKeys(), interfaceValues.keySet()));
        Map<T, Expr> allKeys = BaseUtils.merge(interfaceValues, query.mapKeys);
        WhereBuilder queryWheres = queryType.needChange() ? new WhereBuilder():null;
        query.properties.put("value", aspectGetExpr(allKeys, propClasses, propChanges, queryWheres));
        if(queryType.needChange())
            query.properties.put("changed", ValueExpr.get(queryWheres.toWhere()));
        return query;
    }

    public Expr getQueryExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWheres) {

        Map<T, Expr> interfaceValues = new HashMap<T, Expr>(); Map<T, Expr> interfaceExprs = new HashMap<T, Expr>();
        for(Map.Entry<T, ? extends Expr> entry : joinImplement.entrySet())
            if(entry.getValue().isValue())
                interfaceValues.put(entry.getKey(), entry.getValue());
            else
                interfaceExprs.put(entry.getKey(), entry.getValue());

        IQuery<T, String> query = getQuery(propClasses, propChanges, changedWheres!=null?PropertyQueryType.CHANGED:PropertyQueryType.NOCHANGE, interfaceValues);

        Join<String> queryJoin = query.join(interfaceExprs);
        if(changedWheres!=null)
            changedWheres.add(queryJoin.getExpr("changed").getWhere());
        return queryJoin.getExpr("value");
    }

    @Message("message.core.property.get.expr")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getExpr(joinImplement, false, propChanges, changedWhere);
    }

    // в будущем propClasses можно заменить на PropertyTables propTables
    public Expr getExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (isFull() && (Settings.instance.isUseQueryExpr() || Query.getMapKeys(joinImplement)!=null))
            return getQueryExpr(joinImplement, propClasses, propChanges, changedWhere);
        else
            return getJoinExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public Expr calculateExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, false, PropertyChanges.EMPTY, null);
    }

    public Expr calculateClassExpr(Map<T, ? extends Expr> joinImplement) { // вызывается до stored, поэтому чтобы не было проблем с кэшами, сделано так
        return calculateExpr(joinImplement, true, PropertyChanges.EMPTY, null);
    }

    protected abstract Expr calculateExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere);

    @IdentityLazy
    public ClassWhere<T> getClassWhere() {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return new Query<T, String>(mapKeys, getClassExpr(mapKeys), "value").getClassWhere(new ArrayList<String>());
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public CustomClass getDialogClass(Map<T, DataObject> mapValues, Map<T, ConcreteClass> mapClasses) {
        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for (Map.Entry<T, DataObject> keyField : mapValues.entrySet())
            mapExprs.put(keyField.getKey(), new ValueExpr(keyField.getValue().object, mapClasses.get(keyField.getKey())));
        return (CustomClass) new Query<String, String>(new HashMap<String, KeyExpr>(), getClassExpr(mapExprs), "value").
                getClassWhere(Collections.singleton("value")).getSingleWhere("value").getOr().getCommonClass();
    }

    @IdentityLazy
    public Type getInterfaceType(T propertyInterface) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return mapKeys.get(propertyInterface).getType(getClassExpr(mapKeys).getWhere());
    }

    // возвращает от чего "зависят" изменения - с callback'ов
    protected abstract QuickSet<Property> calculateUsedChanges(StructChanges propChanges, boolean cascade);

    public QuickSet<Property> getUsedChanges(StructChanges propChanges) {
        return getUsedChanges(propChanges, false);
    }
    // 2-й параметр - "сверху" есть каскадная сессия, поэтому eventChange'ы надо проверять полностью, а не только на where
    public QuickSet<Property> getUsedChanges(StructChanges propChanges, boolean cascade) {
        if(propChanges.isEmpty()) // чтобы рекурсию разбить
            return QuickSet.EMPTY();

        QuickSet<Property> usedChanges;
        QuickSet<Property> modifyChanges = propChanges.getUsedChanges(this, cascade);
        if(propChanges.hasChanges(modifyChanges) || (propChanges.hasChanges(usedChanges  = calculateUsedChanges(propChanges, cascade)) && !modifyChanges.isEmpty()))
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
        return hasChanges(propChanges, false);
    }
    public boolean hasChanges(PropertyChanges propChanges, boolean cascade) {
        StructChanges struct = propChanges.getStruct();
        return struct.hasChanges(getUsedChanges(struct, cascade));
    }
    public static Set<Property> hasChanges(Collection<Property> properties, PropertyChanges propChanges, boolean cascade) {
        Set<Property> result = new HashSet<Property>();
        for (Property<?> updateProperty : properties)
            if (updateProperty.hasChanges(propChanges, cascade))
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

    public Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>> splitFitClasses(SinglePropertyTableUsage<T> changeTable, DataSession session) throws SQLException {
        assert isStored();

        // оптимизация
        if(!Settings.instance.isEnableApplySingleStored() || DataSession.notFitKeyClasses(this, changeTable))
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(createChangeTable(), changeTable);
        if(DataSession.fitClasses(this, changeTable))
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(changeTable, createChangeTable());

        PropertyChange<T> change = SinglePropertyTableUsage.getChange(changeTable);

        Map<KeyField, Expr> mapKeys = crossJoin(mapTable.mapKeys, change.getMapExprs());
        Where classWhere = fieldClassWhere.getWhere(merge(mapKeys, Collections.singletonMap(field, change.expr)))
                            .or(mapTable.table.getClasses().getWhere(mapKeys).and(change.expr.getWhere().not())); // или если меняет на null, assert что fitKeyClasses

        SinglePropertyTableUsage<T> fit = readChangeTable(session.sql, change.and(classWhere), session.baseClass, session.env);
        SinglePropertyTableUsage<T> notFit = readChangeTable(session.sql, change.and(classWhere.not()), session.baseClass, session.env);
        assert DataSession.fitClasses(this, fit);
        assert DataSession.fitKeyClasses(this, fit);
        assert DataSession.notFitClasses(this, notFit); // из-за эвристики с not могут быть накладки
        changeTable.drop(session.sql);
        return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(fit,notFit);
    }

    public static class CommonClasses<T extends PropertyInterface> {
        public Map<T, ValueClass> interfaces;
        public ValueClass value;

        public CommonClasses(Map<T, ValueClass> interfaces, ValueClass value) {
            this.interfaces = interfaces;
            this.value = value;
        }
    }

    public Type getType() {
        return getCommonClasses().value.getType();
    }

    public Map<T, ValueClass> getMapClasses() {
        return getCommonClasses().interfaces;
    }

    @IdentityLazy
    public CommonClasses<T> getCommonClasses() {
        Map<Object, ValueClass> mapClasses = getClassValueWhere().getCommonParent(BaseUtils.<Object, T, String>merge(interfaces, Collections.singleton("value")));
        return new CommonClasses<T>(BaseUtils.filterKeys(mapClasses, interfaces), mapClasses.get("value"));
    }

    public ClassWhere<Field> getClassWhere(MapKeysTable<T> mapTable, PropertyField storedField) {
        return getClassValueWhere().remap(BaseUtils.<Object, T, String, Field>merge(mapTable.mapKeys, Collections.singletonMap("value", storedField)));
    }

    public boolean cached = false;

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent

    public ClassWhere<Field> fieldClassWhere;

    public void markStored(TableFactory tableFactory) {
        markStored(tableFactory, null);
    }

    public void markStored(TableFactory tableFactory, ImplementTable table) {
        MapKeysTable<T> mapTable = null;

        if (table != null) {
            mapTable = table.getMapKeysTable(getMapClasses());
        }
        if (mapTable == null) {
            mapTable = tableFactory.getMapTable(getMapClasses());
        }

        PropertyField field = new PropertyField(getSID(), getType());
        fieldClassWhere = getClassWhere(mapTable, field);
        mapTable.table.addField(field, fieldClassWhere);

        this.mapTable = mapTable;
        this.field = field;
    }

    public String outputStored(boolean outputTable) {
        assert isStored() && field!=null;
        return (this instanceof UserProperty? ServerResourceBundle.getString("logics.property.primary"):ServerResourceBundle.getString("logics.property.calculated")) + " "+ServerResourceBundle.getString("logics.property")+" : " + caption+", "+mapTable.table.outputField(field, outputTable);
    }

    public abstract boolean isStored();

    public boolean isFalse = false;
    public enum CheckType { CHECK_NO, CHECK_ALL, CHECK_SOME }
    public CheckType checkChange = CheckType.CHECK_NO;
    public List<Property<?>> checkProperties = null;
    
    public Map<T, T> getIdentityInterfaces() {
        return BaseUtils.toMap(new HashSet<T>(interfaces));
    }

    public PropertyMapImplement<ClassPropertyInterface, T> getEditAction(String editActionSID) {
        return getEditAction(editActionSID, null);
    }

    private Map<String, PropertyMapImplement<ClassPropertyInterface, T>> editActions = new HashMap<String, PropertyMapImplement<ClassPropertyInterface, T>>();
    public PropertyMapImplement<ClassPropertyInterface, T> getEditAction(String editActionSID, Property filterProperty) {
        PropertyMapImplement<ClassPropertyInterface, T> editAction = editActions.get(editActionSID);
        if(editAction!=null)
            return editAction;

        if(editActionSID.equals(ServerResponse.CHANGE_WYS)) {
            PropertyMapImplement<ClassPropertyInterface, T> customChangeEdit = editActions.get(ServerResponse.CHANGE);// если перегружен
            if(customChangeEdit!=null) // возвращаем customChangeEdit
                return customChangeEdit;
        }

        if(editActionSID.equals(ServerResponse.GROUP_CHANGE)) {
            PropertyMapImplement<ClassPropertyInterface, T> customChangeEdit = editActions.get(ServerResponse.CHANGE);// если перегружен
            if(customChangeEdit!=null) { // если перегружен, иначе пусть работает комбинаторная логика (в принципе можно потом по аналогии с PASTE делать)
                return null;
            }
        }

        if(editActionSID.equals(ServerResponse.PASTE)) {
            PropertyMapImplement<ClassPropertyInterface, T> customChangeWYSEdit = getEditAction(ServerResponse.CHANGE_WYS);// если перегружен
            return null;
        }

        return getDefaultEditAction(editActionSID, filterProperty);
    }

    public PropertyMapImplement<ClassPropertyInterface, T> getDefaultEditAction(String editActionSID, Property filterProperty) {
        List<T> listInterfaces = new ArrayList<T>();
        List<ValueClass> listValues = new ArrayList<ValueClass>();
        for(Map.Entry<T, ValueClass> mapClass : getMapClasses().entrySet()) {
            listInterfaces.add(mapClass.getKey());
            listValues.add(mapClass.getValue());
        }
        DefaultChangeActionProperty<T> changeActionProperty = new DefaultChangeActionProperty<T>("DE" + getSID() + "_" + editActionSID, "sys", this, listInterfaces, listValues, editActionSID, null);
        return new PropertyMapImplement<ClassPropertyInterface, T>(changeActionProperty, changeActionProperty.getMapInterfaces());
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
        Query<T, Object> readQuery = new Query<T, Object>(new ArrayList<T>());
        readQuery.properties.put(readValue, getExpr(DataObject.getMapExprs(keys), modifier));
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
    
    public Set<OldProperty> getOldDepends() {
        Set<OldProperty> result = new HashSet<OldProperty>();
        for(Property<?> property : getDepends(false)) // derived'ы в общем то не интересуют так как используется в singleApply
            result.addAll(property.getOldDepends());
        return result;
    }

    public Set<ChangedProperty> getChangedDepends() {
        Set<ChangedProperty> result = new HashSet<ChangedProperty>();
        for(Property<?> property : getDepends(false)) // derived'ы в общем то не интересуют так как используется в singleApply
            result.addAll(property.getChangedDepends());
        return result;
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

        for (PropertyChangeListener<T> listener : changeListeners) {
            result.addAll(listener.getUsedDataChanges(propChanges));
        }
        return result;
    }

    private final List<PropertyChangeListener<T>> changeListeners = new ArrayList<PropertyChangeListener<T>>();

    public void addChangeActionListener(PropertyInterface valueInterface, PropertyImplement<ClassPropertyInterface, PropertyInterface> actionListener) {
        assert actionListener.property instanceof ActionProperty;
        addChangeListener(new ActionPropertyChangeListener<T>(this, valueInterface, actionListener));
    }

    public void addChangeListener(PropertyChangeListener<T> changeListener) {
        changeListeners.add(changeListener);
    }

    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, Modifier modifier) {
        return getDataChanges(change, modifier.getPropertyChanges());
    }

    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, PropertyChanges propChanges) {
        return getDataChanges(change, propChanges, null);
    }

    @Message("message.core.property.data.changes")
    @PackComplex
    @ThisMessage
    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (!change.where.isFalse()) {
            if (changedWhere == null && !changeListeners.isEmpty()) {
                changedWhere = new WhereBuilder();
            }

            MapDataChanges<T> dataChanges = calculateDataChanges(change, changedWhere, propChanges);

            for (PropertyChangeListener<T> changeListener : changeListeners) {
                dataChanges = dataChanges.add(changeListener.getDataChanges(change, propChanges, changedWhere.toWhere()));
            }

            return dataChanges;
        }
        return new MapDataChanges<T>();
    }

    private Set<ExecuteProperty> actionChangeProps = new HashSet<ExecuteProperty>(); // только у Data и IsClassProperty

    public Set<Property> getDataChangeProps() {
        return new HashSet<Property>();
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

    public void setJoinNotNull(Map<T, ? extends Expr> implementKeys, Where where, ExecutionEnvironment env, boolean notNull) throws SQLException {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        setNotNull(mapKeys, GroupExpr.create(implementKeys, where, mapKeys).getWhere(), env, notNull);
    }

    public PropertyMapImplement<T, T> getImplement() {
        return new PropertyMapImplement<T, T>(this, getIdentityInterfaces());
    }

    public void setConstraint(CheckType type, List<Property<?>> checkProperties) {
        assert type != CheckType.CHECK_SOME || checkProperties != null;
        assert noDB();

        isFalse = true;
        this.checkChange = type;
        this.checkProperties = checkProperties;
    }

    // используется если создаваемый WhereBuilder нужен только если задан changed 
    public static WhereBuilder cascadeWhere(WhereBuilder changed) {
        return changed == null ? null : new WhereBuilder();
    }

    public List<ClientAction> execute(ExecutionContext context, Object value) throws SQLException {
        return execute(context.getEnv(), value);
    }

    public List<ClientAction> execute(ExecutionEnvironment env, Object value) throws SQLException {
        return execute(new HashMap(), env, value);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, ExecutionContext context, Object value) throws SQLException {
        return execute(keys, context.getEnv(), value);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException {
        return execute(keys, env, value, null);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, ExecutionEnvironment env, Object value, Map<T, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        return getImplement().execute(keys, env, value, mapObjects);
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

                //добавляем в контекстное меню...
                drawEntity.setContextMenuEditAction(caption, getSID(), entity.propertyObject);
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
                logPropertyView.entity.setEditType(PropertyEditType.EDITABLE); //бывает, что проставляют READONLY для всего groupObject'а
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

    public <L extends PropertyInterface> Collection<Property> addFollows(PropertyMapImplement<L, T> implement, int options, LogicsModule lm) {
        return addFollows(implement, ServerResourceBundle.getString("logics.property.violated.consequence.from") + "(" + this + ") => (" + implement.property + ")", options, lm);
    }

    public <L extends PropertyInterface> Collection<Property> addFollows(PropertyMapImplement<L, T> implement, String caption, int options, LogicsModule lm) {
//        PropertyFollows<T, L> propertyFollows = new PropertyFollows<T, L>(this, implement, options);

        Collection<Property> props = new ArrayList<Property>();
        if((options & PropertyFollows.RESOLVE_TRUE)!=0 && implement.property.hasSet(true)) { // оптимизационная проверка
            assert interfaces.size() == implement.mapping.size(); // assert что количество
            PropertyMapImplement<?, L> setAction = DerivedProperty.createSetAction(implement.property, true, true);
            setAction.mapEventAction(getChanged(IncrementType.SET).getImplement().map(BaseUtils.reverse(implement.mapping)), Event.RESOLVE);
//            PropertyMapImplement<?, L> setAction = DerivedProperty.createSetAction(implement.property, true, false);
//            setAction.mapDerivedChange(DerivedProperty.createAndNot(getChanged(IncrementType.SET), implement).map(BaseUtils.reverse(implement.mapping)));
            lm.addProp(setAction.property);
        } 
        if((options & PropertyFollows.RESOLVE_FALSE)!=0 && hasSet(false)) {
            PropertyMapImplement<?, T> setAction = DerivedProperty.createSetAction(this, false, true);
            setAction.mapEventAction(implement.mapChanged(IncrementType.DROP), Event.RESOLVE);
//            PropertyMapImplement<?, T> setAction = DerivedProperty.createSetAction(this, false, false);
//            setAction.mapDerivedChange(DerivedProperty.createAnd(this, implement.mapChanged(IncrementType.DROP)));
            lm.addProp(setAction.property);
        }

        Property constraint = DerivedProperty.createAndNot(this, implement).property;
        constraint.caption = caption;
        lm.addConstraint(constraint, false);

        return props;
    }

    public <D extends PropertyInterface> void setEventAction(PropertyMapImplement<?, T> whereImplement, int options) {
        assert this instanceof ActionProperty;
        setEvent(DerivedProperty.<T>createStatic(true, ActionClass.instance), whereImplement, options);
    }

    public <D extends PropertyInterface> void setEvent(boolean valueChanged, IncrementType incrementType, PropertyImplement<D, PropertyInterfaceImplement<T>> valueImplement, List<PropertyMapImplement<?, T>> whereImplements, Collection<PropertyMapImplement<?, T>> onChangeImplements) {
        // нужно onChange обернуть в getChange, and where, and change implement'ы
        if(!valueChanged)
            valueImplement = new PropertyImplement<D, PropertyInterfaceImplement<T>>(valueImplement.property.getOld(), valueImplement.mapping);

        List<PropertyMapImplement<?, T>> onChangeWhereImplements = new ArrayList<PropertyMapImplement<?, T>>();
        for(PropertyMapImplement<?, T> onChangeImplement : onChangeImplements)
            onChangeWhereImplements.add(onChangeImplement.mapChanged(incrementType));
        for(PropertyInterfaceImplement<T> mapping : valueImplement.mapping.values())
            if(mapping instanceof PropertyMapImplement)
                onChangeWhereImplements.add(((PropertyMapImplement<?, T>) mapping).mapChanged(IncrementType.CHANGE));

        PropertyMapImplement<?, T> where;
        if(onChangeWhereImplements.size() > 0) {
            PropertyMapImplement<?, T> onChangeWhere;
            if(onChangeWhereImplements.size()==1)
                where = BaseUtils.single(onChangeWhereImplements);
            else
                where = DerivedProperty.createUnion(interfaces, onChangeWhereImplements);
            if(whereImplements.size()>0)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements);
        } else { // по сути новая ветка, assert что whereImplements > 0
            where = whereImplements.get(0);
            if(whereImplements.size() > 1)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements.subList(1, whereImplements.size()));
        }
        setEvent(DerivedProperty.createJoin(valueImplement), where);
    }

    public <D extends PropertyInterface, W extends PropertyInterface> void setEvent(PropertyInterfaceImplement<T> valueImplement, PropertyMapImplement<W, T> whereImplement) {
        setEvent(valueImplement, whereImplement, 0);
    }

    private <D extends PropertyInterface, W extends PropertyInterface> void setEvent(PropertyInterfaceImplement<T> valueImplement, PropertyMapImplement<W, T> whereImplement, int options) {
        if(!whereImplement.property.noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET);

        Event<D,T> event = new Event<D,T>(this, valueImplement, whereImplement, options);
        // запишем в DataProperty
        for(UserProperty dataProperty : getDataChanges())
            dataProperty.event = event;
    }

    protected Expr getDefaultExpr(Map<T, ? extends Expr> mapExprs) {
        Type type = getType();
        if(type instanceof DataClass)
            return ((DataClass) type).getDefaultExpr();
        else
            return null;
    }

    public void setNotNull(Map<T, DataObject> values, ExecutionEnvironment env, boolean notNull, boolean check) throws SQLException {
        setNotNull(values, new HashMap<T, KeyExpr>(), Where.TRUE, env, notNull, check);
    }
    public void setNotNull(Map<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull) throws SQLException {
        setNotNull(new HashMap<T, DataObject>(), mapKeys, where, env, notNull);
    }
    public void setNotNull(Map<T, DataObject> mapValues, Map<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull) throws SQLException {
        setNotNull(mapValues, mapKeys, where, env, notNull, true);
    }
    public void setNotNull(Map<T, DataObject> mapValues, Map<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull, boolean check) throws SQLException {
        setNotNull(new PropertySet<T>(mapValues, mapKeys, where), env, notNull, check);
    }
    public void setNotNull(PropertySet<T> set, ExecutionEnvironment env, boolean notNull, boolean check) throws SQLException {
        if(check) {
            Where where = getExpr(set.getMapExprs(), env.getModifier()).getWhere();
            if(notNull)
                where = where.not();
            set = set.and(where);
        }
        proceedNotNull(set, env, notNull);
    }

    // assert что where содержит getWhere().not
    protected void proceedNotNull(PropertySet<T> set, ExecutionEnvironment env, boolean notNull) throws SQLException {
        if(notNull) {
            Expr defaultExpr = getDefaultExpr(set.getMapExprs());
            if(defaultExpr!=null)
                env.execute(this, new PropertyChange<T>(set, defaultExpr));
        } else
            env.execute(this, new PropertyChange<T>(set, CaseExpr.NULL));
    }

    protected boolean hasSet(boolean notNull) {
        return !getSetChangeProps(notNull, false).isEmpty();
    }

    public Set<Property> getSetChangeProps(boolean notNull, boolean add) {
        return new HashSet<Property>(getDataChanges(PropertyChanges.EMPTY, !notNull).getProperties()); // хотя и getDataChanges() сойдет
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }

    public boolean hasEvent() {
        return this instanceof UserProperty && ((UserProperty)this).event != null;
    }

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
        if(this instanceof ExecuteProperty)
            for(Property<?> property : ((ExecuteProperty)this).getChangeProps()) // вообще говоря DataProperty и IsClassProperty
                property.actionChangeProps.add((ExecuteProperty) this);
    }

    public QuickSet<Property> getUsedEventChange(StructChanges propChanges, boolean cascade) {
        return QuickSet.EMPTY();
    }

    @IdentityLazy
    public PropertyChange<T> getNoChange() {
        return new PropertyChange<T>(getMapKeys(), CaseExpr.NULL);
    }
    
    public void prereadCaches() {
        getClassWhere();
        if(isFull())
            getQuery(false, PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, new HashMap<T, Expr>()).pack();
    }

    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
        for(Property depend : getDepends())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.DEPEND));
        for(Property depend : actionChangeProps) // только у Data и IsClassProperty
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.DEPEND));
        return result;
    }

    private Collection<Pair<Property<?>, LinkType>> links; 
    @ManualLazy
    public Collection<Pair<Property<?>, LinkType>> getLinks() {
        if(links==null)
            links = calculateLinks();
        return links;
    }

    @IdentityLazy
    public ChangedProperty<T> getChanged(IncrementType type) {
        return new ChangedProperty<T>(this, type);
    }

    public boolean noOld() {
        return getOldDepends().isEmpty();
    }
    private OldProperty<T> old;
    public OldProperty<T> getOld() {
        if(old==null) {
            assert noOld();
            old = new OldProperty<T>(this);
        }
        return old;
    }

    public boolean noDB() {
        return !noOld();
    }

    public abstract ClassWhere<Object> getClassValueWhere();

    protected Expr getClassTableExpr(Map<T, ? extends Expr> joinImplement) {
        ClassTable<T> classTable = getClassTable();
        return classTable.join(join(classTable.mapFields, joinImplement)).getExpr(classTable.propValue);
    }

    @IdentityLazy
    public ClassTable<T> getClassTable() {
        return new ClassTable<T>(this);
    }

    public static class ClassTable<P extends PropertyInterface> extends Table {

        public final Map<KeyField, P> mapFields;
        public final PropertyField propValue;

        public ClassTable(Property<P> property) {
            super(property.getSID());

            CommonClasses<P> commonClasses = property.getCommonClasses();
            Map<P, ValueClass> propInterfaces = commonClasses.interfaces;
            ValueClass valueClass = commonClasses.value;

            mapFields = new HashMap<KeyField, P>();
            for(P propInterface : property.interfaces) {
                KeyField key = new KeyField(propInterface.getSID(), propInterfaces.get(propInterface).getType());
                keys.add(key); // чтобы порядок сохранить, хотя может и не критично
                mapFields.put(key, propInterface);
            }

            propValue = new PropertyField("value", valueClass.getType());
            properties.add(propValue);

            Map<KeyField, ValueClass> fieldClasses = BaseUtils.join(mapFields, propInterfaces);
            classes = new ClassWhere<KeyField>(fieldClasses, true);
            propertyClasses.put(propValue, new ClassWhere<Field>(BaseUtils.add(fieldClasses, propValue, valueClass), true));
        }

        public StatKeys<KeyField> getStatKeys() {
            return getStatKeys(this, 100);
        }

        public Map<PropertyField, Stat> getStatProps() {
            return getStatProps(this, 100);
        }
    }

    protected boolean assertPropClasses(boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return !propClasses || (changes.isEmpty() && changedWhere==null);
    }
}
