package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class TreeGroupViewProxy extends GridPropertyViewProxy<TreeGroupView> {
    public TreeGroupViewProxy(TreeGroupView target) {
        super(target);
    }

    @SuppressWarnings("unused")
    public void setAutoSize(boolean autoSize) {
        if(target.width == null || target.width < 0)
            target.width = autoSize ? -1 : -2;
        if(target.height == null || target.height < 0)
            target.height = autoSize ? -1 : -2;
    }

    @SuppressWarnings("unused")
    public void setBoxed(boolean boxed) {
        target.boxed = boxed;
    }

    @SuppressWarnings("unused")
    public void setExpandOnClick(boolean expandOnClick) {
        target.expandOnClick = expandOnClick;
    }

    @SuppressWarnings("unused")
    public void setHierarchicalWidth(int hierarchicalWidth) {
        target.hierarchicalWidth = hierarchicalWidth;
    }

    @SuppressWarnings("unused")
    public void setHierarchicalCaption(Object caption) {
        if(caption instanceof LocalizedString)
            target.hierarchicalCaption = ((LocalizedString) caption).getSourceString();
        else
            target.propertyHierarchicalCaption = (PropertyObjectEntity<?>) caption;
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void setHeaderHeight(int headerHeight) {
        target.captionHeight = headerHeight;
    }

    @SuppressWarnings("unused")
    public void setCaptionHeight(int height) {
        target.captionHeight = height;
    }

    @SuppressWarnings("unused")
    public void setResizeOverflow(boolean resizeOverflow) {
        target.resizeOverflow = resizeOverflow;
    }

    @SuppressWarnings("unused")
    public void setLineHeight(int lines) {
        target.lineHeight = lines;
    }

    @SuppressWarnings("unused")
    public void setLineWidth(int lines) {
        target.lineWidth = lines;
    }
}
