package platform.server.data.expr;

import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.query.PartitionExpr;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

public abstract class NotNullExpr extends VariableClassExpr {

    public abstract class NotNull extends DataWhere {

        public NotNullExpr getExpr() {
            return NotNullExpr.this;
        }

        public String getSource(CompileSource compile) {
            return NotNullExpr.this.getSource(compile) + " IS NOT NULL";
        }

        @Override
        protected String getNotSource(CompileSource compile) {
            return NotNullExpr.this.getSource(compile) + " IS NULL";
        }

        protected boolean isComplex() {
            return false;
        }

        protected Where translate(MapTranslate translator) {
            return NotNullExpr.this.translateOuter(translator).getWhere();
        }
        public Where translateQuery(QueryTranslator translator) {
            return NotNullExpr.this.translateQuery(translator).getWhere();
        }

        public QuickSet<OuterContext> calculateOuterDepends() {
            return new QuickSet<OuterContext>(NotNullExpr.this);
        }

        protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
            NotNullExpr.this.fillAndJoinWheres(joins,andWhere);
        }

        public int hash(HashContext hashContext) {
            return NotNullExpr.this.hashOuter(hashContext);
        }

        @Override
        public boolean twins(TwinImmutableInterface o) {
            return NotNullExpr.this.equals(((NotNull) o).getExpr());
        }

    }

    public void fillFollowSet(DataWhereSet fillSet) {
        Where where = getWhere();
        if(where instanceof DataWhere)
            fillSet.add((DataWhere) getWhere());
        else
            assert PartitionExpr.isWhereCalculated(this);
    }


}
