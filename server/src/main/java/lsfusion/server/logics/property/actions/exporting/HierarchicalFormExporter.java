package lsfusion.server.logics.property.actions.exporting;

import com.google.common.base.Throwables;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.TimeClass;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class HierarchicalFormExporter extends FormExporter{
    protected Map<String, List<String>> formObjectGroups;
    protected Map<String, List<String>> formPropertyGroups;

    public HierarchicalFormExporter(ReportGenerationData reportData, Map<String, List<String>> formObjectGroups, Map<String, List<String>> formPropertyGroups) {
        super(reportData);
        this.formObjectGroups = formObjectGroups;
        this.formPropertyGroups = formPropertyGroups;
    }

    public abstract byte[] exportNodes(List<Node> rootNodes) throws IOException;

    public byte[] export() {
        if (data != null && formHierarchy != null) {
            try {
                List<Node> rootNodes = new ArrayList<>();
                FormObject rootObject = createFormObject(formHierarchy.first, formHierarchy.second);
                for (FormObject object : rootObject.dependencies) {
                    rootNodes.add(createGroup(object));
                }
                return exportNodes(rootNodes);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        return null;
    }

    protected String getLeafValue(Leaf node) {
        String leafValue = null;
        Object value = node.getValue();
        if (value != null) {
            if (value instanceof Date && node.getType().propertyType.equals("DATE")) {
                leafValue = DateClass.instance.formatString((Date) value);
            } else if (value instanceof Time && node.getType().propertyType.equals("TIME")) {
                leafValue = TimeClass.instance.formatString((Time) value);
            } else if (value instanceof Timestamp && node.getType().propertyType.equals("DATETIME")) {
                leafValue = DateTimeClass.instance.formatString((Timestamp) value);
            } else {
                leafValue = String.valueOf(value);
            }
        }
        return leafValue;
    }
}