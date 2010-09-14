package platform.client.form.decorator;

import platform.client.form.cell.CellTableInterface;
import platform.client.form.grid.GridTable;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;

public class ClientHighlighter implements Serializable {
    private Color color;
    private ClientPropertyDraw property;
    private transient String sID;

    public ClientHighlighter(DataInputStream inStream) throws IOException, ClassNotFoundException {
        sID = inStream.readUTF();
        color = (Color)new ObjectInputStream(inStream).readObject();
    }

    public void highlight(JComponent comp, HighlighterContext context) {
        CellTableInterface table = context.getTable();
        if (table instanceof GridTable) {
            GridTable gridTable = (GridTable)table;
            if (gridTable.getValue(property, context.getRow()) != null) {
                if (!context.isHasFocus() && !context.isSelected()) {
                    comp.setBackground(color);
                } else {
                    Color bgColor = comp.getBackground();
                    comp.setBackground(new Color(color.getRGB() & bgColor.getRGB()));
                }
            }
        }
    }

    public void init(Map<String, ClientPropertyDraw> sIDtoProperty) {
        property = sIDtoProperty.get(sID);
    }
}
