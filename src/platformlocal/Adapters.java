/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

class DataAdapter {
    
    Connection Connection;

    void Connect(String ConnectionString) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        java.lang.Class.forName("net.sourceforge.jtds.jdbc.Driver"); 
        Connection = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");

        try {
            Execute("DROP DATABASE TestPlat");
        } catch(Exception e) {            
        }
        
        Execute("CREATE DATABASE TestPlat");
        Execute("USE TestPlat");
    }
    
    void CreateTable(Table Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        Iterator<KeyField> ik = Table.KeyFields.iterator();
        while(ik.hasNext()) {
            KeyField Key = ik.next();
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare();
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        Iterator<Field> it = Table.PropFields.iterator();
        while(it.hasNext()) 
            CreateString = CreateString+',' + it.next().GetDeclare();
        CreateString = CreateString + ",CONSTRAINT PK_" + Table.Name + " PRIMARY KEY CLUSTERED (" + KeyString + ")";

        Execute("CREATE TABLE "+Table.Name+" ("+CreateString+")");
    }
    
    void Execute(String ExecuteString) throws SQLException {
        Statement Statement = Connection.createStatement();
        Statement.execute(ExecuteString);                
    }
    
    void CreateView(String ViewName,Query Select) throws SQLException {
        Execute("CREATE VIEW "+ViewName+" AS "+Select.GetSelect(new ArrayList()));
    }
    
    void InsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<Field,Object> PropFields) throws SQLException {
        
        String InsertString = "";
        String ValueString = "";
        
        // пробежим по KeyFields'ам
        Iterator<KeyField> ik = Table.KeyFields.iterator();
        while(ik.hasNext()) {
            KeyField Key = ik.next();
            InsertString = (InsertString.length()==0?"":InsertString+',') + Key.Name;
            ValueString = (ValueString.length()==0?"":ValueString+',') + KeyFields.get(Key);
        }
        
        // пробежим по Fields'ам
        Iterator<Field> i = PropFields.keySet().iterator();
        while(i.hasNext()) {
            Field Prop = i.next();
            Object Value = PropFields.get(Prop);
            InsertString = InsertString+","+Prop.Name;
            ValueString = ValueString+","+(Value instanceof String?"'"+(String)Value+"'":Value.toString());
        }

        Execute("INSERT INTO "+Table.Name+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<Field,Object> PropFields) throws SQLException {

        FromTable From = new FromTable(Table.Name);
        SelectQuery Select = new SelectQuery(From);
        // сначала закинем KeyField'ы и прогоним Select
        Iterator<KeyField> ik = Table.KeyFields.iterator();
        while(ik.hasNext())  {
            KeyField Key = ik.next();
            Select.From.Wheres.add(new FieldValueWhere(KeyFields.get(Key),Key.Name));
        }
        
        Select.Expressions.put("isrec",new ValueSourceExpr(1));

        if(ExecuteSelect(Select).size()>0) {
            // есть запись нужно Update лупить
            Select.Expressions.clear();
            Iterator<Field> ip = PropFields.keySet().iterator();
            while(ip.hasNext()) {
                Field Prop = ip.next();
                Object PropValue = PropFields.get(Prop);
                if(PropValue!=null) Select.Expressions.put(Prop.Name,new ValueSourceExpr(PropValue));
            }
            UpdateRecords(Select);
        } else
            // делаем Insert
            InsertRecord(Table,KeyFields,PropFields);
                 
    }

    void UpdateRecords(SelectQuery Select) throws SQLException {
        Execute(Select.GetUpdate());
    }
    
    void DeleteRecords(FromTable Table) throws SQLException {
        Execute(Table.GetDelete());
    }
    
    void InsertSelect(Table InsertTo,Query Select) throws SQLException {
        Collection<String> ResultFields = new ArrayList();
        String SelectString = Select.GetSelect(ResultFields);
        String InsertString = "";
        Iterator<String> i = ResultFields.iterator();
        while(i.hasNext()) InsertString = (InsertString.length()==0?"":InsertString+",") + i.next();
//        System.out.println("INSERT INTO "+InsertTo.Name+" ("+InsertString+") "+SelectString);
        Execute("INSERT INTO "+InsertTo.Name+" ("+InsertString+") "+SelectString);
    }

    List<Map<String,Object>> ExecuteSelect(Query Select) throws SQLException {
        List<Map<String,Object>> ExecResult = new ArrayList<Map<String,Object>>();
        Statement Statement = Connection.createStatement();
        Collection<String> ResultFields = new ArrayList();
        String SelectString = Select.GetSelect(ResultFields);
        try {
            ResultSet Result = Statement.executeQuery(SelectString);
            try {
                while(Result.next()) {
                    Iterator<String> is = ResultFields.iterator();
                    Map<String,Object> RowMap = new HashMap<String,Object>();
                    while(is.hasNext()) {
                        String SelectExpr = is.next();
                        RowMap.put(SelectExpr,Result.getObject(SelectExpr));
                    }
                    ExecResult.add(RowMap);
                }
            } finally {
               Result.close();
            }
        } finally {
            Statement.close();
        }
        
        return ExecResult;
    }
    
    void OutSelect(Query Select) throws SQLException {
        // выведем на экран
        Collection<String> ResultFields = new ArrayList();
        System.out.println(Select.GetSelect(ResultFields));

        List<Map<String,Object>> Result = ExecuteSelect(Select);
        ListIterator<Map<String,Object>> ir = Result.listIterator();
        while(ir.hasNext()) {
            Map<String,Object> RowMap = ir.next();
            Iterator<String> is = ResultFields.iterator();
            while(is.hasNext()) {
                System.out.print(RowMap.get(is.next()));
                System.out.print(" ");
            }
            System.out.println("");
        }
    }
    
    void Disconnect() throws SQLException {
        Connection.close();
    }
}

