/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.io.Serializable;

// здесь многие подходы для оптимизации неструктурные, то есть можно было структурно все обновлять но это очень медленно

import net.sf.jasperreports.engine.JRAlignment;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JasperDesign;


// на самом деле нужен collection но при extend'е нужна конкретная реализация
class ObjectImplement {

    ObjectImplement(int iID,Class iBaseClass) {
        ID = iID;
        BaseClass = iBaseClass;
        GridClass = BaseClass;
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

    // идентификатор (в рамках формы)
    int ID = 0;

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup,Map<ObjectImplement,SourceExpr> ClassSource) {
        return (ClassGroup!=null && ClassGroup.contains(GroupTo)?ClassSource.get(this):new ValueSourceExpr(idObject));
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

    public void addObject(ObjectImplement object) {
        add(object);
        object.GroupTo = this;
    }

    Integer Order = 0;

    // глобальный идентификатор чтобы писать во ViewTable
    int GID = 0;

    // классовый вид включен или нет
    Boolean GridClassView = true;

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

    int Updated = UPDATED_GRIDCLASS;

    int PageSize = 20;

    void Out(GroupObjectValue Value) {
        for(ObjectImplement Object : this)
            System.out.print(" "+Object.caption +" = "+Value.get(Object));
    }

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

class PropertyObjectImplement extends PropertyImplement<ObjectImplement> {
    PropertyObjectImplement(Property iProperty) {super(iProperty);}

    // получает Grid в котором рисоваться
    GroupObjectImplement GetApplyObject() {
        GroupObjectImplement ApplyObject=null;
        for(ObjectImplement IntObject : Mapping.values())
            if(ApplyObject==null || IntObject.GroupTo.Order>ApplyObject.Order) ApplyObject = IntObject.GroupTo;

        return ApplyObject;
    }

    // получает класс значения
    Class GetValueClass(GroupObjectImplement ClassGroup) {
        InterfaceClass ClassImplement = new InterfaceClass();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            ObjectImplement IntObject = Mapping.get(Interface);
            Class ImpClass = (IntObject.GroupTo==ClassGroup?IntObject.GridClass:IntObject.Class);
            if(ImpClass==null) return null;
            ClassImplement.put(Interface,ImpClass);
        }

        return Property.GetValueClass(ClassImplement);
    }

    // в интерфейсе
    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        return GetValueClass(ClassGroup)!=null;
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

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, boolean NotNull) {

        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();

        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces)
            JoinImplement.put(Interface,Mapping.get(Interface).getSourceExpr(ClassGroup,ClassSource));

        // если есть не все интерфейсы и есть изменения надо с Full Join'ить старое с новым
        // иначе как и было
        return Session.getSourceExpr(Property,JoinImplement,NotNull);
    }
}

// представление св-ва
class PropertyView {
    PropertyObjectImplement View;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    GroupObjectImplement ToDraw;

    PropertyView(int iID,PropertyObjectImplement iView,GroupObjectImplement iToDraw) {
        View = iView;
        ToDraw = iToDraw;
        ID = iID;
    }

    public PropertyView(PropertyView navigatorProperty) {

        ID = navigatorProperty.ID;
        View = navigatorProperty.View;
        ToDraw = navigatorProperty.ToDraw;
    }

    void Out() {
        System.out.print(View.Property.caption);
    }

    // идентификатор (в рамках формы)
    int ID = 0;
}

class AbstractFormChanges<T,V,Z> {

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
                System.out.println(Group.GID+" - Grid Changes");
                for(GroupObjectValue Value : GroupGridObjects) {
                    Group.Out(Value);
                    System.out.println("");
                }
            }

            GroupObjectValue Value = Objects.get(Group);
            if(Value!=null) {
                System.out.print(Group.GID+" - Object Changes ");
                Group.Out(Value);
                System.out.println("");
            }
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for(PropertyView Property : GridProperties.keySet()) {
            Map<GroupObjectValue,Object> PropertyValues = GridProperties.get(Property);
            Property.Out();
            System.out.println(" ---- property");
            for(GroupObjectValue gov : PropertyValues.keySet()) {
                Property.ToDraw.Out(gov);
                System.out.println(" - "+PropertyValues.get(gov));
            }
        }

        System.out.println(" ------- Panel ---------------");
        for(PropertyView Property : PanelProperties.keySet()) {
            Property.Out();
            System.out.println(" - "+PanelProperties.get(Property));
        }

        System.out.println(" ------- Drop ---------------");
        for(PropertyView Property : DropProperties) {
            Property.Out();
            System.out.println("");
        }
    }
}


