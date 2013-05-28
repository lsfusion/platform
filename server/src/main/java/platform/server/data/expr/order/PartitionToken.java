package platform.server.data.expr.order;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.LongMutable;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.data.sql.SQLSyntax;

public abstract class PartitionToken {

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;

        next = ((MSet<PartitionCalc>)next).immutable();
    }

    private Object next = SetFact.mSet(); // где использовался

    public void addNext(PartitionCalc part) {
        ((MSet<PartitionCalc>)next).add(part);
    }
    @LongMutable
    public ImSet<PartitionCalc> getNext() {
        if(!finalized)
            finalizeInit();

        return (ImSet<PartitionCalc>)next;
    }

    public abstract String getSource(ImMap<PartitionToken, String> sources, SQLSyntax syntax);

    public abstract int getLevel();
}
