package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.view.GTableView;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;

public class GGrid extends GComponent {
    public GGroupObject groupObject;
    public boolean quickSearch;
    public int headerHeight;

    public boolean autoSize;

    public GContainer record;

    public int lineWidth;
    public int lineHeight;

    public Boolean boxed;

    @Override
    protected Integer getDefaultWidth() {
        return groupObject.getWidth(lineWidth);
    }

    @Override
    protected Integer getDefaultHeight() {
        return groupObject.getHeight(lineHeight, headerHeight);
    }

    public boolean isBoxed(GTableView table) {
        if(boxed != null)
            return boxed;

        return table.isDefaultBoxed();
    }
}
