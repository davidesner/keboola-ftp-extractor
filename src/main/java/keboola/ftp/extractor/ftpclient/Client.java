package keboola.ftp.extractor.ftpclient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

/**
 * FTP Client for handling FTP connections
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class Client {

    private String userName;
    private String pass;
    private String url;
    private FTPClient ftpClient;

    /**
     *
     * @param userName
     * @param pass
     * @param url
     */
    public Client(String userName, String pass, String url) {
        this.userName = userName;
        this.pass = pass;
        this.url = url;
        ftpClient = new FTPClient();
        //set buffer size
        this.ftpClient.setBufferSize(1024 * 1024);
    }

    /**
     * Establishes connection
     *
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public boolean connect() throws SocketException, IOException {
        ftpClient.connect(url);
        //login to server
        if (!ftpClient.login(this.userName, this.pass)) {
            ftpClient.logout();
            return false;
        }
        int reply = ftpClient.getReplyCode();

        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            return false;
        }
        //passive mode
        ftpClient.enterLocalPassiveMode();
        return true;
    }

    public void disconnect() throws IOException {
        ftpClient.disconnect();
    }

    /**
     * Renews connection if needed
     *
     * @throws IOException
     */
    public void reconnectIfNeeded() throws IOException {
        if (!ftpClient.isConnected()) {
            connect();
        }
    }

    /**
     * Methods downloads all CSV files from remote directory.
     *
     * <ul>
     * <li>If <i>changedSince</i> parameter is specified only files older
     * timestamp are downloaded.</li>
     * <li>If <i>prevImportedFiles</i> parameter is specified all files are
     * downloaded except those which timestamp has chagned.</li>
     * </ul>
     *
     * @param remoteFolder - existing remote folder.
     * @param localFolderPath
     * @param prevImportedFiles
     * @param changedSince
     * @return Map of downloaded file names and its' timestamps
     * @throws FtpException
     */
    public Map<String, Date> downloadAllNewCsvFiles(String remoteFolder, String localFolderPath, Map<String, Date> prevImportedFiles, Date changedSince) throws FtpException {
        //set ftp file filter accordingly
        FTPFileFilter filter;
        /*
            if(prevImportedFiles!=null){
                filter=FtpFilters.FILES_CHANGED_SINCE(prevImportedFiles);
            }*/
        if (changedSince != null) {
            filter = FtpFilters.CSVFILES_CHANGED_SINCE(changedSince);
        } else {
            filter = FtpFilters.JUSTCSVFILES;
        }
        //get list of files to download

        return downloadAllFilesByFilter(remoteFolder, localFolderPath, filter);
    }

    /**
     *
     * @param remoteFolder
     * @param localFolderPath
     * @param filter
     * @return
     * @throws FtpException
     */
    private Map<String, Date> downloadAllFilesByFilter(String remoteFolder, String localFolderPath, FTPFileFilter filter) throws FtpException {
        Map<String, Date> downloadedFiles = new HashMap();

        try {
            reconnectIfNeeded();
        } catch (IOException ex) {
            throw new FtpException("Error connecting to ftp server. " + ex.getMessage());
        }
        try {
            ftpClient.changeWorkingDirectory(remoteFolder);
            int returnCode = ftpClient.getReplyCode();
            if (returnCode == 550) {
                throw new FtpException("Remote folder: '" + remoteFolder + "' does not exist or is not a folder!");
            }

            //get list of files to download
            FTPFile[] files = ftpClient.listFiles(null, filter);

            for (FTPFile file : files) {
                downloadFile(ftpClient.printWorkingDirectory(), file.getName(), localFolderPath);
                downloadedFiles.put(file.getName(), file.getTimestamp().getTime());
            }

        } catch (IOException ex) {
            throw new FtpException("Error downloading files from folder: " + remoteFolder + ". " + ex.getMessage());
        }
        return downloadedFiles;
    }

    /**
     * Download all files in specified folder with prefix
     *
     * @param remoteFolder
     * @param localFolderPath
     * @param prefix
     * @param changedSince
     * @return
     * @throws FtpException
     */
    public Map<String, Date> downloadAllNewCsvFilesByPrefix(String remoteFolder, String localFolderPath, String prefix, Date changedSince) throws FtpException {
        //set ftp file filter accordingly
        FTPFileFilter filter;

        if (changedSince != null) {
            filter = FtpFilters.CSVFILES_WITH_PREFIX_CHANGED_SINCE(changedSince, prefix);
        } else {
            filter = FtpFilters.JUSTCSVFILES_WITH_PREFIX(prefix);
        }

        return downloadAllFilesByFilter(remoteFolder, localFolderPath, filter);
    }

    /**
     * Downloads a single file to specified location.
     *
     * @param remoteFolder
     * @param fileName
     * @param localFilePath
     * @return
     * @throws FtpException
     */
    public Map<String, Date> downloadFile(String remoteFolder, String fileName, String localFilePath) throws FtpException {
        FileOutputStream fos = null;
        Map<String, Date> downloadedFiles = new HashMap();
        try {
            reconnectIfNeeded();
            if (remoteFolder != null) {
                ftpClient.changeWorkingDirectory(remoteFolder);
            }
            int returnCode = ftpClient.getReplyCode();
            if (returnCode == 550) {
                throw new FtpException("Remote folder does not exist!");
            }

            fos = new FileOutputStream(localFilePath + File.separator + fileName);

            if (ftpClient.changeWorkingDirectory(fileName)) {
                throw new FtpException("Unable to download this file: " + fileName + ". It is a directory.");
            }
            FTPFile[] files = ftpClient.listFiles(fileName);

            this.ftpClient.retrieveFile(fileName, fos);
            downloadedFiles.put(fileName, files[0].getTimestamp().getTime());
        } catch (IOException ex) {
            throw new FtpException("Error downloading file: " + fileName + ". " + ex.getMessage());

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {

                }
            }
        }
        return downloadedFiles;

    }
}
