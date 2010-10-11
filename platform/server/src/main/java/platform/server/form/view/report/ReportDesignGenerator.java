package platform.server.form.view.report;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import platform.interop.form.ReportConstants;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.GroupObjectHierarchy;
import platform.server.form.view.FormView;
import platform.server.form.view.GroupObjectView;
import platform.server.form.view.ObjectView;
import platform.server.form.view.PropertyDrawView;

import java.util.*;

import static platform.server.form.entity.GroupObjectHierarchy.ReportNode;

/**
 * User: DAle
 * Date: 09.08.2010
 * Time: 18:07:13
 */

public class ReportDesignGenerator {
    private GroupObjectHierarchy.ReportHierarchy hierarchy;
    private FormView formView;
    private Set<Integer> hiddenGroupsId;
    private boolean toExcel;

    private final static int defaultPageWidth = 842;   
    private final static int defaultPageHeight = 595; // эти константы есть в JasperReports Ultimate Guide

    private final static int pageWidth = defaultPageWidth - 40;
    private final static int neighboursGap = 5;

    private Map<String, JasperDesign> designs = new HashMap<String, JasperDesign>();

    public ReportDesignGenerator(FormView formView, GroupObjectHierarchy.ReportHierarchy hierarchy, Set<Integer> hiddenGroupsId, boolean toExcel) {
        this.formView = formView;
        this.hierarchy = hierarchy;
        this.hiddenGroupsId = hiddenGroupsId;
        this.toExcel = toExcel;
    }

    public Map<String, JasperDesign> generate() throws JRException {
        JasperDesign rootDesign = createJasperDesignObject(GroupObjectHierarchy.rootNodeName, true, false);

        iterateChildReports(rootDesign, null, 0);

        if (toExcel) {
            for (JasperDesign design : designs.values()) {
                design.setIgnorePagination(true);    
            }
        }

        return designs;
    }

    private void iterateChildReports(JasperDesign design, ReportNode node, int treeGroupLevel) throws JRException {
        List<ReportNode> children = (node == null ? hierarchy.getRootNodes() : hierarchy.getChildNodes(node));
        for (ReportNode childNode : children) {
            JRDesignBand detail = new JRDesignBand();

            if (node == null) {
                treeGroupLevel = childNode.getGroupLevel();
            }
            String sid = childNode.getID();

            boolean hasTopMargin = (childNode != children.get(0));
            JasperDesign childDesign = createJasperDesignObject(sid, false, hasTopMargin);
            createDesignGroups(childDesign, childNode, node, treeGroupLevel);
            iterateChildReports(childDesign, childNode, treeGroupLevel);

            addParametersToDesign(design, sid);

            JRDesignSubreport subreportElement = new JRDesignSubreport(designs.get(sid));
            setExpressionsToSubreportElement(subreportElement, sid);
            subreportElement.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
            detail.addElement(subreportElement);

            ((JRDesignSection)design.getDetailSection()).addBand(detail);
        }
    }

    private void createDesignGroups(JasperDesign design, ReportNode node, ReportNode parent, int treeGroupLevel) throws JRException {
        List<GroupObjectEntity> groups = node.getGroupList();
        for (GroupObjectEntity group : groups) {
            boolean hiddenGroup = hiddenGroupsId.contains(group.getID());
            GroupObjectView groupView = formView.getGroupObject(group);
            List<ReportDrawField> drawFields = new ArrayList<ReportDrawField>();

            for(PropertyDrawView property : formView.properties) {
                if (group == property.entity.toDraw) {
                    ReportDrawField reportField = property.getReportDrawField();
                    if (reportField != null)
                        drawFields.add(reportField);
                }
            }

            if (!hiddenGroup) {
                boolean detail = hierarchy.isLeaf(node) && (group == groups.get(groups.size()-1));
                ReportLayout reportLayout;

                if (detail) {
                    reportLayout = new ReportDetailLayout(design);
                } else {
                    
                    int captionWidth = 0, preferredWidth = 0;
                    for (ReportDrawField reportField : drawFields) {
                        captionWidth += reportField.getCaptionWidth();
                        preferredWidth += reportField.getPreferredWidth();
                    }

                    if (captionWidth + preferredWidth <= pageWidth) {
                        JRDesignGroup designGroup = addDesignGroup(design, groupView, "designGroup" + group.getID());
                        reportLayout = new ReportGroupRowLayout(designGroup);
                    } else {
                        JRDesignGroup captionGroup = addDesignGroup(design, groupView, "captionGroup" + group.getID());
                        JRDesignGroup textGroup = addDesignGroup(design, groupView, "textGroup" + group.getID());
                        reportLayout = new ReportGroupColumnLayout(captionGroup, textGroup);
                    }
                }

                int groupsCnt = groups.size();
                int minGroupLevel = node.getGroupLevel() - groupsCnt;

                JRDesignStyle groupCellStyle;
                if (parent != null) {
                    int minParentGroupLevel = parent.getGroupLevel()-parent.getGroupList().size();
                    groupCellStyle = DesignStyles.getGroupStyle(groups.indexOf(group), groupsCnt, minGroupLevel, minParentGroupLevel, treeGroupLevel);
                } else {
                    groupCellStyle = DesignStyles.getGroupStyle(groups.indexOf(group), groupsCnt, minGroupLevel, treeGroupLevel, treeGroupLevel);
                }
                design.addStyle(groupCellStyle);

                for(ReportDrawField reportField : drawFields) {
                    addReportFieldToLayout(reportLayout, reportField, groupCellStyle);
                }
                reportLayout.doLayout(pageWidth);
            }

            for (ObjectView view : groupView) {
                ReportDrawField objField = new ReportDrawField(view.entity.getSID(), "");
                view.entity.baseClass.getType().fillReportDrawField(objField);
                addDesignField(design, objField);
            }
            for(ReportDrawField reportField : drawFields) {
                // закидываем сначала Field
                addDesignField(design, reportField);
            }
        }
    }

