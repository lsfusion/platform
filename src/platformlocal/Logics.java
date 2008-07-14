/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

class TableImplement extends ArrayList<DataPropertyInterface> {
    // заполняются пока автоматически
    Table Table;
    Map<DataPropertyInterface,KeyField> MapFields;

    TableImplement() {
        Childs = new HashSet<TableImplement>();
        Parents = new HashSet<TableImplement>();
    }
    
    // кэшированный граф
    Set<TableImplement> Childs;
    Set<TableImplement> Parents;

    boolean RecCompare(boolean ToParent,Collection<DataPropertyInterface> ToCompare,ListIterator<DataPropertyInterface> iRec,Map<DataPropertyInterface,DataPropertyInterface> MapTo) {
        if(!iRec.hasNext()) return true;

        DataPropertyInterface ProceedItem = iRec.next();
        Iterator<DataPropertyInterface> iToC = ToCompare.iterator();
        while(iToC.hasNext()) {
            DataPropertyInterface PairItem = iToC.next();
            if((ToParent && ProceedItem.Class.IsParent(PairItem.Class) || (!ToParent && PairItem.Class.IsParent(ProceedItem.Class)))) {
                if(!MapTo.containsKey(PairItem)) {
                // если parent - есть связь и нету ключа, гоним рекурсию дальше
                MapTo.put(PairItem, ProceedItem);
                // если нашли карту выходим
                if(RecCompare(ToParent,ToCompare,iRec,MapTo)) return true;
                MapTo.remove(PairItem);
                }
            }
        }

        iRec.previous();
        return false;
    }
    // 0 никак не связаны, 1 - параметр снизу в дереве, 2 - параметр сверху в дереве или равно
    // также возвращает карту если 2
    int Compare(Collection<DataPropertyInterface> ToCompare,Map<KeyField,DataPropertyInterface> MapTo) {
        
        if(ToCompare.size() != size()) return 0;

        // перебором и не будем страдать фигней
        // сначала что не 1 проверим
    
        HashMap<DataPropertyInterface,DataPropertyInterface> MapProceed = new HashMap<DataPropertyInterface,DataPropertyInterface>();
        
        ListIterator<DataPropertyInterface> iRec = (new ArrayList<DataPropertyInterface>(this)).listIterator();
        if(RecCompare(false,ToCompare,iRec,MapProceed)) {
            if(MapTo!=null) {
                MapTo.clear();
                Iterator<DataPropertyInterface> it = ToCompare.iterator();
                while(it.hasNext()) {
                    DataPropertyInterface DataInterface = it.next();
                    MapTo.put(MapFields.get(MapProceed.get(DataInterface)),DataInterface);
                }
            }
            
            return 2;
        }

        // MapProceed и так чистый и iRec также в начале
        if(RecCompare(true,ToCompare,iRec,MapProceed))
            return 1;
        
        // !!!! должна заполнять MapTo только если уже нашла
        return 0;
    }

    void RecIncludeIntoGraph(TableImplement IncludeItem,boolean ToAdd,Set<TableImplement> Checks) {
        
        if(Checks.contains(this)) return;
        Checks.add(this);
        
        Iterator<TableImplement> i = Parents.iterator();
        while(i.hasNext()) {
            TableImplement Item = i.next();
            Integer Relation = Item.Compare(IncludeItem,null);
            if(Relation==1) {
                // снизу в дереве
                // добавляем ее как промежуточную
                Item.Childs.add(IncludeItem);
                IncludeItem.Parents.add(Item);
                if(ToAdd) {
                    Item.Childs.remove(this);
                    i.remove();
                }
            } else {
                // сверху в дереве или никак не связаны
                // передаем дальше
                Item.RecIncludeIntoGraph(IncludeItem,Relation==2,Checks);
                if(Relation==2) ToAdd = false;
            }
        }
        
        // если снизу добавляем Childs
        if(ToAdd) {
            IncludeItem.Childs.add(this);
            Parents.add(IncludeItem);
        }
    }

