package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class GGrid extends GGridProperty {
    public GGroupObject groupObject;
    public boolean quickSearch;

    public GContainer record;

    @Override
    public List<GComponent> getChildren() { // a grid's record has no container: it is inside its grid
        return record != null ? Collections.singletonList(record) : Collections.emptyList();
    }

    @Override
    public List<GGroupObject> getGroups() {
        return Collections.singletonList(groupObject);
    }
    @Override
    public <R> R match(Function<GGrid, R> grid, Function<GTreeGroup, R> tree) {
        return grid.apply(this);
    }

    @Override
    protected GSize getExtraWidth() {
        return GSize.ZERO;
    }
}