    private void addReportFieldToLayout(ReportLayout layout, ReportDrawField reportField, JRDesignStyle style) {
        JRDesignExpression captionExpr = new JRDesignExpression();
        captionExpr.setValueClass(java.lang.String.class);
        captionExpr.setText('"' + reportField.caption + '"');

        JRDesignTextField drawCaption = new JRDesignTextField();
        drawCaption.setStyle(style);
        drawCaption.setExpression(captionExpr);
        drawCaption.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
        drawCaption.setStretchWithOverflow(true);
        drawCaption.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);

        JRDesignExpression textExpr = new JRDesignExpression();
        textExpr.setValueClass(reportField.valueClass);
        textExpr.setText("$F{"+reportField.sID +"}");

        JRDesignTextField drawText = new JRDesignTextField();
        drawText.setStyle(style);
        drawText.setHorizontalAlignment(reportField.alignment);
        drawText.setExpression(textExpr);
        drawText.setPositionType(PositionTypeEnum.FLOAT);
        drawText.setStretchWithOverflow(true);
        drawText.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
        drawText.setBlankWhenNull(true);

        if (!toExcel) {
            drawText.setPattern(reportField.pattern);
        }

        layout.add(reportField, drawCaption, drawText);
    }

    private JRDesignGroup addDesignGroup(JasperDesign design, GroupObjectView group, String groupName) throws JRException {

        JRDesignGroup designGroup = new JRDesignGroup();
        designGroup.setName(groupName);

        JRDesignExpression groupExpr = new JRDesignExpression();
        groupExpr.setValueClass(java.lang.String.class);
        String groupString = "";
        for(ObjectView object : group)
            groupString = (groupString.length()==0?"":groupString+"+\" \"+")+"String.valueOf($F{"+object.entity.getSID()+"})";
        groupExpr.setText(groupString);

        designGroup.setExpression(groupExpr);

        design.addGroup(designGroup);

        return designGroup;
    }

    private JRDesignField addDesignField(JasperDesign design, ReportDrawField reportField) throws JRException {
        JRDesignField designField = new JRDesignField();

        designField.setName(reportField.sID);
        designField.setValueClassName(reportField.valueClass.getName());

        design.addField(designField);
        return designField;
    }

    private static void setExpressionsToSubreportElement(JRDesignSubreport subreport, String sid) {
        JRDesignExpression subreportExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.reportSuffix), JasperReport.class);
        subreport.setExpression(subreportExpr);

        JRDesignExpression sourceExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.sourceSuffix), JRDataSource.class);
        subreport.setDataSourceExpression(sourceExpr);

        JRDesignExpression paramsExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.paramsSuffix), Map.class);
        subreport.setParametersMapExpression(paramsExpr);
    }

    private static void addParametersToDesign(JasperDesign design, String sid) throws JRException {
        ReportUtils.addParameter(design, sid + ReportConstants.reportSuffix, JasperReport.class);
        ReportUtils.addParameter(design, sid + ReportConstants.sourceSuffix, JRDataSource.class);
        ReportUtils.addParameter(design, sid + ReportConstants.paramsSuffix, Map.class);
    }

    private JasperDesign createJasperDesignObject(String name, boolean isMain, boolean hasTopMargin) throws JRException {
        JasperDesign design = new JasperDesign();
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            design.setName(formView.caption);
        } else {
            design.setName(name);
        }

        design.setPageWidth(defaultPageWidth);
        design.setPageHeight(defaultPageHeight);

        if (!isMain) {
            design.setTopMargin(hasTopMargin ? neighboursGap : 0);
            design.setBottomMargin(0);
            design.setLeftMargin(0);
            design.setRightMargin(0);
        }

        design.setOrientation(OrientationEnum.LANDSCAPE);

        design.addStyle(DesignStyles.getDefaultStyle());
        designs.put(name, design);
        return design;
    }
}
