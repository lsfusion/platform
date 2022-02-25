package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;

public class TreeGroupViewProxy extends ComponentViewProxy<TreeGroupView> {
    public TreeGroupViewProxy(TreeGroupView target) {
        super(target);
    }

    public void setAutoSize(boolean autoSize) {
        target.autoSize = autoSize;
    }

    public void setBoxed(boolean boxed) {
        target.boxed = boxed;
    }

    public void setExpandOnClick(boolean expandOnClick) {
        target.expandOnClick = expandOnClick;
    }

    public void setHeaderHeight(int headerHeight) {
        target.headerHeight = headerHeight;
    }

    public void setLineHeight(int lines) {
        target.lineHeight = lines;
    }

    public void setLineWidth(int lines) {
        target.lineWidth = lines;
    }
}
