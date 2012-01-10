package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.*;
import platform.gwt.view.GComponent;
import platform.gwt.view.GContainer;

import java.util.HashMap;
import java.util.Map;

public class GFormContainer {
    private Layout containerComponent;
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

        containerComponent.setHeight(1);
        addBorder();
    }

    public Layout getComponent() {
        return containerComponent;
    }

    public void addBorder() {
        String title = key.title;
        if (title != null) {
            containerComponent.setGroupTitle(key.title);
            containerComponent.setIsGroup(true);
            containerComponent.setLayoutTopMargin(3);
        }
    }

    public void add(GComponent memberKey, Canvas member) {
        children.put(memberKey, member);
        containerComponent.addMember(member);
    }

    public void remove(GComponent memberKey) {
        if (children.containsKey(memberKey)) {
            containerComponent.removeMember(children.get(memberKey));
            children.remove(memberKey);
        }
    }

    public GContainer getKey() {
        return key;
    }

    public boolean drawsChild(GComponent child) {
        return children.get(child) != null && containerComponent.hasMember(children.get(child)) /*&& children.get(child).isVisible()*/;
    }
}
