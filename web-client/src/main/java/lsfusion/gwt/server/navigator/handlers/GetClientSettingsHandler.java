package lsfusion.gwt.server.navigator.handlers;

import lsfusion.base.ServerUtils;
import lsfusion.gwt.client.controller.remote.action.navigator.GetClientSettings;
import lsfusion.gwt.client.controller.remote.action.navigator.GetClientSettingsResult;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.ClientSettings;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

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
        
        return new GetClientSettingsResult(clientSettings.busyDialogTimeout, clientSettings.devMode, clientSettings.projectLSFDir, clientSettings.showDetailedInfo,
                clientSettings.forbidDuplicateForms, clientSettings.showNotDefinedStrings, clientSettings.pivotOnlySelectedColumn, clientSettings.matchSearchSeparator,
                colorTheme, getVersionedColorThemesCss(), colorPreferences, clientSettings.localePreferences.dateFormat, clientSettings.localePreferences.timeFormat,
                servlet.staticImagesURL, clientSettings.preDefinedDateRangesNames);
    }

    private Map<String, String> getVersionedColorThemesCss() throws RemoteException {
        Map<String, String> versionedColorThemesCss = new HashMap<>();
        try {
            for (GColorTheme value : GColorTheme.values()) {
                versionedColorThemesCss.put(value.getSid(), ServerUtils.getVersionedResource(servlet.getServletContext(), value.getUrl()));
            }
            return versionedColorThemesCss;
        } catch (IOException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }
}