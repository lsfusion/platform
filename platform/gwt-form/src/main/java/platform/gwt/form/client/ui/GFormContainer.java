package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.*;
import platform.gwt.view.GComponent;
import platform.gwt.view.GContainer;
import platform.gwt.view.GGrid;

public class GFormContainer extends GAbstractFormContainer {

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

        if (!hasGrid(key))
            containerComponent.setAutoHeight();

        addBorder();
    }

    private boolean hasGrid(GContainer container) {
        for (GComponent child : container.children) {
            if (child instanceof GGrid) {
                return true;
            } else if (child instanceof GContainer) {
                boolean has = hasGrid((GContainer) child);
                if (has)
                    return true;
            }
        }
        return false;
    }

    public void add(GComponent memberKey, Canvas member, int position) {
        children.put(memberKey, member);
        if (position != -1)
            containerComponent.addMember(member, position);
        else
            containerComponent.addMember(member);
    }

    public void remove(GComponent memberKey) {
        if (children.containsKey(memberKey)) {
            containerComponent.removeMember(children.get(memberKey));
        }
        super.remove(memberKey);
    }

    public boolean drawsChild(GComponent child) {
        if (children.get(child) != null) {
            return containerComponent.hasMember(children.get(child)) /*&& children.get(child).isVisible()*/;
        }
        return false;
    }
}
