package platform.server.data.where;

import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.where.classes.MeanClassWhere;
import platform.server.data.query.CompileSource;
import platform.server.caches.ManualLazy;
import platform.base.BaseUtils;


abstract public class DataWhere extends ObjectWhere {

    public boolean directMeansFrom(AndObjectWhere where) {
        return where instanceof DataWhere && ((DataWhere)where).follow(this);
    }

    public NotWhere not = null;
    @ManualLazy
    public NotWhere not() {  // именно здесь из-за того что типы надо перегружать без generics
        if(not==null)
            not = new NotWhere(this);
        return not;
    }

    public boolean follow(DataWhere dataWhere) {
        return BaseUtils.hashEquals(this, dataWhere) || getFollows().contains(dataWhere);
    }

    // возвращает себя и все зависимости
    private DataWhereSet follows = null;
    @ManualLazy
    public DataWhereSet getFollows() {
        if(follows==null)
            follows = calculateFollows();
        return follows;
    }

    // определяет все
    protected abstract DataWhereSet calculateFollows();

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected String getNotSource(CompileSource compile) {
        return NotWhere.PREFIX + getSource(compile);
    }

    public MeanClassWheres calculateMeanClassWheres() {
        return new MeanClassWheres(getMeanClassWhere(),this);
    }

    public MeanClassWhere getMeanClassWhere() {
        return new MeanClassWhere(getClassWhere());
    }

    public static Where create(DataWhere where) {
        if(where.getClassWhere().isFalse())
            return Where.FALSE;
        else
            return where;
    }
}
