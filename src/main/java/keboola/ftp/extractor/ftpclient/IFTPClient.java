/*
 */
package keboola.ftp.extractor.ftpclient;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;
import java.util.Map;

/**
 *
 * author David Esner <esnerda at gmail.com>
 * created 2016
 */
public interface IFTPClient {

    public Map<String, Date> downloadAllNewCsvFiles(String remoteFolder, String extension, String localFolderPath, Map<String, Date> prevImportedFiles, Date changedSince) throws FtpException;

    public void reconnectIfNeeded() throws IOException;

    public void disconnect() throws IOException;

    public boolean connect() throws SocketException, IOException;

    public Map<String, Date> downloadAllNewCsvFilesByPrefix(String remoteFolder, String extension, String localFolderPath, String prefix, Date changedSince) throws FtpException;

    public Map<String, Date> downloadFile(String remoteFolder, String fileName, String localFilePath) throws FtpException;
}
