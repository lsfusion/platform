package lsfusion.server.logics.tasks;

import lsfusion.server.logics.property.Property;

public abstract class GroupPropertiesSingleTask extends GroupSingleTask<Property> {

    @Override
    protected boolean isGraph() {
        return true;
    }

    @Override
    protected String getElementCaption(Property element, int all, int current) {
        return null;
    }

    @Override
    protected String getElementCaption(Property element) {
        return element == null ? null : element.getSID();
    }

    @Override
    public String getCaption() {
        return null;
    }
}
