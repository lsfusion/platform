package lsfusion.interop.form.print;

import lsfusion.interop.form.print.ReportConstants;

public enum ReportFieldExtraType {
    HEADER {
        @Override
        public String getReportFieldNameSuffix() {
            return ReportConstants.headerSuffix;
        }
    },
    FOOTER {
        @Override
        public String getReportFieldNameSuffix() {
            return ReportConstants.footerSuffix;
        }
    },
    SHOWIF {
        @Override
        public String getReportFieldNameSuffix() {
            return ReportConstants.showIfSuffix;
        }
    },
    BACKGROUND {
        @Override
        public String getReportFieldNameSuffix() {
            return ReportConstants.backgroundSuffix;
        }
    },
    FOREGROUND {
        @Override
        public String getReportFieldNameSuffix() {
            return ReportConstants.foregroundSuffix;
        }
    };

    public String getReportFieldNameSuffix() {
        throw new UnsupportedOperationException();
    }
}
