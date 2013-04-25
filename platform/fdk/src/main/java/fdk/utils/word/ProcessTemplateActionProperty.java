package fdk.utils.word;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.classes.ValueClass;
import platform.server.classes.WordClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ProcessTemplateActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface templateInterface;

    public ProcessTemplateActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.getClassByName("Template")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        templateInterface = i.next();

    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        BufferedInputStream buffInputStream = null;
        BufferedOutputStream buffOutputStream = null;

        try {

            DataObject templateObject = context.getKeyValue(templateInterface);

            if (templateObject != null) {

                DataObject wordObject = new DataObject(LM.findLCPByCompoundName("fileTemplate").read(context, templateObject), WordClass.instance);
                Map<String, String> templateEntriesMap = new HashMap<String, String>();

                if (templateObject != null) {

                    KeyExpr templateEntryExpr = new KeyExpr("TemplateEntry");
                    ImRevMap<Object, KeyExpr> templateEntryKeys = MapFact.singletonRev((Object) "TemplateEntry", templateEntryExpr);

                    QueryBuilder<Object, Object> templateEntryQuery = new QueryBuilder<Object, Object>(templateEntryKeys);
                    templateEntryQuery.addProperty("keyTemplateEntry", getLCP("keyTemplateEntry").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("valueTemplateEntry", getLCP("valueTemplateEntry").getExpr(context.getModifier(), templateEntryExpr));

                    templateEntryQuery.and(getLCP("templateTemplateEntry").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context.getSession().sql);

                    for (ImMap<Object, Object> templateEntry : templateEntryResult.values()) {

                        String keyTemplateEntry = (String) templateEntry.get("keyTemplateEntry");
                        String valueTemplateEntry = (String) templateEntry.get("valueTemplateEntry");

                        if (keyTemplateEntry != null && valueTemplateEntry != null)
                            templateEntriesMap.put(keyTemplateEntry.trim(), valueTemplateEntry.trim().replace('\n', '\r'));
                    }

                    File templateFile = File.createTempFile("template", "doc");
                    FileOutputStream fileStream = new FileOutputStream(templateFile);
                    fileStream.write((byte[]) wordObject.object);
                    fileStream.close();

                    FileInputStream fileInputStream = new FileInputStream(templateFile.getAbsolutePath());
                    buffInputStream = new BufferedInputStream(fileInputStream);
                    HWPFDocument document = new HWPFDocument(new POIFSFileSystem(buffInputStream));

                    Range range = document.getRange();

                    for (Map.Entry<String, String> entry : templateEntriesMap.entrySet()) {
                        range.replaceText(entry.getKey(), entry.getValue());
                    }

                    File resultFile = File.createTempFile("result", "doc");
                    FileOutputStream fileOutputStream = new FileOutputStream(resultFile);
                    buffOutputStream = new BufferedOutputStream(fileOutputStream);
                    document.write(buffOutputStream);
                    buffOutputStream.close();

                    LM.findLCPByCompoundName("resultTemplate").change(IOUtils.toByteArray(new FileInputStream(resultFile)), context);

                }
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } finally {
            if (buffInputStream != null) {
                try {
                    buffInputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (buffOutputStream != null) {
                try {
                    buffOutputStream.flush();
                    buffOutputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}