package platformlocal;

import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRAlignment;
import net.sf.jasperreports.engine.JRPen;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;

public class DefaultJasperDesign extends JasperDesign {

    private JRDesignStyle defaultStyle;

    private void addDefaultStyle() {

        defaultStyle = new JRDesignStyle();
        defaultStyle.setName("DefaultStyle");
        defaultStyle.setDefault(true);

        defaultStyle.setFontName("Tahoma");
        defaultStyle.setFontSize(10);

        defaultStyle.setVerticalAlignment(JRAlignment.VERTICAL_ALIGN_MIDDLE);

        defaultStyle.setPdfFontName("c:/windows/fonts/tahoma.ttf");
        defaultStyle.setPdfEncoding("Cp1251");
        defaultStyle.setPdfEmbedded(false);

        try {
            addStyle(defaultStyle);
        } catch(JRException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private JRDesignStyle cellStyle;
    private void addCellStyle() {

        cellStyle = new JRDesignStyle();
        cellStyle.setParentStyle(defaultStyle);

        cellStyle.getLineBox().setLeftPadding(2);
        cellStyle.getLineBox().setRightPadding(2);

        cellStyle.getLineBox().getPen().setLineColor(Color.black);
        cellStyle.getLineBox().getPen().setLineStyle(JRPen.LINE_STYLE_SOLID);
        cellStyle.getLineBox().getPen().setLineWidth((float) 0.5);

        try {
            addStyle(cellStyle);
        } catch(JRException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    DefaultJasperDesign(NavigatorForm navigatorForm) {

        int pageWidth = 595-40;

        setName(navigatorForm.caption);
        setOrientation(JasperDesign.ORIENTATION_LANDSCAPE);

        addDefaultStyle();
        addCellStyle();

        for(GroupObjectImplement group : (List<GroupObjectImplement>)navigatorForm.Groups) {

            Collection<ReportDrawField> drawFields = new ArrayList();

            // сначала все коды
            for(ObjectImplement object : group)
                drawFields.add(new ReportDrawField(object.getSID(), object.caption, Type.Object));

            // бежим по всем свойствам входящим в объектам
            for(PropertyView property : (List<PropertyView>)navigatorForm.propertyViews) {

                GroupObjectImplement drawProp = (property.ToDraw == null ? property.View.GetApplyObject() : property.ToDraw);
                if (drawProp == group)
                    drawFields.add(new ReportDrawField(property.getSID(), property.View.Property.caption, property.View.Property.getType()));
            }

            JRDesignBand band = new JRDesignBand();
            int bandHeight = 18;
            band.setHeight(bandHeight);

            boolean detail = (group == navigatorForm.Groups.get(navigatorForm.Groups.size()-1));

            JRDesignBand pageHeadBand = null;
            int PageHeadHeight = 20;
            if(detail) {
                // создадим PageHead
                pageHeadBand = new JRDesignBand();
                pageHeadBand.setHeight(PageHeadHeight);
                setPageHeader(pageHeadBand);

                setDetail(band);
            } else {
                
                // создадим группу
                JRDesignGroup DesignGroup = new JRDesignGroup();
                DesignGroup.setName("group"+ group.ID);

                JRDesignExpression GroupExpr = new JRDesignExpression();
                GroupExpr.setValueClass(java.lang.String.class);
                String GroupString = "";
                for(ObjectImplement Object : group)
                    GroupString = (GroupString.length()==0?"":GroupString+"+\" \"+")+"String.valueOf($F{"+Object.getSID()+"})";
                GroupExpr.setText(GroupString);

                DesignGroup.setExpression(GroupExpr);
                DesignGroup.setGroupHeader(band);

                try {
                    addGroup(DesignGroup);
                } catch(JRException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // узнаем общую ширину чтобы пропорционально считать ()
            int TotalWidth = 0;
            for(ReportDrawField Draw : drawFields) {
                if(!detail) TotalWidth += Draw.getCaptionWidth();
                TotalWidth += Draw.width;
            }


            int Left = 0;
            for(ReportDrawField Draw : drawFields) {
                // закидываем сначала Field
                JRDesignField JRField = new JRDesignField();
                JRField.setName(Draw.sID);
                JRField.setValueClassName(Draw.valueClass.getName());
                try {
                    addField(JRField);
                } catch(JRException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

                int DrawWidth = pageWidth*Draw.width /TotalWidth;

                JRDesignStaticText DrawCaption = new JRDesignStaticText();
                DrawCaption.setText(Draw.caption);
                DrawCaption.setX(Left);
                DrawCaption.setY(0);

                if(detail) {
                    DrawCaption.setWidth(DrawWidth);
                    DrawCaption.setHeight(PageHeadHeight);
                    DrawCaption.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_CENTER);
                    pageHeadBand.addElement(DrawCaption);
                } else {
                    int CaptWidth = pageWidth*Draw.getCaptionWidth()/TotalWidth;
                    DrawCaption.setWidth(CaptWidth);
                    DrawCaption.setHeight(bandHeight);
                    DrawCaption.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_LEFT);
                    Left += CaptWidth;
                    band.addElement(DrawCaption);
                }
                DrawCaption.setStretchType(JRDesignStaticText.STRETCH_TYPE_RELATIVE_TO_BAND_HEIGHT);

                JRDesignTextField DrawText = new JRDesignTextField();
                DrawText.setX(Left);
                DrawText.setY(0);
                DrawText.setWidth(DrawWidth);
                DrawText.setHeight(bandHeight);
                DrawText.setHorizontalAlignment(Draw.alignment);
                Left += DrawWidth;

                JRDesignExpression DrawExpr = new JRDesignExpression();
                DrawExpr.setValueClass(Draw.valueClass);
                DrawExpr.setText("$F{"+Draw.sID +"}");
                DrawText.setExpression(DrawExpr);
                band.addElement(DrawText);

                DrawText.setStretchWithOverflow(true);
            }
        }

    }

}
