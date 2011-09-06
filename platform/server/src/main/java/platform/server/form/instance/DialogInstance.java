package platform.server.form.instance;

import org.apache.log4j.Logger;
import platform.server.auth.SecurityPolicy;
import platform.server.data.type.ObjectType;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ServerResourceBundle;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Set;

import static java.util.Collections.singletonMap;

public class DialogInstance<T extends BusinessLogics<T>> extends FormInstance<T> {
    private static Logger logger = Logger.getLogger(DialogInstance.class);

    private ObjectInstance dialogObject;
    public PropertyDrawEntity initFilterPropertyDraw;
    public Boolean readOnly;
    public Boolean undecorated;

    public DialogInstance(FormEntity<T> entity,
                          T BL,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          Object dialogValue,
                          PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, tFocusView, classListener, dialogEntity, dialogValue, computer, null);
    }

    public DialogInstance(FormEntity<T> entity,
                          T BL,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          Object dialogValue,
                          PropertyObjectInterfaceInstance computer,
                          Set<FilterEntity> additionalFilters) throws SQLException {
        super(entity,
              BL,
              session,
              securityPolicy,
              tFocusView,
              classListener,
              computer,
              singletonMap(dialogEntity, session.getObjectValue(dialogValue, ObjectType.instance)),
              true,
              additionalFilters
        );
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
            logger.error(ServerResourceBundle.getString("form.instance.error.getting.property.value.for.display"), e);
            return null;
        }
    }
}
