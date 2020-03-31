package lsfusion.client.form.property.panel.view;

import com.google.common.base.Throwables;
import lsfusion.base.ReflectionUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.data.ClientTextClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.ClientSingleCellRenderer;
import lsfusion.client.form.property.panel.SingleCellTableModel;
import lsfusion.client.form.property.table.view.ClientPropertyTable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.base.view.SwingDefaults.getSingleCellTableIntercellSpacing;
import static lsfusion.client.form.controller.ClientFormController.PasteData;

public abstract class SingleCellTable extends ClientPropertyTable {

    private final SingleCellTableModel model;

    public SingleCellTable(ClientGroupObjectValue columnKey, ClientFormController form) {
        super(new SingleCellTableModel(columnKey), form, null, new ClientSingleCellRenderer());

        model = (SingleCellTableModel) getModel();

        SwingUtils.setupSingleCellTable(this);
    }

    // is called after color theme change, overwrites our values and changes getCellRect() result -> renderer gets larger size  
    @Override
    public void setIntercellSpacing(Dimension intercellSpacing) {
        super.setIntercellSpacing(new Dimension(getSingleCellTableIntercellSpacing(), getSingleCellTableIntercellSpacing()));
    }

    public void setProperty(ClientPropertyDraw property) {
        setName(property.getCaption());
        model.setProperty(property);

        // cell height is calculated without row margins (getCellRect()). Row margin = intercell spacing.
        setPreferredSize(new Dimension(property.getValueWidth(this), property.getValueHeight(this) + getRowMargin()));
    }

    public void setValue(Object value) {
        model.setValue(value);
        if(getProperty().autoSize && value instanceof String) {
            Dimension size = getSize();
            if (size != null && size.getWidth() > 0) {
                setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), getHeight((String) value, (int) size.getWidth())));
                revalidate();
            }
        }
        repaint();
    }

    private int getHeight(String text, int maxWidth) {
        int rows = 0;
        FontMetrics fm = getFontMetrics(getFont());
        if (text != null) {
            String[] lines = text.split("\n");
            rows += lines.length;
            for(String line : lines) {
                String[] splittedText = line.split(" ");
                String output = "";
                int outputWidth = 0;
                int spaceWidth = fm.charWidth(' ');
                int wordWidth;
                int j = 1;

                for (String word : splittedText) {
                    wordWidth = 0;
                    for (int i = 0; i < word.length(); i++)
                        wordWidth += fm.charWidth(word.charAt(i));
                    if ((outputWidth + spaceWidth + wordWidth) < maxWidth) {
                        output = output.concat(" ").concat(word);
                        outputWidth += spaceWidth + wordWidth;
                    } else {
                        rows++;
                        output = word;
                        outputWidth = wordWidth;
                        j = j + 1;
                    }
                }
            }
        }
        return (rows + 1) * fm.getHeight();
    }

    public void setReadOnly(boolean readOnly) {
        model.setReadOnly(readOnly);
    }

    @Override
    public int getCurrentRow() {
        return 0;
    }

    public ClientGroupObjectValue getColumnKey(int row, int col) {
        return model.getColumnKey();
    }

    public ClientPropertyDraw getProperty() {
        return model.getProperty();
    }

    public ClientPropertyDraw getProperty(int row, int column) {
        return model.getProperty();
    }

    @Override
    public boolean richTextSelected() {
        ClientPropertyDraw property = getProperty();
        return property.baseType instanceof ClientTextClass && ((ClientTextClass) property.baseType).rich;
    }

    public void pasteTable(List<List<String>> table) {
        if (!table.isEmpty() && !table.get(0).isEmpty()) {
            try {
                ClientPropertyDraw property = model.getProperty();
                String value = table.get(0).get(0);

                boolean matches = true;
                if (property.regexp != null && value != null && !value.isEmpty()) {
                    if (!value.matches(property.regexp)) {
                        matches = false;
                        showErrorTooltip(property, value);
                    }
                }
                if(matches) {
                    Object newValue = value == null ? null : property.parseChangeValueOrNull(value);
                    if (property.canUsePasteValueForRendering()) {
                        setValue(newValue);
                    }

                    getForm().pasteMulticellValue(
                            singletonMap(property, new PasteData(newValue, singletonList(model.getColumnKey()), singletonList(model.getValue())))
                    );
                }
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
    }

    private void showErrorTooltip(ClientPropertyDraw property, String value) {
        String currentError = (property.regexpMessage == null ? getString("form.editor.incorrect.value") : property.regexpMessage) + ": " + value;

        setToolTipText(currentError);

        //имитируем ctrl+F1 http://qaru.site/questions/368838/force-a-java-tooltip-to-appear
        this.dispatchEvent(new KeyEvent(this, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), InputEvent.CTRL_MASK,
                KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED));

        setToolTipText(null);
    }

    public boolean isSelected(int row, int column) {
        return false;
    }

    // приходится делать вот таким извращенным способом, поскольку ComponentListener срабатывает после перерисовки формы
    @Override
    public void setBounds(int x, int y, int width, int height) {
        rowHeight = height;
        // after switching color theme UI changes rowHeight to LaF default if isRowHeightSet is not set 
        ReflectionUtils.setPrivateFieldValue(JTable.class, this, "isRowHeightSet", true);
        super.setBounds(x, y, width, height);
    }

    public abstract ClientFormController getForm();

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component editorComp = super.prepareEditor(editor, row, column);
        if (editorComp != null) {
            //вырезаем traversal-кнопки, потому что иначе фокус просто вернётся в таблицу
            editorComp.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<>());
            editorComp.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new HashSet<>());
        }
        return editorComp;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        
        setBorder(SwingDefaults.getTextFieldBorder());
    }
}
