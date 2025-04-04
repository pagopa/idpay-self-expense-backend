package it.gov.pagopa.self.expense.utils.excel;

import java.io.*;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelPOIHelper {



    public byte[] genExcel(List<String> headerColumns, List<List<String>> rowValues) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Export");

            Row header = sheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();

            headerStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont font = workbook.createFont();
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            headerStyle.setFont(font);

            Cell headerCell;
            int i=0;
            for(String headername : headerColumns) {
                headerCell = header.createCell(i);
                headerCell.setCellValue(headername);
                headerCell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);

                i++;
            }

            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);

            int r = 1;
            Row row;
            //loop on rows
            for(List<String> rowValueList : rowValues) {
                row = sheet.createRow(r);

                int j=0;
                for(String rowValue : rowValueList) {
                    Cell cell = row.createCell(j);
                    //check number cell
                    if (j == 3 || j == 4) {
                        cell.setCellValue(Integer.parseInt(rowValue));
                    } else if(j==6){
                        cell.setCellValue(Double.parseDouble(rowValue));

                    }else{
                        cell.setCellValue(rowValue);

                    }
                    cell.setCellStyle(style);
                    j++;
                }
                r++;
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

}