    Table GetTable(Collection<DataPropertyInterface> FindItem,Map<KeyField,DataPropertyInterface> MapTo) {
        Iterator<TableImplement> i = Parents.iterator();
        while(i.hasNext()) {
            TableImplement Item = i.next();
            if(Item.Compare(FindItem,MapTo)==2) 
                return Item.GetTable(FindItem,MapTo);
        }
        
        return Table;
    }
    
    void FillSet(Set<TableImplement> TableImplements) {
        if(!TableImplements.add(this)) return;
        Iterator<TableImplement> i = Parents.iterator();
        while(i.hasNext()) i.next().FillSet(TableImplements);
    }

    void OutClasses() {
        Iterator<DataPropertyInterface> i = iterator();
        while(i.hasNext()) 
            System.out.print(i.next().Class.ID.toString()+" ");
    }
    void Out() {
        //выводим себя
        System.out.print("NODE - ");
        OutClasses();
        System.out.println("");
        
        Iterator<TableImplement> i = Childs.iterator();
        while(i.hasNext()) {
            System.out.print("childs - ");
            i.next().OutClasses();
            System.out.println();
        }

        i = Parents.iterator();
        while(i.hasNext()) {
            System.out.print("parents - ");
            i.next().OutClasses();
            System.out.println();
        }
        
        i = Parents.iterator();
        while(i.hasNext()) i.next().Out();
    }
}

// таблица в которой лежат объекты
class ObjectTable extends Table {
    
    KeyField Key;
    Field Class;
    
    ObjectTable() {
        super("objects");
        Key = new KeyField("object","integer");
        KeyFields.add(Key);
        Class = new Field("class","integer");
        PropFields.add(Class);
    };
    
    FromTable ClassSelect(Class ToSelect) {
        Collection<Integer> SetID = new HashSet<Integer>();
        ToSelect.FillSetID(SetID);
        
        FromTable ClassTable = new FromTable(Name);
        ClassTable.Wheres.add(new FieldSetValueWhere(SetID,Class.Name));
        
        return ClassTable;
    }
    
    FromTable ClassJoinSelect(Class ToSelect,SourceExpr JoinImplement) {
        FromTable JoinTable = ClassSelect(ToSelect);
        JoinTable.Wheres.add(new FieldWhere(JoinImplement,Key.Name));
        return JoinTable;
    }
    
    Integer GetClassID(DataAdapter Adapter,Integer idObject) throws SQLException {
        SelectQuery Query = new SelectQuery(new FromTable(Name));
        Query.From.Wheres.add(new FieldValueWhere(idObject,Key.Name));
        Query.Expressions.put("classid",new FieldSourceExpr(Query.From,Class.Name));
        return (Integer)Adapter.ExecuteSelect(Query).get(0).get("classid");
    }
  
}

// таблица счетчика ID
class IDTable extends Table {
    KeyField Key;
    
    IDTable() {
        super("idtable");
        Key = new KeyField("lastid","integer");
        KeyFields.add(Key);
    }
    
    Integer GenerateID(DataAdapter Adapter) throws SQLException { 
        FromTable From = new FromTable(Name);
        SelectQuery SelectID = new SelectQuery(From);
        // читаем
        SelectID.Expressions.put("lastid",new FieldSourceExpr(From,Key.Name));
        Integer FreeID = (Integer)Adapter.ExecuteSelect(SelectID).get(0).get("lastid");
        // замещаем
        SelectID.Expressions.put("lastid",new ValueSourceExpr(FreeID+1));
        Adapter.UpdateRecords(SelectID);
        return FreeID;
    }
}

// таблица куда виды складывают свои объекты
class ViewTable extends Table {
    ViewTable(Integer iObjects) {
        super("viewtable"+iObjects.toString());
        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            KeyFields.add(ObjKeyField);
        }
        
