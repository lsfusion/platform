package lsfusion.interop.action;


import java.io.IOException;

public class ChooseObjectClientAction implements ClientAction {

    String title;
    String[] columnNames;
    Object[][] data;

    public ChooseObjectClientAction(String title, String[] columnNames, Object[][] data) {
        this.title = title;
        this.columnNames = columnNames;
        this.data = data;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        ChooseObjectDialog dialog = new ChooseObjectDialog(title, columnNames, data);
        return dialog.execute();
    }
}