package lsfusion.server.form.instance;

import org.apache.log4j.Logger;
import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.logics.*;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class DialogInstance<T extends BusinessLogics<T>> extends FormInstance<T> {
    private static Logger logger = Logger.getLogger(DialogInstance.class);

    private ObjectInstance dialogObject;
    public PropertyDrawEntity initFilterPropertyDraw;
    public Boolean undecorated;

    public DialogInstance(FormEntity<T> entity,
                          LogicsInstance logicsInstance,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          ObjectValue dialogValue,
                          PropertyObjectInterfaceInstance computer,
                          DataObject connection) throws SQLException {
        this(entity, logicsInstance, session, securityPolicy, tFocusView, classListener, dialogEntity, dialogValue, computer, connection, null, null);
    }

    public DialogInstance(FormEntity<T> entity,
                          LogicsInstance logicsInstance,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          ObjectValue dialogValue,
                          PropertyObjectInterfaceInstance computer,
                          DataObject connection,
                          ImSet<FilterEntity> additionalFilters,
                          ImSet<PullChangeProperty> pullProps) throws SQLException {
        super(entity,
              logicsInstance,
              session,
              securityPolicy,
              tFocusView,
              classListener,
              computer,
              connection,
              MapFact.singleton(dialogEntity, dialogValue),
              true,
              false,
              false,
              true,
              true,
              additionalFilters,
              pullProps
        );
        // все равно нашли объекты или нет

        dialogObject = instanceFactory.getInstance(dialogEntity);
    }

    public ObjectValue getDialogObjectValue() {
        return dialogObject.getObjectValue();
    }

    public Object getDialogValue() {
        return getDialogObjectValue().getValue();
    }

    public Object getCellDisplayValue() {
        try {
            if (initFilterPropertyDraw != null) {
                CalcPropertyObjectInstance filterInstance = instanceFactory.getInstance((CalcPropertyObjectEntity)initFilterPropertyDraw.propertyObject);
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
