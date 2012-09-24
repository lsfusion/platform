package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;

public abstract class NotNullExpr extends VariableClassExpr {

    @Override
    public Where calculateOrWhere() {
        return Where.TRUE;
    }

    @Override
    public Where calculateNotNullWhere() { // assert result instanceof NotNull || result.isTrue()
        return Where.TRUE;
    }

    public abstract class NotNull extends DataWhere {

        public NotNullExpr getExpr() {
            return NotNullExpr.this;
        }

        protected boolean isComplex() {
            return false;
        }

        public String getSource(CompileSource compile) {
            return getExpr().getSource(compile) + " IS NOT NULL";
        }

        @Override
        protected String getNotSource(CompileSource compile) {
            return getExpr().getSource(compile) + " IS NULL";
        }

        protected Where translate(MapTranslate translator) {
            return getExpr().translateOuter(translator).getNotNullWhere();
        }

        @Override
        public Where packFollowFalse(Where falseWhere) {
            Expr packExpr = NotNullExpr.this.packFollowFalse(falseWhere);
//            if(packExpr instanceof BaseExpr) // чтобы бесконечных циклов не было
//                return ((BaseExpr)packExpr).getNotNullWhere();
            if(BaseUtils.hashEquals(packExpr, NotNullExpr.this)) // чтобы бесконечных циклов не было
                return this;
            else
                return packExpr.getWhere();
        }

        public Where translateQuery(QueryTranslator translator) {
            Expr translateExpr = getExpr().translateQuery(translator);
//            if(translateExpr instanceof BaseExpr) // ??? в pack на это нарвались, здесь по идее может быть аналогичная ситуация
//                return ((BaseExpr)translateExpr).getNotNullWhere();
            if(BaseUtils.hashEquals(translateExpr, NotNullExpr.this)) // чтобы бесконечных циклов не было
                return this;
            else
                return translateExpr.getWhere();
        }

        public QuickSet<OuterContext> calculateOuterDepends() {
            return new QuickSet<OuterContext>(getExpr());
        }

        protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
            getExpr().fillAndJoinWheres(joins,andWhere);
        }

        public int hash(HashContext hashContext) {
            return getExpr().hashOuter(hashContext);
        }

        protected DataWhereSet calculateFollows() {
            return new DataWhereSet(getExprFollows(false, true));
        }

        public boolean twins(TwinImmutableInterface o) {
            return getExpr().equals(((NotNull) o).getExpr());
        }
    }

    private NotNullExprSet exprThisFollows = null;
    @ManualLazy
    public NotNullExprSet getExprFollows(boolean includeThis, boolean recursive) {
        assert includeThis || recursive;
        if(recursive) {
            if(includeThis && hasNotNull()) {
                if(exprThisFollows==null) {
                    exprThisFollows = new NotNullExprSet(getExprFollows(true));
                    exprThisFollows.add(this);
                }
                return exprThisFollows;
            } else
                return getExprFollows(true);
        } else // не кэшируем так как редко используется
            return new NotNullExprSet(this);
    }

    public void fillFollowSet(DataWhereSet fillSet) {
        assert hasNotNull();
        fillSet.add((DataWhere)getNotNullWhere());
    }
}
