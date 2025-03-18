package it.gov.pagopa.self.expense.utils.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelPOIHelper {

    public Map<Integer, List<String>> readExcel(String fileLocation) throws IOException {

        Map<Integer, List<String>> data = new HashMap<>();
        FileInputStream file = new FileInputStream(new File(fileLocation));
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);
        int i = 0;
        for (Row row : sheet) {
            data.put(i, new ArrayList<String>());
            for (Cell cell : row) {
                switch (cell.getCellType()) {
                case STRING:
                    data.get(i)
                        .add(cell.getRichStringCellValue()
                            .getString());
                    break;
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        data.get(i)
                            .add(cell.getDateCellValue() + "");
                    } else {
                        data.get(i)
                            .add((int)cell.getNumericCellValue() + "");
                    }
                    break;
                case BOOLEAN:
                    data.get(i)
                        .add(cell.getBooleanCellValue() + "");
                    break;
                case FORMULA:
                    data.get(i)
                        .add(cell.getCellFormula() + "");
                    break;
                default:
                    data.get(i)
                        .add(" ");
                }
            }
            i++;
        }
        if (workbook != null){
            workbook.close();
        }
        return data;
    }

    public void writeExcel(List<String> headerColumns, List<List<String>> rowValues) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        try {
            Sheet sheet = workbook.createSheet();
            sheet.setColumnWidth(0, 6000);
            sheet.setColumnWidth(1, 4000);



            Row header = sheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();

            //headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            //headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont font = ((XSSFWorkbook) workbook).createFont();
            //font.setFontName("Arial");
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            headerStyle.setFont(font);

            Cell headerCell = null;
            int i=0;
            for(String headername : headerColumns) {
                headerCell = header.createCell(i);
                headerCell.setCellValue(headername);
                headerCell.setCellStyle(headerStyle);
                i++;
            }

            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);

            int r = 1;
            Row row = null;
            //loop on rows
            for(List<String> rowValueList : rowValues) {
                row = sheet.createRow(r);

                int j=0;
                for(String rowValue : rowValueList) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(rowValue);
                    cell.setCellStyle(style);

                    j++;
                    //cell = row.createCell(1);
                    //cell.setCellValue(20);
                    //cell.setCellStyle(style);
                }

                r++;
            }
            File currDir = new File(".");
            String path = "C:\\IdPay\\tmp\\";//currDir.getAbsolutePath();
            String fileLocation = path + "temp.xlsx";

            FileOutputStream outputStream = new FileOutputStream(fileLocation);
            workbook.write(outputStream);
        } finally {
            if (workbook != null) {
               
                    workbook.close();
               
            }
        }
    }

}