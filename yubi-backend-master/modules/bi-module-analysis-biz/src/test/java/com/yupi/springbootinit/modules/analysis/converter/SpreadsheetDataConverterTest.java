package com.yupi.springbootinit.modules.analysis.converter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/** 表格转换器测试，不访问数据库、Redis、RabbitMQ 或真实 AI。 */
class SpreadsheetDataConverterTest {
    private final SpreadsheetDataConverter converter = new SpreadsheetDataConverter();

    /** 测试 CSV 能够转换，并且空单元格不会造成列错位。 */
    @Test
    void shouldConvertCsvAndKeepEmptyCells() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "地点,数量,金额\n北京,,100\n".getBytes());
        String result = converter.convert(file);
        assertTrue(result.contains("北京,,100"));
    }

    /** 测试不支持的文件类型会被拒绝。 */
    @Test
    void shouldRejectUnsupportedSuffix() {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain", "a".getBytes());
        assertThrows(IllegalArgumentException.class, () -> converter.convert(file));
    }

    /** 测试超过最大行数时直接拒绝，不静默截断。 */
    @Test
    void shouldRejectTooManyRows() {
        StringBuilder csv = new StringBuilder("a\n");
        for (int i = 0; i <= SpreadsheetDataConverter.MAX_ROWS; i++) csv.append(i).append('\n');
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", csv.toString().getBytes());
        assertThrows(IllegalArgumentException.class, () -> converter.convert(file));
    }
}
