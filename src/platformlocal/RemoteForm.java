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

// здесь многие подходы для оптимизации неструктурные, то есть можно было структурно все обновлять но это очень медленно
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRAlignment;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
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
    int Updated = (1<<3);

    GroupObjectImplement GroupTo;

    // чисто для отладки
    String OutName = "";
    
    // идентификатор (в рамках формы)
    int ID = 0;
    
    SourceExpr GetJoinExpr(Set<GroupObjectImplement> ClassGroup,Map<ObjectImplement,SourceExpr> ClassSource) {
        return (ClassGroup!=null && ClassGroup.contains(GroupTo)?ClassSource.get(this):new ValueSourceExpr(idObject));
    }
}

class GroupObjectMap<T> extends LinkedHashMap<ObjectImplement,T> {
    
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
    int GID = 0;
    
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
    int Updated = (1<<3);

    int PageSize = 5;
    
    void Out(GroupObjectValue Value) {
        for(ObjectImplement Object : this)
            System.out.print(" "+Object.OutName+" = "+Value.get(Object));
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

    void GetSourceSelect(JoinList JoinKeys,Set<GroupObjectImplement> ClassGroup,Map<ObjectImplement,SourceExpr> KeySources,TableFactory TableFactory,ChangesSession Session,Set<ObjectProperty> ChangedProps) {
        
        // фильтры первыми потому как ограничивают ключи
        for(Filter Filt : Filters) Filt.FillSelect(JoinKeys,ClassGroup,KeySources,Session,ChangedProps);

        // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
        for(ObjectImplement Object : this) {
            SourceExpr KeyExpr = KeySources.get(Object);
            From KeySelect = null;
            if(KeyExpr==null) {
                KeySelect = TableFactory.ObjectTable.ClassSelect(Object.GridClass);
                KeyExpr = new FieldSourceExpr(KeySelect,TableFactory.ObjectTable.Key.Name);

                // не было в фильтре
                // если есть remove'классы или новые объекты их надо докинуть                        
                if(Session!=null && Session.AddClasses.contains(Object.GridClass)) {
                    // придется UnionQuery делать
                    UnionQuery ResultQuery = new UnionQuery(2);
                    ResultQuery.Keys.add(TableFactory.ObjectTable.Key.Name);

                    SelectQuery SubQuery = new SelectQuery(KeySelect);
                    SubQuery.Expressions.put(TableFactory.ObjectTable.Key.Name,KeyExpr);
                    ResultQuery.Unions.add(SubQuery);

                    SubQuery = new SelectQuery(TableFactory.AddClassTable.ClassSelect(Session,Object.GridClass));
                    SubQuery.Expressions.put(TableFactory.ObjectTable.Key.Name,new FieldSourceExpr(SubQuery.From,TableFactory.AddClassTable.Object.Name));
                    ResultQuery.Unions.add(SubQuery);

                    KeySelect = new FromQuery(ResultQuery);
                    KeyExpr = new FieldSourceExpr(KeySelect,TableFactory.ObjectTable.Key.Name);
                }

                KeySources.put(Object,KeyExpr);
                JoinKeys.add(KeySelect);

                // надо сделать LEFT JOIN remove' классов
                if(Session!=null && Session.RemoveClasses.contains(Object.GridClass))
                    TableFactory.RemoveClassTable.ExcludeJoin(Session,JoinKeys,Object.GridClass,KeyExpr);
            }
//                    по идее не надо, так как фильтр по определению имеет нужный класс 
//                    else {
//                        KeySelect = BL.TableFactory.ObjectTable.ClassJoinSelect(Object.GridClass,KeyExpr);
//                        JoinKeys.add(KeySelect);
//                    } 
        }
    }
}

class PropertyObjectImplement extends PropertyImplement<ObjectProperty,ObjectImplement> {
    PropertyObjectImplement(ObjectProperty iProperty) {super(iProperty);}
    
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
            if(IntObject.GroupTo!=ClassGroup && ((IntObject.Updated & (1<<0))!=0)) return true;
        
        return false;
    }

    // изменился хоть один из классов интерфейса (могло повлиять на вхождение в интерфейс)
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : Mapping.values())
            if(((IntObject.Updated & (1<<(IntObject.GroupTo==ClassGroup?3:1))))!=0) return true;
        
        return false;
    }

    SourceExpr JoinSelect(JoinList Joins,Set<GroupObjectImplement> ClassGroup,Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps,boolean Left) {

        Collection<PropertyInterface> NullInterfaces = new ArrayList();
        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
        
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            ObjectImplement IntObject = Mapping.get(Interface);
            SourceExpr JoinExpr = IntObject.GetJoinExpr(ClassGroup,ClassSource);
            if(JoinExpr==null) 
                NullInterfaces.add(Interface);
            else
                JoinImplement.put(Interface,JoinExpr);
        }

        boolean Changed = (Session!=null && ChangedProps.contains(Property));
        
        // если есть не все интерфейсы и есть изменения надо с Full Join'ить старое с новым
        // иначе как и было
        SourceExpr Result = Property.UpdatedJoinSelect(Joins,JoinImplement,Left,Session);
        
        for(PropertyInterface Interface : NullInterfaces)
            ClassSource.put(Mapping.get(Interface),JoinImplement.get(Interface));

        return Result;
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
    
    void Out() {
        System.out.print(View.Property.OutName);
    }            
    
    // идентификатор (в рамках формы)
    int ID = 0;
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

