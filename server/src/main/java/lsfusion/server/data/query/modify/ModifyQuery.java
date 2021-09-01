package lsfusion.server.data.query.modify;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.compile.CompileOptions;
import lsfusion.server.data.query.compile.CompiledQuery;
import lsfusion.server.data.sql.SQLDML;
import lsfusion.server.data.sql.SQLExecute;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.*;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.navigator.controller.env.SQLSessionContextProvider;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.monitor.sql.SQLDebugInfo;

public class ModifyQuery {
    public final NamedTable table;
    private final IQuery<KeyField, PropertyField> change;
    public final QueryEnvironment env;
    public final TableOwner owner;
    
    public OperationOwner getOwner() {
        return env.getOpOwner();
    }

    public ModifyQuery(NamedTable table, IQuery<KeyField, PropertyField> change, OperationOwner owner, TableOwner tableOwner) {
        this(table, change, DataSession.emptyEnv(owner), tableOwner);
    }

    public ModifyQuery(NamedTable table, IQuery<KeyField, PropertyField> change, QueryEnvironment env, TableOwner owner) {
        this.table = table;
        this.change = change;
        this.env = env;
        this.owner = owner;
    }

    public SQLExecute getUpdate(final SQLSyntax syntax, SQLSessionContextProvider userProvider) {

        assert !change.getProperties().isEmpty();
        
        int updateModel = syntax.updateModel();
        String update;
        String setString;
        ImCol<String> whereSelect;
        CompileOptions<PropertyField> options = new CompileOptions<>(syntax, table.getPropTypes());
        final CompiledQuery<KeyField, PropertyField> changeCompile = change.compile(options);

        switch(updateModel) {
            case 2:
                // Oracl'вская модель Update'а (ISO'ная)
                String fromSelect = changeCompile.from;

                whereSelect = changeCompile.whereSelect.mergeCol(changeCompile.keySelect.mapColValues((key, value) -> table.getName(syntax)+"."+key.getName(syntax) +"="+ changeCompile.keySelect.get(key)));

                Result<ImOrderSet<KeyField>> keyOrder = new Result<>();
                Result<ImOrderSet<PropertyField>> propertyOrder = new Result<>();
                String selectString = "(" + SQLSession.getSelect(syntax, fromSelect,
                        SQLSession.mapNames(changeCompile.keySelect,changeCompile.keyNames,keyOrder),
                        SQLSession.mapNames(changeCompile.propertySelect,changeCompile.propertyNames,propertyOrder),
                        whereSelect) + ")";

                setString = SetFact.addOrderExcl(keyOrder.result, propertyOrder.result).toString(Field.<Field>nameGetter(syntax), ",");

                update = "UPDATE " + table.getName(syntax) + " SET ("+setString+") = "+selectString+" WHERE EXISTS "+selectString;
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

                whereSelect = changeCompile.keyNames.mapColValues((key, value) -> table.getName(syntax)+"."+key.getName(syntax) + "=" + changeAlias + "." + changeCompile.keyNames.get(key));

                setString = changeCompile.propertyNames.toString((key, value) -> key.getName(syntax) + "=" + changeAlias + "." + value, ",");

                update = "UPDATE " + table.getName(syntax) + " SET " + setString + " FROM " + table.getName(syntax) + " JOIN (" +
                        changeCompile.sql.command + ") " + changeAlias + " ON " + (whereSelect.size()==0? Where.TRUE_STRING:whereSelect.toString(" AND "));
                break;
            case 0:
                // по умолчанию - нормальная
                whereSelect = changeCompile.whereSelect.mergeCol(changeCompile.keySelect.mapColValues((key, value) -> table.getName(syntax)+"."+key.getName(syntax) +"="+ changeCompile.keySelect.get(key)));

                setString = changeCompile.propertySelect.toString((key, value) -> key.getName(syntax) + "=" + value, ",");
                
                update = "UPDATE " + syntax.getUpdate(table.getName(syntax)," SET "+setString,changeCompile.from,BaseUtils.clause("WHERE", whereSelect.toString(" AND ")));
                break;
            default:
                throw new RuntimeException();
        }

        SQLDML dml = new SQLDML(update, changeCompile.sql.baseCost, changeCompile.sql.subQueries, changeCompile.sql.env, changeCompile.sql.recursionFunction);
        return new SQLExecute(dml,changeCompile.getQueryParams(env), changeCompile.getQueryExecEnv(userProvider), env.getTransactTimeout(), env.getOpOwner(), owner, register(TableChange.UPDATE), new SQLDebugInfo<>(change, options));
    }

