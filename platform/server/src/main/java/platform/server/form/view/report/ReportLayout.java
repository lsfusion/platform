package platform.server.form.view.report;

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
    protected final static int ROW_HEIGHT = 18;

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
            captions.get(reportField).setY(reportField.row * ROW_HEIGHT);
            captions.get(reportField).setWidth(reportField.width);
            captions.get(reportField).setHeight(ROW_HEIGHT);

            textFields.get(reportField).setX(reportField.left);
            textFields.get(reportField).setY(reportField.row * ROW_HEIGHT);
            textFields.get(reportField).setWidth(reportField.width);
            textFields.get(reportField).setHeight(ROW_HEIGHT);
        }

        return rowCount;
    }
}

class ReportDetailLayout extends ReportLayout {

    private JRDesignBand pageHeadBand;
    private JRDesignBand detailBand;

    public ReportDetailLayout(JasperDesign design) {
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

        pageHeadBand.setHeight(rowCount * ROW_HEIGHT);
        detailBand.setHeight(rowCount * ROW_HEIGHT);

        return rowCount;
    }
}

abstract class ReportGroupLayout extends ReportLayout {

}

class ReportGroupRowLayout extends ReportGroupLayout {

    protected JRDesignBand groupBand;

    public ReportGroupRowLayout(JRDesignGroup designGroup) {

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

        groupBand.setHeight(rowCount * ROW_HEIGHT);

        return rowCount;
    }
}

class ReportGroupColumnLayout extends ReportGroupLayout {

    protected JRDesignBand captionGroupBand;
    protected JRDesignBand textGroupBand;

    public ReportGroupColumnLayout(JRDesignGroup captionGroup, JRDesignGroup textGroup) {

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

        captionGroupBand.setHeight(rowCount * ROW_HEIGHT);
        textGroupBand.setHeight(rowCount * ROW_HEIGHT);

        return rowCount;
    }
}


