package roman;

import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.fields.*;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.interop.action.ExportFileClientAction;
import platform.server.auth.PolicyManager;
import platform.server.classes.ValueClass;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.UserActionProperty;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class DeclarationExportActionProperty extends UserActionProperty {
    private RomanBusinessLogics BL;
    private DBFExporter.CustomDBF dbfDecl02, dbfDobl, dbfG313, dbfG44, dbfG47;
    private DBFExporter.CustomDBF dbfG18, dbfG20, dbfG21, dbfG316, dbfG40, dbfGB;
    private File tempDecl02, tempDobl, tempG313, tempG44, tempG47, tempG18, tempG20, tempG21, tempG316, tempG40, tempGB;

    public DeclarationExportActionProperty(String sID, String caption, RomanBusinessLogics BL, ValueClass importer, ValueClass freight) {
        super(sID, caption, new ValueClass[]{importer, freight});
        this.BL = BL;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DeclarationExporter exporter = new DeclarationExporter(context.getKeys());
            exporter.extractData();

            Map<String, byte[]> files = new HashMap<String, byte[]>();
            files.put("DECL02.DBF", IOUtils.getFileBytes(dbfDecl02.getFFile()));
            files.put("DOBL.DBF", IOUtils.getFileBytes(dbfDobl.getFFile()));
            files.put("G313.DBF", IOUtils.getFileBytes(dbfG313.getFFile()));
            files.put("G44.DBF", IOUtils.getFileBytes(dbfG44.getFFile()));
            files.put("G47.DBF", IOUtils.getFileBytes(dbfG47.getFFile()));
            files.put("G18.DBF", IOUtils.getFileBytes(dbfG18.getFFile()));
            files.put("G20.DBF", IOUtils.getFileBytes(dbfG20.getFFile()));
            files.put("G21.DBF", IOUtils.getFileBytes(dbfG21.getFFile()));
            files.put("G316.DBF", IOUtils.getFileBytes(dbfG316.getFFile()));
            files.put("G40.DBF", IOUtils.getFileBytes(dbfG40.getFFile()));
            files.put("GB.DBF", IOUtils.getFileBytes(dbfGB.getFFile()));
            context.delayUserInterfaction(new ExportFileClientAction(files));

            tempDecl02.delete();
            tempDobl.delete();
            tempG18.delete();
            tempG20.delete();
            tempG21.delete();
            tempG313.delete();
            tempG316.delete();
            tempG40.delete();
            tempG44.delete();
            tempG47.delete();
            tempGB.delete();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (xBaseJException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class DeclarationExporter extends DBFExporter{
        DataObject importerDO;
        DataObject freightDO;
        FormData data;
        Map<Field, PropertyDrawInstance> map;

        public DeclarationExporter(ImMap<ClassPropertyInterface, DataObject> keys) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, IOException, xBaseJException {
            super(keys);
            createDBFFiles();

            ImOrderSet<ClassPropertyInterface> interfacesList = getOrderInterfaces();
            importerDO = keys.get(interfacesList.get(0));
            freightDO = keys.get(interfacesList.get(1));
        }

        public void getPropertyDraws() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
            session = BL.createSession();
            map = new HashMap<Field, PropertyDrawInstance>();
            FormInstance formInstance = new FormInstance(BL.RomanLM.invoiceFromFormEntity, BL, session, PolicyManager.serverSecurityPolicy, null, null, new DataObject(BL.getServerComputer(), BL.LM.computer));
            ObjectInstance importerObj = formInstance.instanceFactory.getInstance(BL.RomanLM.invoiceFromFormEntity.objImporter);
            ObjectInstance freightObj = formInstance.instanceFactory.getInstance(BL.RomanLM.invoiceFromFormEntity.objFreight);
            ObjectInstance articleObj = formInstance.instanceFactory.getInstance(BL.RomanLM.invoiceFromFormEntity.objArticle);
            ObjectInstance compositionObj = formInstance.instanceFactory.getInstance(BL.RomanLM.invoiceFromFormEntity.objComposition);
            ObjectInstance countryObj = formInstance.instanceFactory.getInstance(BL.RomanLM.invoiceFromFormEntity.objCountry);
            ObjectInstance categoryObj = formInstance.instanceFactory.getInstance(BL.RomanLM.invoiceFromFormEntity.objCategory);

            map.put(G15Decl, formInstance.getPropertyDraw(BL.LM.name, countryObj.groupTo));
            map.put(G33Decl, formInstance.getPropertyDraw(BL.RomanLM.sidCustomCategory10));
            map.put(G33Dobl, formInstance.getPropertyDraw(BL.RomanLM.sidCustomCategory10));
            map.put(G542Decl, formInstance.getPropertyDraw(BL.RomanLM.date, freightObj.groupTo));
            map.put(G542Dobl, formInstance.getPropertyDraw(BL.RomanLM.date, freightObj.groupTo));
            map.put(G082Decl, formInstance.getPropertyDraw(BL.LM.name, importerObj.groupTo));
            map.put(G142Decl, formInstance.getPropertyDraw(BL.LM.name, importerObj.groupTo));
            map.put(G092Decl, formInstance.getPropertyDraw(BL.LM.name, importerObj.groupTo));
            map.put(G083Decl, formInstance.getPropertyDraw(BL.RomanLM.addressSubject));
            map.put(G143Decl, formInstance.getPropertyDraw(BL.RomanLM.addressSubject));
            map.put(G093Decl, formInstance.getPropertyDraw(BL.RomanLM.addressSubject));
            map.put(G142Dobl, formInstance.getPropertyDraw(BL.LM.name, importerObj.groupTo));
            map.put(G143Dobl, formInstance.getPropertyDraw(BL.RomanLM.addressSubject));
            map.put(G312Decl, formInstance.getPropertyDraw(BL.RomanLM.sidArticle));
            map.put(G312Dobl, formInstance.getPropertyDraw(BL.RomanLM.sidArticle));
            map.put(G022IDecl, formInstance.getPropertyDraw(BL.RomanLM.nameExporterFreight));
            map.put(G023IDecl, formInstance.getPropertyDraw(BL.RomanLM.addressExporterFreight));
            map.put(G41Decl, formInstance.getPropertyDraw(BL.RomanLM.quantityImporterFreightArticleCompositionCountryCategory));
            map.put(G41Dobl, formInstance.getPropertyDraw(BL.RomanLM.quantityImporterFreightArticleCompositionCountryCategory));
            map.put(G315ADecl, formInstance.getPropertyDraw(BL.RomanLM.quantityImporterFreightArticleCompositionCountryCategory));
            map.put(G315ADobl, formInstance.getPropertyDraw(BL.RomanLM.quantityImporterFreightArticleCompositionCountryCategory));
            map.put(G315BDecl, formInstance.getPropertyDraw(BL.RomanLM.netWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G315BDobl, formInstance.getPropertyDraw(BL.RomanLM.netWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G38Decl, formInstance.getPropertyDraw(BL.RomanLM.netWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G38Dobl, formInstance.getPropertyDraw(BL.RomanLM.netWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G38ADecl, formInstance.getPropertyDraw(BL.RomanLM.netWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G38ADobl, formInstance.getPropertyDraw(BL.RomanLM.netWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G35Decl, formInstance.getPropertyDraw(BL.RomanLM.grossWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G35Dobl, formInstance.getPropertyDraw(BL.RomanLM.grossWeightImporterFreightArticleCompositionCountryCategory));
            map.put(G317ADecl, formInstance.getPropertyDraw(BL.RomanLM.nameUnitOfMeasureArticle));
            map.put(G317ADobl, formInstance.getPropertyDraw(BL.RomanLM.nameUnitOfMeasureArticle));
            map.put(G41BDecl, formInstance.getPropertyDraw(BL.RomanLM.nameUnitOfMeasureArticle));
            map.put(G41BDobl, formInstance.getPropertyDraw(BL.RomanLM.nameUnitOfMeasureArticle));

            map.put(G31_NTG313, formInstance.getPropertyDraw(BL.RomanLM.sidArticle));
            map.put(G31_FIRMAG313, formInstance.getPropertyDraw(BL.RomanLM.nameExporterFreight));
            map.put(G31_KTG313, formInstance.getPropertyDraw(BL.RomanLM.quantityImporterFreightArticleCompositionCountryCategory));
            map.put(G31_EIG313, formInstance.getPropertyDraw(BL.RomanLM.nameUnitOfMeasureArticle));

            importerObj.changeValue(session, importerDO);
            freightObj.changeValue(session, freightDO);

            data = formInstance.getFormData(map.values(), BaseUtils.toSet(articleObj.groupTo));
        }

        public void extractData() throws SQLException, xBaseJException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
            getPropertyDraws();
            for (FormRow row : data.rows) {
                int index = data.rows.indexOf(row) + 1;
                if (index == 1) {
                    writeDecl02Data(row, index);
                } else {
                    writeDoblData(row, index);
                }
                writeG313Data(row, index);
                writeG44Data(row);
                writeG47Data(row);
            }
            dbfDecl02.close();
            dbfDobl.close();
            dbfG313.close();
            dbfG44.close();
            dbfG47.close();
            session.close();
        }

        private void writeDecl02Data(FormRow row, int index) throws xBaseJException, IOException {
            putString(G011Decl, row.values.get(map.get(G011Decl)));
            putString(G012Decl, row.values.get(map.get(G012Decl)));
            putString(G013Decl, row.values.get(map.get(G013Decl)));
            putString(GADecl, row.values.get(map.get(GADecl)));
            putDate(G542Decl, row.values.get(map.get(G542Decl)));
            putString(N07Decl, row.values.get(map.get(N07Decl)));
            putString(N_REGDecl, row.values.get(map.get(N_REGDecl)));
            putString(GK_7Decl, row.values.get(map.get(GK_7Decl)));
            putString(G372Decl, row.values.get(map.get(G372Decl)));
            putString(G241Decl, row.values.get(map.get(G241Decl)));
            putString(G022IDecl, row.values.get(map.get(G022IDecl)));
            putString(G023IDecl, row.values.get(map.get(G023IDecl)));
            putString(G081Decl, row.values.get(map.get(G081Decl)));
            putString(G082Decl, row.values.get(map.get(G082Decl)));
            putString(G083Decl, row.values.get(map.get(G083Decl)));
            putString(G141Decl, row.values.get(map.get(G141Decl)));
            putString(G142Decl, row.values.get(map.get(G142Decl)));
            putString(G143Decl, row.values.get(map.get(G143Decl)));
            putDouble(G031Decl, row.values.get(map.get(G031Decl)));
            putDouble(G032Decl, row.values.get(map.get(G032Decl)));
            putDouble(G04Decl, row.values.get(map.get(G04Decl)));
            putDouble(G041Decl, row.values.get(map.get(G041Decl)));
            putDouble(G05Decl, row.values.get(map.get(G05Decl)));
            putString(G06Decl, row.values.get(map.get(G06Decl)));
            putString(G091Decl, row.values.get(map.get(G091Decl)));
            putString(G092Decl, row.values.get(map.get(G092Decl)));
            putString(G093Decl, row.values.get(map.get(G093Decl)));
            putString(G11Decl, row.values.get(map.get(G11Decl)));
            putString(G11_1Decl, row.values.get(map.get(G11_1Decl)));
            putString(G15ADecl, row.values.get(map.get(G15ADecl)));
            putString(G15A_1Decl, row.values.get(map.get(G15A_1Decl)));
            putString(G15Decl, row.values.get(map.get(G15Decl)));
            putString(G34Decl, row.values.get(map.get(G34Decl)));
            putString(G16Decl, row.values.get(map.get(G16Decl)));
            putString(G17ADecl, row.values.get(map.get(G17ADecl)));
            putString(G17A_1Decl, row.values.get(map.get(G17A_1Decl)));
            putString(G17Decl, row.values.get(map.get(G17Decl)));
            putString(G19Decl, row.values.get(map.get(G19Decl)));
            putString(G251Decl, row.values.get(map.get(G251Decl)));
            putDouble(G20NDecl, row.values.get(map.get(G20NDecl)));
            putString(G221Decl, row.values.get(map.get(G221Decl)));
            putDouble(G222Decl, row.values.get(map.get(G222Decl)));
            putString(G23ADecl, row.values.get(map.get(G23ADecl)));
            putDouble(G12Decl, row.values.get(map.get(G12Decl)));
            putDouble(G23Decl, row.values.get(map.get(G23Decl)));
            putString(G242Decl, row.values.get(map.get(G242Decl)));
            putString(G30Decl, row.values.get(map.get(G30Decl)));
            putString(G30ADecl, row.values.get(map.get(G30ADecl)));
            putString(G30_ADecl, row.values.get(map.get(G30_ADecl)));
        //    putDouble(G32Decl, row.values.get(map.get(G32Decl)));
            putDouble(G32Decl, (double) index);
            putString(G32_2Decl, row.values.get(map.get(G32_2Decl)));
            putString(G33Decl, row.values.get(map.get(G33Decl)));
            putString(G33NETARIFDecl, row.values.get(map.get(G33NETARIFDecl)));
            putString(G33INTELLDecl, row.values.get(map.get(G33INTELLDecl)));
            putString(G312Decl, row.values.get(map.get(G312Decl)));
            putDouble(G313NDecl, row.values.get(map.get(G313NDecl)));
            putString(G313TDecl, row.values.get(map.get(G313TDecl)));
            putDouble(G315B1Decl, row.values.get(map.get(G315B1Decl)));
            putString(G317B1Decl, row.values.get(map.get(G317B1Decl)));
            putString(G317B1CODEDecl, row.values.get(map.get(G317B1CODEDecl)));
            putDouble(G315BDecl, row.values.get(map.get(G315BDecl)));
            //putString(G317BDecl, row.values.get(map.get(G317BDecl)));
            putString(G317BDecl, "КГ");
            putString(G317BCODEDecl, row.values.get(map.get(G317BCODEDecl)));
            putDouble(G315ADecl, row.values.get(map.get(G315ADecl)));
            putString(G317ADecl, row.values.get(map.get(G317ADecl)));
            putString(G317ACODEDecl, row.values.get(map.get(G317ACODEDecl)));
            putString(G311Decl, row.values.get(map.get(G311Decl)));
            putString(G318Decl, row.values.get(map.get(G318Decl)));
            putDouble(G316NDecl, row.values.get(map.get(G316NDecl)));
            putString(G316TDecl, row.values.get(map.get(G316TDecl)));
            putDouble(G31AMARKDecl, row.values.get(map.get(G31AMARKDecl)));
            putString(G31USLPOSTDecl, row.values.get(map.get(G31USLPOSTDecl)));
            putString(G31USLPERDecl, row.values.get(map.get(G31USLPERDecl)));
            putString(G361Decl, row.values.get(map.get(G361Decl)));
            putString(G362Decl, row.values.get(map.get(G362Decl)));
            putString(G363Decl, row.values.get(map.get(G363Decl)));
            putString(G364Decl, row.values.get(map.get(G364Decl)));
            putString(G371Decl, row.values.get(map.get(G371Decl)));

            putString(G373Decl, row.values.get(map.get(G373Decl)));
            putDouble(G39Decl, row.values.get(map.get(G39Decl)));
            putString(G39EDIZMDecl, row.values.get(map.get(G39EDIZMDecl)));
            putString(G39CODEDIDecl, row.values.get(map.get(G39CODEDIDecl)));
            putDouble(G35Decl, row.values.get(map.get(G35Decl)));
            putDouble(G38Decl, row.values.get(map.get(G38Decl)));
            putDouble(G38ADecl, row.values.get(map.get(G38ADecl)));
            putDouble(G41Decl, row.values.get(map.get(G41Decl)));
            putString(G41BDecl, row.values.get(map.get(G41BDecl)));
            putString(G41ADecl, row.values.get(map.get(G41ADecl)));
            putDouble(G40NDecl, row.values.get(map.get(G40NDecl)));
            putString(G40TDecl, row.values.get(map.get(G40TDecl)));
            putDouble(G44NDecl, row.values.get(map.get(G44NDecl)));
            putString(G44TDecl, row.values.get(map.get(G44TDecl)));
            putDouble(G42Decl, row.values.get(map.get(G42Decl)));
            putString(G43Decl, row.values.get(map.get(G43Decl)));
            putDouble(G451Decl, row.values.get(map.get(G451Decl)));
            putDouble(G46Decl, row.values.get(map.get(G46Decl)));
            putDouble(G47NDecl, row.values.get(map.get(G47NDecl)));
            putString(G481Decl, row.values.get(map.get(G481Decl)));
            putString(G48DOCNUMDecl, row.values.get(map.get(G48DOCNUMDecl)));
            putDate(G48DOCDATEDecl, row.values.get(map.get(G48DOCDATEDecl)));
            putDate(G482Decl, row.values.get(map.get(G482Decl)));
            putDouble(GBNDecl, row.values.get(map.get(GBNDecl)));
            putString(GBTDecl, row.values.get(map.get(GBTDecl)));
            putString(G543Decl, row.values.get(map.get(G543Decl)));
            putDate(G543DATEDecl, row.values.get(map.get(G543DATEDecl)));
            putString(G547Decl, row.values.get(map.get(G547Decl)));
            putDate(G547DATEDecl, row.values.get(map.get(G547DATEDecl)));
            putString(G546Decl, row.values.get(map.get(G546Decl)));
            putString(G546PERNAMDecl, row.values.get(map.get(G546PERNAMDecl)));
            putString(G546MIDNAMDecl, row.values.get(map.get(G546MIDNAMDecl)));
            putString(G545Decl, row.values.get(map.get(G545Decl)));
            putString(G54PHONEDecl, row.values.get(map.get(G54PHONEDecl)));
            putString(G54ANUMBERDecl, row.values.get(map.get(G54ANUMBERDecl)));
            putDate(G54ADATEDecl, row.values.get(map.get(G54ADATEDecl)));
            putDate(G54AENDDATDecl, row.values.get(map.get(G54AENDDATDecl)));
            putString(G544Decl, row.values.get(map.get(G544Decl)));
            putString(G54CSERIESDecl, row.values.get(map.get(G54CSERIESDecl)));
            putString(G54CNUMBERDecl, row.values.get(map.get(G54CNUMBERDecl)));
            putString(G54CIDNUMBDecl, row.values.get(map.get(G54CIDNUMBDecl)));
            putDate(G54CDATEDecl, row.values.get(map.get(G54CDATEDecl)));
            putString(KONTRDecl, row.values.get(map.get(KONTRDecl)));
            putString(TYPE_DCLDecl, row.values.get(map.get(TYPE_DCLDecl)));
            putString(NOMER_GTDDecl, row.values.get(map.get(NOMER_GTDDecl)));
            putString(DOP_NOMERDecl, row.values.get(map.get(DOP_NOMERDecl)));
            putString(NAPRAVLDecl, row.values.get(map.get(NAPRAVLDecl)));
            putString(P_ARXDecl, row.values.get(map.get(P_ARXDecl)));
            putString(OF_DOCDecl, row.values.get(map.get(OF_DOCDecl)));
            putString(G33_PDecl, row.values.get(map.get(G33_PDecl)));
            putDouble(P_OK_EXPDecl, row.values.get(map.get(P_OK_EXPDecl)));
            putString(PR_REPORTDecl, row.values.get(map.get(PR_REPORTDecl)));
            putDouble(TR_RDecl, row.values.get(map.get(TR_RDecl)));
            putDouble(TR_G46Decl, row.values.get(map.get(TR_G46Decl)));
            putDouble(R_STRDecl, row.values.get(map.get(R_STRDecl)));
            putDouble(R_FINDecl, row.values.get(map.get(R_FINDecl)));
            putDouble(R_POGRDecl, row.values.get(map.get(R_POGRDecl)));
            putString(R_SUMMADecl, row.values.get(map.get(R_SUMMADecl)));
            putString(G023IPOSTADecl, row.values.get(map.get(G023IPOSTADecl)));
            putString(G023ICODSTDecl, row.values.get(map.get(G023ICODSTDecl)));
            putString(G023INAMSTDecl, row.values.get(map.get(G023INAMSTDecl)));
            putString(G023IREGIODecl, row.values.get(map.get(G023IREGIODecl)));
            putString(G023ICITYDecl, row.values.get(map.get(G023ICITYDecl)));
            putString(G023ISTREEDecl, row.values.get(map.get(G023ISTREEDecl)));
            putString(G083POSTALDecl, row.values.get(map.get(G083POSTALDecl)));
            putString(G083CODSTRDecl, row.values.get(map.get(G083CODSTRDecl)));
            putString(G083NAMSTRDecl, row.values.get(map.get(G083NAMSTRDecl)));
            putString(G083REGIONDecl, row.values.get(map.get(G083REGIONDecl)));
            putString(G083CITYDecl, row.values.get(map.get(G083CITYDecl)));
            putString(G083STREETDecl, row.values.get(map.get(G083STREETDecl)));
            putString(G093POSTALDecl, row.values.get(map.get(G093POSTALDecl)));
            putString(G093CODSTRDecl, row.values.get(map.get(G093CODSTRDecl)));
            putString(G093NAMSTRDecl, row.values.get(map.get(G093NAMSTRDecl)));
            putString(G093REGIONDecl, row.values.get(map.get(G093REGIONDecl)));
            putString(G093CITYDecl, row.values.get(map.get(G093CITYDecl)));
            putString(G093STREETDecl, row.values.get(map.get(G093STREETDecl)));
            putString(G143POSTALDecl, row.values.get(map.get(G143POSTALDecl)));
            putString(G143CODSTRDecl, row.values.get(map.get(G143CODSTRDecl)));
            putString(G143NAMSTRDecl, row.values.get(map.get(G143NAMSTRDecl)));
            putString(G143REGIONDecl, row.values.get(map.get(G143REGIONDecl)));
            putString(G143CITYDecl, row.values.get(map.get(G143CITYDecl)));
            putString(G143STREETDecl, row.values.get(map.get(G143STREETDecl)));
            putString(G30_APOSTADecl, row.values.get(map.get(G30_APOSTADecl)));
            putString(G30_ACODSTDecl, row.values.get(map.get(G30_ACODSTDecl)));
            putString(G30_ANAMSTDecl, row.values.get(map.get(G30_ANAMSTDecl)));
            putString(G30_AREGIODecl, row.values.get(map.get(G30_AREGIODecl)));
            putString(G30_ACITYDecl, row.values.get(map.get(G30_ACITYDecl)));
            putString(G30_ASTREEDecl, row.values.get(map.get(G30_ASTREEDecl)));
            putString(G31RATEOUTDecl, row.values.get(map.get(G31RATEOUTDecl)));
            putString(G31DOCNUMBDecl, row.values.get(map.get(G31DOCNUMBDecl)));
            putDate(G31DOCDATEDecl, row.values.get(map.get(G31DOCDATEDecl)));
            putString(G31DESCRIPDecl, row.values.get(map.get(G31DESCRIPDecl)));
            putString(G31TNVEDDecl, row.values.get(map.get(G31TNVEDDecl)));
            putDouble(G31QUANTDecl, row.values.get(map.get(G31QUANTDecl)));
            putString(G31EDIZMDecl, row.values.get(map.get(G31EDIZMDecl)));
            putString(G31CODIZMDecl, row.values.get(map.get(G31CODIZMDecl)));
            putString(G31OPERATDecl, row.values.get(map.get(G31OPERATDecl)));
            putString(G31METODDecl, row.values.get(map.get(G31METODDecl)));

            dbfDecl02.write();
        }

        private void writeDoblData(FormRow row, int index) throws xBaseJException, IOException {
            putString(G011Dobl, row.values.get(map.get(G011Dobl)));
            putString(G012Dobl, row.values.get(map.get(G012Dobl)));
            putString(G013Dobl, row.values.get(map.get(G013Dobl)));
            putString(GADobl, row.values.get(map.get(GADobl)));
            putDate(G542Dobl, row.values.get(map.get(G542Dobl)));
            putString(N07Dobl, row.values.get(map.get(N07Dobl)));
            putString(N_REGDobl, row.values.get(map.get(N_REGDobl)));
            putString(G141Dobl, row.values.get(map.get(G141Dobl)));
            putString(G142Dobl, row.values.get(map.get(G142Dobl)));
            putString(G143Dobl, row.values.get(map.get(G143Dobl)));
            putDouble(G031Dobl, row.values.get(map.get(G031Dobl)));
            putDouble(G032Dobl, row.values.get(map.get(G032Dobl)));
        //    putDouble(G32Dobl, row.values.get(map.get(G32Dobl)));
            putDouble(G32Dobl, (double) index);
            putString(G32_2Dobl, row.values.get(map.get(G32_2Dobl)));
            putString(G33Dobl, row.values.get(map.get(G33Dobl)));
            putString(G33NETARIFDobl, row.values.get(map.get(G33NETARIFDobl)));
            putString(G33INTELLDobl, row.values.get(map.get(G33INTELLDobl)));
            putString(G312Dobl, row.values.get(map.get(G312Dobl)));
            putDouble(G313NDobl, row.values.get(map.get(G313NDobl)));
            putString(G313TDobl, row.values.get(map.get(G313TDobl)));
            putDouble(G315B1Dobl, row.values.get(map.get(G315B1Dobl)));
            putString(G317B1Dobl, row.values.get(map.get(G317B1Dobl)));
            putString(G317B1CODEDobl, row.values.get(map.get(G317B1CODEDobl)));
            putDouble(G315BDobl, row.values.get(map.get(G315BDobl)));
            //putString(G317BDobl, row.values.get(map.get(G317BDobl)));
            putString(G317BDobl, "КГ");
            putString(G317BCODEDobl, row.values.get(map.get(G317BCODEDobl)));
            putDouble(G315ADobl, row.values.get(map.get(G315ADobl)));
            putString(G317ADobl, row.values.get(map.get(G317ADobl)));
            putString(G317ACODEDobl, row.values.get(map.get(G317ACODEDobl)));
            putString(G311Dobl, row.values.get(map.get(G311Dobl)));
            putString(G318Dobl, row.values.get(map.get(G318Dobl)));
            putDouble(G316NDobl, row.values.get(map.get(G316NDobl)));
            putString(G316TDobl, row.values.get(map.get(G316TDobl)));
            putDouble(G31AMARKDobl, row.values.get(map.get(G31AMARKDobl)));
            putString(G31USLPOSTDobl, row.values.get(map.get(G31USLPOSTDobl)));
            putString(G31USLPERDobl, row.values.get(map.get(G31USLPERDobl)));
            putString(G34Dobl, row.values.get(map.get(G34Dobl)));
            putString(G361Dobl, row.values.get(map.get(G361Dobl)));
            putString(G362Dobl, row.values.get(map.get(G362Dobl)));
            putString(G363Dobl, row.values.get(map.get(G363Dobl)));
            putString(G364Dobl, row.values.get(map.get(G364Dobl)));
            putString(G371Dobl, row.values.get(map.get(G371Dobl)));
            putString(G372Dobl, row.values.get(map.get(G372Dobl)));
            putString(G373Dobl, row.values.get(map.get(G373Dobl)));
            putDouble(G39Dobl, row.values.get(map.get(G39Dobl)));
            putString(G39EDIZMDobl, row.values.get(map.get(G39EDIZMDobl)));
            putString(G39CODEDIDobl, row.values.get(map.get(G39CODEDIDobl)));
            putDouble(G35Dobl, row.values.get(map.get(G35Dobl)));
            putDouble(G38Dobl, row.values.get(map.get(G38Dobl)));
            putDouble(G38ADobl, row.values.get(map.get(G38ADobl)));
            putDouble(G41Dobl, row.values.get(map.get(G41Dobl)));
            putString(G41BDobl, row.values.get(map.get(G41BDobl)));
            putString(G41ADobl, row.values.get(map.get(G41ADobl)));
            putDouble(G40NDobl, row.values.get(map.get(G40NDobl)));
            putString(G40TDobl, row.values.get(map.get(G40TDobl)));
            putDouble(G44NDobl, row.values.get(map.get(G44NDobl)));
            putString(G44TDobl, row.values.get(map.get(G44TDobl)));
            putDouble(G42Dobl, row.values.get(map.get(G42Dobl)));
            putString(G43Dobl, row.values.get(map.get(G43Dobl)));
            putDouble(G451Dobl, row.values.get(map.get(G451Dobl)));
            putDouble(G46Dobl, row.values.get(map.get(G46Dobl)));
            putDouble(G47NDobl, row.values.get(map.get(G47NDobl)));
            putString(KONTRDobl, row.values.get(map.get(KONTRDobl)));
            putString(TYPE_DCLDobl, row.values.get(map.get(TYPE_DCLDobl)));
            putString(NOMER_GTDDobl, row.values.get(map.get(NOMER_GTDDobl)));
            putString(DOP_NOMERDobl, row.values.get(map.get(DOP_NOMERDobl)));
            putString(OF_DOCDobl, row.values.get(map.get(OF_DOCDobl)));
            putString(G33_PDobl, row.values.get(map.get(G33_PDobl)));
            putDouble(TR_RDobl, row.values.get(map.get(TR_RDobl)));
            putDouble(TR_G46Dobl, row.values.get(map.get(TR_G46Dobl)));
            putDouble(R_STRDobl, row.values.get(map.get(R_STRDobl)));
            putDouble(R_FINDobl, row.values.get(map.get(R_FINDobl)));
            putDouble(R_POGRDobl, row.values.get(map.get(R_POGRDobl)));
            putString(G31RATEOUTDobl, row.values.get(map.get(G31RATEOUTDobl)));
            putString(G31DOCNUMBDobl, row.values.get(map.get(G31DOCNUMBDobl)));
            putDate(G31DOCDATEDobl, row.values.get(map.get(G31DOCDATEDobl)));
            putString(G31DESCRIPDobl, row.values.get(map.get(G31DESCRIPDobl)));
            putString(G31TNVEDDobl, row.values.get(map.get(G31TNVEDDobl)));
            putDouble(G31QUANTDobl, row.values.get(map.get(G31QUANTDobl)));
            putString(G31EDIZMDobl, row.values.get(map.get(G31EDIZMDobl)));
            putString(G31CODIZMDobl, row.values.get(map.get(G31CODIZMDobl)));
            putString(G31OPERATDobl, row.values.get(map.get(G31OPERATDobl)));
            putString(G31METODDobl, row.values.get(map.get(G31METODDobl)));
            dbfDobl.write();
        }

        private void writeG313Data(FormRow row, int index) throws IOException, xBaseJException {
        //    putDouble(G32G313, row.values.get(map.get(G32G313)));
            putDouble(G32G313, (double) index);
            putString(G31_NTG313, row.values.get(map.get(G31_NTG313)));
            putString(G31_MTG313, row.values.get(map.get(G31_MTG313)));
            putString(G31_MODELG313, row.values.get(map.get(G31_MODELG313)));
            putString(G31_MARKING313, row.values.get(map.get(G31_MARKING313)));
            putString(G31_MARKG313, row.values.get(map.get(G31_MARKG313)));
            putString(G31_STANDG313, row.values.get(map.get(G31_STANDG313)));
            putDate(G31_DATEG313, row.values.get(map.get(G31_DATEG313)));
            putString(G31_FIRMAG313, row.values.get(map.get(G31_FIRMAG313)));
            putString(G31_SORTG313, row.values.get(map.get(G31_SORTG313)));
            putString(G31_SORTIMG313, row.values.get(map.get(G31_SORTIMG313)));
            putString(G31_KINDG313, row.values.get(map.get(G31_KINDG313)));
            putString(G31_DIMENSG313, row.values.get(map.get(G31_DIMENSG313)));
            putString(G31AMODELG313, row.values.get(map.get(G31AMODELG313)));
            putString(G31AMARKG313, row.values.get(map.get(G31AMARKG313)));
            putString(G31AYEARG313, row.values.get(map.get(G31AYEARG313)));
            putDouble(G31VOLUMEG313, row.values.get(map.get(G31VOLUMEG313)));
            putString(G31VINIDG313, row.values.get(map.get(G31VINIDG313)));
            putString(G31BODYIDG313, row.values.get(map.get(G31BODYIDG313)));
            putString(G31ENGINIDG313, row.values.get(map.get(G31ENGINIDG313)));
            putString(G31CHASSIDG313, row.values.get(map.get(G31CHASSIDG313)));
            putString(G31CABIDG313, row.values.get(map.get(G31CABIDG313)));
            putString(G31IDCNUMBG313, row.values.get(map.get(G31IDCNUMBG313)));
            putDouble(G31POWERG313, row.values.get(map.get(G31POWERG313)));
            putDouble(G31KILOMG313, row.values.get(map.get(G31KILOMG313)));
            putDouble(G31_KTG313, row.values.get(map.get(G31_KTG313)));
            putString(G31_EIG313, row.values.get(map.get(G31_EIG313)));
            putString(G31_CODIZMG313, row.values.get(map.get(G31_CODIZMG313)));
            putString(G31_TEGG313, row.values.get(map.get(G31_TEGG313)));
            putDouble(G313IG313, row.values.get(map.get(G313IG313)));
            putString(DOP_NOMERG313, row.values.get(map.get(DOP_NOMERG313)));
            putString(NOMER_GTDG313, row.values.get(map.get(NOMER_GTDG313)));
            dbfG313.write();
        }

        private void writeG44Data(FormRow row) throws xBaseJException, IOException {
            putDouble(G32G44, row.values.get(map.get(G32G44)));
            putDouble(G44IG44, row.values.get(map.get(G44IG44)));
            putString(G44KDG44, row.values.get(map.get(G44KDG44)));
            putString(G44NDG44, row.values.get(map.get(G44NDG44)));
            putDate(G44DDG44, row.values.get(map.get(G44DDG44)));
            putString(G44NSG44, row.values.get(map.get(G44NSG44)));
            putDate(G44DSG44, row.values.get(map.get(G44DSG44)));
            putDate(G44BEGDATEG44, row.values.get(map.get(G44BEGDATEG44)));
            putDate(G44ENDDATEG44, row.values.get(map.get(G44ENDDATEG44)));
            putString(G44CODESTRG44, row.values.get(map.get(G44CODESTRG44)));
            putString(G44VIDPLATG44, row.values.get(map.get(G44VIDPLATG44)));
            putString(G44CODSROKG44, row.values.get(map.get(G44CODSROKG44)));
            putDate(G44DATEIMPG44, row.values.get(map.get(G44DATEIMPG44)));
            putString(G44SPECIALG44, row.values.get(map.get(G44SPECIALG44)));
            putString(G44STATUSG44, row.values.get(map.get(G44STATUSG44)));
            putString(NOMER_GTDG44, row.values.get(map.get(NOMER_GTDG44)));
            putString(DOP_NOMERG44, row.values.get(map.get(DOP_NOMERG44)));
            dbfG44.write();
        }

        private void writeG47Data(FormRow row) throws IOException, xBaseJException {
            putDouble(G32G47, row.values.get(map.get(G32G47)));
            putDouble(G47IG47, row.values.get(map.get(G47IG47)));
            putString(G471G47, row.values.get(map.get(G471G47)));
            putDouble(G472G47, row.values.get(map.get(G472G47)));
            putString(G473G47, row.values.get(map.get(G473G47)));
            putString(G4731G47, row.values.get(map.get(G4731G47)));
            putDouble(G474G47, row.values.get(map.get(G474G47)));
            putString(G475G47, row.values.get(map.get(G475G47)));
            putString(G474CODEG47, row.values.get(map.get(G474CODEG47)));
            putDate(G47RATEDATG47, row.values.get(map.get(G47RATEDATG47)));
            putString(G47TYPECODG47, row.values.get(map.get(G47TYPECODG47)));
            putString(G47CODIZMG47, row.values.get(map.get(G47CODIZMG47)));
            putString(G47CODVALG47, row.values.get(map.get(G47CODVALG47)));
            putDouble(G47WEIGHG47, row.values.get(map.get(G47WEIGHG47)));
            putDouble(G47NUMDAYSG47, row.values.get(map.get(G47NUMDAYSG47)));
            putDouble(G47NUMETAPG47, row.values.get(map.get(G47NUMETAPG47)));
            putDouble(G47NUMMONTG47, row.values.get(map.get(G47NUMMONTG47)));
            putDouble(G47LINENUMG47, row.values.get(map.get(G47LINENUMG47)));
            putDouble(G47TARRATEG47, row.values.get(map.get(G47TARRATEG47)));
            putLogical(PR_OSPLATG47, row.values.get(map.get(PR_OSPLATG47)));
            putString(KOD_SIG47, row.values.get(map.get(KOD_SIG47)));
            putString(NOMER_GTDG47, row.values.get(map.get(NOMER_GTDG47)));
            putString(DOP_NOMERG47, row.values.get(map.get(DOP_NOMERG47)));
            dbfG47.write();
        }

        CharField G011Decl = new CharField("G011", 2);
        CharField G012Decl = new CharField("G012", 2);
        CharField G013Decl = new CharField("G013", 2);
        CharField GADecl = new CharField("GA", 14);
        DateField G542Decl = new DateField("G542");
        CharField N07Decl = new CharField("N07", 14);
        CharField N_REGDecl = new CharField("N_REG", 18);
        CharField GK_7Decl = new CharField("GK_7", 3);
        CharField G022IDecl = new CharField("G022I", 35);
        CharField G023IDecl = new CharField("G023I", 117);
        CharField G081Decl = new CharField("G081", 9);
        CharField G082Decl = new CharField("G082", 35);
        CharField G083Decl = new CharField("G083", 117);
        CharField G141Decl = new CharField("G141", 9);
        CharField G142Decl = new CharField("G142", 35);
        CharField G143Decl = new CharField("G143", 117);
        NumField G031Decl = new NumField("G031", 3, 0);
        NumField G032Decl = new NumField("G032", 3, 0);
        NumField G04Decl = new NumField("G04", 4, 0);
        NumField G041Decl = new NumField("G041", 4, 0);
        NumField G05Decl = new NumField("G05", 3, 0);
        CharField G06Decl = new CharField("G06", 7);
        CharField G091Decl = new CharField("G091", 9);
        CharField G092Decl = new CharField("G092", 38);
        CharField G093Decl = new CharField("G093", 117);
        CharField G11Decl = new CharField("G11", 2);
        CharField G11_1Decl = new CharField("G11_1", 4);
        CharField G15ADecl = new CharField("G15A", 2);
        CharField G15A_1Decl = new CharField("G15A_1", 4);
        CharField G15Decl = new CharField("G15", 17);
        CharField G34Decl = new CharField("G34", 2);
        CharField G16Decl = new CharField("G16", 17);
        CharField G17ADecl = new CharField("G17A", 2);
        CharField G17A_1Decl = new CharField("G17A_1", 4);
        CharField G17Decl = new CharField("G17", 17);
        CharField G19Decl = new CharField("G19", 1);
        CharField G251Decl = new CharField("G251", 2);
        NumField G20NDecl = new NumField("G20N", 2, 0);
        CharField G221Decl = new CharField("G221", 3);
        NumField G222Decl = new NumField("G222", 16, 2);
        CharField G23ADecl = new CharField("G23A", 6);
        NumField G12Decl = new NumField("G12", 18, 2);
        NumField G23Decl = new NumField("G23", 10, 2);
        CharField G241Decl = new CharField("G241", 3);
        CharField G242Decl = new CharField("G242", 2);
        CharField G30Decl = new CharField("G30", 5);
        CharField G30ADecl = new CharField("G30A", 15);
        CharField G30_ADecl = new CharField("G30_A", 37);
        NumField G32Decl = new NumField("G32", 3, 0);
        CharField G32_2Decl = new CharField("G32_2", 3);
        CharField G33Decl = new CharField("G33", 10);
        CharField G33NETARIFDecl = new CharField("G33NETARIF", 1);
        CharField G33INTELLDecl = new CharField("G33INTELL", 1);
        CharField G312Decl = new CharField("G312", 248);
        NumField G313NDecl = new NumField("G313N", 5, 0);
        CharField G313TDecl = new CharField("G313T", 20);
        NumField G315B1Decl = new NumField("G315B1", 16, 3);
        CharField G317B1Decl = new CharField("G317B1", 13);
        CharField G317B1CODEDecl = new CharField("G317B1CODE", 3);
        NumField G315BDecl = new NumField("G315B", 16, 3);
        CharField G317BDecl = new CharField("G317B", 13);
        CharField G317BCODEDecl = new CharField("G317BCODE", 3);
        NumField G315ADecl = new NumField("G315A", 16, 3);
        CharField G317ADecl = new CharField("G317A", 13);
        CharField G317ACODEDecl = new CharField("G317ACODE", 3);
        CharField G311Decl = new CharField("G311", 9);
        CharField G318Decl = new CharField("G318", 14);
        NumField G316NDecl = new NumField("G316N", 2, 0);
        CharField G316TDecl = new CharField("G316T", 55);
        NumField G31AMARKDecl = new NumField("G31AMARK", 8, 0);
        CharField G31USLPOSTDecl = new CharField("G31USLPOST", 58);
        CharField G31USLPERDecl = new CharField("G31USLPER", 9);
        CharField G361Decl = new CharField("G361", 2);
        CharField G362Decl = new CharField("G362", 2);
        CharField G363Decl = new CharField("G363", 1);
        CharField G364Decl = new CharField("G364", 2);
        CharField G371Decl = new CharField("G371", 2);
        CharField G372Decl = new CharField("G372", 2);
        CharField G373Decl = new CharField("G373", 3);
        NumField G39Decl = new NumField("G39", 16, 3);
        CharField G39EDIZMDecl = new CharField("G39EDIZM", 13);
        CharField G39CODEDIDecl = new CharField("G39CODEDI", 3);
        NumField G35Decl = new NumField("G35", 15, 3);
        NumField G38Decl = new NumField("G38", 15, 3);
        NumField G38ADecl = new NumField("G38A", 15, 3);
        NumField G41Decl = new NumField("G41", 16, 3);
        CharField G41BDecl = new CharField("G41B", 13);
        CharField G41ADecl = new CharField("G41A", 3);
        NumField G40NDecl = new NumField("G40N", 3, 0);
        CharField G40TDecl = new CharField("G40T", 50);
        NumField G44NDecl = new NumField("G44N", 3, 0);
        CharField G44TDecl = new CharField("G44T", 40);
        NumField G42Decl = new NumField("G42", 16, 2);
        CharField G43Decl = new CharField("G43", 1);
        NumField G451Decl = new NumField("G451", 17, 2);
        NumField G46Decl = new NumField("G46", 13, 2);
        NumField G47NDecl = new NumField("G47N", 2, 0);
        CharField G481Decl = new CharField("G481", 4);
        CharField G48DOCNUMDecl = new CharField("G48DOCNUM", 25);
        DateField G48DOCDATEDecl = new DateField("G48DOCDATE");
        DateField G482Decl = new DateField("G482");
        NumField GBNDecl = new NumField("GBN", 2, 0);
        CharField GBTDecl = new CharField("GBT", 94);
        CharField G543Decl = new CharField("G543", 35);
        DateField G543DATEDecl = new DateField("G543DATE");
        CharField G547Decl = new CharField("G547", 35);
        DateField G547DATEDecl = new DateField("G547DATE");
        CharField G546Decl = new CharField("G546", 40);
        CharField G546PERNAMDecl = new CharField("G546PERNAM", 22);
        CharField G546MIDNAMDecl = new CharField("G546MIDNAM", 23);
        CharField G545Decl = new CharField("G545", 46);
        CharField G54PHONEDecl = new CharField("G54PHONE", 24);
        CharField G54ANUMBERDecl = new CharField("G54ANUMBER", 35);
        DateField G54ADATEDecl = new DateField("G54ADATE");
        DateField G54AENDDATDecl = new DateField("G54AENDDAT");
        CharField G544Decl = new CharField("G544", 30);
        CharField G54CSERIESDecl = new CharField("G54CSERIES", 8);
        CharField G54CNUMBERDecl = new CharField("G54CNUMBER", 15);
        CharField G54CIDNUMBDecl = new CharField("G54CIDNUMB", 14);
        DateField G54CDATEDecl = new DateField("G54CDATE");
        CharField KONTRDecl = new CharField("KONTR", 3);
        CharField TYPE_DCLDecl = new CharField("TYPE_DCL", 2);
        CharField NOMER_GTDDecl = new CharField("NOMER_GTD", 6);
        CharField DOP_NOMERDecl = new CharField("DOP_NOMER", 8);
        CharField NAPRAVLDecl = new CharField("NAPRAVL", 1);
        CharField P_ARXDecl = new CharField("P_ARX", 1);
        CharField OF_DOCDecl = new CharField("OF_DOC", 40);
        CharField G33_PDecl = new CharField("G33_P", 2);
        NumField P_OK_EXPDecl = new NumField("P_OK_EXP", 2, 0);
        CharField PR_REPORTDecl = new CharField("PR_REPORT", 1);
        NumField TR_RDecl = new NumField("TR_R", 9, 0);
        NumField TR_G46Decl = new NumField("TR_G46", 9, 0);
        NumField R_STRDecl = new NumField("R_STR", 9, 0);
        NumField R_FINDecl = new NumField("R_FIN", 9, 0);
        NumField R_POGRDecl = new NumField("R_POGR", 9, 0);
        CharField R_SUMMADecl = new CharField("R_SUMMA", 108);
        CharField G023IPOSTADecl = new CharField("G023IPOSTA", 9);
        CharField G023ICODSTDecl = new CharField("G023ICODST", 2);
        CharField G023INAMSTDecl = new CharField("G023INAMST", 40);
        CharField G023IREGIODecl = new CharField("G023IREGIO", 50);
        CharField G023ICITYDecl = new CharField("G023ICITY", 35);
        CharField G023ISTREEDecl = new CharField("G023ISTREE", 50);
        CharField G083POSTALDecl = new CharField("G083POSTAL", 9);
        CharField G083CODSTRDecl = new CharField("G083CODSTR", 2);
        CharField G083NAMSTRDecl = new CharField("G083NAMSTR", 40);
        CharField G083REGIONDecl = new CharField("G083REGION", 50);
        CharField G083CITYDecl = new CharField("G083CITY", 35);
        CharField G083STREETDecl = new CharField("G083STREET", 50);
        CharField G093POSTALDecl = new CharField("G093POSTAL", 9);
        CharField G093CODSTRDecl = new CharField("G093CODSTR", 2);
        CharField G093NAMSTRDecl = new CharField("G093NAMSTR", 40);
        CharField G093REGIONDecl = new CharField("G093REGION", 50);
        CharField G093CITYDecl = new CharField("G093CITY", 35);
        CharField G093STREETDecl = new CharField("G093STREET", 50);
        CharField G143POSTALDecl = new CharField("G143POSTAL", 9);
        CharField G143CODSTRDecl = new CharField("G143CODSTR", 2);
        CharField G143NAMSTRDecl = new CharField("G143NAMSTR", 40);
        CharField G143REGIONDecl = new CharField("G143REGION", 50);
        CharField G143CITYDecl = new CharField("G143CITY", 35);
        CharField G143STREETDecl = new CharField("G143STREET", 50);
        CharField G30_APOSTADecl = new CharField("G30_APOSTA", 9);
        CharField G30_ACODSTDecl = new CharField("G30_ACODST", 2);
        CharField G30_ANAMSTDecl = new CharField("G30_ANAMST", 40);
        CharField G30_AREGIODecl = new CharField("G30_AREGIO", 50);
        CharField G30_ACITYDecl = new CharField("G30_ACITY", 35);
        CharField G30_ASTREEDecl = new CharField("G30_ASTREE", 50);
        CharField G31RATEOUTDecl = new CharField("G31RATEOUT", 200);
        CharField G31DOCNUMBDecl = new CharField("G31DOCNUMB", 50);
        DateField G31DOCDATEDecl = new DateField("G31DOCDATE");
        CharField G31DESCRIPDecl = new CharField("G31DESCRIP", 200);
        CharField G31TNVEDDecl = new CharField("G31TNVED", 10);
        NumField G31QUANTDecl = new NumField("G31QUANT", 16, 3);
        CharField G31EDIZMDecl = new CharField("G31EDIZM", 13);
        CharField G31CODIZMDecl = new CharField("G31CODIZM", 3);
        CharField G31OPERATDecl = new CharField("G31OPERAT", 200);
        CharField G31METODDecl = new CharField("G31METOD", 200);

        CharField G011Dobl = new CharField("G011", 2);
        CharField G012Dobl = new CharField("G012", 2);
        CharField G013Dobl = new CharField("G013", 2);
        CharField GADobl = new CharField("GA", 14);
        DateField G542Dobl = new DateField("G542");
        CharField N07Dobl = new CharField("N07", 14);
        CharField N_REGDobl = new CharField("N_REG", 18);
        CharField G141Dobl = new CharField("G141", 9);
        CharField G142Dobl = new CharField("G142", 38);
        CharField G143Dobl = new CharField("G143", 117);
        NumField G031Dobl = new NumField("G031", 3, 0);
        NumField G032Dobl = new NumField("G032", 3, 0);
        NumField G32Dobl = new NumField("G32", 3, 0);
        CharField G32_2Dobl = new CharField("G32_2", 3);
        CharField G33Dobl = new CharField("G33", 10);
        CharField G33NETARIFDobl = new CharField("G33NETARIF", 1);
        CharField G33INTELLDobl = new CharField("G33INTELL", 1);
        CharField G312Dobl = new CharField("G312", 248);
        NumField G313NDobl = new NumField("G313N", 5, 0);
        CharField G313TDobl = new CharField("G313T", 20);
        NumField G315B1Dobl = new NumField("G315B1", 16, 3);
        CharField G317B1Dobl = new CharField("G317B1", 13);
        CharField G317B1CODEDobl = new CharField("G317B1CODE", 3);
        NumField G315BDobl = new NumField("G315B", 16, 3);
        CharField G317BDobl = new CharField("G317B", 13);
        CharField G317BCODEDobl = new CharField("G317BCODE", 3);
        NumField G315ADobl = new NumField("G315A", 16, 3);
        CharField G317ADobl = new CharField("G317A", 13);
        CharField G317ACODEDobl = new CharField("G317ACODE", 3);
        CharField G311Dobl = new CharField("G311", 9);
        CharField G318Dobl = new CharField("G318", 14);
        NumField G316NDobl = new NumField("G316N", 2, 0);
        CharField G316TDobl = new CharField("G316T", 55);
        NumField G31AMARKDobl = new NumField("G31AMARK", 8, 0);
        CharField G31USLPOSTDobl = new CharField("G31USLPOST", 58);
        CharField G31USLPERDobl = new CharField("G31USLPER", 9);
        CharField G34Dobl = new CharField("G34", 2);
        CharField G361Dobl = new CharField("G361", 2);
        CharField G362Dobl = new CharField("G362", 2);
        CharField G363Dobl = new CharField("G363", 1);
        CharField G364Dobl = new CharField("G364", 2);
        CharField G371Dobl = new CharField("G371", 2);
        CharField G372Dobl = new CharField("G372", 2);
        CharField G373Dobl = new CharField("G373", 3);
        NumField G39Dobl = new NumField("G39", 16, 3);
        CharField G39EDIZMDobl = new CharField("G39EDIZM", 13);
        CharField G39CODEDIDobl = new CharField("G39CODEDI", 3);
        NumField G35Dobl = new NumField("G35", 15, 3);
        NumField G38Dobl = new NumField("G38", 15, 3);
        NumField G38ADobl = new NumField("G38A", 15, 3);
        NumField G41Dobl = new NumField("G41", 16, 3);
        CharField G41BDobl = new CharField("G41B", 13);
        CharField G41ADobl = new CharField("G41A", 3);
        NumField G40NDobl = new NumField("G40N", 3, 0);
        CharField G40TDobl = new CharField("G40T", 50);
        NumField G44NDobl = new NumField("G44N", 3, 0);
        CharField G44TDobl = new CharField("G44T", 40);
        NumField G42Dobl = new NumField("G42", 16, 2);
        CharField G43Dobl = new CharField("G43", 1);
        NumField G451Dobl = new NumField("G451", 17, 2);
        NumField G46Dobl = new NumField("G46", 13, 2);
        NumField G47NDobl = new NumField("G47N", 2, 0);
        CharField KONTRDobl = new CharField("KONTR", 3);
        CharField TYPE_DCLDobl = new CharField("TYPE_DCL", 2);
        CharField NOMER_GTDDobl = new CharField("NOMER_GTD", 6);
        CharField DOP_NOMERDobl = new CharField("DOP_NOMER", 8);
        CharField OF_DOCDobl = new CharField("OF_DOC", 40);
        CharField G33_PDobl = new CharField("G33_P", 2);
        NumField TR_RDobl = new NumField("TR_R", 9, 0);
        NumField TR_G46Dobl = new NumField("TR_G46", 9, 0);
        NumField R_STRDobl = new NumField("R_STR", 9, 0);
        NumField R_FINDobl = new NumField("R_FIN", 9, 0);
        NumField R_POGRDobl = new NumField("R_POGR", 9, 0);
        CharField G31RATEOUTDobl = new CharField("G31RATEOUT", 200);
        CharField G31DOCNUMBDobl = new CharField("G31DOCNUMB", 50);
        DateField G31DOCDATEDobl = new DateField("G31DOCDATE");
        CharField G31DESCRIPDobl = new CharField("G31DESCRIP", 200);
        CharField G31TNVEDDobl = new CharField("G31TNVED", 10);
        NumField G31QUANTDobl = new NumField("G31QUANT", 16, 3);
        CharField G31EDIZMDobl = new CharField("G31EDIZM", 13);
        CharField G31CODIZMDobl = new CharField("G31CODIZM", 3);
        CharField G31OPERATDobl = new CharField("G31OPERAT", 200);
        CharField G31METODDobl = new CharField("G31METOD", 200);

        NumField G32G313 = new NumField("G32", 3, 0);
        CharField G31_NTG313 = new CharField("G31_NT", 250);
        CharField G31_MTG313 = new CharField("G31_MT", 150);
        CharField G31_MODELG313 = new CharField("G31_MODEL", 50);
        CharField G31_MARKING313 = new CharField("G31_MARKIN", 50);
        CharField G31_MARKG313 = new CharField("G31_MARK", 100);
        CharField G31_STANDG313 = new CharField("G31_STAND", 50);
        DateField G31_DATEG313 = new DateField("G31_DATE");
        CharField G31_FIRMAG313 = new CharField("G31_FIRMA", 80);
        CharField G31_SORTG313 = new CharField("G31_SORT", 50);
        CharField G31_SORTIMG313 = new CharField("G31_SORTIM", 30);
        CharField G31_KINDG313 = new CharField("G31_KIND", 20);
        CharField G31_DIMENSG313 = new CharField("G31_DIMENS", 50);
        CharField G31AMODELG313 = new CharField("G31AMODEL", 60);
        CharField G31AMARKG313 = new CharField("G31AMARK", 20);
        CharField G31AYEARG313 = new CharField("G31AYEAR", 4);
        NumField G31VOLUMEG313 = new NumField("G31VOLUME", 6, 0);
        CharField G31VINIDG313 = new CharField("G31VINID", 40);
        CharField G31BODYIDG313 = new CharField("G31BODYID", 40);
        CharField G31ENGINIDG313 = new CharField("G31ENGINID", 40);
        CharField G31CHASSIDG313 = new CharField("G31CHASSID", 40);
        CharField G31CABIDG313 = new CharField("G31CABID", 40);
        CharField G31IDCNUMBG313 = new CharField("G31IDCNUMB", 50);
        NumField G31POWERG313 = new NumField("G31POWER", 9, 2);
        NumField G31KILOMG313 = new NumField("G31KILOM", 8, 0);
        NumField G31_KTG313 = new NumField("G31_KT", 16, 3);
        CharField G31_EIG313 = new CharField("G31_EI", 13);
        CharField G31_CODIZMG313 = new CharField("G31_CODIZM", 3);
        CharField G31_TEGG313 = new CharField("G31_TEG", 1);
        NumField G313IG313 = new NumField("G313I", 5, 0);
        CharField DOP_NOMERG313 = new CharField("DOP_NOMER", 8);
        CharField NOMER_GTDG313 = new CharField("NOMER_GTD", 6);

        NumField G32G44 = new NumField("G32", 3, 0);
        NumField G44IG44 = new NumField("G44I", 3, 0);
        CharField G44KDG44 = new CharField("G44KD", 5);
        CharField G44NDG44 = new CharField("G44ND", 50);
        DateField G44DDG44 = new DateField("G44DD");
        CharField G44NSG44 = new CharField("G44NS", 18);
        DateField G44DSG44 = new DateField("G44DS");
        DateField G44BEGDATEG44 = new DateField("G44BEGDATE");
        DateField G44ENDDATEG44 = new DateField("G44ENDDATE");
        CharField G44CODESTRG44 = new CharField("G44CODESTR", 2);
        CharField G44VIDPLATG44 = new CharField("G44VIDPLAT", 4);
        CharField G44CODSROKG44 = new CharField("G44CODSROK", 1);
        DateField G44DATEIMPG44 = new DateField("G44DATEIMP");
        CharField G44SPECIALG44 = new CharField("G44SPECIAL", 1);
        CharField G44STATUSG44 = new CharField("G44STATUS", 2);
        CharField NOMER_GTDG44 = new CharField("NOMER_GTD", 6);
        CharField DOP_NOMERG44 = new CharField("DOP_NOMER", 8);

        NumField G32G47 = new NumField("G32", 3, 0);
        NumField G47IG47 = new NumField("G47I", 2, 0);
        CharField G471G47 = new CharField("G471", 4);
        NumField G472G47 = new NumField("G472", 18, 3);
        CharField G473G47 = new CharField("G473", 7);
        CharField G4731G47 = new CharField("G4731", 4);
        NumField G474G47 = new NumField("G474", 15, 2);
        CharField G475G47 = new CharField("G475", 2);
        CharField G474CODEG47 = new CharField("G474CODE", 3);
        DateField G47RATEDATG47 = new DateField("G47RATEDAT");
        CharField G47TYPECODG47 = new CharField("G47TYPECOD", 1);
        CharField G47CODIZMG47 = new CharField("G47CODIZM", 3);
        CharField G47CODVALG47 = new CharField("G47CODVAL", 3);
        NumField G47WEIGHG47 = new NumField("G47WEIGH", 9, 3);
        NumField G47NUMDAYSG47 = new NumField("G47NUMDAYS", 3, 0);
        NumField G47NUMETAPG47 = new NumField("G47NUMETAP", 3, 0);
        NumField G47NUMMONTG47 = new NumField("G47NUMMONT", 3, 0);
        NumField G47LINENUMG47 = new NumField("G47LINENUM", 5, 0);
        NumField G47TARRATEG47 = new NumField("G47TARRATE", 4, 2);
        LogicalField PR_OSPLATG47 = new LogicalField("PR_OSPLAT");
        CharField KOD_SIG47 = new CharField("KOD_SI", 2);
        CharField NOMER_GTDG47 = new CharField("NOMER_GTD", 6);
        CharField DOP_NOMERG47 = new CharField("DOP_NOMER", 8);

        private void createDBFFiles() throws IOException, xBaseJException {
            tempDecl02 = File.createTempFile("DECL02", ".DBF");
            dbfDecl02 = new CustomDBF(tempDecl02.getPath(), DBF.DBASEIV, true, "Cp866");
            Util.setxBaseJProperty("ignoreDBFLengthCheck", "true");
            dbfDecl02.addField(new Field[]{
                    G011Decl, G012Decl, G013Decl, GADecl, G542Decl, N07Decl, N_REGDecl, GK_7Decl, G022IDecl, G023IDecl, G081Decl,
                    G082Decl, G083Decl, G141Decl, G142Decl, G143Decl, G031Decl, G032Decl, G04Decl, G041Decl, G05Decl, G06Decl,
                    G091Decl, G092Decl, G093Decl, G11Decl, G11_1Decl, G15ADecl, G15A_1Decl, G15Decl, G34Decl, G16Decl, G17ADecl,
                    G17A_1Decl, G17Decl, G19Decl, G251Decl, G20NDecl, G221Decl, G222Decl, G23ADecl, G12Decl, G23Decl, G241Decl,
                    G242Decl, G30Decl, G30ADecl, G30_ADecl, G32Decl, G32_2Decl, G33Decl, G33NETARIFDecl, G33INTELLDecl, G312Decl,
                    G313NDecl, G313TDecl, G315B1Decl, G317B1Decl, G317B1CODEDecl, G315BDecl, G317BDecl, G317BCODEDecl, G315ADecl,
                    G317ADecl, G317ACODEDecl, G311Decl, G318Decl, G316NDecl, G316TDecl, G31AMARKDecl, G31USLPOSTDecl, G31USLPERDecl,
                    G361Decl, G362Decl, G363Decl, G364Decl, G371Decl, G372Decl, G373Decl, G39Decl, G39EDIZMDecl, G39CODEDIDecl,
                    G35Decl, G38Decl, G38ADecl, G41Decl, G41BDecl, G41ADecl, G40NDecl, G40TDecl, G44NDecl, G44TDecl, G42Decl,
                    G43Decl, G451Decl, G46Decl, G47NDecl, G481Decl, G48DOCNUMDecl, G48DOCDATEDecl, G482Decl, GBNDecl, GBTDecl,
                    G543Decl, G543DATEDecl, G547Decl, G547DATEDecl, G546Decl, G546PERNAMDecl, G546MIDNAMDecl, G545Decl, G54PHONEDecl,
                    G54ANUMBERDecl, G54ADATEDecl, G54AENDDATDecl, G544Decl, G54CSERIESDecl, G54CNUMBERDecl, G54CIDNUMBDecl,
                    G54CDATEDecl, KONTRDecl, TYPE_DCLDecl, NOMER_GTDDecl, DOP_NOMERDecl, NAPRAVLDecl, P_ARXDecl, OF_DOCDecl,
                    G33_PDecl, P_OK_EXPDecl, PR_REPORTDecl, TR_RDecl, TR_G46Decl, R_STRDecl, R_FINDecl, R_POGRDecl, R_SUMMADecl,
                    G023IPOSTADecl, G023ICODSTDecl, G023INAMSTDecl, G023IREGIODecl, G023ICITYDecl, G023ISTREEDecl, G083POSTALDecl,
                    G083CODSTRDecl, G083NAMSTRDecl, G083REGIONDecl, G083CITYDecl, G083STREETDecl, G093POSTALDecl, G093CODSTRDecl,
                    G093NAMSTRDecl, G093REGIONDecl, G093CITYDecl, G093STREETDecl, G143POSTALDecl, G143CODSTRDecl, G143NAMSTRDecl,
                    G143REGIONDecl, G143CITYDecl, G143STREETDecl, G30_APOSTADecl, G30_ACODSTDecl, G30_ANAMSTDecl, G30_AREGIODecl,
                    G30_ACITYDecl, G30_ASTREEDecl, G31RATEOUTDecl, G31DOCNUMBDecl, G31DOCDATEDecl, G31DESCRIPDecl, G31TNVEDDecl,
                    G31QUANTDecl, G31EDIZMDecl, G31CODIZMDecl, G31OPERATDecl, G31METODDecl});

            tempDobl = File.createTempFile("DOBL", ".DBF");
            dbfDobl = new CustomDBF(tempDobl.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfDobl.addField(new Field[]{G011Dobl, G012Dobl, G013Dobl, GADobl, G542Dobl, N07Dobl, N_REGDobl, G141Dobl, G142Dobl,
                    G143Dobl, G031Dobl, G032Dobl, G32Dobl, G32_2Dobl, G33Dobl, G33NETARIFDobl, G33INTELLDobl, G312Dobl, G313NDobl,
                    G313TDobl, G315B1Dobl, G317B1Dobl, G317B1CODEDobl, G315BDobl, G317BDobl, G317BCODEDobl, G315ADobl, G317ADobl,
                    G317ACODEDobl, G311Dobl, G318Dobl, G316NDobl, G316TDobl, G31AMARKDobl, G31USLPOSTDobl, G31USLPERDobl,
                    G34Dobl, G361Dobl, G362Dobl, G363Dobl, G364Dobl, G371Dobl, G372Dobl, G373Dobl, G39Dobl, G39EDIZMDobl,
                    G39CODEDIDobl, G35Dobl, G38Dobl, G38ADobl, G41Dobl, G41BDobl, G41ADobl, G40NDobl, G40TDobl, G44NDobl,
                    G44TDobl, G42Dobl, G43Dobl, G451Dobl, G46Dobl, G47NDobl, KONTRDobl, TYPE_DCLDobl, NOMER_GTDDobl, DOP_NOMERDobl,
                    OF_DOCDobl, G33_PDobl, TR_RDobl, TR_G46Dobl, R_STRDobl, R_FINDobl, R_POGRDobl, G31RATEOUTDobl, G31DOCNUMBDobl,
                    G31DOCDATEDobl, G31DESCRIPDobl, G31TNVEDDobl, G31QUANTDobl, G31EDIZMDobl, G31CODIZMDobl, G31OPERATDobl, G31METODDobl});

            tempG313 = File.createTempFile("G131", ".DBF");
            dbfG313 = new CustomDBF(tempG313.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG313.addField(new Field[]{G32G313, G31_NTG313, G31_MTG313, G31_MODELG313, G31_MARKING313, G31_MARKG313, G31_STANDG313,
                    G31_DATEG313, G31_FIRMAG313, G31_SORTG313, G31_SORTIMG313, G31_KINDG313, G31_DIMENSG313, G31AMODELG313,
                    G31AMARKG313, G31AYEARG313, G31VOLUMEG313, G31VINIDG313, G31BODYIDG313, G31ENGINIDG313, G31CHASSIDG313,
                    G31CABIDG313, G31IDCNUMBG313, G31POWERG313, G31KILOMG313, G31_KTG313, G31_EIG313, G31_CODIZMG313, G31_TEGG313,
                    G313IG313, DOP_NOMERG313, NOMER_GTDG313});

            tempG44 = File.createTempFile("G44", ".DBF");
            dbfG44 = new CustomDBF(tempG44.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG44.addField(new Field[]{G32G44, G44IG44, G44KDG44, G44NDG44, G44DDG44, G44NSG44, G44DSG44, G44BEGDATEG44,
                    G44ENDDATEG44, G44CODESTRG44, G44VIDPLATG44, G44CODSROKG44, G44DATEIMPG44, G44SPECIALG44, G44STATUSG44,
                    NOMER_GTDG44, DOP_NOMERG44});

            tempG47 = File.createTempFile("G47", ".DBF");
            dbfG47 = new CustomDBF(tempG47.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG47.addField(new Field[]{G32G47, G47IG47, G471G47, G472G47, G473G47, G4731G47, G474G47, G475G47, G474CODEG47,
                    G47RATEDATG47, G47TYPECODG47, G47CODIZMG47, G47CODVALG47, G47WEIGHG47, G47NUMDAYSG47, G47NUMETAPG47,
                    G47NUMMONTG47, G47LINENUMG47, G47TARRATEG47, PR_OSPLATG47, KOD_SIG47, NOMER_GTDG47, DOP_NOMERG47});

            /////////////////////////////////////// empty files \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

            NumField G18IG18 = new NumField("G18I", 3, 0);
            CharField G181AG18 = new CharField("G181A", 20);
            CharField G182G18 = new CharField("G182", 2);
            CharField G18SIGNG18 = new CharField("G18SIGN", 1);
            CharField NOMER_GTDG18 = new CharField("NOMER_GTD", 6);
            CharField DOP_NOMERG18 = new CharField("DOP_NOMER", 8);

            NumField G20IG20 = new NumField("G20I", 2, 0);
            CharField G202G20 = new CharField("G202", 3);
            CharField G205G20 = new CharField("G205", 20);
            CharField G206G20 = new CharField("G206", 2);
            CharField NOMER_GTDG20 = new CharField("NOMER_GTD", 6);
            CharField DOP_NOMERG20 = new CharField("DOP_NOMER", 8);

            NumField G21IG21 = new NumField("G21I", 3, 0);
            CharField G211G21 = new CharField("G211", 20);
            CharField G212G21 = new CharField("G212", 2);
            CharField G21SIGNG21 = new CharField("G21SIGN", 1);
            CharField NOMER_GTDG21 = new CharField("NOMER_GTD", 6);
            CharField DOP_NOMERG21 = new CharField("DOP_NOMER", 8);

            NumField G32G316 = new NumField("G32", 3, 0);
            NumField G316IG316 = new NumField("G316I", 2, 0);
            CharField G316_NKG316 = new CharField("G316_NK", 17);
            CharField NOMER_GTDG316 = new CharField("NOMER_GTD", 6);
            CharField DOP_NOMERG316 = new CharField("DOP_NOMER", 8);

            NumField G32G40 = new NumField("G32", 3, 0);
            NumField G40IG40 = new NumField("G40I", 3, 0);
            CharField G40NDG40 = new CharField("G40ND", 23);
            NumField G40NTG40 = new NumField("G40NT", 3, 0);
            DateField G40DATEG40 = new DateField("G40DATE");
            NumField G40VESNETG40 = new NumField("G40VESNET", 12, 3);
            NumField G40KOLVOG40 = new NumField("G40KOLVO", 12, 3);
            CharField G40EDIZMG40 = new CharField("G40EDIZM", 11);
            CharField G40CODIZMG40 = new CharField("G40CODIZM", 3);
            CharField NOMER_GTDG40 = new CharField("NOMER_GTD", 6);
            CharField DOP_NOMERG40 = new CharField("DOP_NOMER", 8);

            NumField GBIGB = new NumField("GBI", 2, 0);
            CharField GBGB = new CharField("GB", 4);
            NumField GB1GB = new NumField("GB1", 15, 2);
            CharField GB3GB = new CharField("GB3", 3);
            CharField GB4GB = new CharField("GB4", 6);
            DateField GB5GB = new DateField("GB5");
            CharField GB2GB = new CharField("GB2", 2);
            CharField NOMER_GTDGB = new CharField("NOMER_GTD", 6);
            CharField DOP_NOMERGB = new CharField("DOP_NOMER", 8);

            tempG18 = File.createTempFile("G18", ".DBF");
            dbfG18 = new CustomDBF(tempG18.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG18.addField(new Field[]{G18IG18, G181AG18, G182G18, G18SIGNG18, NOMER_GTDG18, DOP_NOMERG18});
            dbfG18.close();

            tempG20 = File.createTempFile("G20", ".DBF");
            dbfG20 = new CustomDBF(tempG20.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG20.addField(new Field[]{G20IG20, G202G20, G205G20, G206G20, NOMER_GTDG20, DOP_NOMERG20});
            dbfG20.close();

            tempG21 = File.createTempFile("G21", ".DBF");
            dbfG21 = new CustomDBF(tempG21.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG21.addField(new Field[]{G21IG21, G211G21, G212G21, G21SIGNG21, NOMER_GTDG21, DOP_NOMERG21});
            dbfG21.close();

            tempG316 = File.createTempFile("G316", ".DBF");
            dbfG316 = new CustomDBF(tempG316.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG316.addField(new Field[]{G32G316, G316IG316, G316_NKG316, NOMER_GTDG316, DOP_NOMERG316});
            dbfG316.close();

            tempG40 = File.createTempFile("G40", ".DBF");
            dbfG40 = new CustomDBF(tempG40.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfG40.addField(new Field[]{G32G40, G40IG40, G40NDG40, G40NTG40, G40DATEG40, G40VESNETG40, G40KOLVOG40,
                    G40EDIZMG40, G40CODIZMG40, NOMER_GTDG40, DOP_NOMERG40});
            dbfG40.close();

            tempGB = File.createTempFile("GBB", ".DBF");
            dbfGB = new CustomDBF(tempGB.getPath(), DBF.DBASEIV, true, "Cp866");
            dbfGB.addField(new Field[]{GBIGB, GBGB, GB1GB, GB3GB, GB4GB, GB5GB, GB2GB, NOMER_GTDGB, DOP_NOMERGB});
            dbfGB.close();
        }
    }
}
