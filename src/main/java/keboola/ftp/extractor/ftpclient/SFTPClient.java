package keboola.ftp.extractor.ftpclient;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import keboola.ftp.extractor.ftpclient.filters.SFTPfilter;
import keboola.ftp.extractor.ftpclient.filters.SftpFilters;

/**
 * FTP FTPClient for handling FTP connections
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class SFTPClient implements IFTPClient {

    private static final String CURRENT_FOLDER_PATH = "./";
    private String userName;
    private String pass;
    private String url;
    private JSch ftpClient;
    private Session session;
    private ChannelSftp sftpChannel;

    /**
     *
     * @param userName
     * @param pass
     * @param url
     */
    public SFTPClient(String userName, String pass, String url) {
        this.userName = userName;
        this.pass = pass;
        this.url = url;
        ftpClient = new JSch();

    }

    /**
     * Establishes connection
     *
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public boolean connect() throws SocketException, IOException {
        try {

            this.session = ftpClient.getSession(userName, url);
            /*Ignore unknown host keys*/
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.setPassword(pass);
            session.connect();
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            return true;
        } catch (Exception ex) {
            throw new IOException("Unable to connect to sftp repository. " + ex.getMessage());

        }
    }

    public void disconnect() throws IOException {
        session.disconnect();
    }

    /**
     * Renews connection if needed
     *
     * @throws IOException
     */
    public void reconnectIfNeeded() throws IOException {
        if (!session.isConnected() || !sftpChannel.isConnected()) {
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
     * @param extension
     * @param localFolderPath
     * @param prevImportedFiles
     * @param changedSince
     * @return Map of downloaded file names and its' timestamps
     * @throws FtpException
     */
    public Map<String, Date> downloadAllNewCsvFiles(String remoteFolder, String extension, String localFolderPath, Map<String, Date> prevImportedFiles, Date changedSince) throws FtpException {
        //set ftp file filter accordingly
        SFTPfilter filter;
        /*
            if(prevImportedFiles!=null){
                filter=FtpFilters.FILES_CHANGED_SINCE(prevImportedFiles);
            }*/
        if (changedSince != null) {
            filter = SftpFilters.FILES_CHANGED_SINCE(changedSince, extension);
        } else {
            filter = SftpFilters.JUSTFILES_WITH_EXT(extension);
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
    private Map<String, Date> downloadAllFilesByFilter(String remoteFolder, String localFolderPath, SFTPfilter filter) throws FtpException {
        Map<String, Date> downloadedFiles = new HashMap();
        try {
            reconnectIfNeeded();
        } catch (Exception ex) {
            throw new FtpException("Unable to reconnect to sftp repository. " + ex.getMessage());
        }
        try {
            if (remoteFolder != null) {
                sftpChannel.cd(remoteFolder);
            }

            //check if it is a directory
            SftpATTRS attrs = sftpChannel.lstat(CURRENT_FOLDER_PATH);
            if (!attrs.isDir()) {
                throw new FtpException("Remote folder: '" + remoteFolder + "' does not exist or is not a folder!");
            }
        } catch (SftpException ex) {
            throw new FtpException("Unable to list the path. " + ex.getMessage());
        }

        try {

            //get list of files to download
            List<String> fileNames = getFileNamesInCurrentFolderByFilter(filter);

            for (String file : fileNames) {
                Map<String, Date> res = downloadFile(remoteFolder, file, localFolderPath);
                downloadedFiles.put(file, res.get(file));
            }

        } catch (Exception ex) {
            throw new FtpException("Error downloading files from folder: " + remoteFolder + ". " + ex.getMessage());
        }
        return downloadedFiles;
    }

    /**
     * Download all files in specified folder with prefix
     *
     * @param remoteFolder
     * @param extension
     * @param localFolderPath
     * @param prefix
     * @param changedSince
     * @return
     * @throws FtpException
     */
    public Map<String, Date> downloadAllNewCsvFilesByPrefix(String remoteFolder, String extension, String localFolderPath, String prefix, Date changedSince) throws FtpException {
        //set ftp file filter accordingly
        SFTPfilter filter;

        if (changedSince != null) {
            filter = SftpFilters.FILES_WITH_PREFIX_CHANGED_SINCE(changedSince, prefix, extension);
        } else {
            filter = SftpFilters.JUSTFILES_WITH_PREFIX(prefix, extension);
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
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        Map<String, Date> downloadedFiles = new HashMap();
        try {
            reconnectIfNeeded();
            try {
                if (remoteFolder != null && !sftpChannel.pwd().endsWith(remoteFolder)) {
                    sftpChannel.cd(remoteFolder);
                }
            } catch (SftpException ex) {
                throw new FtpException("Unable to list the path. " + ex.getMessage());
            }
            //check if not directory
            SftpATTRS attrs = sftpChannel.lstat(fileName);
            if (attrs.isDir()) {
                throw new FtpException("Unable to download this file: " + fileName + ". It is a directory.");
            }
            Date last_mod_time = new Date(attrs.getMTime() * 1000L);

            byte[] buffer = new byte[1024];
            bis = new BufferedInputStream(sftpChannel.get(fileName));
            fos = new FileOutputStream(localFilePath + File.separator + fileName);
            bos = new BufferedOutputStream(fos);
            int readCount;
            while ((readCount = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }

            downloadedFiles.put(fileName, last_mod_time);
        } catch (IOException ex) {
            throw new FtpException("Error downloading file: " + fileName + ". " + ex.getMessage());

        } catch (SftpException ex) {
            Logger.getLogger(SFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bis.close();
                bos.close();
                fos.close();
            } catch (Exception ex) {
                //do nothing
            }
        }
        return downloadedFiles;

    }

    protected List<String> getFileNamesInCurrentFolderByFilter(SFTPfilter filter) throws SftpException {
        List<String> resultFiles = new ArrayList<String>();
        Vector<LsEntry> entries = sftpChannel.ls(CURRENT_FOLDER_PATH);
        for (LsEntry entry : entries) {
            if (filter.accept(entry)) {
                resultFiles.add(entry.getFilename());
            }
        }
        return resultFiles;
    }
}
