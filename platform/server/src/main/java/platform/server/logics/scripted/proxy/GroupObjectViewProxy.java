package platform.server.logics.scripted.proxy;

import platform.server.form.view.GroupObjectView;

import java.awt.*;

public class GroupObjectViewProxy extends ViewProxy<GroupObjectView> {
    public GroupObjectViewProxy(GroupObjectView target) {
        super(target);
    }

    public void setHighlightColor(Color highlightColor) {
        target.highlightColor = highlightColor;
    }

    public void setNeedVerticalScroll(Boolean needVerticalScroll) {
        target.needVerticalScroll = needVerticalScroll;
    }

    public void setTableRowsCount(Integer tableRowsCount) {
        target.setTableRowsCount(tableRowsCount);
    }
}
