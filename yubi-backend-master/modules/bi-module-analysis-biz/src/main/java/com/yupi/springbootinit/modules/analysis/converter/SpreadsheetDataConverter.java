package com.yupi.springbootinit.modules.analysis.converter;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 将 CSV/XLSX 转换为受限的标准 CSV；超限时拒绝，不静默截断。 */
@Component
public class SpreadsheetDataConverter {
    public static final int MAX_COLUMNS = 50;
    public static final int MAX_ROWS = 1000;
    public static final int MAX_CELL_LENGTH = 1000;
    public static final int MAX_TEXT_LENGTH = 100_000;

    /** 读取第一个非空工作表或 CSV，并保留空单元格位置。 */
    public String convert(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("文件不能为空");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) return convertCsv(file);
            if (name.endsWith(".xlsx")) return convertXlsx(file);
            throw new IllegalArgumentException("仅支持 CSV 或 XLSX 文件");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("表格读取失败", e);
        }
    }

    private String convertCsv(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            List<List<String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (String value : record) row.add(value == null ? "" : value);
                rows.add(row);
            }
            return render(rows);
        }
    }

    private String convertXlsx(MultipartFile file) throws Exception {
        List<Map<Integer, String>> rows = EasyExcel.read(file.getInputStream())
                .excelType(ExcelTypeEnum.XLSX).sheet().headRowNumber(0).doReadSync();
        List<List<String>> normalized = new ArrayList<>();
        for (Map<Integer, String> source : rows) {
            int width = source.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
            List<String> row = new ArrayList<>();
            for (int i = 0; i < width; i++) row.add(source.get(i) == null ? "" : source.get(i));
            normalized.add(row);
        }
        return render(normalized);
    }

    private String render(List<List<String>> rows) {
        if (rows.isEmpty()) throw new IllegalArgumentException("表格不能为空");
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns == 0 || columns > MAX_COLUMNS) throw new IllegalArgumentException("列数超过限制：最多 " + MAX_COLUMNS + " 列");
        if (rows.size() - 1 > MAX_ROWS) throw new IllegalArgumentException("数据行数超过限制：最多 " + MAX_ROWS + " 行");
        StringBuilder output = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < columns; i++) {
                String value = i < row.size() && row.get(i) != null ? row.get(i) : "";
                if (value.length() > MAX_CELL_LENGTH) throw new IllegalArgumentException("单元格内容超过限制");
                if (i > 0) output.append(',');
                output.append(escape(value));
            }
            output.append('\n');
            if (output.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException("转换后的文本超过限制");
        }
        return output.toString();
    }

    private String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
