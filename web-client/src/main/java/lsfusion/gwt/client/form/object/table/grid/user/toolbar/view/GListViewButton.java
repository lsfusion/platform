package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;

public abstract class GListViewButton extends GToolbarButton {

    public GListViewButton(String imagePath, String tooltipText) {
        super(imagePath, tooltipText);
    }

    public abstract GListViewType getListViewType();
}
