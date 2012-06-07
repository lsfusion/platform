package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.*;
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

    public void add(GComponent memberKey, Canvas member, int position) {
        children.put(memberKey, member);
        if (position != -1)
            containerComponent.addMember(member, position);
        else
            containerComponent.addMember(member);
    }

    public void add(GComponent memberKey, Canvas member) {
        add(memberKey, member, -1);
    }

    public void remove(GComponent memberKey) {
        if (children.containsKey(memberKey)) {
            containerComponent.removeMember(children.get(memberKey));
        }
        children.remove(memberKey);
    }

    public GContainer getKey() {
        return key;
    }

    public boolean isInTabbedPane() {
        return key.container != null && GContainerType.isTabbedPane(key.container.type);
    }

    public boolean isInSplitPane() {
        return key.container != null && GContainerType.isSplitPane(key.container.type);
    }

    public boolean drawsChild(GComponent child) {
        return children.get(child) != null && containerComponent.hasMember(children.get(child));
    }
}
