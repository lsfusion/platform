package lsfusion.server.logics.property.actions.importing;

import java.util.List;

public abstract class ImportIterator {
    
    protected abstract List<String> nextRow();
    
    protected abstract void release();
}
