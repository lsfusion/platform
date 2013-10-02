package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.SaveUserPreferencesAction;
import lsfusion.gwt.form.shared.view.GColumnUserPreferences;
import lsfusion.gwt.form.shared.view.GGroupObjectUserPreferences;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SaveUserPreferencesActionHandler extends FormActionHandler<SaveUserPreferencesAction, VoidResult> {
    private final static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SaveUserPreferencesActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SaveUserPreferencesAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        ArrayList<GroupObjectUserPreferences> groupObjectUPList = new ArrayList<GroupObjectUserPreferences>();
        for (GGroupObjectUserPreferences gGroupObjectUP : action.formUserPreferences.getGroupObjectUserPreferencesList()) {
            HashMap<String, ColumnUserPreferences> columnUPMap = new HashMap<String, ColumnUserPreferences>();
            for (Map.Entry<String, GColumnUserPreferences> entry : gGroupObjectUP.getColumnUserPreferences().entrySet()) {
                GColumnUserPreferences gColumnUP = entry.getValue();
                columnUPMap.put(entry.getKey(), new ColumnUserPreferences(gColumnUP.isNeedToHide(), gColumnUP.getWidthUser(), gColumnUP.getOrderUser(), gColumnUP.getSortUser(), gColumnUP.getAscendingSortUser()));
            }
            GroupObjectUserPreferences groupObjectUP = new GroupObjectUserPreferences(columnUPMap, gGroupObjectUP.getGroupObjectSID(), gwtConverter.convertFont(gGroupObjectUP.getFontInfo()), gGroupObjectUP.hasUserPreferences());
            groupObjectUPList.add(groupObjectUP);
        }
        FormUserPreferences userPreferences = new FormUserPreferences(groupObjectUPList);

        form.remoteForm.saveUserPreferences(action.requestIndex, userPreferences, action.forAllUsers);
        return new VoidResult();
    }
}