abstract class Filter {
    
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
        return ChangedProps.contains(Property.Property);
    }
    
    // для полиморфизма сюда
    void FillSelect(JoinList Joins, Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps) {
        Property.JoinSelect(Joins,ClassGroup,ClassSource,Session,ChangedProps,false);
    }
}

class NotNullFilter extends Filter {
    
    NotNullFilter(PropertyObjectImplement iProperty) {
        super(iProperty);
    }
    
    @Override
    void FillSelect(JoinList Joins, Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps) {
        SourceExpr ValueExpr = Property.JoinSelect(Joins,ClassGroup,ClassSource,Session,ChangedProps,false);
        Joins.get(0).Wheres.add(new SourceIsNullWhere(ValueExpr,true));
    }
}

class CompareFilter extends Filter {
    
    ValueLink Value;
    int Compare;
    
    CompareFilter(PropertyObjectImplement iProperty,int iCompare,ValueLink iValue) {
        super(iProperty);
        Compare = iCompare;
        Value = iValue;
    }
    
    @Override
    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        Class ValueClass = Value.GetValueClass(ClassGroup);
        if(ValueClass==null) 
            return Property.IsInInterface(ClassGroup);
        else
            return ValueClass.IsParent(Property.GetValueClass(ClassGroup));
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        if(super.ClassUpdated(ClassGroup)) return true;
        
        return Value.ClassUpdated(ClassGroup);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        if(super.ObjectUpdated(ClassGroup)) return true;

        return Value.ObjectUpdated(ClassGroup);
    }

    @Override
    void FillSelect(JoinList Joins, Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps) {
        SourceExpr ValueExpr = Property.JoinSelect(Joins,ClassGroup,ClassSource,Session,ChangedProps,false);
        Joins.get(0).Wheres.add(new FieldExprCompareWhere(ValueExpr,Value.GetValueExpr(Joins,ClassGroup,ClassSource,Session,ChangedProps),Compare));
    }

}

abstract class ValueLink {

    Class GetValueClass(GroupObjectImplement ClassGroup) {return null;}
    
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {return false;}
    
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {return false;}

    abstract SourceExpr GetValueExpr(JoinList Joins, Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps);
}


class UserValueLink extends ValueLink {
    
    Object Value;
    
    UserValueLink(Object iValue) {Value=iValue;}

    SourceExpr GetValueExpr(JoinList Joins, Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps) {
        return new ValueSourceExpr(Value);
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
        return ((Object.Updated & (1<<1))!=0);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.Updated & (1<<0))!=0);
    }

    SourceExpr GetValueExpr(JoinList Joins, Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps) {
        return Object.GetJoinExpr(ClassGroup,ClassSource);
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

    SourceExpr GetValueExpr(JoinList Joins, Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement,SourceExpr> ClassSource, ChangesSession Session, Set<ObjectProperty> ChangedProps) {
        return Property.JoinSelect(Joins,ClassGroup,ClassSource,Session,ChangedProps,false);
    }
}


// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

abstract class RemoteForm<T extends BusinessLogics<T>> {

    T BL;

    ChangesSession Session;
    
    Set<ObjectProperty> ChangedProps = new HashSet();
    
    RemoteForm(DataAdapter iAdapter,T iBL) {
        Adapter = iAdapter;
        BL = iBL;
        
        Session = BL.CreateSession();
        
        StructUpdated = true;
    }

    // счетчик идентивикаторов
    int IDCount = 0;
    
    int GenID(int Offs) {
        return IDCount + Offs;
    }
            
    int IDShift(int Offs) {
        IDCount += Offs;
        return IDCount;
    }
    
    void AddGroup(GroupObjectImplement Group) { 
        Groups.add(Group);
        Group.Order = Groups.size();
        for(ObjectImplement Object : Group) Object.GroupTo = Group;
    }

    List<GroupObjectImplement> Groups = new ArrayList();
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    List<PropertyView> Properties = new ArrayList();
    
    // Set чтобы сравнивать удобнее было бы
    Set<Filter> Filters = new HashSet();
    List<PropertyObjectImplement> Orders = new ArrayList();
    
    // карта что сейчас в интерфейсе + карта в классовый\объектный вид
    Map<PropertyView,Boolean> InterfacePool = new HashMap();    

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
                Class ObjectClass = BL.GetClass(Session, Adapter, idObject);
                if(Object.Class!=ObjectClass) {
                    Object.Class = ObjectClass;
                    Object.Updated = Object.Updated | (1<<1);
                }
                
