package lsfusion.server.data.where;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.Settings;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.MeanClassWhere;
import lsfusion.server.data.where.classes.MeanClassWheres;

public class NotWhere extends ObjectWhere {

    DataWhere where;
    NotWhere(DataWhere iWhere) {
        where = iWhere;
    }

    public boolean directMeansFrom(AndObjectWhere meanWhere) {
        for(OrObjectWhere orWhere : meanWhere.getOr())
            if(orWhere instanceof NotWhere && where.follow(((NotWhere)orWhere).where))
                return true;
        return false;
    }

    public boolean directMeansFromNot(AndObjectWhere[] notWheres, boolean[] used, int skip) {
        for(int i=0;i<notWheres.length;i++) {
            OrObjectWhere orWhere = notWheres[i].not();
            if (orWhere instanceof NotWhere && where.follow(((NotWhere)orWhere).where)) {
                OrWhere.markUsed(used, i, skip);
                return true;
            }
        }
        return false;
    }

    public DataWhere not() {
        return where;
    }

    final static String PREFIX = "NOT ";

    public boolean calcTwins(TwinImmutableObject o) {
        return where.equals(((NotWhere)o).where);
    }

    public int hash(HashContext hashContext) {
        return where.hashOuter(hashContext)*31;
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected Where translate(MapTranslate translator) {
        return where.translateOuter(translator).not();
    }
    public Where translate(ExprTranslator translator) {
        return where.translateExpr(translator).not();
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(where);
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return PREFIX + where.getSource(compile);

        return where.getNotSource(compile);
    }

    protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        where.fillDataJoinWheres(joins, andWhere);
    }

    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        GroupJoinsWheres notGroup;
        if(!Settings.get().isDisableGroupNotJoinsWheres() && (notGroup = where.groupNotJoinsWheres(keepStat, statType, keyStat, orderTop, type))!=null) // на самом деле СУБД часто умеет делать BitmapOr, но весьма редка ведет себя очень нестабильно, но если что можно будет попробовать потом отключить
            return notGroup;
        return super.groupJoinsWheres(keepStat, statType, keyStat, orderTop, type);
    }

    public MeanClassWheres calculateMeanClassWheres(boolean useNots) {
        if(useNots && where.isClassWhere())
            return new MeanClassWheres(new MeanClassWhere(where.getClassWhere(), true), this);
        return new MeanClassWheres(MeanClassWhere.TRUE,this);
    }

    public ClassExprWhere calculateClassWhere() {
        return ClassExprWhere.TRUE;
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        return where.packFollowFalse(falseWhere).not();
    }

    public boolean isNot() {
        return true;
    }
}
