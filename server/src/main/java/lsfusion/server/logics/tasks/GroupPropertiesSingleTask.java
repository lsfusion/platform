package lsfusion.server.logics.tasks;

import lsfusion.server.logics.DBManager;

public abstract class GroupPropertiesSingleTask extends GroupSingleTask<Object> {

    @Override
    protected boolean isGraph() {
        return true;
    }

    @Override
    protected String getElementCaption(Object element, int all, int current) {
        return null;
    }

    @Override
    public String getCaption() {
        return null;
    }

    public DBManager getDbManager() {
        return getBL().getDbManager();
    }
}
