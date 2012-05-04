package platform.gwt.sgwtbase.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.tab.Tab;

public class TitledTab extends Tab {
    public TitledTab(String title, Canvas content) {
        super(title);
        setPane(content);
    }
}
