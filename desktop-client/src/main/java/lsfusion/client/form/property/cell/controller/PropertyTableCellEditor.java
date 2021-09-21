package lsfusion.client.form.property.cell.controller;

import javax.swing.*;
import javax.swing.table.TableCellEditor;

public interface PropertyTableCellEditor extends TableCellEditor {
    JTable getTable();
    
    /**
     *  Похожая ситуация, как и в SingleFilterBox: не стоит блокировать EDT (показывать BusyDialog) внутри обработки события (изменения фокуса).
     *  http://bugs.java.com/view_bug.do?bug_id=6924233
     *  Но вызов через invokeLater() по идее более безопасен.
     *  Добавлялось по причине исчезновения фокуса из области видимости при редактировании BOOLEAN свойств.
     */                                            
    void stopCellEditingLater();
    
    void preCommit(boolean enterPressed);
    void postCommit();
}
