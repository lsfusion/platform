package platform.gwt.form.client.ui;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.*;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import platform.gwt.view.GComponent;
import platform.gwt.view.GContainer;

import java.util.HashMap;
import java.util.Map;

public class GFormContainer {
    private Layout containerComponent;
    private TabSet tabPane;
    private GContainer key;
    private Map<GComponent, Canvas> children = new HashMap<GComponent, Canvas>();

    public GFormContainer(GContainer key) {
        this.key = key;

        if (key.gwtIsLayout)
            if (key.gwtVertical)
                containerComponent = new VLayout();
            else
                containerComponent = new HLayout(10);
        else
            if (key.gwtVertical)
                containerComponent = new VStack();
            else
                containerComponent = new HStack(10);

        if (isTabbed()) {
            tabPane = new TabSet();
            tabPane.setAutoHeight();
            tabPane.setOverflow(Overflow.VISIBLE);
            tabPane.setPaneContainerOverflow(Overflow.VISIBLE);
            containerComponent.addMember(tabPane);
        }
        containerComponent.setHeight(1);
        addBorder();
    }

    public boolean isTabbed() {
        return key.tabbedPane;
    }

    public Layout getComponent() {
        return containerComponent;
    }

    public void addBorder() {
        String title = key.title;
        if (title != null && key.container != null && !key.container.tabbedPane) {
            containerComponent.setGroupTitle(key.title);
            containerComponent.setIsGroup(true);
            containerComponent.setLayoutTopMargin(3);
        }
    }

    public void add(GComponent memberKey, Canvas member, int position) {
        children.put(memberKey, member);
        if (isTabbed()) {
            Tab tab = new Tab();
            tab.setPane(member);
            if (memberKey instanceof GContainer) {
                String title = ((GContainer) memberKey).title;
                if (title != null)
                    tab.setTitle(title);
            }
            tab.setID("tabid" + String.valueOf(memberKey.ID));
            if (position != -1)
                tabPane.addTab(tab, position);
            else
                tabPane.addTab(tab);
        } else {
            if (position != -1)
                containerComponent.addMember(member, position);
            else
                containerComponent.addMember(member);
        }
    }

    public void add(GComponent memberKey, Canvas member) {
        add(memberKey, member, -1);
    }

    public void remove(GComponent memberKey) {
        if (children.containsKey(memberKey)) {
            if (isTabbed()) {
                tabPane.removeTab("tabid" + String.valueOf(memberKey.ID));
            } else {
                containerComponent.removeMember(children.get(memberKey));
            }
            children.remove(memberKey);
        }
    }

    public GContainer getKey() {
        return key;
    }

    public boolean needToBeHidden() {
        return containerComponent.getMembers().length == 0 /*children.isEmpty()*/ || (key.container != null && key.container.tabbedPane && !containerComponent.isVisible());
    }

    public boolean drawsChild(GComponent child) {
        if (children.get(child) != null) {
            if (isTabbed()) {
                return tabPane.contains(children.get(child)) && children.get(child).isVisible();
            } else {
                return containerComponent.hasMember(children.get(child)) /*&& children.get(child).isVisible()*/;
            }
        }
        return false;
    }
}
