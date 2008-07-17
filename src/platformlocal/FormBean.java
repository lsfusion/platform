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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

// здесь многие подходы для оптимизации неструктурные, то есть можно было структурно все обновлять но это очень медленно


// на самом деле нужен collection но при extend'е нужна конкретная реализация
class ObjectImplement {

    // выбранный объект, класс выбранного объекта
    Integer idObject = null;
    Class Class = null;

    // выбранный класс
    Class GridClass = null;

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид
    int Updated = 0;

    GroupObjectImplement GroupTo;

    // чисто для отладки
    String OutName = "";
}

class GroupObjectMap<T> extends HashMap<ObjectImplement,T> {
    
}

class GroupObjectValue extends GroupObjectMap<Integer> {
}

class GroupObjectImplement extends ArrayList<ObjectImplement> {

    GroupObjectImplement() {
        MapFilters = new HashSet();
        MapOrders = new ArrayList();
        Filters = new HashSet();
        Orders = new ArrayList();
    }

    Integer Order = 0;

    // глобальный идентификатор чтобы писать во ViewTable
    Integer GID = 0;

    // классовый вид включен или нет
    Boolean GridClassView = true;

    // закэшированные
    
    // вообще все фильтры
    Set<Filter> MapFilters;
    List<PropertyObjectImplement> MapOrders;
    
    // с активным интерфейсом
    Set<Filter> Filters;
    List<PropertyObjectImplement> Orders;
    
    boolean UpKeys, DownKeys;
    List<GroupObjectValue> Keys = null;
    // какие ключи активны
    Map<GroupObjectValue,Map<PropertyObjectImplement,Object>> KeyOrders = null;

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид
    int Updated = 0;

    int PageSize = 5;
    
    void Out(GroupObjectValue Value) {
        for(ObjectImplement Object : this)
            System.out.print(" "+Object.OutName+" = "+Value.get(Object));
    }
}

class PropertyObjectImplement extends PropertyImplement<ObjectProperty,ObjectImplement> {
    PropertyObjectImplement(ObjectProperty iProperty) {super(iProperty);}
    
    GroupObjectImplement GetApplyObject() {
        GroupObjectImplement ApplyObject=null;
        for(ObjectImplement IntObject : Mapping.values())
            if(ApplyObject==null || IntObject.GroupTo.Order>ApplyObject.Order) ApplyObject = IntObject.GroupTo;

        return ApplyObject;
    }
    
    Class GetValueClass(GroupObjectImplement ClassGroup) {
        InterfaceClass ClassImplement = new InterfaceClass();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            ObjectImplement IntObject = Mapping.get(Interface);
            ClassImplement.put(Interface,(IntObject.GroupTo==ClassGroup?IntObject.GridClass:IntObject.Class));
        }

        return Property.GetValueClass(ClassImplement);
    }

    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        return GetValueClass(ClassGroup)!=null;
    }

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : Mapping.values())
            if(IntObject.GroupTo!=ClassGroup && ((IntObject.Updated & (1<<0))!=0)) return true;
        
        return false;
    }

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : Mapping.values())
            if(((IntObject.Updated & (1<<(IntObject.GroupTo==ClassGroup?3:1))))!=0) return true;
        
        return false;
    }

    SourceExpr JoinSelect(JoinList Joins,GroupObjectImplement ClassGroup,Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps,boolean Left) {

        Collection<PropertyInterface> NullInterfaces = new ArrayList();
        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
        
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            ObjectImplement IntObject = Mapping.get(Interface);
            SourceExpr JoinExpr = (IntObject.GroupTo==ClassGroup?ClassSource.get(IntObject):new ValueSourceExpr(IntObject.idObject));
            if(JoinExpr==null) 
                NullInterfaces.add(Interface);
            else
                JoinImplement.put(Interface,JoinExpr);
        }

        SourceExpr Result = Property.JoinSelect(Joins,JoinImplement,Left);

        for(PropertyInterface Interface : NullInterfaces)
            ClassSource.put(Mapping.get(Interface),JoinImplement.get(Interface));
        
        // есди св-во изменилось закинем сюда 
        if(ChangedProps.contains(Property)) {
            SourceExpr NewResult = Property.ChangedJoinSelect(Joins,JoinImplement,Session,0,true);
            Result = new IsNullSourceExpr(NewResult,Result);
        }
        
        return Result;
    }
}

