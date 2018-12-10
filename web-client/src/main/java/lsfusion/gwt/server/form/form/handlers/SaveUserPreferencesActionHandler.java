package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.form.shared.actions.form.SaveUserPreferencesAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.GColumnUserPreferences;
import lsfusion.gwt.form.shared.view.GGroupObjectUserPreferences;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveUserPreferencesActionHandler extends FormServerResponseActionHandler<SaveUserPreferencesAction> {
    private final static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SaveUserPreferencesActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SaveUserPreferencesAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        GGroupObjectUserPreferences gGroupObjectUP = action.groupObjectUserPreferences;
        
        HashMap<String, ColumnUserPreferences> columnUPMap = new HashMap<>();
        for (Map.Entry<String, GColumnUserPreferences> entry : gGroupObjectUP.getColumnUserPreferences().entrySet()) {
            GColumnUserPreferences gColumnUP = entry.getValue();
            columnUPMap.put(entry.getKey(), new ColumnUserPreferences(gColumnUP.userHide, gColumnUP.userCaption, gColumnUP.userPattern, gColumnUP.userWidth, gColumnUP.userOrder, gColumnUP.userSort, gColumnUP.userAscendingSort));
        }
        GroupObjectUserPreferences groupObjectUP = new GroupObjectUserPreferences(columnUPMap, gGroupObjectUP.getGroupObjectSID(), gwtConverter.convertFont(gGroupObjectUP.getFont()), gGroupObjectUP.getPageSize(), gGroupObjectUP.getHeaderHeight(), gGroupObjectUP.hasUserPreferences());

        return getServerResponseResult(form, form.remoteForm.saveUserPreferences(action.requestIndex, defaultLastReceivedRequestIndex, groupObjectUP, action.forAllUsers, action.completeOverride, action.hiddenProps));
    }
}
