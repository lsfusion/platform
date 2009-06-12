package platform.server.data;

import platform.server.data.query.CompiledQuery;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.ParsedQuery;
import platform.server.data.sql.SQLExecute;
import platform.server.data.sql.SQLSyntax;
import platform.server.session.SQLSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
            String SelectString = syntax.getSelect(fromSelect, SQLSession.stringExpr(
                    SQLSession.mapNames(changeCompile.keySelect,changeCompile.keyNames,keyOrder),
                    SQLSession.mapNames(changeCompile.propertySelect,changeCompile.propertyNames,propertyOrder)),
                    SQLSession.stringWhere(whereSelect),"","","");

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
                JoinQuery<KeyField, PropertyField> updateQuery = new JoinQuery<KeyField, PropertyField>(change,false);
                updateQuery.and(table.joinAnd(updateQuery.mapKeys).getWhere());
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
        JoinQuery<KeyField, PropertyField> leftKeysQuery = new JoinQuery<KeyField, PropertyField>(change, true);
        // исключим ключи которые есть
        leftKeysQuery.and(table.joinAnd(leftKeysQuery.mapKeys).getWhere().not());

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
}
