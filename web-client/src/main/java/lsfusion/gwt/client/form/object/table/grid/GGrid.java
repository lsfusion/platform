package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.view.GTableView;

public class GGrid extends GComponent {
    public GGroupObject groupObject;
    public boolean quickSearch;
    public int headerHeight;

    public GSize getHeaderHeight() {
        if(headerHeight >= 0)
            return GSize.getValueSize(headerHeight);
        return null;
    }

    public boolean autoSize;

    public GContainer record;

    public int lineWidth;
    public int lineHeight;

    public Boolean boxed;

    @Override
    protected GSize getDefaultWidth() {
        return getDefaultSize().first;
    }

    @Override
    protected GSize getDefaultHeight() {
        return getDefaultSize().second;
    }

    private Pair<GSize, GSize> getDefaultSize() {
        return groupObject.getSize(lineHeight, lineWidth,  false, getHeaderHeight());
    }

    public boolean isBoxed(GTableView table) {
        if(boxed != null)
            return boxed;

        return table.isDefaultBoxed();
    }
}
