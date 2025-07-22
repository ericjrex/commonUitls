package net.gmcc.dg.aiask.module.ai.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;

@Slf4j
public class ExcelParser {
    private static String path = "C:\\Users\\ericj\\Desktop\\人员信息示例.xlsx";

    public static void main(String[] args) {
        path = "C:\\Users\\ericj\\Desktop\\人员信息.xlsx";
        //path = "C:\\Users\\ericj\\Desktop\\人员信息示例3.xlsx";
        getExcelContent(path);
    }

    public static void getExcelContent(String filePath) {
        FileInputStream fis = null;
        Workbook workbook = null;
        try {
            fis = new FileInputStream(filePath);
            workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            int headerRowCount = getDynamicHeaderRowCount(sheet);
            int columnCount = sheet.getRow(0).getLastCellNum();

            // 1. 构造合并单元格映射
            Map<String, String> mergedMap = new HashMap<>();
            Map<String, List<String>> mergedGroupMap = new HashMap<>();
            for (CellRangeAddress range : sheet.getMergedRegions()) {
                String mainValue = getCellValue(sheet.getRow(range.getFirstRow()).getCell(range.getFirstColumn()));
                for (int r = range.getFirstRow(); r <= range.getLastRow(); r++) {
                    String key =range.formatAsString() ;
                    List<String> groupKeys = mergedGroupMap.computeIfAbsent(key, k -> new ArrayList<>());
                    for (int c = range.getFirstColumn(); c <= range.getLastColumn(); c++) {
                        mergedMap.put(r + "_" + c, mainValue);
                        groupKeys.add(r + "_" + c);
                    }
                }
            }

            // 2. 生成最终列标题（支持多级）
            List<String> headers = new ArrayList<>();
            for (int col = 0; col < columnCount; col++) {
                List<String> parts = new ArrayList<>();
                for (int row = 0; row < headerRowCount; row++) {
                    Cell cell = sheet.getRow(row).getCell(col);
                    // 判断当前单元格是否是合并单元格的一部分
                    List<String> mergeGroups = null;
                    for (CellRangeAddress range : sheet.getMergedRegions()) {
                        if (range.isInRange(row, col)) {
                            mergeGroups = mergedGroupMap.get(range.formatAsString());
                        }
                    }
                    
                    String value = getCellValue(cell);
                    if ((value == null || value.isEmpty()) && mergedMap.containsKey(row + "_" + col)) {
                        value = mergedMap.get(row + "_" + col);
                    }
                    if (value != null && !value.isBlank()) {
                        if(mergeGroups != null){
                            String first = mergeGroups.get(0);
                            int firstRowIndex = Integer.parseInt(first.split("_")[0]);
                            if(row != firstRowIndex){
                                continue;
                            }
                        }
                        parts.add(value.trim());
                    }
                }
                headers.add(String.join(".", parts));
            }

            // 3. 读取每一行数据
            for (int rowIndex = headerRowCount; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int col = 0; col < columnCount; col++) {
                    Cell cell = row.getCell(col);
                    String value = getCellValue(cell);
                    if ((value == null || value.isEmpty()) && mergedMap.containsKey(rowIndex + "_" + col)) {
                        value = mergedMap.get(rowIndex + "_" + col);
                    }
                    String columnLabel = convertToColumnLabel(col);
                    String mergedHeader = StringUtils.isNotEmpty(headers.get(col)) ? headers.get(col) : "列" + columnLabel;
                    rowMap.put(mergedHeader, value);
                }

                System.out.println(JSONObject.toJSONString(rowMap));
            }
        } catch (Exception e) {
            log.error("解析Excel文件失败：{}", e.getMessage());
        } finally {
            try {
                assert fis != null;
                fis.close();
                assert workbook != null;
                workbook.close();
            } catch (Exception e) {
                log.error("关闭文件输入流失败：{}", e.getMessage());
            }
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static String convertToColumnLabel(int colIndex) {
        StringBuilder label = new StringBuilder();
        colIndex++; // Excel 列从 1 开始，而索引从 0 开始
        while (colIndex > 0) {
            colIndex--;
            char c = (char) ('A' + (colIndex % 26));
            label.insert(0, c);
            colIndex = colIndex / 26;
        }
        return label.toString();
    }

    private static int getDynamicHeaderRowCount(Sheet sheet) {
        int headerRowCount = 0;
        int columnCount = sheet.getRow(0).getLastCellNum();

        outer:
        while (true) {
            if (headerRowCount >= sheet.getLastRowNum() + 1) break;
            Row row = sheet.getRow(headerRowCount);
            if (row == null) break;

            for (int col = 0; col < columnCount; col++) {
                for (CellRangeAddress cellAddresses : sheet.getMergedRegions()) {
                    if (cellAddresses.getFirstRow() == headerRowCount) {
                        int rangeIndex = cellAddresses.getLastRow() - cellAddresses.getFirstRow();
                        if (rangeIndex > 0 && rangeIndex > headerRowCount) {
                            headerRowCount = rangeIndex;// 当前是合并区域的起始行
                            continue outer; // 继续判断下一行是否也是合并表头
                        }
                    }
                }
            }
            break; // 如果当前行没有合并单元格，则表头结束
        }
        return headerRowCount == 0 ? 1 : headerRowCount + 1; // 默认至少一行表头
    }

}
