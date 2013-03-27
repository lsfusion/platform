package roman.actions;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import platform.base.BaseUtils;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImportGroupsXMLDeclarationActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface declarationInterface;
    List<Object> row;

    public ImportGroupsXMLDeclarationActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{LM.getClassByName("Declaration")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        declarationInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.getDefinedInstance(false, "Файлы XML", "xml");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                DataObject declaration = context.getKeyValue(declarationInterface);
                ObjectValue customsZone = LM.findLCPByCompoundName("customsZoneDeclaration").readClasses(context.getSession(), declaration);
                for (byte[] file : fileList) {

                    List<List<Object>> data = new ArrayList<List<Object>>();

                    SAXBuilder builder = new SAXBuilder();
                    Document document = builder.build(new ByteArrayInputStream(file));
                    Element rootNode = document.getRootElement();
                    Namespace ns = rootNode.getNamespace("ESADout_CU");
                    Namespace gns = rootNode.getNamespace("catESAD_cu");
                    rootNode = rootNode.getChild("ESADout_CUGoodsShipment", ns);
                    List list = rootNode.getChildren("ESADout_CUGoods", ns);
                    for (int i = 0; i < list.size(); i++) {
                        Element node = (Element) list.get(i);
                        row = new ArrayList<Object>();
                        List payment = node.getChildren("ESADout_CUCustomsPaymentCalculation", ns);

                        Double duty = null;
                        Double vat = null;
                        for (Object p : payment) {
                            String paymentModeCode = ((Element) p).getChildText("PaymentModeCode", gns);
                            if ("2010".equals(paymentModeCode)) {
                                duty = Double.valueOf(((Element) p).getChildText("PaymentAmount", gns));
                            } else if ("5010".equals(paymentModeCode))
                                vat = Double.valueOf(((Element) p).getChildText("PaymentAmount", gns));
                        }
                        Double sum = Double.valueOf(node.getChildText("CustomsCost", gns));
                        Integer number = Integer.valueOf(node.getChildText("GoodsNumeric", gns));
                        String description = node.getChildText("GoodsDescription", gns);
                        String TNVED = node.getChildText("GoodsTNVEDCode", gns);
                        String countryCode = node.getChildText("OriginCountryCode", gns);
                        Element goodsGroupDescription = node.getChild("GoodsGroupDescription", gns);
                        Element goodsGroupInformation = goodsGroupDescription.getChild("GoodsGroupInformation", gns);
                        String goodsMarking = goodsGroupInformation.getChildText("GoodsMarking", gns);
                        String goodsMark = goodsGroupInformation.getChildText("GoodsMark", gns);
                        row.add(number);
                        row.add(description);
                        row.add(sum);
                        row.add(duty);
                        row.add(vat);
                        row.add(TNVED);
                        row.add(countryCode);
                        row.add(goodsMark);
                        row.add(goodsMarking);
                        data.add(row);
                    }

                    ImportField overNumberGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("overNumberGroupDeclaration"));
                    ImportField nameDataGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("nameDataGroupDeclaration"));
                    ImportField sumDataGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("sumDataGroupDeclaration"));
                    ImportField dutyDataGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("dutyDataGroupDeclaration"));
                    ImportField VATDataGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("VATDataGroupDeclaration"));
                    ImportField customCategory10Field = new ImportField(LM.findLCPByCompoundName("sidCustomCategory10"));
                    ImportField sidOrigin2CountryGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("sidOrigin2CountryGroupDeclaration"));
                    ImportField customsSIDBrandSupplierField = new ImportField(LM.findLCPByCompoundName("customsSIDBrandSupplier"));
                    ImportField sidArticleGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("sidArticleGroupDeclaration"));

                    List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

                    ImportKey<?> groupDeclarationKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("groupDeclaration"),
                            LM.findLCPByCompoundName("uniqueGroupDeclaration").getMapping(overNumberGroupDeclarationField, declaration));

                    ImportKey<?> customCategory10Key = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("customCategory10"),
                            getLCP("sidToCustomCategory10").getMapping(customCategory10Field, customsZone));

                    ImportKey<?> articleKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("articleComposite"), LM.findLCPByCompoundName("articleCustomsSIDSupplier").getMapping(sidArticleGroupDeclarationField, customsSIDBrandSupplierField));

                    properties.add(new ImportProperty(overNumberGroupDeclarationField, LM.findLCPByCompoundName("overNumberGroupDeclaration").getMapping(groupDeclarationKey)));
                    properties.add(new ImportProperty(nameDataGroupDeclarationField, LM.findLCPByCompoundName("nameDataGroupDeclaration").getMapping(groupDeclarationKey)));
                    properties.add(new ImportProperty(sumDataGroupDeclarationField, LM.findLCPByCompoundName("sumDataGroupDeclaration").getMapping(groupDeclarationKey)));
                    properties.add(new ImportProperty(dutyDataGroupDeclarationField, LM.findLCPByCompoundName("dutyDataGroupDeclaration").getMapping(groupDeclarationKey)));
                    properties.add(new ImportProperty(VATDataGroupDeclarationField, LM.findLCPByCompoundName("VATDataGroupDeclaration").getMapping(groupDeclarationKey)));
                    properties.add(new ImportProperty(declaration, getLCP("declarationGroupDeclaration").getMapping(groupDeclarationKey)));

                    properties.add(new ImportProperty(customCategory10Field, getLCP("sidCustomCategory10").getMapping(customCategory10Key)));
                    properties.add(new ImportProperty(customCategory10Field, getLCP("customCategory10GroupDeclaration").getMapping(groupDeclarationKey),
                            LM.object(getClass("customCategory10")).getMapping(customCategory10Key)));
                    properties.add(new ImportProperty(sidOrigin2CountryGroupDeclarationField, LM.findLCPByCompoundName("sidOrigin2CountryGroupDeclaration").getMapping(groupDeclarationKey)));
                    properties.add(new ImportProperty(sidArticleGroupDeclarationField, LM.findLCPByCompoundName("sidArticleGroupDeclaration").getMapping(groupDeclarationKey)));
                    properties.add(new ImportProperty(sidArticleGroupDeclarationField, getLCP("sidArticle").getMapping(articleKey)));
                    properties.add(new ImportProperty(sidArticleGroupDeclarationField, getLCP("articleGroupDeclaration").getMapping(groupDeclarationKey),
                            LM.object(getClass("articleComposite")).getMapping(articleKey)));

                    List<ImportField> fields = BaseUtils.toList(overNumberGroupDeclarationField, nameDataGroupDeclarationField,
                            sumDataGroupDeclarationField, dutyDataGroupDeclarationField, VATDataGroupDeclarationField,
                            customCategory10Field, sidOrigin2CountryGroupDeclarationField,
                            customsSIDBrandSupplierField, sidArticleGroupDeclarationField
                    );
                    articleKey.skipKey = true;
                    ImportKey<?>[] keysArray = new ImportKey<?>[]{groupDeclarationKey, customCategory10Key, articleKey};

                    IntegrationService integrationService = new IntegrationService(context.getSession(), new ImportTable(fields, data), Arrays.asList(keysArray), properties);
                    integrationService.synchronize(true, false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
