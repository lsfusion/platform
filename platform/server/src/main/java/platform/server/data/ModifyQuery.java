package platform.server.data;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.expr.Expr;
import platform.server.data.query.CompiledQuery;
import platform.server.data.query.IQuery;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.data.sql.SQLExecute;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.NullReader;
import platform.server.data.where.Where;

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

    public SQLExecute getUpdate(final SQLSyntax syntax) {

        assert !change.getProperties().isEmpty();
        
        int updateModel = syntax.updateModel();
        String update;
        String setString;
        ImCol<String> whereSelect;
        final CompiledQuery<KeyField, PropertyField> changeCompile = change.compile(syntax);

        switch(updateModel) {
            case 2:
                // Oracl'вская модель Update'а (ISO'ная)
                String fromSelect = changeCompile.from;

                whereSelect = changeCompile.whereSelect.mergeCol(changeCompile.keySelect.mapColValues(new GetKeyValue<String, KeyField, String>() {
                    public String getMapValue(KeyField key, String value) {
                        return table.getName(syntax)+"."+key.name +"="+ changeCompile.keySelect.get(key);
                    }}));

                Result<ImOrderSet<KeyField>> keyOrder = new Result<ImOrderSet<KeyField>>();
                Result<ImOrderSet<PropertyField>> propertyOrder = new Result<ImOrderSet<PropertyField>>();
                String selectString = syntax.getSelect(fromSelect, SQLSession.stringExpr(
                        SQLSession.mapNames(changeCompile.keySelect,changeCompile.keyNames,keyOrder),
                        SQLSession.mapNames(changeCompile.propertySelect,changeCompile.propertyNames,propertyOrder)),
                        whereSelect.toString(" AND "),"","","", "");

                setString = SetFact.addOrderExcl(keyOrder.result, propertyOrder.result).toString(Field.<Field>nameGetter(), ",");

                update = "UPDATE " + table.getName(syntax) + " SET ("+setString+") = ("+selectString+") WHERE EXISTS ("+selectString+")";
                break;
            case 1:
                // SQL-серверная модель когда она подхватывает первый JoinSelect и старую таблицу уже не вилит
                // построим Query куда переJoin'им все эти поля (оптимизатор уберет все дублирующиеся таблицы) - не получится так как если full join'ы пойдут нарушится инвариант
/*                Query<KeyField, PropertyField> updateQuery = new Query<KeyField, PropertyField>(change);
                updateQuery.and(table.joinAnd(updateQuery.mapKeys).getWhere());
                // надо в compile параметром для какого join'а оставлять alias
                changeCompile = updateQuery.compile(syntax);
                whereSelect = changeCompile.whereSelect;*/
                final String changeAlias = "ch_upd_al";

                whereSelect = changeCompile.keyNames.mapColValues(new GetKeyValue<String, KeyField, String>() {
                    public String getMapValue(KeyField key, String value) {
                        return table.getName(syntax)+"."+key.name + "=" + changeAlias + "." + changeCompile.keyNames.get(key);
                    }});

                setString = changeCompile.propertyNames.toString(new GetKeyValue<String, PropertyField, String>() {
                    public String getMapValue(PropertyField key, String value) {
                        return key.name + "=" + changeAlias + "." + value;
                    }}, ",");

                update = "UPDATE " + table.getName(syntax) + " SET " + setString + " FROM " + table.getName(syntax) + " JOIN (" +
                        changeCompile.select + ") " + changeAlias + " ON " + (whereSelect.size()==0? Where.TRUE_STRING:whereSelect.toString(" AND "));
                break;
            case 0:
                // по умолчанию - нормальная
                whereSelect = changeCompile.whereSelect.mergeCol(changeCompile.keySelect.mapColValues(new GetKeyValue<String, KeyField, String>() {
                    public String getMapValue(KeyField key, String value) {
                        return table.getName(syntax)+"."+key.name +"="+ changeCompile.keySelect.get(key);
                    }}));

                setString = changeCompile.propertySelect.toString(new GetKeyValue<String, PropertyField, String>() {
                    public String getMapValue(PropertyField key, String value) {
                        return key.name + "=" + value;
                    }}, ",");
                
                update = "UPDATE " + syntax.getUpdate(table.getName(syntax)," SET "+setString,changeCompile.from,BaseUtils.clause("WHERE", whereSelect.toString(" AND ")));
                break;
            default:
                throw new RuntimeException();
        }

        return new SQLExecute(update,changeCompile.getQueryParams(env), changeCompile.env);
    }

    public SQLExecute getDelete(final SQLSyntax syntax) {
        // пока реализуем чисто PostgreSQL синтаксис
        final CompiledQuery<KeyField, PropertyField> deleteCompile = change.compile(syntax);
        final String deleteAlias = "ch_dl_sq";

        ImSet<String> whereSelect = table.getTableKeys().mapSetValues(new GetValue<String, KeyField>() {
            public String getMapValue(KeyField value) {
                return table.getName(syntax) + "." + value.name + "=" + deleteAlias + "." + deleteCompile.keyNames.get(value);
            }});

        String delete = "DELETE FROM " + table.getName(syntax) + " USING (" + deleteCompile.select + ") " + deleteAlias + " WHERE " + (whereSelect.size()==0? Where.TRUE_STRING:whereSelect.toString(" AND "));

        return new SQLExecute(delete, deleteCompile.getQueryParams(env), deleteCompile.env);
    }

    public SQLExecute getInsertLeftKeys(SQLSyntax syntax, boolean updateProps, boolean insertOnlyNotNull) {
        return (new ModifyQuery(table, getInsertLeftQuery(updateProps, insertOnlyNotNull), env)).getInsertSelect(syntax);
    }

    public Query<KeyField, PropertyField> getInsertLeftQuery(boolean updateProps, boolean insertOnlyNotNull) {
        // делаем для этого еще один запрос
        QueryBuilder<KeyField, PropertyField> leftKeysQuery = new QueryBuilder<KeyField, PropertyField>(change.getMapKeys());

        Where onlyNotNull = Where.FALSE;
        if(updateProps || insertOnlyNotNull)
            for(PropertyField property : change.getProperties()) {
                Expr expr = change.getExpr(property);
                if(updateProps)
                    leftKeysQuery.addProperty(property, expr);
                if(insertOnlyNotNull)
                    onlyNotNull = onlyNotNull.or(expr.getWhere());
            }
        if(insertOnlyNotNull)
            leftKeysQuery.and(onlyNotNull);

        leftKeysQuery.and(change.getWhere());
        // исключим ключи которые есть
        leftKeysQuery.and(table.join(leftKeysQuery.getMapExprs()).getWhere().not());
        return leftKeysQuery.getQuery();
    }

    private static String getInsertCastSelect(final CompiledQuery<KeyField, PropertyField> changeCompile, SQLSyntax syntax) {
        if(changeCompile.union && syntax.nullUnionTrouble()) {
            final String alias = "castalias";
            boolean casted = false;
            String exprs = changeCompile.keyOrder.toString(new GetValue<String, KeyField>() {
                public String getMapValue(KeyField value) {
                    return alias + "." + changeCompile.keyNames.get(value);
                }}, ",");
            for(PropertyField propertyField : changeCompile.propertyOrder) { // последействие
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

        String insertString = SetFact.addOrderExcl(changeCompile.keyOrder, changeCompile.propertyOrder).toString(Field.<Field>nameGetter(), ",");

        return new SQLExecute("INSERT INTO " + name + " (" + (insertString.length()==0?"dumb":insertString) + ") " + getInsertCastSelect(changeCompile, syntax),changeCompile.getQueryParams(env), changeCompile.env);
    }

    public SQLExecute getInsertSelect(SQLSyntax syntax) {
        return getInsertSelect(table.getName(syntax), change, env, syntax);
    }

    public boolean isEmpty() {
        return change.isEmpty();
    }
}
