package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.ValueJoin;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class CurrentEnvironmentExpr extends StaticNotNullExpr {

    private final String paramString;

    public CurrentEnvironmentExpr(String paramString, AndClassSet paramClass) {
        super(paramClass);
        this.paramString = paramString;
    }
    public boolean calcTwins(TwinImmutableObject obj) {
        return paramString.equals(((CurrentEnvironmentExpr) obj).paramString);
    }

    protected int hash(HashContext hashContext) {
        return paramString.hashCode();
    }

    public String getSource(CompileSource compile) {
        return paramString;
    }
}
