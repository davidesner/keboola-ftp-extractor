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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;

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
	private static final int MAX_RETRIES = 5;
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
				//System.setProperty("https.protocols", "TLSv1");
				//System.setProperty("jdk.tls.useExtendedMasterSecret", "false");
				setUptFtpsClientExplicit(port);
				break;
			case FTP:
				setUpDefaultFtpClient(port);
				break;
			default:
				setUpDefaultFtpClient(port);
			}
		} catch (GeneralSecurityException | IOException e) {
			throw new FtpException(e.getMessage(), 1, e);
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

    private void setUptFtpsClientExplicit(Integer port) throws GeneralSecurityException, SSLException, IOException {    	  	
    	ftpClient = new SSLSessionReuseFTPSClient("SSL",false);

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
    	Callable<Boolean> callable = new Callable<Boolean>() {
    	    public Boolean call() throws Exception {
    	    	if (ftpClient.isConnected()) {
    	    		return true;
    	    	}
    	    	ftpClient.connect(url, port);
    	    	//login to server
    	    	if (!ftpClient.login(userName, pass)) {
    	    		ftpClient.logout();
    	    		return false;
    	    	}
    	    	int reply = ftpClient.getReplyCode();
    	    	
    	    	if (!FTPReply.isPositiveCompletion(reply)) {
    	    		ftpClient.disconnect();
    	    		return false;
    	    	}
    	    	if (ftpClient instanceof FTPSClient){
    	    		setFtpSecurity();
    	    	}
    	    	//passive mode
    	    	ftpClient.enterLocalPassiveMode();
    	    	return true;
    	    }
    	};

    	Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
    	        .retryIfResult(Predicates.<Boolean>isNull())
    	        .retryIfExceptionOfType(IOException.class)
    	        .retryIfRuntimeException()
    	        .withWaitStrategy(WaitStrategies.exponentialWait())
    	        .withStopStrategy(StopStrategies.stopAfterAttempt(MAX_RETRIES))
    	        .build();
    	try {
    	    return retryer.call(callable);
    	} catch (RetryException e) {
    	    throw new IOException("Failed to connect to the client. After " +  MAX_RETRIES + " attempts.", e);
    	} catch (ExecutionException e) {
    		throw new IOException("Failed to connect to the client.", e);
    	}
    }
    
    
    private void setFtpSecurity() throws SSLException, IOException {
    	/* Using TLS/SSL on data channel, some server enforce this.    	 * 
    	 */
    	// Set protection buffer size
    	((FTPSClient) ftpClient).execPBSZ(0);
    	// Set data channel protection to private
    	((FTPSClient) ftpClient).execPROT("P");
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
    	Retryer<Map<String, Date>> retryer = RetryerBuilder.<Map<String, Date>>newBuilder()
    	        .retryIfResult(Predicates.<Map<String, Date>>isNull())
    	        .retryIfExceptionOfType(IOException.class)
    	        .retryIfRuntimeException()
    	        .withWaitStrategy(WaitStrategies.exponentialWait())
    	        .withStopStrategy(StopStrategies.stopAfterAttempt(MAX_RETRIES))
    	        .build();

        try {
            connect();
        } catch (IOException ex) {
            throw new FtpException("Error connecting to ftp server. " + ex.getMessage(), ex);
        }
        try {
        	return retryer.call(getDownloadAllFilesByFilter(remoteFolder, localFolderPath, filter));
        } catch (Exception ex) {
            throw new FtpException("Error downloading files from folder: " + remoteFolder + ". " + ex.getMessage(), ex);
        }
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
    public Map<String, Date> downloadAllNewCsvFilesByPrefix(String remoteFolder, String extension, String localFolderPath, String prefix, Date changedSince, boolean wildcardSupport) throws FtpException {
        //set ftp file filter accordingly
        FTPFileFilter filter;

        if (changedSince != null) {
            filter = FtpFilters.FILES_WITH_PREFIX_CHANGED_SINCE(changedSince, prefix, extension, this.hostTz, wildcardSupport);
        } else {
            filter = FtpFilters.JUSTFILES_WITH_PREFIX(prefix, extension, wildcardSupport);
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
        
    	Retryer<Map<String, Date>> retryer = RetryerBuilder.<Map<String, Date>>newBuilder()
    	        .retryIfResult(Predicates.<Map<String, Date>>isNull())
    	        .retryIfExceptionOfType(IOException.class)
    	        .retryIfRuntimeException()
    	        .withWaitStrategy(WaitStrategies.exponentialWait())
    	        .withStopStrategy(StopStrategies.stopAfterAttempt(MAX_RETRIES))
    	        .build();
    	FileOutputStream fos = null;    	
        try {        	
        	fos = new FileOutputStream(localFilePath + File.separator + fileName);
        	return retryer.call(getDownloadFileCallable(remoteFolder, fileName, localFilePath, fos));
        } catch (Exception ex) {
            throw new FtpException("Error downloading file: " + fileName + ". " + ex.getMessage(), ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                }
            }
        }
        

    }
    
    public Callable getDownloadFileCallable(final String remoteFolder, final String fileName, final String localFilePath, final FileOutputStream fos){
    	Callable<Map<String, Date>> callable = new Callable<Map<String, Date>>() {
    	    public Map<String, Date> call() throws Exception {
    	        Map<String, Date> downloadedFiles = new HashMap();
    	    	reconnectIfNeeded();
    	        if (remoteFolder != null) {
    	            ftpClient.changeWorkingDirectory(remoteFolder);
    	        }
    	        int returnCode = ftpClient.getReplyCode();
    	        if (returnCode == 550) {
    	            throw new FtpException("Remote folder does not exist!", null);
    	        }

    	       

    	        if (ftpClient.changeWorkingDirectory(fileName)) {
    	            throw new FtpException("Unable to download this file: " + fileName + ". It is a directory.", null);
    	        }
    	        FTPFile[] files = ftpClient.listFiles(fileName);
    	        if (files == null || files.length == 0) {
    	        	return downloadedFiles;
    	        }
    	        ftpClient.retrieveFile(fileName, fos);
    	        downloadedFiles.put(fileName, files[0].getTimestamp().getTime());
    	        return downloadedFiles;
    	    }
    	};
    	return callable;
    	
    }
   
	public Callable<Map<String, Date>> getDownloadAllFilesByFilter(final String remoteFolder, final String localFolderPath, final FTPFileFilter filter) {
			Callable<Map<String, Date>> callable = new Callable<Map<String, Date>>() {
			public Map<String, Date> call() throws Exception {
				Map<String, Date> downloadedFiles = new HashMap();

				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				// change back to root first
				ftpClient.changeWorkingDirectory("/");
				ftpClient.changeWorkingDirectory(remoteFolder);
				int returnCode = ftpClient.getReplyCode();
				if (returnCode == 550) {
					throw new FtpException("Remote folder: '" + remoteFolder + "' does not exist or is not a folder!",
							null);
				}
				// get list of files to download
				FTPFile[] files = ftpClient.listFiles(null, filter);
				
				if (ftpClient.getReplyCode() >= 300) {
					throw new FtpException("Unable to list remote files! In folder: '" + remoteFolder + "'. Cause: " + ftpClient.getReplyString(),
							null);
				}

				for (FTPFile file : files) {
					downloadFile(ftpClient.printWorkingDirectory(), file.getName(), localFolderPath);
					downloadedFiles.put(file.getName(), file.getTimestamp().getTime());
				}
				return downloadedFiles;
			}

		};
		return callable;

	}
}
