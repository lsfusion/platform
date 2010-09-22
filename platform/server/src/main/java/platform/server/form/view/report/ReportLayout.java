package platform.server.form.view.report;

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
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

    public void doLayout(int pageWidth) {

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

class ReportDetailLayout extends ReportLayout {

    private JRDesignBand pageHeadBand;
    private JRDesignBand detailBand;

    public ReportDetailLayout(JasperDesign design) {
        // todo : убрать из заголовка страницы заголовок detail?
        pageHeadBand = new JRDesignBand();
        pageHeadBand.setHeight(ROW_HEIGHT);
        design.setPageHeader(pageHeadBand);

        detailBand = new JRDesignBand();
        detailBand.setHeight(ROW_HEIGHT);
        design.setDetail(detailBand);
    }

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

        pageHeadBand.addElement(caption);
        detailBand.addElement(text);
    }
}

abstract class ReportGroupLayout extends ReportLayout {

}

class ReportGroupRowLayout extends ReportGroupLayout {

    protected JRDesignBand groupBand;

    public ReportGroupRowLayout(JRDesignGroup designGroup) {

        groupBand = new JRDesignBand();
        groupBand.setHeight(ROW_HEIGHT);
        groupBand.setSplitType(SplitTypeEnum.PREVENT);
        designGroup.setGroupHeader(groupBand);
    }

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

        groupBand.addElement(caption);
        groupBand.addElement(text);
    }

    public void doLayout(int pageWidth) {

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

class ReportGroupColumnLayout extends ReportGroupLayout {

    protected JRDesignBand captionGroupBand;
    protected JRDesignBand textGroupBand;

    public ReportGroupColumnLayout(JRDesignGroup captionGroup, JRDesignGroup textGroup) {

        captionGroupBand = new JRDesignBand();
        captionGroupBand.setHeight(ROW_HEIGHT);
        captionGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        captionGroup.setGroupHeader(captionGroupBand);

        textGroupBand = new JRDesignBand();
        textGroupBand.setHeight(ROW_HEIGHT);
        textGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        textGroup.setGroupHeader(textGroupBand);
    }

    public void add(ReportDrawField reportField, JRDesignTextField caption, JRDesignTextField text) {
        super.add(reportField, caption, text);

        captionGroupBand.addElement(caption);
        textGroupBand.addElement(text);
    }
}


