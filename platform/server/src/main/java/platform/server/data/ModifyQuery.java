package platform.server.data;

import platform.server.data.query.CompiledQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.sql.SQLExecute;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class ModifyQuery {
    public Table table;
    JoinQuery<KeyField, PropertyField> change;

    public ModifyQuery(Table iTable, JoinQuery<KeyField, PropertyField> iChange) {
        table = iTable;
        change = iChange;
    }

    public SQLExecute getUpdate(SQLSyntax syntax) {

        int updateModel = syntax.updateModel();
        CompiledQuery<KeyField, PropertyField> changeCompile;
        Collection<String> whereSelect;
        String update;
        
        if(updateModel==2) {
            // Oracl'вская модель Update'а
            changeCompile = change.compile(syntax);
            whereSelect = new ArrayList<String>(changeCompile.whereSelect);
            String fromSelect = changeCompile.from;

            for(KeyField Key : table.keys)
                whereSelect.add(table.getName(syntax)+"."+Key.name +"="+ changeCompile.keySelect.get(Key));

            List<KeyField> keyOrder = new ArrayList<KeyField>();
            List<PropertyField> propertyOrder = new ArrayList<PropertyField>();
            String SelectString = syntax.getSelect(fromSelect, Source.stringExpr(
                    Source.mapNames(changeCompile.keySelect,changeCompile.keyNames,keyOrder),
                    Source.mapNames(changeCompile.propertySelect,changeCompile.propertyNames,propertyOrder)),
                    Source.stringWhere(whereSelect),"","","");

            String setString = "";
            for(KeyField field : keyOrder)
                setString = (setString.length()==0?"":setString+",") + field.name;
            for(PropertyField field : propertyOrder)
                setString = (setString.length()==0?"":setString+",") + field.name;

            update = "UPDATE " + table.getName(syntax) + " SET ("+setString+") = ("+SelectString+") WHERE EXISTS ("+SelectString+")";
        } else {
            if(updateModel==1) {
                // SQL-серверная модель когда она подхватывает первый Join и старую таблицу уже не вилит
                // построим JoinQuery куда переJoin'им все эти поля (оптимизатор уберет все дублирующиеся таблицы)
                JoinQuery<KeyField, PropertyField> updateQuery = new JoinQuery<KeyField, PropertyField>(table.keys);
                Join<KeyField, PropertyField> tableJoin = new Join<KeyField, PropertyField>(table, updateQuery);
                tableJoin.noAlias = true;
                updateQuery.and(tableJoin.inJoin);

                Join<KeyField, PropertyField> changeJoin = new Join<KeyField, PropertyField>(change, updateQuery);
                updateQuery.and(changeJoin.inJoin);
                for(PropertyField changeField : change.properties.keySet())
                    updateQuery.properties.put(changeField, changeJoin.exprs.get(changeField));
                changeCompile = updateQuery.compile(syntax);
                whereSelect = changeCompile.whereSelect;
            } else {
                changeCompile = change.compile(syntax);

                whereSelect = new ArrayList<String>(changeCompile.whereSelect);
                for(KeyField key : table.keys)
                    whereSelect.add(table.getName(syntax)+"."+key.name +"="+ changeCompile.keySelect.get(key));
            }

            String whereString = "";            
            for(String where : whereSelect)
                whereString = (whereString.length()==0?"":whereString+" AND ") + where;

            String setString = "";
            for(Map.Entry<PropertyField,String> setProperty : changeCompile.propertySelect.entrySet())
                setString = (setString.length()==0?"":setString+",") + setProperty.getKey().name + "=" + setProperty.getValue();

            update = "UPDATE " + syntax.getUpdate(table.getName(syntax)," SET "+setString, changeCompile.from,(whereString.length()==0?"":" WHERE "+whereString));
        }

        return new SQLExecute(update,changeCompile.getQueryParams());        
    }

    public SQLExecute getInsertLeftKeys(SQLSyntax syntax) {

        // делаем для этого еще один запрос
        JoinQuery<KeyField, PropertyField> leftKeysQuery = new JoinQuery<KeyField, PropertyField>(table.keys);
        // при Join'им ModifyQuery
        leftKeysQuery.and(new Join<KeyField, PropertyField>(change,leftKeysQuery).inJoin);
        // исключим ключи которые есть
        leftKeysQuery.and((new Join<KeyField, PropertyField>(table,leftKeysQuery)).inJoin.not());

        return (new ModifyQuery(table,leftKeysQuery)).getInsertSelect(syntax);
    }

    public SQLExecute getInsertSelect(SQLSyntax syntax) {

        CompiledQuery<KeyField, PropertyField> changeCompile = change.compile(syntax);

        String insertString = "";
        for(KeyField keyField : changeCompile.keyOrder)
            insertString = (insertString.length()==0?"":insertString+",") + keyField.name;
        for(PropertyField propertyField : changeCompile.propertyOrder)
            insertString = (insertString.length()==0?"":insertString+",") + propertyField.name;

        return new SQLExecute("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") " + changeCompile.select,changeCompile.getQueryParams());
    }

    void outSelect(DataSession Session) throws SQLException {
        System.out.println("Table");
        table.outSelect(Session);
        System.out.println("Source");
        change.outSelect(Session);
    }
}
