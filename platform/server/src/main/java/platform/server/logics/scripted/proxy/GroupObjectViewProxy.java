package platform.server.logics.scripted.proxy;

import platform.server.form.view.GroupObjectView;

public class GroupObjectViewProxy extends ViewProxy<GroupObjectView> {
    public GroupObjectViewProxy(GroupObjectView target) {
        super(target);
    }

    public void setNeedVerticalScroll(Boolean needVerticalScroll) {
        target.needVerticalScroll = needVerticalScroll;
    }

    public void setTableRowsCount(Integer tableRowsCount) {
        target.setTableRowsCount(tableRowsCount);
    }
}
