package lsfusion.server.logics.form.stat.print.design;

import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.SplitTypeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ReportLayout {
    protected int rowHeight;
    
    public ReportLayout(int rowHeight) {
        this.rowHeight = rowHeight;            
    }

    protected List<ReportDrawField> reportFields = new ArrayList<>();
    protected Map<ReportDrawField, JRDesignTextField> captions = new HashMap<>();
    protected Map<ReportDrawField, JRDesignTextField> textFields = new HashMap<>();

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

    public static abstract class BandSection {
        public abstract void setBand(JRBand band);
    }

    public static abstract class MultipleBandSection extends BandSection {
        public void setBand(JRBand band) {
            addBand(band);
        }

        public abstract void addBand(JRBand band);
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

    public ReportGroupRowLayout(BandSection section, int rowHeight) {
        super(rowHeight);

        groupBand = new JRDesignBand();
        groupBand.setSplitType(SplitTypeEnum.PREVENT);
        section.setBand(groupBand);
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

    public ReportGroupColumnLayout(MultipleBandSection designSection, int rowHeight) {
        super(rowHeight);

        captionGroupBand = new JRDesignBand();
        captionGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        designSection.addBand(captionGroupBand);

        textGroupBand = new JRDesignBand();
        textGroupBand.setSplitType(SplitTypeEnum.PREVENT);
        designSection.addBand(textGroupBand);
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


