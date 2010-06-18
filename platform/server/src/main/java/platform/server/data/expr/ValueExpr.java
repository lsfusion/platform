package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ValueExpr extends StaticClassExpr {

    public final Object object;
    public final ConcreteClass objectClass;

    public ValueExpr(Object object, ConcreteClass objectClass) {
        this.object = object;
        this.objectClass = objectClass;

        assert !(this.objectClass instanceof LogicalClass && !this.object.equals(true));
    }

    public static ValueExpr TRUE = new ValueExpr(true,LogicalClass.instance);
    public static Expr get(Where where) {
        return TRUE.and(where);
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
    }

    public String getSource(CompileSource compile) {
        String source = compile.params.get(this);
        if(source==null) source = getString(compile.syntax);
        return source;
    }

    public String getString(SQLSyntax syntax) {
        return objectClass.getType().getString(object, syntax);
    }

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public void enumerate(ContextEnumerator enumerator) {
        enumerator.add(this);
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

    public int hashContext(HashContext hashContext) {
        return hashContext.hash(this);
    }

    // нельзя потому как при трансляции значения потеряются
/*    @Override
    public ValueExpr scale(int mult) {
        return new ValueExpr(((IntegralClass)objectClass).multiply((Number) object,mult),objectClass);
    }*/

    public ValueExpr translateQuery(QueryTranslator translator) {
        return this;
    }

    public BaseExpr translate(MapTranslate translator) {
        return translator.translate(this);
    }

    public VariableExprSet calculateExprFollows() {
        return new VariableExprSet();
    }

    public static ValueExpr ZERO = new ValueExpr(0, DoubleClass.instance);

    private static Set<ValueExpr> staticExprs;
    {
        staticExprs = new HashSet<ValueExpr>();
        staticExprs.add(ValueExpr.TRUE);
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
}
