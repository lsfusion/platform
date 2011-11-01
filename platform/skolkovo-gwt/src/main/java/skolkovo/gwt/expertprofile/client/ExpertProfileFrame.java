package skolkovo.gwt.expertprofile.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Window;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.BaseFrame;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.utils.GwtUtils;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.gwt.expertprofile.client.ui.ExpertProfileMainPanel;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfo;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfoResult;
import skolkovo.gwt.expertprofile.shared.actions.SetProfileInfo;

public class ExpertProfileFrame extends BaseFrame {
    interface StyleResources extends ClientBundle {
        @CssResource.NotStrict
        @Source("ExpertProfileFrame.css")
        CssResource css();
    }

    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();
    private final static StandardDispatchAsync expertProfileService = new StandardDispatchAsync(new DefaultExceptionHandler());
    private ExpertProfileMainPanel expertProfileMainPanel;

    public void onModuleLoad() {
        // inject global styles
        GWT.<StyleResources>create(StyleResources.class).css().ensureInjected();

        Window.setTitle(messages.title());
        update();
    }

    protected void update() {
        expertProfileService.execute(new GetProfileInfo(), new ErrorAsyncCallback<GetProfileInfoResult>() {
            public void success(GetProfileInfoResult result) {
                ProfileInfo pi = result.profileInfo;
                if (pi == null) {
                    showErrorPage(null);
                    return;
                }

                if (expertProfileMainPanel != null) {
                    expertProfileMainPanel.clear();
                }
                expertProfileMainPanel = new ExpertProfileMainPanel(pi) {
                    @Override
                    public void updateButtonClicked() {
                        expertProfileService.execute(new SetProfileInfo(populateProfileInfo()), new ErrorAsyncCallback<VoidResult>() {
                            @Override
                            public void success(VoidResult result) {
                                update();
                            }
                        });
                    }
                };
                expertProfileMainPanel.draw();

                GwtUtils.removeLoaderFromHostedPage();
            }
        });
    }
}
