/*
 */
package keboola.ftp.extractor.ftpclient;

import keboola.ftp.extractor.KBCException;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class FtpException extends KBCException {

    public FtpException(String message) {
        super(message);
    }

    public FtpException(String message, int severity) {
        super(message, severity);
    }

}
