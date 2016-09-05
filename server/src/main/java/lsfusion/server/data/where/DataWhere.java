package lsfusion.server.data.where;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.innerjoins.DataUpWhere;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.innerjoins.UpWhere;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.where.classes.MeanClassWhere;
import lsfusion.server.data.where.classes.MeanClassWheres;


abstract public class DataWhere extends ObjectWhere {

    public boolean directMeansFrom(AndObjectWhere where) {
        for(OrObjectWhere orWhere : where.getOr())
            if(orWhere instanceof DataWhere && ((DataWhere)orWhere).follow(this))
                return true;
        return false;
    }

    public boolean directMeansFromNot(AndObjectWhere[] notWheres, boolean[] used, int skip) {
        for(int i=0;i<notWheres.length;i++) {
            OrObjectWhere orWhere = notWheres[i].not();
            if (orWhere instanceof DataWhere && ((DataWhere) orWhere).follow(this)) {
                OrWhere.markUsed(used, i, skip);
                return true;
            }
        }
        return false;
    }

    public NotWhere not = null;
    @ManualLazy
    public NotWhere not() {  // именно здесь из-за того что типы надо перегружать без generics
        if(not==null)
            not = new NotWhere(this);
        return not;
    }

    public boolean follow(DataWhere dataWhere) {
        return getEqualFollows().contains(dataWhere);
    }

    // возвращает себя и все зависимости
    private ImSet<DataWhere> equalFollows = null;
    @ManualLazy
    public ImSet<DataWhere> getEqualFollows() {
        if(equalFollows ==null)
            equalFollows = SetFact.addExcl(calculateFollows(), this);
        return equalFollows;
    }

    // определяет все
    protected ImSet<DataWhere> calculateFollows() {
        ImSet<NullableExprInterface> exprFollows = getExprFollows();
        MSet<DataWhere> result = SetFact.mSet();
        for(int i=0,size=exprFollows.size();i<size;i++)
            exprFollows.get(i).fillFollowSet(result);
        return result.immutable();
    }

    protected abstract ImSet<NullableExprInterface> getExprFollows();

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected String getNotSource(CompileSource compile) {
        return NotWhere.PREFIX + getSource(compile);
    }

    public MeanClassWheres calculateMeanClassWheres(boolean useNots) {
        return new MeanClassWheres(getMeanClassWhere(),this);
    }

    protected MeanClassWhere getMeanClassWhere() {
        return new MeanClassWhere(getClassWhere());
    }

    public static Where create(DataWhere where) {
        if(where.getClassWhere().isFalse())
            return Where.FALSE;
        else
            return where;
    }

    public <K extends BaseExpr> GroupJoinsWheres groupNotJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        return null;
    }

    protected GroupJoinsWheres groupDataJoinsWheres(WhereJoin join, GroupJoinsWheres.Type type) {
        return new GroupJoinsWheres(join, getUpWhere(), this, type);
    }

    protected GroupJoinsWheres groupDataNotJoinsWheres(WhereJoin join, GroupJoinsWheres.Type type) {
        return new GroupJoinsWheres(join, getUpWhere().not(), not(), type);
    }

    protected UpWhere getUpWhere() {
        return new DataUpWhere(this);
    }

    public boolean isNot() {
        return false;
    }
}