    public SQLExecute getDelete(final SQLSyntax syntax, SQLSessionContextProvider userProvider) {

        int updateModel = syntax.updateModel();
        // noInline'ом пытаемся предотвратить self join у которого все очень плохо со статистикой
        // конечно из-за этого может быть проблема когда изменяемая таблица маленькая, а запрос большой, но в таком случае все равно будет проблема с predicate push down, поэтому будем assert'ить что такой ситуации не будет
        // вообще аналогичная проблема возможна и в getUpdate, но пока с ней не сталкивались
        CompileOptions<PropertyField> options = new CompileOptions<>(syntax, syntax.inlineSelfJoinTrouble() && Settings.get().isUseDeleteNoInline());
        final CompiledQuery<KeyField, PropertyField> deleteCompile = change.compile(options);
        ImSet<String> whereSelect;
        String delete; final String deleteAlias;

        switch(updateModel) {
            case 1:
                deleteAlias = "ch_dl_sq";

                whereSelect = table.getTableKeys().mapSetValues(value -> table.getName(syntax) + "." + value.getName(syntax) + "=" + deleteAlias + "." + deleteCompile.keyNames.get(value));

                delete = "DELETE FROM " + table.getName(syntax) + " FROM " + table.getName(syntax) + " JOIN (" +
                        deleteCompile.sql.command + ") " + deleteAlias + " ON " + (whereSelect.size()==0? Where.TRUE_STRING:whereSelect.toString(" AND "));
                break;
            case 0:
                deleteAlias = "ch_dl_sq";

                whereSelect = table.getTableKeys().mapSetValues(value -> table.getName(syntax) + "." + value.getName(syntax) + "=" + deleteAlias + "." + deleteCompile.keyNames.get(value));

                delete = "DELETE FROM " + table.getName(syntax) + " USING (" + deleteCompile.sql.command + ") " + deleteAlias + " WHERE " + (whereSelect.size()==0? Where.TRUE_STRING:whereSelect.toString(" AND "));
                break;
            default:
                throw new UnsupportedOperationException();
        }

        SQLDML dml = new SQLDML(delete, deleteCompile.sql.baseCost, deleteCompile.sql.subQueries, deleteCompile.sql.env, deleteCompile.sql.recursionFunction);
        return new SQLExecute(dml, deleteCompile.getQueryParams(env), deleteCompile.getQueryExecEnv(userProvider), env.getTransactTimeout(), env.getOpOwner(), owner, register(TableChange.DELETE), new SQLDebugInfo<>(change, options));
    }

    private RegisterChange register(TableChange coeff) {
        return SQLSession.register(table, owner, coeff);
    }


    public SQLExecute getInsertLeftKeys(SQLSyntax syntax, SQLSessionContextProvider userProvider, boolean updateProps, boolean insertOnlyNotNull) {
        return (new ModifyQuery(table, getInsertLeftQuery(updateProps, insertOnlyNotNull), env, owner)).getInsertSelect(syntax, userProvider);
    }

    public Query<KeyField, PropertyField> getInsertLeftQuery(boolean updateProps, boolean insertOnlyNotNull) {
        // делаем для этого еще один запрос
        QueryBuilder<KeyField, PropertyField> leftKeysQuery = new QueryBuilder<>(change.getMapKeys());

        Where onlyNotNull = Where.FALSE();
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

    public static SQLExecute getInsertSelect(String name, IQuery<KeyField, PropertyField> query, QueryEnvironment env, TableOwner owner, SQLSyntax syntax, SQLSessionContextProvider userProvider, Table table, RegisterChange change) {
        return getInsertSelect(name, query, env, owner, syntax, userProvider, table, change, 0);
    }

    public static SQLExecute getInsertSelect(String name, IQuery<KeyField, PropertyField> query, QueryEnvironment env, TableOwner owner, SQLSyntax syntax, SQLSessionContextProvider userProvider, Table table, RegisterChange change, int selectTop) {
        CompileOptions<PropertyField> options = new CompileOptions<>(syntax, table != null ? table.getPropTypes() : null, selectTop);
        CompiledQuery<KeyField, PropertyField> changeCompile = query.compile(options);

        SQLDML dml = changeCompile.sql.getInsertDML(name, changeCompile.keyOrder, changeCompile.propertyOrder, true, changeCompile.keyOrder.mapOrder(changeCompile.keyNames), changeCompile.propertyOrder.mapOrder(changeCompile.propertyNames), syntax);
        return new SQLExecute(dml, changeCompile.getQueryParams(env, selectTop), changeCompile.getQueryExecEnv(userProvider), env.getTransactTimeout(), env.getOpOwner(), owner, change, new SQLDebugInfo<>(query, options));
    }

    public SQLExecute getInsertSelect(SQLSyntax syntax, SQLSessionContextProvider userProvider) {
        return getInsertSelect(table.getName(syntax), change, env, owner, syntax, userProvider, table, register(TableChange.INSERT));
    }

    public boolean isEmpty() {
        return change.isEmpty();
    }
}
