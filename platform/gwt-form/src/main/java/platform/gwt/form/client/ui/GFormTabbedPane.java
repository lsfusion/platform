package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;
import platform.gwt.view.GComponent;
import platform.gwt.view.GContainer;

public class GFormTabbedPane extends GAbstractFormContainer {
    private TabSet tabPane;

    public GFormTabbedPane(final GFormController formController, final GContainer key) {
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

        tabPane = new TabSet();
        containerComponent.addMember(tabPane);

        tabPane.addTabSelectedHandler(new TabSelectedHandler() {
            @Override
            public void onTabSelected(TabSelectedEvent tabSelectedEvent) {
                int index = tabSelectedEvent.getTabNum();
                formController.setTabVisible(key, (GComponent) children.keySet().toArray()[index]);
            }
        });

        addBorder();
    }

    @Override
    public void add(GComponent memberKey, Canvas member, int position) {
        children.put(memberKey, member);
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
    }

    @Override
    public void remove(GComponent memberKey) {
        if (children.containsKey(memberKey)) {
            tabPane.removeTab("tabid" + String.valueOf(memberKey.ID));
        }
        children.remove(memberKey);
    }

    @Override
    public boolean drawsChild(GComponent child) {
        return children.get(child) != null && tabPane.contains(children.get(child));
    }
}
