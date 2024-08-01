package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedWidget;

public class CaptionWidget {

    public SizedWidget widget;

    public GFlexAlignment horzAlignment;
    public GFlexAlignment vertAlignment;

    public GFlexAlignment valueAlignmentVert;

    public CaptionWidget(Widget widget) {
        this(new SizedWidget(widget), GFlexAlignment.START, GFlexAlignment.CENTER, GFlexAlignment.STRETCH);

    }
    public CaptionWidget(SizedWidget widget, GFlexAlignment horzAlignment, GFlexAlignment vertAlignment, GFlexAlignment valueAlignmentVert) {
        this.widget = widget;
        this.horzAlignment = horzAlignment;
        this.vertAlignment = vertAlignment;
        this.valueAlignmentVert = valueAlignmentVert;
    }
}
