package platform.server.data.where;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.translator.HashLazy;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.Arrays;

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

    public void enumDepends(ExprEnumerator enumerator) {
        for(Where where : wheres)
            where.enumerate(enumerator);
    }

    @HashLazy
    public int hashOuter(HashContext hashContext) {
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
        return groupMeanClassWheres().getClassWhere();
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

    public long calculateComplexity() {
        return getComplexity(Arrays.asList(wheres)) + 1;
    }
}
