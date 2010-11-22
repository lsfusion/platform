package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ValueExpr extends AbstractValueExpr {

    public ValueExpr(Object object, ConcreteClass objectClass) {
        super(object, objectClass);
    }

    public static SystemValueExpr TRUE = new SystemValueExpr(true,LogicalClass.instance);
    public static Expr get(Where where) {
        return TRUE.and(where);
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
    }

    public String getSource(CompileSource compile) {
        return compile.params.get(this);
    }

    public void enumDepends(ExprEnumerator enumerator) {
    }

    public Type getType(KeyType keyType) {
        return objectClass.getType();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // возвращает Where без следствий
    public Where calculateWhere() {
        return Where.TRUE;
    }

    public boolean twins(AbstractSourceJoin o) {
        return object.equals(((ValueExpr)o).object) && objectClass.equals(((ValueExpr)o).objectClass);
    }

    @Override
    public int hashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    public int hashOuter(HashContext hashContext) {
        return hashContext.values.hash(this);
    }

    // нельзя потому как при трансляции значения потеряются
/*    @Override
    public ValueExpr scale(int mult) {
        return new ValueExpr(((IntegralClass)objectClass).multiply((Number) object,mult),objectClass);
    }*/

    public ValueExpr translateQuery(QueryTranslator translator) {
        return this;
    }

    public ValueExpr translateOuter(MapTranslate translator) {
        return translator.translate(this);
    }

    public VariableExprSet calculateExprFollows() {
        return new VariableExprSet();
    }

    public static ValueExpr ZERO = new ValueExpr(0, DoubleClass.instance);

    private static Set<ValueExpr> staticExprs;
    {
        staticExprs = new HashSet<ValueExpr>();
        staticExprs.add(ValueExpr.ZERO);
        staticExprs.add(null);
    }

    public static Set<? extends Expr> removeStatic(Set<? extends Expr> col) {
        return BaseUtils.removeSet(col,staticExprs);
    }

    public static <V> Map<ValueExpr,V> removeStatic(Map<ValueExpr,V> map) {
        return BaseUtils.filterNotKeys(map,staticExprs);
    }

    // пересечение с игнорированием ValueExpr.TRUE
    public static boolean noStaticEquals(Set<? extends Expr> col1, Set<? extends Expr> col2) {
        return removeStatic(col1).equals(removeStatic(col2));
    }

    public long calculateComplexity() {
        return 1;
    }
}