        View = new KeyField("viewid","integer");
        KeyFields.add(View);
    }
            
    List<KeyField> Objects;
    KeyField View;
    
    void DropViewID(DataAdapter Adapter,Integer ViewID) throws SQLException {
        FromTable Delete = new FromTable(Name);
        Delete.Wheres.add(new FieldValueWhere(ViewID,View.Name));
        Adapter.DeleteRecords(Delete);
    }
}

class ChangeTable extends Table {

    Collection<KeyField> Objects;
    KeyField Session;
    KeyField Property;
    Field Value;
    Field PrevValue;
    // системное поля, по сути для MaxGroupProperty
    Field SysValue;
    
    ChangeTable(Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super("changetable"+iObjects+"t"+iDBType);

        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            KeyFields.add(ObjKeyField);
        }
        
        Session = new KeyField("session","integer");
        KeyFields.add(Session);
        
        Property = new KeyField("property","integer");
        KeyFields.add(Property);
        
        Value = new Field("value",DBTypes.get(iDBType));
        PropFields.add(Value);

        PrevValue = new Field("prevvalue",DBTypes.get(iDBType));
        PropFields.add(PrevValue);

        SysValue = new Field("sysvalue",DBTypes.get(iDBType));
        PropFields.add(SysValue);
    }
}

class TableFactory extends TableImplement{
    
    ObjectTable ObjectTable;
    IDTable IDTable;
    List<ViewTable> ViewTables;
    List<List<ChangeTable>> ChangeTables;
    
    // для отладки
    boolean ReCalculateAggr = false;
    
    ChangeTable GetChangeTable(Integer Objects, String DBType) {
        return ChangeTables.get(Objects-1).get(DBTypes.indexOf(DBType));
    }

    List<String> DBTypes;
    
    TableFactory() {
        ObjectTable = new ObjectTable();
        IDTable = new IDTable();
        ViewTables = new ArrayList();
        ChangeTables = new ArrayList();
        
        for(int i=1;i<5;i++)
            ViewTables.add(new ViewTable(i));

        DBTypes = new ArrayList();
        DBTypes.add("integer");
        DBTypes.add("char(50)");
        
        for(int i=1;i<5;i++) {
            List<ChangeTable> ObjChangeTables = new ArrayList();
            ChangeTables.add(ObjChangeTables);
            for(int j=0;j<DBTypes.size();j++)
                ObjChangeTables.add(new ChangeTable(i,j,DBTypes));
        }
    }

    void IncludeIntoGraph(TableImplement IncludeItem) {
        Set<TableImplement> Checks = new HashSet<TableImplement>();
        RecIncludeIntoGraph(IncludeItem,true,Checks);
    }
    
    void FillDB(DataAdapter Adapter) throws SQLException {
        Integer TableNum = 0;
        Set<TableImplement> TableImplements = new HashSet<TableImplement>();
        FillSet(TableImplements);
        
        Iterator<TableImplement> i = TableImplements.iterator();
        while(i.hasNext()) {
            TableImplement Node = i.next();
            TableNum++;
            Node.Table = new Table("table"+TableNum.toString());
            Node.MapFields = new HashMap<DataPropertyInterface,KeyField>();
            Integer FieldNum = 0;
            Iterator<DataPropertyInterface> it = Node.iterator();
            while(it.hasNext()) {
                FieldNum++;
                KeyField Field = new KeyField("key"+FieldNum.toString(),"integer");
                Node.Table.KeyFields.add(Field);
                Node.MapFields.put(it.next(),Field);
            }
        }
        
        Adapter.CreateTable(ObjectTable);
        Adapter.CreateTable(IDTable);
        Iterator<ViewTable> iv = ViewTables.iterator();
        while(iv.hasNext()) Adapter.CreateTable(iv.next());

        Iterator<List<ChangeTable>> ilc = ChangeTables.iterator();
        while(ilc.hasNext()) {
            Iterator<ChangeTable> ic = ilc.next().iterator();
            while(ic.hasNext()) Adapter.CreateTable(ic.next());
        }

        // закинем одну запись
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InsertKeys.put(IDTable.Key, 0);
        Adapter.InsertRecord(IDTable,InsertKeys,new HashMap<Field,Object>());
    }
}

