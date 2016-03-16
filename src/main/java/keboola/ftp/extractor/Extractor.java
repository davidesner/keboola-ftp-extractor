/*
 */
package keboola.ftp.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import keboola.ftp.extractor.config.FtpMapping;
import keboola.ftp.extractor.config.KBCConfig;
import keboola.ftp.extractor.config.YamlConfigParser;
import keboola.ftp.extractor.config.tableconfig.ManifestBuilder;
import keboola.ftp.extractor.config.tableconfig.ManifestFile;
import keboola.ftp.extractor.ftpclient.Client;
import keboola.ftp.extractor.ftpclient.FtpException;
import keboola.ftp.extractor.state.VisitedFolder;
import keboola.ftp.extractor.state.VisitedFoldersList;
import keboola.ftp.extractor.state.YamlStateWriter;
import keboola.ftp.extractor.utils.CsvUtils;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class Extractor {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.print("No parameters provided.");
            System.exit(1);
        }
        String dataPath = args[0];

        String outTablesPath = dataPath + File.separator + "out" + File.separator + "tables"; //parse config

        KBCConfig config = null;

        File confFile = new File(args[0] + File.separator + "config.yml");
        if (!confFile.exists()) {
            System.out.println("config.yml does not exist!");
            System.err.println("config.yml does not exist!");
            System.exit(1);
        }
        //Parse config file
        try {
            if (confFile.exists() && !confFile.isDirectory()) {
                config = YamlConfigParser.parseFile(confFile);
            }
        } catch (Exception ex) {
            System.out.println("Failed to parse config file");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        if (!config.validate()) {
            System.out.println(config.getValidationError());
            System.err.println(config.getValidationError());
            System.exit(1);
        }
        //retrieve stateFile
        File stateFile = new File(dataPath + File.separator + "out" + File.separator + "state.yml");
        VisitedFoldersList visitedFolders = null;
        if (stateFile.exists()) {
            try {
                visitedFolders = (VisitedFoldersList) YamlConfigParser.parseFile(stateFile, VisitedFoldersList.class);
            } catch (IOException ex) {
                Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //get user mappings
        List<FtpMapping> mappings = config.getParams().getMappings();

        Client ftpClient = new Client(config.getParams().getUser(), config.getParams().getPass(), config.getParams().getFtpUrl());
        try {
            ftpClient.connect();
        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        List<VisitedFolder> visitedFoldersCurrent = new ArrayList();
        Map<String, Date> retrievedFiles = null;
        Map<String, Date> prevImpFiles = null;
        Date lastRun = null;
        Date currDate;
        /*Download all files and generate manifests*/
        System.out.println("Downloading files...");
        int count = 0;
        try {
            for (FtpMapping mapping : mappings) {
                //set download parameters according to previous runs
                if (visitedFolders != null) {
                    VisitedFolder vf = visitedFolders.getFolderByPath(mapping.getFtpPath());
                    prevImpFiles = vf.getFileMap();
                    lastRun = vf.getLastRun();
                }

                // TODO: handle other cases, i.e. isFolder=0,prefixes, dateFrom,dateTo
                if (mapping.isFolder()) {
                    //download all csv files in case of whole folder
                    if (mapping.getPrefix() != null) {//in case of files by prefix
                        retrievedFiles = ftpClient.downloadAllNewCsvFilesByPrefix(mapping.getFtpPath(), outTablesPath, mapping.getPrefix(), lastRun);
                    } else {//all files in folder
                        retrievedFiles = ftpClient.downloadAllNewCsvFiles(mapping.getFtpPath(), outTablesPath, null, lastRun);
                    }
                } else {
                    File f = new File(mapping.getFtpPath());

                    retrievedFiles = ftpClient.downloadFile(f.getParent(), f.getName(), outTablesPath);

                }

                count += retrievedFiles.size();

                //create historical state record for current folder
                currDate = new Date();
                VisitedFolder f = new VisitedFolder(mapping.getFtpPath(), retrievedFiles, currDate);
                visitedFoldersCurrent.add(f);

                //build manifest files
                for (String fileName : retrievedFiles.keySet()) {
                    ManifestFile manFile = new ManifestFile(mapping.getSapiPath(), mapping.isIncremental(), mapping.getPkey(), mapping.getDelimiter(), mapping.getEnclosure());
                    try {
                        ManifestBuilder.buildManifestFile(manFile, outTablesPath, fileName);
                    } catch (IOException ex) {
                        System.out.println("Failed to build manifest file. " + ex.getMessage());
                        System.err.println("Failed to build manifest file. " + ex.getMessage());
                        System.exit(2);
                    }
                }

                try {
                    //check for validity of composed csvs. Doesnt stop processing yet.
                    CsvUtils.dataStructureMatch(retrievedFiles.keySet(), outTablesPath);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (FtpException ex) {
            System.out.println("Failed to download files. " + ex.getMessage());
            System.err.println("Failed to download files. " + ex.getMessage());
            System.exit(ex.getSeverity());
        } finally {
            try {
                ftpClient.disconnect();
            } catch (IOException ex) {
                Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /*Write state file*/
        try {
            YamlStateWriter.writeStateFile(dataPath + File.separator + "out" + File.separator + "state.yml", visitedFoldersCurrent);
        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println(count + " files successfuly downloaded.");

    }

}
