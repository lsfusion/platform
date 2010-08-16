package platform.server.form.instance;

import platform.server.auth.SecurityPolicy;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.logics.BusinessLogics;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DialogInstance<T extends BusinessLogics<T>> extends FormInstance<T> {

    ObjectInstance dialogObject;

    private DialogInstance(FormEntity<T> formEntity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> tFocusView, CustomClassListener classListener, ObjectEntity dialogEntity, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, Object> mapObjects) throws SQLException {
        super(formEntity, BL, session, securityPolicy, tFocusView, classListener, computer, mapObjects);
        
        dialogObject = mapper.mapObject(dialogEntity);
    }

    public DialogInstance(FormEntity<T> formEntity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, ObjectEntity dialogEntity, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(formEntity, BL, session, securityPolicy, focusListener, classListener, dialogEntity, computer, new HashMap<ObjectEntity, Object>());
    }

    public DialogInstance(FormEntity<T> formEntity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, ObjectEntity dialogEntity, PropertyObjectInterfaceInstance computer, Object dialogValue) throws SQLException {
        this(formEntity, BL, session, securityPolicy, focusListener, classListener, dialogEntity, computer, Collections.singletonMap(dialogEntity, dialogValue));
    }

    public Object getDialogValue() {
        return dialogObject.getObjectValue().getValue();
    }
}
