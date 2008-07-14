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
        ListIterator<ObjectImplement> i = listIterator();
        while(i.hasNext()) {
            ObjectImplement Object = i.next();
            System.out.print(" "+Object.OutName+" = "+Value.get(Object));
        }                
    }
}

class PropertyObjectImplement extends PropertyImplement<ObjectImplement> {
    PropertyObjectImplement(Property iProperty) {super(iProperty);}
    
    GroupObjectImplement GetApplyObject() {
        GroupObjectImplement ApplyObject=null;
        Iterator<ObjectImplement> i = Mapping.values().iterator();
        while(i.hasNext()) {
            ObjectImplement IntObject = i.next();
            if(ApplyObject==null || IntObject.GroupTo.Order>ApplyObject.Order) ApplyObject = IntObject.GroupTo;
        }

        return ApplyObject;
    }
    
    Class GetValueClass(GroupObjectImplement ClassGroup) {
        Iterator<PropertyInterface> i = Property.Interfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            ObjectImplement IntObject = Mapping.get(Interface);
            Interface.ValueClass = (IntObject.GroupTo==ClassGroup?IntObject.GridClass:IntObject.Class);
        }
        
        return Property.GetValueClass();
    }

    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        return GetValueClass(ClassGroup)!=null;
    }

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        Iterator<ObjectImplement> i = Mapping.values().iterator();
        while(i.hasNext()) {
            ObjectImplement IntObject = i.next();
            if(IntObject.GroupTo!=ClassGroup && ((IntObject.Updated & (1<<0))!=0)) return true;
        }
        
        return false;
    }

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        Iterator<ObjectImplement> i = Mapping.values().iterator();
        while(i.hasNext()) {
            ObjectImplement IntObject = i.next();
            if(((IntObject.Updated & (1<<(IntObject.GroupTo==ClassGroup?3:1))))!=0) return true;
        }
        
        return false;
    }

    SourceExpr JoinSelect(JoinList Joins,GroupObjectImplement ClassGroup,Map<ObjectImplement,SourceExpr> ClassSource,boolean Left) {

        Collection<PropertyInterface> NullInterfaces = new ArrayList();
        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
        
        Iterator<PropertyInterface> i = Property.Interfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            ObjectImplement IntObject = Mapping.get(Interface);
            SourceExpr JoinExpr = (IntObject.GroupTo==ClassGroup?ClassSource.get(IntObject):new ValueSourceExpr(IntObject.idObject));
            if(JoinExpr==null) 
                NullInterfaces.add(Interface);
            else
                JoinImplement.put(Interface,JoinExpr);
        }

        SourceExpr Result = Property.JoinSelect(Joins,JoinImplement,Left);

        i = NullInterfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            ClassSource.put(Mapping.get(Interface),JoinImplement.get(Interface));
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
        Iterator<GroupObjectImplement> ig = bv.Groups.iterator();
        System.out.println(" ------- GROUPOBJECTS ---------------");
        while(ig.hasNext()) {
            GroupObjectImplement Group = ig.next();

            List<GroupObjectValue> GroupGridObjects = GridObjects.get(Group);
            if(GroupGridObjects!=null) {
                System.out.println(Group.GID+" - Grid Changes");
                ListIterator<GroupObjectValue> ir = GroupGridObjects.listIterator();
                while(ir.hasNext()) { 
                    Group.Out(ir.next());
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

        Iterator<PropertyView> ipv = null;
        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        ipv = GridProperties.keySet().iterator();
        while(ipv.hasNext()) {
            PropertyView Property = ipv.next();
            Map<GroupObjectValue,Object> PropertyValues = GridProperties.get(Property);
            Property.Out();
            System.out.println(" ---- property");
            Iterator<GroupObjectValue> igp = PropertyValues.keySet().iterator();
            while(igp.hasNext()) {
                GroupObjectValue gov = igp.next();
                Property.ToDraw.Out(gov);
                System.out.println(" - "+PropertyValues.get(gov));
            }                    
        }

        System.out.println(" ------- Panel ---------------");
        ipv = PanelProperties.keySet().iterator();
        while(ipv.hasNext()) {
            PropertyView Property = ipv.next();
            Property.Out();
            System.out.println(" - "+PanelProperties.get(Property));
        }
        
        System.out.println(" ------- Drop ---------------");
        ipv = DropProperties.iterator();
        while(ipv.hasNext()) {
            PropertyView Property = ipv.next();
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
    
    // для полиморфизма сюда
    void FillSelect(JoinList Joins, GroupObjectImplement ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource) {
        Property.JoinSelect(Joins,ClassGroup,ClassSource,false);
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

    FormBeanView(DataAdapter iAdapter,BusinessLogics iBL) {
        Adapter = iAdapter;
        BL = iBL;
        
        Groups = new ArrayList();
        Properties = new ArrayList();
        Filters = new HashSet();
        Orders = new ArrayList();
        InterfacePool = new HashMap();
        
        StructUpdated = true;
    }
    
    void AddGroup(GroupObjectImplement Group) { 
        Groups.add(Group);
        Group.Order = Groups.size();
        Iterator<ObjectImplement> i = Group.iterator();
        while(i.hasNext()) i.next().GroupTo = Group;
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
        Iterator<ObjectImplement> i = Group.iterator();
        while(i.hasNext()) {
            ObjectImplement Object = i.next();
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
    
    public FormChanges EndApply() throws SQLException {

        FormChanges Result = new FormChanges();

        // бежим по списку вниз
        Map<GroupObjectImplement,Set<Filter>> MapGroupFilters = null;
        Map<GroupObjectImplement,List<PropertyObjectImplement>> MapGroupOrders = null;
        if(StructUpdated) {
            // построим Map'ы
            // очистим старые 
            Iterator<GroupObjectImplement> igo = Groups.iterator();
            while(igo.hasNext()) {
                GroupObjectImplement Group = igo.next();
                Group.MapFilters = new HashSet();
                Group.MapOrders = new ArrayList();
            }
            // фильтры
            Iterator<Filter> ift = Filters.iterator();
            while(ift.hasNext()) {
                Filter Filt = ift.next();
                Filt.GetApplyObject().MapFilters.add(Filt);
            }
            // порядки
            ListIterator<PropertyObjectImplement> ior = Orders.listIterator();
            while(ior.hasNext()) {
                PropertyObjectImplement Order = ior.next();
                Order.GetApplyObject().MapOrders.add(Order);
            }
        }

        Iterator<Filter> iof = null;
        ListIterator<PropertyObjectImplement> ioo = null;
        ListIterator<GroupObjectImplement> ig = Groups.listIterator();
        while(ig.hasNext()) {
            GroupObjectImplement Group = ig.next();            
            
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
            iof = Group.MapFilters.iterator();
            while(iof.hasNext()) {
                Filter Filt = iof.next();
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(StructUpdated || Filt.ClassUpdated(Group))
                    UpdateKeys = (Filt.IsInInterface(Group)?Group.Filters.add(Filt):Group.Filters.remove(Filt)) || UpdateKeys;
            }
            // порядки
            boolean SetOrderChanged = false;
            ioo = Group.MapOrders.listIterator();
            Set<PropertyObjectImplement> SetOrders = new HashSet(Group.Orders);
            while(ioo.hasNext()) {
                PropertyObjectImplement Order = ioo.next();
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(StructUpdated || Order.ClassUpdated(Group))
                    SetOrderChanged = (Order.IsInInterface(Group)?SetOrders.add(Order):Group.Filters.remove(Order));
            }
            if(StructUpdated || SetOrderChanged) {
                // переформирываваем порядок, если структура или принадлежность Order'у изменилась
                ioo = Group.MapOrders.listIterator();
                List<PropertyObjectImplement> NewOrder = new ArrayList();
                while(ioo.hasNext()) {
                    PropertyObjectImplement Order = ioo.next();
                    if(SetOrders.contains(Order)) NewOrder.add(Order);
                }

                UpdateKeys = UpdateKeys || SetOrderChanged || !Group.Orders.equals(NewOrder);
                Group.Orders = NewOrder;
            }        

            if(!UpdateKeys) {
                // объекты задействованные в фильтре\порядке (по Filters\Orders верхних элементов GroupImplement'ов на флаг Updated - 0)
                iof = Group.Filters.iterator();
                while(!UpdateKeys && iof.hasNext())
                    if(iof.next().ObjectUpdated(Group)) UpdateKeys = true;
                ioo = Group.Orders.listIterator();
                while(!UpdateKeys && ioo.hasNext())
                    if(ioo.next().ObjectUpdated(Group)) UpdateKeys = true;
            }
            if(!UpdateKeys && Group.GridClassView) {
                Map<ObjectImplement,Integer> GroupKey = null;
                if((Group.Updated & ((1<<4)+(1<<0)))!=0) {
                    GroupKey = new HashMap();
                    Iterator<ObjectImplement> i = Group.iterator();
                    while(i.hasNext()) {
                        ObjectImplement Object = i.next();
                        GroupKey.put(Object,Object.idObject);
                    }
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

                iof = Group.Filters.iterator();
                while(iof.hasNext()) iof.next().FillSelect(JoinKeys,Group,KeySources);
                ioo = Group.Orders.listIterator();
                while(ioo.hasNext()) {
                    PropertyObjectImplement ToOrder = ioo.next();

                    SourceExpr OrderExpr = ToOrder.JoinSelect(JoinKeys,Group,KeySources,false);
                    SelectKeys.Orders.add(OrderExpr);
                    // надо закинуть их в запрос, а также установить фильтры на порядки чтобы
                    if(OrderValues!=null) {
                        OrderSources.add(OrderExpr);
                        OrderWheres.add(OrderValues.get(ToOrder));
                    }
                    
                    // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                    SelectKeys.Expressions.put("order"+((Integer)(ioo.nextIndex()-1)).toString(),OrderExpr);
                }

                // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
                ListIterator<ObjectImplement> igo = Group.listIterator();
                while(igo.hasNext()) {
                    ObjectImplement Object = igo.next();
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
                    SelectKeys.Expressions.put("key"+((Integer)(igo.nextIndex()-1)).toString(),KeyExpr);
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
                ListIterator<Map<String,Object>> ikr = KeyResult.listIterator();

                // параллельно будем обновлять ключи чтобы Join'ить
                ViewTable InsertTable = BL.TableFactory.ViewTables.get(Group.size()-1);
                InsertTable.DropViewID(Adapter, Group.GID);

                while(ikr.hasNext()) {
                    Map<String,Object> ResultRow = ikr.next();
                    GroupObjectValue KeyRow = new GroupObjectValue();
                    Map<PropertyObjectImplement,Object> OrderRow = new HashMap();
                    
                    // закинем сразу ключи для св-в чтобы Join'ить
                    Map<KeyField,Integer> ViewKeyInsert = new HashMap();
                    ViewKeyInsert.put(InsertTable.View,Group.GID);
                    ListIterator<KeyField> ivk = InsertTable.Objects.listIterator();
                    igo = Group.listIterator();
                    while(igo.hasNext()) {
                        ObjectImplement Object = igo.next();                    
                        Integer KeyValue = (Integer)ResultRow.get("key"+((Integer)(igo.nextIndex()-1)));
                        KeyRow.put(Object,KeyValue);
                        
                        ViewKeyInsert.put(ivk.next(), KeyValue);
                    }
                    Adapter.InsertRecord(InsertTable,ViewKeyInsert,new HashMap());

                    ioo = Group.Orders.listIterator();
                    while(ioo.hasNext()) {
                        PropertyObjectImplement ToOrder = ioo.next();
                        OrderRow.put(ToOrder,ResultRow.get("order"+((Integer)(ioo.nextIndex()-1))));
                    }
                    
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

        Iterator<PropertyView> ipv = Properties.iterator();
        while(ipv.hasNext()) {
            PropertyView DrawProp = ipv.next();
            
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

            Iterator<ObjectImplement> iov = DrawProp.View.Mapping.values().iterator();
            while(iov.hasNext())  {
                ObjectImplement Object = iov.next();
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
            ipv = PanelProps.iterator();
            while(ipv.hasNext()) {
                SelectFields++;
                PropertyView DrawProp = ipv.next();
                String SelectField = "prop"+SelectFields;
                SelectProps.Expressions.put(SelectField,DrawProp.View.JoinSelect(JoinProps,null,null,true));
                ToFields.put(DrawProp, SelectField);
            }
        
            ListIterator<From> isj = JoinProps.listIterator();
            while(isj.hasNext())
                SelectProps.From.Joins.add(isj.next());
        
            Map<String,Object> ResultProps = Adapter.ExecuteSelect(SelectProps).get(0);

            ipv = PanelProps.iterator();
            while(ipv.hasNext()) {
                PropertyView DrawProp = ipv.next();
                Result.PanelProperties.put(DrawProp,ResultProps.get(ToFields.get(DrawProp)));
            }
        }
        
        Iterator<GroupObjectImplement> igo = GroupProps.keySet().iterator();
        while(igo.hasNext()) {
            GroupObjectImplement Group = igo.next();
                                        
            Collection<PropertyView> GroupList = GroupProps.get(Group);
            JoinProps = new JoinList();
            
            ViewTable KeyTable = BL.TableFactory.ViewTables.get(Group.size());
            FromTable SelectKeyTable = new FromTable(KeyTable.Name);
            SelectProps = new SelectQuery(SelectKeyTable);

            ListIterator<ObjectImplement> lgo = Group.listIterator();
            ListIterator<KeyField> ikt = KeyTable.Objects.listIterator();
            
            Map<ObjectImplement,SourceExpr> MapKeys = new HashMap();
            while(lgo.hasNext()) {
                SourceExpr KeyExpr = new FieldSourceExpr(SelectKeyTable,ikt.next().Name);
                MapKeys.put(lgo.next(),KeyExpr);
                // также в запрос надо ключи закинуть
                SelectProps.Expressions.put("key"+(Integer)(lgo.nextIndex()-1),KeyExpr);
            }

            Integer SelectFields = 0;
            Map<PropertyView,String> ToFields = new HashMap();
            ipv = GroupList.iterator();
            while(ipv.hasNext()) {
                SelectFields++;
                PropertyView DrawProp = ipv.next();
                String SelectField = "prop"+SelectFields;
                SelectProps.Expressions.put(SelectField,DrawProp.View.JoinSelect(JoinProps,Group,MapKeys,true));
                ToFields.put(DrawProp, SelectField);
            }
        
            ListIterator<From> isj = JoinProps.listIterator();
            while(isj.hasNext())
                SelectProps.From.Joins.add(isj.next());
        
            List<Map<String,Object>> ResultProps = Adapter.ExecuteSelect(SelectProps);

            ipv = GroupList.iterator();
            while(ipv.hasNext()) {
                PropertyView DrawProp = ipv.next();

                Map<GroupObjectValue,Object> PropResult = new HashMap();
                Result.GridProperties.put(DrawProp,PropResult);

                ListIterator<Map<String,Object>> irp = ResultProps.listIterator();
                while(irp.hasNext()) {
                    Map<String,Object> ResultRow = irp.next();
                    GroupObjectValue ResultKeys = new GroupObjectValue();
                    lgo = Group.listIterator();
                    while(lgo.hasNext()) {
                        ObjectImplement ObjectImp = lgo.next();
                        ResultKeys.put(ObjectImp,(Integer)ResultRow.get("key"+(Integer)(lgo.nextIndex()-1)));
                    }

                    PropResult.put(ResultKeys,ResultRow.get(ToFields.get(DrawProp)));
                }
            }
        }   

        // сбрасываем все пометки
        StructUpdated = false;
        ig = Groups.listIterator();
        while(ig.hasNext()) {
            GroupObjectImplement Group = ig.next();
            Iterator<ObjectImplement> io = Group.iterator();
            while(io.hasNext()) io.next().Updated=0;
            Group.Updated = 0;
        }

        return Result;
    }
}
