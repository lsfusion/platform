package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.print.ReportFieldExtraType;
import lsfusion.interop.form.property.PropertyReadType;

public enum PropertyDrawExtraType {
    CAPTION {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.HEADER;
        }
        
        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CAPTION;
        }
        
        @Override
        public String getText() {
            return "{logics.property.caption}";    
        }
    }, 
    FOOTER {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.FOOTER;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.FOOTER;
        }
        
        @Override
        public String getText() {
            return "{logics.property.footer}";
        }
    }, 
    SHOWIF {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.SHOWIF;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.SHOWIF;
        }
        
        @Override
        public String getText() {
            return "SHOWIF";
        }
    }, 
    READONLYIF {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.READONLY;
        }

        @Override
        public String getText() {
            return "{logics.property.readonly}";
        }
    }, 
    BACKGROUND {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.BACKGROUND;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CELL_BACKGROUND;
        }
        
        @Override
        public String getText() {
            return "{logics.background}";
        }
    }, 
    FOREGROUND {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.FOREGROUND;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CELL_FOREGROUND;
        }

        @Override
        public String getText() {
            return "{logics.foreground}";
        }
    },
    IMAGE {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.IMAGE;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.IMAGE;
        }

        @Override
        public String getText() {
            return "{logics.image}";
        }
    };

    public static final ImSet<PropertyDrawExtraType> extras = SetFact.toSet(values());

    public ReportFieldExtraType getReportExtraType() {
        throw new UnsupportedOperationException();
    }

    public byte getPropertyReadType() {
        throw new UnsupportedOperationException();
    }
    
    public String getText() {
        return "";
    }
}
