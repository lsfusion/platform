package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;

public class GGrid extends GComponent {
    public GGroupObject groupObject;
    public boolean quickSearch;
    public int headerHeight;

    public GContainer record;

    public int lineWidth;
    public int lineHeight;

    @Override
    protected Integer getDefaultWidth() {
        return groupObject.getWidth(lineWidth);
    }

    @Override
    protected Integer getDefaultHeight() {
        return groupObject.getHeight(lineHeight) + (headerHeight >= 0 ? headerHeight : GGridPropertyTableHeader.DEFAULT_HEADER_HEIGHT);
    }
}
