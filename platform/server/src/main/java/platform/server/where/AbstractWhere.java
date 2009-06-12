package platform.server.where;

import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.exprs.SourceExpr;

import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.Immutable;

@Immutable
public abstract class AbstractWhere<Not extends Where> extends AbstractSourceJoin<Where> implements Where<Not> {

    private Not not = null;
    public Not not() {
        if(not==null)
            not = getNot();
        return not;
    }
    abstract Not getNot();

    public Where and(Where where) {
        return not().or(where.not()).not(); // A AND B = not(notA OR notB)
    }
    public Where andMeans(Where where) {
        return not().orMeans(where.not()).not(); // A AND B = not(notA OR notB)
    }
    public Where or(Where where) {
        Where or = OrWhere.op(this,where,false);
        assert !(!or.isTrue() && or.checkTrue());
        return or;
    }
    public Where orMeans(Where where) {
        return OrWhere.op(this,where,true);
    }

    public Where followFalse(Where falseWhere) {
        return OrWhere.followFalse(this,falseWhere,false,false);
    }

    public <K> Map<K, SourceExpr> followTrue(Map<K, ? extends SourceExpr> map) {
        Map<K,SourceExpr> result = new HashMap<K, SourceExpr>();
        for(Map.Entry<K,? extends SourceExpr> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().followFalse(not()));
        return result;
    }

    public boolean means(Where where) {
        return OrWhere.op(not(),where,true).checkTrue();
    }

    static Where toWhere(AndObjectWhere[] wheres) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new OrWhere(wheres);
    }
    static Where toWhere(AndObjectWhere[] wheres,int numWheres) {
        if(numWheres==1)
            return wheres[0];
        else {
            AndObjectWhere[] compiledWheres = new AndObjectWhere[numWheres]; System.arraycopy(wheres,0,compiledWheres,0,numWheres);
            return new OrWhere(compiledWheres);
        }
    }
    static Where toWhere(OrObjectWhere[] wheres) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new AndWhere(wheres);
    }
    static Where toWhere(OrObjectWhere[] wheres,int numWheres) {
        if(numWheres==1)
            return wheres[0];
        else {
            OrObjectWhere[] compiledWheres = new OrObjectWhere[numWheres]; System.arraycopy(wheres,0,compiledWheres,0,numWheres);
            return new AndWhere(compiledWheres);
        }
    }

    // не пересекаются ни в одном направлении
    static boolean decomposed(Where where1,Where where2) {
        return where1.getObjects().depends(where2.getObjects());
    }

    // системные
    static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i) {
        AndObjectWhere[] siblings = new AndObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }
    static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i,int numWheres) {
        AndObjectWhere[] siblings = new AndObjectWhere[numWheres-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,numWheres-i-1);
        return siblings;
    }
    static OrObjectWhere[] siblings(OrObjectWhere[] wheres,int i) {
        OrObjectWhere[] siblings = new OrObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }


    abstract ObjectWhereSet calculateObjects();
    private ObjectWhereSet objects = null;
    public ObjectWhereSet getObjects() {
        if(objects==null) objects = calculateObjects();
        return objects;
    }

    private ClassExprWhere classWhere=null;
    public ClassExprWhere getClassWhere() {
        if(classWhere==null)
            classWhere = calculateClassWhere();
        return classWhere;
    }

    protected abstract ClassExprWhere calculateClassWhere();
}
