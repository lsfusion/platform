package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.view.GTableView;

import java.util.Collections;
import java.util.List;

public class GGrid extends GGridProperty {
    public GGroupObject groupObject;
    public boolean quickSearch;

    public Boolean resizeOverflow;

    public GContainer record;

    @Override
    public List<GComponent> getChildren() { // a grid's record has no container: it is inside its grid
        return record != null ? Collections.singletonList(record) : Collections.emptyList();
    }

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
