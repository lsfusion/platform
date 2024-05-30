package lsfusion.gwt.server.navigator.handlers;

import com.google.common.base.Throwables;
import lsfusion.base.ServerUtils;
import lsfusion.client.navigator.NavigatorData;
import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.navigator.GClientSettings;
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
import lsfusion.http.controller.MainController;
import lsfusion.interop.connection.ClientType;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.navigator.ClientInfo;
import lsfusion.interop.navigator.ClientSettings;
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
        return new InitializeNavigatorResult(getClientSettings(getRemoteNavigator(action), servlet, new ClientInfo(action.screenSize, action.scale, action.mobile ? ClientType.WEB_MOBILE : ClientType.WEB_DESKTOP)), getNavigatorInfo(getRemoteNavigator(action), servlet, action.sessionID));
    }

    private static NavigatorInfo getNavigatorInfo(RemoteNavigatorInterface remoteNavigator, MainDispatchServlet servlet, String sessionID) throws RemoteException {
        ClientNavigatorToGwtConverter converter = new ClientNavigatorToGwtConverter(servlet, sessionID);

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
        windows.add((GAbstractWindow) converter.convertOrCast(navigatorData.forms));

        //put in navigator info navigator data first changes
        GNavigatorChangesDTO navigatorChanges = converter.convertOrCast(navigatorData.navigatorChanges);

        return new NavigatorInfo(root, navigatorWindows, navigatorChanges, windows);
    }

    private static GClientSettings getClientSettings(RemoteNavigatorInterface remoteNavigator, MainDispatchServlet servlet, ClientInfo clientInfo) throws RemoteException {
        ClientSettings clientSettings = MainController.getClientSettings(remoteNavigator, servlet.getRequest(), clientInfo);
        ClientFormChangesToGwtConverter converter = ClientFormChangesToGwtConverter.getInstance();

        GColorTheme colorTheme = GColorTheme.valueOf(clientSettings.colorTheme.name());

        GColorPreferences colorPreferences = new GColorPreferences(
                converter.convertOrCast(clientSettings.colorPreferences.getSelectedRowBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getSelectedCellBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBackground()),
                converter.convertOrCast(clientSettings.colorPreferences.getFocusedCellBorderColor()),
                converter.convertOrCast(clientSettings.colorPreferences.getTableGridColor())
        );

        LocalePreferences localePreferences = clientSettings.localePreferences;
        return new GClientSettings(clientSettings.busyDialogTimeout, clientSettings.devMode, clientSettings.projectLSFDir, clientSettings.showDetailedInfo,
                clientSettings.showDetailedInfoDelay, clientSettings.suppressOnFocusChange, clientSettings.autoReconnectOnConnectionLost,
                clientSettings.forbidDuplicateForms, clientSettings.pivotOnlySelectedColumn, clientSettings.matchSearchSeparator, colorTheme, clientSettings.useBootstrap,
                getVersionedColorThemesCss(servlet), colorPreferences, localePreferences.dateFormat, localePreferences.timeFormat, localePreferences.twoDigitYearStart,
                servlet.staticImagesURL, clientSettings.preDefinedDateRangesNames, clientSettings.useTextAsFilterSeparator, clientSettings.verticalNavbar,
                clientSettings.userFiltersManualApplyMode, clientSettings.disableActionsIfReadonly, clientSettings.enableShowingRecentlyLogMessages);
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
