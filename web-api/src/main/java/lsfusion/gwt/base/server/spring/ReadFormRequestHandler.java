package lsfusion.gwt.base.server.spring;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import jasperapi.ClientReportData;
import jasperapi.ReportGenerator;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ReportConstants;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

public class ReadFormRequestHandler implements HttpRequestHandler {
    private static final String FORM_SID_PARAM = "sid";

    @Autowired
    private BusinessLogicsProvider blProvider;
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String formSID = request.getParameter(FORM_SID_PARAM);
        Map<String, String> initialObjects = new HashMap<String, String>();

        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = (String) parameterNames.nextElement();
            if (!FORM_SID_PARAM.equals(paramName)) {
                initialObjects.put(paramName, request.getParameter(paramName));
            }
        }

        ReportGenerationData reportData;
        try {
            RemoteLogicsInterface bl = blProvider.getLogics();

            String exportUser = context.getInitParameter("serviceUserLogin");
            String exportPassword = context.getInitParameter("serviceUserPassword");
            RemoteNavigatorInterface navigator = bl.createNavigator(true, exportUser, exportPassword, bl.getComputer(SystemUtils.getLocalHostName()), "127.0.0.1", false);

            if (!bl.checkFormExportPermission(formSID)) {
                blProvider.invalidate();
                throw new RuntimeException("Невозможно прочитать данные формы: нет прав.");
            }

            RemoteFormInterface form = navigator.createForm(formSID, initialObjects, false, false);
            reportData = form.getReportData(-1, -1, null, false, null);

        } catch (RemoteException e) {
            blProvider.invalidate();
            throw new RuntimeException("Не могу прочитать данные", e);
        }

        XmlBuilder xmlBuilder = new XmlBuilder(reportData);

        File file = xmlBuilder.build();
        FileInputStream fis = new FileInputStream(file);
        response.setContentType("application/xml");
        response.addHeader("Content-Disposition", "attachment; filename=" + formSID + ".xml");
        ByteStreams.copy(fis, response.getOutputStream());
        fis.close();
        file.delete();
    }

    private class XmlBuilder {
        XMLStreamWriter xsw;
        Map<String, ClientReportData> data;
        Pair<String, Map<String, List<String>>> formHierarchy;

        public XmlBuilder(ReportGenerationData reportData) {
            try {
                data = ReportGenerator.retrieveReportSources(reportData, null).data;
                formHierarchy = ReportGenerator.retrieveReportHierarchy(reportData.reportHierarchyData);
            } catch (IOException e) {
                Throwables.propagate(e);
            } catch (ClassNotFoundException e) {
                Throwables.propagate(e);
            }
        }

        public File build() {
            try {
                String fileName = "formData" + GwtSharedUtils.randomString(7) + ".xml";
                File file = new File(context.getRealPath("WEB-INF/temp"), fileName);
                XMLOutputFactory xof = XMLOutputFactory.newInstance();
                FileOutputStream fos = new FileOutputStream(file);

                xsw = xof.createXMLStreamWriter(new BufferedOutputStream(fos), "utf-8");

                xsw.writeStartDocument("utf-8", "1.0");
                xsw.writeCharacters("\n\n");

                FormObject rootObject = createFormObject(formHierarchy.first, formHierarchy.second);

                for (FormObject object : rootObject.dependencies) {
                    xsw.writeStartElement("group");

                    writeGroup(object);

                    xsw.writeCharacters("\n");
                    xsw.writeEndElement();
                    xsw.writeCharacters("\n");
                }

                xsw.writeEndDocument();
                xsw.close();
                fos.close();
                return file;
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            return null;
        }

        private FormObject createFormObject(String object, Map<String, List<String>> hierarchy) {
            FormObject fo = new FormObject(object);
            for (String dep : hierarchy.get(object)) {
                fo.dependencies.add(createFormObject(dep, hierarchy));
            }
            return fo;
        }

        private void writeGroup(FormObject object) throws XMLStreamException {
            String initialIndent = "\n\t";
            for (HashMap<Integer, Object> keys : data.get(object.object).getKeyRows()) {
                writeObjectValues(object, keys, initialIndent, new HashSet<String>());
            }
        }

        private void writeDependentObjects(FormObject object, HashMap<Integer, Object> keys, String indent, Set<String> usedProperties) throws XMLStreamException {
            for (FormObject dependent : object.dependencies) {
                ClientReportData reportData = data.get(dependent.object);

                for (HashMap<Integer, Object> k : reportData.getKeyRows()) {
                    if (BaseUtils.containsAll(k, keys)) {
                        writeObjectValues(dependent, k, indent, usedProperties);
                    }
                }
            }
        }

        private void writeObjectValues(FormObject object, HashMap<Integer, Object> keys, String indent, Set<String> usedProperties) throws XMLStreamException {
            ClientReportData reportData = data.get(object.object);

            xsw.writeCharacters(indent);
            xsw.writeStartElement(object.object);

            Map<Pair<Integer, Integer>, Object> values = reportData.getRows().get(keys);

            for (String property : reportData.getPropertyNames()) {
                if (!usedProperties.contains(property) && !property.endsWith(ReportConstants.captionSuffix)) {
                    if (reportData.getCompositeColumnObjects().containsKey(property)) {
                        for (List<Object> columnKeys : reportData.getCompositeColumnValues().get(property)) {
                            List<Integer> columnObjects = reportData.getCompositeColumnObjects().get(property);

                            List<Object> cKeys = new ArrayList<Object>();
                            for (Integer key : reportData.getCompositeFieldsObjects().get(property)) {
                                if (columnObjects.contains(key)) {
                                    cKeys.add(columnKeys.get(columnObjects.indexOf(key)));
                                } else {
                                    cKeys.add(keys.get(key));
                                }
                            }

                            xsw.writeCharacters(indent + "\t");
                            if (reportData.getPropertyNames().contains(property + ReportConstants.captionSuffix)) {
                                xsw.writeStartElement(String.valueOf(reportData.getCompositeObjectValues().get(property + ReportConstants.captionSuffix).get(cKeys)));
                            } else {
                                xsw.writeStartElement(property);
                            }
                            xsw.writeCharacters(String.valueOf(reportData.getCompositeObjectValues().get(property).get(cKeys)));
                            xsw.writeEndElement();
                        }
                    } else {
                        xsw.writeCharacters(indent + "\t");
                        xsw.writeStartElement(property);
                        xsw.writeCharacters(String.valueOf(values.get(reportData.getProperties().get(property))));
                        xsw.writeEndElement();
                    }
                }
            }
            Set<String> usedPropertiesSet = new HashSet<String>(usedProperties);
            usedPropertiesSet.addAll(reportData.getPropertyNames());

            writeDependentObjects(object, keys, indent + "\t", usedPropertiesSet);

            xsw.writeCharacters(indent);
            xsw.writeEndElement();
        }
    }

    private class FormObject {
        public String object;
        public List<FormObject> dependencies = new ArrayList<FormObject>();

        public FormObject(String obj) {
            object = obj;
        }
    }
}
