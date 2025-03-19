package it.gov.pagopa.self.expense.utils.excel;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
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

    public byte[] genExcel(List<String> headerColumns, List<List<String>> rowValues) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Export");

            Row header = sheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();

            headerStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont font = ((XSSFWorkbook) workbook).createFont();
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            headerStyle.setFont(font);

            Cell headerCell = null;
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
            Row row = null;
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

    public void writeExcel(List<String> headerColumns, List<List<String>> rowValues, String filePath) throws IOException {

        byte[] binaryReport = genExcel(headerColumns, rowValues);

        // Specifica il percorso del file
        //String filePath = "C:\\IdPay\\tmp\\report.xlsx";

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            fileOut.write(binaryReport);
        }
    }

}