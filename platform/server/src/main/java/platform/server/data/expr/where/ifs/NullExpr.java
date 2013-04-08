package platform.server.data.expr.where.ifs;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.interop.Compare;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ValueClassSet;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.ExprCase;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.ClassReader;
import platform.server.data.type.NullReader;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassField;

import java.util.HashSet;
import java.util.Set;

public class NullExpr extends Expr {

    private NullExpr() {
    }
    public final static NullExpr instance = new NullExpr();

    public Type getType(KeyType keyType) {
        return null;
    }
    public Stat getTypeStat(Where fullWhere) {
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
        return new ExprCaseList(SetFact.<ExprCase>EMPTY());
    }

    public Expr followFalse(Where where, boolean pack) {
        return this;
    }

    public Expr classExpr(ImSet<ClassField> classes) {
        return this;
    }

    public Where isClass(ValueClassSet set) {
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

    public boolean twins(TwinImmutableObject o) {
        return true;
    }

    protected int hash(HashContext hashContext) {
        return 132;
    }

    protected Expr translate(MapTranslate translator) {
        return this;
    }

    public String getSource(CompileSource compile) {
        return SQLSyntax.NULL;
    }

    public void fillJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.EMPTY();
    }

    public Where getBaseWhere() {
        throw new RuntimeException("not supported");
    }

    public Set<BaseExpr> getBaseExprs() {
        return new HashSet<BaseExpr>();
    }

    public ObjectValue getObjectValue() {
        return NullValue.instance;
    }

    public ConcreteClass getStaticClass() {
        return null;
    }
}
