package lsfusion.server.logics.property.actions.importing;

public abstract class ImportIterator {
    protected abstract Object nextRow();
    protected abstract void release();
}