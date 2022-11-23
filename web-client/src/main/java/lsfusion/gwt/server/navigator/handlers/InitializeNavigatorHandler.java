package lsfusion.gwt.server.navigator.handlers;

import com.google.common.base.Throwables;
import lsfusion.base.ServerUtils;
import lsfusion.client.navigator.NavigatorData;
import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.navigator.WebClientSettings;
import lsfusion.gwt.client.controller.remote.action.navigator.InitializeNavigator;
import lsfusion.gwt.client.controller.remote.action.navigator.InitializeNavigatorResult;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorInfo;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.server.convert.ClientNavigatorToGwtConverter;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InitializeNavigatorHandler extends NavigatorActionHandler<InitializeNavigator, InitializeNavigatorResult> {
    public InitializeNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public InitializeNavigatorResult executeEx(InitializeNavigator action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        RemoteNavigatorInterface remoteNavigator = getRemoteNavigator(action);
        NavigatorInfo navigatorInfo = getNavigatorInfo(remoteNavigator, servlet, getServerSettings(action));
        WebClientSettings webClientSettings = getClientSettings(remoteNavigator, servlet);

        servlet.getNavigatorProvider().updateNavigatorClientSettings(action.screenSize, action.mobile);

        return new InitializeNavigatorResult(webClientSettings, navigatorInfo);
    }

    private static NavigatorInfo getNavigatorInfo(RemoteNavigatorInterface remoteNavigator, MainDispatchServlet servlet, ServerSettings serverSettings) throws RemoteException {
        ClientNavigatorToGwtConverter converter = new ClientNavigatorToGwtConverter(servlet.getServletContext(), serverSettings);

        NavigatorData navigatorData;
        byte[] navigatorTree = remoteNavigator.getNavigatorTree();
        try {
            navigatorData = NavigatorData.deserializeListClientNavigatorElementWithChildren(navigatorTree);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        GNavigatorElement root = converter.convertOrCast(navigatorData.root);

        ArrayList<GNavigatorWindow> navigatorWindows = new ArrayList<>();
        for (ClientNavigatorWindow window : navigatorData.windows.values()) {
            GNavigatorWindow gWindow = converter.convertOrCast(window);
            navigatorWindows.add(gWindow);
        }

        //getting common windows
        List<GAbstractWindow> windows = new ArrayList<>();
        windows.add((GAbstractWindow) converter.convertOrCast(navigatorData.logs));
        windows.add((GAbstractWindow) converter.convertOrCast(navigatorData.status));
        windows.add((GAbstractWindow) converter.convertOrCast(navigatorData.forms));

        return new NavigatorInfo(root, navigatorWindows, windows);
    }

    private static WebClientSettings getClientSettings(RemoteNavigatorInterface remoteNavigator, MainDispatchServlet servlet) throws RemoteException {
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

        return new WebClientSettings(clientSettings.busyDialogTimeout, clientSettings.devMode, clientSettings.projectLSFDir, clientSettings.showDetailedInfo,
                clientSettings.forbidDuplicateForms, clientSettings.pivotOnlySelectedColumn, clientSettings.matchSearchSeparator,
                colorTheme, clientSettings.useBootstrap, getVersionedColorThemesCss(servlet), colorPreferences, clientSettings.localePreferences.dateFormat, clientSettings.localePreferences.timeFormat,
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
