package lsfusion.server.data.expr.where.ifs;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.interop.Compare;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.IsClassType;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.cases.ExprCase;
import lsfusion.server.data.expr.where.cases.ExprCaseList;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.ClassReader;
import lsfusion.server.data.type.NullReader;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassField;

import java.util.HashSet;
import java.util.Set;

public class NullExpr extends Expr {

    private NullExpr() {
    }
    public final static NullExpr instance = new NullExpr();

    public Type getType(KeyType keyType) {
        return null;
    }
    public Stat getTypeStat(Where fullWhere, boolean forJoin) {
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

    public Expr classExpr(ImSet<ClassField> classes, IsClassType type) {
        return this;
    }

    public Where isClass(ValueClassSet set, boolean inconsistent) {
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

    public boolean calcTwins(TwinImmutableObject o) {
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
