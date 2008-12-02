package platformlocal;

import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRAlignment;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class DefaultJasperDesign extends JasperDesign {

    DefaultJasperDesign(NavigatorForm navigatorForm) {

        int PageWidth = 595-40;
//        Design.setPageWidth(PageWidth);
        setName("Report");

        JRDesignStyle Style = new JRDesignStyle();
        Style.setName("Arial_Normal");
        Style.setDefault(true);
        Style.setFontName("Arial");
        Style.setFontSize(8);
        Style.setPdfFontName("c:\\windows\\fonts\\tahoma.ttf");
        Style.setPdfEncoding("Cp1251");
        Style.setPdfEmbedded(false);
        try {
            addStyle(Style);
        } catch(JRException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }


        for(GroupObjectImplement Group : (List<GroupObjectImplement>)navigatorForm.Groups) {
            Collection<ReportDrawField> DrawFields = new ArrayList();

            // сначала все коды
            for(ObjectImplement Object : Group)
                DrawFields.add(new ReportDrawField("obj"+Object.ID,Object.caption,Type.Object));

            // бежим по всем свойствам входящим в объектам
            for(PropertyView Property : (List<PropertyView>)navigatorForm.propertyViews) {
                GroupObjectImplement DrawProp = (Property.ToDraw==null?Property.View.GetApplyObject():Property.ToDraw);
                if(DrawProp==Group)
                    DrawFields.add(new ReportDrawField("prop"+Property.ID,Property.View.Property.caption,Property.View.Property.getType()));
            }

            JRDesignBand Band = new JRDesignBand();
            int BandHeight = 20;
            Band.setHeight(BandHeight);

            boolean Detail = (Group==navigatorForm.Groups.get(navigatorForm.Groups.size()-1));
            JRDesignBand PageHeadBand = null;
            int PageHeadHeight = 20;
            if(Detail) {
                // создадим PageHead
                PageHeadBand = new JRDesignBand();
                PageHeadBand.setHeight(PageHeadHeight);
                setPageHeader(PageHeadBand);

                setDetail(Band);
            } else {
                // создадим группу
                JRDesignGroup DesignGroup = new JRDesignGroup();
                DesignGroup.setName("Group"+Group.ID);
                JRDesignExpression GroupExpr = new JRDesignExpression();
                GroupExpr.setValueClass(java.lang.String.class);
                String GroupString = "";
                for(ObjectImplement Object : Group)
                    GroupString = (GroupString.length()==0?"":GroupString+"+\" \"+")+"String.valueOf($F{obj"+Object.ID+"})";
                GroupExpr.setText(GroupString);

                DesignGroup.setExpression(GroupExpr);
                DesignGroup.setGroupHeader(Band);

                try {
                    addGroup(DesignGroup);
                } catch(JRException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // узнаем общую ширину чтобы пропорционально считать ()
            int TotalWidth = 0;
            for(ReportDrawField Draw : DrawFields) {
                if(!Detail) TotalWidth += Draw.GetCaptionWidth();
                TotalWidth += Draw.Width;
            }


            int Left = 0;
            for(ReportDrawField Draw : DrawFields) {
                // закидываем сначала Field
                JRDesignField JRField = new JRDesignField();
                JRField.setName(Draw.ID);
                JRField.setValueClassName(Draw.ValueClass.getName());
                try {
                    addField(JRField);
                } catch(JRException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

                int DrawWidth = PageWidth*Draw.Width/TotalWidth;

                JRDesignStaticText DrawCaption = new JRDesignStaticText();
                DrawCaption.setText(Draw.Caption);
                DrawCaption.setX(Left);
                DrawCaption.setY(0);

                if(Detail) {
                    DrawCaption.setWidth(DrawWidth);
                    DrawCaption.setHeight(PageHeadHeight);
                    DrawCaption.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_CENTER);
                    PageHeadBand.addElement(DrawCaption);
                } else {
                    int CaptWidth = PageWidth*Draw.GetCaptionWidth()/TotalWidth;
                    DrawCaption.setWidth(CaptWidth);
                    DrawCaption.setHeight(BandHeight);
                    DrawCaption.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_LEFT);
                    Left += CaptWidth;
                    Band.addElement(DrawCaption);
                }
                DrawCaption.setStretchType(JRDesignStaticText.STRETCH_TYPE_RELATIVE_TO_BAND_HEIGHT);

                JRDesignTextField DrawText = new JRDesignTextField();
                DrawText.setX(Left);
                DrawText.setY(0);
                DrawText.setWidth(DrawWidth);
                DrawText.setHeight(BandHeight);
                DrawText.setHorizontalAlignment(Draw.Alignment);
                Left += DrawWidth;

                JRDesignExpression DrawExpr = new JRDesignExpression();
                DrawExpr.setValueClass(Draw.ValueClass);
                DrawExpr.setText("$F{"+Draw.ID+"}");
                DrawText.setExpression(DrawExpr);
                Band.addElement(DrawText);

                DrawText.setStretchWithOverflow(true);
            }
        }

        setOrientation(JasperDesign.ORIENTATION_LANDSCAPE);
    }

}
