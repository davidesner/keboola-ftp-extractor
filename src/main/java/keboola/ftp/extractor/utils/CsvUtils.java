/*
 */
package keboola.ftp.extractor.utils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class CsvUtils {

    /**
     * Validates the structure of the merged csv files.
     *
     * @param mFiles List of MasterFiles to check
     * @return Returns true if the data structure of all given files matches.
     * Throws an user exception (Exception) otherwise.
     * @throws Exception
     */
    public static boolean dataStructureMatch(Collection<String> fileNames, String folderPath) throws Exception {
        String[] headers = null;
        String headerLine = "";
        String currFile = "";
        BufferedReader reader;
        FileInputStream fis;
        boolean firstRun = true;
        try {
            int maxHeaderSize = 0;
            for (String fname : fileNames) {
                String fPath = folderPath + File.separator + fname;
                currFile = fPath;

                File csvFile = new File(fPath);
                FileReader freader = new FileReader(csvFile);
                CSVReader csvreader = new CSVReader(freader, '\t', CSVWriter.NO_QUOTE_CHARACTER);
                headers = csvreader.readNext();
                if (headers != null) {
                    if (headers.length != maxHeaderSize) {

                        maxHeaderSize = headers.length;
                        //get header line
                        fis = new FileInputStream(fPath);
                        reader = new BufferedReader(new InputStreamReader(fis));
                        headerLine = reader.readLine();
                        reader.close();
                        if (!firstRun) {
                            throw new Exception("Data structure of downloaded files within is different, cannot upload to a single SAPI table!");
                        }
                        firstRun = false;

                    }

                } else {
                    throw new Exception("Error reading csv file header: " + currFile);
                }
                csvreader.close();
            }
            if (maxHeaderSize == 0 || headerLine.equals("")) {
                throw new Exception("Zero length header in csv file!");
            }
            return true;

        } catch (FileNotFoundException ex) {
            throw new Exception("CSV file not found. " + currFile + " " + ex.getMessage());
        } catch (IOException ex) {
            throw new Exception("Error reading csv file: " + currFile + " " + ex.getMessage());
        }
    }
}
