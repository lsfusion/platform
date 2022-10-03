package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.view.GTableView;

public class GGrid extends GComponent {
    public GGroupObject groupObject;
    public boolean quickSearch;
    public int headerHeight;

    public Boolean resizeOverflow;

    public GSize getHeaderHeight() {
        if(headerHeight >= 0)
            return GSize.getValueSize(headerHeight).add(GSize.TEMP_PADDING_ADJ);
        return null;
    }

    public boolean autoSize;

    public GContainer record;

    public int lineWidth;
    public int lineHeight;

    public Boolean boxed;

    @Override
    protected GSize getDefaultWidth() {
        return groupObject.getWidth(lineWidth);
    }

    @Override
    protected GSize getDefaultHeight() {
        return groupObject.getHeight(lineHeight, getHeaderHeight());
    }

    public boolean isBoxed(GTableView table) {
        if(boxed != null)
            return boxed;

        return table.isDefaultBoxed();
    }
}
