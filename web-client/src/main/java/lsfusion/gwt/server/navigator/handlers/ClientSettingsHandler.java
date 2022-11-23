package lsfusion.gwt.server.navigator.handlers;

import lsfusion.base.ServerUtils;
import lsfusion.gwt.client.controller.remote.action.navigator.ClientSettingsResult;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class ClientSettingsHandler {

    public static ClientSettingsResult getClientSettings(RemoteNavigatorInterface remoteNavigator, MainDispatchServlet servlet) throws RemoteException {
        lsfusion.interop.navigator.ClientSettings clientSettings = remoteNavigator.getClientSettings();
        ClientFormChangesToGwtConverter converter = ClientFormChangesToGwtConverter.getInstance();

        GColorTheme colorTheme = GColorTheme.valueOf(clientSettings.colorTheme.name());

        GColorPreferences colorPreferences = new GColorPreferences(
                converter.convertOrCast(clientSettings.colorPreferences.getSelectedRowBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getSelectedCellBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBorderColor()),
                converter.convertOrCast(clientSettings.colorPreferences.getTableGridColor())
        );

        return new ClientSettingsResult(clientSettings.busyDialogTimeout, clientSettings.devMode, clientSettings.projectLSFDir, clientSettings.showDetailedInfo,
                clientSettings.forbidDuplicateForms, clientSettings.showNotDefinedStrings, clientSettings.pivotOnlySelectedColumn, clientSettings.matchSearchSeparator,
                colorTheme, getVersionedColorThemesCss(servlet), colorPreferences, clientSettings.localePreferences.dateFormat, clientSettings.localePreferences.timeFormat,
                servlet.staticImagesURL, clientSettings.preDefinedDateRangesNames, clientSettings.useTextAsFilterSeparator);
    }

    private static Map<String, String> getVersionedColorThemesCss(MainDispatchServlet servlet) throws RemoteException {
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