package lsfusion.server.physics.admin.profiler;

import lsfusion.base.col.MapFact;
import lsfusion.server.logics.form.struct.FormEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Profiler {
    public static boolean PROFILER_ENABLED = false;
    
    public static ConcurrentHashMap<ProfileItem, ProfileValue> profileData = MapFact.getGlobalConcurrentHashMap();
    
    public static Set<Long> profileUsers = new HashSet<>();
    public static Set<String> profileForms = new HashSet<>();
    
    public static void increase(ProfileObject profileObject, ProfileObject upperProfileObject, Long userID, FormEntity form, long time, long sqlTime, long userInteractionTime) {
        ProfileValue profileValue = getProfileValue(profileObject, upperProfileObject, userID, form);
        profileValue.increase(time, sqlTime, userInteractionTime);
    }
    
    public static ProfileValue getProfileValue(ProfileObject profileObject, ProfileObject upperProfileObject, Long userID, FormEntity form) {
        return getProfileValue(new ProfileItem(profileObject, upperProfileObject, userID, form));    
    }

    public static ProfileValue getProfileValue(ProfileItem profileItem) {
        ProfileValue value = profileData.get(profileItem);
        if (value == null) {
            value = new ProfileValue();
            profileData.put(profileItem, value);
        }
        return value;
    }
    
    public static boolean checkUserForm(Long user, FormEntity form) {
        return checkUser(user) && checkForm(form);
    }
    
    private static boolean checkUser(Long user) {
        return profileUsers.isEmpty() || profileUsers.contains(user); 
    }
    
    private static boolean checkForm(FormEntity form) {
        return profileForms.isEmpty() || profileForms.contains(form == null ? "NULL" : form.getCanonicalName());
    }
    
    public static void clearData() {
        profileData.clear();
    }
}
