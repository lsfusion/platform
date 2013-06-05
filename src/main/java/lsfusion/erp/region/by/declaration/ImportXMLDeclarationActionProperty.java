package lsfusion.erp.region.by.declaration;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.classes.DateClass;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class ImportXMLDeclarationActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface declarationInterface;
    List<Object> row;

    public ImportXMLDeclarationActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LM.findClassByCompoundName("Declaration"));

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        declarationInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы XML", "xml");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                DataObject declaration = context.getDataKeyValue(declarationInterface);
                //ObjectValue customsZone = LM.findLCPByCompoundName("customsZoneDeclaration").readClasses(context.getSession(), declaration);
                for (byte[] file : fileList) {

                    List<List<Object>> data = new ArrayList<List<Object>>();
                    Date defaultDate = new Date(2011-1900, 0, 1);

                    SAXBuilder builder = new SAXBuilder();
                    Document document = builder.build(new ByteArrayInputStream(file));
                    Element rootNode = document.getRootElement();
                    Namespace ns = rootNode.getNamespace("ESADout_CU");
                    Namespace gns = rootNode.getNamespace("catESAD_cu");
                    Namespace cu = rootNode.getNamespace("cat_ru");
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
                        Element goodsGroupQuantity = goodsGroupInformation.getChild("GoodsGroupQuantity", gns);
                        String uomName = goodsGroupQuantity.getChildText("MeasureUnitQualifierName", cu);
                        String uomCode = goodsGroupQuantity.getChildText("MeasureUnitQualifierCode", cu);

                        row.add(number);
                        row.add(description);
                        row.add(sum);
                        row.add(duty);
                        row.add(vat);
                        row.add(defaultDate);
                        row.add(TNVED);
                        row.add(countryCode);
                        row.add(uomName);
                        row.add(uomCode);
                        data.add(row);
                    }

                    ImportField userNumberField = new ImportField(LM.findLCPByCompoundName("userNumberDeclarationDetail"));
                    ImportField nameCustomsField = new ImportField(LM.findLCPByCompoundName("nameCustomsDeclarationDetail"));
                    ImportField sumDataField = new ImportField(LM.findLCPByCompoundName("sumDataDeclarationDetail"));
                    ImportField sumDutyDataField = new ImportField(LM.findLCPByCompoundName("sumDutyDataDeclarationDetail"));
                    ImportField sumVATDataField = new ImportField(LM.findLCPByCompoundName("sumVATDataDeclarationDetail"));
                    ImportField dateField = new ImportField(DateClass.instance);
                    ImportField codeCustomsGroupField = new ImportField(LM.findLCPByCompoundName("codeCustomsGroupDeclarationDetail"));
                    ImportField sidOrigin2CountryField = new ImportField(LM.findLCPByCompoundName("sidOrigin2CountryDeclarationDetail"));
                    ImportField nameUOMField = new ImportField(LM.findLCPByCompoundName("nameUOM"));
                    ImportField UOMIDField = new ImportField(LM.findLCPByCompoundName("idUOM"));

                    List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

                    ImportKey<?> declarationDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("declarationDetail"),
                            LM.findLCPByCompoundName("declarationDetailUserNumberNameCustoms").getMapping(userNumberField, nameCustomsField));

                    ImportKey<?> customsGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("customsGroup"),
                            LM.findLCPByCompoundName("customsGroupCode").getMapping(codeCustomsGroupField));

                    ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("country"),
                            LM.findLCPByCompoundName("countrySIDOrigin2").getMapping(sidOrigin2CountryField));

                    ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UOM"),
                            LM.findLCPByCompoundName("UOMId").getMapping(UOMIDField));

                    properties.add(new ImportProperty(userNumberField, LM.findLCPByCompoundName("userNumberDeclarationDetail").getMapping(declarationDetailKey)));
                    properties.add(new ImportProperty(nameCustomsField, LM.findLCPByCompoundName("nameCustomsDeclarationDetail").getMapping(declarationDetailKey)));
                    properties.add(new ImportProperty(sumDataField, LM.findLCPByCompoundName("sumDataDeclarationDetail").getMapping(declarationDetailKey)));
                    properties.add(new ImportProperty(sumDutyDataField, LM.findLCPByCompoundName("sumDutyDataDeclarationDetail").getMapping(declarationDetailKey)));
                    properties.add(new ImportProperty(sumVATDataField, LM.findLCPByCompoundName("sumVATDataDeclarationDetail").getMapping(declarationDetailKey)));
                    properties.add(new ImportProperty(declaration, LM.findLCPByCompoundName("declarationDeclarationDetail").getMapping(declarationDetailKey)));

                    properties.add(new ImportProperty(nameUOMField, LM.findLCPByCompoundName("nameUOM").getMapping(UOMKey)));
                    properties.add(new ImportProperty(nameUOMField, LM.findLCPByCompoundName("shortName").getMapping(UOMKey)));
                    properties.add(new ImportProperty(UOMIDField, LM.findLCPByCompoundName("idUOM").getMapping(UOMKey)));
                    properties.add(new ImportProperty(UOMIDField, getLCP("UOMDeclarationDetail").getMapping(declarationDetailKey),
                            LM.object(getClass("UOM")).getMapping(UOMKey)));

                    properties.add(new ImportProperty(codeCustomsGroupField, getLCP("codeCustomsGroup").getMapping(customsGroupKey)));
                    properties.add(new ImportProperty(codeCustomsGroupField, getLCP("customsGroupDeclarationDetail").getMapping(declarationDetailKey),
                            LM.object(getClass("CustomsGroup")).getMapping(customsGroupKey)));
                    properties.add(new ImportProperty(sidOrigin2CountryField, LM.findLCPByCompoundName("sidOrigin2CountryDeclarationDetail").getMapping(declarationDetailKey)));
                    properties.add(new ImportProperty(sidOrigin2CountryField, LM.findLCPByCompoundName("countryDeclarationDetail").getMapping(declarationDetailKey),
                            LM.object(getClass("Country")).getMapping(countryKey)));

                    List<ImportField> fields = BaseUtils.toList(userNumberField, nameCustomsField, sumDataField,
                            sumDutyDataField, sumVATDataField, dateField, codeCustomsGroupField, sidOrigin2CountryField,
                            nameUOMField, UOMIDField);
                    ImportKey<?>[] keysArray = new ImportKey<?>[]{declarationDetailKey, customsGroupKey, countryKey, UOMKey};

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
