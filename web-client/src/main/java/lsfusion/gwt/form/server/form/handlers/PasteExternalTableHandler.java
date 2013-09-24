package lsfusion.gwt.form.server.form.handlers;

import lsfusion.client.ClientResourceBundle;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.client.form.TableTransferHandler;
import lsfusion.client.logics.classes.*;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.PasteExternalTable;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.serializeObject;

public class PasteExternalTableHandler extends ServerResponseActionHandler<PasteExternalTable> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public PasteExternalTableHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(PasteExternalTable action, ExecutionContext context) throws DispatchException, IOException {
        List<List<String>> stringTable = TableTransferHandler.getClipboardTable(action.line);

        int tableColumns = 0;
        if (!stringTable.isEmpty()) {
            tableColumns = stringTable.get(0).size();
        }
        int columnsToInsert = Math.min(tableColumns, action.properties.size());

        List<Integer> propertyIDs = new ArrayList<Integer>();
        List<byte[]> columnKeys = new ArrayList<byte[]>();
        for (int i = 0; i < columnsToInsert; i++) {
            propertyIDs.add(action.properties.get(i).ID);
            columnKeys.add((byte[]) gwtConverter.convertOrCast(action.columnKeys.get(i)));
        }

        List<List<byte[]>> values = new ArrayList<List<byte[]>>();
        for (List<String> sRow : stringTable) {
            List<byte[]> valueRow = new ArrayList<byte[]>();

            int rowLength = Math.min(sRow.size(), columnsToInsert);
            for (int i = 0; i < rowLength; i++) {
                GPropertyDraw property = action.properties.get(i);

                Object oCell = parseString(property, sRow.get(i));

                valueRow.add(serializeObject(oCell));
            }
            values.add(valueRow);
        }

        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.pasteExternalTable(action.requestIndex, propertyIDs, columnKeys, values));
    }

    private Object parseString(GPropertyDraw property, String s) {
        if (s == null) {
            return null;
        }
        try {
            if (property.baseType instanceof GDateType) {
                return ClientDateClass.instance.parseString(s);
            } else if (property.baseType instanceof GDateTimeType) {
                return ClientDateTimeClass.instance.parseString(s);
            } else if (property.baseType instanceof GTimeType) {
                return ClientTimeClass.instance.parseString(s);
            } else if (property.baseType instanceof GNumericType) {
                return commonParseString(s.replaceAll(" ", ""));
            } else if (property.baseType instanceof GDoubleType) {
                return ClientDoubleClass.instance.parseString(s.replaceAll(" ", ""));
            } else if (property.baseType instanceof GColorType) {
                return ClientColorClass.instance.parseString(s);
            } else {
                return property.parseString(s);
            }
        } catch (ParseException e) {
            return null;
        }
    }

    public static Object commonParseString(String s) throws ParseException {
        try {
            return BigDecimal.valueOf(NumberFormat.getInstance().parse(s).doubleValue());
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.big.decimal"), 0);
        }
    }

}
