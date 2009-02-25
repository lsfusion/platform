package platform.server.view.form.report;

import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRAlignment;
import net.sf.jasperreports.engine.JRPen;
import net.sf.jasperreports.engine.JRElement;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.util.List;
import java.awt.*;

import platform.server.view.form.report.ReportDrawField;
import platform.Main;
import platform.server.view.form.client.FormView;
import platform.server.view.form.client.GroupObjectImplementView;
import platform.server.view.form.client.ObjectImplementView;
import platform.server.view.form.client.PropertyCellView;
import platform.server.view.form.report.AbstractRowLayout;

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
        cellStyle.setName("CellStyle");
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

    private JRDesignStyle addGroupCellStyle(int groupIndex, int groupCount) {

        JRDesignStyle groupCellStyle = new JRDesignStyle();
        groupCellStyle.setName("GroupCellStyle" + groupIndex);
        groupCellStyle.setParentStyle(cellStyle);

        int color = 255 - 64 * (groupCount - groupIndex - 1) / groupCount;
        groupCellStyle.setMode(JRElement.MODE_OPAQUE);
        groupCellStyle.setBackcolor(new Color(color, color, color));

        try {
            addStyle(groupCellStyle);
        } catch(JRException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return groupCellStyle;
    }

    private JRDesignGroup addDesignGroup(GroupObjectImplementView group, String groupName) {

        JRDesignGroup designGroup = new JRDesignGroup();
        designGroup.setName(groupName);

        JRDesignExpression groupExpr = new JRDesignExpression();
        groupExpr.setValueClass(java.lang.String.class);
        String groupString = "";
        for(ObjectImplementView object : group)
            groupString = (groupString.length()==0?"":groupString+"+\" \"+")+"String.valueOf($F{"+object.view.getSID()+"})";
        groupExpr.setText(groupString);

        designGroup.setExpression(groupExpr);

        try {
            addGroup(designGroup);
        } catch(JRException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return designGroup;
    }

    private JRDesignField addDesignField(ReportDrawField reportField) {

        JRDesignField designField = new JRDesignField();

        designField.setName(reportField.sID);
        designField.setValueClassName(reportField.valueClass.getName());

        try {
            addField(designField);
        } catch(JRException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return designField;
    }

    private static int ROW_HEIGHT = 18;

    private abstract class ReportLayout {

        List<ReportDrawField> reportFields = new ArrayList<ReportDrawField>();
        Map<ReportDrawField, JRDesignTextField> captions = new HashMap<ReportDrawField, JRDesignTextField>();
        Map<ReportDrawField, JRDesignTextField> textFields = new HashMap<ReportDrawField, JRDesignTextField>();

        void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
            reportFields.add(reportField);
            captions.put(reportField, caption);
            textFields.put(reportField, text);
        }

        void doLayout(int pageWidth) {

            AbstractRowLayout.doLayout(reportFields, pageWidth, true);

            for (ReportDrawField reportField : reportFields) {

                captions.get(reportField).setX(reportField.left);
                captions.get(reportField).setY(reportField.row * ROW_HEIGHT);
                captions.get(reportField).setWidth(reportField.width);
                captions.get(reportField).setHeight(ROW_HEIGHT);

                textFields.get(reportField).setX(reportField.left);
                textFields.get(reportField).setY(reportField.row * ROW_HEIGHT);
                textFields.get(reportField).setWidth(reportField.width);
                textFields.get(reportField).setHeight(ROW_HEIGHT);
            }

        }

    }

    private class ReportDetailLayout extends ReportLayout {

        private JRDesignBand pageHeadBand;
        private JRDesignBand detailBand;

        ReportDetailLayout() {

            pageHeadBand = new JRDesignBand();
            pageHeadBand.setHeight(ROW_HEIGHT);
            setPageHeader(pageHeadBand);

            detailBand = new JRDesignBand();
            detailBand.setHeight(ROW_HEIGHT);
            setDetail(detailBand);
        }

        void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
            super.add(reportField, caption, text);

            pageHeadBand.addElement(caption);
            detailBand.addElement(text);
        }
    }

    private abstract class ReportGroupLayout extends ReportLayout {

    }

    private class ReportGroupRowLayout extends ReportGroupLayout {

        protected JRDesignBand groupBand;

        ReportGroupRowLayout(JRDesignGroup designGroup) {

            groupBand = new JRDesignBand();
            groupBand.setHeight(ROW_HEIGHT);
            designGroup.setGroupHeader(groupBand);
        }

        void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
            super.add(reportField, caption, text);

            groupBand.addElement(caption);
            groupBand.addElement(text);
        }

        void doLayout(int pageWidth) {

            int captionWidth = 0;
            for (ReportDrawField reportField : reportFields)
                captionWidth += reportField.getCaptionWidth();

            AbstractRowLayout.doLayout(reportFields, pageWidth - captionWidth, true);

            int left = 0, width = 0;
            for (ReportDrawField reportField : reportFields) {

                captions.get(reportField).setX(left);
                captions.get(reportField).setY(reportField.row * ROW_HEIGHT);
                captions.get(reportField).setWidth(reportField.getCaptionWidth());
                captions.get(reportField).setHeight(ROW_HEIGHT);

                width += reportField.getCaptionWidth();

                textFields.get(reportField).setX(width + reportField.left);
                textFields.get(reportField).setY(reportField.row * ROW_HEIGHT);
                textFields.get(reportField).setWidth(reportField.width);
                textFields.get(reportField).setHeight(ROW_HEIGHT);

                left = width + reportField.left + reportField.width;
            }
        }
    }

    private class ReportGroupColumnLayout extends ReportGroupLayout {

        protected JRDesignBand captionGroupBand;
        protected JRDesignBand textGroupBand;

        ReportGroupColumnLayout(JRDesignGroup captionGroup, JRDesignGroup textGroup) {

            captionGroupBand = new JRDesignBand();
            captionGroupBand.setHeight(ROW_HEIGHT);
            captionGroup.setGroupHeader(captionGroupBand);

            textGroupBand = new JRDesignBand();
            textGroupBand.setHeight(ROW_HEIGHT);
            textGroup.setGroupHeader(textGroupBand);
        }

        void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
            super.add(reportField, caption, text);

            captionGroupBand.addElement(caption);
            textGroupBand.addElement(text);
        }

    }

    public DefaultJasperDesign(FormView formView) {

        int pageWidth = 842 - 40;
        int pageHeight = 595;

        setName("Report");
        setPageWidth(pageWidth + 40);
        
        setPageHeight(pageHeight);
        setOrientation(JasperDesign.ORIENTATION_LANDSCAPE);

        addDefaultStyle();
        addCellStyle();

        for(GroupObjectImplementView group : (List<GroupObjectImplementView>)formView.groupObjects) {

            Collection<ReportDrawField> drawFields = new ArrayList<ReportDrawField>();

            // сначала все коды
            for(ObjectImplementView object : group)
                drawFields.add(new ReportDrawField(object));

            // бежим по всем свойствам входящим в объектам
            for(PropertyCellView property : (List<PropertyCellView>)formView.properties) {

                if (group.view == property.view.toDraw)
                    drawFields.add(new ReportDrawField(property));
            }

            boolean detail = (group == formView.groupObjects.get(formView.groupObjects.size()-1));

            int captionWidth = 0, minimumWidth = 0, preferredWidth = 0;
            for (ReportDrawField reportField : drawFields) {
                captionWidth += reportField.getCaptionWidth();
                minimumWidth += reportField.getMinimumWidth();
                preferredWidth += reportField.getPreferredWidth();
            }

            ReportLayout reportLayout;

            if (detail) {
                reportLayout = new ReportDetailLayout();
            } else {

                if (captionWidth + preferredWidth <= pageWidth) {
                    JRDesignGroup designGroup = addDesignGroup(group, "designGroup" + group.view.ID);
                    reportLayout = new ReportGroupRowLayout(designGroup);
                }
                else {
                    JRDesignGroup captionGroup = addDesignGroup(group, "captionGroup" + group.view.ID);
                    JRDesignGroup textGroup = addDesignGroup(group, "textGroup" + group.view.ID);
                    reportLayout = new ReportGroupColumnLayout(captionGroup, textGroup);
                }
            }

            JRDesignStyle groupCellStyle = addGroupCellStyle(formView.groupObjects.indexOf(group), formView.groupObjects.size());

            for(ReportDrawField reportField : drawFields) {

                // закидываем сначала Field
                addDesignField(reportField);

                JRDesignExpression captionExpr = new JRDesignExpression();
                captionExpr.setValueClass(java.lang.String.class);
                captionExpr.setText('"' + reportField.caption + '"');

                JRDesignTextField drawCaption = new JRDesignTextField();
                drawCaption.setStyle(groupCellStyle);
                drawCaption.setExpression(captionExpr);
                drawCaption.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_CENTER);
                drawCaption.setStretchWithOverflow(true);
                drawCaption.setStretchType(JRDesignStaticText.STRETCH_TYPE_RELATIVE_TO_BAND_HEIGHT);

                JRDesignExpression textExpr = new JRDesignExpression();
                textExpr.setValueClass(reportField.valueClass);
                textExpr.setText("$F{"+reportField.sID +"}");

                JRDesignTextField drawText = new JRDesignTextField();
                drawText.setStyle(groupCellStyle);
                drawText.setHorizontalAlignment(reportField.alignment);
                drawText.setExpression(textExpr);
                drawText.setPositionType(JRDesignTextField.POSITION_TYPE_FLOAT);
                drawText.setStretchWithOverflow(true);
                drawText.setStretchType(JRDesignTextField.STRETCH_TYPE_RELATIVE_TO_BAND_HEIGHT);
                drawText.setBlankWhenNull(true);

                drawText.setPattern(reportField.pattern);

                reportLayout.add(reportField, drawCaption, drawText);
            }

            reportLayout.doLayout(pageWidth);
        }

    }

}
