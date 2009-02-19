/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.*;
import java.io.Serializable;

// здесь многие подходы для оптимизации неструктурные, то есть можно было структурно все обновлять но это очень медленно

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.design.JasperDesign;

import javax.swing.*;


// на самом деле нужен collection но при extend'е нужна конкретная реализация
class ObjectImplement {

    ObjectImplement(int iID, Class iBaseClass, String iCaption, GroupObjectImplement groupObject) {
        this(iID, iBaseClass, iCaption);

        groupObject.addObject(this);
    }

    ObjectImplement(int iID, Class iBaseClass, String iCaption) {
        ID = iID;
        baseClass = iBaseClass;
        gridClass = baseClass;
        caption = iCaption;
    }

    ObjectImplement(int iID, Class iBaseClass) {
        this(iID, iBaseClass, "");
    }

    // выбранный объект, класс выбранного объекта
    Integer idObject = null;
    Class Class = null;

    Class baseClass;
    // выбранный класс
    Class gridClass;

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид

    static int UPDATED_OBJECT = (1);
    static int UPDATED_CLASS = (1 << 1);
    static int UPDATED_GRIDCLASS = (1 << 3);

    int updated = UPDATED_GRIDCLASS;

    GroupObjectImplement groupTo;

    String caption = "";

    public String toString() {
        return caption;
    }

    // идентификатор (в рамках формы)
    int ID = 0;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    String sID;
    public String getSID() {
        if (sID != null) return sID; else return "obj" + ID;
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource) {
        return (ClassGroup!=null && ClassGroup.contains(groupTo)?ClassSource.get(this):Type.object.getExpr(idObject));
    }
}

class GroupObjectMap<T> extends LinkedHashMap<ObjectImplement,T> {

}

class GroupObjectValue extends GroupObjectMap<Integer> {

    GroupObjectValue() {}
    GroupObjectValue(Map<ObjectImplement,Integer> iValue) {
        putAll(iValue);
    }
}

class GroupObjectImplement extends ArrayList<ObjectImplement> {

    // глобальный идентификатор чтобы писать во ViewTable
    public final int ID;
    GroupObjectImplement(int iID) {

        if (iID >= RemoteForm.GID_SHIFT)
            throw new RuntimeException("sID must be less than " + RemoteForm.GID_SHIFT);

        ID = iID;
    }

    public void addObject(ObjectImplement object) {
        add(object);
        object.groupTo = this;
    }

    Integer Order = 0;

    // классовый вид включен или нет
    Boolean gridClassView = true;
    Boolean singleViewType = false;

    // закэшированные

    // вообще все фильтры
    Set<Filter> mapFilters = new HashSet<Filter>();
    List<PropertyView> mapOrders = new ArrayList<PropertyView>();

    // с активным интерфейсом
    Set<Filter> filters = new HashSet<Filter>();
    LinkedHashMap<PropertyObjectImplement,Boolean> orders = new LinkedHashMap<PropertyObjectImplement, Boolean>();

    boolean upKeys, downKeys;
    List<GroupObjectValue> keys = null;
    // какие ключи активны
    Map<GroupObjectValue,Map<PropertyObjectImplement,Object>> keyOrders = null;

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    static int UPDATED_OBJECT = (1);
    static int UPDATED_KEYS = (1 << 2);
    static int UPDATED_GRIDCLASS = (1 << 3);
    static int UPDATED_CLASSVIEW = (1 << 4);

    int updated = UPDATED_GRIDCLASS | UPDATED_CLASSVIEW;

    int pageSize = 12;

    GroupObjectValue getObjectValue() {
        GroupObjectValue Result = new GroupObjectValue();
        for(ObjectImplement Object : this)
            Result.put(Object,Object.idObject);

        return Result;
    }

    // получает Set группы
    Set<GroupObjectImplement> getClassGroup() {

        Set<GroupObjectImplement> Result = new HashSet<GroupObjectImplement>();
        Result.add(this);
        return Result;
    }

    void fillSourceSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableFactory tableFactory, DataSession session) {

        // фильтры первыми потому как ограничивают ключи
        for(Filter filt : filters) filt.fillSelect(query, classGroup, session);

        // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
        for(ObjectImplement object : this) {

            if (object.baseClass instanceof IntegralClass) continue;

            // не было в фильтре
            // если есть remove'классы или новые объекты их надо докинуть
            JoinQuery<KeyField,PropertyField> objectQuery = tableFactory.objectTable.getClassJoin(object.gridClass);
            if(session !=null && session.changes.addClasses.contains(object.gridClass)) {
                // придется UnionQuery делать, ObjectTable'а Key и AddClass Object'а
                ChangeQuery<KeyField,PropertyField> resultQuery = new ChangeQuery<KeyField,PropertyField>(objectQuery.keys);

                resultQuery.add(objectQuery);

                // придется создавать запрос чтобы ключи перекодировать
                JoinQuery<KeyField,PropertyField> addQuery = new JoinQuery<KeyField, PropertyField>(objectQuery.keys);
                Join<KeyField,PropertyField> addJoin = new Join<KeyField,PropertyField>(tableFactory.addClassTable.getClassJoin(session, object.gridClass));
                addJoin.joins.put(tableFactory.addClassTable.object,addQuery.mapKeys.get(tableFactory.objectTable.key));
                addQuery.and(addJoin.inJoin);
                resultQuery.add(addQuery);

                objectQuery = resultQuery;
            }

            Join<KeyField,PropertyField> ObjectJoin = new Join<KeyField,PropertyField>(objectQuery);
            ObjectJoin.joins.put(tableFactory.objectTable.key, query.mapKeys.get(object));
            query.and(ObjectJoin.inJoin);

            if(session !=null && session.changes.removeClasses.contains(object.gridClass))
                tableFactory.removeClassTable.excludeJoin(query, session, object.gridClass, query.mapKeys.get(object));
        }
    }
}

class PropertyObjectImplement<P extends PropertyInterface> extends PropertyImplement<ObjectImplement,P> {

    PropertyObjectImplement(PropertyObjectImplement<P> iProperty) { super(iProperty); }
    PropertyObjectImplement(Property<P> iProperty) {super(iProperty);}

    // получает Grid в котором рисоваться
    GroupObjectImplement getApplyObject() {
        GroupObjectImplement ApplyObject=null;
        for(ObjectImplement IntObject : mapping.values())
            if(ApplyObject==null || IntObject.groupTo.Order>ApplyObject.Order) ApplyObject = IntObject.groupTo;

        return ApplyObject;
    }

    // получает класс значения
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        InterfaceClass<P> ClassImplement = new InterfaceClass<P>();
        for(P Interface : property.interfaces) {
            ObjectImplement IntObject = mapping.get(Interface);
            ClassSet ImpClass;
            if(IntObject.groupTo ==ClassGroup)
                if(IntObject.gridClass ==null)
                    throw new RuntimeException("надо еще думать");
                else
                    ImpClass = new ClassSet(IntObject.gridClass);//ClassSet.getUp(IntObject.GridClass);
            else
                if(IntObject.Class==null)
                    return new ClassSet();
                else
                    ImpClass = new ClassSet(IntObject.Class);
            ClassImplement.put(Interface,ImpClass);
        }

