package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.expr.where.Case;
import lsfusion.server.data.where.Where;

public abstract class MCaseList<A, D extends A,C extends Case<D>> {

    protected final boolean exclusive;

    private final Object list;
    protected Where upWhere;

    public MCaseList(boolean exclusive) {
        this(Where.FALSE, exclusive);
    }

    public MCaseList(Where falseWhere, boolean exclusive) {
        upWhere = falseWhere;
        this.exclusive = exclusive;
        
        if(exclusive) // можно было бы size протянуть впоследствие
            list = SetFact.mExclSet();
        else
            list = ListFact.mList();
    }

    protected C get(int i) {
        return ((MList<C>)list).get(i);
    }
    protected int size() {
        if(exclusive)
            return ((MExclSet<C>)list).size();
        else
            return ((MList<C>)list).size();
    }
    protected C single() {
        if(exclusive)
            return ((MExclSet<C>)list).single();
        else
            return ((MList<C>)list).get(0);
    }
    protected void add(C addCase) {
        if(exclusive)
            ((MExclSet<C>)list).exclAdd(addCase);
        else
            ((MList<C>)list).add(addCase);
    }
    public ImSet<C> immutableSet() {
        return ((MExclSet<C>)list).immutable();
    }
    public ImList<C> immutableList() {
        return ((MList<C>)list).immutableList();
    }
    
    public abstract void add(Where where, A data);
    public abstract A getFinal();
}
