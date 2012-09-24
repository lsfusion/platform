package platform.server.data;

import platform.base.BaseUtils;
import platform.server.data.query.CompiledQuery;
import platform.server.data.query.IQuery;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLExecute;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.NullReader;
import platform.server.data.where.Where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ModifyQuery {
    public final Table table;
    private final IQuery<KeyField, PropertyField> change;
    private final QueryEnvironment env;

    public ModifyQuery(Table table, IQuery<KeyField, PropertyField> change) {
        this(table, change, QueryEnvironment.empty);
    }

    public ModifyQuery(Table table, IQuery<KeyField, PropertyField> change, QueryEnvironment env) {
        this.table = table;
        this.change = change;
        this.env = env;
    }

    public SQLExecute getUpdate(SQLSyntax syntax) {

        assert !change.getProperties().isEmpty();
        
        int updateModel = syntax.updateModel();
        CompiledQuery<KeyField, PropertyField> changeCompile;
        String update;
        String setString;
        Collection<String> whereSelect;

        switch(updateModel) {
            case 2:
                // Oracl'вская модель Update'а
                changeCompile = change.compile(syntax);
                whereSelect = new ArrayList<String>(changeCompile.whereSelect);
                String fromSelect = changeCompile.from;

                for(KeyField key : table.keys)
                    whereSelect.add(table.getName(syntax)+"."+key.name +"="+ changeCompile.keySelect.get(key));

                List<KeyField> keyOrder = new ArrayList<KeyField>();
                List<PropertyField> propertyOrder = new ArrayList<PropertyField>();
                String SelectString = syntax.getSelect(fromSelect, SQLSession.stringExpr(
                        SQLSession.mapNames(changeCompile.keySelect,changeCompile.keyNames,keyOrder),
                        SQLSession.mapNames(changeCompile.propertySelect,changeCompile.propertyNames,propertyOrder)),
                        BaseUtils.toString(whereSelect, " AND "),"","","", "");

                setString = "";
                for(KeyField field : keyOrder)
                    setString = (setString.length()==0?"":setString+",") + field.name;
                for(PropertyField field : propertyOrder)
                    setString = (setString.length()==0?"":setString+",") + field.name;

                update = "UPDATE " + table.getName(syntax) + " SET ("+setString+") = ("+SelectString+") WHERE EXISTS ("+SelectString+")";
                break;
            case 1:
                // SQL-серверная модель когда она подхватывает первый JoinSelect и старую таблицу уже не вилит
                // построим Query куда переJoin'им все эти поля (оптимизатор уберет все дублирующиеся таблицы) - не получится так как если full join'ы пойдут нарушится инвариант
/*                Query<KeyField, PropertyField> updateQuery = new Query<KeyField, PropertyField>(change);
                updateQuery.and(table.joinAnd(updateQuery.mapKeys).getWhere());
                // надо в compile параметром для какого join'а оставлять alias
                changeCompile = updateQuery.compile(syntax);
                whereSelect = changeCompile.whereSelect;*/
                changeCompile = change.compile(syntax);
                String changeAlias = "ch_upd_al";

                whereSelect = new ArrayList<String>();
                for(KeyField key : table.keys)
                    whereSelect.add(table.getName(syntax)+"."+key.name + "=" + changeAlias + "." + changeCompile.keyNames.get(key));

                setString = "";
                for(Map.Entry<PropertyField,String> setProperty : changeCompile.propertyNames.entrySet())
                    setString = (setString.length()==0?"":setString+",") + setProperty.getKey().name + "=" + changeAlias + "." + setProperty.getValue();
                
                update = "UPDATE " + table.getName(syntax) + " SET " + setString + " FROM " + table.getName(syntax) + " JOIN (" +
                        changeCompile.select + ") " + changeAlias + " ON " + (whereSelect.size()==0? Where.TRUE_STRING:BaseUtils.toString(whereSelect," AND "));
                break;
            case 0:
                // по умолчанию - нормальная
                changeCompile = change.compile(syntax);

                whereSelect = new ArrayList<String>(changeCompile.whereSelect);
                for(KeyField key : table.keys)
                    whereSelect.add(table.getName(syntax)+"."+key.name +"="+ changeCompile.keySelect.get(key));

                setString = "";
                for(Map.Entry<PropertyField,String> setProperty : changeCompile.propertySelect.entrySet())
                    setString = (setString.length()==0?"":setString+",") + setProperty.getKey().name + "=" + setProperty.getValue();

                update = "UPDATE " + syntax.getUpdate(table.getName(syntax)," SET "+setString,changeCompile.from,BaseUtils.clause("WHERE", BaseUtils.toString(whereSelect, " AND ")));
                break;
            default:
                throw new RuntimeException();
        }

        return new SQLExecute(update,changeCompile.getQueryParams(env), changeCompile.env);
    }

    public SQLExecute getDelete(SQLSyntax syntax) {
        // пока реализуем чисто PostgreSQL синтаксис
        CompiledQuery<KeyField, PropertyField> deleteCompile = change.compile(syntax);
        String deleteAlias = "ch_dl_sq";

        Collection<String> whereSelect = new ArrayList<String>();
        for(KeyField key : table.keys)
            whereSelect.add(table.getName(syntax)+"."+key.name + "=" + deleteAlias + "." + deleteCompile.keyNames.get(key));

        String delete = "DELETE FROM " + table.getName(syntax) + " USING (" + deleteCompile.select + ") " + deleteAlias + " WHERE " + (whereSelect.size()==0? Where.TRUE_STRING:BaseUtils.toString(whereSelect," AND "));

        return new SQLExecute(delete, deleteCompile.getQueryParams(env), deleteCompile.env);
    }

    public SQLExecute getInsertLeftKeys(SQLSyntax syntax, boolean updateProps) {
        return (new ModifyQuery(table, getInsertLeftQuery(updateProps), env)).getInsertSelect(syntax);
    }

    public Query<KeyField, PropertyField> getInsertLeftQuery(boolean updateProps) {
        // делаем для этого еще один запрос
        Query<KeyField, PropertyField> leftKeysQuery = new Query<KeyField, PropertyField>(change.getMapKeys());
        if(updateProps)
            for(PropertyField property : change.getProperties())
                leftKeysQuery.properties.put(property, change.getExpr(property));
        leftKeysQuery.and(change.getWhere());
        // исключим ключи которые есть
        leftKeysQuery.and(table.joinAnd(leftKeysQuery.mapKeys).getWhere().not());
        return leftKeysQuery;
    }

    private static String getInsertCastSelect(CompiledQuery<KeyField, PropertyField> changeCompile, SQLSyntax syntax) {
        if(changeCompile.union && syntax.nullUnionTrouble()) {
            String alias = "castalias";
            String exprs = "";
            boolean casted = false;
            for(KeyField keyField : changeCompile.keyOrder)
                exprs = (exprs.length()==0?"":exprs+",") + alias + "." + changeCompile.keyNames.get(keyField);
            for(PropertyField propertyField : changeCompile.propertyOrder) {
                String propertyExpr = alias + "." + changeCompile.propertyNames.get(propertyField);
                if(changeCompile.propertyReaders.get(propertyField) instanceof NullReader) { // если null, вставляем явный cast
                    propertyExpr = propertyField.type.getCast(propertyExpr,syntax, false);
                    casted = true;
                }
                exprs = (exprs.length()==0?"":exprs+",") + propertyExpr;
            }
            if(casted)
                return "SELECT " + exprs + " FROM (" + changeCompile.select + ") " + alias; 
        }
        return changeCompile.select;
    }

    public static SQLExecute getInsertSelect(String name, IQuery<KeyField, PropertyField> query, QueryEnvironment env, SQLSyntax syntax) {
        CompiledQuery<KeyField, PropertyField> changeCompile = query.compile(syntax);

        String insertString = "";
        for(KeyField keyField : changeCompile.keyOrder)
            insertString = (insertString.length()==0?"":insertString+",") + keyField.name;
        for(PropertyField propertyField : changeCompile.propertyOrder)
            insertString = (insertString.length()==0?"":insertString+",") + propertyField.name;

        return new SQLExecute("INSERT INTO " + name + " (" + (insertString.length()==0?"dumb":insertString) + ") " + getInsertCastSelect(changeCompile, syntax),changeCompile.getQueryParams(env), changeCompile.env);
    }

    public SQLExecute getInsertSelect(SQLSyntax syntax) {
        return getInsertSelect(table.getName(syntax), change, env, syntax);
    }

    public boolean isEmpty() {
        return change.isEmpty();
    }
}
