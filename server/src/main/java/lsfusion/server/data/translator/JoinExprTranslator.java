package lsfusion.server.data.translator;

import lsfusion.base.BaseUtils;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.SourceJoin;

// заменяет expr / join на ключи
public class JoinExprTranslator extends ExprTranslator {

    private final ImMap<BaseExpr, KeyExpr> exprs; // те которые заменяем
    private final ImSet<BaseExpr> fullExprs; // те которые заведомо не трогаем

    public JoinExprTranslator(ImMap<BaseExpr, KeyExpr> exprs, ImSet<BaseExpr> fullExprs) {
        this.exprs = exprs;
        this.fullExprs = fullExprs;
    }

    public static <T extends SourceJoin<T>> T translateExpr(T source, JoinExprTranslator translator) {
        if(translator != null) // важно делать до getWhere, потому как могут добавится условия из getOrWhere (скажем с висячими ключами), а "основной" expr translate'ся и будет висячий ключ
            return source.translateExpr(translator);
        return source;
    }

    public Expr translate(BaseExpr key) {
        KeyExpr transExpr = exprs.get(key);
        if(transExpr==null) {
            if(fullExprs.contains(key))
                return key;
            return null;
        } else
            return transExpr;
    }

    @Override
    public <T extends SourceJoin<T>> T translate(T expr) {
        SourceJoin sourceJoin = expr;
        if(sourceJoin instanceof BaseExpr)
            return BaseUtils.<T>immutableCast(translate((BaseExpr)sourceJoin));
        return super.translate(expr);
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return exprs.equals(((JoinExprTranslator)o).exprs) && fullExprs.equals(((JoinExprTranslator)o).fullExprs);
    }

    @Override
    public int immutableHashCode() {
        return 31 * exprs.hashCode() + fullExprs.hashCode();
    }
}
