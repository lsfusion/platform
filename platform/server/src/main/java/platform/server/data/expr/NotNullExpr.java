package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
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

        public Where translateOuter(MapTranslate translator) {
            return NotNullExpr.this.translateOuter(translator).getWhere();
        }
        public Where translateQuery(QueryTranslator translator) {
            return NotNullExpr.this.translateQuery(translator).getWhere();
        }

        public void enumDepends(ExprEnumerator enumerator) {
            NotNullExpr.this.enumerate(enumerator);
        }

        protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
            NotNullExpr.this.fillAndJoinWheres(joins,andWhere);
        }

        public int hashOuter(HashContext hashContext) {
            return NotNullExpr.this.hashOuter(hashContext);
        }

        @Override
        public boolean twins(TwinImmutableInterface o) {
            return NotNullExpr.this.equals(((NotNull) o).getExpr());
        }

        public long calculateComplexity() {
            return NotNullExpr.this.getComplexity();
        }
    }

    public void fillFollowSet(DataWhereSet fillSet) {
        Where where = getWhere();
        if(where instanceof DataWhere)
            fillSet.add((DataWhere) getWhere());
        else
            assert this instanceof OrderExpr;
    }


}