        return property.getValueClass(ClassImplement);
    }

    // в интерфейсе
    boolean isInInterface(GroupObjectImplement ClassGroup) {
        return !getValueClass(ClassGroup).isEmpty();
    }

    // проверяет на то что изменился верхний объект
    boolean objectUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : mapping.values())
            if(IntObject.groupTo !=ClassGroup && ((IntObject.updated & ObjectImplement.UPDATED_OBJECT)!=0)) return true;

        return false;
    }

    // изменился хоть один из классов интерфейса (могло повлиять на вхождение в интерфейс)
    boolean classUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : mapping.values())
            if(((IntObject.updated & ((IntObject.groupTo ==ClassGroup)?ObjectImplement.UPDATED_CLASS:ObjectImplement.UPDATED_CLASS)))!=0) return true;

        return false;
    }

    ChangeValue getChangeProperty(DataSession Session, ChangePropertySecurityPolicy securityPolicy) {
        Map<P,ObjectValue> Interface = new HashMap<P,ObjectValue>();
        for(Entry<P, ObjectImplement> Implement : mapping.entrySet())
            Interface.put(Implement.getKey(),new ObjectValue(Implement.getValue().idObject,Implement.getValue().Class));

        return property.getChangeProperty(Session,Interface,1,securityPolicy);
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session) {

        Map<P,SourceExpr> JoinImplement = new HashMap<P,SourceExpr>();
        for(P Interface : property.interfaces)
            JoinImplement.put(Interface, mapping.get(Interface).getSourceExpr(ClassGroup,ClassSource));

        InterfaceClass<P> JoinClasses = new InterfaceClass<P>();
        for(Entry<P, ObjectImplement> Implement : mapping.entrySet()) {
            ClassSet Classes;
            if(ClassGroup!=null && ClassGroup.contains(Implement.getValue().groupTo)) {
                Class ImplementClass = Implement.getValue().gridClass;
                Classes = ClassSet.getUp(ImplementClass);
                ClassSet AddClasses = Session.addChanges.get(ImplementClass);
                if(AddClasses!=null)
                    Classes.or(AddClasses);
            } else {
                Class ImplementClass = Session.BaseClasses.get(Implement.getValue().idObject);
                if(ImplementClass==null) ImplementClass = Implement.getValue().Class;
                // чего не должно быть
                if(ImplementClass==null)
                    Classes = new ClassSet();
                else
                    Classes = new ClassSet(ImplementClass);
            }
            JoinClasses.put(Implement.getKey(),Classes);
        }

        // если есть не все интерфейсы и есть изменения надо с Full Join'ить старое с новым
        // иначе как и было
        return Session.getSourceExpr(property,JoinImplement,new InterfaceClassSet<P>(JoinClasses));
    }
}

// представление св-ва
class PropertyView<P extends PropertyInterface> {
    PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    GroupObjectImplement toDraw;

    PropertyView(int iID,PropertyObjectImplement<P> iView,GroupObjectImplement iToDraw) {
        view = iView;
        toDraw = iToDraw;
        ID = iID;
    }

    public PropertyView(PropertyView<P> navigatorProperty) {

        ID = navigatorProperty.ID;
        view = navigatorProperty.view;
        toDraw = navigatorProperty.toDraw;
    }

    public String toString() {
        return view.toString();
    }

    // идентификатор (в рамках формы)
    int ID = 0;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    String sID;
    public String getSID() {
        if (sID != null) return sID; else return "prop" + ID;
    }
}

class AbstractFormChanges<T,V,Z> {

    Map<T,Boolean> classViews = new HashMap<T, Boolean>();
    Map<T,V> objects = new HashMap<T, V>();
    Map<T,List<V>> gridObjects = new HashMap<T, List<V>>();
    Map<Z,Map<V,Object>> gridProperties = new HashMap<Z, Map<V, Object>>();
    Map<Z,Object> panelProperties = new HashMap<Z, Object>();
    Set<Z> dropProperties = new HashSet<Z>();
}

// класс в котором лежит какие изменения произошли
// появляется по сути для отделения клиента, именно он возвращается назад клиенту
class FormChanges extends AbstractFormChanges<GroupObjectImplement,GroupObjectValue,PropertyView> {

    void Out(RemoteForm<?> bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for(GroupObjectImplement group : bv.groups) {
            List<GroupObjectValue> groupGridObjects = gridObjects.get(group);
            if(groupGridObjects!=null) {
                System.out.println(group.ID +" - Grid Changes");
                for(GroupObjectValue value : groupGridObjects)
                    System.out.println(value);
            }

            GroupObjectValue value = objects.get(group);
            if(value!=null)
                System.out.println(group.ID +" - Object Changes "+value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for(PropertyView property : gridProperties.keySet()) {
            Map<GroupObjectValue,Object> propertyValues = gridProperties.get(property);
            System.out.println(property+" ---- property");
            for(GroupObjectValue gov : propertyValues.keySet())
                System.out.println(gov+" - "+propertyValues.get(gov));
        }

        System.out.println(" ------- Panel ---------------");
        for(PropertyView property : panelProperties.keySet())
            System.out.println(property+" - "+ panelProperties.get(property));

        System.out.println(" ------- Drop ---------------");
        for(PropertyView property : dropProperties)
            System.out.println(property);
    }
}


class Filter<P extends PropertyInterface> {

    static int EQUALS = CompareWhere.EQUALS;
    static int GREATER = CompareWhere.GREATER;
    static int LESS = CompareWhere.LESS;
    static int GREATER_EQUALS = CompareWhere.GREATER_EQUALS;
    static int LESS_EQUALS = CompareWhere.LESS_EQUALS;
    static int NOT_EQUALS = CompareWhere.NOT_EQUALS;

    PropertyObjectImplement<P> property;
    ValueLink value;
    int Compare;

    Filter(PropertyObjectImplement<P> iProperty,int iCompare,ValueLink iValue) {
        property =iProperty;
        Compare = iCompare;
        value = iValue;
    }


    GroupObjectImplement getApplyObject() {
        return property.getApplyObject();
    }

    boolean dataUpdated(Collection<Property> ChangedProps) {
        return ChangedProps.contains(property.property);
    }

    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        ClassSet ValueClass = value.getValueClass(ClassGroup);
        if(ValueClass==null)
            return property.isInInterface(ClassGroup);
        else
            return property.getValueClass(ClassGroup).intersect(ValueClass);
    }

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return property.classUpdated(ClassGroup) || value.ClassUpdated(ClassGroup);
    }

    boolean objectUpdated(GroupObjectImplement ClassGroup) {
        return property.objectUpdated(ClassGroup) || value.ObjectUpdated(ClassGroup);
    }

    void fillSelect(JoinQuery<ObjectImplement, ?> Query, Set<GroupObjectImplement> ClassGroup, DataSession Session) {
        Query.and(new CompareWhere(property.getSourceExpr(ClassGroup,Query.mapKeys,Session), value.getValueExpr(ClassGroup,Query.mapKeys,Session, property.property.getType()),Compare));
    }

    public Collection<? extends Property> getProperties() {
        Collection<Property<P>> Result = Collections.singletonList(property.property);
        if(value instanceof PropertyValueLink)
            Result.add(((PropertyValueLink) value).Property.property);
        return Result;
    }
}

abstract class ValueLink {

    ClassSet getValueClass(GroupObjectImplement ClassGroup) {return null;}

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {return false;}

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {return false;}

    abstract SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType);
}


class UserValueLink extends ValueLink {

    Object value;

    UserValueLink(Object iValue) {
        value =iValue;
    }

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return DBType.getExpr(value);
    }
}

class ObjectValueLink extends ValueLink {

    ObjectValueLink(ObjectImplement iObject) {Object=iObject;}

    ObjectImplement Object;

    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        if(Object.Class==null)
            return new ClassSet();
        else
            return new ClassSet(Object.Class);
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.updated & ObjectImplement.UPDATED_CLASS)!=0);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.updated & ObjectImplement.UPDATED_OBJECT)!=0);
    }

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return Object.getSourceExpr(ClassGroup,ClassSource);
    }
}

class RegularFilter implements Serializable {

    int ID;
    transient Filter filter;
    String name = "";
    KeyStroke key;

    RegularFilter(int iID, Filter ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }

    public String toString() {
        return name;
    }
}

class RegularFilterGroup implements Serializable {

    int ID;
    RegularFilterGroup(int iID) {
        ID = iID;
    }

    List<RegularFilter> filters = new ArrayList<RegularFilter>();
    void addFilter(RegularFilter filter) {
        filters.add(filter);
    }

    RegularFilter getFilter(int filterID) {
        for (RegularFilter filter : filters)
            if (filter.ID == filterID)
                return filter;
        return null;
    }
}

class PropertyValueLink extends ValueLink {

    PropertyValueLink(PropertyObjectImplement iProperty) {Property=iProperty;}

    PropertyObjectImplement Property;

    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        return Property.getValueClass(ClassGroup);
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return Property.classUpdated(ClassGroup);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return Property.objectUpdated(ClassGroup);
    }

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return Property.getSourceExpr(ClassGroup,ClassSource,Session);
    }
}


// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

class RemoteForm<T extends BusinessLogics<T>> implements PropertyUpdateView {

    public static int GID_SHIFT = 1000;

    // используется для записи в сессии изменений в базу - требуется глобально уникальный идентификатор
    private final int GID;
    public int getGID() { return GID; }

    private int getGroupObjectGID(GroupObjectImplement group) { return GID * GID_SHIFT + group.ID; }

    private final int ID;
    int getID() { return ID; }

    T BL;

    DataSession session;

    SecurityPolicy securityPolicy;

