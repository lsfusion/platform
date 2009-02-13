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
        BaseClass = iBaseClass;
        GridClass = BaseClass;
        caption = iCaption;
    }

    ObjectImplement(int iID, Class iBaseClass) {
        this(iID, iBaseClass, "");
    }

    // выбранный объект, класс выбранного объекта
    Integer idObject = null;
    Class Class = null;

    Class BaseClass;
    // выбранный класс
    Class GridClass;

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид

    static int UPDATED_OBJECT = (1 << 0);
    static int UPDATED_CLASS = (1 << 1);
    static int UPDATED_GRIDCLASS = (1 << 3);

    int Updated = UPDATED_GRIDCLASS;

    GroupObjectImplement GroupTo;

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
        return (ClassGroup!=null && ClassGroup.contains(GroupTo)?ClassSource.get(this):Type.Object.getExpr(idObject));
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
        object.GroupTo = this;
    }

    Integer Order = 0;

    // классовый вид включен или нет
    Boolean gridClassView = true;
    Boolean singleViewType = false;

    // закэшированные

    // вообще все фильтры
    Set<Filter> MapFilters = new HashSet();
    List<PropertyView> MapOrders = new ArrayList();

    // с активным интерфейсом
    Set<Filter> Filters = new HashSet();
    LinkedHashMap<PropertyObjectImplement,Boolean> orders = new LinkedHashMap();

    boolean UpKeys, DownKeys;
    List<GroupObjectValue> keys = null;
    // какие ключи активны
    Map<GroupObjectValue,Map<PropertyObjectImplement,Object>> keyOrders = null;

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    static int UPDATED_OBJECT = (1 << 0);
    static int UPDATED_KEYS = (1 << 2);
    static int UPDATED_GRIDCLASS = (1 << 3);
    static int UPDATED_CLASSVIEW = (1 << 4);

    int updated = UPDATED_GRIDCLASS | UPDATED_CLASSVIEW;

    int PageSize = 12;

    GroupObjectValue GetObjectValue() {
        GroupObjectValue Result = new GroupObjectValue();
        for(ObjectImplement Object : this)
            Result.put(Object,Object.idObject);

        return Result;
    }

    // получает Set группы
    Set<GroupObjectImplement> getClassGroup() {

        Set<GroupObjectImplement> Result = new HashSet();
        Result.add(this);
        return Result;
    }

    void fillSourceSelect(JoinQuery<ObjectImplement, ?> Query, Set<GroupObjectImplement> ClassGroup, TableFactory TableFactory, DataSession Session) {

        // фильтры первыми потому как ограничивают ключи
        for(Filter Filt : Filters) Filt.fillSelect(Query,ClassGroup,Session);

        // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
        for(ObjectImplement Object : this) {

            if (Object.BaseClass instanceof IntegralClass) continue;

            // не было в фильтре
            // если есть remove'классы или новые объекты их надо докинуть
            JoinQuery<KeyField,PropertyField> ObjectQuery = TableFactory.ObjectTable.getClassJoin(Object.GridClass);
            if(Session!=null && Session.Changes.AddClasses.contains(Object.GridClass)) {
                // придется UnionQuery делать, ObjectTable'а Key и AddClass Object'а
                UnionQuery<KeyField,PropertyField> ResultQuery = new UnionQuery<KeyField,PropertyField>(ObjectQuery.keys,2);

                ResultQuery.add(ObjectQuery,1);

                // придется создавать запрос чтобы ключи перекодировать
                JoinQuery<KeyField,PropertyField> AddQuery = new JoinQuery<KeyField, PropertyField>(ObjectQuery.keys);
                Join<KeyField,PropertyField> AddJoin = new Join<KeyField,PropertyField>(TableFactory.AddClassTable.getClassJoin(Session,Object.GridClass));
                AddJoin.joins.put(TableFactory.AddClassTable.object,AddQuery.mapKeys.get(TableFactory.ObjectTable.key));
                AddQuery.and(AddJoin.inJoin);
                ResultQuery.add(AddQuery,1);

                ObjectQuery = ResultQuery;
            }

            Join<KeyField,PropertyField> ObjectJoin = new Join<KeyField,PropertyField>(ObjectQuery);
            ObjectJoin.joins.put(TableFactory.ObjectTable.key,Query.mapKeys.get(Object));
            Query.and(ObjectJoin.inJoin);

            if(Session!=null && Session.Changes.RemoveClasses.contains(Object.GridClass))
                TableFactory.RemoveClassTable.excludeJoin(Query,Session,Object.GridClass,Query.mapKeys.get(Object));
        }
    }
}

class PropertyObjectImplement<P extends PropertyInterface> extends PropertyImplement<ObjectImplement,P> {

    PropertyObjectImplement(PropertyObjectImplement<P> iProperty) { super(iProperty); }
    PropertyObjectImplement(Property<P> iProperty) {super(iProperty);}

    // получает Grid в котором рисоваться
    GroupObjectImplement GetApplyObject() {
        GroupObjectImplement ApplyObject=null;
        for(ObjectImplement IntObject : Mapping.values())
            if(ApplyObject==null || IntObject.GroupTo.Order>ApplyObject.Order) ApplyObject = IntObject.GroupTo;

        return ApplyObject;
    }

    // получает класс значения
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        InterfaceClass<P> ClassImplement = new InterfaceClass<P>();
        for(P Interface : Property.interfaces) {
            ObjectImplement IntObject = Mapping.get(Interface);
            ClassSet ImpClass;
            if(IntObject.GroupTo==ClassGroup)
                if(IntObject.GridClass==null)
                    throw new RuntimeException("надо еще думать");
                else
                    ImpClass = new ClassSet(IntObject.GridClass);//ClassSet.getUp(IntObject.GridClass);
            else
                if(IntObject.Class==null)
                    return new ClassSet();
                else
                    ImpClass = new ClassSet(IntObject.Class);
            ClassImplement.put(Interface,ImpClass);
        }

