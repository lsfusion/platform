package skolkovo.gwt.expert.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface ExpertMainWidgetCSSBundle extends ClientBundle {
    public final static ExpertMainWidgetCSSBundle INSTANCE = GWT.create(ExpertMainWidgetCSSBundle.class);

    public interface ExpertMainWidgetCss extends CssResource {
        String dialog();
        String commentBox();
        String warningCommentBox();
    }

    @CssResource.NotStrict
    @Source("/ExpertMainWidget.ui.css")
    ExpertMainWidgetCss css();
}