// представление св-ва
class PropertyView {
    PropertyObjectImplement View;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    GroupObjectImplement ToDraw;
    
    PropertyView(PropertyObjectImplement iView,GroupObjectImplement iToDraw) {
        View = iView;
        ToDraw = iToDraw;
    }
    
    void Out() {
        System.out.print(View.Property.OutName);
    }            
}

class AbstractFormChanges<T,V,Z> {

    AbstractFormChanges() {
        Objects = new HashMap();
        GridObjects = new HashMap();
        GridProperties = new HashMap();
        PanelProperties = new HashMap();
        DropProperties = new HashSet();
    }

    Map<T,V> Objects;
    Map<T,List<V>> GridObjects;
    Map<Z,Map<V,Object>> GridProperties;
    Map<Z,Object> PanelProperties;
    Set<Z> DropProperties;
}

// класс в котором лежит какие изменения произошли
// появляется по сути для отделения клиента, именно он возвращается назад клиенту
class FormChanges extends AbstractFormChanges<GroupObjectImplement,GroupObjectValue,PropertyView> {
   
    void Out(FormBeanView bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for(GroupObjectImplement Group : bv.Groups) {
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
    
    Filter(PropertyObjectImplement iProperty) {Property=iProperty;}
    PropertyObjectImplement Property;

    GroupObjectImplement GetApplyObject() {
        return Property.GetApplyObject();
    }
    
    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        return Property.IsInInterface(ClassGroup);
    }

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return Property.ClassUpdated(ClassGroup);
    }

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return Property.ObjectUpdated(ClassGroup);
    }
    
    boolean DataUpdated(Set<ObjectProperty> ChangedProps) {
        return ChangedProps.contains(Property);
    }
    
    // для полиморфизма сюда
    void FillSelect(JoinList Joins, GroupObjectImplement ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps) {
        Property.JoinSelect(Joins,ClassGroup,ClassSource,Session,ChangedProps,false);
    }
}

class NotNullFilter extends Filter {
    
    NotNullFilter(PropertyObjectImplement iProperty) {
        super(iProperty);
    }
}

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

class FormBeanView {

    ChangesSession Session;
    
    Set<ObjectProperty> ChangedProps;
    
    FormBeanView(DataAdapter iAdapter,BusinessLogics iBL) {
        Adapter = iAdapter;
        BL = iBL;
        
        Groups = new ArrayList();
        Properties = new ArrayList();
        Filters = new HashSet();
        Orders = new ArrayList();
        InterfacePool = new HashMap();

        Session = BL.CreateSession();
        ChangedProps = new HashSet();

        StructUpdated = true;
    }
    
    void AddGroup(GroupObjectImplement Group) { 
        Groups.add(Group);
        Group.Order = Groups.size();
        for(ObjectImplement Object : Group) Object.GroupTo = Group;
    }

    List<GroupObjectImplement> Groups;
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    List<PropertyView> Properties;
    
    // Set чтобы сравнивать удобнее было бы
    Set<Filter> Filters;
    List<PropertyObjectImplement> Orders;
    
    // карта что сейчас в интерфейсе + карта в классовый\объектный вид
    Map<PropertyView,Boolean> InterfacePool;    

    BusinessLogics BL;
    DataAdapter Adapter;