class Field {
    String Name;
    String Type;
    
    Field(String iName,String iType) {Name=iName;Type=iType;}
    
    String GetDeclare() {
        return Name + " " + Type;
    }
}

class KeyField extends Field {
    KeyField(String iName,String iType) {super(iName,iType);}
}
        
class Table {
    String Name;
    
    Table(String iName) {
        Name=iName;
        KeyFields = new ArrayList<KeyField>();
        PropFields = new ArrayList<Field>();
        MapFields = new HashMap<String,Field>();
    }
    
    Map<String,Field> MapFields;
    
    Collection<KeyField> KeyFields;
    Collection<Field> PropFields;
}


/*
   void IncrementChanges(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        // алгоритм пока такой :
        // 1. берем GROUPPROPERTY(изм на +) по аналогии с реляционными
        // G = SS(с пуст.) 1 SUM(+)
        // 2. для новых св-в делаем GROUPPROPERTY(все) так же как и для реляционных св-в FULL JOIN'ы - JOIN'ов с "перегр." подмн-вами (единственный способ сразу несколько изменений "засечь") (и GROUP BY по ISNULL справо налево ключей)
        // A = SS(без пуст.) 1 SUM(+)
        // 3. для старых св-в GROUPPROPERTY(все) FULL JOIN (JOIN "перегр." измененных с LEFT JOIN'ами старых) (без подмн-в) (и GROUP BY по ISNULL(обычных JOIN'ов,LEFT JOIN'a изм.))
        // A P (без SS/без пуст.) -1 SUM(+)
        // все UNION ALL и GROUP BY или же каждый GROUP BY а затем FULL JOIN на +

        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        
        List<GroupPropertyInterface> ChangedProperties = new ArrayList();
        while(im.hasNext()) {
            GroupPropertyInterface Interface = im.next();
            // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
            if(Interface.Implement.MapHasChanges(Session)) ChangedProperties.add(Interface);
        }
        
        // ничего не изменилось вываливаемся
        if(ChangedProperties.size()==0 && !GroupProperty.HasChanges(Session)) return;

        ChangeTable Table = StartChangeTable(Adapter,Session);
        // конечный результат, с ключами и выражением 
        UnionQuery ResultQuery = GetChangeUnion(Table,Session);

        for(int ij=(GroupProperty.HasChanges(Session)?1:2);ij<=(ChangedProperties.size()==0?1:3);ij++) {
            UnionQuery DataQuery = new UnionQuery();
                    
            // заполняем ключи и значения
            int DataKeysNum = 1;
            Map<PropertyInterface,String> DataKeysMap = new HashMap();
            Iterator<PropertyInterface> in = GroupProperty.Interfaces.iterator();
            while(in.hasNext()) {
                String Field = "dkey" + DataKeysNum++;
                DataKeysMap.put(in.next(), Field);
                DataQuery.Keys.add(Field);
            }
            im = Interfaces.iterator();
            while (im.hasNext()) DataQuery.Values.add(ChangeTableMap.get(im.next()).Name);
            DataQuery.Values.add(Table.Value.Name);
            
            boolean GroupChanged = (ij==1);
            Integer Coeff = 0;
            Integer GroupType = 0;
            ListIterator<List<GroupPropertyInterface>> il = null;
            // Subsets
            if(ij<=2) {
                il = (new SetBuilder<GroupPropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();
                GroupType = 0;
                Coeff = 1;
                // пустое при 2 не рассматриваем
                if(ij==2) il.next();
            }
            else {
                List<List<GroupPropertyInterface>> ChangedList = new ArrayList();
                im = ChangedProperties.iterator();
                while(im.hasNext()) {
                    List<GroupPropertyInterface> SingleList = new ArrayList();
                    SingleList.add(im.next());
                    ChangedList.add(SingleList);
                }
                il = ChangedList.listIterator();
                GroupType = 2;
                Coeff = -1;
            }
            
            while(il.hasNext()) {
                List<GroupPropertyInterface> ChangeProps = il.next();
                SelectQuery SubQuery = new SelectQuery(null);
                JoinList Joins = new JoinList();

                // обнуляем, закидываем GroupProperty,
                Map<PropertyInterface,SourceExpr> GroupImplement = new HashMap();
                
                // значение
                SubQuery.Expressions.put(Table.Value.Name,(GroupChanged?GroupProperty.ChangedJoinSelect(Joins,GroupImplement,Session,1):GroupProperty.JoinSelect(Joins,GroupImplement,false)));

                // значения интерфейсов
                im = Interfaces.iterator();
                while (im.hasNext()) {
                    GroupPropertyInterface Interface = im.next();
                    SubQuery.Expressions.put(ChangeTableMap.get(Interface).Name,Interface.Implement.MapJoinSelect(Joins,GroupImplement,false,(ChangeProps.contains(Interface)?Session:null),GroupType));
                }
                
                // значения ключей базовые
                in = GroupProperty.Interfaces.iterator();
                while (in.hasNext()) {
                    PropertyInterface Interface = in.next();
                    SubQuery.Expressions.put(DataKeysMap.get(Interface),GroupImplement.get(Interface));
                }

                // закинем Join'ы как обычно
                Iterator<From> is = Joins.iterator();
                SubQuery.From = is.next();
                while(is.hasNext()) SubQuery.From.Joins.add(is.next());

                DataQuery.Unions.add(SubQuery);
            }
            
            FromQuery FromDataQuery = new FromQuery(DataQuery);
            GroupQuery GroupQuery = new GroupQuery(FromDataQuery);
            im = Interfaces.iterator();
            while (im.hasNext()) {
                String KeyField = ChangeTableMap.get(im.next()).Name;
                GroupQuery.GroupBy.put(KeyField,new FieldSourceExpr(FromDataQuery,KeyField));
            }
            GroupQuery.AggrExprs.put(Table.Value.Name,new GroupExpression(new FieldSourceExpr(FromDataQuery,Table.Value.Name),"SUM"));

            ResultQuery.Unions.add(GroupQuery);
            ResultQuery.SumCoeffs.put(GroupQuery,Coeff);
        }

        Adapter.InsertSelect(Table,ResultQuery);
        // помечаем изменение в сессии
        SessionChanged.put(Session,1);
     }
}*/