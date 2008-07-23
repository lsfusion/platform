/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ME2
 */

// constraint
abstract class Constraint {
    
    abstract boolean Check(DataAdapter Adapter,ChangesSession Session,ObjectProperty Property) throws SQLException;

}

abstract class ValueConstraint extends Constraint {
    
    ValueConstraint(int iInvalid) {Invalid=iInvalid;}

    int Invalid;

    boolean Check(DataAdapter Adapter,ChangesSession Session,ObjectProperty Property) throws SQLException {

        JoinList Joins = new JoinList();
        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
        
        SourceExpr ValueExpr = Property.ChangedJoinSelect(Joins,JoinImplement,Session,0,false);

        Iterator<From> i = Joins.iterator();
        SelectQuery Changed = new SelectQuery(i.next());
        while(i.hasNext()) Changed.From.Joins.add(i.next());

        // закинем условие на то что мы ищем
        Changed.From.Wheres.add(new FieldExprCompareWhere(new FieldSourceExpr(Changed.From,Property.ChangeTable.Value.Name), 
            (Property.ChangeTable.Value.Type.equals("integer")?0:""),Invalid));

        Map<PropertyInterface,String> KeyFields = new HashMap();
        Integer KeyNum = 0;
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            String KeyField = "key" + (KeyNum++);
            Changed.Expressions.put(KeyField,JoinImplement.get(Interface));
            KeyFields.put(Interface,KeyField);
        }
        Changed.Expressions.put("value",ValueExpr);
        
        List<Map<String,Object>> Result = Adapter.ExecuteSelect(Changed);
        if(Result.size()>0) {
            System.out.println("Ограничение на св-во "+Property.OutName+" нарушено");
            for(Map<String,Object> Row : Result) {
                System.out.print("Объекты : ");
                for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces)
                    System.out.print(Row.get(KeyFields.get(Interface))+" ");
                System.out.print("Значение : "+Row.get("value"));
                System.out.println();
            }
            
            return false;
        } else
            return true;
    }
}

// != 0 или !="      "
class EmptyConstraint extends ValueConstraint {
    EmptyConstraint() {super(5);}
}

// <= 0 или <= ''
class NotEmptyConstraint extends ValueConstraint {
    NotEmptyConstraint() {super(4);}
}

// < 0
class PositiveConstraint extends ValueConstraint {
    PositiveConstraint() {super(2);}
}

// >= 0
class UniqueConstraint extends Constraint {
    
    boolean Check(DataAdapter Adapter,ChangesSession Session,ObjectProperty Property) throws SQLException {
        
        // надо проверить для каждого что старых нету
        // изменения JOIN'им (ст. запрос FULL JOIN новый) ON изм. зн = новому зн. WHERE код изм. = код нов. и ключи не равны и зн. не null

        JoinList Joins = new JoinList();
        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
        SourceExpr ChangedExpr = Property.ChangedJoinSelect(Joins,JoinImplement,Session,0,false);

        // заполним названия полей в которые будем Select'ать
        String Value = "joinvalue";
        Map<PropertyInterface,String> MapFields = new HashMap();
        FromQuery FromResultQuery = new FromQuery(Property.UpdateUnionQuery(Session,Value,MapFields));
        FromResultQuery.Wheres.add(new FieldWhere(ChangedExpr,Value));
        
        Joins.add(FromResultQuery);
        
        Iterator<From> i = Joins.iterator();
        SelectQuery Changed = new SelectQuery(i.next());
        while(i.hasNext()) Changed.From.Joins.add(i.next());
        
        // равны значения
        Changed.From.Wheres.add(new FieldExprCompareWhere(ChangedExpr,new FieldSourceExpr(FromResultQuery,Value),0));
        Changed.From.Wheres.add(new SourceIsNullWhere(ChangedExpr,true));

        // не равны ключи
        Where OrDiffKeys = null;
        
        Map<PropertyInterface,String> KeyFields = new HashMap();
        Integer KeyNum = 0;
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            String KeyField = "newkey" + (KeyNum++);
            SourceExpr KeyExpr = JoinImplement.get(Interface);
            Changed.Expressions.put(KeyField,KeyExpr);
            KeyFields.put(Interface,KeyField);
            
            String MapField = MapFields.get(Interface);
            SourceExpr MapExpr = new FieldSourceExpr(FromResultQuery,MapField);
            Changed.Expressions.put(MapField,MapExpr);
            
            // не равны ключи
            Where KeyDiff = new FieldExprCompareWhere(KeyExpr,MapExpr,5);
            if(OrDiffKeys==null)
                OrDiffKeys = KeyDiff;
            else
                OrDiffKeys = new FieldOPWhere(KeyDiff,OrDiffKeys,false);
        }
        Changed.From.Wheres.add(OrDiffKeys);
        Changed.Expressions.put("value",ChangedExpr);

        List<Map<String,Object>> Result = Adapter.ExecuteSelect(Changed);
        if(Result.size()>0) {
            System.out.println("Уникальное ограничение на св-во "+Property.OutName+" нарушено");
            for(Map<String,Object> Row : Result) {
                System.out.print("Объекты (1,2) : ");
                for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces)
                    System.out.print(Row.get(KeyFields.get(Interface))+","+Row.get(MapFields.get(Interface))+" ");
                System.out.print("Значение : "+Row.get("value"));
                System.out.println();
            }
            
            return false;
        } else
            return true;
    }    
}
