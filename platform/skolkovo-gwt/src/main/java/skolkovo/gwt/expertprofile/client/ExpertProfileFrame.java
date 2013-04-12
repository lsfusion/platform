package skolkovo.gwt.expertprofile.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.util.SC;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.sgwtbase.client.SGWTErrorHandlingCallback;
import skolkovo.gwt.expertprofile.client.ui.ExpertProfileMainPanel;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfo;
import skolkovo.gwt.expertprofile.shared.actions.GetProfileInfoResult;
import skolkovo.gwt.expertprofile.shared.actions.SetProfileInfo;

public class ExpertProfileFrame implements EntryPoint {
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
        expertProfileService.execute(new GetProfileInfo(), new SGWTErrorHandlingCallback<GetProfileInfoResult>() {
            public void success(GetProfileInfoResult result) {
                if (expertProfileMainPanel != null) {
                    expertProfileMainPanel.clear();
                } else {
                    GwtClientUtils.removeLoaderFromHostedPage();
                }

                expertProfileMainPanel = new ExpertProfileMainPanel(result.profileInfo) {
                    @Override
                    public void updateButtonClicked() {
                        String error = validate();
                        if (error != null) {
                            SC.warn(error);
                            return;
                        }

                        showLoading();
                        expertProfileService.execute(new SetProfileInfo(populateProfileInfo()), new SGWTErrorHandlingCallback<VoidResult>() {
                            @Override
                            public void success(VoidResult result) {
                                update();
                            }

                            @Override
                            public void failure(Throwable caught) {
                                hideLoading();
                                super.failure(caught);
                            }
                        });
                    }
                };
                expertProfileMainPanel.draw();
            }
        });
    }
}
