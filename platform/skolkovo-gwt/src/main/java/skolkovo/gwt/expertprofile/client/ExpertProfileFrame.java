package skolkovo.gwt.expertprofile.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import skolkovo.gwt.base.client.BaseFrame;
import skolkovo.gwt.base.shared.GwtProfileInfo;
import skolkovo.gwt.base.shared.GwtVoteInfo;
import skolkovo.gwt.expertprofile.client.ui.ExpertProfileMainWidget;

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

                RootPanel.get().clear();
                RootPanel.get().add(new ExpertProfileMainWidget(pi) {
                    @Override
                    public void onSend(GwtVoteInfo vi) {
                        expertProfileService.sentVoteDocuments(vi.voteId, new UpdateAsyncCallback());
                    }
                });
            }
        });
    }
}