    RemoteForm(int iID, T iBL, DataSession iSession, SecurityPolicy isecurityPolicy) throws SQLException {

        ID = iID;
        BL = iBL;
        session = iSession;
        securityPolicy = isecurityPolicy;

        structUpdated = true;

        GID = BL.tableFactory.idTable.GenerateID(session, IDTable.FORM);
    }

    List<GroupObjectImplement> groups = new ArrayList<GroupObjectImplement>();
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    List<PropertyView> properties = new ArrayList<PropertyView>();

    // карта что сейчас в интерфейсе + карта в классовый\объектный вид
    Map<PropertyView,Boolean> interfacePool = new HashMap<PropertyView, Boolean>();

    // --------------------------------------------------------------------------------------- //
    // --------------------- Фасад для работы с примитивными данными ------------------------- //
    // --------------------------------------------------------------------------------------- //

    // ----------------------------------- Инициализация ------------------------------------- //

    public byte[] getRichDesignByteArray() {
        return ByteArraySerializer.serializeClientFormView(GetRichDesign());
    }

    public byte[] getReportDesignByteArray() {
        return ByteArraySerializer.serializeReportDesign(GetReportDesign());
    }

    public byte[] getReportDataByteArray() throws SQLException {
        return ByteArraySerializer.serializeReportData(getReportData());
    }


    // ----------------------------------- Получение информации ------------------------------ //

    public int getObjectClassID(Integer objectID) {
        return getObjectImplement(objectID).Class.ID;
    }

    public byte[] getBaseClassByteArray(int objectID) {
        return ByteArraySerializer.serializeClass(getObjectImplement(objectID).baseClass);
    }

    public byte[] getChildClassesByteArray(int objectID, int classID) {
        return ByteArraySerializer.serializeListClass(getObjectImplement(objectID).baseClass.findClassID(classID).Childs);
    }

