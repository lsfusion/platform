package lsfusion.server.logics.property.actions.importing;

public abstract class SingleImportFormIterator {
    protected abstract Object nextRow(String key);
    protected abstract void release();
}