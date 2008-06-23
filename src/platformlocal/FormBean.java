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

class ObjectImplement {
    
    // выбранный объект, класс выбранного объекта
    Integer idObject = null;
    Class Class = null;
    
    // выбранный класс
    Class GridClass = null;
    
    // 0 !!! - изменился объект, 1 - класс объекта, 2 - отбор, 3 - класса, 4 - классовый вид
    int Updated = 0;
    
    GroupObjectImplement GroupTo;
}

// на самом деле нужен collection но при extend'е нужна конкретная реализация
class GroupObjectImplement extends ArrayList<ObjectImplement> {

    Integer Order = 0;
    
    // глобальный идентификатор чтобы писать во ViewTable
    Integer GID = 0;

    // классовый вид включен или нет
    Boolean GridClassView = true;

    // закэшированные
    Set<Filter> Filters = null;
    List<PropertyObjectImplement> Orders = null;
    
    boolean UpKeys, DownKeys;
    List<Map<ObjectImplement,Integer>> Keys = null;
    // какие ключи активны
    Map<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> KeyOrders = null;

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид
    int Updated = 0;

    int PageSize = 10;
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

    SourceExpr JoinSelect(JoinList Joins,GroupObjectImplement ClassGroup,Map<ObjectImplement,SourceExpr> ClassSource) {

        Collection<PropertyInterface> NullInterfaces = new ArrayList();
        
        Iterator<PropertyInterface> i = Property.Interfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            ObjectImplement IntObject = Mapping.get(Interface);
            Interface.JoinImplement = (IntObject.GroupTo==ClassGroup?ClassSource.get(IntObject):new ValueSourceExpr(IntObject.idObject));
            if(Interface.JoinImplement==null) NullInterfaces.add(Interface);
        }

        SourceExpr Result = Property.JoinSelect(Joins);
        
        i = NullInterfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            ObjectImplement IntObject = Mapping.get(Interface);
            ClassSource.put(IntObject,Interface.JoinImplement);
        }
        
        return Result;
    }
}

// представление св-ва
class PropertyView {
    PropertyObjectImplement View;
    
    // в какой "класс" рисоваться, ессно одмн из ObjectImplement.GroupTo должен быть ToDraw
    GroupObjectImplement ToDraw;
}

// класс в котором лежит какие изменения произошли
// появляется по сути для отделения клиента, именно он возвращается назад клиенту
class FormChanges {
    Map<ObjectImplement,Integer> Objects;
    Map<GroupObjectImplement,List<Map<ObjectImplement,Integer>>> GridObjects;
    Map<PropertyView,Map<Integer,Object>> GridProperties;
    Map<PropertyView,Object> PanelProperties;
    Set<PropertyView> DropProperties;
}

class Filter {
    PropertyObjectImplement Property;

    GroupObjectImplement GetApplyObject() {
        return Property.GetApplyObject();
    }
    
    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        return Property.IsInInterface(ClassGroup);
    }

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return Property.ObjectUpdated(ClassGroup);
    }
    
    // для полиморфизма сюда
    void FillSelect(JoinList Joins, GroupObjectImplement ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource) {
        Property.JoinSelect(Joins,ClassGroup,ClassSource);
    }
}

class NotNullFilter extends Filter {
}

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

class FormBeanView {

    BusinessLogics BL;
    DataAdapter Adapter;

