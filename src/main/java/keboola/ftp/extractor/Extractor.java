/*
 */
package keboola.ftp.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import keboola.ftp.extractor.config.FtpMapping;
import keboola.ftp.extractor.config.JsonConfigParser;
import keboola.ftp.extractor.config.KBCConfig;
import keboola.ftp.extractor.config.KBCParameters;
import keboola.ftp.extractor.config.tableconfig.ManifestBuilder;
import keboola.ftp.extractor.config.tableconfig.ManifestFile;
import keboola.ftp.extractor.ftpclient.FTPClientBuilder;
import keboola.ftp.extractor.ftpclient.FtpException;
import keboola.ftp.extractor.ftpclient.IFTPClient;
import keboola.ftp.extractor.state.JsonStateWriter;
import keboola.ftp.extractor.state.VisitedFolder;
import keboola.ftp.extractor.state.VisitedFoldersList;
import keboola.ftp.extractor.utils.CsvFileMerger;
import keboola.ftp.extractor.utils.MergeException;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class Extractor {
    
    public static void run(String[] args) throws Exception{
        
        if (args.length == 0) {
            System.out.print("No parameters provided.");
            System.exit(1);
        }
        String dataPath = args[0];
        
        String outTablesPath = dataPath + File.separator + "out" + File.separator + "tables"; //parse config

        KBCConfig config = null;
        
        File confFile = new File(args[0] + File.separator + "config.json");
        if (!confFile.exists()) {
            System.err.println("config.json does not exist!");
            System.exit(1);
        }
        //Parse config file
        try {
            if (confFile.exists() && !confFile.isDirectory()) {            	
                config = JsonConfigParser.parseFile(confFile);              
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
        File stateFile = new File(dataPath + File.separator + "in" + File.separator + "state.json");
        VisitedFoldersList visitedFolders = null;
        if (stateFile.exists()) {
        	
            try {
                visitedFolders = (VisitedFoldersList) JsonConfigParser.parseFile(stateFile, VisitedFoldersList.class);
            } catch (IOException ex) {
                Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("State file does not exist. (first run?)");
        }

        //get user mappings
        KBCParameters confParams = config.getParams();
        List<FtpMapping> mappings = confParams.getMappings();
        IFTPClient ftpClient = null;
        try {
            ftpClient = FTPClientBuilder.create(confParams.getFtpProtocol(), confParams.getFtpUrl())
                    .setPort(confParams.getPort())
                    .setUser(confParams.getUser())
                    .setPass(confParams.getPass())
                    .setHostTz(TimeZone.getTimeZone(confParams.getTimezone())).build();
        } catch (FtpException ex) {
            System.err.println("Failed to create FTP client. " + ex.getMessage());
            //ex.printStackTrace();
            System.exit(ex.getSeverity());
        }
        
        try {
            ftpClient.connect();
        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Failed to create FTP client. " + ex.getMessage());
            //ex.printStackTrace();
            System.exit(1);
        }
        
        List<VisitedFolder> visitedFoldersCurrent = new ArrayList();
        Map<String, Date> retrievedFiles = null;
        //Map<String, Date> prevImpFiles = null;
        Date lastRun = null;
        Date currDate;
        /*Download all files and generate manifests*/
        System.out.println("Downloading files...");
        int count = 0;
        int index = 0;
        try {
            for (FtpMapping mapping : mappings) {
                index++;
                //set download parameters according to previous runs
                if (visitedFolders != null) {
                    try {
                        if (visitedFolders.getVisitedFolders() != null) {
                            VisitedFolder vf = visitedFolders.getFolderByPath(mapping.getFtpPath());
                            if (vf != null && mapping.equals(vf.getLastMapping())) {
                                //prevImpFiles = vf.getFileMap();
                                lastRun = vf.getLastRun();
                            }
                        }
                    } catch (NullPointerException ex) {
                        System.out.println("No matching state.");
                    }
                }

                // TODO: handle other cases, i.e. isFolder=0,prefixes, dateFrom,dateTo
                if (mapping.isFolder()) {
                    //download all csv files in case of whole folder
                    if (mapping.getPrefix() != null) {//in case of files by prefix
                        retrievedFiles = ftpClient.downloadAllNewCsvFilesByPrefix(mapping.getFtpPath(), mapping.getExtension(), outTablesPath, mapping.getPrefix(), lastRun);
                    } else {//all files in folder
                        retrievedFiles = ftpClient.downloadAllNewCsvFiles(mapping.getFtpPath(), mapping.getExtension(), outTablesPath, null, lastRun);
                    }
                } else {
                    File f = new File(mapping.getFtpPath());
                    retrievedFiles = ftpClient.downloadFile(f.getParent(), f.getName(), outTablesPath);
                }
                
                count += retrievedFiles.size();

                //create historical state record for current folder
                currDate = new Date();
                VisitedFolder f = new VisitedFolder(mapping.getFtpPath(), retrievedFiles, currDate, mapping);
                visitedFoldersCurrent.add(f);
                
                if (count == 0) {
                    continue;
                }
                try {
                    CsvFileMerger.dataStructureMatch(retrievedFiles.keySet(), outTablesPath, mapping.getDelimiter().charAt(0), mapping.getEnclosure().charAt(0));
                } catch (MergeException ex) {
                    System.out.println("Failed to merge files. " + ex.getMessage());
                    System.err.println("Failed to merge files. " + ex.getMessage());
                    System.exit(1);
                }
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
                
            }
        } catch (FtpException ex) {
            System.out.println("Failed to download files. " + ex.getMessage());
            System.err.println("Failed to download files. " + ex.getMessage());
            //ex.printStackTrace();
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
            JsonStateWriter.writeStateFile(dataPath + File.separator + "out" + File.separator + "state.json", visitedFoldersCurrent);
        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println(count + " files successfuly downloaded.");
        
    }
    
}
