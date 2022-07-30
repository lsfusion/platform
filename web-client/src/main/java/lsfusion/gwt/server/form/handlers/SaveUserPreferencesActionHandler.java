package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.SaveUserPreferencesAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class SaveUserPreferencesActionHandler extends FormServerResponseActionHandler<SaveUserPreferencesAction> {
    private final static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SaveUserPreferencesActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SaveUserPreferencesAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                GGroupObjectUserPreferences gGroupObjectUP = action.groupObjectUserPreferences;

                HashMap<String, ColumnUserPreferences> columnUPMap = new HashMap<>();
                for (Map.Entry<String, GColumnUserPreferences> entry : gGroupObjectUP.getColumnUserPreferences().entrySet()) {
                    GColumnUserPreferences gColumnUP = entry.getValue();
                    columnUPMap.put(entry.getKey(), new ColumnUserPreferences(gColumnUP.userHide, gColumnUP.userCaption, gColumnUP.userPattern, gColumnUP.userWidth, gColumnUP.userFlex, gColumnUP.userOrder, gColumnUP.userSort, gColumnUP.userAscendingSort));
                }
                GroupObjectUserPreferences groupObjectUP = new GroupObjectUserPreferences(columnUPMap, gGroupObjectUP.getGroupObjectSID(), gwtConverter.convertFont(gGroupObjectUP.getFont()), gGroupObjectUP.getPageSize(), gGroupObjectUP.getHeaderHeight(), gGroupObjectUP.hasUserPreferences());

                return remoteForm.saveUserPreferences(action.requestIndex, action.lastReceivedRequestIndex, groupObjectUP, action.forAllUsers, action.completeOverride, action.hiddenProps);
            }
        });
    }
}
