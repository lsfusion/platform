package lsfusion.gwt.sgwtbase.client.ui;

import com.smartgwt.client.widgets.layout.HLayout;

public class HLayout100 extends HLayout {

    public HLayout100() {
        initialize();
    }

    public HLayout100(int memeberMargin) {
        super(memeberMargin);
        initialize();
    }

    private void initialize() {
        setWidth100();
        setHeight100();
    }
}
