package lsfusion.server.logics.tasks;

import lsfusion.server.logics.property.AggregateProperty;

public abstract class GroupPropertiesSingleTask extends GroupSingleTask<AggregateProperty> {

    @Override
    protected boolean isGraph() {
        return true;
    }

    @Override
    protected String getElementCaption(AggregateProperty element, int all, int current) {
        return null;
    }

    @Override
    protected String getElementCaption(AggregateProperty element) {
        return null;
    }

    @Override
    public String getCaption() {
        return null;
    }
}
