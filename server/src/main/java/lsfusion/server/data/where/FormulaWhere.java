package lsfusion.server.data.where;

import lsfusion.base.ArrayInstancer;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.innerjoins.KeyEquals;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.MeanClassWhere;
import lsfusion.server.data.where.classes.MeanClassWheres;

public abstract class FormulaWhere<WhereType extends Where> extends AbstractWhere {

    public final boolean check; // если true в неправильном состоянии

    protected final WhereType[] wheres;

    protected FormulaWhere(WhereType[] wheres, boolean check) {
        this.wheres = wheres;

        this.check = check;
    }

    protected abstract String getOp();

    public String getSource(CompileSource compile) {
        if(wheres.length==0) return getOp().equals("AND")? TRUE_STRING : FALSE_STRING;

        String result = "";
        for(Where where : wheres)
            result = (result.length()==0?"":result+" "+getOp()+" ") + where.getSource(compile);
        return "("+result+")";
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>toExclSet(wheres);
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return hashCoeff() + hashSetOuter(wheres, hashContext);
    }

    protected abstract int hashCoeff();

    protected static OrObjectWhere[] not(AndObjectWhere[] wheres) {
        OrObjectWhere[] result = new OrObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = wheres[i].not();
        return result;
    }

    protected static AndObjectWhere[] not(OrObjectWhere[] wheres) {
        AndObjectWhere[] result = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = wheres[i].not();
        return result;
    }

    int height;
    public int getHeight() {
        if(wheres.length==0) return 0;
        if(height==0) {
            int maxHeight = 0;
            for(int i=1;i<wheres.length;i++)
                if(wheres[i].getHeight()>wheres[maxHeight].getHeight())
                    maxHeight = i;
            height = wheres[maxHeight].getHeight()+1;
        }
        return height;
    }

    protected static <WhereType> WhereType[] substractWheres(WhereType[] wheres, WhereType[] substract, ArrayInstancer<WhereType> instancer) {
        if(substract.length>wheres.length) return null;

        WhereType[] rawRestWheres = wheres.clone();
        for(WhereType andWhere : substract) {
            boolean found = false;
            for(int i=0;i<rawRestWheres.length;i++)
                if(rawRestWheres[i]!=null && BaseUtils.hashEquals(andWhere,rawRestWheres[i])) {
                    rawRestWheres[i] = null;
                    found = true;
                    break;
                }
            if(!found) return null;
        }

        WhereType[] restWheres = instancer.newArray(wheres.length-substract.length); int rest=0;
        for(WhereType where : rawRestWheres)
            if(where!=null) restWheres[rest++] = where;
        return restWheres;
    }

    public ClassExprWhere calculateClassWhere() {
        return groupMeanClassWheres(true).getClassWhere();
    }

    protected abstract boolean checkFormulaTrue();

    private Boolean checkTrue = null;
    @ManualLazy
    public boolean checkTrue() {
        if(!check) {
            assert isTrue() == checkFormulaTrue();
            return isTrue();
        }

        if(checkTrue==null)
            checkTrue = checkFormulaTrue();
        return checkTrue;
    }

    protected abstract <K extends BaseExpr> GroupJoinsWheres calculateGroupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type);

    protected static <K extends BaseExpr> GroupJoinsWheres packIntermediate(GroupJoinsWheres result, GroupJoinsWheres.Type type, ImSet<K> keepStat, KeyStat keyStat, Where where, ImOrderSet<Expr> orderTop) {
        return result.pack(keepStat, keyStat, type, where, true, orderTop);
    }   
    
    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        GroupJoinsWheres result = calculateGroupJoinsWheres(keepStat, keyStat, orderTop, type);
        if(result.isExceededIntermediatePackThreshold())
            result = packIntermediate(result, type, keepStat, keyStat, this, orderTop);
        return result;
    }

    protected static MeanClassWheres compactHeuristic(MeanClassWheres result, Where where) {
        return new MeanClassWheres(new MeanClassWhere(result.getClassWhere()), where);
    }

    protected abstract MeanClassWheres calculateGroupMeanClassWheres(boolean useNots);
    public MeanClassWheres calculateMeanClassWheres(boolean useNots) {
        MeanClassWheres result = calculateGroupMeanClassWheres(useNots);
        if(!useNots && (result.size() > Settings.get().getLimitClassWhereCount() || result.getComplexity(true) > Settings.get().getLimitClassWhereComplexity()))
            result = compactHeuristic(result, this);
        return result;
    }

    protected abstract KeyEquals calculateGroupKeyEquals();
    public KeyEquals calculateKeyEquals() {
        if(isFalse())
            return KeyEquals.EMPTY;

        for(Where where : wheres)
            if(!where.getKeyEquals().isSimple)
                return calculateGroupKeyEquals();

        return new KeyEquals(this, true);
    }
}
