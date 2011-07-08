package skolkovo.gwt.expertprofile.client;

import com.google.gwt.user.client.Window;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.BaseFrame;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.gwt.expertprofile.client.ui.ExpertProfileMainPanel;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfo;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfoResult;

public class ExpertProfileFrame extends BaseFrame {
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();
    private final static StandardDispatchAsync expertProfileService = new StandardDispatchAsync(new DefaultExceptionHandler());

    public void onModuleLoad() {
        Window.setTitle(messages.title());
        update();
    }

    protected void update() {
        expertProfileService.execute(new GetProfileInfo(), new ErrorAsyncCallback<GetProfileInfoResult>() {
            public void onSuccess(GetProfileInfoResult result) {
                ProfileInfo pi = result.profileInfo;
                if (pi == null) {
                    showErrorPage(null);
                    return;
                }

                new ExpertProfileMainPanel(pi).draw();
            }
        });
    }
}
