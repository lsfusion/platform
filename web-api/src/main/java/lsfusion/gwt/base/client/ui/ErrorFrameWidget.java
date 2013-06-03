package lsfusion.gwt.base.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.BaseMessages;
import lsfusion.gwt.base.shared.MessageException;

public class ErrorFrameWidget extends Composite {
    public static BaseMessages baseMessages = BaseMessages.Instance.get();

    interface ErrorFrameWidgetUiBinder extends UiBinder<Widget, ErrorFrameWidget> {}
    private static ErrorFrameWidgetUiBinder uiBinder = GWT.create(ErrorFrameWidgetUiBinder.class);
    @UiField
    SpanElement messageSpan;

    public ErrorFrameWidget(Throwable caught) {
        initWidget(uiBinder.createAndBindUi(this));

        String message = caught instanceof MessageException
                         ? caught.getMessage()
                         : baseMessages.internalServerErrorMessage();

        messageSpan.setInnerHTML(message);
    }
}