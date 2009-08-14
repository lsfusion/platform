package platform.server.data.query;

import platform.server.data.Table;
import platform.server.data.KeyField;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.query.exprs.*;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.where.Where;
import platform.server.caches.MapParamsIterable;
import platform.server.session.SQLSession;
import platform.base.BaseUtils;

import java.util.*;

// класс нисколько не immutable
public abstract class CompileSource {

    public final Map<ValueExpr,String> params;
    public final Map<KeyExpr,String> keySelect = new HashMap<KeyExpr, String>();
    public final SQLSyntax syntax;

    protected CompileSource(Map<ValueExpr, String> params, SQLSyntax syntax) {
        this.params = params;
        this.syntax = syntax;
    }

    public abstract String getSource(Table.Join.Expr expr);
    public abstract String getSource(Table.Join.IsIn where);
    public abstract String getSource(GroupExpr groupExpr);
}
