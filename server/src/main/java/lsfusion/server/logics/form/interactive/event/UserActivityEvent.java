package lsfusion.server.logics.form.interactive.event;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.struct.FormEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public abstract class UserActivityEvent<T> {
    public String groupObject;
    public LP<?> toProperty;

    public UserActivityEvent(String groupObject) {
        this(groupObject, null);
    }

    public UserActivityEvent(String groupObject, LP<?> toProperty) {
        this.groupObject = groupObject;
        this.toProperty = toProperty;
    }
    
    public abstract List<JSONObject> createJSON(List<T> items);
    public abstract LP<?> getDefaultToProperty(BusinessLogics BL);
    
    public void store(BusinessLogics BL, FormEntity formEntity, List<T> items, DataSession session) throws SQLException, SQLHandledException {
        List<JSONObject> objects = createJSON(items);
        if (!objects.isEmpty()) {
            LP<?> filterEventProperty = null;
            for (Object eventObject : formEntity.getEventActions().keyIt()) {
                if (eventObject.equals(this)) {
                    filterEventProperty = ((UserActivityEvent<?>) eventObject).toProperty;
                    break;
                }
            }
            if (filterEventProperty == null) {
                filterEventProperty = getDefaultToProperty(BL);
            }
            filterEventProperty.change(objects.size() > 1 ? new JSONArray(objects) : objects.get(0), session);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == this.getClass()) {
            return BaseUtils.nullEquals(groupObject, ((UserActivityEvent<?>) obj).groupObject);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupObject);
    }
}
