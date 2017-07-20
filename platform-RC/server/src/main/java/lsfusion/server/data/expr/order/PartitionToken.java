package lsfusion.server.data.expr.order;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

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
