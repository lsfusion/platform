package lsfusion.server.language.proxy;

import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.server.base.Custom;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.form.interactive.design.object.GridView;

public class GridViewProxy extends GridPropertyViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
    }

    // a grid's custom is its group's own, as a property's is the property's (PropertyDrawViewProxy): a name says what
    // CUSTOM 'name' says in OBJECTS - the group's rows shown in CUSTOM view by that render function, a classic one or a
    // React component, which the web client tells apart; `custom = NULL` says the grid is the platform's, whatever view
    // the form gives the group
    @Override
    public void setCustom(Object custom) {
        Version version = getVersion();
        if (isExplicitNull(custom))
            target.setCustom(Custom.NOCUSTOM, version);
        else if (custom instanceof LocalizedString && !Custom.isHtmlTemplate(custom.toString()) && !custom.toString().isEmpty()) {
            GroupObjectEntity group = target.groupObject.entity;
            group.setCustomRenderFunction(custom.toString(), version);
            group.setListViewType(ListViewType.CUSTOM, version);
        } else
            throw new UnsupportedOperationException("custom of a grid is NULL, the platform's own views of the group's"
                    + " rows, or the name of the group's CUSTOM render function, given as CUSTOM gives it in OBJECTS: a"
                    + " template, '' and a property lay out the children of a container");
    }

    @SuppressWarnings("unused")
    public void setAutoSize(boolean autoSize) {
        Version version = getVersion();
        Integer width = target.getNFWidth(version);
        Integer height = target.getNFHeight(version);
        if(width == null || width < 0)
            target.setWidth(autoSize ? -1 : -2, version);
        if(height == null || height < 0)
            target.setHeight(autoSize ? -1 : -2, version);
    }

    @SuppressWarnings("unused")
    public void setBoxed(boolean boxed) {
        target.setBoxed(boxed, getVersion());
    }

    @SuppressWarnings("unused")
    public void setTabVertical(boolean tabVertical) {
        target.setTabVertical(tabVertical, getVersion());
    }

    @SuppressWarnings("unused")
    public void setQuickSearch(boolean quickSearch) {
        target.setQuickSearch(quickSearch, getVersion());
    }

    @Deprecated //since 6.2, will be removed in 8.0
    @SuppressWarnings("unused")
    public void setHeaderHeight(int headerHeight) {
        target.setCaptionHeight(headerHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionHeight(int height) {
        target.setCaptionHeight(height, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionCharHeight(int height) {
        target.setCaptionCharHeight(height, getVersion());
    }

    @SuppressWarnings("unused")
    public void setResizeOverflow(boolean resizeOverflow) {
        target.setResizeOverflow(resizeOverflow, getVersion());
    }

    @SuppressWarnings("unused")
    public void setResizeShrink(boolean resizeShrink) {
        target.setResizeShrink(resizeShrink, getVersion());
    }

    @SuppressWarnings("unused")
    public void setLineWidth(int lineWidth) {
        target.setLineWidth(lineWidth, getVersion());
    }

    @SuppressWarnings("unused")
    public void setLineHeight(int lineHeight) {
        target.setLineHeight(lineHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setEnableManualUpdate(boolean enableManualUpdate) {
        target.groupObject.entity.setEnableManualUpdate(enableManualUpdate, getVersion());
    }
}
