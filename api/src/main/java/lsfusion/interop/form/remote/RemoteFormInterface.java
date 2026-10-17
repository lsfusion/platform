package lsfusion.interop.form.remote;

import lsfusion.base.Pair;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.event.ChangeSelection;
import lsfusion.interop.form.event.FormEvent;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.property.PropertyGroupType;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface RemoteFormInterface extends RemoteRequestInterface {

    ServerResponse getRemoteChanges(long requestIndex, long lastReceivedRequestIndex, boolean refresh, boolean forceLocalEvents) throws RemoteException;

    // events : form

    ServerResponse voidFormAction(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    ServerResponse gainedFocus(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    ServerResponse setTabActive(long requestIndex, long lastReceivedRequestIndex, int tabPaneID, int childId) throws RemoteException;

    ServerResponse changePropertyActive(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] columnKey, boolean focused,
                                        int[] changeSelectionProps, byte[][] changeSelectionColumnKeys, boolean[] changeSelectionValues) throws RemoteException;

    // user-driven hide of a component: a collapsed container or a CUSTOM REACT component hiding a delegated child
    ServerResponse setUserHidden(long requestIndex, long lastReceivedRequestIndex, int componentID, boolean hidden) throws RemoteException;

    ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, FormEvent formEvent, byte[] pushAsyncResult) throws RemoteException;

    // events : group objects

    ServerResponse changePageSize(long requestIndex, long lastReceivedRequestIndex, int groupID, Integer pageSize) throws RemoteException; // размер страницы

    ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupID, byte changeType, ChangeSelection changeSelection) throws RemoteException; // home / end

    ServerResponse selectAll(long requestIndex, long lastReceivedRequestIndex, int groupID,
                             int[] changeSelectionProps, byte[][] changeSelectionColumnKeys, boolean[] changeSelectionValues) throws RemoteException; // select all cells

    ServerResponse pasteExternalTable(long requestIndex, long lastReceivedRequestIndex, ArrayList<Integer> propertyIDs, List<byte[]> columnKeys, List<List<byte[]>> values, ArrayList<ArrayList<String>> rawValues, boolean forceGroupChange) throws RemoteException; // paste подряд

    ServerResponse pasteMulticellValue(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> keys, Map<Integer, byte[]> values, Map<Integer, String> rawValues) throws RemoteException; // paste выборочно

    Pair<List<List<byte[]>>, ArrayList<ArrayList<String>>> copyExternalTable(long requestIndex, long lastReceivedRequestIndex, List<Integer> propertyIDs, List<byte[]> columnKeys) throws RemoteException; // copy подряд - returns (values, rawValues)

    ServerResponse changeMode(long requestIndex, long lastReceivedRequestIndex, int groupObjectID, boolean setGroup, int[] propertyIDs, byte[][] columnKeys, int aggrProps, PropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, UpdateMode updateMode, ListViewType listViewType) throws RemoteException;

    // events : properties

    ServerResponse executeNotificationAction(long requestIndex, long lastReceivedRequestIndex, String notification) throws RemoteException;

    // async events : properties

    byte[] getAsyncValues(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, String actionSID, String value, int index, int increaseValuesNeededCount) throws RemoteException;

    // events : filters + orders

    // the requests a BatchRequest kind holds - a group's current object, a property's event action, a tree's nodes,
    // sortings, user filters, a filter group's filter -, one or several, in their order: one processing, one answer
    ServerResponse executeBatch(long requestIndex, long lastReceivedRequestIndex, List<BatchRequest> requests) throws RemoteException;

    ServerResponse setViewFilters(long requestIndex, long lastReceivedRequestIndex, byte[][] filters, int pageSize) throws RemoteException;

    // group object shortcut actions (system toolbar)

    Object getGroupReportData(long requestIndex, long lastReceivedRequestIndex, Integer groupId, FormUserPreferences userPreferences) throws RemoteException;

    Object getTreeGroupReportData(long requestIndex, long lastReceivedRequestIndex, int groupId, FormUserPreferences userPreferences) throws RemoteException;

    long countRecords(long requestIndex, long lastReceivedRequestIndex, int groupObjectID) throws RemoteException;

    Object calculateSum(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] columnKeys) throws RemoteException;

    byte[] groupData(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                              Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException;
    
    List<FormGrouping> readGroupings(long requestIndex, long lastReceivedRequestIndex, String groupObjectSID) throws RemoteException;
    
    void saveGrouping(long requestIndex, long lastReceivedRequestIndex, FormGrouping grouping) throws RemoteException;

    // user design customization

    ServerResponse saveUserPreferences(long requestIndex, long lastReceivedRequestIndex, GroupObjectUserPreferences preferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps) throws RemoteException;

    ServerResponse refreshUPHiddenProperties(long requestIndex, long lastReceivedRequestIndex, String groupObjectSID, String[] propSids) throws RemoteException;

    // external

    Pair<Long, String> changeExternal(long requestIndex, long lastReceivedRequestIndex, String json) throws RemoteException;
}
