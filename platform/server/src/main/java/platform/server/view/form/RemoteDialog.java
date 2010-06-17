package platform.server.view.form;

import platform.server.auth.SecurityPolicy;
import platform.server.logics.BusinessLogics;
import platform.server.session.DataSession;
import platform.server.view.navigator.NavigatorForm;
import platform.server.view.navigator.ObjectNavigator;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RemoteDialog<T extends BusinessLogics<T>> extends RemoteForm<T> {

    ObjectImplement dialogObject;

    private RemoteDialog(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> tFocusView, CustomClassView classView, ObjectNavigator dialogNavigator, PropertyObjectInterface computer, Map<ObjectNavigator, Object> mapObjects) throws SQLException {
        super(navigatorForm, BL, session, securityPolicy, tFocusView, classView, computer, mapObjects);
        
        dialogObject = mapper.mapObject(dialogNavigator);
    }

    public RemoteDialog(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView, ObjectNavigator dialogNavigator, PropertyObjectInterface computer) throws SQLException {
        this(navigatorForm, BL, session, securityPolicy, focusView, classView, dialogNavigator, computer, new HashMap<ObjectNavigator, Object>());
    }

    public RemoteDialog(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView, ObjectNavigator dialogNavigator, PropertyObjectInterface computer, Object dialogValue) throws SQLException {
        this(navigatorForm, BL, session, securityPolicy, focusView, classView, dialogNavigator, computer, Collections.singletonMap(dialogNavigator, dialogValue));
    }

    public Object getDialogValue() {
        return dialogObject.getObjectValue().getValue();
    }
}
