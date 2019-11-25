package com.higgs.system.tts;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;

public class ExcelFileOperate {
    private static final String TAG = "ExcelFileOperate";

    private int special;

    public enum FileLocation{
        INNER,
        PATH,
    }

    public Map<Integer, Map<Integer, String>> readExcelFile(String filePath, FileLocation fileLocation){
        Map<Integer,Map<Integer, String>> rowline = null;
        String print = "";
        InputStream stream = null;
        try {
            if(fileLocation == FileLocation.PATH ){
                File excelFile = new File(filePath);
                if(!excelFile.exists()){
                    return null;
                }
                stream = new FileInputStream(new File(filePath));
            }else if(fileLocation == FileLocation.INNER ){
                stream = TimingAlarmActivity.mContext.getAssets().open(filePath);
            }
            if(stream != null) {
                rowline = new HashMap<Integer, Map<Integer, String>>();
                XSSFWorkbook workbook = new XSSFWorkbook(stream);
                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowsCount = sheet.getPhysicalNumberOfRows();
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper()
                        .createFormulaEvaluator();
                for (int r = 0; r < rowsCount; r++) {
                    Row row = sheet.getRow(r);
                    int cellsCount = row.getPhysicalNumberOfCells();
                    Map<Integer, String> cellLine = new HashMap<Integer, String>();

                    for (int c = 0; c < cellsCount; c++) {
                        String value = getCellAsString(row, c, formulaEvaluator);
                        if (r == 0) {
                            String[] array = value.split("=");
                            if (array[0].equals("TL")) {
                                special = c;
                            }
                        }
                        cellLine.put(c, value);
                        String cellInfo = "[" + r + ":" + c + "]=" + value + " ";
                        print += cellInfo;
                    }
                    rowline.put(r, cellLine);
                    Log.d(TAG, print);
                    print = "";
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return rowline;
    }

    private String getCellAsString(Row row, int c, FormulaEvaluator formulaEvaluator) {
        String value = "";
        try {
            Cell cell = row.getCell(c);
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            if(cellValue != null) {
                switch (cellValue.getCellType()) {
                    case Cell.CELL_TYPE_BOOLEAN:
                        value = ""+cellValue.getBooleanValue();
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        double numericValue = cellValue.getNumberValue();
                        if(HSSFDateUtil.isCellDateFormatted(cell)) {
                            double date = cellValue.getNumberValue();
                            SimpleDateFormat formatter;
                            if( c == special ){
                                formatter = new SimpleDateFormat("mm:ss");
                            }else{
                                formatter = new SimpleDateFormat("HH:mm");
                            }
                            value = formatter.format(HSSFDateUtil.getJavaDate(date));
                        } else {
                            value = ""+numericValue;
                        }
                        break;
                    case Cell.CELL_TYPE_STRING:
                        value = "" + cellValue.getStringValue();
                        break;
                    default:
                }
            }
        } catch (NullPointerException e) {
            /** proper error handling should be here */
            e.printStackTrace();
        }
        return value;
    }

}
