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

    public FtpException(String message, Exception cause) {
        super(message, 1, cause);
    }

    public FtpException(String message, int severity) {
        super(message, severity, null);
    }

    public FtpException(String message, int severity, Exception cause) {
    	super(message, severity, cause);
    }

}
