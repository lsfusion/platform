package lsfusion.server.data.expr.where.ifs;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.filter.user.Compare;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.where.cases.ExprCase;
import lsfusion.server.data.expr.where.cases.ExprCaseList;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.type.reader.NullReader;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClassSet;

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

    public Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type) {
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

    public Expr translate(ExprTranslator translator) {
        return this;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return true;
    }

    public int hash(HashContext hashContext) {
        return 132;
    }

    protected Expr translate(MapTranslate translator) {
        return this;
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return SQLSyntax.NULL;
    }

    public void fillJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.EMPTY();
    }

    public Where getBaseWhere() {
        throw new RuntimeException("not supported");
    }

    public ImSet<BaseExpr> getBaseExprs() {
        return SetFact.EMPTY();
    }

    public ObjectValue getObjectValue(QueryEnvironment env) {
        return NullValue.instance;
    }

    public ConcreteClass getStaticClass() {
        return null;
    }

    @Override
    public boolean isAlwaysPositiveOrNull() {
        return true;
    }
}
