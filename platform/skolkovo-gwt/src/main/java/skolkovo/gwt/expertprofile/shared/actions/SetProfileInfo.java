package skolkovo.gwt.expertprofile.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.api.gwt.shared.ProfileInfo;

public class SetProfileInfo implements Action<VoidResult> {
    public ProfileInfo profileInfo;

    public SetProfileInfo() {}

    public SetProfileInfo(ProfileInfo profileInfo) {
        this.profileInfo = profileInfo;
    }
}