    // это будут Bean'овские интерфейсы
    public void ChangeObject(GroupObjectImplement Group,GroupObjectValue Value) throws SQLException {    
        // проставим все объектам метки изменений
        for(ObjectImplement Object : Group) {
            Integer idObject = Value.get(Object);
            if(Object.idObject!=idObject) {
                // запишем объект
                Object.idObject = idObject;
                Object.Updated = Object.Updated | (1<<0);

                // запишем класс объекта
                Class ObjectClass = BL.GetClass(Adapter, idObject);
                if(Object.Class!=ObjectClass) {
                    Object.Class = ObjectClass;
                    Object.Updated = Object.Updated | (1<<1);
                }
                
                Group.Updated = Group.Updated | (1<<0);
            }
        }
    }
    public void ChangeClass(ObjectImplement Object,Integer idClass) throws SQLException {    
        Class GridClass = BL.BaseClass.FindClassID(idClass);
        if(Object.GridClass==GridClass) return;
        Object.GridClass = GridClass;
        Object.Updated = Object.Updated | (1<<3);
        // помечаем что в группе изменися класс одного из объектов
        Object.GroupTo.Updated = Object.GroupTo.Updated | (1<<3);
    }
    
    public void ChangeClassView(GroupObjectImplement Group,Boolean Show) {
        if(Group.GridClassView==Show) return;
        Group.GridClassView = Show;
        Group.Updated = Group.Updated | (1<<4);
    }
    
    // флаги изменения фильтров\порядков чисто для ускорения
    boolean StructUpdated = false;
    // фильтры !null (св-во), св-во - св-во, св-во - объект, класс св-ва (для < > в том числе)?, 
    public void AddFilter(Filter AddFilter) {
        Filters.add(AddFilter);

        StructUpdated = true;
    }
    
    // собсно скорее все-таки будет PropertyView передаваться
    public void AddOrder(PropertyObjectImplement Property) {
        Orders.add(Property);
        
        StructUpdated = true;
    }
    
    // пометка что изменилось св-во
    boolean DataChanged = false;
    
    public void ChangeProperty(PropertyObjectImplement Property,Object Value) throws SQLException {
        
        if(Property.Property instanceof DataProperty) {
            DataProperty DataProperty = (DataProperty)Property.Property;
            Map<DataPropertyInterface,Integer> Keys = new HashMap();
            for(DataPropertyInterface Interface : DataProperty.Interfaces)
                Keys.put(Interface,Property.Mapping.get(Interface).idObject);

            // изменяем св-во
            DataProperty.ChangeProperty(Adapter,Keys,Value,Session);
            
            DataChanged = true;
        }
    }
    
    public void AddObject(ObjectImplement Object) throws SQLException {
        // пока тупо в базу 
        Object.GridClass.AddObject(Adapter,BL.TableFactory);
    }   

    // рекурсия для генерации порядка
    Where GenerateOrderWheres(List<SourceExpr> OrderSources,List<Object> OrderWheres,boolean More,int Index) {
        
        SourceExpr OrderExpr = OrderSources.get(Index);
        Object OrderValue = OrderWheres.get(Index);
        Where OrderWhere = new FieldExprCompareWhere(OrderExpr,OrderValue,More?1:2);
        if(Index+1<OrderSources.size()) {
            Where NextWhere = GenerateOrderWheres(OrderSources,OrderWheres,More,Index+1);
            
            // >A OR (=A AND >B)
            return new FieldOPWhere(OrderWhere,new FieldOPWhere(new FieldExprCompareWhere(OrderExpr,OrderValue,0),NextWhere,true),false);
        } else
            return OrderWhere;
    }

    // применяет изменения
    public void UpdateChanges(boolean Cancel) throws SQLException {

        if(!Cancel)
            BL.Apply(Adapter,Session);

        Session = BL.CreateSession();
    }
    
