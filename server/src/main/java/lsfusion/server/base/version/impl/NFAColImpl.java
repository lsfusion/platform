package lsfusion.server.base.version.impl;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFColChange;

public abstract class NFAColImpl<T, CH extends NFColChange<T>, F extends Iterable<T>> extends NFColChangeImpl<T, CH, F> {

    protected NFAColImpl() {
    }

    protected NFAColImpl(F changes) {
        super(changes);
    }
    
    protected abstract ImCol<T> getFinalCol(F fcol);  
    
    public ImCol<T> getNFCol(Version version) {
        return getNFCol(version, false);
    }

    private ImCol<T> getNFCol(Version version, boolean allowRead) {
        F result = proceedVersionFinal(version, allowRead);
        if(result!=null)
            return getFinalCol(result);
        
        final MCol<T> mCol = ListFact.mCol();
        proceedChanges(change -> change.proceedCol(mCol, version), version);
        return mCol.immutableCol();
    }

    public Iterable<T> getNFIt(Version version) {
        return getNFCol(version);
    }

    public Iterable<T> getNFCopyIt(Version version) {
        return getNFCol(version, true);
    }
}
