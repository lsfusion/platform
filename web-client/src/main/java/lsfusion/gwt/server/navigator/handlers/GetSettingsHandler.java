package lsfusion.gwt.server.navigator.handlers;

import com.google.common.base.Throwables;
import lsfusion.base.ServerUtils;
import lsfusion.client.navigator.NavigatorData;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.navigator.GetSettings;
import lsfusion.gwt.client.controller.remote.action.navigator.GetSettingsResult;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.server.convert.ClientNavigatorToGwtConverter;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetSettingsHandler extends NavigatorActionHandler<GetSettings, GetSettingsResult> {
    public GetSettingsHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetSettingsResult executeEx(GetSettings action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        ServletContext servletContext = servlet.getServletContext();
        ClientNavigatorToGwtConverter clientNavigatorToGwtConverter = new ClientNavigatorToGwtConverter(servletContext, getServerSettings(action));

        RemoteNavigatorInterface remoteNavigator = getRemoteNavigator(action);
        NavigatorData navigatorData;
        try {
            navigatorData = NavigatorData.deserializeListClientNavigatorElementWithChildren(remoteNavigator.getNavigatorTree());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        GNavigatorElement root = clientNavigatorToGwtConverter.convertOrCast(navigatorData.root);

        ArrayList<GNavigatorWindow> navigatorWindows = navigatorData.windows.values().stream()
                        .map(t -> (GNavigatorWindow)clientNavigatorToGwtConverter.convertOrCast(t))
                        .collect(Collectors.toCollection(ArrayList::new));

        //getting common windows
        List<GAbstractWindow> windows = new ArrayList<>(Arrays.asList(clientNavigatorToGwtConverter.convertOrCast(navigatorData.logs),
                clientNavigatorToGwtConverter.convertOrCast(navigatorData.status),
                clientNavigatorToGwtConverter.convertOrCast(navigatorData.forms)));

        ClientSettings clientSettings = remoteNavigator.getClientSettings();
        ClientFormChangesToGwtConverter clientFormChangesToGwtConverter = ClientFormChangesToGwtConverter.getInstance();

        GColorTheme colorTheme = GColorTheme.valueOf(clientSettings.colorTheme.name());

        GColorPreferences colorPreferences = new GColorPreferences(
                clientFormChangesToGwtConverter.convertOrCast(clientSettings.colorPreferences.getSelectedRowBackground()),
                clientFormChangesToGwtConverter.convertOrCast(clientSettings.colorPreferences.getSelectedCellBackground()),
                clientFormChangesToGwtConverter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBackground()),
                clientFormChangesToGwtConverter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBorderColor()),
                clientFormChangesToGwtConverter.convertOrCast(clientSettings.colorPreferences.getTableGridColor()));

        return new GetSettingsResult(root, navigatorWindows, windows, clientSettings.busyDialogTimeout, clientSettings.devMode, clientSettings.projectLSFDir, clientSettings.showDetailedInfo,
                clientSettings.forbidDuplicateForms, clientSettings.showNotDefinedStrings, clientSettings.pivotOnlySelectedColumn, clientSettings.matchSearchSeparator,
                colorTheme, getVersionedColorThemesCss(), colorPreferences, clientSettings.localePreferences.dateFormat, clientSettings.localePreferences.timeFormat,
                servlet.staticImagesURL, clientSettings.preDefinedDateRangesNames, clientSettings.useTextAsFilterSeparator);
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
