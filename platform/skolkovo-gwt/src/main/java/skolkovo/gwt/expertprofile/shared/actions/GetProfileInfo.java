package skolkovo.gwt.expertprofile.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class GetProfileInfo implements Action<GetProfileInfoResult> {
    public String locale;

    public GetProfileInfo() {
    }

    public GetProfileInfo(String locale) {
        this.locale = locale;
    }
}
