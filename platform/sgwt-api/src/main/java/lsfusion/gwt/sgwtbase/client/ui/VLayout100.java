package lsfusion.gwt.sgwtbase.client.ui;

import com.smartgwt.client.widgets.layout.VLayout;

public class VLayout100 extends VLayout {

    public VLayout100() {
        initialize();
    }

    public VLayout100(int memeberMargin) {
        super(memeberMargin);
        initialize();
    }

    private void initialize() {
        setWidth100();
        setHeight100();
    }
}
