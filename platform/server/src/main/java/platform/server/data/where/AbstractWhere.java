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
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.type.Type;
import platform.server.classes.sets.AndClassSet;

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
    public Where andMeans(Where where) {
        return not().orMeans(where.not()).not(); // A AND B = not(notA OR notB)
    }
    public Where or(Where where) {
        return or(where, false);
    }
    public Where or(Where where, boolean packExprs) {
        return OrWhere.op(this,where, FollowDeep.inner(packExprs));
    }
    public Where orMeans(Where where) {
        return OrWhere.opPlain(this,where);
    }

    public Where followFalse(Where falseWhere) {
        return followFalse(falseWhere, false);
    }
    public Where followFalse(Where falseWhere, boolean packExprs) {
        return OrWhere.followFalse(this,falseWhere, FollowDeep.inner(packExprs),false);
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

    public boolean means(Where where) {
        return OrWhere.orTrue(not(),where);
    }

    static Where toWhere(AndObjectWhere[] wheres, FollowDeep followDeep) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new OrWhere(wheres, followDeep);
    }
    public static Where toWhere(AndObjectWhere[] wheres, Where siblingsWhere) {
        if(wheres.length==1)
            return wheres[0];
        else
        if(wheres.length==0)
            return Where.FALSE;
        else // очевидно что сиблинги > 1 могут быть только не FormulaWhere
            return new OrWhere(wheres, ((FormulaWhere)siblingsWhere).followDeep);
    }
    static Where toWhere(AndObjectWhere[] wheres,int numWheres, FollowDeep followDeep) {
        if(numWheres==1)
            return wheres[0];
        else {
            AndObjectWhere[] compiledWheres = new AndObjectWhere[numWheres]; System.arraycopy(wheres,0,compiledWheres,0,numWheres);
            return new OrWhere(compiledWheres, followDeep);
        }
    }
    static Where toWhere(AndObjectWhere[] wheres,int numWheres, Where siblingsWhere) {
        if(numWheres==1)
            return wheres[0];
        else
        if(numWheres==0)
            return Where.FALSE;
        else { // очевидно что сиблинги > 1 могут быть только не FormulaWhere
            AndObjectWhere[] compiledWheres = new AndObjectWhere[numWheres]; System.arraycopy(wheres,0,compiledWheres,0,numWheres);
            return new OrWhere(compiledWheres, ((FormulaWhere)siblingsWhere).followDeep);
        }
    }
    public static Where toWhere(OrObjectWhere[] wheres, FollowDeep followDeep) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new AndWhere(wheres, followDeep);
    }
    public static Where toWhere(OrObjectWhere[] wheres, Where siblingsWhere) {
        if(wheres.length==1)
            return wheres[0];
        else
        if(wheres.length==0)
            return Where.TRUE;
        else  // очевидно что сиблинги > 1 могут быть только не FormulaWhere
            return new AndWhere(wheres, ((FormulaWhere)siblingsWhere).followDeep);
    }
    static Where toWhere(OrObjectWhere[] wheres,int numWheres, FollowDeep followDeep) {
        if(numWheres==1)
            return wheres[0];
        else {
            OrObjectWhere[] compiledWheres = new OrObjectWhere[numWheres]; System.arraycopy(wheres,0,compiledWheres,0,numWheres);
            return new AndWhere(compiledWheres, followDeep);
        }
    }

    // системные
    static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i) {
        AndObjectWhere[] siblings = new AndObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }
    static Where siblingsWhere(AndObjectWhere[] wheres,int i,FollowDeep followDeep) {
        return toWhere(siblings(wheres,i), followDeep);
    }
    static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i,int numWheres) {
        AndObjectWhere[] siblings = new AndObjectWhere[numWheres-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,numWheres-i-1);
        return siblings;
    }
    static Where siblingsWhere(AndObjectWhere[] wheres,int i,int numWheres, FollowDeep followDeep) {
        return toWhere(siblings(wheres,i,numWheres), followDeep);
    }
    static OrObjectWhere[] siblings(OrObjectWhere[] wheres,int i) {
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

    public Map<BaseExpr, BaseExpr> getExprValues(boolean and) {
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

    @TwinLazy
    public Map<KeyExpr, BaseExpr> getKeyExprs() {
        Map<KeyExpr, BaseExpr> result = new HashMap<KeyExpr, BaseExpr>();
        for(OrObjectWhere orWhere : getOr())
            if(orWhere instanceof EqualsWhere) {
                CompareWhere where = (CompareWhere)orWhere;
                if(where.operator1 instanceof KeyExpr && !where.operator2.hasKey((KeyExpr) where.operator1))
                    result.put((KeyExpr) where.operator1, where.operator2);
                else
                if(where.operator2 instanceof KeyExpr && !where.operator1.hasKey((KeyExpr) where.operator2))
                    result.put((KeyExpr) where.operator2, where.operator1);
            }
        return result;
    }

    public Where map(Map<KeyExpr, ? extends Expr> map) {
        return new Query<KeyExpr,Object>(BaseUtils.toMap(map.keySet()),this).join(map).getWhere();
    }

    public KeyEquals getKeyEquals() { // для того чтобы устранить рекурсивные keyEquals
        KeyEquals result = new KeyEquals();

        KeyEquals keyEquals = groupKeyEquals();
        for(int i=0;i<keyEquals.size;i++) {
            KeyEqual keyEqual = keyEquals.getKey(i);
            Where where = keyEquals.getValue(i);

            if(keyEqual.isEmpty())
                result.add(keyEqual, where);
            else {
                Where transWhere = where.translateQuery(keyEqual.getTranslator()); // может еще создать keyEquals
                KeyEquals transKeyEquals = transWhere.getKeyEquals();
                for(int j=0;j<transKeyEquals.size;j++)
                    result.add(keyEqual.and(transKeyEquals.getKey(j)), transKeyEquals.getValue(j)); // assert keyEqual не пересекается с transKeyEqual
            }
        }
        
        return result;
    }

    public Collection<InnerSelectJoin> getInnerJoins() {
        return getKeyEquals().getInnerJoins(true);
    }

    public Type getKeyType(KeyExpr expr) {
        return getClassWhere().getType(expr);
    }

    public AndClassSet getKeepClass(KeyExpr expr) {
        return getClassWhere().getKeepClass(expr);
    }
}
