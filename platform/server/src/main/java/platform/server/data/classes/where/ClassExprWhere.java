package platform.server.data.classes.where;

import platform.server.data.query.exprs.VariableClassExpr;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.Collection;

public class ClassExprWhere extends AbstractClassWhere<VariableClassExpr,AndClassExprWhere,ClassExprWhere> {

    public Type getType(KeyExpr keyExpr) {
        assert wheres.length>0;
        Type type = wheres[0].get(keyExpr).getType();
        assert checkType(keyExpr,type);
        return type;
    }

    public boolean checkType(KeyExpr keyExpr,Type type) {
        for(int i=1;i<wheres.length;i++)
            assert type.isCompatible(wheres[0].get(keyExpr).getType());
        return true;
    }

    // получает Where который можно использовать в followFalse'ах, and'ах и  
    public Where toMeanWhere(Collection<? extends AndExpr> notNull) {
        return new MeanClassWhere(notNull,this);
    }

    public Where toWhere() {
        Where result = Where.FALSE;
        for(AndClassExprWhere where : wheres)
            result = result.or(where.toWhere());
        return result;
    }

    public boolean means(Where where) {
        for(AndClassExprWhere and : wheres)
            if(!new ClassExprWhere(and).toMeanWhere(and.keySet()).means(where)) return false;
        return true;
    }

    protected ClassExprWhere getThis() {
        return this;
    }

    private ClassExprWhere(AndClassExprWhere[] iWheres) {
        super(iWheres);
    }

    protected ClassExprWhere createThis(AndClassExprWhere[] iWheres) {
        return new ClassExprWhere(iWheres);
    }

    private ClassExprWhere(AndClassExprWhere where) {
        super(where);
    }
    public static ClassExprWhere TRUE = new ClassExprWhere(new AndClassExprWhere());
    private ClassExprWhere() {
    }
    public static ClassExprWhere FALSE = new ClassExprWhere();

    public ClassExprWhere(VariableClassExpr key, ClassSet classes) {
        super(new AndClassExprWhere(key, classes));
    }

    protected AndClassExprWhere[] newArray(int size) {
        return new AndClassExprWhere[size];
    }

    public ClassExprWhere andEquals(VariableClassExpr expr, VariableClassExpr to) {
        AndClassExprWhere[] rawAndWheres = new AndClassExprWhere[wheres.length]; int num=0;
        for(AndClassExprWhere where : wheres) {
            AndClassExprWhere andWhere = new AndClassExprWhere(where);
            if (andWhere.add(to, where.get(expr)))
                rawAndWheres[num++] = andWhere;
        }
        AndClassExprWhere[] andWheres = new AndClassExprWhere[num]; System.arraycopy(rawAndWheres,0,andWheres,0,num);
        return new ClassExprWhere(andWheres);
    }
}
