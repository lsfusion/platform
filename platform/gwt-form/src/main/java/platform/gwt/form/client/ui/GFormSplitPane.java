package platform.gwt.form.client.ui;

import com.smartgwt.client.types.LayoutResizeBarPolicy;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.*;
import platform.gwt.view.GComponent;
import platform.gwt.view.GContainer;

public class GFormSplitPane extends GAbstractFormContainer {
    public GFormSplitPane(GContainer key) {
        this.key = key;

        if (key.gwtVertical) {
            containerComponent = new VLayout();
        } else {
            containerComponent = new HLayout();
        }

        containerComponent.setDefaultResizeBars(LayoutResizeBarPolicy.MIDDLE);
        addBorder();
    }

    public void add(GComponent memberKey, Canvas member, int position) {
        children.put(memberKey, member);
        containerComponent.addMember(member);
    }

    public void remove(GComponent memberKey) {
        if (children.containsKey(memberKey)) {
            containerComponent.removeMember(children.get(memberKey));
        }
        super.remove(memberKey);
    }


    public boolean drawsChild(GComponent child) {
        return children.get(child) != null && containerComponent.contains(children.get(child)) && children.get(child).isVisible();
    }
}
