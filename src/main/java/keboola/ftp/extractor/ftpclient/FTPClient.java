package keboola.ftp.extractor.ftpclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import keboola.ftp.extractor.ftpclient.FTPClientBuilder.Protocol;
import keboola.ftp.extractor.ftpclient.filters.FtpFilters;

/**
 * FTP FTPClient for handling FTP connections
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class FTPClient implements IFTPClient {

	public static final int DEFAUTL_SSL_PORT = 990;
    private String userName;
    private String pass;
    private String url;
    private Integer port;
    private final TimeZone hostTz;
    private org.apache.commons.net.ftp.FTPClient ftpClient;

    /**
     *
     * @param userName
     * @param pass
     * @param url
     * @param port
     * @param hostTz Timezone of host machine
     * @throws FtpException 
     * @throws GeneralSecurityException 
     */
    public FTPClient(String userName, String pass, String url, Integer port, TimeZone hostTz, Protocol protocol) throws FtpException {
		this.userName = userName;
		this.pass = pass;
		this.url = url;

		this.hostTz = hostTz;
		try {
			switch (protocol) {
			case FTPS_IMPLICIT:
				setUptFtpsClientImplicit(port);
				break;
			case FTPS_EXPLICIT:
				System.setProperty("https.protocols", "TLSv1");
				setUptFtpsClientExplicit(port);
				break;
			case FTP:
				setUpDefaultFtpClient(port);
				break;
			default:
				setUpDefaultFtpClient(port);
			}
		} catch (GeneralSecurityException e) {
			throw new FtpException(e.getMessage(), 1);
		}
		//set buffer size
        this.ftpClient.setBufferSize(1024 * 1024);
    }

    private void setUpDefaultFtpClient(Integer port) {
    	ftpClient = new org.apache.commons.net.ftp.FTPClient();
		if (port == null) {
			this.port = FTP.DEFAULT_PORT;
		} else {
			this.port = port;
		}
    }

    private void setUptFtpsClientImplicit(Integer port) {
    	ftpClient = new FTPSClient(true);
		if (port == null) {
			this.port = DEFAUTL_SSL_PORT;
		} else {
			this.port = port;
		}
		setTrustManager();
    }

    private void setUptFtpsClientExplicit(Integer port) throws GeneralSecurityException {    	  	
    	ftpClient = new FTPSClient(false);
    	if (port == null) {
    		this.port = FTP.DEFAULT_PORT;
    	}  else {
			this.port = port;
		}   	
    	setTrustManager();
    }

    private void setTrustManager() {
    	((FTPSClient) ftpClient).setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
    }
    /**
     * Establishes connection
     *
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public boolean connect() throws SocketException, IOException {
        ftpClient.connect(url, this.port);
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
     * @param extension
     * @param localFolderPath
     * @param prevImportedFiles
     * @param changedSince
     * @return Map of downloaded file names and its' timestamps
     * @throws FtpException
     */
    public Map<String, Date> downloadAllNewCsvFiles(String remoteFolder, String extension, String localFolderPath, Map<String, Date> prevImportedFiles, Date changedSince) throws FtpException {
        //set ftp file filter accordingly
        FTPFileFilter filter;
        /*
            if(prevImportedFiles!=null){
                filter=FtpFilters.FILES_CHANGED_SINCE(prevImportedFiles);
            }*/
        if (changedSince != null) {
            filter = FtpFilters.FILES_CHANGED_SINCE(changedSince, extension, this.hostTz);
        } else {
            filter = FtpFilters.JUSTFILES_WITH_EXT(extension);
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
     * @param extension
     * @param localFolderPath
     * @param prefix
     * @param changedSince
     * @return
     * @throws FtpException
     */
    public Map<String, Date> downloadAllNewCsvFilesByPrefix(String remoteFolder, String extension, String localFolderPath, String prefix, Date changedSince) throws FtpException {
        //set ftp file filter accordingly
        FTPFileFilter filter;

        if (changedSince != null) {
            filter = FtpFilters.FILES_WITH_PREFIX_CHANGED_SINCE(changedSince, prefix, extension, this.hostTz);
        } else {
            filter = FtpFilters.JUSTFILES_WITH_PREFIX(prefix, extension);
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
