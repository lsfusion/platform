package lsfusion.server.logics.property.actions.exporting;

import com.google.common.base.Throwables;
import lsfusion.interop.form.ReportGenerationData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class HierarchicalFormExporter extends FormExporter{

    public HierarchicalFormExporter(ReportGenerationData reportData) {
        super(reportData);
    }

    public abstract byte[] exportNodes(List<Node> rootNodes) throws IOException;

    public byte[] export() throws IOException {
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
}