    // получаем все аггрегированные св-ва задействованные на форме
    Set<AggregateProperty> GetAggrProperties() {

        Set<AggregateProperty> Result = new HashSet();
        for(PropertyView PropView : Properties)
            if(PropView.View.Property instanceof AggregateProperty)
                Result.add((AggregateProperty)PropView.View.Property);
        return Result;
    }

    
    public FormChanges EndApply() throws SQLException {

        FormChanges Result = new FormChanges();

        // если изменились данные, применяем изменения
        if(DataChanged) {
            ChangedProps.addAll(BL.UpdateAggregations(Adapter,GetAggrProperties(),Session));
            ChangedProps.addAll(Session.Properties);
        }

        // бежим по списку вниз
        Map<GroupObjectImplement,Set<Filter>> MapGroupFilters = null;
        Map<GroupObjectImplement,List<PropertyObjectImplement>> MapGroupOrders = null;
        if(StructUpdated) {
            // построим Map'ы
            // очистим старые 
            for(GroupObjectImplement Group : Groups) {
                Group.MapFilters = new HashSet();
                Group.MapOrders = new ArrayList();
            }
            // фильтры
            for(Filter Filt : Filters)
                Filt.GetApplyObject().MapFilters.add(Filt);
            
            // порядки
            for(PropertyObjectImplement Order : Orders)
                Order.GetApplyObject().MapOrders.add(Order);
        }

        for(GroupObjectImplement Group : Groups) {
            // флаг меняется ли объект (по сути делать GO TOP)
            boolean GoTop = true;
            
            // по возврастанию, убыванию и откуда начинать
            boolean DescOrder = false;
            Map<ObjectImplement,Integer> KeyOrder = null;

            // если изменились :
            // хоть один класс из этого GroupObjectImplement'a - (флаг Updated - 3), изменился "классовый" вид с false на true - (флаг Updated - 4)
            boolean UpdateKeys = ((Group.Updated & (1<<3))!=0);
            // фильтр\порядок (надо сначала определить что в интерфейсе (верхних объектов Group и класса этого Group) в нем затем сравнить с теми что были до) - (Filters, Orders объектов)
            // фильтры
            for(Filter Filt : Group.MapFilters) {
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(StructUpdated || Filt.ClassUpdated(Group))
                    UpdateKeys = (Filt.IsInInterface(Group)?Group.Filters.add(Filt):Group.Filters.remove(Filt)) || UpdateKeys;
            }
            // порядки
            boolean SetOrderChanged = false;
            Set<PropertyObjectImplement> SetOrders = new HashSet(Group.Orders);
            for(PropertyObjectImplement Order : Group.MapOrders) {
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(StructUpdated || Order.ClassUpdated(Group))
                    SetOrderChanged = (Order.IsInInterface(Group)?SetOrders.add(Order):Group.Filters.remove(Order));
            }
            if(StructUpdated || SetOrderChanged) {
                // переформирываваем порядок, если структура или принадлежность Order'у изменилась
                List<PropertyObjectImplement> NewOrder = new ArrayList();
                for(PropertyObjectImplement Order : Group.MapOrders)
                    if(SetOrders.contains(Order)) NewOrder.add(Order);

                UpdateKeys = UpdateKeys || SetOrderChanged || !Group.Orders.equals(NewOrder);
                Group.Orders = NewOrder;
            }        

            // объекты задействованные в фильтре\порядке (по Filters\Orders верхних элементов GroupImplement'ов на флаг Updated - 0)
            if(!UpdateKeys)
                for(Filter Filt : Group.Filters)
                    if(Filt.ObjectUpdated(Group)) {UpdateKeys = true; break;}
            if(!UpdateKeys)
                for(PropertyObjectImplement Order : Group.Orders)
                    if(Order.ObjectUpdated(Group)) {UpdateKeys = true; break;}
            // проверим на изменение данных
            if(!UpdateKeys)
                for(Filter Filt : Group.Filters)
                    if(Filt.DataUpdated(ChangedProps)) {UpdateKeys = true; break;}
            if(!UpdateKeys)
                for(PropertyObjectImplement Order : Group.Orders)
                    if(ChangedProps.contains(Order.Property)) {UpdateKeys = true; break;}

            if(!UpdateKeys && Group.GridClassView) {
                Map<ObjectImplement,Integer> GroupKey = null;
                if((Group.Updated & ((1<<4)+(1<<0)))!=0) {
                    GroupKey = new HashMap();
                    for(ObjectImplement Object : Group)
                        GroupKey.put(Object,Object.idObject);
                }

                if((Group.Updated & (1<<4))!=0) {
                    // изменился "классовый" вид
                    KeyOrder = GroupKey;
                    UpdateKeys = true;
                    GoTop = false;
                } else {
                if((Group.Updated & (1<<0))!=0) {
                    // объекты стали близки к краю (idObject не далеко от края - надо хранить список не базу же дергать) - изменился объект
                    int KeyNum = Group.Keys.indexOf(GroupKey);
                    // если меньше PageSize осталось и сверху есть ключи
                    if(KeyNum<Group.PageSize && Group.UpKeys) {
                        DescOrder = true;
                        UpdateKeys = true;
                        GoTop = false;
                    
                        if (Group.PageSize*2<Group.Keys.size())
                            KeyOrder = Group.Keys.get(Group.PageSize*2);
                    } else {
                    // наоборот вниз
                    if(KeyNum>Group.Keys.size()-Group.PageSize && Group.DownKeys) {
                        DescOrder = false;
                        UpdateKeys = true;
                        GoTop = false;

                        if (Group.Keys.size()-2*Group.PageSize>=0)
                            KeyOrder = Group.Keys.get(Group.Keys.size()-2*Group.PageSize);
                    }
                    }
                }
                }
            }
            
            if(UpdateKeys) {
                // --- перечитываем источник (если "классовый" вид - 50, + помечаем изменения GridObjects, иначе TOP 1
                // добавим новые
                OrderedSelectQuery SelectKeys = new OrderedSelectQuery(null);

                // докидываем Join'ами (INNER) фильтры, порядки
                JoinList JoinKeys = new JoinList();
                
                // складываются источники и значения
                List<SourceExpr> OrderSources = new ArrayList();
                List<Object> OrderWheres = new ArrayList();
                
                Map<ObjectImplement,SourceExpr> KeySources = new HashMap();

                Map<PropertyObjectImplement,Object> OrderValues = null;
                if(KeyOrder!=null) OrderValues = Group.KeyOrders.get(KeyOrder);

                for(Filter Filt : Group.Filters) Filt.FillSelect(JoinKeys,Group,KeySources,Session,ChangedProps);
                int OrderNum = 0;
                for(PropertyObjectImplement ToOrder : Group.Orders) {
                    SourceExpr OrderExpr = ToOrder.JoinSelect(JoinKeys,Group,KeySources,Session,ChangedProps,false);
                    SelectKeys.Orders.add(OrderExpr);
                    // надо закинуть их в запрос, а также установить фильтры на порядки чтобы
                    if(OrderValues!=null) {
                        OrderSources.add(OrderExpr);
                        OrderWheres.add(OrderValues.get(ToOrder));
                    }
                    
                    // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                    SelectKeys.Expressions.put("order"+(OrderNum++),OrderExpr);
                }

                // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
                int ObjectNum = 0;
                for(ObjectImplement Object : Group) {
                    SourceExpr KeyExpr = KeySources.get(Object);
                    FromTable KeySelectTable = null;
                    if(KeyExpr==null) {
                        KeySelectTable = BL.TableFactory.ObjectTable.ClassSelect(Object.GridClass);
                        KeyExpr = new FieldSourceExpr(KeySelectTable,BL.TableFactory.ObjectTable.Key.Name);
                        
                        // вставляем слева (если справа то null'ы кидает при ORDER BY)
                        JoinKeys.add(0,KeySelectTable);
                        if(JoinKeys.size()>1) JoinKeys.get(1).JoinType = "FULL";
                    } else {
                        KeySelectTable = BL.TableFactory.ObjectTable.ClassJoinSelect(Object.GridClass,KeyExpr);
                        JoinKeys.add(KeySelectTable);
                    }

                    // также закинем их в порядок и в запрос
                    SelectKeys.Orders.add(KeyExpr);
                    if(KeyOrder!=null) {
                        OrderSources.add(KeyExpr);
                        OrderWheres.add(KeyOrder.get(Object));
                    }
                    
                    // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                    SelectKeys.Expressions.put("key"+(ObjectNum++),KeyExpr);
                }
                
                // закидываем в Select все таблицы (с JOIN'ами по умодчанию)                ''
                ListIterator<From> ijk = JoinKeys.listIterator();
                SelectKeys.From = ijk.next();
                while(ijk.hasNext()) SelectKeys.From.Joins.add(ijk.next());

                if(OrderSources.size()>0) {
                    Where OrderWhere = GenerateOrderWheres(OrderSources,OrderWheres,!DescOrder,0);
                    SelectKeys.From.Wheres.add(OrderWhere);
                }
                
                SelectKeys.Descending = DescOrder;
                SelectKeys.Top = (Group.GridClassView?Group.PageSize*3:1);
                
                // выполняем запрос
                List<Map<String,Object>> KeyResult = Adapter.ExecuteSelect(SelectKeys);
                                
                // нужно заполнить UpKeys, DownKeys, Keys, KeyOrders
                if(DescOrder) {
                    Group.DownKeys = (KeyOrder!=null);
                    Group.UpKeys = (KeyResult.size()==SelectKeys.Top);
                } else {
                    Group.UpKeys = (KeyOrder!=null);
                    Group.DownKeys = (KeyResult.size()==SelectKeys.Top);
                }
                Group.Keys = new ArrayList();
                Group.KeyOrders = new HashMap();

                // параллельно будем обновлять ключи чтобы Join'ить
                ViewTable InsertTable = BL.TableFactory.ViewTables.get(Group.size()-1);
                InsertTable.DropViewID(Adapter, Group.GID);

                ListIterator<Map<String,Object>> ikr = KeyResult.listIterator();
                for(Map<String,Object> ResultRow : KeyResult) {
                    GroupObjectValue KeyRow = new GroupObjectValue();
                    Map<PropertyObjectImplement,Object> OrderRow = new HashMap();
                    
                    // закинем сразу ключи для св-в чтобы Join'ить
                    Map<KeyField,Integer> ViewKeyInsert = new HashMap();
                    ViewKeyInsert.put(InsertTable.View,Group.GID);
                    ListIterator<KeyField> ivk = InsertTable.Objects.listIterator();
                    ObjectNum = 0;
                    for(ObjectImplement Object : Group) {
                        Integer KeyValue = (Integer)ResultRow.get("key"+(ObjectNum++));
                        KeyRow.put(Object,KeyValue);
                        
                        ViewKeyInsert.put(ivk.next(), KeyValue);
                    }
                    Adapter.InsertRecord(InsertTable,ViewKeyInsert,new HashMap());

                    OrderNum = 0;
                    for(PropertyObjectImplement ToOrder : Group.Orders)
                        OrderRow.put(ToOrder,ResultRow.get("order"+(OrderNum++)));
                    
                    Group.Keys.add(KeyRow);
                    Group.KeyOrders.put(KeyRow, OrderRow);
                }

                Result.GridObjects.put(Group,Group.Keys);

                Group.Updated = (Group.Updated | (1<<2));
                
                // --- помечаем изменение объекта - если не скроллинг или чистое изменение вида
                if(GoTop) {
                    Result.Objects.put(Group, Group.Keys.get(0));
                    ChangeObject(Group,Group.Keys.get(0));
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
                Read = ((DrawProp.ToDraw.Updated & ((1<<2)+(1<<4)))!=0);
                Boolean PrevPool = InterfacePool.get(DrawProp);
                InInterface = (PrevPool==null?0:(PrevPool?2:1));
            }

            for(ObjectImplement Object : DrawProp.View.Mapping.values())  {
                if(Object.GroupTo!=DrawProp.ToDraw) {
                    // "верхние" объекты интересует только изменение объектов\классов
                    if((Object.Updated & 1<<0)!=0) {
                        // изменился верхний объект, перечитываем
                        Read = true;
                        if(((Object.Updated)& 1<<1)!=0) {
                            // изменился класс объекта перепроверяем все
                            if(DrawProp.ToDraw!=null) CheckClass = true;
                            CheckObject = true;
                        }
                    }
                } else {
                    // изменился объект и св-во не было классовым
                    if((Object.Updated & 1<<0)!=0 && InInterface!=2) {
                        Read = true;
                        // изменися класс объекта
                        if(((Object.Updated)& 1<<1)!=0) CheckObject = true;
                    }
                    // изменение общего класса
                    if((Object.Updated & 1<<3)!=0) CheckClass = true;
                }
            }

            // обновим InterfacePool, было в InInterface
            if(CheckClass || CheckObject) {
                int NewInInterface=0;
                if(CheckClass)
                    NewInInterface = (DrawProp.View.IsInInterface(DrawProp.ToDraw)?2:0);
                if(CheckObject && !(CheckClass && NewInInterface==2))
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
        JoinList JoinProps = null;
        SelectQuery SelectProps = null;
        
        // сначала PanelProps
        if(PanelProps.size()>0) {
            JoinProps = new JoinList();
            SelectProps = new SelectQuery(new FromTable("dumb"));

            Integer SelectFields = 0;
            Map<PropertyView,String> ToFields = new HashMap();
            for(PropertyView DrawProp : PanelProps) {
                SelectFields++;
                String SelectField = "prop"+SelectFields;
                SelectProps.Expressions.put(SelectField,DrawProp.View.JoinSelect(JoinProps,null,null,Session,ChangedProps,true));
                ToFields.put(DrawProp, SelectField);
            }
        
            for(From Join : JoinProps)
                SelectProps.From.Joins.add(Join);
        
            Map<String,Object> ResultProps = Adapter.ExecuteSelect(SelectProps).get(0);

            for(PropertyView DrawProp : PanelProps)
                Result.PanelProperties.put(DrawProp,ResultProps.get(ToFields.get(DrawProp)));
        }
        
        for(GroupObjectImplement Group : GroupProps.keySet()) {
            Collection<PropertyView> GroupList = GroupProps.get(Group);
            JoinProps = new JoinList();
            
            ViewTable KeyTable = BL.TableFactory.ViewTables.get(Group.size()-1);
            FromTable SelectKeyTable = new FromTable(KeyTable.Name);
            SelectProps = new SelectQuery(SelectKeyTable);

            Map<ObjectImplement,SourceExpr> MapKeys = new HashMap();

            int ObjectNum = 0;
            ListIterator<KeyField> ikt = KeyTable.Objects.listIterator();
            for(ObjectImplement Object : Group) {
                SourceExpr KeyExpr = new FieldSourceExpr(SelectKeyTable,ikt.next().Name);
                MapKeys.put(Object,KeyExpr);
                // также в запрос надо ключи закинуть
                SelectProps.Expressions.put("key"+(ObjectNum++),KeyExpr);
            }

            Integer SelectFields = 0;
            Map<PropertyView,String> ToFields = new HashMap();
            for(PropertyView DrawProp : GroupList) {
                SelectFields++;
                String SelectField = "prop"+SelectFields;
                SelectProps.Expressions.put(SelectField,DrawProp.View.JoinSelect(JoinProps,Group,MapKeys,Session,ChangedProps,true));
                ToFields.put(DrawProp, SelectField);
            }
        
            for(From Join : JoinProps) SelectProps.From.Joins.add(Join);
        
            List<Map<String,Object>> ResultProps = Adapter.ExecuteSelect(SelectProps);

            for(PropertyView DrawProp : GroupList) {
                Map<GroupObjectValue,Object> PropResult = new HashMap();
                Result.GridProperties.put(DrawProp,PropResult);

                for(Map<String,Object> ResultRow : ResultProps) {
                    GroupObjectValue ResultKeys = new GroupObjectValue();
                    ObjectNum = 0;
                    for(ObjectImplement ObjectImp : Group)
                        ResultKeys.put(ObjectImp,(Integer)ResultRow.get("key"+(ObjectNum++)));

                    PropResult.put(ResultKeys,ResultRow.get(ToFields.get(DrawProp)));
                }
            }
        }   

        // сбрасываем все пометки
        StructUpdated = false;
        for(GroupObjectImplement Group : Groups) {
            Iterator<ObjectImplement> io = Group.iterator();
            while(io.hasNext()) io.next().Updated=0;
            Group.Updated = 0;
        }
        DataChanged = false;

        return Result;
    }
}
