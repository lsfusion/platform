package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.where.Case;
import lsfusion.server.data.where.Where;

import java.util.Iterator;

public abstract class CaseList<A, D extends A,C extends Case<D>> implements Iterable<C> {

    public Iterator<C> iterator() {
        if(exclusive)
            return ((ImSet<C>)list).iterator();
        else
            return ((ImList<C>)list).iterator();
    }

    public int size() {
        if(exclusive)
            return ((ImSet<C>)list).size();
        else
            return ((ImList<C>)list).size();
    }

    public C get(int i) {
        if(exclusive)
            return ((ImSet<C>)list).get(i);
        else
            return ((ImList<C>)list).get(i);
    }

    protected Object list;
    public final boolean exclusive;
    
    protected CaseList(ImList<C> list) {
        this.list = list;
        this.exclusive = false;
    }

    protected CaseList(ImSet<C> list) {
        this.list = list;
        this.exclusive = true;

//        assert checkExclusiveness(); // далеко не всегда будет выполняться в частности при изменении классов
    }

//    protected boolean checkExclusiveness() {
//        ImSet<C> set = (ImSet<C>) list;
//        for(int i=0,size=set.size();i<size;i++) {
//            for(int j=i+1;j<size;j++) {
//                if(!set.get(i).where.and(set.get(j).where).not().checkTrue())
//                    if(!set.toString().contains("_CLASS_"))
//                        set = set;
//            }
//        }
//        return true;
//    }

    public boolean equals(Object obj) {
        return obj instanceof CaseList && exclusive == ((CaseList)obj).exclusive && list.equals(((CaseList)obj).list);
    }

    public int immutableHashCode() {
        return list.hashCode() + (exclusive ? 1 : 0);
    }

    public Where getWhere(GetValue<Where, D> caseInterface) {

        Where result = Where.FALSE;
        Where up = Where.FALSE;
        for(C cCase : this) {
            Where caseWhere = cCase.where.and(caseInterface.getMapValue(cCase.data));
            if(!exclusive) {
                caseWhere = caseWhere.and(up.not());
                up = up.or(cCase.where);
                result = result.or(caseWhere);
            } else
                result = result.exclOr(caseWhere);
        }

        return result;
    }
}
