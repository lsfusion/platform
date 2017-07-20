package lsfusion.server.profiler;

import lsfusion.server.form.entity.FormEntity;

import static lsfusion.base.BaseUtils.nullEquals;
import static lsfusion.base.BaseUtils.nullHash;

public class ProfileItem {
    public ProfileObject profileObject;
    public ProfileObject upperProfileObject;
    public Integer userID;
    public FormEntity form;
    
    public ProfileItem(ProfileObject profileObject, ProfileObject upperProfileObject, Integer userID, FormEntity form) {
        this.profileObject = profileObject;
        this.upperProfileObject = upperProfileObject;
        this.userID = userID;
        this.form = form;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * nullHash(profileObject) + nullHash(upperProfileObject)) + nullHash(userID)) + nullHash(form);
    }

    @Override
    public boolean equals(Object obj) {
        return !(obj == null || !(obj instanceof ProfileItem)) 
                && nullEquals(profileObject, ((ProfileItem) obj).profileObject) 
                && nullEquals(upperProfileObject, ((ProfileItem) obj).upperProfileObject) 
                && nullEquals(userID, ((ProfileItem) obj).userID) 
                && nullEquals(form, ((ProfileItem) obj).form);
    }
}
