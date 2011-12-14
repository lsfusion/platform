package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.PanelLocation;
import platform.interop.ShortcutPanelLocation;
import platform.interop.action.ClientAction;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.*;
import platform.server.data.expr.*;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.extra.CompareWhere;
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
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.*;
import platform.server.logics.linear.LP;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.derived.OnChangeProperty;
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

    public PanelLocation panelLocation;

    public Boolean shouldBeLast;

    public ClassViewType forceViewType;

    public boolean askConfirm = false;

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
        Map<T, KeyExpr> result = new HashMap<T, KeyExpr>();
        for (T propertyInterface : interfaces)
            result.put(propertyInterface, new KeyExpr(propertyInterface.toString()));
        return result;
    }

    public static Modifier<SimpleChanges> defaultModifier = new Modifier<SimpleChanges>() {
        public SimpleChanges newChanges() {
            return SimpleChanges.EMPTY;
        }

        public ExprChanges getSession() {
            return ExprChanges.EMPTY;
        }

        public SimpleChanges newFullChanges() {
            return SimpleChanges.EMPTY;
        }

        public SimpleChanges preUsed(Property property) {
            return null;
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof SimpleChanges;
        }
    };

    public Expr getExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, defaultModifier);
    }

    public Expr getClassExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, SessionDataProperty.modifier);
    }

    public <U extends Changes<U>> Expr getExpr(Map<T, ? extends Expr> joinImplement, Modifier<U> modifier) {
        return getExpr(joinImplement, modifier, null);
    }

    public <U extends Changes<U>> Expr getExpr(Map<T, ? extends Expr> joinImplement, Modifier<U> modifier, WhereBuilder changedWhere) {

        assert joinImplement.size() == interfaces.size();

        WhereBuilder changedExprWhere = new WhereBuilder();
        Expr changedExpr = modifier.changed(this, joinImplement, changedExprWhere);

        if (changedExpr == null && isStored()) {
            if (!hasChanges(modifier)) // если нету изменений
                return mapTable.table.join(join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if (useSimpleIncrement())
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
        U usedChanges = modifier.preUsed(this);
        if(usedChanges==null) // так сделано чтобы не считать лишний раз calculateUsedChanges
            return modifier.postUsed(this, calculateUsedChanges(modifier));
        else
            return usedChanges;
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

    // assert пока что aggrProps со свойствами с одним входом
    public PropertyMapImplement<?, T> getChangeImplement(Result<Property> aggProp) {
        return new PropertyMapImplement<T, T>(this, getIdentityInterfaces());
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

    public Object read(DataSession session, Modifier<? extends Changes> modifier) throws SQLException {
        return read(session.sql, modifier, session.env);
    }

    public Object read(SQLSession session, Modifier<? extends Changes> modifier, QueryEnvironment env) throws SQLException {
        return read(session, new HashMap(), modifier, env);
    }

    public Object read(SQLSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier, QueryEnvironment env) throws SQLException {
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

    public Object read(DataSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier) throws SQLException {
        return read(session.sql, keys, modifier, session.env);
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier) throws SQLException {
        return readClasses(session, keys, modifier, session.env);
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier, QueryEnvironment env) throws SQLException {
        return session.getObjectValue(read(session.sql, keys, modifier, env), getType());
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        WhereBuilder incrementWhere = new WhereBuilder();
        Expr incrementExpr = getExpr(joinImplement, modifier, incrementWhere);
        changedWhere.add(incrementWhere.toWhere().and(incrementExpr.getWhere().or(getExpr(joinImplement).getWhere()))); // если старые или новые изменились
        return incrementExpr;
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
    public Collection<OnChangeProperty<?, T>> getOnChangeProperties(Collection<Property> properties) {
        Collection<OnChangeProperty<?, T>> result = new ArrayList<OnChangeProperty<?, T>>();
        for (Property<?> property : properties)
            if (depends(property, this))
                result.add(property.getOnChangeProperty(this));
        return result;
    }

    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        U result = calculateUsedDataChanges(modifier);
        for (TimePropertyChange<T> timeChange : timeChanges.values())
            result = result.add(timeChange.property.getUsedDataChanges(modifier));
        return result;
    }

    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        if (!change.where.isFalse()) {
            // для оптимизации, если не было изменений
            WhereBuilder calculateChangedWhere = timeChanges.isEmpty() ? changedWhere : new WhereBuilder();
            MapDataChanges<T> dataChanges = calculateDataChanges(change, calculateChangedWhere, modifier);
            // обновляем свойства времени изменения
            for (Map.Entry<Time, TimePropertyChange<T>> timeChange : timeChanges.entrySet()) {
                TimePropertyChange<T> timeProperty = timeChange.getValue();
                dataChanges = dataChanges.add(
                        timeProperty.property.getDataChanges(
                                new PropertyChange<ClassPropertyInterface>(
                                        join(timeProperty.mapInterfaces, change.mapKeys),
                                        new TimeExpr(timeChange.getKey()), calculateChangedWhere.toWhere()
                                ), null, modifier
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

    public Collection<UserProperty> getDataChanges() { // не должно быть Action'ов
        return getDataChanges(defaultModifier, false).keys();
    }

    protected MapDataChanges<T> getJoinDataChanges(Map<T, ? extends Expr> implementExprs, Expr expr, Where where, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder changedImplementWhere = cascadeWhere(changedWhere);
        MapDataChanges<T> result = getDataChanges(new PropertyChange<T>(mapKeys,
                GroupExpr.create(implementExprs, expr, where, GroupType.ANY, mapKeys),
                GroupExpr.create(implementExprs, where, mapKeys).getWhere()),
                changedImplementWhere, modifier);
        if (changedWhere != null)
            changedWhere.add(new Query<T, Object>(mapKeys, changedImplementWhere.toWhere()).join(implementExprs).getWhere());// нужно перемаппить назад
        return result;
    }

    public void setJoinNotNull(Map<T, KeyExpr> implementKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        setNotNull(mapKeys, GroupExpr.create(implementKeys, where, mapKeys).getWhere(), session, BL);
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

    public List<ClientAction> execute(DataSession session, Object value, Modifier<? extends Changes> modifier) throws SQLException {
        return execute(new HashMap(), session, value, modifier);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, ExecutionContext context, Object value) throws SQLException {
        return execute(keys, context.getSession(), value, context.getModifier());
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier<? extends Changes> modifier) throws SQLException {
        return getImplement().execute(keys, session, value, modifier);
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier<? extends Changes> modifier, RemoteForm executeForm, Map<T, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        return getImplement().execute(keys, session, value, modifier, executeForm, mapObjects);
    }

    // по умолчанию заполняет свойства
    // assert что entity этого свойства
    public void proceedDefaultDraw(PropertyDrawEntity<T> entity, FormEntity form) {
        if (loggable && logFormProperty != null) {
            form.addPropertyDraw(logFormProperty, BaseUtils.orderMap(entity.propertyObject.mapping, interfaces).values().toArray(new ObjectEntity[]{}));
            form.setForceViewType(logFormProperty, ClassViewType.PANEL);
        }

        if (shouldBeLast != null)
            entity.shouldBeLast = shouldBeLast;

        if (forceViewType != null)
            entity.forceViewType = forceViewType;
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

        if (panelLocation != null)
            propertyView.setPanelLocation(panelLocation);
        
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
                logPropertyView.setPanelLocation(new ShortcutPanelLocation(getSID()));
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

    // дебилизм конечно, но это самый простой обход DistrGroupProperty
    public boolean isOnlyNotZero = false;

    public Set<PropertyFollows<?, T>> followed = new HashSet<PropertyFollows<?, T>>();
    public Set<PropertyFollows<T, ?>> follows = new HashSet<PropertyFollows<T, ?>>();

    public <L extends PropertyInterface> Property addFollows(PropertyMapImplement<L, T> implement) {
        return addFollows(implement, ServerResourceBundle.getString("logics.property.violated.consequence.from") + "(" + this + ") => (" + implement.property + ")", PropertyFollows.RESOLVE_ALL);
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

    public void setNotNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        proceedNotNull(mapKeys, where.and(getExpr(mapKeys, session.modifier).getWhere().not()), session, BL);
    }

    public void setNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        proceedNull(mapKeys, where.and(getExpr(mapKeys, session.modifier).getWhere()), session, BL);
    }

    // assert что where содержит getWhere().not
    protected void proceedNotNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        Expr defaultExpr = getDefaultExpr(mapKeys);
        if(defaultExpr!=null)
            session.execute(this, new PropertyChange<T>(mapKeys, defaultExpr, where), session.modifier, null, null);
    }

    // assert что where содержит getWhere()
    protected void proceedNull(Map<T, KeyExpr> mapKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        session.execute(this, new PropertyChange<T>(mapKeys, CaseExpr.NULL, where), session.modifier, null, null);
    }

    private AbstractIncrementProps.PropertyGroup tableGroup;
    public AbstractIncrementProps.PropertyGroup<KeyField> getTableGroup() { // через Lazy чтобы equals'ы не писать
        assert isStored();

        final MapKeysTable<T> mapTable = this.mapTable;
        if(tableGroup==null)
            tableGroup = new AbstractIncrementProps.PropertyGroup<KeyField>() {

            public List<KeyField> getKeys() {
                return mapTable.table.keys;
            }

            public Type.Getter<KeyField> typeGetter() {
                return Field.typeGetter();
            }

            public <P extends PropertyInterface> Map<P, KeyField> getPropertyMap(Property<P> property) {
                assert Property.this.equals(property);
                return property.mapTable.mapKeys;
            }
        };

        return tableGroup;
    }

    public AbstractIncrementProps.PropertyGroup<PropertyInterface> getInterfaceGroup() {
        return new AbstractIncrementProps.PropertyGroup<PropertyInterface>() {
            public List<PropertyInterface> getKeys() {
                return new ArrayList<PropertyInterface>(interfaces);
            }

            public Type.Getter<PropertyInterface> typeGetter() {
                return (Type.Getter<PropertyInterface>) interfaceTypeGetter;
            }

            public <PP extends PropertyInterface> Map<PP, PropertyInterface> getPropertyMap(Property<PP> mapProperty) {
                assert Property.this.equals(mapProperty);
                Map<PP, PropertyInterface> result = new HashMap<PP, PropertyInterface>();
                for (T propertyInterface : interfaces)
                    result.put((PP) propertyInterface, propertyInterface);
                return result;
            }
        };
    }

    public boolean isDerived() {
        return this instanceof UserProperty && ((UserProperty)this).derivedChange != null;
    }

    public boolean isExecuteDerived() {
        return this instanceof ExecuteProperty && ((ExecuteProperty)this).derivedChange != null;
    }
}
