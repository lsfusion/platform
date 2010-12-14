package platform.server.form.instance;

import platform.server.auth.SecurityPolicy;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;
import platform.server.data.type.ObjectType;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DialogInstance<T extends BusinessLogics<T>> extends FormInstance<T> {

    ObjectInstance dialogObject;
    public PropertyDrawEntity initFilterPropertyDraw;

    private DialogInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> tFocusView, CustomClassListener classListener, ObjectEntity dialogEntity, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ObjectValue> mapObjects) throws SQLException {
        super(entity, BL, session, securityPolicy, tFocusView, classListener, computer, mapObjects);
        
        dialogObject = instanceFactory.getInstance(dialogEntity);
    }

    public DialogInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, ObjectEntity dialogEntity, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, dialogEntity, computer, new HashMap<ObjectEntity, ObjectValue>());
    }

    public DialogInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, ObjectEntity dialogEntity, Object dialogValue, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, dialogEntity, computer, Collections.singletonMap(dialogEntity, session.getObjectValue(dialogValue, ObjectType.instance)));
    }

    public Object getDialogValue() {
        return dialogObject.getObjectValue().getValue();
    }
}
