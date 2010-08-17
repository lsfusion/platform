package platform.server.data.where;

import platform.base.BaseUtils;
import platform.server.caches.ManualLazy;
import platform.server.caches.TwinLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.Query;
import platform.server.data.query.innerjoins.InnerSelectJoin;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWhere extends AbstractSourceJoin<Where> implements Where {

    public abstract AbstractWhere not();

    public Where and(Where where) {
        return and(where, false); // A AND B = not(notA OR notB)
    }
    public Where and(Where where, boolean packExprs) {
        return not().or(where.not(), packExprs).not(); // A AND B = not(notA OR notB)
    }
    public CheckWhere andCheck(CheckWhere where) {
        return not().orCheck(where.not()).not(); // A AND B = not(notA OR notB)
    }
    public Where or(Where where) {
        return or(where, false);
    }
    public Where or(Where where, boolean packExprs) {
//        assert BaseUtils.hashEquals(OrWhere.oldor(this, where, packExprs),OrWhere.newor(this, where, packExprs));
        return OrWhere.or(this, where, packExprs);
    }
    public CheckWhere orCheck(CheckWhere where) {
        return OrWhere.orCheck(this,where);
    }

    public Where followFalse(Where falseWhere) {
        return followFalse(falseWhere, false);
    }
    public Where followFalse(Where falseWhere, boolean packExprs) {
        Where result = followFalse(falseWhere, packExprs, new FollowChange());
        if(result instanceof ObjectWhere && OrWhere.checkTrue(this,falseWhere))
            result = TRUE;
//        assert BaseUtils.hashEquals(oldff(falseWhere, false, packExprs, new FollowChange()),result);
        return result;
    }
    public Where pack() { // собсно все packExprs нужен только для этого метода, он в свою очередь нужен чтобы в LinearExpr убрать лишние и в GroupExpr протолкнуть классы \ exprValues
        return followFalse(Where.FALSE, true);
    }

    public <K> Map<K, Expr> followTrue(Map<K, ? extends Expr> map) {
        Map<K, Expr> result = new HashMap<K, Expr>();
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().followFalse(not(), true));
        return result;
    }

    public boolean means(CheckWhere where) {
        return OrWhere.checkTrue(not(),where);
    }

    protected static Where toWhere(AndObjectWhere[] wheres) {
        return toWhere(wheres, false);
    }
    protected static Where toWhere(AndObjectWhere[] wheres, boolean check) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new OrWhere(wheres, check);
    }
    protected static Where toWhere(AndObjectWhere[] wheres, CheckWhere siblingsWhere) {
        if(wheres.length==1)
            return wheres[0];
        else
        if(wheres.length==0)
            return Where.FALSE;
        else
            return new OrWhere(wheres, ((OrWhere)siblingsWhere).check);
    }

    protected static Where toWhere(OrObjectWhere[] wheres) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new AndWhere(wheres, false);
    }
    protected static Where toWhere(OrObjectWhere[] wheres, CheckWhere siblingsWhere) {
        if(wheres.length==1)
            return wheres[0];
        else
        if(wheres.length==0)
            return Where.TRUE;
        else
            return new AndWhere(wheres, ((AndWhere)siblingsWhere).check);
    }

    protected static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i) {
        AndObjectWhere[] siblings = new AndObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }
    protected static Where siblingsWhere(AndObjectWhere[] wheres, int i) {
        return toWhere(siblings(wheres,i));
    }
    protected static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i,int numWheres) {
        AndObjectWhere[] siblings = new AndObjectWhere[numWheres-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,numWheres-i-1);
        return siblings;
    }
    protected static OrObjectWhere[] siblings(OrObjectWhere[] wheres,int i) {
        OrObjectWhere[] siblings = new OrObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }

    public ClassExprWhere classWhere = null;
    @ManualLazy
    public ClassExprWhere getClassWhere() {
        if(classWhere==null)
            classWhere = calculateClassWhere();
        return classWhere;
    }
    public abstract ClassExprWhere calculateClassWhere();


    public MeanClassWheres meanClassWheres = null;
    @ManualLazy
    public MeanClassWheres groupMeanClassWheres() {
        if(meanClassWheres==null)
            meanClassWheres = calculateMeanClassWheres();
        return meanClassWheres;
    }
    public abstract MeanClassWheres calculateMeanClassWheres();

    private Map<BaseExpr, BaseExpr> getExprValues(boolean and) {
        Map<BaseExpr, BaseExpr> result = new HashMap<BaseExpr, BaseExpr>();
        for(Where opWhere : and?getOr():getAnd())
            if(opWhere instanceof EqualsWhere) {
                CompareWhere where = (CompareWhere)opWhere;
                if(where.operator1.isValue() && !(where.operator2 instanceof KeyExpr && and))
                    result.put(where.operator2, where.operator1);
                else
                if(where.operator2.isValue() && !(where.operator1 instanceof KeyExpr && and))
                    result.put(where.operator1, where.operator2);
            }
        return result;
    }

    @TwinLazy
    public Map<BaseExpr, BaseExpr> getExprValues() {
        return getExprValues(true);
    }

    @TwinLazy
    public Map<BaseExpr, BaseExpr> getNotExprValues() {
        return not().getExprValues(false);
    }

    public Where map(Map<KeyExpr, ? extends Expr> map) {
        return new Query<KeyExpr,Object>(BaseUtils.toMap(map.keySet()),this).join(map).getWhere();
    }

    public Collection<InnerSelectJoin> getInnerJoins(boolean notExclusive, boolean noJoins) {
        if(notExclusive)
            return getKeyEquals().getInnerJoins(noJoins);
        else {
            KeyEquals keyEquals = getKeyEquals();
            Collection<InnerSelectJoin> innerJoins = keyEquals.getInnerJoins(noJoins); // получаем notExclusive
            if(innerJoins.size()<=1) // exclusive по определению
                return innerJoins;

            InnerSelectJoin firstJoin = innerJoins.iterator().next();
            return BaseUtils.add(keyEquals.getWhere().and(firstJoin.fullWhere.not()).getInnerJoins(false,noJoins),firstJoin); // assert что keyEquals.getWhere тоже самое что this только упрощенное транслятором
        }
    }

    public Type getKeyType(KeyExpr expr) {
        return getClassWhere().getType(expr);
    }

    public Where getKeepWhere(KeyExpr expr) {
        return getClassWhere().getKeepWhere(expr);
    }

    public abstract Where translateOuter(MapTranslate translator);

    protected abstract KeyEquals calculateKeyEquals();

    private KeyEquals keyEquals = null;
    @ManualLazy
    public KeyEquals getKeyEquals() {
        if(keyEquals == null)
            keyEquals = calculateKeyEquals();
        return keyEquals;
    }
}