    // это будут Bean'овские интерфейсы
    public void ChangeObject(GroupObjectImplement Group,Map<ObjectImplement,Integer> idObjects) throws SQLException {    
        // проставим все объектам метки изменений
        Iterator<ObjectImplement> i = Group.iterator();
        while(i.hasNext()) {
            ObjectImplement Object = i.next();
            Integer idObject = idObjects.get(Object);
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
        Class GridClass = BL.GetClass(Adapter, idClass);
        if(Object.GridClass==GridClass) return;
        Object.GridClass = GridClass;
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
    
    List<GroupObjectImplement> Groups;
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    List<PropertyView> Properties;
    
    // Set чтобы сравнивать удобнее было бы
    Set<Filter> Filters;
    List<PropertyObjectImplement> Orders;
    
    // карта что сейчас в интерфейсе + карта в классовый\объектный вид
    Map<PropertyView,Boolean> DrawPool;
            
    public FormChanges EndApply() throws SQLException {

        FormChanges Result = new FormChanges();

        // бежим по списку вниз
        Map<GroupObjectImplement,Set<Filter>> MapGroupFilters = null;
        Map<GroupObjectImplement,List<PropertyObjectImplement>> MapGroupOrders = null;
        if(StructUpdated) {
            // построим Map'ы
            // фильтры
            MapGroupFilters = new HashMap();
            Iterator<Filter> ift = Filters.iterator();
            while(ift.hasNext()) {
                Filter Filt = ift.next();
                GroupObjectImplement ApplyObject = Filt.GetApplyObject();
                
                Set<Filter> FilterSet = MapGroupFilters.get(ApplyObject);
                if(FilterSet==null) {
                    FilterSet = new HashSet<Filter>();
                    MapGroupFilters.put(ApplyObject, FilterSet);
                }
                FilterSet.add(Filt);
            }
            // порядки
            MapGroupOrders = new HashMap();
            ListIterator<PropertyObjectImplement> ior = Orders.listIterator();
            while(ior.hasNext()) {
                PropertyObjectImplement Order = ior.next();
                GroupObjectImplement ApplyObject = Order.GetApplyObject();
                
                List<PropertyObjectImplement> OrderList = MapGroupOrders.get(ApplyObject);
                if(OrderList==null) {
                    OrderList = new ArrayList<PropertyObjectImplement>();
                    MapGroupOrders.put(ApplyObject, OrderList);
                }
                OrderList.add(Order);
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
            if(StructUpdated) {
                // проверяем интерфейсы, неактивные вырезаем
                Set<Filter> GroupFilters = MapGroupFilters.get(Group);
                List<PropertyObjectImplement> GroupOrders = MapGroupOrders.get(Group);

                iof = GroupFilters.iterator();
                while(iof.hasNext())
                    if(!iof.next().IsInInterface(Group)) iof.remove();
                ioo = GroupOrders.listIterator();
                while(ioo.hasNext())
                    if(!ioo.next().IsInInterface(Group)) ioo.remove();

                if(!(GroupFilters.equals(Group.Filters) || GroupOrders.equals(Group.Orders))) {
                    Group.Filters = GroupFilters;
                    Group.Orders = GroupOrders;
                    UpdateKeys = true;
                }
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
                if((Group.Updated & (1<<4+1<<0))!=0) {
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

                    SourceExpr OrderExpr = ToOrder.JoinSelect(JoinKeys,Group,KeySources);
                    SelectKeys.Orders.add(OrderExpr);
                    // надо закинуть их в запрос, а также установить фильтры на порядки чтобы
                    if(OrderValues!=null) {
                        OrderSources.add(OrderExpr);
                        OrderWheres.add(OrderValues.get(OrderExpr));
                    }
                    
                    // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                    SelectKeys.Expressions.add(new SelectExpression(OrderExpr,"order"+((Integer)(ioo.nextIndex()-1)).toString()));
                }

                // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
                ListIterator<ObjectImplement> igo = Group.listIterator();
                while(igo.hasNext()) {
                    ObjectImplement Object = igo.next();
                    SourceExpr KeyExpr = KeySources.get(Object);
                    Select KeySelectTable = null;
                    if(KeyExpr==null) {
                        KeySelectTable = BL.TableFactory.ObjectTable.ClassSelect(Object.GridClass);
                        KeySelectTable.JoinType = "FULL";
                        KeyExpr = new FieldSourceExpr(KeySelectTable,BL.TableFactory.ObjectTable.Key.Name);
                    } else 
                        KeySelectTable = BL.TableFactory.ObjectTable.ClassJoinSelect(Object.GridClass,KeyExpr);

                    JoinKeys.add(KeySelectTable);
                    
                    // также закинем их в порядок и в запрос
                    SelectKeys.Orders.add(KeyExpr);
                    if(KeyOrder!=null) {
                        OrderSources.add(KeyExpr);
                        OrderWheres.add(KeyOrder.get(Object));
                    }
                    
                    // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                    SelectKeys.Expressions.add(new SelectExpression(KeyExpr,"key"+((Integer)(igo.nextIndex()-1)).toString()));
                }
                
                // закидываем в Select все таблицы (с JOIN'ами по умодчанию)                ''
                Iterator<Select> ijk = JoinKeys.iterator();
                SelectKeys.From = ijk.next();
                while(ijk.hasNext()) SelectKeys.Joins.add(ijk.next());

                if(OrderSources.size()>0) {
                    Where OrderWhere = GenerateOrderWheres(OrderSources,OrderWheres,!DescOrder,0);
                    SelectKeys.From.Wheres.add(OrderWhere);
                }
                
                SelectKeys.Descending = DescOrder;
                SelectKeys.Top = Group.PageSize*3;
                
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
                ViewTable InsertTable = BL.TableFactory.ViewTables.get(Group.size());
                InsertTable.DropViewID(Adapter, Group.GID);

                while(ikr.hasNext()) {
                    Map<String,Object> ResultRow = ikr.next();
                    Map<ObjectImplement,Integer> KeyRow = new HashMap();
                    Map<PropertyObjectImplement,Object> OrderRow = new HashMap();
                    
                    // закинем сразу ключи для св-в чтобы Join'ить
                    Map<KeyField,Integer> ViewKeyInsert = new HashMap();
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
                        OrderRow.put(ToOrder,(Integer)ResultRow.get("order"+((Integer)(ioo.nextIndex()-1))));
                    }
                    
                    Group.Keys.add(KeyRow);
                    Group.KeyOrders.put(KeyRow, OrderRow);
                }

                Result.GridObjects.put(Group,Group.Keys);

                Group.Updated = (Group.Updated | (1<<2));
                
                // --- помечаем изменение объекта - если не скроллинг или чистое изменение вида
                if(GoTop)
                    ChangeObject(Group,Group.Keys.get(0));
            }
        }

//        while(i.hasNext()) i.next().EndApply();

        Iterator<PropertyView> ipv = Properties.iterator();
        while(ipv.hasNext()) {
            PropertyView DrawProp = ipv.next();
            
            // 0 - не изменилось, 1-простое изменение, 2-сложное изменени
            int PropUpdated = ((DrawProp.ToDraw.Updated & (1<<2+1<<4))!=0?2:0);
            // для всех представлений где изменился объект (или источник, но тогда объект точно поменялся)
            // сразу же и следующее условие проверяем
            Iterator<ObjectImplement> iov = DrawProp.View.Mapping.values().iterator();
            while(!(PropUpdated==2) && iov.hasNext())  {
                ObjectImplement Object = iov.next();
                if((Object.Updated & 1<<0)!=0)
                    PropUpdated = (Object.GroupTo==DrawProp.ToDraw?1:2);
            }
            
            if(PropUpdated>0) {
                // проверяем на вхождение в интерфейс (если изменился класс хоть одного объекта), определяем рисуемся в панель или грид (если изменилось вхождение\класс грида\классовый вид)
                // определяем где рисоваться, 3 варианта нигде, в классовом виде, в объектном
                // нужно сначала для классовых (GroupTo) проверить GetValueClass
                // затем для объектных проверяем GetValueClass
                
                DrawProp.View.GetValueClass();

                if(PropUpdated==1) {
                    // если изменился только один Group и рисуемся в гриде этого вида и его классовый вид не изменился и источник не мзменился
                    // если он рисовался в гри
                    
                } else {

                }
            // то не перечитываем, а просто ставим пометку
            // иначе 
            // если вид рисования панель 
            // то докидываем имплементацию к общему (dumb) Select'у
            // если грид 
            // то докидываем JoinSelect к Select'у (класса)
            }

        }
        
        // выполняем все Select'ы заполняем FormChanges возвращаем
        
        return Result;
    }
}

class Abc
{

}