        return Property.getValueClass(ClassImplement);
    }

    // в интерфейсе
    boolean isInInterface(GroupObjectImplement ClassGroup) {
        return !getValueClass(ClassGroup).isEmpty();
    }

    // проверяет на то что изменился верхний объект
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : Mapping.values())
            if(IntObject.GroupTo!=ClassGroup && ((IntObject.Updated & ObjectImplement.UPDATED_OBJECT)!=0)) return true;

        return false;
    }

    // изменился хоть один из классов интерфейса (могло повлиять на вхождение в интерфейс)
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : Mapping.values())
            if(((IntObject.Updated & ((IntObject.GroupTo==ClassGroup)?ObjectImplement.UPDATED_CLASS:ObjectImplement.UPDATED_CLASS)))!=0) return true;

        return false;
    }

    ChangeValue getChangeProperty(DataSession Session, ChangePropertySecurityPolicy securityPolicy) {
        Map<P,ObjectValue> Interface = new HashMap<P,ObjectValue>();
        for(Entry<P, ObjectImplement> Implement : Mapping.entrySet())
            Interface.put(Implement.getKey(),new ObjectValue(Implement.getValue().idObject,Implement.getValue().Class));

        return Property.getChangeProperty(Session,Interface,1,securityPolicy);
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session) {

        Map<P,SourceExpr> JoinImplement = new HashMap<P,SourceExpr>();
        for(P Interface : Property.interfaces)
            JoinImplement.put(Interface,Mapping.get(Interface).getSourceExpr(ClassGroup,ClassSource));

        InterfaceClass<P> JoinClasses = new InterfaceClass<P>();
        for(Entry<P, ObjectImplement> Implement : Mapping.entrySet()) {
            ClassSet Classes;
            if(ClassGroup!=null && ClassGroup.contains(Implement.getValue().GroupTo)) {
                Class ImplementClass = Implement.getValue().GridClass;
                Classes = ClassSet.getUp(ImplementClass);
                ClassSet AddClasses = Session.AddChanges.get(ImplementClass);
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
        return Session.getSourceExpr(Property,JoinImplement,new InterfaceClassSet<P>(JoinClasses));
    }
}

// представление св-ва
class PropertyView<P extends PropertyInterface> {
    PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    GroupObjectImplement ToDraw;

    PropertyView(int iID,PropertyObjectImplement<P> iView,GroupObjectImplement iToDraw) {
        view = iView;
        ToDraw = iToDraw;
        ID = iID;
    }

    public PropertyView(PropertyView<P> navigatorProperty) {

        ID = navigatorProperty.ID;
        view = navigatorProperty.view;
        ToDraw = navigatorProperty.ToDraw;
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

    Map<T,Boolean> ClassViews = new HashMap();
    Map<T,V> Objects = new HashMap();
    Map<T,List<V>> gridObjects = new HashMap();
    Map<Z,Map<V,Object>> GridProperties = new HashMap();
    Map<Z,Object> PanelProperties = new HashMap();
    Set<Z> DropProperties = new HashSet();
}

// класс в котором лежит какие изменения произошли
// появляется по сути для отделения клиента, именно он возвращается назад клиенту
class FormChanges extends AbstractFormChanges<GroupObjectImplement,GroupObjectValue,PropertyView> {

    void Out(RemoteForm bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for(GroupObjectImplement Group : (List<GroupObjectImplement>)bv.groups) {
            List<GroupObjectValue> GroupGridObjects = gridObjects.get(Group);
            if(GroupGridObjects!=null) {
                System.out.println(Group.ID +" - Grid Changes");
                for(GroupObjectValue Value : GroupGridObjects)
                    System.out.println(Value);
            }

            GroupObjectValue Value = Objects.get(Group);
            if(Value!=null)
                System.out.println(Group.ID +" - Object Changes "+Value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for(PropertyView Property : GridProperties.keySet()) {
            Map<GroupObjectValue,Object> PropertyValues = GridProperties.get(Property);
            System.out.println(Property+" ---- property");
            for(GroupObjectValue gov : PropertyValues.keySet())
                System.out.println(gov+" - "+PropertyValues.get(gov));
        }

        System.out.println(" ------- Panel ---------------");
        for(PropertyView Property : PanelProperties.keySet())
            System.out.println(Property+" - "+PanelProperties.get(Property));

        System.out.println(" ------- Drop ---------------");
        for(PropertyView Property : DropProperties)
            System.out.println(Property);
    }
}


class Filter<P extends PropertyInterface> {

    static int EQUALS = CompareWhere.EQUALS;
    static int GREATER = CompareWhere.GREATER;
    static int LESS = CompareWhere.LESS;
    static int GREATER_EQUALS = CompareWhere.GREATER_EQUALS;
    static int LESS_EQUALS = CompareWhere.LESS_EQUALS;
    static int NOT_EQUALS = CompareWhere.NOT_EQUALS;

    PropertyObjectImplement<P> Property;
    ValueLink Value;
    int Compare;

    Filter(PropertyObjectImplement<P> iProperty,int iCompare,ValueLink iValue) {
        Property=iProperty;
        Compare = iCompare;
        Value = iValue;
    }


    GroupObjectImplement GetApplyObject() {
        return Property.GetApplyObject();
    }

    boolean DataUpdated(Collection<Property> ChangedProps) {
        return ChangedProps.contains(Property.Property);
    }

    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        ClassSet ValueClass = Value.getValueClass(ClassGroup);
        if(ValueClass==null)
            return Property.isInInterface(ClassGroup);
        else
            return Property.getValueClass(ClassGroup).intersect(ValueClass);
    }

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return Property.ClassUpdated(ClassGroup) || Value.ClassUpdated(ClassGroup);
    }

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return Property.ObjectUpdated(ClassGroup) || Value.ObjectUpdated(ClassGroup);
    }

    void fillSelect(JoinQuery<ObjectImplement, ?> Query, Set<GroupObjectImplement> ClassGroup, DataSession Session) {
        Query.and(new CompareWhere(Property.getSourceExpr(ClassGroup,Query.mapKeys,Session),Value.getValueExpr(ClassGroup,Query.mapKeys,Session,Property.Property.getType()),Compare));
    }

    public Collection<? extends Property> getProperties() {
        Collection<Property<P>> Result = Collections.singletonList(Property.Property);
        if(Value instanceof PropertyValueLink)
            Result.add(((PropertyValueLink)Value).Property.Property);
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

    Object Value;

    UserValueLink(Object iValue) {Value=iValue;}

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return DBType.getExpr(Value);
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
        return ((Object.Updated & ObjectImplement.UPDATED_CLASS)!=0);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.Updated & ObjectImplement.UPDATED_OBJECT)!=0);
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

    List<RegularFilter> filters = new ArrayList();
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
        return Property.ClassUpdated(ClassGroup);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return Property.ObjectUpdated(ClassGroup);
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

        StructUpdated = true;

        GID = BL.tableFactory.idTable.GenerateID(session, IDTable.FORM);
    }

    List<GroupObjectImplement> groups = new ArrayList();
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    List<PropertyView> properties = new ArrayList();

    // карта что сейчас в интерфейсе + карта в классовый\объектный вид
    Map<PropertyView,Boolean> InterfacePool = new HashMap();

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
        return ByteArraySerializer.serializeClass(getObjectImplement(objectID).BaseClass);
    }

    public byte[] getChildClassesByteArray(int objectID, int classID) {
        return ByteArraySerializer.serializeListClass(getObjectImplement(objectID).BaseClass.findClassID(classID).Childs);
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
        ChangeGroupObject(groupObject, ByteArraySerializer.deserializeGroupObjectValue(value, groupObject));
    }

    public void ChangeObject(Integer objectID, Integer value) throws SQLException {
        ChangeObject(getObjectImplement(objectID), value);
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
        AddObject(object, (classID == -1) ? null : object.BaseClass.findClassID(classID));
    }

    public void ChangeClass(int objectID, int classID) throws SQLException {

        ObjectImplement object = getObjectImplement(objectID);
        changeClass(object, (classID == -1) ? null : object.BaseClass.findClassID(classID));
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
    public Map<PropertyObjectImplement,Object> UserPropertySeeks = new HashMap();
    public Map<ObjectImplement,Integer> UserObjectSeeks = new HashMap();

    public static int CHANGEGROUPOBJECT_FIRSTROW = 0;
    public static int CHANGEGROUPOBJECT_LASTROW = 1;

    private Map<GroupObjectImplement, Integer> pendingGroupChanges = new HashMap();

    public void ChangeGroupObject(GroupObjectImplement group, int changeType) throws SQLException {
        pendingGroupChanges.put(group, changeType);
    }

    private void ChangeGroupObject(GroupObjectImplement group,GroupObjectValue value) throws SQLException {
        // проставим все объектам метки изменений
        for(ObjectImplement object : group) {
            Integer idObject = value.get(object);
            if(object.idObject != idObject) {
                ChangeObject(object, idObject);
            }
        }
    }

    void ChangeObject(ObjectImplement object, Integer value) throws SQLException {

        if (object.idObject == value) return;

        object.idObject = value;

        // запишем класс объекта
        Class objectClass = null;
        if (value != null) {
            if(object.BaseClass instanceof ObjectClass)
                objectClass = session.getObjectClass(value);
            else
                objectClass = object.BaseClass;
        }

        if(object.Class != objectClass) {

            object.Class = objectClass;

            object.Updated = object.Updated | ObjectImplement.UPDATED_CLASS;
        }

        object.Updated = object.Updated | ObjectImplement.UPDATED_OBJECT;
        object.GroupTo.updated = object.GroupTo.updated | GroupObjectImplement.UPDATED_OBJECT;

        // сообщаем всем, кто следит
        // если object.Class == null, то значит объект удалили
        if (object.Class != null)
            objectChanged(object.Class, value);
    }

    private void ChangeGridClass(ObjectImplement Object,Integer idClass) throws SQLException {

        Class GridClass = BL.objectClass.findClassID(idClass);
        if(Object.GridClass == GridClass) return;

        if(GridClass==null) throw new RuntimeException();
        Object.GridClass = GridClass;

        // расставляем пометки
        Object.Updated = Object.Updated | ObjectImplement.UPDATED_GRIDCLASS;
        Object.GroupTo.updated = Object.GroupTo.updated | GroupObjectImplement.UPDATED_GRIDCLASS;

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
        StructUpdated = true;
    }

    // Фильтры

    // флаги изменения фильтров\порядков чисто для ускорения
    private boolean StructUpdated = true;
    // фильтры !null (св-во), св-во - св-во, св-во - объект, класс св-ва (для < > в том числе)?,

    public Set<Filter> fixedFilters = new HashSet();
    public List<RegularFilterGroup> regularFilterGroups = new ArrayList();
    private Set<Filter> userFilters = new HashSet();

    public void clearUserFilters() {

        userFilters.clear();
        StructUpdated = true;
    }

    private void addUserFilter(Filter addFilter) {

        userFilters.add(addFilter);
        StructUpdated = true;
    }

    private Map<RegularFilterGroup, RegularFilter> regularFilterValues = new HashMap();
    private void setRegularFilter(RegularFilterGroup filterGroup, RegularFilter filter) {

        if (filter == null || filter.filter == null)
            regularFilterValues.remove(filterGroup);
        else
            regularFilterValues.put(filterGroup, filter);

        StructUpdated = true;
    }

    // Порядки

    public static int ORDER_REPLACE = 1;
    public static int ORDER_ADD = 2;
    public static int ORDER_REMOVE = 3;
    public static int ORDER_DIR = 4;

    private LinkedHashMap<PropertyView,Boolean> Orders = new LinkedHashMap();

    private void ChangeOrder(PropertyView propertyView, int modiType) {

        if (modiType == ORDER_REMOVE)
            Orders.remove(propertyView);
        else
        if (modiType == ORDER_DIR)
            Orders.put(propertyView,!Orders.get(propertyView));
        else {
            if (modiType == ORDER_REPLACE) {
                for (PropertyView propView : Orders.keySet())
                    if (propView.ToDraw == propertyView.ToDraw)
                        Orders.remove(propView);
            }
            Orders.put(propertyView,false);
        }

        StructUpdated = true;
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    private boolean DataChanged = false;

    private void AddObject(ObjectImplement Object, Class cls) throws SQLException {
        // пока тупо в базу

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return;

        Integer AddID = BL.AddObject(session, cls);

        boolean foundConflict = false;

        // берем все текущие CompareFilter на оператор 0(=) делаем ChangeProperty на ValueLink сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение ValueLink, GetValueExpr
        for(Filter<?> Filter : Object.GroupTo.Filters) {
            if(Filter.Compare==0) {
                JoinQuery<ObjectImplement,String> SubQuery = new JoinQuery<ObjectImplement,String>(Filter.Property.Mapping.values());
                Map<ObjectImplement,Integer> FixedObjects = new HashMap();
                for(ObjectImplement SibObject : Filter.Property.Mapping.values()) {
                    if(SibObject.GroupTo!=Object.GroupTo) {
                        FixedObjects.put(SibObject,SibObject.idObject);
                    } else {
                        if(SibObject!=Object) {
                            Join<KeyField,PropertyField> ObjectJoin = new Join<KeyField,PropertyField>(BL.tableFactory.ObjectTable.getClassJoin(SibObject.GridClass));
                            ObjectJoin.joins.put(BL.tableFactory.ObjectTable.key,SubQuery.mapKeys.get(SibObject));
                            SubQuery.and(ObjectJoin.inJoin);
                        } else
                            FixedObjects.put(SibObject,AddID);
                    }
                }

                SubQuery.putKeyWhere(FixedObjects);

                SubQuery.properties.put("newvalue", Filter.Value.getValueExpr(Object.GroupTo.getClassGroup(),SubQuery.mapKeys, session, Filter.Property.Property.getType()));

                LinkedHashMap<Map<ObjectImplement,Integer>,Map<String,Object>> Result = SubQuery.executeSelect(session);
                // изменяем св-ва
                for(Entry<Map<ObjectImplement,Integer>,Map<String,Object>> Row : Result.entrySet()) {
                    Property ChangeProperty = Filter.Property.Property;
                    Map<PropertyInterface,ObjectValue> Keys = new HashMap();
                    for(PropertyInterface Interface : (Collection<PropertyInterface>)ChangeProperty.interfaces) {
                        ObjectImplement ChangeObject = Filter.Property.Mapping.get(Interface);
                        Keys.put(Interface,new ObjectValue(Row.getKey().get(ChangeObject),ChangeObject.GridClass));
                    }
                    ChangeProperty.changeProperty(Keys,Row.getValue().get("newvalue"), false, session, null);
                }
            } else {
                if (Object.GroupTo.equals(Filter.GetApplyObject())) foundConflict = true;
            }
        }

        for (PropertyView prop : Orders.keySet()) {
            if (Object.GroupTo.equals(prop.ToDraw)) foundConflict = true;
        }

        ChangeObject(Object, AddID);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится
        if (foundConflict) {
            changeClassView(Object.GroupTo, false);
        }

        DataChanged = true;
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
            ChangeObject(object, null);
        }

        object.Updated = object.Updated | ObjectImplement.UPDATED_CLASS;

        DataChanged = true;
    }

    private void ChangePropertyView(PropertyView property, Object value, boolean externalID) throws SQLException {
        ChangeProperty(property.view, value, externalID);
    }

    private void ChangeProperty(PropertyObjectImplement property, Object value, boolean externalID) throws SQLException {

        // изменяем св-во
        property.Property.changeProperty(fillPropertyInterface(property), value, externalID, session, securityPolicy.property.change);

        DataChanged = true;
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

        DataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(Class cls, Integer objectID) {}
    protected void gainedFocus() {
        DataChanged = true;
    }

    void Close() throws SQLException {

        session.IncrementChanges.remove(this);
        for(GroupObjectImplement Group : groups) {
            ViewTable DropTable = BL.tableFactory.viewTables.get(Group.size()-1);
            DropTable.DropViewID(session, getGroupObjectGID(Group));
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    private Map<PropertyInterface,ObjectValue> fillPropertyInterface(PropertyObjectImplement<?> property) {

        Property changeProperty = property.Property;
        Map<PropertyInterface,ObjectValue> keys = new HashMap();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)changeProperty.interfaces) {
            ObjectImplement object = property.Mapping.get(Interface);
            keys.put(Interface,new ObjectValue(object.idObject,object.Class));
        }

        return keys;
    }

    // рекурсия для генерации порядка
    private Where GenerateOrderWheres(List<SourceExpr> OrderSources,List<Object> OrderWheres,List<Boolean> OrderDirs,boolean Down,int Index) {

        SourceExpr OrderExpr = OrderSources.get(Index);
        Object OrderValue = OrderWheres.get(Index);
        boolean Last = !(Index+1<OrderSources.size());

        int CompareIndex;
        if (OrderDirs.get(Index)) {
            if (Down) {
                if (Last)
                    CompareIndex = CompareWhere.LESS_EQUALS;
                else
                    CompareIndex = CompareWhere.LESS;
            } else
                CompareIndex = CompareWhere.GREATER;
        } else {
            if (Down) {
                if (Last)
                    CompareIndex = CompareWhere.GREATER_EQUALS;
                else
                    CompareIndex = CompareWhere.GREATER;
            } else
                CompareIndex = CompareWhere.LESS;
        }
        Where OrderWhere = new CompareWhere(OrderExpr,OrderExpr.getType().getExpr(OrderValue),CompareIndex);

        if(!Last) // >A OR (=A AND >B)
            return new CompareWhere(OrderExpr,OrderExpr.getType().getExpr(OrderValue),CompareWhere.EQUALS).
                    and(GenerateOrderWheres(OrderSources,OrderWheres,OrderDirs,Down,Index+1)).or(OrderWhere);
        else
            return OrderWhere;
    }

    public Collection<Property> getUpdateProperties() {

        Set<Property> Result = new HashSet();
        for(PropertyView PropView : properties)
            Result.add(PropView.view.Property);
        for(Filter Filter : fixedFilters)
            Result.addAll(Filter.getProperties());
        return Result;
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

        FormChanges Result = new FormChanges();

        // если изменились данные, применяем изменения
        Collection<Property> ChangedProps;
        Collection<Class> ChangedClasses = new HashSet();
        if(DataChanged)
            ChangedProps = session.update(this,ChangedClasses);
        else
            ChangedProps = new ArrayList();

        // бежим по списку вниз
        if(StructUpdated) {
            // построим Map'ы
            // очистим старые

            for(GroupObjectImplement Group : groups) {
                Group.MapFilters = new HashSet();
                Group.MapOrders = new ArrayList();
            }

            // фильтры
            Set<Filter> filters = new HashSet();
            filters.addAll(fixedFilters);
            for (RegularFilter regFilter : regularFilterValues.values()) filters.add(regFilter.filter);
            for (Filter filter : userFilters) {
                // если вид панельный, то фильтры не нужны
                if (!filter.Property.GetApplyObject().gridClassView) continue;
                filters.add(filter);
            }

            for(Filter Filt : filters)
                Filt.GetApplyObject().MapFilters.add(Filt);

            // порядки
            for(PropertyView Order : Orders.keySet())
                Order.view.GetApplyObject().MapOrders.add(Order);

        }

        for(GroupObjectImplement group : groups) {

            if ((group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) != 0) {
                Result.ClassViews.put(group, group.gridClassView);
            }
            // если изменились :
            // хоть один класс из этого GroupObjectImplement'a - (флаг Updated - 3)
            boolean updateKeys = (group.updated & GroupObjectImplement.UPDATED_GRIDCLASS)!=0;

            // фильтр\порядок (надо сначала определить что в интерфейсе (верхних объектов Group и класса этого Group) в нем затем сравнить с теми что были до) - (Filters, Orders объектов)
            // фильтры
            // если изменилась структура или кто-то изменил класс, перепроверяем
            if(StructUpdated) {
                Set<Filter> NewFilter = new HashSet();
                for(Filter Filt : group.MapFilters)
                    if(Filt.IsInInterface(group)) NewFilter.add(Filt);

                updateKeys |= !NewFilter.equals(group.Filters);
                group.Filters = NewFilter;
            } else
                for(Filter Filt : group.MapFilters)
                    if(Filt.ClassUpdated(group))
                        updateKeys |= (Filt.IsInInterface(group)? group.Filters.add(Filt): group.Filters.remove(Filt));

            // порядки
            boolean SetOrderChanged = false;
            Set<PropertyObjectImplement> SetOrders = new HashSet(group.orders.keySet());
            for(PropertyView Order : group.MapOrders) {
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(StructUpdated || Order.view.ClassUpdated(group))
                    SetOrderChanged = (Order.view.isInInterface(group)?SetOrders.add(Order.view): group.orders.remove(Order));
            }
            if(StructUpdated || SetOrderChanged) {
                // переформирываваем порядок, если структура или принадлежность Order'у изменилась
                LinkedHashMap<PropertyObjectImplement,Boolean> NewOrder = new LinkedHashMap();
                for(PropertyView Order : group.MapOrders)
                    if(SetOrders.contains(Order.view)) NewOrder.put(Order.view,Orders.get(Order));

                updateKeys |= SetOrderChanged || !(new ArrayList(group.orders.entrySet())).equals(new ArrayList(NewOrder.entrySet())); //Group.Orders.equals(NewOrder)
                group.orders = NewOrder;
            }

            // объекты задействованные в фильтре\порядке (по Filters\Orders верхних элементов GroupImplement'ов на флаг Updated - 0)
            if(!updateKeys)
                for(Filter Filt : group.Filters)
                    if(Filt.ObjectUpdated(group)) {updateKeys = true; break;}
            if(!updateKeys)
                for(PropertyObjectImplement Order : group.orders.keySet())
                    if(Order.ObjectUpdated(group)) {updateKeys = true; break;}
            // проверим на изменение данных
            if(!updateKeys)
                for(Filter Filt : group.Filters)
                    if(DataChanged && Filt.DataUpdated(ChangedProps)) {updateKeys = true; break;}
            if(!updateKeys)
                for(PropertyObjectImplement Order : group.orders.keySet())
                    if(DataChanged && ChangedProps.contains(Order.Property)) {updateKeys = true; break;}
            // классы удалились\добавились
            if(!updateKeys && DataChanged) {
                for(ObjectImplement Object : group)
                    if(ChangedClasses.contains(Object.GridClass)) {updateKeys = true; break;}
            }

            // по возврастанию (0), убыванию (1), центру (2) и откуда начинать
            Map<PropertyObjectImplement,Object> propertySeeks = new HashMap();

            // объект на который будет делаться активным после нахождения ключей
            GroupObjectValue currentObject = group.GetObjectValue();

            // объект относительно которого будет устанавливаться фильтр
            GroupObjectValue objectSeeks = group.GetObjectValue();
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
            for(PropertyObjectImplement Property : UserPropertySeeks.keySet()) {
                if(Property.GetApplyObject()== group) {
                    propertySeeks.put(Property,UserPropertySeeks.get(Property));
                    currentObject = null;
                    updateKeys = true;
                    direction = DIRECTION_CENTER;
                }
            }
            for(ObjectImplement Object : UserObjectSeeks.keySet()) {
                if(Object.GroupTo== group) {
                    objectSeeks.put(Object,UserObjectSeeks.get(Object));
                    currentObject.put(Object,UserObjectSeeks.get(Object));
                    updateKeys = true;
                    direction = DIRECTION_CENTER;
                }
            }

            if(!updateKeys && (group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) !=0) {
               // изменился "классовый" вид перечитываем св-ва
                objectSeeks = group.GetObjectValue();
                updateKeys = true;
                direction = DIRECTION_CENTER;
            }

            if(!updateKeys && group.gridClassView && (group.updated & GroupObjectImplement.UPDATED_OBJECT)!=0) {
                // листание - объекты стали близки к краю (object не далеко от края - надо хранить список не базу же дергать) - изменился объект
                int KeyNum = group.keys.indexOf(group.GetObjectValue());
                // если меньше PageSize осталось и сверху есть ключи
                if(KeyNum< group.PageSize && group.UpKeys) {
                    direction = DIRECTION_UP;
                    updateKeys = true;

                    int lowestInd = group.PageSize*2-1;
                    if (lowestInd >= group.keys.size()) {
                        objectSeeks = new GroupObjectValue();
                        hasMoreKeys = false;
                    } else {
                        objectSeeks = group.keys.get(lowestInd);
                        propertySeeks = group.keyOrders.get(objectSeeks);
                    }

                } else {
                    // наоборот вниз
                    if(KeyNum>= group.keys.size()- group.PageSize && group.DownKeys) {
                        direction = DIRECTION_DOWN;
                        updateKeys = true;

                        int highestInd = group.keys.size()- group.PageSize*2;
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
                    if(objectSeeks.get(Object)==null && Object.BaseClass instanceof IntegralClass && !group.gridClassView)
                        objectSeeks.put(Object,1);

                // докидываем Join'ами (INNER) фильтры, порядки

                // уберем все некорректности в Seekах :
                // корректно если : PropertySeeks = Orders или (Orders.sublist(PropertySeeks.size) = PropertySeeks и ObjectSeeks - пустое)
                // если Orders.sublist(PropertySeeks.size) != PropertySeeks, тогда дочитываем ObjectSeeks полностью
                // выкидываем лишние PropertySeeks, дочитываем недостающие Orders в PropertySeeks
                // также если панель то тупо прочитаем объект
                boolean NotEnoughOrders = !(propertySeeks.keySet().equals(group.orders.keySet()) || ((propertySeeks.size()< group.orders.size() && (new HashSet((new ArrayList(group.orders.keySet())).subList(0, propertySeeks.size()))).equals(propertySeeks.keySet())) && objectSeeks.size()==0));
                boolean objectFound = true;
                if((NotEnoughOrders && objectSeeks.size()< group.size()) || !group.gridClassView) {
                    // дочитываем ObjectSeeks то есть на = PropertySeeks, ObjectSeeks
                    JoinQuery<ObjectImplement,Object> SelectKeys = new JoinQuery<ObjectImplement,Object>(group);
                    SelectKeys.putKeyWhere(objectSeeks);
                    group.fillSourceSelect(SelectKeys, group.getClassGroup(),BL.tableFactory, session);
                    for(Entry<PropertyObjectImplement,Object> Property : propertySeeks.entrySet())
                        SelectKeys.and(new CompareWhere(Property.getKey().getSourceExpr(group.getClassGroup(),SelectKeys.mapKeys, session),
                                Property.getKey().Property.getType().getExpr(Property.getValue()),CompareWhere.EQUALS));

                    // докидываем найденные ключи
                    LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> ResultKeys = SelectKeys.executeSelect(session);
                    if(ResultKeys.size()>0)
                        for(ObjectImplement ObjectKey : group)
                            objectSeeks.put(ObjectKey,ResultKeys.keySet().iterator().next().get(ObjectKey));
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
                    Result.Objects.put(group, objectSeeks);
                    ChangeGroupObject(group, objectSeeks);

                } else {
                    // выкидываем Property которых нет, дочитываем недостающие Orders, по ObjectSeeks то есть не в привязке к отбору
                    if(NotEnoughOrders && objectSeeks.size()== group.size() && group.orders.size() > 0) {
                        JoinQuery<ObjectImplement,PropertyObjectImplement> OrderQuery = new JoinQuery<ObjectImplement,PropertyObjectImplement>(objectSeeks.keySet());
                        OrderQuery.putKeyWhere(objectSeeks);

                        for(PropertyObjectImplement Order : group.orders.keySet())
                            OrderQuery.properties.put(Order, Order.getSourceExpr(group.getClassGroup(),OrderQuery.mapKeys, session));

                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> ResultOrders = OrderQuery.executeSelect(session);
                        for(PropertyObjectImplement Order : group.orders.keySet())
                            propertySeeks.put(Order,ResultOrders.values().iterator().next().get(Order));
                    }

                    LinkedHashMap<Object,Boolean> selectOrders = new LinkedHashMap<Object, Boolean>();
                    JoinQuery<ObjectImplement,Object> selectKeys = new JoinQuery<ObjectImplement,Object>(group); // object потому как нужно еще по ключам упорядочивать, а их тогда надо в св-ва кидать
                    group.fillSourceSelect(selectKeys, group.getClassGroup(),BL.tableFactory, session);

                    // складываются источники и значения
                    List<SourceExpr> orderSources = new ArrayList();
                    List<Object> orderWheres = new ArrayList();
                    List<Boolean> orderDirs = new ArrayList();

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
                    LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> keyResult = new LinkedHashMap();

                    int readSize = group.PageSize*3/(direction ==DIRECTION_CENTER?2:1);

                    JoinQuery<ObjectImplement,Object> copySelect = null;
                    if(direction ==DIRECTION_CENTER)
                        copySelect = new JoinQuery<ObjectImplement, Object>(selectKeys);
                    // откопируем в сторону запрос чтобы еще раз потом использовать
                    // сначала Descending загоним
                    group.DownKeys = false;
                    group.UpKeys = false;
                    if(direction ==DIRECTION_UP || direction ==DIRECTION_CENTER) {
                        if(orderSources.size()>0) {
                            selectKeys.and(GenerateOrderWheres(orderSources,orderWheres,orderDirs,false,0));
                            group.DownKeys = hasMoreKeys;
                        }

//                        System.out.println(Group + " KEYS UP ");
//                        SelectKeys.outSelect(Session,JoinQuery.reverseOrder(SelectOrders),ReadSize);
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> execResult = selectKeys.executeSelect(session,JoinQuery.reverseOrder(selectOrders), readSize);
                        ListIterator<Map<ObjectImplement,Integer>> ik = (new ArrayList(execResult.keySet())).listIterator();
                        while(ik.hasNext()) ik.next();
                        while(ik.hasPrevious()) {
                            Map<ObjectImplement,Integer> Row = ik.previous();
                            keyResult.put(Row,execResult.get(Row));
                        }
                        group.UpKeys = (keyResult.size()== readSize);

                        // проверка чтобы не сбить объект при листании и неправильная (потому как после 2 поиска может получится что надо с 0 без Seek'а перечитывать)
//                        if(OrderSources.size()==0)
                        // сделано так, чтобы при ненайденном объекте текущий объект смещался вверх, а не вниз
                        activeRow = keyResult.size()-1;

                    }
                    if(direction ==DIRECTION_CENTER) selectKeys = copySelect;
                    // потом Ascending
                    if(direction ==DIRECTION_DOWN || direction ==DIRECTION_CENTER) {
                        if(orderSources.size()>0) {
                            selectKeys.and(GenerateOrderWheres(orderSources,orderWheres,orderDirs,true,0));
                            if(direction !=DIRECTION_CENTER) group.UpKeys = hasMoreKeys;
                        }

//                        System.out.println(Group + " KEYS DOWN ");
//                        SelectKeys.outSelect(Session,SelectOrders,ReadSize);
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> executeList = selectKeys.executeSelect(session, selectOrders, readSize);
//                        if((OrderSources.size()==0 || Direction==2) && ExecuteList.size()>0) ActiveRow = KeyResult.size();
                        keyResult.putAll(executeList);
                        group.DownKeys = (executeList.size()== readSize);

                        if ((direction == DIRECTION_DOWN || activeRow == -1) && keyResult.size() > 0)
                            activeRow = 0;
                    }

                    group.keys = new ArrayList();
                    group.keyOrders = new HashMap();

                    // параллельно будем обновлять ключи чтобы Join'ить

                    int groupGID = getGroupObjectGID(group);
                    ViewTable insertTable = BL.tableFactory.viewTables.get(group.size()-1);
                    insertTable.DropViewID(session, groupGID);

                    for(Entry<Map<ObjectImplement,Integer>,Map<Object,Object>> resultRow : keyResult.entrySet()) {
                        GroupObjectValue keyRow = new GroupObjectValue();
                        Map<PropertyObjectImplement,Object> orderRow = new HashMap();

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

                    Result.gridObjects.put(group, group.keys);

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
                            Result.Objects.put(group,newValue);
                            ChangeGroupObject(group,newValue);
//                        }
                    } else
                        ChangeGroupObject(group,new GroupObjectValue());
                }
            }
        }

        Collection<PropertyView> PanelProps = new ArrayList();
        Map<GroupObjectImplement,Collection<PropertyView>> GroupProps = new HashMap();

//        PanelProps.

        for(PropertyView<?> DrawProp : properties) {

            // 3 признака : перечитать, (возможно класс изменился, возможно объектный интерфейс изменился - чисто InterfacePool)
            boolean Read = false;
            boolean CheckClass = false;
            boolean CheckObject = false;
            int InInterface = 0;

            if(DrawProp.ToDraw!=null) {
                // если рисуемся в какой-то вид и обновился источник насильно перечитываем все св-ва
                Read = ((DrawProp.ToDraw.updated & (GroupObjectImplement.UPDATED_KEYS | GroupObjectImplement.UPDATED_CLASSVIEW))!=0);
                Boolean PrevPool = InterfacePool.get(DrawProp);
                InInterface = (PrevPool==null?0:(PrevPool?2:1));
            }

            for(ObjectImplement Object : DrawProp.view.Mapping.values())  {
                if(Object.GroupTo != DrawProp.ToDraw) {
                    // "верхние" объекты интересует только изменение объектов\классов
                    if((Object.Updated & ObjectImplement.UPDATED_OBJECT)!=0) {
                        // изменился верхний объект, перечитываем
                        Read = true;
                        if((Object.Updated & ObjectImplement.UPDATED_CLASS)!=0) {
                            // изменился класс объекта перепроверяем все
                            if(DrawProp.ToDraw!=null) CheckClass = true;
                            CheckObject = true;
                        }
                    }
                } else {
                    // изменился объект и св-во не было классовым
                    if((Object.Updated & ObjectImplement.UPDATED_OBJECT)!=0 && (InInterface!=2 || !DrawProp.ToDraw.gridClassView)) {
                        Read = true;
                        // изменися класс объекта
                        if((Object.Updated & ObjectImplement.UPDATED_CLASS)!=0) CheckObject = true;
                    }
                    // изменение общего класса
                    if((Object.Updated & ObjectImplement.UPDATED_GRIDCLASS)!=0) CheckClass = true;

                }
            }

            // обновим InterfacePool, было в InInterface
            if(CheckClass || CheckObject) {
                int NewInInterface=0;
                if(CheckClass)
                    NewInInterface = (DrawProp.view.isInInterface(DrawProp.ToDraw)?2:0);
                if((CheckObject && !(CheckClass && NewInInterface==2)) || (CheckClass && NewInInterface==0 )) // && InInterface==2))
                    NewInInterface = (DrawProp.view.isInInterface(null)?1:0);

                if(InInterface!=NewInInterface) {
                    InInterface = NewInInterface;

                    if(InInterface==0) {
                        InterfacePool.remove(DrawProp);
                        // !!! СЮДА НАДО ВКИНУТЬ УДАЛЕНИЕ ИЗ ИНТЕРФЕЙСА
                        Result.DropProperties.add(DrawProp);
                    }
                    else
                        InterfacePool.put(DrawProp,InInterface==2);
                }
            }

            if(!Read && (DataChanged && ChangedProps.contains(DrawProp.view.Property)))
                Read = true;

            if (!Read && DataChanged) {
                for (ObjectImplement object : DrawProp.view.Mapping.values()) {
                    if (ChangedClasses.contains(object.BaseClass)) {
                        Read = true;
                        break;
                    }
                }
            }

            if(InInterface>0 && Read) {
                if(InInterface==2 && DrawProp.ToDraw.gridClassView) {
                    Collection<PropertyView> PropList = GroupProps.get(DrawProp.ToDraw);
                    if(PropList==null) {
                        PropList = new ArrayList();
                        GroupProps.put(DrawProp.ToDraw,PropList);
                    }
                    PropList.add(DrawProp);
                } else
                    PanelProps.add(DrawProp);
            }
        }

        // погнали выполнять все собранные запросы и FormChanges

        // сначала PanelProps
        if(PanelProps.size()>0) {
            JoinQuery<Object,PropertyView> SelectProps = new JoinQuery<Object,PropertyView>(new ArrayList<Object>());
            for(PropertyView DrawProp : PanelProps)
                SelectProps.properties.put(DrawProp, DrawProp.view.getSourceExpr(null,null, session));

            Map<PropertyView,Object> ResultProps = SelectProps.executeSelect(session).values().iterator().next();
            for(PropertyView DrawProp : PanelProps)
                Result.PanelProperties.put(DrawProp,ResultProps.get(DrawProp));
        }

        for(Entry<GroupObjectImplement, Collection<PropertyView>> MapGroup : GroupProps.entrySet()) {
            GroupObjectImplement Group = MapGroup.getKey();
            Collection<PropertyView> GroupList = MapGroup.getValue();

            JoinQuery<ObjectImplement,PropertyView> SelectProps = new JoinQuery<ObjectImplement,PropertyView>(Group);

            ViewTable KeyTable = BL.tableFactory.viewTables.get(Group.size()-1);
            Join<KeyField,PropertyField> KeyJoin = new Join<KeyField,PropertyField>(KeyTable);

            ListIterator<KeyField> ikt = KeyTable.objects.listIterator();
            for(ObjectImplement Object : Group)
                KeyJoin.joins.put(ikt.next(),SelectProps.mapKeys.get(Object));
            KeyJoin.joins.put(KeyTable.view,KeyTable.view.type.getExpr(getGroupObjectGID(Group)));
            SelectProps.and(KeyJoin.inJoin);

            for(PropertyView DrawProp : GroupList)
                SelectProps.properties.put(DrawProp, DrawProp.view.getSourceExpr(Group.getClassGroup(),SelectProps.mapKeys, session));

//            System.out.println(Group + " Props ");
//            System.out.println(SelectProps.getComplexity());
//            SelectProps.outSelect(Session);
            LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> ResultProps = SelectProps.executeSelect(session);

            for(PropertyView DrawProp : GroupList) {
                Map<GroupObjectValue,Object> PropResult = new HashMap();
                Result.GridProperties.put(DrawProp,PropResult);

                for(Entry<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> ResultRow : ResultProps.entrySet())
                    PropResult.put(new GroupObjectValue(ResultRow.getKey()),ResultRow.getValue().get(DrawProp));
            }
        }

        UserPropertySeeks.clear();
        UserObjectSeeks.clear();

        pendingGroupChanges.clear();

        // сбрасываем все пометки
        StructUpdated = false;
        for(GroupObjectImplement Group : groups) {
            Iterator<ObjectImplement> io = Group.iterator();
            while(io.hasNext()) io.next().Updated=0;
            Group.updated = 0;
        }
        DataChanged = false;

//        Result.Out(this);

        return Result;
    }

    // возвращает какие объекты отчета фиксируются
    private Set<GroupObjectImplement> getReportObjects() {

        Set<GroupObjectImplement> reportObjects = new HashSet();
        for (GroupObjectImplement group : groups) {
            if (group.gridClassView)
                reportObjects.add(group);
        }

        return reportObjects;
    }

    // считывает все данные (для отчета)
    private ReportData getReportData() throws SQLException {

        Set<GroupObjectImplement> reportObjects = getReportObjects();

        Collection<ObjectImplement> readObjects = new ArrayList();
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

    List<Map<Integer,Integer>> readOrder = new ArrayList();
    Map<String,Integer> objectsID = new HashMap();
    Map<String,Integer> propertiesID = new HashMap();
    Map<Integer,Map<Map<Integer,Integer>,Object>> properties = new HashMap();

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
        Object Value = null;
        if(objectsID.containsKey(fieldName))
            Value = readOrder.get(CurrentRow).get(objectsID.get(fieldName));
        else {
            Integer propertyID = propertiesID.get(fieldName);
            if (propertyID == null) throw new RuntimeException("Поле " + fieldName + " отсутствует в переданных данных");
            Value = properties.get(propertiesID.get(fieldName)).get(readOrder.get(CurrentRow));
        }

        if (Date.class.getName().equals(jrField.getValueClassName()) && Value != null) {
            Value = DateConverter.intToDate((Integer)Value);
        }

        if(Value instanceof String)
            Value = ((String)Value).trim();

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
        
        return Value;
    }
}