                Group.Updated = Group.Updated | (1<<0);
            }
        }
    }
    public void ChangeGridClass(ObjectImplement Object,Integer idClass) throws SQLException {    
        Class GridClass = BL.BaseClass.FindClassID(idClass);
        if(Object.GridClass==GridClass) return;
        if(GridClass==null) throw new RuntimeException();
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
        
        ObjectProperty ChangeProperty = Property.Property;
        Map<PropertyInterface,ObjectValue> Keys = new HashMap();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)ChangeProperty.Interfaces) {
            ObjectImplement Object = Property.Mapping.get(Interface);
            Keys.put(Interface,new ObjectValue(Object.idObject,Object.Class));
        }

        // изменяем св-во
        ChangeProperty.ChangeProperty(Adapter,Keys,Value,Session);
            
        DataChanged = true;
    }

    public void ChangePropertyView(PropertyView Property,Object Value) throws SQLException {
        ChangeProperty(Property.View,Value);
    }

    public void AddObject(ObjectImplement Object) throws SQLException {
        // пока тупо в базу
        Integer AddID = BL.AddObject(Session,Adapter,Object.GridClass);
        
        // берем все текущие CompareFilter на оператор 0(=) делаем ChangeProperty на ValueLink сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение ValueLink, GetValueExpr
        for(Filter Filter : Object.GroupTo.Filters) {
            if(Filter instanceof CompareFilter && ((CompareFilter)Filter).Compare==0) {
                SelectQuery SubQuery = new SelectQuery(null);
                JoinList Joins = new JoinList();
                Map<ObjectImplement,SourceExpr> ClassSource = new HashMap();
                Map<ObjectImplement,String> MapFields = new HashMap();
                int FieldNum = 0;
                for(ObjectImplement SibObject : Filter.Property.Mapping.values()) {
                    SourceExpr KeyExpr = null;
                    if(SibObject.GroupTo!=Object.GroupTo) {
                        KeyExpr = new ValueSourceExpr(SibObject.idObject);
                    } else {
                        if(SibObject!=Object) {
                            FromTable FromSib = BL.TableFactory.ObjectTable.ClassSelect(SibObject.GridClass);
                            KeyExpr = new FieldSourceExpr(FromSib,BL.TableFactory.ObjectTable.Key.Name);
                            Joins.add(FromSib);
                        } else
                            KeyExpr = new ValueSourceExpr(AddID);
                    }

                    ClassSource.put(SibObject,KeyExpr);

                    String KeyField = "key" + (FieldNum++);
                    MapFields.put(SibObject,KeyField);
                    SubQuery.Expressions.put(KeyField,KeyExpr);
                }

                SubQuery.Expressions.put("newvalue",((CompareFilter)Filter).Value.GetValueExpr(Joins,Object.GroupTo.GetClassGroup(),ClassSource,Session,ChangedProps));
                
                if(Joins.size()==0) Joins.add(new FromTable("dumb"));
                Iterator<From> ij = Joins.iterator();
                SubQuery.From = ij.next();
                while(ij.hasNext()) SubQuery.From.Joins.add(ij.next());
                
                List<Map<String,Object>> Result = Adapter.ExecuteSelect(SubQuery);
                // изменяем св-ва
                for(Map<String,Object> Row : Result) {
                    ObjectProperty ChangeProperty = Filter.Property.Property;
                    Map<PropertyInterface,ObjectValue> Keys = new HashMap();
                    for(PropertyInterface Interface : (Collection<PropertyInterface>)ChangeProperty.Interfaces) {
                        ObjectImplement ChangeObject = Filter.Property.Mapping.get(Interface);
                        Keys.put(Interface,new ObjectValue((Integer)Row.get(MapFields.get(ChangeObject)),ChangeObject.GridClass));
                    }
                    ChangeProperty.ChangeProperty(Adapter,Keys,Row.get("newvalue"),Session);
                }                
            }
        }
        DataChanged = true;
    }   

    public void ChangeClass(ObjectImplement Object,Class Class) throws SQLException {
        BL.ChangeClass(Session,Adapter,Object.idObject,Class);
        DataChanged = true;
    }
    
    // рекурсия для генерации порядка
    Where GenerateOrderWheres(List<SourceExpr> OrderSources,List<Object> OrderWheres,boolean More,int Index) {
        
        SourceExpr OrderExpr = OrderSources.get(Index);
        Object OrderValue = OrderWheres.get(Index);
        boolean Last = !(Index+1<OrderSources.size());
        Where OrderWhere = new FieldExprCompareWhere(OrderExpr,OrderValue,(More?(Last?3:1):2));
        if(!Last) {
            Where NextWhere = GenerateOrderWheres(OrderSources,OrderWheres,More,Index+1);
            
            // >A OR (=A AND >B)
            return new FieldOPWhere(OrderWhere,new FieldOPWhere(new FieldExprCompareWhere(OrderExpr,OrderValue,0),NextWhere,true),false);
        } else
            return OrderWhere;
    }

    // применяет изменения
    public String SaveChanges() throws SQLException {

        if(BL.Apply(Adapter,Session)) {
            StartNewSession();
            
            return "pass";
        }        
            return "failed";
    }

    boolean Cancel = false;
    public void CancelChanges() {
        Cancel = true;
        
        DataChanged = true;
    }
    
    // получаем все аггрегированные св-ва задействованные на форме
    Set<ObjectProperty> GetProperties() {

        Set<ObjectProperty> Result = new HashSet();
        for(PropertyView PropView : Properties)
            Result.add(PropView.View.Property);
        return Result;
    }
    
    public void StartNewSession() throws SQLException {

        ChangedProps.clear();
        BL.DropSession(Adapter, Session);
        Session = BL.CreateSession();
    }
    
    void Close() throws SQLException {
        
        for(GroupObjectImplement Group : Groups) {
            ViewTable DropTable = BL.TableFactory.ViewTables.get(Group.size()-1);
            DropTable.DropViewID(Adapter, Group.GID);
        }        
    }

    // поиски по свойствам\объектам
    Map<PropertyObjectImplement,Object> UserPropertySeeks = new HashMap();
    Map<ObjectImplement,Integer> UserObjectSeeks = new HashMap();
    
    void AddPropertySeek(PropertyObjectImplement Property, Object Value) {
        UserPropertySeeks.put(Property,Value);
    }

    void AddObjectSeek(ObjectImplement Object, Integer Value) {
        UserObjectSeeks.put(Object,Value);
    }

    public FormChanges EndApply() throws SQLException {

        FormChanges Result = new FormChanges();

        // если изменились данные, применяем изменения
        if(DataChanged)
            ChangedProps.addAll(BL.UpdateAggregations(Adapter,GetProperties(),Session));

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
            // если изменились :
            // хоть один класс из этого GroupObjectImplement'a - (флаг Updated - 3)
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
                    if(DataChanged && Filt.DataUpdated(ChangedProps)) {UpdateKeys = true; break;}
            if(!UpdateKeys)
                for(PropertyObjectImplement Order : Group.Orders)
                    if(DataChanged && ChangedProps.contains(Order.Property)) {UpdateKeys = true; break;}
            // классы удалились\добавились
            if(!UpdateKeys && DataChanged) {
                for(ObjectImplement Object : Group)
                    if(Session.AddClasses.contains(Object.GridClass) || Session.RemoveClasses.contains(Object.GridClass)) {UpdateKeys = true; break;}
            }

            // по возврастанию (0), убыванию (1), центру (2) и откуда начинать
            int Direction = 0;
            Map<PropertyObjectImplement,Object> PropertySeeks = new HashMap();
            GroupObjectValue ObjectSeeks = new GroupObjectValue();
            // один раз читаем не так часто делается, поэтому не будем как с фильтрами
            for(PropertyObjectImplement Property : UserPropertySeeks.keySet()) {
                if(Property.GetApplyObject()==Group) {
                    PropertySeeks.put(Property,UserPropertySeeks.get(Property));
                    UpdateKeys = true;
                    Direction = 2;
                }
            }
            for(ObjectImplement Object : UserObjectSeeks.keySet()) {
                if(Object.GroupTo==Group) {
                    ObjectSeeks.put(Object,UserObjectSeeks.get(Object));
                    UpdateKeys = true;
                    Direction = 2;
                }
            }

            if(!UpdateKeys && (Group.Updated & (1<<4))!=0) {
               // изменился "классовый" вид перечитываем св-ва
                ObjectSeeks = Group.GetObjectValue();
                UpdateKeys = true;
                Direction = 2;
            }
                
            if(!UpdateKeys && Group.GridClassView && (Group.Updated & (1<<0))!=0) {
                // листание - объекты стали близки к краю (idObject не далеко от края - надо хранить список не базу же дергать) - изменился объект
                int KeyNum = Group.Keys.indexOf(Group.GetObjectValue());
                // если меньше PageSize осталось и сверху есть ключи
                if(KeyNum<Group.PageSize && Group.UpKeys) {
                    Direction = 1;
                    UpdateKeys = true;
                    
                    if(Group.PageSize*2<Group.Keys.size()) {
                        ObjectSeeks = Group.Keys.get(Group.PageSize*2);
                        PropertySeeks = Group.KeyOrders.get(ObjectSeeks);
                    }
                } else {
                // наоборот вниз
                if(KeyNum>Group.Keys.size()-Group.PageSize && Group.DownKeys) {
                    Direction = 0;
                    UpdateKeys = true;
                    
                    if(Group.Keys.size()-2*Group.PageSize>=0) {
                        ObjectSeeks = Group.Keys.get(Group.Keys.size()-2*Group.PageSize);
                        PropertySeeks = Group.KeyOrders.get(ObjectSeeks);
                    }
                }
                }
            }
            
            if(UpdateKeys) {
                // --- перечитываем источник (если "классовый" вид - 50, + помечаем изменения GridObjects, иначе TOP 1

                // сделаем Map названий ключей
                int ObjectNum = 0;
                Map<ObjectImplement,String> KeyNames = new HashMap();
                for(ObjectImplement ObjectKey : Group)
                    KeyNames.put(ObjectKey,"key"+(ObjectNum++));
                int OrderNum = 0;
                Map<PropertyObjectImplement,String> OrderNames = new HashMap();
                for(PropertyObjectImplement Order : Orders)
                    OrderNames.put(Order,"order"+(OrderNum++));
                
                // докидываем Join'ами (INNER) фильтры, порядки
                
                // уберем все некорректности в Seekах :
                // корректно если : PropertySeeks = Orders или (Orders.sublist(PropertySeeks.size) = PropertySeeks и ObjectSeeks - пустое)
                // если Orders.sublist(PropertySeeks.size) != Orders, тогда дочитываем ObjectSeeks полностью
                // выкидываем лишние PropertySeeks, дочитываем недостающие Orders в PropertySeeks
                // также если панель то тупо прочитаем объект
                boolean NotEnoughOrders = !(PropertySeeks.keySet().equals(new HashSet(Orders)) || ((PropertySeeks.size()<Orders.size() && (new HashSet(Orders.subList(0,PropertySeeks.size()))).equals(PropertySeeks.keySet())) && ObjectSeeks.size()==0));
                if((NotEnoughOrders || !Group.GridClassView) && ObjectSeeks.size()<Group.size()) {
                    // дочитываем ObjectSeeks то есть на = PropertySeeks, ObjectSeeks
                    JoinList JoinKeys = new JoinList();
                    Map<ObjectImplement,SourceExpr> KeySources = new HashMap();
                    Group.GetSourceSelect(JoinKeys,Group.GetClassGroup(),KeySources,BL.TableFactory,Session,ChangedProps);
                    for(Entry<PropertyObjectImplement,Object> Property : PropertySeeks.entrySet())
                        JoinKeys.get(0).Wheres.add(new FieldExprCompareWhere(Property.getKey().JoinSelect(JoinKeys,Group.GetClassGroup(),KeySources,Session,ChangedProps,false),Property.getValue(),0));
                    for(Entry<ObjectImplement,Integer> ObjectValue : ObjectSeeks.entrySet())
                        JoinKeys.get(0).Wheres.add(new FieldExprCompareWhere(KeySources.get(ObjectValue.getKey()),ObjectValue.getValue(),0));

                    Iterator<From> ij = JoinKeys.iterator();
                    SelectQuery SelectKeys = new SelectQuery(ij.next());
                    while(ij.hasNext()) SelectKeys.From.Joins.add(ij.next());

                    for(ObjectImplement ObjectKey : Group)
                        SelectKeys.Expressions.put(KeyNames.get(ObjectKey),KeySources.get(ObjectKey));

                    SelectKeys.Top = 1;
                    Adapter.OutSelect(SelectKeys);
                    List<Map<String,Object>> ResultKeys = Adapter.ExecuteSelect(SelectKeys);
                    if(ResultKeys.size()>0)
                        for(ObjectImplement ObjectKey : Group)
                            ObjectSeeks.put(ObjectKey,(Integer)ResultKeys.get(0).get(KeyNames.get(ObjectKey)));
                }

                if(!Group.GridClassView) {
                    // если панель и ObjectSeeks "полный", то просто меняем объект и ничего не читаем
                    Result.Objects.put(Group,ObjectSeeks);
                    ChangeObject(Group,ObjectSeeks);
                } else {
                    // выкидываем Property которых нет, дочитываем недостающие Orders, по ObjectSeeks то есть не в привязке к отбору
                    if(NotEnoughOrders && ObjectSeeks.size()==Group.size() && Orders.size() > 0) {
                        SelectQuery OrderQuery = new SelectQuery(null);
                        JoinList JoinKeys = new JoinList();
                        JoinKeys.add(new FromTable("dumb"));
                        // придется создавать KeySources
                        Map<ObjectImplement,SourceExpr> KeySources = new HashMap();
                        for(Entry<ObjectImplement,Integer> ObjectSeek : ObjectSeeks.entrySet())
                            KeySources.put(ObjectSeek.getKey(),new ValueSourceExpr(ObjectSeek.getValue()));
                        for(PropertyObjectImplement Order : Orders)
                            OrderQuery.Expressions.put(OrderNames.get(Order),Order.JoinSelect(JoinKeys,Group.GetClassGroup(),KeySources,Session,ChangedProps,true));
                        Iterator<From> ij = JoinKeys.iterator();
                        OrderQuery.From = ij.next();
                        while(ij.hasNext()) OrderQuery.From.Joins.add(ij.next());
                        Adapter.OutSelect(OrderQuery);
                        List<Map<String,Object>> ResultOrders = Adapter.ExecuteSelect(OrderQuery);
                        for(PropertyObjectImplement Order : Orders)
                            PropertySeeks.put(Order,ResultOrders.get(0).get(OrderNames.get(Order)));
                    }
                
                    JoinList JoinKeys = new JoinList();
                    Map<ObjectImplement,SourceExpr> KeySources = new HashMap();
                    Group.GetSourceSelect(JoinKeys,Group.GetClassGroup(),KeySources,BL.TableFactory,Cancel?null:Session,ChangedProps);

                    OrderedSelectQuery SelectKeys = new OrderedSelectQuery(null);

                    // складываются источники и значения
                    List<SourceExpr> OrderSources = new ArrayList();
                    List<Object> OrderWheres = new ArrayList();

                    // закинем порядки (с LEFT JOIN'ом)
                    for(PropertyObjectImplement ToOrder : Group.Orders) {
                        SourceExpr OrderExpr = ToOrder.JoinSelect(JoinKeys,Group.GetClassGroup(),KeySources,Cancel?null:Session,ChangedProps,true);
                        SelectKeys.Orders.add(OrderExpr);
                        // надо закинуть их в запрос, а также установить фильтры на порядки чтобы
                        if(PropertySeeks.containsKey(ToOrder)) {
                            OrderSources.add(OrderExpr);
                            OrderWheres.add(PropertySeeks.get(ToOrder));
                        }                    
                        // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                        SelectKeys.Expressions.put(OrderNames.get(ToOrder),OrderExpr);
                    }

                    // докинем в ObjectSeeks недостающие группы
                    for(ObjectImplement ObjectKey : Group)
                        if(!ObjectSeeks.containsKey(ObjectKey))
                            ObjectSeeks.put(ObjectKey,null);
                    
                    // закинем объекты в порядок
                    for(ObjectImplement ObjectKey : ObjectSeeks.keySet()) {
                        SourceExpr KeyExpr = KeySources.get(ObjectKey);
                        // также закинем их в порядок и в запрос6
                        SelectKeys.Orders.add(KeyExpr);
                        Integer SeekValue = ObjectSeeks.get(ObjectKey);
                        if(SeekValue!=null) {
                            OrderSources.add(KeyExpr);
                            OrderWheres.add(SeekValue);
                        }
                        // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                        SelectKeys.Expressions.put(KeyNames.get(ObjectKey),KeyExpr);
                    }
                    
                    // закидываем в Select все таблицы (с JOIN'ами по умодчанию)                ''
                    ListIterator<From> ijk = JoinKeys.listIterator();
                    SelectKeys.From = ijk.next();
                    while(ijk.hasNext()) SelectKeys.From.Joins.add(ijk.next());

                    // выполняем запрос

                    // какой ряд выбранным будем считать
                    int ActiveRow = -1;
                    // результат
                    List<Map<String,Object>> KeyResult = new ArrayList();

                    SelectKeys.Top = Group.PageSize*3/(Direction==2?2:1);

                    // сначала Descending загоним
                    Group.DownKeys = false;
                    Group.UpKeys = false;
                    if(Direction==1 || Direction==2) {
                        Where OrderWhere = null;
                        if(OrderSources.size()>0) {
                            OrderWhere = GenerateOrderWheres(OrderSources,OrderWheres,false,0);
                            SelectKeys.From.Wheres.add(OrderWhere);
                            Group.DownKeys = true;
                        }

                        SelectKeys.Descending = true;
                        Adapter.OutSelect(SelectKeys);
                        ListIterator<Map<String,Object>> ik = Adapter.ExecuteSelect(SelectKeys).listIterator();
                        while(ik.hasNext()) ik.next();
                        while(ik.hasPrevious()) KeyResult.add(ik.previous());
                        Group.UpKeys = (KeyResult.size()==SelectKeys.Top);

                        if(OrderSources.size()>0)
                            SelectKeys.From.Wheres.remove(OrderWhere);
                        else
                            ActiveRow = KeyResult.size()-1;
                    }
                    // потом Ascending
                    if(Direction==0 || Direction==2) {
                        if(OrderSources.size()>0) {
                            Where OrderWhere = GenerateOrderWheres(OrderSources,OrderWheres,true,0);
                            SelectKeys.From.Wheres.add(OrderWhere);
                            if(Direction!=2) Group.UpKeys = true;
                        }

                        SelectKeys.Descending = false;
                        Adapter.OutSelect(SelectKeys);
                        List<Map<String,Object>> ExecuteList = Adapter.ExecuteSelect(SelectKeys);
                        if((OrderSources.size()==0 || Direction==2) && ExecuteList.size()>0) ActiveRow = KeyResult.size();
                        KeyResult.addAll(ExecuteList);
                        Group.DownKeys = (ExecuteList.size()==SelectKeys.Top);
                    }
                    
                    Group.Keys = new ArrayList();
                    Group.KeyOrders = new HashMap();

                    // параллельно будем обновлять ключи чтобы Join'ить
                    ViewTable InsertTable = BL.TableFactory.ViewTables.get(Group.size()-1);
                    InsertTable.DropViewID(Adapter, Group.GID);
                    
                    for(Map<String,Object> ResultRow : KeyResult) {
                        GroupObjectValue KeyRow = new GroupObjectValue();
                        Map<PropertyObjectImplement,Object> OrderRow = new HashMap();

                        // закинем сразу ключи для св-в чтобы Join'ить
                        Map<KeyField,Integer> ViewKeyInsert = new HashMap();
                        ViewKeyInsert.put(InsertTable.View,Group.GID);
                        ListIterator<KeyField> ivk = InsertTable.Objects.listIterator();
                        // !!!! важно в Keys сохранить порядок ObjectSeeks
                        for(ObjectImplement ObjectKey : ObjectSeeks.keySet()) {
                            Integer KeyValue = (Integer)ResultRow.get(KeyNames.get(ObjectKey));
                            KeyRow.put(ObjectKey,KeyValue);
                            ViewKeyInsert.put(ivk.next(), KeyValue);
                        }
                        Adapter.InsertRecord(InsertTable,ViewKeyInsert,new HashMap());

                        OrderNum = 0;
                        for(PropertyObjectImplement ToOrder : Group.Orders)
                            OrderRow.put(ToOrder,ResultRow.get(OrderNames.get(ToOrder)));

                        Group.Keys.add(KeyRow);
                        Group.KeyOrders.put(KeyRow, OrderRow);
                    }

                    Result.GridObjects.put(Group,Group.Keys);

                    Group.Updated = (Group.Updated | (1<<2));

                    // если ряд никто не подставил и ключи есть пробуем старый найти
                    if(ActiveRow<0 && Group.Keys.size()>0)
                        ActiveRow = Group.Keys.indexOf(Group.GetObjectValue());

                    if(ActiveRow>=0) {
                        // нашли ряд его выбираем
                        Result.Objects.put(Group,Group.Keys.get(ActiveRow));
                        ChangeObject(Group,Group.Keys.get(ActiveRow));
                    } else 
                        ChangeObject(Group,new GroupObjectValue());
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
                SelectProps.Expressions.put(SelectField,DrawProp.View.JoinSelect(JoinProps,null,null,Cancel?null:Session,ChangedProps,true));
                ToFields.put(DrawProp, SelectField);
            }
        
            for(From Join : JoinProps)
                SelectProps.From.Joins.add(Join);

            Adapter.OutSelect(SelectProps);
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
                SelectProps.Expressions.put(SelectField,DrawProp.View.JoinSelect(JoinProps,Group.GetClassGroup(),MapKeys,Cancel?null:Session,ChangedProps,true));
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
        if(Cancel) {
            StartNewSession();            
            Cancel = false;
        }        
        
        Result.Out(this);

        return Result;
    }

    abstract ClientFormBean GetRichDesign();

    // считывает все данные (для отчета)
    FormData ReadData() throws SQLException {
        
        Set<GroupObjectImplement> ReportObjects = GetReportObjects();
        
        // пока сделаем тупо получаем один большой запрос
        
        OrderedSelectQuery Query = new OrderedSelectQuery(null);
        JoinList Joins = new JoinList();

        Set<GroupObjectImplement> ClassGroup = new HashSet();
        Map<ObjectImplement,SourceExpr> KeySources = new HashMap();
        
        for(GroupObjectImplement Group : Groups) {
            if(!ReportObjects.contains(Group)) {
                // не фиксированные ключи
                ClassGroup.add(Group);
                Group.GetSourceSelect(Joins,ClassGroup,KeySources,BL.TableFactory,Session,ChangedProps);
            }
            
            // закинем Order'ы
            for(PropertyObjectImplement Order : Group.Orders)
                Query.Orders.add(Order.JoinSelect(Joins,ClassGroup,KeySources,Session,ChangedProps,true));

            for(ObjectImplement Object : Group) {
                SourceExpr KeyExpr = Object.GetJoinExpr(ClassGroup,KeySources);
                
                Query.Expressions.put("obj"+Object.ID,KeyExpr);
                Query.Orders.add(KeyExpr);
            }
        }

        FormData Result = new FormData();

        for(PropertyView Property : Properties) {
            Query.Expressions.put("prop"+Property.ID,Property.View.JoinSelect(Joins,ClassGroup,KeySources,Session,ChangedProps,true));

            Result.Properties.put(Property.ID,new HashMap());
        }

        // JoinList закинем 
        Iterator<From> ij = Joins.iterator();
        Query.From = ij.next();
        while(ij.hasNext()) Query.From.Joins.add(ij.next());

        List<Map<String,Object>> ResultSelect = Adapter.ExecuteSelect(Query);
        
        for(Map<String,Object> Row : ResultSelect) {
            Map<Integer,Integer> GroupValue = new HashMap();
            for(GroupObjectImplement Group : Groups)
                for(ObjectImplement Object : Group)
                    GroupValue.put(Object.ID,(Integer)Row.get("obj"+Object.ID));
            
            Result.ReadOrder.add(GroupValue);
            
            for(PropertyView Property : Properties)
                Result.Properties.get(Property.ID).put(GroupValue,Row.get("prop"+Property.ID));
        }
        
        return Result;
    }

    // возвращает какие объекты фиксируются
    Set<GroupObjectImplement> GetReportObjects() {
        return new HashSet();
    }
    
    // получает XML отчета
    JasperDesign GetReportDesign() {
        // итак цель сделать генерацию XML
        // бежим по GroupObjectImplement
        JasperDesign Design = new JasperDesign();
        int PageWidth = 595-40;
//        Design.setPageWidth(PageWidth);
        Design.setName("Report");
        
	JRDesignStyle Style = new JRDesignStyle();
	Style.setName("Arial_Normal");
	Style.setDefault(true);
	Style.setFontName("Arial");
	Style.setFontSize(12);
	Style.setPdfFontName("c:\\windows\\fonts\\tahoma.ttf");
	Style.setPdfEncoding("Cp1251");
	Style.setPdfEmbedded(false);
        try {
            Design.addStyle(Style);
        } catch(JRException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
            
                
        for(GroupObjectImplement Group : Groups) {
            Collection<ReportDrawField> DrawFields = new ArrayList();
            
            // сначала все коды 
            for(ObjectImplement Object : Group)
                DrawFields.add(new ReportDrawField("obj"+Object.ID,Object.OutName,"integer"));

            // бежим по всем свойствам входящим в объектам
            for(PropertyView Property : Properties) {
                GroupObjectImplement DrawProp = (Property.ToDraw==null?Property.View.GetApplyObject():Property.ToDraw);
                if(DrawProp==Group) 
                    DrawFields.add(new ReportDrawField("prop"+Property.ID,Property.View.Property.OutName,Property.View.Property.GetDBType()));
            }

            JRDesignBand Band = new JRDesignBand();
            int BandHeight = 20;
            Band.setHeight(BandHeight);

            boolean Detail = (Group==Groups.get(Groups.size()-1));
            JRDesignBand PageHeadBand = null;
            int PageHeadHeight = 20;
            if(Detail) {
                // создадим PageHead
                PageHeadBand = new JRDesignBand();
                PageHeadBand.setHeight(PageHeadHeight);
                Design.setPageHeader(PageHeadBand);
                
                Design.setDetail(Band);
            } else {
                // создадим группу
		JRDesignGroup DesignGroup = new JRDesignGroup();
		DesignGroup.setName("Group"+Group.GID);
       		JRDesignExpression GroupExpr = new JRDesignExpression();
		GroupExpr.setValueClass(java.lang.String.class);
		String GroupString = "";
                for(ObjectImplement Object : Group)
                    GroupString = (GroupString.length()==0?"":GroupString+"+\" \"+")+"String.valueOf($F{obj"+Object.ID+"})";
                GroupExpr.setText(GroupString);

                DesignGroup.setExpression(GroupExpr);
                DesignGroup.setGroupHeader(Band);

                try {
                    Design.addGroup(DesignGroup);
                } catch(JRException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            // узнаем общую ширину чтобы пропорционально считать ()
            int TotalWidth = 0;
            for(ReportDrawField Draw : DrawFields) {
                if(!Detail) TotalWidth += Draw.GetCaptionWidth();
                TotalWidth += Draw.Width;
            }
            

            int Left = 0;
            for(ReportDrawField Draw : DrawFields) {
                // закидываем сначала Field
       		JRDesignField JRField = new JRDesignField();
		JRField.setName(Draw.ID);
		JRField.setValueClassName(Draw.ValueClass.getName());
                try {
                    Design.addField(JRField);
                } catch(JRException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                int DrawWidth = PageWidth*Draw.Width/TotalWidth;
                
                JRDesignStaticText DrawCaption = new JRDesignStaticText();
		DrawCaption.setText(Draw.Caption);
                DrawCaption.setX(Left);
                DrawCaption.setY(0);

                if(Detail) {
                    DrawCaption.setWidth(DrawWidth);
                    DrawCaption.setHeight(PageHeadHeight);
                    DrawCaption.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_CENTER);
                    PageHeadBand.addElement(DrawCaption);                    
                } else {
                    int CaptWidth = PageWidth*Draw.GetCaptionWidth()/TotalWidth;
                    DrawCaption.setWidth(CaptWidth);
                    DrawCaption.setHeight(BandHeight);
                    DrawCaption.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_LEFT);
                    Left += CaptWidth;
                    Band.addElement(DrawCaption);
                }
                DrawCaption.setStretchType(JRDesignStaticText.STRETCH_TYPE_RELATIVE_TO_BAND_HEIGHT);

                JRDesignTextField DrawText = new JRDesignTextField();
                DrawText.setX(Left);
                DrawText.setY(0);
                DrawText.setWidth(DrawWidth);
                DrawText.setHeight(BandHeight);
                DrawText.setHorizontalAlignment(Draw.Alignment);
                Left += DrawWidth;
                
		JRDesignExpression DrawExpr = new JRDesignExpression();
		DrawExpr.setValueClass(Draw.ValueClass);
		DrawExpr.setText("$F{"+Draw.ID+"}");
		DrawText.setExpression(DrawExpr);
                Band.addElement(DrawText);
                
                DrawText.setStretchWithOverflow(true);
            }
        }
        
        return Design;
    }
    
    RemoteNavigator<T> CreateNavigator() {
        return new RemoteNavigator(Adapter,BL,new HashMap());
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
class FormData implements JRDataSource {
    
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