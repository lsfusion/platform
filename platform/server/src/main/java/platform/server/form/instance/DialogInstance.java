package platform.server.form.instance;

import org.apache.log4j.Logger;
import platform.server.auth.SecurityPolicy;
import platform.server.data.type.ObjectType;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Collections;

public class DialogInstance<T extends BusinessLogics<T>> extends FormInstance<T> {
    private static Logger logger = Logger.getLogger(DialogInstance.class);

    private ObjectInstance dialogObject;
    public PropertyDrawEntity initFilterPropertyDraw;
    public Boolean readOnly;

    public DialogInstance(FormEntity<T> entity,
                          T BL,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          Object dialogValue,
                          PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, tFocusView, classListener, dialogEntity, session.getObjectValue(dialogValue, ObjectType.instance), computer);
    }

    public DialogInstance(FormEntity<T> entity,
                          T BL,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          ObjectValue dialogValue,
                          PropertyObjectInterfaceInstance computer) throws SQLException {
        super(entity, BL, session, securityPolicy, tFocusView, classListener, computer, Collections.singletonMap(dialogEntity, dialogValue), true);
        // все равно нашли объекты или нет

        dialogObject = instanceFactory.getInstance(dialogEntity);
    }

    public Object getDialogValue() {
        return dialogObject.getObjectValue().getValue();
    }

    public Object getCellDisplayValue() {
        try {
            if (initFilterPropertyDraw != null) {
                PropertyObjectInstance filterInstance = instanceFactory.getInstance(initFilterPropertyDraw.propertyObject);
                if (filterInstance != null) {
                    return read(filterInstance);
                }
            }
            return getDialogValue();
        } catch (SQLException e) {
            logger.error("Ошибка при получения значения свойства для отображения.", e);
            return null;
        }
    }
}
