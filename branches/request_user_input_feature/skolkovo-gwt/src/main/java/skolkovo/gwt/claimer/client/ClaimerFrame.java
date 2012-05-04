package skolkovo.gwt.claimer.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Window;
import platform.gwt.base.client.GwtClientUtils;
import skolkovo.gwt.claimer.client.ui.ClaimerMainPanel;

public class ClaimerFrame implements EntryPoint {
    private static ClaimerMessages messages = ClaimerMessages.Instance.get();
//    private final static StandardDispatchAsync claimerService = new StandardDispatchAsync(new DefaultExceptionHandler());

    interface GlobalResources extends ClientBundle {
        @CssResource.NotStrict
        @Source("claimer.css")
        CssResource css();
    }

    public void onModuleLoad() {
        GWT.<GlobalResources>create(GlobalResources.class).css().ensureInjected();

        Window.setTitle(messages.title());
        update();
    }

    protected void update() {
        new ClaimerMainPanel().draw();
        GwtClientUtils.removeLoaderFromHostedPage();

//        claimerService.execute(new GetProfileInfo(), new BaseFrame.ErrorAsyncCallback<GetProfileInfoResult>() {
//            public void onSuccess(GetProfileInfoResult result) {
//                ProfileInfo pi = result.profileInfo;
//                if (pi == null) {
//                    showErrorPage(null);
//                    return;
//                }
//
//                new ExpertProfileMainPanel(pi).draw();
//            }
//        });
    }
}
