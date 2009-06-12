package platform.server.where;

import platform.base.BaseUtils;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.QueryData;
import platform.server.data.query.Context;
import platform.server.data.query.MapContext;
import platform.server.data.sql.SQLSyntax;

import java.util.Map;


abstract class FormulaWhere<Not extends FormulaWhere,WhereType extends Where> extends AbstractWhere<Not> {

    protected final WhereType[] wheres;
    protected FormulaWhere(WhereType[] iWheres) {
        wheres = iWheres;
    }

    abstract String getOp();
    public String toString() {
        if(wheres.length==0) return getOp().equals("AND")?"TRUE":"FALSE";

        String result = "";
        for(Where where : wheres)
            result = (result.length()==0?"":result+" "+getOp()+" ") + where;
        return "("+result+")";
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        if(wheres.length==0) return getOp().equals("AND")? TRUE_STRING : FALSE_STRING;

        String result = "";
        for(Where where : wheres)
            result = (result.length()==0?"":result+" "+getOp()+" ") + where.getSource(queryData, syntax);
        return "("+result+")";
    }

    public int fillContext(Context context, boolean compile) {
        int level = -1;
        for(Where where : wheres)
            level = BaseUtils.max(where.fillContext(context, compile),level);
        return level;
    }

    public boolean equals(Where where, MapContext mapContext) {
        if(where.getClass()!=getClass()) return false;

        FormulaWhere thisWhere = (FormulaWhere)where;

        if(wheres.length!= thisWhere.wheres.length) return false;

        Where[] checkWheres = thisWhere.wheres.clone();
        for(Where andWhere : wheres) {
            boolean found = false;
            for(int i=0;i<checkWheres.length;i++)
                if(checkWheres[i]!=null && andWhere.equals(checkWheres[i], mapContext)) {
                    checkWheres[i] = null;
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    protected int getHash() {
        int result = hashCoeff();
        for(Where where : wheres)
            result += where.hash();
        return result;
    }

    abstract int hashCoeff();

    // ручной кэш хэша
    protected int getHashCode() {
        return BaseUtils.hashArraySet(wheres)*hashCoeff();
    }

    public ObjectWhereSet calculateObjects() {
        if(wheres.length==0)
            return new ObjectWhereSet();
        else {
            ObjectWhereSet result = new ObjectWhereSet(wheres[0].getObjects());
            for(int i=1;i<wheres.length;i++)
                result.addAll(wheres[i].getObjects());
            return result;
        }
    }

    static OrObjectWhere[] not(AndObjectWhere[] wheres) {
        OrObjectWhere[] result = new OrObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = ((AndObjectWhere<?>)wheres[i]).not();
        return result;
    }

    static AndObjectWhere[] not(OrObjectWhere[] wheres) {
        AndObjectWhere[] result = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = ((OrObjectWhere<?>)wheres[i]).not();
        return result;
    }

    // из массива or'ов определяет массив and'ов
    static AndObjectWhere[] reverse(OrObjectWhere[] wheres) {
        if(wheres.length==1) {
            if(wheres[0] instanceof ObjectWhere)
                return new AndObjectWhere[]{(ObjectWhere)wheres[0]};
            else // значит OrWhere
                return ((OrWhere)wheres[0]).wheres;
        } else
            return new AndObjectWhere[]{new AndWhere(wheres)};
    }

    static AndObjectWhere[] reverseNot(AndObjectWhere[] wheres) {
        return reverse(not(wheres));
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

    // отнимает одно мн-во от второго
    WhereType[] substractWheres(WhereType[] substract) {
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

        WhereType[] restWheres = newArray(wheres.length-substract.length); int rest=0;
        for(WhereType where : rawRestWheres)
            if(where!=null) restWheres[rest++] = where;
        return restWheres;
    }
    abstract WhereType[] newArray(int length);

    public ClassExprWhere calculateClassWhere() {
        return getMeanClassWheres().orMeans();
    }
}
