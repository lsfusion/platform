package skolkovo.gwt.expertprofile.client;

import com.google.gwt.user.client.Window;
import skolkovo.gwt.base.client.BaseFrame;
import skolkovo.gwt.base.shared.GwtProfileInfo;
import skolkovo.gwt.expertprofile.client.ui.ExpertProfileMainPanel;

public class ExpertProfileFrame extends BaseFrame {
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();
    private static ExpertProfileServiceAsync expertProfileService = ExpertProfileService.App.getInstance();

    public void onModuleLoad() {
        Window.setTitle(messages.title());
        update();
    }

    protected void update() {
        expertProfileService.getProfileInfo(new ErrorAsyncCallback<GwtProfileInfo>() {
            public void onSuccess(GwtProfileInfo pi) {
                if (pi == null) {
                    showErrorPage(null);
                    return;
                }

                new ExpertProfileMainPanel(pi).draw();
            }
        });
    }
}
