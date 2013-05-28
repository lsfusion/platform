package platform.gwt.paas.client.pages.error;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

public class ErrorPageView extends ViewImpl implements ErrorPagePresenter.MyView {

    private static String html = "<div>\n"
                                 + "<table align=\"center\">\n"
                                 + "  <tr>\n" + "<td>&nbsp;</td>\n" + "</tr>\n"
                                 + "  <tr>\n" + "<td>&nbsp;</td>\n" + "</tr>\n"
                                 + "  <tr>\n" + "<td>&nbsp;</td>\n" + "</tr>\n"
                                 + "  <tr>\n"
                                 + "    <td style=\"font-weight:bold;\">An error has occurred.</td>\n"
                                 + "  </tr>\n"
                                 + "  <tr>\n" + "<td>&nbsp;</td>\n" + "</tr>\n"
                                 + "  <tr>\n" + "<td>Try this action again. If the problem continues,</td>\n" + "</tr>\n"
                                 + "  <tr>\n" + "<td>contact your administrator.</td>\n" + "</tr>\n"
                                 + "  <tr>\n" + "<td>&nbsp;</td>\n" + "</tr>\n"
                                 + "  <tr>\n"
                                 + "    <td id=\"okButtonContainer\"></td>\n"
                                 + "  </tr>\n"
                                 + "</table>\n"
                                 + "</div>\n";

    HTMLPanel panel = new HTMLPanel(html);

    private final Button okButton;

    @Inject
    public ErrorPageView() {
        okButton = new Button("OK");

        panel.add(okButton, "okButtonContainer");
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

    @Override
    public Button getOkButton() {
        return okButton;
    }
}
