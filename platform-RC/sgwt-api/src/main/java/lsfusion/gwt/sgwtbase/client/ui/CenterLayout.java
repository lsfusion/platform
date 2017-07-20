package lsfusion.gwt.sgwtbase.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;

public class CenterLayout extends VLayout {
    private Canvas currentChild;

    public CenterLayout() {
        this(null);
    }

    public CenterLayout(Canvas currentChild) {
        setWidth100();
        setHeight100();
        setCenterComponent(currentChild);
    }

    @Override
    public void addMember(Canvas component) {
        if (currentChild != null) {
            removeChild(currentChild);
        }
        if (component != null) {
            HLayout hLayout = new HLayout();
            hLayout.addMember(createSpacer());
            hLayout.addMember(component);
            hLayout.addMember(createSpacer());

            super.addMember(createSpacer());
            super.addMember(hLayout);
            super.addMember(createSpacer());

            this.currentChild = component;
        }
    }

//  private static String[] colors = new String[]{
//          "FF6600", "808000", "008000", "008080", "0000FF", "666699",
//          "FF0000", "FF9900", "99CC00", "339966", "33CCCC", "3366FF",
//          "800080", "969696", "FF00FF", "FFCC00", "FFFF00", "00FF00",
//          "00FFFF", "00CCFF", "993366", "C0C0C0", "FF99CC", "FFCC99",
//          "FFFF99", "CCFFCC", "CCFFFF", "99CCFF", "CC99FF", "FFFFFF"
//  };
    private Canvas createSpacer() {
//        Canvas layoutSpacer = new Canvas();
//        layoutSpacer.setWidth100();
//        layoutSpacer.setHeight100();
//        layoutSpacer.setBorder("2px solid #" + colors[Random.nextInt(colors.length - 1)]);
//        return layoutSpacer;
        LayoutSpacer layoutSpacer = new LayoutSpacer();
        layoutSpacer.setWidth100();
        layoutSpacer.setHeight100();
        return layoutSpacer;
    }

    public void setCenterComponent(Canvas currentChild) {
        addMember(currentChild);
    }
}
