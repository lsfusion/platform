/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.*;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

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

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup,Map<ObjectImplement,SourceExpr> ClassSource) {
        return (ClassGroup!=null && ClassGroup.contains(GroupTo)?ClassSource.get(this):new ValueSourceExpr(idObject,Type.Object));
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
            throw new RuntimeException("ID must be less than " + RemoteForm.GID_SHIFT);

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
    LinkedHashMap<PropertyObjectImplement,Boolean> Orders = new LinkedHashMap();

    boolean UpKeys, DownKeys;
    List<GroupObjectValue> Keys = null;
    // какие ключи активны
    Map<GroupObjectValue,Map<PropertyObjectImplement,Object>> KeyOrders = null;

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    static int UPDATED_OBJECT = (1 << 0);
    static int UPDATED_KEYS = (1 << 2);
    static int UPDATED_GRIDCLASS = (1 << 3);
    static int UPDATED_CLASSVIEW = (1 << 4);

    int Updated = UPDATED_GRIDCLASS | UPDATED_CLASSVIEW;

    int PageSize = 33;

    GroupObjectValue GetObjectValue() {
        GroupObjectValue Result = new GroupObjectValue();
        for(ObjectImplement Object : this)
            Result.put(Object,Object.idObject);

        return Result;
    }

    // получает Set группы
    Set<GroupObjectImplement> GetClassGroup() {

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
            Query<KeyField,PropertyField> ObjectQuery = TableFactory.ObjectTable.getClassJoin(Object.GridClass);
            if(Session!=null && Session.Changes.AddClasses.contains(Object.GridClass)) {
                // придется UnionQuery делать, ObjectTable'а Key и AddClass Object'а
                UnionQuery<KeyField,PropertyField> ResultQuery = new UnionQuery<KeyField,PropertyField>(ObjectQuery.Keys,2);

                ResultQuery.add(ObjectQuery,1);

                // придется создавать запрос чтобы ключи перекодировать
                JoinQuery<KeyField,PropertyField> AddQuery = new JoinQuery<KeyField, PropertyField>(ObjectQuery.Keys);
                Join<KeyField,PropertyField> AddJoin = new Join<KeyField,PropertyField>(TableFactory.AddClassTable.getClassJoin(Session,Object.GridClass),true);
                AddJoin.Joins.put(TableFactory.AddClassTable.Object,AddQuery.MapKeys.get(TableFactory.ObjectTable.Key));
                AddQuery.add(AddJoin);
                ResultQuery.add(AddQuery,1);

                ObjectQuery = ResultQuery;
            }

            Join<KeyField,PropertyField> ObjectJoin = new Join<KeyField,PropertyField>(ObjectQuery,true);
            ObjectJoin.Joins.put(TableFactory.ObjectTable.Key,Query.MapKeys.get(Object));
            Query.add(ObjectJoin);

            if(Session!=null && Session.Changes.RemoveClasses.contains(Object.GridClass))
                TableFactory.RemoveClassTable.excludeJoin(Query,Session,Object.GridClass,Query.MapKeys.get(Object));
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
        for(P Interface : Property.Interfaces) {
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

    ChangeValue getChangeProperty(DataSession Session) {
        Map<P,ObjectValue> Interface = new HashMap<P,ObjectValue>();
        for(Entry<P, ObjectImplement> Implement : Mapping.entrySet())
            Interface.put(Implement.getKey(),new ObjectValue(Implement.getValue().idObject,Implement.getValue().Class));
        return Property.getChangeProperty(Session,Interface,1);
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, boolean NotNull) {

        Map<P,SourceExpr> JoinImplement = new HashMap<P,SourceExpr>();
        for(P Interface : Property.Interfaces)
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
        return Session.getSourceExpr(Property,JoinImplement,new InterfaceClassSet<P>(JoinClasses),NotNull);
    }
}

// представление св-ва
class PropertyView<P extends PropertyInterface> {
    PropertyObjectImplement<P> View;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    GroupObjectImplement ToDraw;

    PropertyView(int iID,PropertyObjectImplement<P> iView,GroupObjectImplement iToDraw) {
        View = iView;
        ToDraw = iToDraw;
        ID = iID;
    }

    public PropertyView(PropertyView<P> navigatorProperty) {

        ID = navigatorProperty.ID;
        View = navigatorProperty.View;
        ToDraw = navigatorProperty.ToDraw;
    }

    public String toString() {
        return View.toString();
    }

    // идентификатор (в рамках формы)
    int ID = 0;
}

class AbstractFormChanges<T,V,Z> {

    Map<T,Boolean> ClassViews = new HashMap();
    Map<T,V> Objects = new HashMap();
    Map<T,List<V>> GridObjects = new HashMap();
    Map<Z,Map<V,Object>> GridProperties = new HashMap();
    Map<Z,Object> PanelProperties = new HashMap();
    Set<Z> DropProperties = new HashSet();
}

// класс в котором лежит какие изменения произошли
// появляется по сути для отделения клиента, именно он возвращается назад клиенту
class FormChanges extends AbstractFormChanges<GroupObjectImplement,GroupObjectValue,PropertyView> {

    void Out(RemoteForm bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for(GroupObjectImplement Group : (List<GroupObjectImplement>)bv.Groups) {
            List<GroupObjectValue> GroupGridObjects = GridObjects.get(Group);
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
        Query.add(new FieldExprCompareWhere(Property.getSourceExpr(ClassGroup,Query.MapKeys,Session, true),Value.getValueExpr(ClassGroup,Query.MapKeys,Session,Property.Property.getType()),Compare));
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

    abstract SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, Type DBType);
}


class UserValueLink extends ValueLink {

    Object Value;

    UserValueLink(Object iValue) {Value=iValue;}

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return new ValueSourceExpr(Value,DBType);
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

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, Type DBType) {
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

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return Property.getSourceExpr(ClassGroup,ClassSource,Session, true);
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

    DataSession Session;

    RemoteForm(int iID, T iBL, DataSession iSession) throws SQLException {

        ID = iID;

        BL = iBL;

        Session = iSession;

        StructUpdated = true;

        GID = BL.TableFactory.idTable.GenerateID(Session, IDTable.FORM);
    }

    List<GroupObjectImplement> Groups = new ArrayList();
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    List<PropertyView> Properties = new ArrayList();

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

    public byte[] getPropertyEditorObjectValueByteArray(int propertyID) {
        return ByteArraySerializer.serializeChangeValue(getPropertyEditorObjectValue(getPropertyView(propertyID)));
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
        ChangeClass(object, (classID == -1) ? null : object.BaseClass.findClassID(classID));
    }

    public void ChangePropertyView(Integer propertyID, byte[] object) throws SQLException {
        ChangePropertyView(getPropertyView(propertyID), ByteArraySerializer.deserializeObject(object));
    }

    // ----------------------- Применение изменений ------------------------------- //
    public byte[] getFormChangesByteArray() throws SQLException {
        return ByteArraySerializer.serializeFormChanges(EndApply());
    }
      
    // --------------------------------------------------------------------------------------- //
    // ----------------------------------- Управляющий интерфейс ----------------------------- //
    // --------------------------------------------------------------------------------------- //

    // ----------------------------------- Поиск объектов по ID ------------------------------ //

    GroupObjectImplement getGroupObjectImplement(int groupID) {
        for (GroupObjectImplement groupObject : Groups)
            if (groupObject.ID == groupID)
                return groupObject;
        return null;
    }

    ObjectImplement getObjectImplement(int objectID) {
        for (GroupObjectImplement groupObject : Groups)
            for (ObjectImplement object : groupObject)
                if (object.ID == objectID)
                    return object;
        return null;
    }

    PropertyView getPropertyView(int propertyID) {
        for (PropertyView property : Properties)
            if (property.ID == propertyID)
                return property;
        return null;
    }

    private ChangeValue getPropertyEditorObjectValue(PropertyView propertyView) {
        return propertyView.View.getChangeProperty(Session);
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

    private void ChangeObject(ObjectImplement object, Integer value) throws SQLException {

        if (object.idObject == value) return;

        object.idObject = value;

        // запишем класс объекта
        Class objectClass = null;
        if(object.BaseClass instanceof ObjectClass)
            objectClass = Session.getObjectClass(value);
        else
            objectClass = object.BaseClass;

        if(object.Class != objectClass) {

            object.Class = objectClass;

            object.Updated = object.Updated | ObjectImplement.UPDATED_CLASS;
        }

        object.Updated = object.Updated | ObjectImplement.UPDATED_OBJECT;
        object.GroupTo.Updated = object.GroupTo.Updated | GroupObjectImplement.UPDATED_OBJECT;

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
        Object.GroupTo.Updated = Object.GroupTo.Updated | GroupObjectImplement.UPDATED_GRIDCLASS;

    }

    private void switchClassView(GroupObjectImplement Group) {
        changeClassView(Group, !Group.gridClassView);
    }

    private void changeClassView(GroupObjectImplement Group,Boolean Show) {

        if(Group.gridClassView == Show) return;
        Group.gridClassView = Show;

        // расставляем пометки
        Group.Updated = Group.Updated | GroupObjectImplement.UPDATED_CLASSVIEW;

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
        Integer AddID = BL.AddObject(Session, cls);

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
                            Join<KeyField,PropertyField> ObjectJoin = new Join<KeyField,PropertyField>(BL.TableFactory.ObjectTable.getClassJoin(SibObject.GridClass),true);
                            ObjectJoin.Joins.put(BL.TableFactory.ObjectTable.Key,SubQuery.MapKeys.get(SibObject));
                            SubQuery.add(ObjectJoin);
                        } else
                            FixedObjects.put(SibObject,AddID);
                    }
                }

                SubQuery.putDumbJoin(FixedObjects);

                SubQuery.add("newvalue",Filter.Value.getValueExpr(Object.GroupTo.GetClassGroup(),SubQuery.MapKeys,Session,Filter.Property.Property.getType()));

                LinkedHashMap<Map<ObjectImplement,Integer>,Map<String,Object>> Result = SubQuery.executeSelect(Session);
                // изменяем св-ва
                for(Entry<Map<ObjectImplement,Integer>,Map<String,Object>> Row : Result.entrySet()) {
                    Property ChangeProperty = Filter.Property.Property;
                    Map<PropertyInterface,ObjectValue> Keys = new HashMap();
                    for(PropertyInterface Interface : (Collection<PropertyInterface>)ChangeProperty.Interfaces) {
                        ObjectImplement ChangeObject = Filter.Property.Mapping.get(Interface);
                        Keys.put(Interface,new ObjectValue(Row.getKey().get(ChangeObject),ChangeObject.GridClass));
                    }
                    ChangeProperty.changeProperty(Keys,Row.getValue().get("newvalue"),Session);
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

    public void ChangeClass(ObjectImplement Object,Class Class) throws SQLException {

        BL.ChangeClass(Session, Object.idObject,Class);

        // Если объект удалили, то сбрасываем текущий объект в null
        if (Class == null) {
            ChangeObject(Object, null);
        }

        Object.Updated = Object.Updated | ObjectImplement.UPDATED_CLASS;

        DataChanged = true;
    }

    private void ChangePropertyView(PropertyView Property,Object Value) throws SQLException {
        ChangeProperty(Property.View,Value);
    }

    private void ChangeProperty(PropertyObjectImplement property,Object value) throws SQLException {

        // изменяем св-во
        property.Property.changeProperty(fillPropertyInterface(property),value,Session);

        DataChanged = true;
    }

    // Обновление данных
    public void refreshData() {

        for(GroupObjectImplement Group : Groups) {
            Group.Updated |= GroupObjectImplement.UPDATED_GRIDCLASS;
        }
    }

    // Применение изменений
    public String SaveChanges() throws SQLException {
        return BL.Apply(Session);
    }

    public void CancelChanges() throws SQLException {
        Session.restart(true);

        DataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(Class cls, Integer objectID) {}
    protected void gainedFocus() {
        DataChanged = true;
    }

    void Close() throws SQLException {

        Session.IncrementChanges.remove(this);
        for(GroupObjectImplement Group : Groups) {
            ViewTable DropTable = BL.TableFactory.ViewTables.get(Group.size()-1);
            DropTable.DropViewID(Session, getGroupObjectGID(Group));
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    private Map<PropertyInterface,ObjectValue> fillPropertyInterface(PropertyObjectImplement<?> property) {

        Property changeProperty = property.Property;
        Map<PropertyInterface,ObjectValue> keys = new HashMap();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)changeProperty.Interfaces) {
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
                    CompareIndex = FieldExprCompareWhere.LESS_EQUALS;
                else
                    CompareIndex = FieldExprCompareWhere.LESS;
            } else
                CompareIndex = FieldExprCompareWhere.GREATER;
        } else {
            if (Down) {
                if (Last)
                    CompareIndex = FieldExprCompareWhere.GREATER_EQUALS;
                else
                    CompareIndex = FieldExprCompareWhere.GREATER;
            } else
                CompareIndex = FieldExprCompareWhere.LESS;
        }
        Where OrderWhere = new FieldExprCompareWhere(OrderExpr,OrderValue,CompareIndex);

        if(!Last) {
            Where NextWhere = GenerateOrderWheres(OrderSources,OrderWheres,OrderDirs,Down,Index+1);

            // >A OR (=A AND >B)
            return new FieldOPWhere(OrderWhere,new FieldOPWhere(new FieldExprCompareWhere(OrderExpr,OrderValue,FieldExprCompareWhere.EQUALS),NextWhere,true),false);
        } else
            return OrderWhere;
    }

    public Collection<Property> getUpdateProperties() {

        Set<Property> Result = new HashSet();
        for(PropertyView PropView : Properties)
            Result.add(PropView.View.Property);
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
        return Session.hasChanges();
    }

    private static int DIRECTION_DOWN = 0;
    private static int DIRECTION_UP = 1;
    private static int DIRECTION_CENTER = 2;

    private FormChanges EndApply() throws SQLException {

        FormChanges Result = new FormChanges();

        // если изменились данные, применяем изменения
        Collection<Property> ChangedProps;
        Collection<Class> ChangedClasses = new HashSet();
        if(DataChanged)
            ChangedProps = Session.update(this,ChangedClasses);
        else
            ChangedProps = new ArrayList();

        // бежим по списку вниз
        if(StructUpdated) {
            // построим Map'ы
            // очистим старые

            for(GroupObjectImplement Group : Groups) {
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
                Order.View.GetApplyObject().MapOrders.add(Order);
        }

        for(GroupObjectImplement Group : Groups) {

            if ((Group.Updated & GroupObjectImplement.UPDATED_CLASSVIEW) != 0) {
                Result.ClassViews.put(Group, Group.gridClassView);
            }
            // если изменились :
            // хоть один класс из этого GroupObjectImplement'a - (флаг Updated - 3)
            boolean UpdateKeys = (Group.Updated & GroupObjectImplement.UPDATED_GRIDCLASS)!=0;
            // фильтр\порядок (надо сначала определить что в интерфейсе (верхних объектов Group и класса этого Group) в нем затем сравнить с теми что были до) - (Filters, Orders объектов)
            // фильтры
            // если изменилась структура или кто-то изменил класс, перепроверяем
            if(StructUpdated) {
                Set<Filter> NewFilter = new HashSet();
                for(Filter Filt : Group.MapFilters)
                    if(Filt.IsInInterface(Group)) NewFilter.add(Filt);

                UpdateKeys = UpdateKeys || !NewFilter.equals(Group.Filters);
                Group.Filters = NewFilter;
            } else
                for(Filter Filt : Group.MapFilters)
                    if(Filt.ClassUpdated(Group))
                        UpdateKeys = (Filt.IsInInterface(Group)?Group.Filters.add(Filt):Group.Filters.remove(Filt)) || UpdateKeys;

            // порядки
            boolean SetOrderChanged = false;
            Set<PropertyObjectImplement> SetOrders = new HashSet(Group.Orders.keySet());
            for(PropertyView Order : Group.MapOrders) {
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(StructUpdated || Order.View.ClassUpdated(Group))
                    SetOrderChanged = (Order.View.isInInterface(Group)?SetOrders.add(Order.View):Group.Orders.remove(Order));
            }
            if(StructUpdated || SetOrderChanged) {
                // переформирываваем порядок, если структура или принадлежность Order'у изменилась
                LinkedHashMap<PropertyObjectImplement,Boolean> NewOrder = new LinkedHashMap();
                for(PropertyView Order : Group.MapOrders)
                    if(SetOrders.contains(Order.View)) NewOrder.put(Order.View,Orders.get(Order));

                UpdateKeys = UpdateKeys || SetOrderChanged || !(new ArrayList(Group.Orders.entrySet())).equals(new ArrayList(NewOrder.entrySet())); //Group.Orders.equals(NewOrder)
                Group.Orders = NewOrder;
            }

            // объекты задействованные в фильтре\порядке (по Filters\Orders верхних элементов GroupImplement'ов на флаг Updated - 0)
            if(!UpdateKeys)
                for(Filter Filt : Group.Filters)
                    if(Filt.ObjectUpdated(Group)) {UpdateKeys = true; break;}
            if(!UpdateKeys)
                for(PropertyObjectImplement Order : Group.Orders.keySet())
                    if(Order.ObjectUpdated(Group)) {UpdateKeys = true; break;}
            // проверим на изменение данных
            if(!UpdateKeys)
                for(Filter Filt : Group.Filters)
                    if(DataChanged && Filt.DataUpdated(ChangedProps)) {UpdateKeys = true; break;}
            if(!UpdateKeys)
                for(PropertyObjectImplement Order : Group.Orders.keySet())
                    if(DataChanged && ChangedProps.contains(Order.Property)) {UpdateKeys = true; break;}
            // классы удалились\добавились
            if(!UpdateKeys && DataChanged) {
                for(ObjectImplement Object : Group)
                    if(ChangedClasses.contains(Object.GridClass)) {UpdateKeys = true; break;}
            }

            // по возврастанию (0), убыванию (1), центру (2) и откуда начинать
            Map<PropertyObjectImplement,Object> PropertySeeks = new HashMap();

            // объект на который будет делаться активным после нахождения ключей
            GroupObjectValue currentObject = Group.GetObjectValue();

            // объект относительно которого будет устанавливаться фильтр
            GroupObjectValue ObjectSeeks = Group.GetObjectValue();
            int Direction;
            boolean hasMoreKeys = true;

            if (ObjectSeeks.containsValue(null)) {
                ObjectSeeks = new GroupObjectValue();
                Direction = DIRECTION_DOWN;
            } else
                Direction = DIRECTION_CENTER;

            // Различные переходы - в самое начало или конец
            Integer pendingChanges = pendingGroupChanges.get(Group);
            if (pendingChanges == null) pendingChanges = -1;

            if (pendingChanges == CHANGEGROUPOBJECT_FIRSTROW) {
                ObjectSeeks = new GroupObjectValue();
                currentObject = null;
                UpdateKeys = true;
                hasMoreKeys = false;
                Direction = DIRECTION_DOWN;
            }

            if (pendingChanges == CHANGEGROUPOBJECT_LASTROW) {
                ObjectSeeks = new GroupObjectValue();
                currentObject = null;
                UpdateKeys = true;
                hasMoreKeys = false;
                Direction = DIRECTION_UP;
            }

            // один раз читаем не так часто делается, поэтому не будем как с фильтрами
            for(PropertyObjectImplement Property : UserPropertySeeks.keySet()) {
                if(Property.GetApplyObject()==Group) {
                    PropertySeeks.put(Property,UserPropertySeeks.get(Property));
                    currentObject = null;
                    UpdateKeys = true;
                    Direction = DIRECTION_CENTER;
                }
            }
            for(ObjectImplement Object : UserObjectSeeks.keySet()) {
                if(Object.GroupTo==Group) {
                    ObjectSeeks.put(Object,UserObjectSeeks.get(Object));
                    currentObject.put(Object,UserObjectSeeks.get(Object));
                    UpdateKeys = true;
                    Direction = DIRECTION_CENTER;
                }
            }

            if(!UpdateKeys && (Group.Updated & GroupObjectImplement.UPDATED_CLASSVIEW) !=0) {
               // изменился "классовый" вид перечитываем св-ва
                ObjectSeeks = Group.GetObjectValue();
                UpdateKeys = true;
                Direction = DIRECTION_CENTER;
            }

            if(!UpdateKeys && Group.gridClassView && (Group.Updated & GroupObjectImplement.UPDATED_OBJECT)!=0) {
                // листание - объекты стали близки к краю (object не далеко от края - надо хранить список не базу же дергать) - изменился объект
                int KeyNum = Group.Keys.indexOf(Group.GetObjectValue());
                // если меньше PageSize осталось и сверху есть ключи
                if(KeyNum<Group.PageSize && Group.UpKeys) {
                    Direction = DIRECTION_UP;
                    UpdateKeys = true;

                    int lowestInd = Group.PageSize*2-1;
                    if (lowestInd >= Group.Keys.size()) {
                        lowestInd = Group.Keys.size()-1;
                        hasMoreKeys = false;
                    }

                    if(lowestInd < Group.Keys.size()) {
                        ObjectSeeks = Group.Keys.get(lowestInd);
                        PropertySeeks = Group.KeyOrders.get(ObjectSeeks);
                    }

                } else {
                    // наоборот вниз
                    if(KeyNum>=Group.Keys.size()-Group.PageSize && Group.DownKeys) {
                        Direction = DIRECTION_DOWN;
                        UpdateKeys = true;

                        int highestInd = Group.Keys.size()-Group.PageSize*2;
                        if (highestInd < 0) {
                            highestInd = 0;
                            hasMoreKeys = false;
                        }

                        if (highestInd < Group.Keys.size()) {
                            ObjectSeeks = Group.Keys.get(highestInd);
                            PropertySeeks = Group.KeyOrders.get(ObjectSeeks);
                        }
                    }
                }
            }

            if(UpdateKeys) {
                // --- перечитываем источник (если "классовый" вид - 50, + помечаем изменения GridObjects, иначе TOP 1

                // проверим на интегральные классы в Group'e
                for(ObjectImplement Object : Group)
                    if(ObjectSeeks.get(Object)==null && Object.BaseClass instanceof IntegralClass && !Group.gridClassView)
                        ObjectSeeks.put(Object,1);

                // докидываем Join'ами (INNER) фильтры, порядки

                // уберем все некорректности в Seekах :
                // корректно если : PropertySeeks = Orders или (Orders.sublist(PropertySeeks.size) = PropertySeeks и ObjectSeeks - пустое)
                // если Orders.sublist(PropertySeeks.size) != PropertySeeks, тогда дочитываем ObjectSeeks полностью
                // выкидываем лишние PropertySeeks, дочитываем недостающие Orders в PropertySeeks
                // также если панель то тупо прочитаем объект
                boolean NotEnoughOrders = !(PropertySeeks.keySet().equals(Group.Orders.keySet()) || ((PropertySeeks.size()<Group.Orders.size() && (new HashSet((new ArrayList(Group.Orders.keySet())).subList(0,PropertySeeks.size()))).equals(PropertySeeks.keySet())) && ObjectSeeks.size()==0));
                boolean objectFound = true;
                if((NotEnoughOrders && ObjectSeeks.size()<Group.size()) || !Group.gridClassView) {
                    // дочитываем ObjectSeeks то есть на = PropertySeeks, ObjectSeeks
                    OrderedJoinQuery<ObjectImplement,Object> SelectKeys = new OrderedJoinQuery<ObjectImplement,Object>(Group);
                    SelectKeys.putDumbJoin(ObjectSeeks);
                    Group.fillSourceSelect(SelectKeys,Group.GetClassGroup(),BL.TableFactory,Session);
                    for(Entry<PropertyObjectImplement,Object> Property : PropertySeeks.entrySet())
                        SelectKeys.add(new FieldExprCompareWhere(Property.getKey().getSourceExpr(Group.GetClassGroup(),SelectKeys.MapKeys,Session, true),Property.getValue(),0));

                    // докидываем найденные ключи
                    SelectKeys.Top = 1;
                    LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> ResultKeys = SelectKeys.executeSelect(Session);
                    if(ResultKeys.size()>0)
                        for(ObjectImplement ObjectKey : Group)
                            ObjectSeeks.put(ObjectKey,ResultKeys.keySet().iterator().next().get(ObjectKey));
                    else
                        objectFound = false;
                }

                if(!Group.gridClassView) {

                    // если не нашли объект, то придется искать
                    if (!objectFound) {

                        OrderedJoinQuery<ObjectImplement,Object> SelectKeys = new OrderedJoinQuery<ObjectImplement,Object>(Group);
                        Group.fillSourceSelect(SelectKeys,Group.GetClassGroup(),BL.TableFactory,Session);
                        SelectKeys.Top = 1;
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> ResultKeys = SelectKeys.executeSelect(Session);
                        if(ResultKeys.size()>0)
                            for(ObjectImplement ObjectKey : Group)
                                ObjectSeeks.put(ObjectKey,ResultKeys.keySet().iterator().next().get(ObjectKey));
                    }
                    
                    // если панель и ObjectSeeks "полный", то просто меняем объект и ничего не читаем
                    Result.Objects.put(Group,ObjectSeeks);
                    ChangeGroupObject(Group,ObjectSeeks);

                } else {
                    // выкидываем Property которых нет, дочитываем недостающие Orders, по ObjectSeeks то есть не в привязке к отбору
                    if(NotEnoughOrders && ObjectSeeks.size()==Group.size() && Group.Orders.size() > 0) {
                        JoinQuery<ObjectImplement,PropertyObjectImplement> OrderQuery = new JoinQuery<ObjectImplement,PropertyObjectImplement>(ObjectSeeks.keySet());
                        OrderQuery.putDumbJoin(ObjectSeeks);

                        for(PropertyObjectImplement Order : Group.Orders.keySet())
                            OrderQuery.add(Order,Order.getSourceExpr(Group.GetClassGroup(),OrderQuery.MapKeys,Session, false));

                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> ResultOrders = OrderQuery.executeSelect(Session);
                        for(PropertyObjectImplement Order : Group.Orders.keySet())
                            PropertySeeks.put(Order,ResultOrders.values().iterator().next().get(Order));
                    }

                    OrderedJoinQuery<ObjectImplement,PropertyObjectImplement> SelectKeys = new OrderedJoinQuery<ObjectImplement,PropertyObjectImplement>(Group);
                    Group.fillSourceSelect(SelectKeys,Group.GetClassGroup(),BL.TableFactory,Session);

                    // складываются источники и значения
                    List<SourceExpr> OrderSources = new ArrayList();
                    List<Object> OrderWheres = new ArrayList();
                    List<Boolean> OrderDirs = new ArrayList();

                    // закинем порядки (с LEFT JOIN'ом)
                    for(Map.Entry<PropertyObjectImplement,Boolean> ToOrder : Group.Orders.entrySet()) {
                        SourceExpr OrderExpr = ToOrder.getKey().getSourceExpr(Group.GetClassGroup(),SelectKeys.MapKeys,Session,false);
                        SelectKeys.Orders.put(OrderExpr,ToOrder.getValue());
                        // надо закинуть их в запрос, а также установить фильтры на порядки чтобы
                        if(PropertySeeks.containsKey(ToOrder.getKey())) {
                            OrderSources.add(OrderExpr);
                            OrderWheres.add(PropertySeeks.get(ToOrder.getKey()));
                            OrderDirs.add(ToOrder.getValue());
                        }
                        // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                        SelectKeys.add(ToOrder.getKey(),OrderExpr);
                    }

                    // докинем в ObjectSeeks недостающие группы
                    for(ObjectImplement ObjectKey : Group)
                        if(!ObjectSeeks.containsKey(ObjectKey))
                            ObjectSeeks.put(ObjectKey,null);

                    // закинем объекты в порядок
                    for(ObjectImplement ObjectKey : ObjectSeeks.keySet()) {
                        // также закинем их в порядок и в запрос6
                        SourceExpr KeyExpr = SelectKeys.MapKeys.get(ObjectKey);
                        SelectKeys.Orders.put(KeyExpr,false);
                        Integer SeekValue = ObjectSeeks.get(ObjectKey);
                        if(SeekValue!=null) {
                            OrderSources.add(KeyExpr);
                            OrderWheres.add(SeekValue);
                            OrderDirs.add(false);
                        }
                    }


                    // выполняем запрос
                    // какой ряд выбранным будем считать
                    int ActiveRow = -1;
                    // результат
                    LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> KeyResult = new LinkedHashMap();

                    SelectKeys.Top = Group.PageSize*3/(Direction==DIRECTION_CENTER?2:1);

                    // откопируем в сторону запрос чтобы еще раз потом использовать
                    OrderedJoinQuery<ObjectImplement,PropertyObjectImplement> SelectAscKeys = (Direction==2?SelectKeys.copy():SelectKeys);
                    // сначала Descending загоним
                    Group.DownKeys = false;
                    Group.UpKeys = false;
                    if(Direction==DIRECTION_UP || Direction==DIRECTION_CENTER) {
                        if(OrderSources.size()>0) {
                            SelectKeys.add(GenerateOrderWheres(OrderSources,OrderWheres,OrderDirs,false,0));
                            Group.DownKeys = hasMoreKeys;
                        }

                        SelectKeys.Up = true;
//                        SelectKeys.outSelect(Session);
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> ExecResult = SelectKeys.executeSelect(Session);
                        ListIterator<Map<ObjectImplement,Integer>> ik = (new ArrayList(ExecResult.keySet())).listIterator();
                        while(ik.hasNext()) ik.next();
                        while(ik.hasPrevious()) {
                            Map<ObjectImplement,Integer> Row = ik.previous();
                            KeyResult.put(Row,ExecResult.get(Row));
                        }
                        Group.UpKeys = (KeyResult.size()==SelectKeys.Top);

                        // проверка чтобы не сбить объект при листании и неправильная (потому как после 2 поиска может получится что надо с 0 без Seek'а перечитывать)
//                        if(OrderSources.size()==0)
                        // сделано так, чтобы при ненайденном объекте текущий объект смещался вверх, а не вниз
                        ActiveRow = KeyResult.size()-1;

                    }
                    SelectKeys = SelectAscKeys;
                    // потом Ascending
                    if(Direction==DIRECTION_DOWN || Direction==DIRECTION_CENTER) {
                        if(OrderSources.size()>0) {
                            SelectKeys.add(GenerateOrderWheres(OrderSources,OrderWheres,OrderDirs,true,0));
                            if(Direction!=DIRECTION_CENTER) Group.UpKeys = hasMoreKeys;
                        }

                        SelectKeys.Up = false;
//                        SelectKeys.outSelect(Session);
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> ExecuteList = SelectKeys.executeSelect(Session);
//                        if((OrderSources.size()==0 || Direction==2) && ExecuteList.size()>0) ActiveRow = KeyResult.size();
                        KeyResult.putAll(ExecuteList);
                        Group.DownKeys = (ExecuteList.size()==SelectKeys.Top);

                        if ((Direction == DIRECTION_DOWN || ActiveRow == -1) && KeyResult.size() > 0)
                            ActiveRow = 0;
                    }

                    Group.Keys = new ArrayList();
                    Group.KeyOrders = new HashMap();

                    // параллельно будем обновлять ключи чтобы Join'ить

                    int groupGID = getGroupObjectGID(Group);
                    ViewTable InsertTable = BL.TableFactory.ViewTables.get(Group.size()-1);
                    InsertTable.DropViewID(Session, groupGID);

                    for(Entry<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> ResultRow : KeyResult.entrySet()) {
                        GroupObjectValue KeyRow = new GroupObjectValue();
                        Map<PropertyObjectImplement,Object> OrderRow = new HashMap();

                        // закинем сразу ключи для св-в чтобы Join'ить
                        Map<KeyField,Integer> ViewKeyInsert = new HashMap();
                        ViewKeyInsert.put(InsertTable.View,groupGID);
                        ListIterator<KeyField> ivk = InsertTable.Objects.listIterator();

                        // важен правильный порядок в KeyRow
                        for(ObjectImplement ObjectKey : Group) {
                            Integer KeyValue = ResultRow.getKey().get(ObjectKey);
                            KeyRow.put(ObjectKey,KeyValue);
                            ViewKeyInsert.put(ivk.next(), KeyValue);
                        }
                        Session.InsertRecord(InsertTable,ViewKeyInsert,new HashMap());

                        for(PropertyObjectImplement ToOrder : Group.Orders.keySet())
                            OrderRow.put(ToOrder,ResultRow.getValue().get(ToOrder));

                        Group.Keys.add(KeyRow);
                        Group.KeyOrders.put(KeyRow, OrderRow);
                    }

                    Result.GridObjects.put(Group,Group.Keys);

                    Group.Updated = (Group.Updated | GroupObjectImplement.UPDATED_KEYS);

                    // если ряд никто не подставил и ключи есть пробуем старый найти
//                    if(ActiveRow<0 && Group.Keys.size()>0)
//                        ActiveRow = Group.Keys.indexOf(Group.GetObjectValue());

                    // если есть в новых ключах старый ключ, то делаем его активным
                    if (Group.Keys.contains(currentObject))
                        ActiveRow = Group.Keys.indexOf(currentObject);

                    if(ActiveRow>=0) {
                        // нашли ряд его выбираем
                        GroupObjectValue newValue = Group.Keys.get(ActiveRow);
//                        if (!newValue.equals(Group.GetObjectValue())) {
                            Result.Objects.put(Group,newValue);
                            ChangeGroupObject(Group,newValue);
//                        }
                    }
//                    else
//                        ChangeGroupObject(Group,new GroupObjectValue());
                }
            }
        }

        Collection<PropertyView> PanelProps = new ArrayList();
        Map<GroupObjectImplement,Collection<PropertyView>> GroupProps = new HashMap();

//        PanelProps.

        for(PropertyView<?> DrawProp : Properties) {

            // 3 признака : перечитать, (возможно класс изменился, возможно объектный интерфейс изменился - чисто InterfacePool)
            boolean Read = false;
            boolean CheckClass = false;
            boolean CheckObject = false;
            int InInterface = 0;

            if(DrawProp.ToDraw!=null) {
                // если рисуемся в какой-то вид и обновился источник насильно перечитываем все св-ва
                Read = ((DrawProp.ToDraw.Updated & (GroupObjectImplement.UPDATED_KEYS | GroupObjectImplement.UPDATED_CLASSVIEW))!=0);
                Boolean PrevPool = InterfacePool.get(DrawProp);
                InInterface = (PrevPool==null?0:(PrevPool?2:1));
            }

            for(ObjectImplement Object : DrawProp.View.Mapping.values())  {
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
                    NewInInterface = (DrawProp.View.isInInterface(DrawProp.ToDraw)?2:0);
                if((CheckObject && !(CheckClass && NewInInterface==2)) || (CheckClass && NewInInterface==0 )) // && InInterface==2))
                    NewInInterface = (DrawProp.View.isInInterface(null)?1:0);

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

            if(!Read && (DataChanged && ChangedProps.contains(DrawProp.View.Property)))
                Read = true;

            if (!Read && DataChanged) {
                for (ObjectImplement object : DrawProp.View.Mapping.values()) {
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
                SelectProps.add(DrawProp,DrawProp.View.getSourceExpr(null,null,Session,false));

            Map<PropertyView,Object> ResultProps = SelectProps.executeSelect(Session).values().iterator().next();
            for(PropertyView DrawProp : PanelProps)
                Result.PanelProperties.put(DrawProp,ResultProps.get(DrawProp));
        }

        for(Entry<GroupObjectImplement, Collection<PropertyView>> MapGroup : GroupProps.entrySet()) {
            GroupObjectImplement Group = MapGroup.getKey();
            Collection<PropertyView> GroupList = MapGroup.getValue();

            JoinQuery<ObjectImplement,PropertyView> SelectProps = new JoinQuery<ObjectImplement,PropertyView>(Group);

            ViewTable KeyTable = BL.TableFactory.ViewTables.get(Group.size()-1);
            Join<KeyField,PropertyField> KeyJoin = new Join<KeyField,PropertyField>(KeyTable,true);

            ListIterator<KeyField> ikt = KeyTable.Objects.listIterator();
            for(ObjectImplement Object : Group)
                KeyJoin.Joins.put(ikt.next(),SelectProps.MapKeys.get(Object));
            KeyJoin.Joins.put(KeyTable.View,new ValueSourceExpr(getGroupObjectGID(Group),KeyTable.View.Type));
            SelectProps.add(KeyJoin);

            for(PropertyView DrawProp : GroupList)
                SelectProps.add(DrawProp,DrawProp.View.getSourceExpr(Group.GetClassGroup(),SelectProps.MapKeys,Session,false));

//            System.out.println(Group);
//            SelectProps.outSelect(Session);
            LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> ResultProps = SelectProps.executeSelect(Session);

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
        for(GroupObjectImplement Group : Groups) {
            Iterator<ObjectImplement> io = Group.iterator();
            while(io.hasNext()) io.next().Updated=0;
            Group.Updated = 0;
        }
        DataChanged = false;

//        Result.Out(this);

        return Result;
    }

    // возвращает какие объекты отчета фиксируются
    private Set<GroupObjectImplement> getReportObjects() {

        Set<GroupObjectImplement> reportObjects = new HashSet();
        for (GroupObjectImplement group : Groups) {
            if (group.gridClassView)
                reportObjects.add(group);
        }

        return reportObjects;
    }

    // считывает все данные (для отчета)
    private ReportData getReportData() throws SQLException {

        Set<GroupObjectImplement> ReportObjects = getReportObjects();

        Collection<ObjectImplement> ReadObjects = new ArrayList();
        for(GroupObjectImplement Group : ReportObjects)
            ReadObjects.addAll(Group);

        // пока сделаем тупо получаем один большой запрос

        OrderedJoinQuery<ObjectImplement,PropertyView> Query = new OrderedJoinQuery<ObjectImplement,PropertyView>(ReadObjects);

        for(GroupObjectImplement Group : Groups) {

            if (ReportObjects.contains(Group)) {

                // не фиксированные ключи
                Group.fillSourceSelect(Query,ReportObjects,BL.TableFactory,Session);

                // закинем Order'ы
                for(Map.Entry<PropertyObjectImplement,Boolean> Order : Group.Orders.entrySet())
                    Query.Orders.put(Order.getKey().getSourceExpr(ReportObjects,Query.MapKeys,Session,false),Order.getValue());

                for(ObjectImplement Object : Group)
                    Query.Orders.put(Object.getSourceExpr(ReportObjects,Query.MapKeys),false);
            }
        }

        ReportData Result = new ReportData();

        for(PropertyView Property : Properties) {
            Query.add(Property,Property.View.getSourceExpr(ReportObjects,Query.MapKeys,Session, false));

            Result.Properties.put(Property.ID,new HashMap());
        }

        LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> ResultSelect = Query.executeSelect(Session);

        for(Entry<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> Row : ResultSelect.entrySet()) {
            Map<Integer,Integer> GroupValue = new HashMap();
            for(GroupObjectImplement Group : Groups)
                for(ObjectImplement Object : Group)
                    GroupValue.put(Object.ID,Row.getKey().get(Object));

            Result.ReadOrder.add(GroupValue);

            for(PropertyView Property : Properties)
                Result.Properties.get(Property.ID).put(GroupValue,Row.getValue().get(Property));
        }

//        Result.Out();

        return Result;
    }

}

// поле для отрисовки отчета
class ReportDrawField {
    String ID;
    String Caption;
    java.lang.Class ValueClass;
    int Width;
    byte Alignment;

    ReportDrawField(String iID,String iCaption,Type DBType) {
        ID = iID;
        Caption = iCaption;
        DBType.fillReportDrawField(this);
    }

    int GetCaptionWidth() {
        return Caption.length()+3;
    }
}

// считанные данные (должен быть интерфейс Serialize)
class ReportData implements JRDataSource, Serializable {
    
    List<Map<Integer,Integer>> ReadOrder = new ArrayList();
    Map<Integer,Map<Map<Integer,Integer>,Object>> Properties = new HashMap();
    
    void Out() {
        for(Integer Object : ReadOrder.get(0).keySet())
            System.out.print("obj"+Object+" ");
        for(Integer Property : Properties.keySet())
            System.out.print("prop"+Property+" ");
        System.out.println();

        for(Map<Integer,Integer> Row : ReadOrder) {
            for(Integer Object : ReadOrder.get(0).keySet())
                System.out.print(Row.get(Object)+" ");
            for(Integer Property : Properties.keySet())
                System.out.print(Properties.get(Property).get(Row)+" ");
            System.out.println();
        }
    }

    int CurrentRow = -1;
    public boolean next() throws JRException {
        CurrentRow++;
        return CurrentRow<ReadOrder.size();
    }

    public Object getFieldValue(JRField jrField) throws JRException {
        
        String FieldName = jrField.getName();
        Object Value = null;
        if(FieldName.startsWith("obj"))
            Value = ReadOrder.get(CurrentRow).get(Integer.parseInt(FieldName.substring(3)));
        else
            Value = Properties.get(Integer.parseInt(FieldName.substring(4))).get(ReadOrder.get(CurrentRow));

        if(Value instanceof String)
            Value = ((String)Value).trim();
        
        if(Value==null) {

            try {
                return BaseUtils.getDefaultValue(java.lang.Class.forName(jrField.getValueClassName()));
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            } catch (ClassNotFoundException e) {
            }
        }
        
        return Value;
    }
}