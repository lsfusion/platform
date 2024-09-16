package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;

public class TreeGroupViewProxy extends GridPropertyViewProxy<TreeGroupView> {
    public TreeGroupViewProxy(TreeGroupView target) {
        super(target);
    }

    public void setAutoSize(boolean autoSize) {
        if(target.width == null || target.width < 0)
            target.width = autoSize ? -1 : -2;
        if(target.height == null || target.height < 0)
            target.height = autoSize ? -1 : -2;
    }

    public void setBoxed(boolean boxed) {
        target.boxed = boxed;
    }

    public void setExpandOnClick(boolean expandOnClick) {
        target.expandOnClick = expandOnClick;
    }

    public void setHierarchicalWidth(int hierarchicalWidth) {
        target.hierarchicalWidth = hierarchicalWidth;
    }

    @Deprecated
    public void setHeaderHeight(int headerHeight) {
        target.captionHeight = headerHeight;
    }

    public void setCaptionHeight(int height) {
        target.captionHeight = height;
    }

    public void setResizeOverflow(boolean resizeOverflow) {
        target.resizeOverflow = resizeOverflow;
    }

    public void setLineHeight(int lines) {
        target.lineHeight = lines;
    }

    public void setLineWidth(int lines) {
        target.lineWidth = lines;
    }
}
