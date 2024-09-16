package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.view.GTableView;

public class GGrid extends GGridProperty {
    public GGroupObject groupObject;
    public boolean quickSearch;

    public Boolean resizeOverflow;

    public GContainer record;

    public Boolean boxed;

    @Override
    protected GGroupObject getLastGroup() {
        return groupObject;
    }

    @Override
    protected GSize getExtraWidth() {
        return GSize.ZERO;
    }

    public boolean isBoxed(GTableView table) {
        if(boxed != null)
            return boxed;

        return table.isDefaultBoxed();
    }
}