    public byte[] getPropertyEditorObjectValueByteArray(int propertyID, boolean externalID) {
        return ByteArraySerializer.serializeChangeValue(getPropertyEditorObjectValue(getPropertyView(propertyID), externalID));
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    public void ChangeGroupObject(Integer groupID, int changeType) throws SQLException {
        ChangeGroupObject(getGroupObjectImplement(groupID), changeType);
    }

    public void ChangeGroupObject(Integer groupID, byte[] value) throws SQLException {
        GroupObjectImplement groupObject = getGroupObjectImplement(groupID);
        changeGroupObject(groupObject, ByteArraySerializer.deserializeGroupObjectValue(value, groupObject));
    }

    public void ChangeObject(Integer objectID, Integer value) throws SQLException {
        changeObject(getObjectImplement(objectID), value);
    }

    public void ChangeGridClass(int objectID,int idClass) throws SQLException {
        ChangeGridClass(getObjectImplement(objectID), idClass);
    }

    public void SwitchClassView(Integer groupID) throws SQLException {
        switchClassView(getGroupObjectImplement(groupID));
    }

    // Фильтры

    public void addFilter(byte[] state) {
        addUserFilter(ByteArraySerializer.deserializeFilter(state, this));
    }

    public void setRegularFilter(int groupID, int filterID) {

        RegularFilterGroup filterGroup = getRegularFilterGroup(groupID);
        setRegularFilter(filterGroup, filterGroup.getFilter(filterID));
    }

    // Порядки

    public void ChangeOrder(int propertyID, int modiType) {
        ChangeOrder(getPropertyView(propertyID), modiType);
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    public void AddObject(int objectID, int classID) throws SQLException {
        ObjectImplement object = getObjectImplement(objectID);
        AddObject(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
    }

    public void ChangeClass(int objectID, int classID) throws SQLException {

        ObjectImplement object = getObjectImplement(objectID);
        changeClass(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
    }

    public void ChangePropertyView(Integer propertyID, byte[] object, boolean externalID) throws SQLException {
        ChangePropertyView(getPropertyView(propertyID), ByteArraySerializer.deserializeObject(object), externalID);
    }

    // ----------------------- Применение изменений ------------------------------- //
    public void runEndApply() throws SQLException {
        endApply();
    }

    public byte[] getFormChangesByteArray() throws SQLException {
        return ByteArraySerializer.serializeFormChanges(endApply());
    }

    // --------------------------------------------------------------------------------------- //
    // ----------------------------------- Управляющий интерфейс ----------------------------- //
    // --------------------------------------------------------------------------------------- //

    // ----------------------------------- Поиск объектов по sID ------------------------------ //

    GroupObjectImplement getGroupObjectImplement(int groupID) {
        for (GroupObjectImplement groupObject : groups)
            if (groupObject.ID == groupID)
                return groupObject;
        return null;
    }

    ObjectImplement getObjectImplement(int objectID) {
        for (GroupObjectImplement groupObject : groups)
            for (ObjectImplement object : groupObject)
                if (object.ID == objectID)
                    return object;
        return null;
    }

    PropertyView getPropertyView(int propertyID) {
        for (PropertyView property : properties)
            if (property.ID == propertyID)
                return property;
        return null;
    }

    private ChangeValue getPropertyEditorObjectValue(PropertyView propertyView, boolean externalID) {

        ChangeValue changeValue = propertyView.view.getChangeProperty(session, securityPolicy.property.change);
        if (!externalID) return changeValue;

        if (changeValue == null) return null;
        DataProperty propertyID = changeValue.Class.getExternalID();
        if (propertyID == null) return null;

        return new ChangeObjectValue(propertyID.Value, null);
    }

    private RegularFilterGroup getRegularFilterGroup(int groupID) {
        for (RegularFilterGroup filterGroup : regularFilterGroups)
            if (filterGroup.ID == groupID)
                return filterGroup;
        return null;
    }

    // ----------------------------------- Инициализация ------------------------------------- //

    public ClientFormView richDesign;
    // возвращает клиентские настройки формы
    private ClientFormView GetRichDesign() {
        return richDesign;
    }

    public JasperDesign reportDesign;
    // возвращает структуру печатной формы
    public JasperDesign GetReportDesign() {
        return reportDesign;
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    // поиски по свойствам\объектам
    public Map<PropertyObjectImplement,Object> userPropertySeeks = new HashMap<PropertyObjectImplement, Object>();
    public Map<ObjectImplement,Integer> userObjectSeeks = new HashMap<ObjectImplement, Integer>();

    public static int CHANGEGROUPOBJECT_FIRSTROW = 0;
    public static int CHANGEGROUPOBJECT_LASTROW = 1;

    private Map<GroupObjectImplement, Integer> pendingGroupChanges = new HashMap<GroupObjectImplement, Integer>();

    public void ChangeGroupObject(GroupObjectImplement group, int changeType) throws SQLException {
        pendingGroupChanges.put(group, changeType);
    }

    private void changeGroupObject(GroupObjectImplement group,GroupObjectValue value) throws SQLException {
        // проставим все объектам метки изменений
        for(ObjectImplement object : group)
            changeObject(object, value.get(object));
    }

    void changeObject(ObjectImplement object, Integer value) throws SQLException {

        if ((object.idObject==null && value==null) || (object.idObject!=null && object.idObject.equals(value))) return;

        object.idObject = value;

        // запишем класс объекта
        Class objectClass = null;
        if (value != null) {
            if(object.baseClass instanceof ObjectClass)
                objectClass = session.getObjectClass(value);
            else
                objectClass = object.baseClass;
        }

        if(object.Class != objectClass) {

            object.Class = objectClass;

            object.updated = object.updated | ObjectImplement.UPDATED_CLASS;
        }

        object.updated = object.updated | ObjectImplement.UPDATED_OBJECT;
        object.groupTo.updated = object.groupTo.updated | GroupObjectImplement.UPDATED_OBJECT;

        // сообщаем всем, кто следит
        // если object.Class == null, то значит объект удалили
        if (object.Class != null)
            objectChanged(object.Class, value);
    }

    private void ChangeGridClass(ObjectImplement Object,Integer idClass) throws SQLException {

        Class GridClass = BL.objectClass.findClassID(idClass);
        if(Object.gridClass == GridClass) return;

        if(GridClass==null) throw new RuntimeException();
        Object.gridClass = GridClass;

        // расставляем пометки
        Object.updated = Object.updated | ObjectImplement.UPDATED_GRIDCLASS;
        Object.groupTo.updated = Object.groupTo.updated | GroupObjectImplement.UPDATED_GRIDCLASS;

    }

    private void switchClassView(GroupObjectImplement Group) {
        changeClassView(Group, !Group.gridClassView);
    }

    private void changeClassView(GroupObjectImplement Group,Boolean Show) {

        if(Group.gridClassView == Show || Group.singleViewType) return;
        Group.gridClassView = Show;

        // расставляем пометки
        Group.updated = Group.updated | GroupObjectImplement.UPDATED_CLASSVIEW;

        // на данный момент ClassView влияет на фильтры
        structUpdated = true;
    }

    // Фильтры

    // флаги изменения фильтров\порядков чисто для ускорения
    private boolean structUpdated = true;
    // фильтры !null (св-во), св-во - св-во, св-во - объект, класс св-ва (для < > в том числе)?,

    public Set<Filter> fixedFilters = new HashSet<Filter>();
    public List<RegularFilterGroup> regularFilterGroups = new ArrayList<RegularFilterGroup>();
    private Set<Filter> userFilters = new HashSet<Filter>();

    public void clearUserFilters() {

        userFilters.clear();
        structUpdated = true;
    }

    private void addUserFilter(Filter addFilter) {

        userFilters.add(addFilter);
        structUpdated = true;
    }

    private Map<RegularFilterGroup, RegularFilter> regularFilterValues = new HashMap<RegularFilterGroup, RegularFilter>();
    private void setRegularFilter(RegularFilterGroup filterGroup, RegularFilter filter) {

        if (filter == null || filter.filter == null)
            regularFilterValues.remove(filterGroup);
        else
            regularFilterValues.put(filterGroup, filter);

        structUpdated = true;
    }

    // Порядки

    public static int ORDER_REPLACE = 1;
    public static int ORDER_ADD = 2;
    public static int ORDER_REMOVE = 3;
    public static int ORDER_DIR = 4;

    private LinkedHashMap<PropertyView,Boolean> orders = new LinkedHashMap<PropertyView, Boolean>();

    private void ChangeOrder(PropertyView propertyView, int modiType) {

        if (modiType == ORDER_REMOVE)
            orders.remove(propertyView);
        else
        if (modiType == ORDER_DIR)
            orders.put(propertyView,!orders.get(propertyView));
        else {
            if (modiType == ORDER_REPLACE) {
                for (PropertyView propView : orders.keySet())
                    if (propView.toDraw == propertyView.toDraw)
                        orders.remove(propView);
            }
            orders.put(propertyView,false);
        }

        structUpdated = true;
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    private boolean dataChanged = false;

    private void AddObject(ObjectImplement object, Class cls) throws SQLException {
        // пока тупо в базу

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return;

        Integer addID = BL.AddObject(session, cls);

        boolean foundConflict = false;

        // берем все текущие CompareFilter на оператор 0(=) делаем ChangeProperty на ValueLink сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение ValueLink, GetValueExpr
        for(Filter<?> filter : object.groupTo.filters) {
            if(filter.Compare==0) {
                JoinQuery<ObjectImplement,String> subQuery = new JoinQuery<ObjectImplement,String>(filter.property.mapping.values());
                Map<ObjectImplement,Integer> fixedObjects = new HashMap<ObjectImplement, Integer>();
                for(ObjectImplement SibObject : filter.property.mapping.values()) {
                    if(SibObject.groupTo !=object.groupTo) {
                        fixedObjects.put(SibObject,SibObject.idObject);
                    } else {
                        if(SibObject!=object) {
                            Join<KeyField,PropertyField> ObjectJoin = new Join<KeyField,PropertyField>(BL.tableFactory.objectTable.getClassJoin(SibObject.gridClass));
                            ObjectJoin.joins.put(BL.tableFactory.objectTable.key,subQuery.mapKeys.get(SibObject));
                            subQuery.and(ObjectJoin.inJoin);
                        } else
                            fixedObjects.put(SibObject,addID);
                    }
                }

                subQuery.putKeyWhere(fixedObjects);

                subQuery.properties.put("newvalue", filter.value.getValueExpr(object.groupTo.getClassGroup(),subQuery.mapKeys, session, filter.property.property.getType()));

                LinkedHashMap<Map<ObjectImplement,Integer>,Map<String,Object>> Result = subQuery.executeSelect(session);
                // изменяем св-ва
                for(Entry<Map<ObjectImplement,Integer>,Map<String,Object>> row : Result.entrySet()) {
                    Property changeProperty = filter.property.property;
                    Map<PropertyInterface,ObjectValue> keys = new HashMap<PropertyInterface, ObjectValue>();
                    for(PropertyInterface propertyInterface : (Collection<PropertyInterface>)changeProperty.interfaces) {
                        ObjectImplement changeObject = filter.property.mapping.get(propertyInterface);
                        keys.put(propertyInterface,new ObjectValue(row.getKey().get(changeObject),changeObject.gridClass));
                    }
                    changeProperty.changeProperty(keys,row.getValue().get("newvalue"), false, session, null);
                }
            } else {
                if (object.groupTo.equals(filter.getApplyObject())) foundConflict = true;
            }
        }

        for (PropertyView prop : orders.keySet()) {
            if (object.groupTo.equals(prop.toDraw)) foundConflict = true;
        }

        changeObject(object, addID);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится
        if (foundConflict) {
            changeClassView(object.groupTo, false);
        }

        dataChanged = true;
    }

    public void changeClass(ObjectImplement object,Class cls) throws SQLException {

        // проверка, что разрешено удалять объекты
        if (cls == null) {
            if (!securityPolicy.cls.edit.remove.checkPermission(object.Class)) return;
        } else {
            if (!(securityPolicy.cls.edit.remove.checkPermission(object.Class) || securityPolicy.cls.edit.change.checkPermission(object.Class))) return;
            if (!(securityPolicy.cls.edit.add.checkPermission(cls) || securityPolicy.cls.edit.change.checkPermission(cls))) return;
        }

        BL.ChangeClass(session, object.idObject,cls);

        // Если объект удалили, то сбрасываем текущий объект в null
        if (cls == null) {
            changeObject(object, null);
        }

        object.updated = object.updated | ObjectImplement.UPDATED_CLASS;

        dataChanged = true;
    }

    private void ChangePropertyView(PropertyView property, Object value, boolean externalID) throws SQLException {
        ChangeProperty(property.view, value, externalID);
    }

    private void ChangeProperty(PropertyObjectImplement property, Object value, boolean externalID) throws SQLException {

        // изменяем св-во
        property.property.changeProperty(fillPropertyInterface(property), value, externalID, session, securityPolicy.property.change);

        dataChanged = true;
    }

    // Обновление данных
    public void refreshData() {

        for(GroupObjectImplement Group : groups) {
            Group.updated |= GroupObjectImplement.UPDATED_GRIDCLASS;
        }
    }

    // Применение изменений
    public String SaveChanges() throws SQLException {
        return BL.Apply(session);
    }

    public void CancelChanges() throws SQLException {
        session.restart(true);

        dataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(Class cls, Integer objectID) {}
    protected void gainedFocus() {
        dataChanged = true;
    }

    void Close() throws SQLException {

        session.IncrementChanges.remove(this);
        for(GroupObjectImplement Group : groups) {
            ViewTable DropTable = BL.tableFactory.viewTables.get(Group.size()-1);
            DropTable.dropViewID(session, getGroupObjectGID(Group));
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    private Map<PropertyInterface,ObjectValue> fillPropertyInterface(PropertyObjectImplement<?> property) {

        Property changeProperty = property.property;
        Map<PropertyInterface,ObjectValue> keys = new HashMap<PropertyInterface, ObjectValue>();
        for(PropertyInterface propertyInterface : (Collection<PropertyInterface>)changeProperty.interfaces) {
            ObjectImplement object = property.mapping.get(propertyInterface);
            keys.put(propertyInterface,new ObjectValue(object.idObject,object.Class));
        }

        return keys;
    }

    // рекурсия для генерации порядка
    private Where generateOrderWheres(List<SourceExpr> orderSources,List<Object> orderWheres,List<Boolean> orderDirs,boolean down,int index) {

        SourceExpr orderExpr = orderSources.get(index);
        Object orderValue = orderWheres.get(index);
        if(orderValue==null) orderValue = orderExpr.getType().getEmptyValue();
        boolean last = !(index +1< orderSources.size());

        int compareIndex;
        if (orderDirs.get(index)) {
            if (down) {
                if (last)
                    compareIndex = CompareWhere.LESS_EQUALS;
                else
                    compareIndex = CompareWhere.LESS;
            } else
                compareIndex = CompareWhere.GREATER;
        } else {
            if (down) {
                if (last)
                    compareIndex = CompareWhere.GREATER_EQUALS;
                else
                    compareIndex = CompareWhere.GREATER;
            } else
                compareIndex = CompareWhere.LESS;
        }
        Where orderWhere = new CompareWhere(orderExpr,orderExpr.getType().getExpr(orderValue),compareIndex);

        if(!last) // >A OR (=A AND >B)
            return new CompareWhere(orderExpr,orderExpr.getType().getExpr(orderValue),CompareWhere.EQUALS).
                    and(generateOrderWheres(orderSources, orderWheres, orderDirs, down, index +1)).or(orderWhere);
        else
            return orderWhere;
    }

    public Collection<Property> getUpdateProperties() {

        Set<Property> result = new HashSet<Property>();
        for(PropertyView propView : properties)
            result.add(propView.view.property);
        for(Filter<?> filter : fixedFilters)
            result.addAll(filter.getProperties());
        return result;
    }

    Collection<Property> hintsNoUpdate = new HashSet<Property>();
    public Collection<Property> getNoUpdateProperties() {
        return hintsNoUpdate;
    }

    Collection<Property> hintsSave = new HashSet<Property>();
    public boolean toSave(Property Property) {
        return hintsSave.contains(Property);
    }

    public boolean hasSessionChanges() {
        return session.hasChanges();
    }

    private static int DIRECTION_DOWN = 0;
    private static int DIRECTION_UP = 1;
    private static int DIRECTION_CENTER = 2;

    private FormChanges endApply() throws SQLException {

        FormChanges result = new FormChanges();

        // если изменились данные, применяем изменения
        Collection<Property> changedProps;
        Collection<Class> changedClasses = new HashSet<Class>();
        if(dataChanged)
            changedProps = session.update(this,changedClasses);
        else
            changedProps = new ArrayList<Property>();

        // бежим по списку вниз
        if(structUpdated) {
            // построим Map'ы
            // очистим старые

            for(GroupObjectImplement group : groups) {
                group.mapFilters = new HashSet<Filter>();
                group.mapOrders = new ArrayList<PropertyView>();
            }

            // фильтры
            Set<Filter> filters = new HashSet<Filter>();
            filters.addAll(fixedFilters);
            for (RegularFilter regFilter : regularFilterValues.values()) filters.add(regFilter.filter);
            for (Filter filter : userFilters) {
                // если вид панельный, то фильтры не нужны
                if (!filter.property.getApplyObject().gridClassView) continue;
                filters.add(filter);
            }

            for(Filter Filt : filters)
                Filt.getApplyObject().mapFilters.add(Filt);

            // порядки
            for(PropertyView Order : orders.keySet())
                Order.view.getApplyObject().mapOrders.add(Order);

        }

        for(GroupObjectImplement group : groups) {

            if ((group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) != 0) {
                result.classViews.put(group, group.gridClassView);
            }
            // если изменились :
            // хоть один класс из этого GroupObjectImplement'a - (флаг Updated - 3)
            boolean updateKeys = (group.updated & GroupObjectImplement.UPDATED_GRIDCLASS)!=0;

            // фильтр\порядок (надо сначала определить что в интерфейсе (верхних объектов Group и класса этого Group) в нем затем сравнить с теми что были до) - (Filters, Orders объектов)
            // фильтры
            // если изменилась структура или кто-то изменил класс, перепроверяем
            if(structUpdated) {
                Set<Filter> NewFilter = new HashSet<Filter>();
                for(Filter Filt : group.mapFilters)
                    if(Filt.IsInInterface(group)) NewFilter.add(Filt);

                updateKeys |= !NewFilter.equals(group.filters);
                group.filters = NewFilter;
            } else
                for(Filter Filt : group.mapFilters)
                    if(Filt.ClassUpdated(group))
                        updateKeys |= (Filt.IsInInterface(group)? group.filters.add(Filt): group.filters.remove(Filt));

            // порядки
            boolean setOrderChanged = false;
            Set<PropertyObjectImplement> setOrders = new HashSet<PropertyObjectImplement>(group.orders.keySet());
            for(PropertyView order : group.mapOrders) {
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(structUpdated || order.view.classUpdated(group))
                    setOrderChanged = (order.view.isInInterface(group)?setOrders.add(order.view): group.orders.remove(order));
            }
            if(structUpdated || setOrderChanged) {
                // переформирываваем порядок, если структура или принадлежность Order'у изменилась
                LinkedHashMap<PropertyObjectImplement,Boolean> newOrder = new LinkedHashMap<PropertyObjectImplement, Boolean>();
                for(PropertyView Order : group.mapOrders)
                    if(setOrders.contains(Order.view)) newOrder.put(Order.view, orders.get(Order));

                updateKeys |= setOrderChanged || !(new ArrayList<Map.Entry<PropertyObjectImplement,Boolean>>(group.orders.entrySet())).equals(
                        new ArrayList<Map.Entry<PropertyObjectImplement,Boolean>>(newOrder.entrySet())); //Group.Orders.equals(NewOrder)
                group.orders = newOrder;
            }

            // объекты задействованные в фильтре\порядке (по Filters\Orders верхних элементов GroupImplement'ов на флаг Updated - 0)
            if(!updateKeys)
                for(Filter filt : group.filters)
                    if(filt.objectUpdated(group)) {updateKeys = true; break;}
            if(!updateKeys)
                for(PropertyObjectImplement order : group.orders.keySet())
                    if(order.objectUpdated(group)) {updateKeys = true; break;}
            // проверим на изменение данных
            if(!updateKeys)
                for(Filter filt : group.filters)
                    if(dataChanged && filt.dataUpdated(changedProps)) {updateKeys = true; break;}
            if(!updateKeys)
                for(PropertyObjectImplement order : group.orders.keySet())
                    if(dataChanged && changedProps.contains(order.property)) {updateKeys = true; break;}
            // классы удалились\добавились
            if(!updateKeys && dataChanged) {
                for(ObjectImplement object : group)
                    if(changedClasses.contains(object.gridClass)) {updateKeys = true; break;}
            }

            // по возврастанию (0), убыванию (1), центру (2) и откуда начинать
            Map<PropertyObjectImplement,Object> propertySeeks = new HashMap<PropertyObjectImplement, Object>();

            // объект на который будет делаться активным после нахождения ключей
            GroupObjectValue currentObject = group.getObjectValue();

            // объект относительно которого будет устанавливаться фильтр
            GroupObjectValue objectSeeks = group.getObjectValue();
            int direction;
            boolean hasMoreKeys = true;

            if (objectSeeks.containsValue(null)) {
                objectSeeks = new GroupObjectValue();
                direction = DIRECTION_DOWN;
            } else
                direction = DIRECTION_CENTER;

            // Различные переходы - в самое начало или конец
            Integer pendingChanges = pendingGroupChanges.get(group);
            if (pendingChanges == null) pendingChanges = -1;

            if (pendingChanges == CHANGEGROUPOBJECT_FIRSTROW) {
                objectSeeks = new GroupObjectValue();
                currentObject = null;
                updateKeys = true;
                hasMoreKeys = false;
                direction = DIRECTION_DOWN;
            }

            if (pendingChanges == CHANGEGROUPOBJECT_LASTROW) {
                objectSeeks = new GroupObjectValue();
                currentObject = null;
                updateKeys = true;
                hasMoreKeys = false;
                direction = DIRECTION_UP;
            }

            // один раз читаем не так часто делается, поэтому не будем как с фильтрами
            for(PropertyObjectImplement Property : userPropertySeeks.keySet()) {
                if(Property.getApplyObject()== group) {
                    propertySeeks.put(Property, userPropertySeeks.get(Property));
                    currentObject = null;
                    updateKeys = true;
                    direction = DIRECTION_CENTER;
                }
            }
            for(ObjectImplement Object : userObjectSeeks.keySet()) {
                if(Object.groupTo == group) {
                    objectSeeks.put(Object, userObjectSeeks.get(Object));
                    currentObject.put(Object, userObjectSeeks.get(Object));
                    updateKeys = true;
                    direction = DIRECTION_CENTER;
                }
            }

            if(!updateKeys && (group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) !=0) {
               // изменился "классовый" вид перечитываем св-ва
                objectSeeks = group.getObjectValue();
                updateKeys = true;
                direction = DIRECTION_CENTER;
            }

            if(!updateKeys && group.gridClassView && (group.updated & GroupObjectImplement.UPDATED_OBJECT)!=0) {
                // листание - объекты стали близки к краю (object не далеко от края - надо хранить список не базу же дергать) - изменился объект
                int KeyNum = group.keys.indexOf(group.getObjectValue());
                // если меньше PageSize осталось и сверху есть ключи
                if(KeyNum< group.pageSize && group.upKeys) {
                    direction = DIRECTION_UP;
                    updateKeys = true;

                    int lowestInd = group.pageSize *2-1;
                    if (lowestInd >= group.keys.size()) {
                        objectSeeks = new GroupObjectValue();
                        hasMoreKeys = false;
                    } else {
                        objectSeeks = group.keys.get(lowestInd);
                        propertySeeks = group.keyOrders.get(objectSeeks);
                    }

                } else {
                    // наоборот вниз
                    if(KeyNum>= group.keys.size()- group.pageSize && group.downKeys) {
                        direction = DIRECTION_DOWN;
                        updateKeys = true;

                        int highestInd = group.keys.size()- group.pageSize *2;
                        if (highestInd < 0) {
                            objectSeeks = new GroupObjectValue();
                            hasMoreKeys = false;
                        } else {
                            objectSeeks = group.keys.get(highestInd);
                            propertySeeks = group.keyOrders.get(objectSeeks);
                        }
                    }
                }
            }

            if(updateKeys) {
                // --- перечитываем источник (если "классовый" вид - 50, + помечаем изменения GridObjects, иначе TOP 1

                // проверим на интегральные классы в Group'e
                for(ObjectImplement Object : group)
                    if(objectSeeks.get(Object)==null && Object.baseClass instanceof IntegralClass && !group.gridClassView)
                        objectSeeks.put(Object,1);

                // докидываем Join'ами (INNER) фильтры, порядки

                // уберем все некорректности в Seekах :
                // корректно если : PropertySeeks = Orders или (Orders.sublist(PropertySeeks.size) = PropertySeeks и ObjectSeeks - пустое)
                // если Orders.sublist(PropertySeeks.size) != PropertySeeks, тогда дочитываем ObjectSeeks полностью
                // выкидываем лишние PropertySeeks, дочитываем недостающие Orders в PropertySeeks
                // также если панель то тупо прочитаем объект
                boolean notEnoughOrders = !(propertySeeks.keySet().equals(group.orders.keySet()) || ((propertySeeks.size()< group.orders.size() && (
                        new HashSet<PropertyObjectImplement>((new ArrayList<PropertyObjectImplement>(group.orders.keySet())).subList(0, propertySeeks.size())))
                        .equals(propertySeeks.keySet())) && objectSeeks.size()==0));
                boolean objectFound = true;
                if((notEnoughOrders && objectSeeks.size()< group.size()) || !group.gridClassView) {
                    // дочитываем ObjectSeeks то есть на = PropertySeeks, ObjectSeeks
                    JoinQuery<ObjectImplement,Object> selectKeys = new JoinQuery<ObjectImplement,Object>(group);
                    selectKeys.putKeyWhere(objectSeeks);
                    group.fillSourceSelect(selectKeys, group.getClassGroup(),BL.tableFactory, session);
                    for(Entry<PropertyObjectImplement,Object> property : propertySeeks.entrySet())
                        selectKeys.and(new CompareWhere(property.getKey().getSourceExpr(group.getClassGroup(),selectKeys.mapKeys, session),
                                property.getKey().property.getType().getExpr(property.getValue()),CompareWhere.EQUALS));

                    // докидываем найденные ключи
                    LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> resultKeys = selectKeys.executeSelect(session);
                    if(resultKeys.size()>0)
                        for(ObjectImplement objectKey : group)
                            objectSeeks.put(objectKey,resultKeys.keySet().iterator().next().get(objectKey));
                    else
                        objectFound = false;
                }

                if(!group.gridClassView) {

                    // если не нашли объект, то придется искать
                    if (!objectFound) {

                        JoinQuery<ObjectImplement,Object> SelectKeys = new JoinQuery<ObjectImplement,Object>(group);
                        group.fillSourceSelect(SelectKeys, group.getClassGroup(),BL.tableFactory, session);
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> ResultKeys = SelectKeys.executeSelect(session,new LinkedHashMap<Object,Boolean>(),1);
                        if(ResultKeys.size()>0)
                            for(ObjectImplement ObjectKey : group)
                                objectSeeks.put(ObjectKey,ResultKeys.keySet().iterator().next().get(ObjectKey));
                    }

                    // если панель и ObjectSeeks "полный", то просто меняем объект и ничего не читаем
                    result.objects.put(group, objectSeeks);
                    changeGroupObject(group, objectSeeks);

                } else {
                    // выкидываем Property которых нет, дочитываем недостающие Orders, по ObjectSeeks то есть не в привязке к отбору
                    if(notEnoughOrders && objectSeeks.size()== group.size() && group.orders.size() > 0) {
                        JoinQuery<ObjectImplement,PropertyObjectImplement> orderQuery = new JoinQuery<ObjectImplement,PropertyObjectImplement>(objectSeeks.keySet());
                        orderQuery.putKeyWhere(objectSeeks);

                        for(PropertyObjectImplement order : group.orders.keySet())
                            orderQuery.properties.put(order, order.getSourceExpr(group.getClassGroup(),orderQuery.mapKeys, session));

                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> resultOrders = orderQuery.executeSelect(session);
                        for(PropertyObjectImplement order : group.orders.keySet())
                            propertySeeks.put(order,resultOrders.values().iterator().next().get(order));
                    }

                    LinkedHashMap<Object,Boolean> selectOrders = new LinkedHashMap<Object, Boolean>();
                    JoinQuery<ObjectImplement,Object> selectKeys = new JoinQuery<ObjectImplement,Object>(group); // object потому как нужно еще по ключам упорядочивать, а их тогда надо в св-ва кидать
                    group.fillSourceSelect(selectKeys, group.getClassGroup(),BL.tableFactory, session);

                    // складываются источники и значения
                    List<SourceExpr> orderSources = new ArrayList<SourceExpr>();
                    List<Object> orderWheres = new ArrayList<Object>();
                    List<Boolean> orderDirs = new ArrayList<Boolean>();

                    // закинем порядки (с LEFT JOIN'ом)
                    for(Map.Entry<PropertyObjectImplement,Boolean> toOrder : group.orders.entrySet()) {
                        SourceExpr orderExpr = toOrder.getKey().getSourceExpr(group.getClassGroup(), selectKeys.mapKeys, session);
                        // надо закинуть их в запрос, а также установить фильтры на порядки чтобы
                        if(propertySeeks.containsKey(toOrder.getKey())) {
                            orderSources.add(orderExpr);
                            orderWheres.add(propertySeeks.get(toOrder.getKey()));
                            orderDirs.add(toOrder.getValue());
                        } else //здесь надо что-то волшебное написать, чтобы null не было
                            selectKeys.and(orderExpr.getWhere());
                        // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                        selectKeys.properties.put(toOrder.getKey(), orderExpr);
                        selectOrders.put(toOrder.getKey(),toOrder.getValue());
                    }

                    // докинем в ObjectSeeks недостающие группы
                    for(ObjectImplement objectKey : group)
                        if(!objectSeeks.containsKey(objectKey))
                            objectSeeks.put(objectKey,null);

                    // закинем объекты в порядок
                    for(ObjectImplement objectKey : objectSeeks.keySet()) {
                        // также закинем их в порядок и в запрос6
                        SourceExpr keyExpr = selectKeys.mapKeys.get(objectKey);
                        selectKeys.properties.put(objectKey,keyExpr); // чтобы упорядочивать
                        selectOrders.put(objectKey,false);
                        Integer seekValue = objectSeeks.get(objectKey);
                        if(seekValue!=null) {
                            orderSources.add(keyExpr);
                            orderWheres.add(seekValue);
                            orderDirs.add(false);
                        }
                    }

                    // выполняем запрос
                    // какой ряд выбранным будем считать
                    int activeRow = -1;
                    // результат
                    LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> keyResult = new LinkedHashMap<Map<ObjectImplement, Integer>, Map<Object, Object>>();

                    int readSize = group.pageSize *3/(direction ==DIRECTION_CENTER?2:1);

                    JoinQuery<ObjectImplement,Object> copySelect = null;
                    if(direction ==DIRECTION_CENTER)
                        copySelect = new JoinQuery<ObjectImplement, Object>(selectKeys);
                    // откопируем в сторону запрос чтобы еще раз потом использовать
                    // сначала Descending загоним
                    group.downKeys = false;
                    group.upKeys = false;
                    if(direction ==DIRECTION_UP || direction ==DIRECTION_CENTER) {
                        if(orderSources.size()>0) {
                            selectKeys.and(generateOrderWheres(orderSources,orderWheres,orderDirs,false,0));
                            group.downKeys = hasMoreKeys;
                        }

//                        System.out.println(group + " KEYS UP ");
//                        selectKeys.outSelect(session,JoinQuery.reverseOrder(selectOrders),readSize);
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> execResult = selectKeys.executeSelect(session,JoinQuery.reverseOrder(selectOrders), readSize);
                        ListIterator<Map<ObjectImplement,Integer>> ik = (new ArrayList<Map<ObjectImplement,Integer>>(execResult.keySet())).listIterator();
                        while(ik.hasNext()) ik.next();
                        while(ik.hasPrevious()) {
                            Map<ObjectImplement,Integer> row = ik.previous();
                            keyResult.put(row,execResult.get(row));
                        }
                        group.upKeys = (keyResult.size()== readSize);

                        // проверка чтобы не сбить объект при листании и неправильная (потому как после 2 поиска может получится что надо с 0 без Seek'а перечитывать)
//                        if(OrderSources.size()==0)
                        // сделано так, чтобы при ненайденном объекте текущий объект смещался вверх, а не вниз
                        activeRow = keyResult.size()-1;

                    }
                    if(direction ==DIRECTION_CENTER) selectKeys = copySelect;
                    // потом Ascending
                    if(direction ==DIRECTION_DOWN || direction ==DIRECTION_CENTER) {
                        if(orderSources.size()>0) {
                            selectKeys.and(generateOrderWheres(orderSources,orderWheres,orderDirs,true,0));
                            if(direction !=DIRECTION_CENTER) group.upKeys = hasMoreKeys;
                        }

//                        System.out.println(group + " KEYS DOWN ");
//                        selectKeys.outSelect(session,selectOrders,readSize);
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> executeList = selectKeys.executeSelect(session, selectOrders, readSize);
//                        if((OrderSources.size()==0 || Direction==2) && ExecuteList.size()>0) ActiveRow = KeyResult.size();
                        keyResult.putAll(executeList);
                        group.downKeys = (executeList.size()== readSize);

                        if ((direction == DIRECTION_DOWN || activeRow == -1) && keyResult.size() > 0)
                            activeRow = 0;
                    }

                    group.keys = new ArrayList<GroupObjectValue>();
                    group.keyOrders = new HashMap<GroupObjectValue, Map<PropertyObjectImplement, Object>>();

                    // параллельно будем обновлять ключи чтобы Join'ить

                    int groupGID = getGroupObjectGID(group);
                    ViewTable insertTable = BL.tableFactory.viewTables.get(group.size()-1);
                    insertTable.dropViewID(session, groupGID);

                    for(Entry<Map<ObjectImplement,Integer>,Map<Object,Object>> resultRow : keyResult.entrySet()) {
                        GroupObjectValue keyRow = new GroupObjectValue();
                        Map<PropertyObjectImplement,Object> orderRow = new HashMap<PropertyObjectImplement, Object>();

                        // закинем сразу ключи для св-в чтобы Join'ить
                        Map<KeyField,Integer> viewKeyInsert = new HashMap<KeyField, Integer>();
                        viewKeyInsert.put(insertTable.view,groupGID);
                        ListIterator<KeyField> ivk = insertTable.objects.listIterator();

                        // важен правильный порядок в KeyRow
                        for(ObjectImplement objectKey : group) {
                            Integer keyValue = resultRow.getKey().get(objectKey);
                            keyRow.put(objectKey,keyValue);
                            viewKeyInsert.put(ivk.next(), keyValue);
                        }
                        session.insertRecord(insertTable,viewKeyInsert,new HashMap<PropertyField, Object>());

                        for(PropertyObjectImplement toOrder : group.orders.keySet())
                            orderRow.put(toOrder,resultRow.getValue().get(toOrder));

                        group.keys.add(keyRow);
                        group.keyOrders.put(keyRow, orderRow);
                    }

                    result.gridObjects.put(group, group.keys);

                    group.updated = (group.updated | GroupObjectImplement.UPDATED_KEYS);

                    // если ряд никто не подставил и ключи есть пробуем старый найти
//                    if(ActiveRow<0 && Group.Keys.size()>0)
//                        ActiveRow = Group.Keys.indexOf(Group.GetObjectValue());

                    // если есть в новых ключах старый ключ, то делаем его активным
                    if (group.keys.contains(currentObject))
                        activeRow = group.keys.indexOf(currentObject);

                    if(activeRow >=0 && activeRow < group.keys.size()) {
                        // нашли ряд его выбираем
                        GroupObjectValue newValue = group.keys.get(activeRow);
//                        if (!newValue.equals(Group.GetObjectValue())) {
                            result.objects.put(group,newValue);
                            changeGroupObject(group,newValue);
//                        }
                    } else
                        changeGroupObject(group,new GroupObjectValue());
                }
            }
        }

        Collection<PropertyView> panelProps = new ArrayList<PropertyView>();
        Map<GroupObjectImplement,Collection<PropertyView>> groupProps = new HashMap<GroupObjectImplement, Collection<PropertyView>>();

//        PanelProps.

        for(PropertyView<?> drawProp : properties) {

            // 3 признака : перечитать, (возможно класс изменился, возможно объектный интерфейс изменился - чисто InterfacePool)
            boolean read = false;
            boolean checkClass = false;
            boolean checkObject = false;
            int inInterface = 0;

            if(drawProp.toDraw !=null) {
                // если рисуемся в какой-то вид и обновился источник насильно перечитываем все св-ва
                read = ((drawProp.toDraw.updated & (GroupObjectImplement.UPDATED_KEYS | GroupObjectImplement.UPDATED_CLASSVIEW))!=0);
                Boolean prevPool = interfacePool.get(drawProp);
                inInterface = (prevPool==null?0:(prevPool?2:1));
            }

            for(ObjectImplement object : drawProp.view.mapping.values())  {
                if(object.groupTo != drawProp.toDraw) {
                    // "верхние" объекты интересует только изменение объектов\классов
                    if((object.updated & ObjectImplement.UPDATED_OBJECT)!=0) {
                        // изменился верхний объект, перечитываем
                        read = true;
                        if((object.updated & ObjectImplement.UPDATED_CLASS)!=0) {
                            // изменился класс объекта перепроверяем все
                            if(drawProp.toDraw !=null) checkClass = true;
                            checkObject = true;
                        }
                    }
                } else {
                    // изменился объект и св-во не было классовым
                    if((object.updated & ObjectImplement.UPDATED_OBJECT)!=0 && (inInterface !=2 || !drawProp.toDraw.gridClassView)) {
                        read = true;
                        // изменися класс объекта
                        if((object.updated & ObjectImplement.UPDATED_CLASS)!=0) checkObject = true;
                    }
                    // изменение общего класса
                    if((object.updated & ObjectImplement.UPDATED_GRIDCLASS)!=0) checkClass = true;

                }
            }

            // обновим InterfacePool, было в InInterface
            if(checkClass || checkObject) {
                int newInInterface=0;
                if(checkClass)
                    newInInterface = (drawProp.view.isInInterface(drawProp.toDraw)?2:0);
                if((checkObject && !(checkClass && newInInterface==2)) || (checkClass && newInInterface==0 )) // && InInterface==2))
                    newInInterface = (drawProp.view.isInInterface(null)?1:0);

                if(inInterface !=newInInterface) {
                    inInterface = newInInterface;

                    if(inInterface ==0) {
                        interfacePool.remove(drawProp);
                        // !!! СЮДА НАДО ВКИНУТЬ УДАЛЕНИЕ ИЗ ИНТЕРФЕЙСА
                        result.dropProperties.add(drawProp);
                    }
                    else
                        interfacePool.put(drawProp, inInterface ==2);
                }
            }

            if(!read && (dataChanged && changedProps.contains(drawProp.view.property)))
                read = true;

            if (!read && dataChanged) {
                for (ObjectImplement object : drawProp.view.mapping.values()) {
                    if (changedClasses.contains(object.baseClass)) {
                        read = true;
                        break;
                    }
                }
            }

            if(inInterface >0 && read) {
                if(inInterface ==2 && drawProp.toDraw.gridClassView) {
                    Collection<PropertyView> propList = groupProps.get(drawProp.toDraw);
                    if(propList==null) {
                        propList = new ArrayList<PropertyView>();
                        groupProps.put(drawProp.toDraw,propList);
                    }
                    propList.add(drawProp);
                } else
                    panelProps.add(drawProp);
            }
        }

        // погнали выполнять все собранные запросы и FormChanges

        // сначала PanelProps
        if(panelProps.size()>0) {
            JoinQuery<Object,PropertyView> selectProps = new JoinQuery<Object,PropertyView>(new ArrayList<Object>());
            for(PropertyView drawProp : panelProps)
                selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(null,null, session));

            Map<PropertyView,Object> resultProps = selectProps.executeSelect(session).values().iterator().next();
            for(PropertyView drawProp : panelProps)
                result.panelProperties.put(drawProp,resultProps.get(drawProp));
        }

        for(Entry<GroupObjectImplement, Collection<PropertyView>> mapGroup : groupProps.entrySet()) {
            GroupObjectImplement group = mapGroup.getKey();
            Collection<PropertyView> groupList = mapGroup.getValue();

            JoinQuery<ObjectImplement,PropertyView> selectProps = new JoinQuery<ObjectImplement,PropertyView>(group);

            ViewTable keyTable = BL.tableFactory.viewTables.get(group.size()-1);
            Join<KeyField,PropertyField> keyJoin = new Join<KeyField,PropertyField>(keyTable);

            ListIterator<KeyField> ikt = keyTable.objects.listIterator();
            for(ObjectImplement object : group)
                keyJoin.joins.put(ikt.next(), selectProps.mapKeys.get(object));
            keyJoin.joins.put(keyTable.view,keyTable.view.type.getExpr(getGroupObjectGID(group)));
            selectProps.and(keyJoin.inJoin);

            for(PropertyView drawProp : groupList)
                selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(group.getClassGroup(), selectProps.mapKeys, session));

//            System.out.println(group + " Props ");
//            System.out.println(selectProps.getComplexity());
//            selectProps.outSelect(session);
            LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> resultProps = selectProps.executeSelect(session);

            for(PropertyView drawProp : groupList) {
                Map<GroupObjectValue,Object> propResult = new HashMap<GroupObjectValue, Object>();
                result.gridProperties.put(drawProp,propResult);

                for(Entry<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> resultRow : resultProps.entrySet())
                    propResult.put(new GroupObjectValue(resultRow.getKey()),resultRow.getValue().get(drawProp));
            }
        }

        userPropertySeeks.clear();
        userObjectSeeks.clear();

        pendingGroupChanges.clear();

        // сбрасываем все пометки
        structUpdated = false;
        for(GroupObjectImplement group : groups) {
            for(ObjectImplement object : group)
                object.updated = 0;
            group.updated = 0;
        }
        dataChanged = false;

//        Result.Out(this);

        return result;
    }

    // возвращает какие объекты отчета фиксируются
    private Set<GroupObjectImplement> getReportObjects() {

        Set<GroupObjectImplement> reportObjects = new HashSet<GroupObjectImplement>();
        for (GroupObjectImplement group : groups) {
            if (group.gridClassView)
                reportObjects.add(group);
        }

        return reportObjects;
    }

    // считывает все данные (для отчета)
    private ReportData getReportData() throws SQLException {

        Set<GroupObjectImplement> reportObjects = getReportObjects();

        Collection<ObjectImplement> readObjects = new ArrayList<ObjectImplement>();
        for(GroupObjectImplement group : reportObjects)
            readObjects.addAll(group);

        // пока сделаем тупо получаем один большой запрос

        JoinQuery<ObjectImplement,Object> query = new JoinQuery<ObjectImplement,Object>(readObjects);
        LinkedHashMap<Object,Boolean> queryOrders = new LinkedHashMap<Object, Boolean>();

        for (GroupObjectImplement group : groups) {

            if (reportObjects.contains(group)) {

                // не фиксированные ключи
                group.fillSourceSelect(query,reportObjects,BL.tableFactory, session);

                // закинем Order'ы
                for(Map.Entry<PropertyObjectImplement,Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(),order.getKey().getSourceExpr(reportObjects, query.mapKeys, session));
                    queryOrders.put(order.getKey(),order.getValue());
                }

                for(ObjectImplement object : group) {
                    query.properties.put(object,object.getSourceExpr(reportObjects,query.mapKeys));
                    queryOrders.put(object,false);
                }
            }
        }

        ReportData result = new ReportData();

        for (GroupObjectImplement group : groups)
            for (ObjectImplement object : group)
                result.objectsID.put(object.getSID(), object.ID);

        for(PropertyView Property : properties) {
            query.properties.put(Property, Property.view.getSourceExpr(reportObjects, query.mapKeys, session));

            result.propertiesID.put(Property.getSID(), Property.ID);
            result.properties.put(Property.ID,new HashMap<Map<Integer, Integer>, Object>());
        }

        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> resultSelect = query.executeSelect(session,queryOrders,0);

        for(Entry<Map<ObjectImplement,Integer>,Map<Object,Object>> row : resultSelect.entrySet()) {
            Map<Integer,Integer> groupValue = new HashMap<Integer, Integer>();
            for(GroupObjectImplement group : groups)
                for(ObjectImplement object : group) {
                    if (readObjects.contains(object))
                        groupValue.put(object.ID,row.getKey().get(object));
                    else
                        groupValue.put(object.ID,object.idObject);
                }

            result.readOrder.add(groupValue);

            for(PropertyView property : properties)
                result.properties.get(property.ID).put(groupValue,row.getValue().get(property));
        }

//        Result.Out();

        return result;
    }

}

// поле для отрисовки отчета
class ReportDrawField implements AbstractRowLayoutElement{

    String sID;
    String caption;
    java.lang.Class valueClass;

    int minimumWidth;
    int preferredWidth;
    byte alignment;

    String pattern;

    ReportDrawField(ClientCellView cellView) {
        cellView.fillReportDrawField(this);
    }

    int getCaptionWidth() {
        return caption.length() * 10;
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    int left;
    public void setLeft(int ileft) {
        left = ileft;
    }

    int width;
    public void setWidth(int iwidth) {
        width = iwidth;
    }

    int row;
    public void setRow(int irow) {
        row = irow;
    }
}

// считанные данные (должен быть интерфейс Serialize)
class ReportData implements JRDataSource, Serializable {

    List<Map<Integer,Integer>> readOrder = new ArrayList<Map<Integer, Integer>>();
    Map<String,Integer> objectsID = new HashMap<String, Integer>();
    Map<String,Integer> propertiesID = new HashMap<String, Integer>();
    Map<Integer,Map<Map<Integer,Integer>,Object>> properties = new HashMap<Integer, Map<Map<Integer, Integer>, Object>>();

    void Out() {
        for(Integer Object : readOrder.get(0).keySet())
            System.out.print("obj"+Object+" ");
        for(Integer Property : properties.keySet())
            System.out.print("prop"+Property+" ");
        System.out.println();

        for(Map<Integer,Integer> Row : readOrder) {
            for(Integer Object : readOrder.get(0).keySet())
                System.out.print(Row.get(Object)+" ");
            for(Integer Property : properties.keySet())
                System.out.print(properties.get(Property).get(Row)+" ");
            System.out.println();
        }
    }

    int CurrentRow = -1;
    public boolean next() throws JRException {
        CurrentRow++;
        return CurrentRow< readOrder.size();
    }

    public Object getFieldValue(JRField jrField) throws JRException {

        String fieldName = jrField.getName();
        Object value;
        if(objectsID.containsKey(fieldName))
            value = readOrder.get(CurrentRow).get(objectsID.get(fieldName));
        else {
            Integer propertyID = propertiesID.get(fieldName);
            if (propertyID == null) throw new RuntimeException("Поле " + fieldName + " отсутствует в переданных данных");
            value = properties.get(propertiesID.get(fieldName)).get(readOrder.get(CurrentRow));
        }

        if (Date.class.getName().equals(jrField.getValueClassName()) && value != null) {
            value = DateConverter.intToDate((Integer) value);
        }

        if(value instanceof String)
            value = ((String) value).trim();

/*        if(Value==null) {

            try {
                return BaseUtils.getDefaultValue(java.lang.Class.forName(jrField.getValueClassName()));
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            } catch (ClassNotFoundException e) {
            }
        } */
        
        return value;
    }
}