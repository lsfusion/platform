package platform.server.view.form;

import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.view.navigator.NavigatorForm;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.session.DataSession;
import platform.server.auth.SecurityPolicy;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class RemoteDialog<T extends BusinessLogics<T>> extends RemoteForm<T> {

    ObjectImplement dialogObject;

    private RemoteDialog(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> tFocusView, CustomClassView classView, ObjectNavigator dialogNavigator, Map<ObjectNavigator, Object> mapObjects) throws SQLException {
        super(navigatorForm, BL, session, securityPolicy, tFocusView, classView, mapObjects);
        
        dialogObject = mapper.mapObject(dialogNavigator);
    }

    public RemoteDialog(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView, ObjectNavigator dialogNavigator) throws SQLException {
        this(navigatorForm, BL, session, securityPolicy, focusView, classView, dialogNavigator, new HashMap<ObjectNavigator, Object>());
    }

    public RemoteDialog(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView, ObjectNavigator dialogNavigator, Object dialogValue) throws SQLException {
        this(navigatorForm, BL, session, securityPolicy, focusView, classView, dialogNavigator, Collections.singletonMap(dialogNavigator, dialogValue));
    }

    public Object getDialogValue() {
        return dialogObject.getObjectValue().getValue();
    }
}
