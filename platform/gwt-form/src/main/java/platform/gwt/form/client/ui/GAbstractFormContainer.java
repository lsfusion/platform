package platform.gwt.form.client.ui;

import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.*;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import platform.gwt.base.shared.GContainerType;
import platform.gwt.view.GComponent;
import platform.gwt.view.GContainer;

import java.util.HashMap;
import java.util.Map;

public abstract class GAbstractFormContainer {
    protected Layout containerComponent;
    protected GContainer key;
    protected Map<GComponent, Canvas> children = new HashMap<GComponent, Canvas>();


    public boolean isTabbed() {
        return key.type == GContainerType.TABBED_PANE;
    }

    public boolean isSplit() {
        return key.type == GContainerType.SPLIT_PANE_HORIZONTAL || key.type == GContainerType.SPLIT_PANE_VERTICAL;
    }

    public Layout getComponent() {
        return containerComponent;
    }

    public void addBorder() {
        String title = key.title;
        if (title != null && key.container != null && !GContainerType.isTabbedPane(key.container.type)) {
            containerComponent.setGroupTitle(key.title);
            containerComponent.setIsGroup(true);
            containerComponent.setLayoutTopMargin(3);
        }
    }

    public abstract void add(GComponent memberKey, Canvas member, int position);

    public void add(GComponent memberKey, Canvas member) {
        add(memberKey, member, -1);
    }

    public void remove(GComponent memberKey) {
        children.remove(memberKey);
    }

    public GContainer getKey() {
        return key;
    }

    public boolean needToBeHidden() {
        return containerComponent.getMembers().length == 0 /*children.isEmpty()*/ || (key.container != null &&
                !containerComponent.isVisible() && (GContainerType.isTabbedPane(key.container.type) || GContainerType.isSplitPane(key.container.type)));
    }

    public abstract boolean drawsChild(GComponent child);
}
