package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.LimitOptions;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.compile.CompileOptions;
import lsfusion.server.data.query.compile.SubQueryContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.stat.LimitOffset;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// the SQL selecting the objects a property is not NULL for, the last two of them - what EXPORT ... WHERE p(o) ORDER o
// DESC TOP 2 sends -, compiled for the server's database, into nniSql, and how many times it checks the column of a
// stored property for IS NOT NULL into nniNotNulls
public class ReadFilterSqlAction extends InternalAction {

    private final ClassPropertyInterface filterInterface;
    private final ClassPropertyInterface columnInterface;

    public ReadFilterSqlAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> interfaces = getOrderInterfaces().iterator();
        filterInterface = interfaces.next();
        columnInterface = interfaces.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            SQLSyntax syntax = context.getSession().sql.syntax;
            String sql = compile(findProperty((String) context.getKeyValue(filterInterface).getValue()).property, syntax);
            String column = findProperty((String) context.getKeyValue(columnInterface).getValue()).property.field.getName(syntax);

            int notNulls = 0;
            for (Matcher matcher = Pattern.compile("\\." + Pattern.quote(column) + " IS NOT NULL").matcher(sql); matcher.find(); )
                notNulls++;

            findProperty("nniSql[]").change(sql, context);
            findProperty("nniNotNulls[]").change(notNulls, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static <T extends PropertyInterface> String compile(Property<T> filter, SQLSyntax syntax) {
        ImRevMap<T, KeyExpr> mapKeys = filter.getMapKeys();
        QueryBuilder<T, String> query = new QueryBuilder<>(mapKeys, filter.getExpr(mapKeys).getWhere());
        query.addProperty("object", mapKeys.singleValue());
        return query.getQuery().compile(MapFact.singletonOrder("object", true),
                new CompileOptions<>(syntax, LimitOptions.get(new LimitOffset(2)), SubQueryContext.EMPTY, false)).sql.command;
    }
}