class BusinessLogics {
    
    BusinessLogics() {
        BaseClass = new ObjectClass(0);
        TableFactory = new TableFactory();
        Properties = new ArrayList();
        PersistentProperties = new HashSet();
        
        BaseClass = new ObjectClass(0);
        
        StringClass = new StringClass(1);
        StringClass.AddParent(BaseClass);
        IntegerClass = new QuantityClass(2);
        IntegerClass.AddParent(BaseClass);
    }
    
    void AddDataProperty(DataProperty Property) {
        Properties.add(Property);
    }

    // получает класс по ID объекта
    Class GetClass(DataAdapter Adapter,Integer idObject) throws SQLException {
        // сначала получаем idClass
        return BaseClass.FindClassID(TableFactory.ObjectTable.GetClassID(Adapter,idObject));
    }

    Class BaseClass;
    Class StringClass;
    IntegralClass IntegerClass;
    
    TableFactory TableFactory;
    Collection<Property> Properties;
    
    Set<AggregateProperty> PersistentProperties;
    
        
    void UpdateAggregations(DataAdapter Adapter,Collection<AggregateProperty> Properties, ChangesSession Session) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)
        
        // нужно из графа зависимостей выделить направленный список аггрегированных св-в (здесь из предположения что список запрашиваемых аггрегаций меньше общего во много раз)
        List<AggregateProperty> UpdateList = new ArrayList();
        Iterator<AggregateProperty> i = Properties.iterator();
        while(i.hasNext()) i.next().FillAggregateList(UpdateList,Session.Properties);
        
        // здесь бежим слева направо определяем изм. InterfaceClassSet (в DataProperty они первичны) - удаляем сразу те у кого null (правда это может убить всю ветку)
        // потом реализуем

        // пробежим вперед пометим свойства которые изменились, но неясно на что
        ListIterator<AggregateProperty> il = UpdateList.listIterator();
        AggregateProperty Property = null;
        while(il.hasNext()) { 
            Property = il.next();
            Property.SessionChanged.put(Session,null);
        }
        // пробежим по которым надо поставим 0
        i = Properties.iterator();
        while(i.hasNext()) i.next().SetChangeType(Session,0);
        // прогоним DataProperty также им 0 поставим чтобы 1 не появлялись
        Iterator<DataProperty> id = Session.Properties.iterator();
        while(id.hasNext()) id.next().SetChangeType(Session,0);

        // бежим по списку (в обратном порядке) заполняем требования, 
        while(Property!=null) {
            Property.FillRequiredChanges(Session);

            if(il.hasPrevious())
                Property = il.previous();
            else
                Property = null;
        }
        
        // прогоним DataProperty предыдущие значения докинуть
        id = Session.Properties.iterator();
        while(id.hasNext()) id.next().UpdateIncrementChanges(Adapter,Session);
        
        // запускаем IncrementChanges для этого списка
        il = UpdateList.listIterator();
        while(il.hasNext()) 
            il.next().IncrementChanges(Adapter, Session);
        
        // дропнем изменения (пока, потом для FormBean'ов понадобится по другому)
        il = UpdateList.listIterator();
        while(il.hasNext())
            il.next().SessionChanged.remove(Session);
    }
    
    // сохраняет из Changes в базу
    void SaveProperties(DataAdapter Adapter,Collection<? extends ObjectProperty> Properties, ChangesSession Session) throws SQLException {
        Iterator<ObjectProperty> i = (Iterator<ObjectProperty>) Properties.iterator();
        while(i.hasNext()) i.next().SaveChanges(Adapter, Session);
    }
    
    boolean Apply(DataAdapter Adapter,ChangesSession Session) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        UpdateAggregations(Adapter,PersistentProperties,Session);

        // записываем Data, затем PersistentProperties в таблицы из сессии
        SaveProperties(Adapter,PersistentProperties,Session);
        SaveProperties(Adapter,Session.Properties,Session);
        
        return true;
    }

    void FillDB(DataAdapter Adapter) throws SQLException {
        // инициализируем таблицы
        TableFactory.FillDB(Adapter);

        // запишем ID'ки
        int IDPropNum = 0;
        Iterator<Property> ip = Properties.iterator();
        while(ip.hasNext())
            ip.next().ID = IDPropNum++;
        
        Set<DataProperty> DataProperties = new HashSet();
        Collection<AggregateProperty> AggrProperties = new ArrayList();
        Map<Table,Integer> Tables = new HashMap<Table,Integer>();
        // закинем в таблицы(создав там все что надо) св-ва
        Iterator<Property> i = Properties.iterator();
        while(i.hasNext()) {
            Property Property = i.next();
            
            // ChangeTable'ы заполним
            if(Property instanceof ObjectProperty)
                ((ObjectProperty)Property).FillChangeTable();;

            if(Property instanceof DataProperty)
                DataProperties.add((DataProperty)Property);
            
            if(Property instanceof AggregateProperty)
                AggrProperties.add((AggregateProperty)Property);

            if(Property instanceof DataProperty || (Property instanceof AggregateProperty && PersistentProperties.contains(Property))) {
                Table Table = ((ObjectProperty)Property).GetTable(null);

                Integer PropNum = Tables.get(Table);
                if(PropNum==null) PropNum = 1;
                PropNum = PropNum + 1;
                Tables.put(Table, PropNum);

                Field PropField = new Field("prop"+PropNum.toString(),Property.GetDBType());
                Table.PropFields.add(PropField);
                ((ObjectProperty)Property).Field = PropField;
            }
        }

        Iterator<Table> it = Tables.keySet().iterator();
        while(it.hasNext()) Adapter.CreateTable(it.next());

        // построим в нужном порядке AggregateProperty и будем заполнять их
        List<AggregateProperty> UpdateList = new ArrayList();
        Iterator<AggregateProperty> ia = AggrProperties.iterator();
        while(ia.hasNext()) ia.next().FillAggregateList(UpdateList,DataProperties);
        ia = UpdateList.iterator();
        Integer ViewNum = 0;
        while(ia.hasNext()) {
            AggregateProperty Property = ia.next();
            if(Property instanceof GroupProperty)
                ((GroupProperty)Property).FillDB(Adapter,ViewNum++);
        }
        
        // создадим dumb
        Table DumbTable = new Table("dumb");
        DumbTable.KeyFields.add(new KeyField("dumb","integer"));
        Adapter.CreateTable(DumbTable);
        Adapter.Execute("INSERT INTO dumb (dumb) VALUES (1)");
    }
    
    void CheckPersistent(DataAdapter Adapter) throws SQLException {
        Iterator<AggregateProperty> i = PersistentProperties.iterator();
        while(i.hasNext()) {
            AggregateProperty Property = i.next();
            Property.CheckAggregation(Adapter,Property.OutName);
        }        
    }

    // функционал по заполнению св-в по номерам, нужен для BL
    
    LDP AddDProp(Class Value,Class ...Params) {
        DataProperty Property = new DataProperty(TableFactory,Value);
        LDP ListProperty = new LDP(Property);
        for(Class Int : Params) {
            ListProperty.AddInterface(Int);
        }
        AddDataProperty(Property);
        return ListProperty;
    }

    LSFP AddSFProp(String Formula,Integer Params) {
        StringFormulaProperty Property = new StringFormulaProperty(Formula);
        LSFP ListProperty = new LSFP(Property,IntegerClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    LMFP AddMFProp(Integer Params) {
        MultiplyFormulaProperty Property = new MultiplyFormulaProperty();
        LMFP ListProperty = new LMFP(Property,IntegerClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    
    List<PropertyInterfaceImplement> ReadPropImpl(LP MainProp,Object ...Params) {
        List<PropertyInterfaceImplement> Result = new ArrayList<PropertyInterfaceImplement>();
        int WaitInterfaces = 0, MainInt = 0;
        PropertyMapImplement MapRead = null;
        LP PropRead = null;
        for(Object P : Params) {
            if(P instanceof Integer) {
                // число может быть как ссылкой на родной интерфейс так и 
                PropertyInterface PropInt = MainProp.ListInterfaces.get((Integer)P-1);
                if(WaitInterfaces==0) {
                    // родную берем 
                    Result.add(PropInt);
                } else {
                    // докидываем в маппинг
                    MapRead.Mapping.put(PropRead.ListInterfaces.get(PropRead.ListInterfaces.size()-WaitInterfaces), PropInt);
                    WaitInterfaces--;
                }
            } else {
               // имплементация, типа LP
               PropRead = (LP)P;
               MapRead = new PropertyMapImplement(PropRead.Property);
               WaitInterfaces = PropRead.ListInterfaces.size();
               Result.add(MapRead);
            }
        }
        
        return Result;
    }

    LRP AddRProp(LP MainProp, int IntNum, Object ...Params) {
        RelationProperty Property = new RelationProperty(TableFactory,MainProp.Property);
        LRP ListProperty = new LRP(Property,IntNum);
        int MainInt = 0;
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(ListProperty,Params);
        ListIterator<PropertyInterfaceImplement> i = PropImpl.listIterator();
        while(i.hasNext()) {
            Property.Implements.Mapping.put(MainProp.ListInterfaces.get(MainInt), i.next());
            MainInt++;
        }
        Properties.add(Property);

        return ListProperty;
    }
    
    LGP AddGProp(LP GroupProp,boolean Sum,Object ...Params) {
        GroupProperty Property = null;
        if(Sum)
            Property = new SumGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        else
            Property = new MaxGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        LGP ListProperty = new LGP(Property,GroupProp);
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(GroupProp,Params);
        ListIterator<PropertyInterfaceImplement> i = PropImpl.listIterator();
        while(i.hasNext()) ListProperty.AddInterface(i.next());
        Properties.add(Property);
        
        return ListProperty;
    }

    LRP AddLProp(int ListType, int IntNum, Object ...Params) {
        ListProperty Property = null;
        switch(ListType) {
            case 0:
                Property = new MaxListProperty(TableFactory);
                break;
            case 1:
                Property = new SumListProperty(TableFactory);
                break;
            case 2:
                Property = new OverrideListProperty(TableFactory);
                break;
        }
        
        LRP ListProperty = new LRP(Property,IntNum);

        for(int i=0;i<IntNum;i++) {
            Integer Offs = i*(IntNum+2);
            LP OpImplement = (LP)Params[Offs+1];
            PropertyMapImplement Operand = new PropertyMapImplement(OpImplement.Property);
            for(int j=0;j<IntNum;j++)
                Operand.Mapping.put(OpImplement.ListInterfaces.get(((Integer)Params[Offs+2+j])-1),ListProperty.ListInterfaces.get(j));
            Property.Operands.add(Operand);
            Property.Coeffs.put(Operand,(Integer)Params[Offs]);
        }
        Properties.add(Property);

        return ListProperty;
    }

    void RegGClass(LGP GroupProp,Object ...iParams) {
        int iInt=0;
        boolean bInt=true;
        for(Object i : iParams) {
            if(bInt) {
                iInt = (Integer)i-1;
                bInt = false;
            } else {
                ((GroupProperty)GroupProp.Property).ToClasses.put(GroupProp.GroupProperty.ListInterfaces.get(iInt),(Class)i);
                bInt = true;
            }
        }        
    }
    
    // генерирует белую БЛ
    void OpenBL(boolean Implements,boolean Persistent) {

        Class Base = new ObjectClass(3);
        Base.AddParent(BaseClass);
        Class Article = new ObjectClass(4);
        Article.AddParent(Base);
        Class Store = new ObjectClass(5);
        Store.AddParent(Base);
        Class Document = new ObjectClass(6);
        Document.AddParent(Base);
        Class PrihDocument = new ObjectClass(7);
        PrihDocument.AddParent(Document);
        Class RashDocument = new ObjectClass(8);
        RashDocument.AddParent(Document);
        Class ArticleGroup = new ObjectClass(9);
        ArticleGroup.AddParent(Base);
        
        LDP Name = AddDProp(StringClass,Base);
        LDP DocStore = AddDProp(Store,Document);
        LDP Quantity = AddDProp(IntegerClass,Document,Article);
        LDP PrihQuantity = AddDProp(IntegerClass,PrihDocument,Article);
        LDP RashQuantity = AddDProp(IntegerClass,RashDocument,Article);
        LDP ArtToGroup = AddDProp(ArticleGroup,Article);
        LDP DocDate = AddDProp(IntegerClass,Document);

        LSFP Dirihle = AddSFProp("(CASE WHEN prm1<prm2 THEN 1 ELSE 0 END)",2);
        LMFP Multiply = AddMFProp(2);

        Name.Property.OutName = "имя";
        DocStore.Property.OutName = "склад";
        Quantity.Property.OutName = "кол-во";
        PrihQuantity.Property.OutName = "кол-во прих.";
        RashQuantity.Property.OutName = "кол-во расх.";
        ArtToGroup.Property.OutName = "гр. тов";
        DocDate.Property.OutName = "дата док.";

        LRP StoreName = AddRProp(Name,1,DocStore,1);
        StoreName.Property.OutName = "имя склада";
        LRP ArtGroupName = AddRProp(Name,1,ArtToGroup,1);
        ArtGroupName.Property.OutName = "имя гр. тов.";

        LRP DDep = AddRProp(Dirihle,2,DocDate,1,DocDate,2);
        DDep.Property.OutName = "предш. док.";

        LRP QDep = AddRProp(Multiply,3,DDep,1,2,Quantity,1,3);
        QDep.Property.OutName = "изм. баланса";

        LGP GSum = AddGProp(QDep,true,2,3);
        GSum.Property.OutName = "остаток до операции";

        LGP GP = AddGProp(Quantity,true,DocStore,1,2);
        GP.Property.OutName = "сумм кол-во док. тов.";
        LGP GAP = AddGProp(GP,true,2);
        GAP.Property.OutName = "сумм кол-во тов.";
        LGP G2P = AddGProp(Quantity,true,DocStore,1,ArtToGroup,2);
        G2P.Property.OutName = "скл-гр. тов";

        LGP PrihArtStore = AddGProp(PrihQuantity,true,DocStore,1,2);
        PrihArtStore.Property.OutName = "приход по складу";

        LGP RashArtStore = AddGProp(RashQuantity,true,DocStore,1,2);
        RashArtStore.Property.OutName = "расход по складу";

        LRP OstArtStore = AddLProp(1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);
        OstArtStore.Property.OutName = "остаток по складу";

        LGP OstArt = AddGProp(OstArtStore,true,2);
        OstArt.Property.OutName = "остаток по товару";

        LGP MaxPrih = AddGProp(PrihQuantity,false,DocStore,1,ArtToGroup,2);
        MaxPrih.Property.OutName = "макс. приход по гр. тов.";

        LRP MaxOpStore = AddLProp(0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);
        MaxOpStore.Property.OutName = "макс. операция";
        
        LGP SumMaxArt = AddGProp(MaxOpStore,true,2);
        SumMaxArt.Property.OutName = "сумма макс. операция";

        TableImplement Include;
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Base));
        Include.add(new DataPropertyInterface(Base));
        TableFactory.IncludeIntoGraph(Include);

        if(Implements) {
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Base));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Article));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Store));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(ArticleGroup));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Article));
            Include.add(new DataPropertyInterface(Document));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Article));
            Include.add(new DataPropertyInterface(Store));
            TableFactory.IncludeIntoGraph(Include);
        }

        if(Persistent) {
            PersistentProperties.add((AggregateProperty)GP.Property);
            PersistentProperties.add((AggregateProperty)GAP.Property);
            PersistentProperties.add((AggregateProperty)G2P.Property);
            PersistentProperties.add((AggregateProperty)GSum.Property);
            PersistentProperties.add((AggregateProperty)OstArtStore.Property);
            PersistentProperties.add((AggregateProperty)OstArt.Property);
            PersistentProperties.add((AggregateProperty)MaxPrih.Property);
            PersistentProperties.add((AggregateProperty)MaxOpStore.Property);
            PersistentProperties.add((AggregateProperty)SumMaxArt.Property);
        }
    }

    void FullDBTest()  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        
        // сгенерить БЛ
        // сгенерить физ. модель
        // сгенерить persistent аггрегации
        OpenBL(true,false);

        
        while(true) {
            DataAdapter Adapter = new DataAdapter();
            Adapter.Connect("");
        
            FillDB(Adapter);

            // сгенерить объекты (их пока тестить не надо)
            List<Class> Classes = new ArrayList();
            BaseClass.FillClassList(Classes);
            Iterator<Class> i = Classes.iterator();
            while(i.hasNext()) {
                Class ObjectClass = i.next();
                for(int j=0;j<6;j++)
                    ObjectClass.AddObject(Adapter,TableFactory);
            }

            Random Randomizer = new Random();
            // запустить ChangeDBTest
            ChangeDBTest(Adapter,20,Randomizer);
        
            Adapter.Disconnect();
        }
    }
    
    
    void ChangeDBTest(DataAdapter Adapter,Integer MaxIterations,Random Randomizer) throws SQLException {
        
        // сначала список получим
        List<DataProperty> DataProperties = new ArrayList();
        Iterator<Property> i = Properties.iterator();
        while(i.hasNext()) {
            Property Property = i.next();
            if(Property instanceof DataProperty)
                DataProperties.add((DataProperty)Property);
        }
        
//        Randomizer.setSeed(1);
        int Iterations = 2;
        while(Iterations<MaxIterations) {
            ChangesSession Session = new ChangesSession(Iterations++);
            System.out.println(Iterations);

            int PropertiesChanged = Randomizer.nextInt(7)+1;
            for(int ip=0;ip<PropertiesChanged;ip++) {
                // берем случайные n св-в
                DataProperty ChangeProp = DataProperties.get(Randomizer.nextInt(DataProperties.size()));
                int NumChanges = Randomizer.nextInt(4)+1;
                for(int in=0;in<NumChanges;in++) {
                    Object ValueObject = ChangeProp.Value.GetRandomObject(Adapter,TableFactory,Randomizer,Iterations);
/*                    // теперь определяем класс найденного объекта
                    Class ValueClass = null;
                    if(ChangeProp.Value instanceof ObjectClass)
                        ValueClass = BaseClass.FindClassID(ValueObject);
                    else
                        ValueClass = ChangeProp.Value;*/
                        
                    InterfaceClassSet InterfaceSet = ChangeProp.GetClassSet(null);
                    // определяем входные классы
                    InterfaceClass Classes = InterfaceSet.get(Randomizer.nextInt(InterfaceSet.size()));
                    // генерим рандомные объекты этих классов
                    Map<DataPropertyInterface,Integer> Keys = new HashMap();
                    Iterator<DataPropertyInterface> ii = ChangeProp.Interfaces.iterator();
                    while(ii.hasNext()) {
                        DataPropertyInterface Interface = ii.next();
                        Keys.put(Interface,(Integer)Classes.get(Interface).GetRandomObject(Adapter,TableFactory,Randomizer,0));
                    }
                    
                    ChangeProp.ChangeProperty(Adapter,Keys,ValueObject,Session);
                }
            }
            
            Apply(Adapter,Session);
            CheckPersistent(Adapter);
        }
    }
}
