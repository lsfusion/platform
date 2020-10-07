package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.navigator.GetClientSettings;
import lsfusion.gwt.client.controller.remote.action.navigator.GetClientSettingsResult;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.ClientSettings;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GetClientSettingsHandler extends NavigatorActionHandler<GetClientSettings, GetClientSettingsResult> {
    public GetClientSettingsHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetClientSettingsResult executeEx(GetClientSettings action, ExecutionContext context) throws RemoteException {
        ClientSettings clientSettings = getRemoteNavigator(action).getClientSettings();
        ClientFormChangesToGwtConverter converter = ClientFormChangesToGwtConverter.getInstance();
        
        GColorTheme colorTheme = GColorTheme.valueOf(clientSettings.colorTheme.name());
        
        GColorPreferences colorPreferences = new GColorPreferences(
                converter.convertOrCast(clientSettings.colorPreferences.getSelectedRowBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getSelectedCellBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBorderColor()),
                converter.convertOrCast(clientSettings.colorPreferences.getTableGridColor())
        );
        
        return new GetClientSettingsResult(clientSettings.busyDialog, clientSettings.busyDialogTimeout,
                clientSettings.devMode, clientSettings.showDetailedInfo, clientSettings.forbidDuplicateForms, 
                clientSettings.showNotDefinedStrings, clientSettings.pivotOnlySelectedColumn, colorTheme, colorPreferences);
    }
}