class Filter {

    PropertyObjectImplement Property;
    ValueLink Value;
    int Compare;

    Filter(PropertyObjectImplement iProperty,int iCompare,ValueLink iValue) {
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
        Class ValueClass = Value.GetValueClass(ClassGroup);
        if(ValueClass==null)
            return Property.IsInInterface(ClassGroup);
        else
            return ValueClass.IsParent(Property.GetValueClass(ClassGroup));
    }

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        if(Property.ClassUpdated(ClassGroup)) return true;

        return Value.ClassUpdated(ClassGroup);
    }

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        if(Property.ObjectUpdated(ClassGroup)) return true;

        return Value.ObjectUpdated(ClassGroup);
    }

    void fillSelect(JoinQuery<ObjectImplement, ?> Query, Set<GroupObjectImplement> ClassGroup, DataSession Session) {
        Query.add(new FieldExprCompareWhere(Property.getSourceExpr(ClassGroup,Query.MapKeys,Session, true),Value.getValueExpr(ClassGroup,Query.MapKeys,Session,Property.Property.getDBType()),Compare));
    }
}

abstract class ValueLink {

    Class GetValueClass(GroupObjectImplement ClassGroup) {return null;}

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {return false;}

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {return false;}

    abstract SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, String DBType);
}


class UserValueLink extends ValueLink {

    Object Value;

    UserValueLink(Object iValue) {Value=iValue;}

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, String DBType) {
        return ValueSourceExpr.getExpr(Value,DBType);
    }
}

class ObjectValueLink extends ValueLink {

    ObjectValueLink(ObjectImplement iObject) {Object=iObject;}

    ObjectImplement Object;

    @Override
    Class GetValueClass(GroupObjectImplement ClassGroup) {
        return Object.Class;
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.Updated & ObjectImplement.UPDATED_CLASS)!=0);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.Updated & ObjectImplement.UPDATED_OBJECT)!=0);
    }

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, String DBType) {
        return Object.getSourceExpr(ClassGroup,ClassSource);
    }
}

class PropertyValueLink extends ValueLink {

    PropertyValueLink(PropertyObjectImplement iProperty) {Property=iProperty;}

    PropertyObjectImplement Property;

    @Override
    Class GetValueClass(GroupObjectImplement ClassGroup) {
        return Property.GetValueClass(ClassGroup);
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return Property.ClassUpdated(ClassGroup);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return Property.ObjectUpdated(ClassGroup);
    }

    SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, SourceExpr> ClassSource, DataSession Session, String DBType) {
        return Property.getSourceExpr(ClassGroup,ClassSource,Session, true);
    }
}


// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

class RemoteForm<T extends BusinessLogics<T>> implements PropertyUpdateView {

    private final int ID;
    int getID() { return ID; }

    T BL;

    DataSession Session;

    RemoteForm(int iID, T iBL, DataSession iSession) throws SQLException {

        ID = iID;

        BL = iBL;

        Session = iSession;

        StructUpdated = true;
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
        return ByteArraySerializer.serializeListClass(getObjectImplement(objectID).BaseClass.FindClassID(classID).Childs);
    }

    public int getPropertyClassID(int propertyID) {
        return getPropertyClass(propertyID).ID;
    }

    public byte[] getPropertyClassByteArray(int propertyID) {
        return ByteArraySerializer.serializeClass(getPropertyClass(getPropertyView(propertyID)));
    }

    // ----------------------------------- Навигация ----------------------------------------- //

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

    public void ChangeClassView(Integer groupID, Boolean show) throws SQLException {
        ChangeClassView(getGroupObjectImplement(groupID), show);
    }

