/*
 */
package keboola.ftp.extractor.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.com.bytecode.opencsv.CSVReader;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class CsvFileMerger {

    public static void mergeFiles(Collection<String> fileNames, String srcFolderPath, String mergedPath, String mergedName, char separator, char enclosure) throws MergeException {

        String headerLine = "";

        //create output file
        File folder = new File(mergedPath);
        // if the directory does not exist, create it
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File outFile = new File(mergedPath + File.separator + mergedName);

        int i = 0;
        FileChannel out = null;
        FileOutputStream fout = null;

        try {
            //validate merged csv files
            dataStructureMatch(fileNames, srcFolderPath, separator, enclosure);
        } catch (MergeException ex) {
            throw ex;
        }

        char[] headerChars;
        for (String fName : fileNames) {
            FileInputStream fis = null;
            String fPath = srcFolderPath + File.separator + fName;
            try {
                File file = new File(fPath);
                //retrieve file header
                fis = new FileInputStream(file);
//                reader = new BufferedReader(new InputStreamReader(fis));

                headerChars = readLineWithNL(fis);
                headerLine = new String(headerChars);

                if (headerChars == null) {
                    continue;
                }
                //write header from first file and retrieve filechannel 
                if (i == 0) {
                    fout = new FileOutputStream(outFile);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout));
                    bw.write(headerLine);
//                    bw.newLine();
                    bw.flush();
                    out = fout.getChannel();

                }
                //write to outFile using NIO
                FileChannel in = fis.getChannel();
                long pos = 0;
                //set position according to header (first run is set by writer)

                pos = in.position() - 1;//+2 because of NL character

                for (long p = pos, l = in.size(); p < l;) {
                    p += in.transferTo(p, l - p, out);
                }

                i++;
            } catch (FileNotFoundException ex) {

                throw new MergeException("File not found. " + ex.getMessage());
            } catch (IOException ex) {

                throw new MergeException("Error merging files. " + ex.getMessage());
            } finally {
                try {
//                    reader.close();
                    fis.close();

                } catch (IOException ex) {
                    Logger.getLogger(CsvFileMerger.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    /**
     * Validates the structure of the merged csv files.
     *
     * @param fileNames
     * @param folderPath
     * @param separator
     * @param enclosure
     * @return Returns true if the data structure of all given files matches.
     * Throws an user exception (MergeException) otherwise.
     * @throws MergeException
     */
    public static boolean dataStructureMatch(Collection<String> fileNames, String folderPath, char separator, char enclosure) throws MergeException {
    	if (fileNames == null || fileNames.isEmpty()) {
    		return true;
    	}
        String[] headers = null;
        String[] refHeader = {""};
        String refFileName = "";
        String headerLine = "";
        String currFile = "";

        BufferedReader reader;
        FileInputStream fis;
        boolean firstRun = true;
        try {

            for (String fname : fileNames) {
                String fPath = folderPath + File.separator + fname;
                currFile = fPath;

                File csvFile = new File(fPath);
                FileReader freader = new FileReader(csvFile);
                CSVReader csvreader = new CSVReader(freader, separator, enclosure);
                headers = csvreader.readNext();
                if (headers != null) {

                    refFileName = fname;
                    refHeader = headers.clone();
                    //get header line
                    fis = new FileInputStream(fPath);
                    reader = new BufferedReader(new InputStreamReader(fis));
                    headerLine = reader.readLine();
                    reader.close();

                    if (!headerEquals(headers, refHeader)) {
                        throw new MergeException("Data structure of downloaded file: " + fname + " is different than first file: " + refFileName + ", cannot upload to a single SAPI table!");
                    }

                } else {
                    throw new MergeException("Error reading csv file header: " + currFile);
                }

                csvreader.close();
            }
            if (headerLine.equals("")) {
                throw new MergeException("Zero length header in csv file!");
            }
            return true;

        } catch (FileNotFoundException ex) {
            throw new MergeException("CSV file not found. " + currFile + " " + ex.getMessage());
        } catch (IOException ex) {
            throw new MergeException("Error reading csv file: " + currFile + " " + ex.getMessage());
        }
    }

    private static boolean headerEquals(String[] h1, String[] h2) {
        if (h1.length != h2.length) {
            return false;
        }
        for (int i = 0; i < h1.length; i++) {
            if (!h1[i].equals(h2[i])) {
                return false;
            }
        }
        return true;
    }

    private static final boolean isNL(int character) {
        if ((character == -1)) {
            return false;
        } else {
            return ((((char) character == '\n')
                    || ((char) character == '\r')));
        }
    }

    private static char[] readLineWithNL(FileInputStream in) {
        try {
            int hLen = 0;

            ArrayList<Character> chars = new ArrayList();
            int ch = in.read();
            chars.add((char) ch);
            while (!isNL(ch)) {
                ch = in.read();
                chars.add((char) ch);
            }
            boolean isNl = true;
            while (isNl) {
                ch = in.read();
                if (isNL(ch)) {
                    chars.add((char) ch);
                    isNl = true;
                } else {
                    isNl = false;
                }
                hLen++;
            }
            char[] charArray = new char[chars.size()];
            for (int i = 0; i < chars.size(); i++) {
                charArray[i] = chars.get(i);
            }
            return charArray;//in.getChannel().position() - 1;
        } catch (IOException ex) {
            Logger.getLogger(CsvFileMerger.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

}
