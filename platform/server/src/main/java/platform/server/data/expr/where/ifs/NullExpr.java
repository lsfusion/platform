package platform.server.data.expr.where.ifs;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.type.Type;
import platform.server.data.type.ClassReader;
import platform.server.data.type.NullReader;
import platform.server.data.where.Where;
import platform.server.data.where.MapWhere;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.sql.SQLSyntax;
import platform.server.classes.BaseClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.caches.hash.HashContext;
import platform.interop.Compare;
import platform.base.TwinImmutableInterface;

public class NullExpr extends Expr {

    private NullExpr() {
    }
    public final static NullExpr instance = new NullExpr();

    public Type getType(KeyType keyType) {
        return null;
    }

    public int getWhereDepth() {
        return 0;
    }

    public ClassReader getReader(KeyType keyType) {
        return NullReader.instance;
    }

    public Where calculateWhere() {
        return Where.FALSE;
    }

    public ExprCaseList getCases() {
        throw new RuntimeException("not supported");
    }

    public Expr followFalse(Where where, boolean pack) {
        return this;
    }

    public Expr classExpr(BaseClass baseClass) {
        return this;
    }

    public Where isClass(AndClassSet set) {
        return Where.FALSE;
    }

    public Where compareBase(BaseExpr expr, Compare compareBack) {
        return Where.FALSE;
    }

    public Where compare(Expr expr, Compare compare) {
        return Where.FALSE;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }

    public VariableExprSet getExprFollows() {
        return new VariableExprSet();
    }

    protected long calculateComplexity() {
        return 1;
    }

    public boolean twins(TwinImmutableInterface o) {
        return true;
    }

    public int hashOuter(HashContext hashContext) {
        return 132;
    }

    public Expr translateOuter(MapTranslate translator) {
        return this;
    }

    public String getSource(CompileSource compile) {
        return SQLSyntax.NULL;
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public void enumDepends(ExprEnumerator enumerator) {
    }

    public Where getBaseWhere() {
        throw new RuntimeException("not supported");
    }

    public BaseExpr getBaseExpr() {
        throw new RuntimeException("not supported");
    }
}
