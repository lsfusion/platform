package platform.server.where;

import platform.server.data.classes.where.MeanClassWheres;
import platform.server.data.query.CompileSource;


abstract public class DataWhere extends ObjectWhere<NotWhere> {

    // определяет все
    protected abstract DataWhereSet getExprFollows();

    public boolean directMeansFrom(AndObjectWhere where) {
        return where instanceof DataWhere && ((DataWhere)where).follow(this);
    }

    public NotWhere calculateNot() {
        return new NotWhere(this);
    }

    public boolean follow(DataWhere dataWhere) {
        return getFollows().contains(dataWhere);
    }

    public ObjectWhereSet calculateObjects() {
        return new ObjectWhereSet(this);
    }

    public Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects) {
        if(decompose.followNot.contains(this))
            return TRUE;
        else {
            objects.data.add(this);
            objects.followData.addAll(getFollows());
            return this;
        }
    }

    // возвращает себя и все зависимости
    private DataWhereSet follows = null;
    public DataWhereSet getFollows() {
        if(follows==null) {
            follows = new DataWhereSet(getExprFollows());
            follows.add(this);
        }
        return follows;
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected String getNotSource(CompileSource compile) {
        return NotWhere.PREFIX + getSource(compile);
    }

    public MeanClassWheres calculateMeanClassWheres() {
        return new MeanClassWheres(getClassWhere(),this);
    }

    public static Where create(DataWhere where) {
        if(where.getClassWhere().isFalse())
            return Where.FALSE;
        else
            return where;
    }
}
