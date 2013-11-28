package lsfusion.base;

import org.junit.Test;

import java.math.BigDecimal;

import static junit.framework.Assert.*;

public class BaseUtilsTest {
    private final static char UNBREAKABLE_SPACE = '\u00a0';

    @Test
    public void testBigDecimalToString() throws Exception {
        assertEquals(spaced("0,123"), BaseUtils.bigDecimalToString(new BigDecimal(".123123")));
        assertEquals(spaced("2 123 123,123"), BaseUtils.bigDecimalToString(new BigDecimal("2123123.1230000")));
        assertEquals(spaced("2 123 123"), BaseUtils.bigDecimalToString(new BigDecimal("2123123.0000")));
        assertEquals(spaced("0,01"), BaseUtils.bigDecimalToString(new BigDecimal("0.01")));
        assertEquals(spaced("1,001"), BaseUtils.bigDecimalToString(new BigDecimal("1.001")));
        assertEquals(spaced("1 000"), BaseUtils.bigDecimalToString(new BigDecimal("1e3")));
        assertEquals(spaced("4 564,56"), BaseUtils.bigDecimalToString(new BigDecimal("456456e-2")));
        assertEquals(spaced("0"), BaseUtils.bigDecimalToString(new BigDecimal("0.000")));
        assertEquals(spaced("123 123"), BaseUtils.bigDecimalToString(new BigDecimal("123123.")));

        assertEquals(spaced("123 123,1233"), BaseUtils.bigDecimalToString("#,##0.####", new BigDecimal("123123.1233")));
    }

    private String spaced(String s) {
        return s.replace(' ', UNBREAKABLE_SPACE);
    }
}
