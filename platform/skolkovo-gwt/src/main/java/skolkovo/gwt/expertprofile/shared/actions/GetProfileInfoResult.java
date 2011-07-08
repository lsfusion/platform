package skolkovo.gwt.expertprofile.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import skolkovo.api.gwt.shared.ProfileInfo;

public class GetProfileInfoResult implements Result {
    public ProfileInfo profileInfo;

    public GetProfileInfoResult() {}

    public GetProfileInfoResult(ProfileInfo profileInfo) {
        this.profileInfo = profileInfo;
    }
}
