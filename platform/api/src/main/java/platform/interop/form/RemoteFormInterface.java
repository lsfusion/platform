package platform.interop.form;

import platform.interop.ClassViewType;
import platform.interop.RemoteContextInterface;
import platform.interop.action.ClientApply;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteFormInterface extends PendingRemote, RemoteContextInterface {

    // операции с ответом
    byte[] getRichDesignByteArray() throws RemoteException;

    byte[] getReportHierarchyByteArray() throws RemoteException;
    byte[] getSingleGroupReportHierarchyByteArray(int groupId) throws RemoteException;

    byte[] getReportDesignsByteArray(boolean toExcel, FormUserPreferences userPreferences) throws RemoteException;
    byte[] getSingleGroupReportDesignByteArray(boolean toExcel, int groupId, FormUserPreferences userPreferences) throws RemoteException;
    byte[] getReportSourcesByteArray() throws RemoteException;
    byte[] getSingleGroupReportSourcesByteArray(int groupId) throws RemoteException;
    Map<String, String> getReportPath(boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException;

    public RemoteChanges getRemoteChanges() throws RemoteException;

    String getSID() throws RemoteException;

    // синхронная проверка на то можно ли менять свойство
    byte[] getPropertyChangeType(int propertyID, byte[] columnKey, boolean aggValue) throws RemoteException;

    boolean canChangeClass(int objectID) throws RemoteException;

    boolean hasClientApply() throws RemoteException; // чисто для оптимизации одного RMI вызова

    RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException;

    RemoteDialogInterface createObjectEditorDialog(int viewID) throws RemoteException;

    RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException;

    // операции без ответа, можно pendiть до первой операции с ответом

    void changePageSize(int groupID, Integer pageSize) throws RemoteException;
    void gainedFocus() throws RemoteException;

    void changeGroupObject(int groupID, byte[] value) throws RemoteException;

    void changeGroupObject(int groupID, byte changeType) throws RemoteException;

    RemoteChanges changePropertyDraw(int propertyID, byte[] columnKey, byte[] object, boolean all, boolean aggValue) throws RemoteException;

    RemoteChanges continuePausedInvocation() throws RemoteException;

    RemoteChanges groupChangePropertyDraw(int mainID, byte[] mainColumnKey, int getterID, byte[] getterColumnKey) throws RemoteException;

    RemoteChanges pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws RemoteException;

    RemoteChanges pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws RemoteException;

    boolean[] getCompatibleProperties(int mainPropertyID, int[] propertiesIDs) throws RemoteException;

    Object getPropertyChangeValue(int propertyID) throws RemoteException;

    void switchClassView(int groupID) throws RemoteException;

    void changeClassView(int groupID, ClassViewType classView) throws RemoteException;

    void changeClassView(int groupID, String classViewName) throws RemoteException;

    void changeGridClass(int objectID,int idClass) throws RemoteException;

    void changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    void clearUserFilters() throws RemoteException;

    void addFilter(byte[] state) throws RemoteException;

    void setRegularFilter(int groupID, int filterID) throws RemoteException;

    int countRecords(int groupObjectID) throws RemoteException;

    Object calculateSum(int propertyID, byte[] columnKeys) throws RemoteException;

    Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                              Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException;

    void refreshData() throws RemoteException;

    void cancelChanges() throws RemoteException;

    void expandGroupObject(int groupId, byte[] bytes) throws RemoteException;

    void moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException;

    ClientApply checkClientChanges() throws RemoteException;

    RemoteChanges okPressed() throws RemoteException;
    RemoteChanges closedPressed() throws RemoteException;

    RemoteChanges applyChanges(Object clientResult) throws RemoteException;

    void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) throws RemoteException;

    FormUserPreferences loadUserPreferences() throws RemoteException;
}
