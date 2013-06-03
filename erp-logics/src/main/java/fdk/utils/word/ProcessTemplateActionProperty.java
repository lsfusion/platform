package fdk.utils.word;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.WordClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProcessTemplateActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface templateInterface;

    public ProcessTemplateActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.getClassByName("Template")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        templateInterface = i.next();

    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            DataObject templateObject = context.getDataKeyValue(templateInterface);

            if (templateObject != null) {

                Object fileObject = LM.findLCPByCompoundName("fileTemplate").read(context, templateObject);
                if (fileObject != null) {

                    DataObject wordObject = new DataObject(fileObject, WordClass.get(false, false));
                    Map<String, String> templateEntriesMap = new HashMap<String, String>();

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

                    boolean isDocx = ((byte[]) fileObject).length<=2 ? false : ((byte[]) fileObject)[0] == 80 && ((byte[]) fileObject)[1] == 75;

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    if (isDocx) {
                        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream((byte[]) wordObject.object));
                        List<XWPFParagraph> docParagraphs = document.getParagraphs();

                        for (Map.Entry<String, String> entry : templateEntriesMap.entrySet()) {
                            for (XWPFParagraph p : docParagraphs) {
                                List<XWPFRun> runs = p.getRuns();
                                for (int i = runs.size() - 1; i >= 0; i--) {
                                    String text = runs.get(i).getText(0);
                                    if (text != null)
                                        text = text.replace(entry.getKey(), entry.getValue());
                                    runs.get(i).setText(text, 0);
                                }
                            }
                        }

                        document.write(outputStream);

                    } else {
                        HWPFDocument document = new HWPFDocument(new POIFSFileSystem(new ByteArrayInputStream((byte[]) wordObject.object)));
                        Range range = document.getRange();

                        for (Map.Entry<String, String> entry : templateEntriesMap.entrySet()) {
                            range.replaceText(entry.getKey(), entry.getValue());
                        }

                        document.write(outputStream);
                    }

                    LM.findLCPByCompoundName("resultTemplate").change(outputStream.toByteArray(), context);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }
}