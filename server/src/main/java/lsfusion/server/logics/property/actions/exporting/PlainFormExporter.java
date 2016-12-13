package lsfusion.server.logics.property.actions.exporting;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.interop.form.ReportGenerationData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PlainFormExporter extends FormExporter{

    public PlainFormExporter(ReportGenerationData reportData) {
        super(reportData);
    }

    public abstract Map<String, byte[]> exportNodes(List<Node> rootNodes) throws IOException;

    public Map<String, byte[]> export() throws IOException {
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

    protected Map<String, byte[]> getFilesBytes(Map<String, File> filesMap) throws IOException {
        Map<String, byte[]> filesBytes = new HashMap<>();
        for (Map.Entry<String, File> entry : filesMap.entrySet())
            filesBytes.put(entry.getKey(), IOUtils.getFileBytes(entry.getValue()));
        return filesBytes;
    }

    protected void deleteFiles(Map<String, File> filesMap) {
        if (filesMap != null) {
            for (File file : filesMap.values())
                if (!file.delete())
                    file.deleteOnExit();
        }
    }
}