    // Фильтры

    public void addFilter(byte[] state) {
        addFilter(ByteArraySerializer.deserializeFilter(state, this));
    }

    // Порядки

    public void ChangeOrder(int propertyID, int modiType) {
        ChangeOrder(getPropertyView(propertyID), modiType);
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    public void AddObject(int objectID, int classID) throws SQLException {
        ObjectImplement object = getObjectImplement(objectID);
        AddObject(object, (classID == -1) ? null : object.BaseClass.FindClassID(classID));
    }

    public void ChangeClass(int objectID, int classID) throws SQLException {

        ObjectImplement object = getObjectImplement(objectID);
        ChangeClass(object, (classID == -1) ? null : object.BaseClass.FindClassID(classID));
    }

    public boolean allowChangeProperty(int propertyID) {
        return allowChangeProperty(getPropertyView(propertyID).View);
    }

    public void ChangePropertyView(Integer propertyID, byte[] object) throws SQLException {
        ChangePropertyView(getPropertyView(propertyID), ByteArraySerializer.deserializeObjectValue(object));
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
            if (groupObject.GID == groupID)
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

    Class getPropertyClass(int propertyID) {
        return getPropertyClass(getPropertyView(propertyID));
    }

    Class getPropertyClass(PropertyView propertyView) {
        return propertyView.View.GetValueClass(propertyView.ToDraw);
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

        Class GridClass = BL.objectClass.FindClassID(idClass);
        if(Object.GridClass == GridClass) return;

        if(GridClass==null) throw new RuntimeException();
        Object.GridClass = GridClass;

        // расставляем пометки
        Object.Updated = Object.Updated | ObjectImplement.UPDATED_GRIDCLASS;
        Object.GroupTo.Updated = Object.GroupTo.Updated | GroupObjectImplement.UPDATED_GRIDCLASS;

    }

    private void ChangeClassView(GroupObjectImplement Group,Boolean Show) {

        if(Group.GridClassView == Show) return;
        Group.GridClassView = Show;

        // расставляем пометки
        Group.Updated = Group.Updated | GroupObjectImplement.UPDATED_CLASSVIEW;

    }

    // Фильтры

    // флаги изменения фильтров\порядков чисто для ускорения
    private boolean StructUpdated = true;
    // фильтры !null (св-во), св-во - св-во, св-во - объект, класс св-ва (для < > в том числе)?,

    public Set<Filter> fixedFilters = new HashSet();

    private Set<Filter> filters = new HashSet();

    public void clearFilter() {

        filters = new HashSet(fixedFilters);
        StructUpdated = true;
    }

    private void addFilter(Filter addFilter) {

        filters.add(addFilter);
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

        // берем все текущие CompareFilter на оператор 0(=) делаем ChangeProperty на ValueLink сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение ValueLink, GetValueExpr
        for(Filter Filter : Object.GroupTo.Filters) {
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

                SubQuery.add("newvalue",Filter.Value.getValueExpr(Object.GroupTo.GetClassGroup(),SubQuery.MapKeys,Session,Filter.Property.Property.getDBType()));

                LinkedHashMap<Map<ObjectImplement,Integer>,Map<String,Object>> Result = SubQuery.executeSelect(Session);
                // изменяем св-ва
                for(Entry<Map<ObjectImplement,Integer>,Map<String,Object>> Row : Result.entrySet()) {
                    Property ChangeProperty = Filter.Property.Property;
                    Map<PropertyInterface,ObjectValue> Keys = new HashMap();
                    for(PropertyInterface Interface : (Collection<PropertyInterface>)ChangeProperty.Interfaces) {
                        ObjectImplement ChangeObject = Filter.Property.Mapping.get(Interface);
                        Keys.put(Interface,new ObjectValue(Row.getKey().get(ChangeObject),ChangeObject.GridClass));
                    }
                    ChangeProperty.ChangeProperty(Keys,Row.getValue().get("newvalue"),Session);
                }
            }
        }

        ChangeObject(Object, AddID);

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

    private boolean allowChangeProperty(PropertyObjectImplement property) {
        return property.Property.allowChangeProperty(fillPropertyInterface(property));
    }

    private void ChangePropertyView(PropertyView Property,Object Value) throws SQLException {
        ChangeProperty(Property.View,Value);
    }

    private void ChangeProperty(PropertyObjectImplement property,Object value) throws SQLException {

        // изменяем св-во
        property.Property.ChangeProperty(fillPropertyInterface(property),value,Session);

        DataChanged = true;
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
            DropTable.DropViewID(Session, Group.GID);
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    private Map<PropertyInterface,ObjectValue> fillPropertyInterface(PropertyObjectImplement property) {

        Property changeProperty = property.Property;
        Map<PropertyInterface,ObjectValue> keys = new HashMap();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)changeProperty.Interfaces) {
            ObjectImplement object = property.Mapping.get(Interface);
            keys.put(Interface,new ObjectValue(object.idObject,object.Class));
        }

        return keys;
    }

    // рекурсия для генерации порядка
    private SourceWhere GenerateOrderWheres(List<SourceExpr> OrderSources,List<Object> OrderWheres,List<Boolean> OrderDirs,boolean Down,int Index) {

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
        SourceWhere OrderWhere = new FieldExprCompareWhere(OrderExpr,OrderValue,CompareIndex);

        if(!Last) {
            SourceWhere NextWhere = GenerateOrderWheres(OrderSources,OrderWheres,OrderDirs,Down,Index+1);

            // >A OR (=A AND >B)
            return new FieldOPWhere(OrderWhere,new FieldOPWhere(new FieldExprCompareWhere(OrderExpr,OrderValue,FieldExprCompareWhere.EQUALS),NextWhere,true),false);
        } else
            return OrderWhere;
    }

    public Collection<Property> getUpdateProperties() {

        Set<Property> Result = new HashSet();
        for(PropertyView PropView : Properties)
            Result.add(PropView.View.Property);
        return Result;
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
            for(Filter Filt : filters)
                Filt.GetApplyObject().MapFilters.add(Filt);

            // порядки
            for(PropertyView Order : Orders.keySet())
                Order.View.GetApplyObject().MapOrders.add(Order);
        }

        for(GroupObjectImplement Group : Groups) {
            // если изменились :
            // хоть один класс из этого GroupObjectImplement'a - (флаг Updated - 3)
            boolean UpdateKeys = ((Group.Updated & GroupObjectImplement.UPDATED_GRIDCLASS)!=0);
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
                    SetOrderChanged = (Order.View.IsInInterface(Group)?SetOrders.add(Order.View):Group.Orders.remove(Order));
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
            GroupObjectValue ObjectSeeks = Group.GetObjectValue();
            int Direction;
            boolean hasMoreKeys = true;

            if (ObjectSeeks.containsValue(null)) {
                ObjectSeeks = new GroupObjectValue();
                Direction = DIRECTION_DOWN;
            } else
                Direction = DIRECTION_CENTER;

            // один раз читаем не так часто делается, поэтому не будем как с фильтрами
            for(PropertyObjectImplement Property : UserPropertySeeks.keySet()) {
                if(Property.GetApplyObject()==Group) {
                    PropertySeeks.put(Property,UserPropertySeeks.get(Property));
                    UpdateKeys = true;
                    Direction = DIRECTION_CENTER;
                }
            }
            for(ObjectImplement Object : UserObjectSeeks.keySet()) {
                if(Object.GroupTo==Group) {
                    ObjectSeeks.put(Object,UserObjectSeeks.get(Object));
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

            if(!UpdateKeys && Group.GridClassView && (Group.Updated & GroupObjectImplement.UPDATED_OBJECT)!=0) {
                // листание - объекты стали близки к краю (idObject не далеко от края - надо хранить список не базу же дергать) - изменился объект
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
                    if(ObjectSeeks.get(Object)==null && Object.BaseClass instanceof IntegralClass && !Group.GridClassView)
                        ObjectSeeks.put(Object,1);

                // докидываем Join'ами (INNER) фильтры, порядки

                // уберем все некорректности в Seekах :
                // корректно если : PropertySeeks = Orders или (Orders.sublist(PropertySeeks.size) = PropertySeeks и ObjectSeeks - пустое)
                // если Orders.sublist(PropertySeeks.size) != PropertySeeks, тогда дочитываем ObjectSeeks полностью
                // выкидываем лишние PropertySeeks, дочитываем недостающие Orders в PropertySeeks
                // также если панель то тупо прочитаем объект
                boolean NotEnoughOrders = !(PropertySeeks.keySet().equals(Group.Orders.keySet()) || ((PropertySeeks.size()<Group.Orders.size() && (new HashSet((new ArrayList(Group.Orders.keySet())).subList(0,PropertySeeks.size()))).equals(PropertySeeks.keySet())) && ObjectSeeks.size()==0));
                if((NotEnoughOrders && ObjectSeeks.size()<Group.size()) || !Group.GridClassView) {
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
                }

                if(!Group.GridClassView) {
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
                    ViewTable InsertTable = BL.TableFactory.ViewTables.get(Group.size()-1);
                    InsertTable.DropViewID(Session, Group.GID);

                    for(Entry<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> ResultRow : KeyResult.entrySet()) {
                        GroupObjectValue KeyRow = new GroupObjectValue();
                        Map<PropertyObjectImplement,Object> OrderRow = new HashMap();

                        // закинем сразу ключи для св-в чтобы Join'ить
                        Map<KeyField,Integer> ViewKeyInsert = new HashMap();
                        ViewKeyInsert.put(InsertTable.View,Group.GID);
                        ListIterator<KeyField> ivk = InsertTable.Objects.listIterator();
                        // !!!! важно в Keys сохранить порядок ObjectSeeks
                        for(ObjectImplement ObjectKey : ObjectSeeks.keySet()) {
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
                    if (Group.Keys.contains(Group.GetObjectValue()))
                        ActiveRow = Group.Keys.indexOf(Group.GetObjectValue());

                    if(ActiveRow>=0) {
                        // нашли ряд его выбираем
                        Result.Objects.put(Group,Group.Keys.get(ActiveRow));
                        ChangeGroupObject(Group,Group.Keys.get(ActiveRow));
                    }
//                    else
//                        ChangeGroupObject(Group,new GroupObjectValue());
                }
            }
        }

        Collection<PropertyView> PanelProps = new ArrayList();
        Map<GroupObjectImplement,Collection<PropertyView>> GroupProps = new HashMap();

//        PanelProps.

        for(PropertyView DrawProp : Properties) {

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
                    if((Object.Updated & ObjectImplement.UPDATED_OBJECT)!=0 && InInterface!=2) {
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
                    NewInInterface = (DrawProp.View.IsInInterface(DrawProp.ToDraw)?2:0);
                if((CheckObject && !(CheckClass && NewInInterface==2)) || (CheckClass && NewInInterface==0 && InInterface==2))
                    NewInInterface = (DrawProp.View.IsInInterface(null)?1:0);

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
                if(InInterface==2 && DrawProp.ToDraw.GridClassView) {
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
            KeyJoin.Joins.put(KeyTable.View,new ValueSourceExpr(Group.GID));
            SelectProps.add(KeyJoin);

            for(PropertyView DrawProp : GroupList)
                SelectProps.add(DrawProp,DrawProp.View.getSourceExpr(Group.GetClassGroup(),SelectProps.MapKeys,Session,false));

//            System.out.println(Group.iterator().next().caption);
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
            if (group.GridClassView)
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

    ReportDrawField(String iID,String iCaption,String DBType) {
        ID = iID;
        Caption = iCaption;
        if(DBType.equals("integer")) {
            ValueClass = java.lang.Integer.class;
            Width = 7;
            Alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
        } else {
            ValueClass = java.lang.String.class;
            Width = 40;
            Alignment = JRAlignment.HORIZONTAL_ALIGN_LEFT;
        }
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
             if(jrField.getValueClassName().equals(Integer.class.getName()))
                 return 0;
             else 
                 return "";
        }
        
        return Value;
    }
}