package lsfusion.server.logics.property.actions;

import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ExportCSVDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {

    private String separator;
    private boolean noHeader;
    private String charset;

    public ExportCSVDataActionProperty(LocalizedString caption,
                                       ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                       ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, CalcPropertyInterfaceImplement<I> where, LCP targetProp,
                                       String separator, boolean noHeader, String charset) {
        super(caption, innerInterfaces, mapInterfaces, fields, exprs, where, targetProp);

        this.separator = separator == null ? "|" : separator;
        this.noHeader = noHeader;
        this.charset = charset == null ? "UTF-8" : charset;
    }

    @Override
    protected byte[] getExtension() {
        return "csv".getBytes();
    }

    @Override
    protected byte[] getFile(final Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {

        File file = File.createTempFile("export", ".csv");
        try (PrintWriter writer = new PrintWriter(file, charset)) {
            List<List<String>> lines = new ArrayList<>();
            if (!noHeader) {
                lines.add(fields.toJavaList());
            }

            for (ImMap<String, Object> row : rows) {
                List<String> line = new ArrayList<>();
                for (String key : row.keyIt()) {
                    Object cellValue = fieldTypes.getType(key).format(row.get(key));
                    line.add(cellValue != null && cellValue instanceof String ? (String) cellValue : "");
                }
                lines.add(line);
            }

            for (int i = 0; i < lines.size(); i++) {
                String line = StringUtils.join(lines.get(i).toArray(), separator);
                if (i < lines.size() - 1)
                    writer.println(line);
                else
                    writer.print(line);
            }
            writer.flush();
            return IOUtils.getFileBytes(file);
        } finally {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }
}
