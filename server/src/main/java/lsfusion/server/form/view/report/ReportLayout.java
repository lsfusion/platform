package lsfusion.server.form.view.report;

import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.SplitTypeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 13.08.2010
 * Time: 16:23:30
 */

public abstract class ReportLayout {
    protected int rowHeight;
    
    public ReportLayout(int rowHeight) {
        this.rowHeight = rowHeight;            
    }

    protected List<ReportDrawField> reportFields = new ArrayList<ReportDrawField>();
    protected Map<ReportDrawField, JRDesignTextField> captions = new HashMap<ReportDrawField, JRDesignTextField>();
    protected Map<ReportDrawField, JRDesignTextField> textFields = new HashMap<ReportDrawField, JRDesignTextField>();

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        reportFields.add(reportField);
        captions.put(reportField, caption);
        textFields.put(reportField, text);
    }

    public int doLayout(int pageWidth) {

        int rowCount = AbstractRowLayout.doLayout(reportFields, pageWidth, false);

        for (ReportDrawField reportField : reportFields) {

            captions.get(reportField).setX(reportField.left);
            captions.get(reportField).setY(reportField.row * rowHeight);
            captions.get(reportField).setWidth(reportField.width);
            captions.get(reportField).setHeight(rowHeight);

            textFields.get(reportField).setX(reportField.left);
            textFields.get(reportField).setY(reportField.row * rowHeight);
            textFields.get(reportField).setWidth(reportField.width);
            textFields.get(reportField).setHeight(rowHeight);
        }

        return rowCount;
    }
}

class ReportDetailLayout extends ReportLayout {

    private JRDesignBand pageHeadBand;
    private JRDesignBand detailBand;

    public ReportDetailLayout(JasperDesign design, int rowHeight) {
        super(rowHeight);
        
        // убрать из заголовка страницы заголовок detail?
        pageHeadBand = new JRDesignBand();
        design.setPageHeader(pageHeadBand);

        detailBand = new JRDesignBand();

        ((JRDesignSection)design.getDetailSection()).addBand(detailBand);
    }

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

        pageHeadBand.addElement(caption);
        detailBand.addElement(text);
    }

    @Override
    public int doLayout(int pageWidth) {
        int rowCount = super.doLayout(pageWidth);

        pageHeadBand.setHeight(rowCount * rowHeight);
        detailBand.setHeight(rowCount * rowHeight);

        return rowCount;
    }
}

abstract class ReportGroupLayout extends ReportLayout {
    public ReportGroupLayout(int rowHeight) {
        super(rowHeight);
    }
}

class ReportGroupRowLayout extends ReportGroupLayout {

    protected JRDesignBand groupBand;

    public ReportGroupRowLayout(JRDesignGroup designGroup, int rowHeight) {
        super(rowHeight);

        groupBand = new JRDesignBand();
        groupBand.setSplitType(SplitTypeEnum.PREVENT);
        ((JRDesignSection)designGroup.getGroupHeaderSection()).addBand(groupBand);
    }

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

        groupBand.addElement(caption);
        groupBand.addElement(text);
    }

    @Override
    public int doLayout(int pageWidth) {

        int captionWidth = 0;
        for (ReportDrawField reportField : reportFields)
            captionWidth += reportField.getCaptionWidth();

        int rowCount = AbstractRowLayout.doLayout(reportFields, pageWidth - captionWidth, false);

        int left = 0, width = 0;
        for (ReportDrawField reportField : reportFields) {

            captions.get(reportField).setX(left);
            captions.get(reportField).setY(reportField.row * rowHeight);
            captions.get(reportField).setWidth(reportField.getCaptionWidth());
            captions.get(reportField).setHeight(rowHeight);

            width += reportField.getCaptionWidth();

            textFields.get(reportField).setX(width + reportField.left);
            textFields.get(reportField).setY(reportField.row * rowHeight);
            textFields.get(reportField).setWidth(reportField.width);
            textFields.get(reportField).setHeight(rowHeight);

            left = width + reportField.left + reportField.width;
        }

        groupBand.setHeight(rowCount * rowHeight);

        return rowCount;
    }
}

class ReportGroupColumnLayout extends ReportGroupLayout {

    protected JRDesignBand captionGroupBand;
    protected JRDesignBand textGroupBand;

    public ReportGroupColumnLayout(JRDesignGroup captionGroup, JRDesignGroup textGroup, int rowHeight) {
        super(rowHeight);

        captionGroupBand = new JRDesignBand();
        captionGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        ((JRDesignSection)captionGroup.getGroupHeaderSection()).addBand(captionGroupBand);

        textGroupBand = new JRDesignBand();
        textGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        ((JRDesignSection)textGroup.getGroupHeaderSection()).addBand(textGroupBand);
    }

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

        captionGroupBand.addElement(caption);
        textGroupBand.addElement(text);
    }

    @Override
    public int doLayout(int pageWidth) {
        int rowCount = super.doLayout(pageWidth);

        captionGroupBand.setHeight(rowCount * rowHeight);
        textGroupBand.setHeight(rowCount * rowHeight);

        return rowCount;
    }
}


