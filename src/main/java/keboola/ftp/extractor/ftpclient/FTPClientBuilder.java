/*
 */
package keboola.ftp.extractor.ftpclient;

import java.util.TimeZone;

/**
 *
 * author David Esner <esnerda at gmail.com>
 * created 2016
 */
public class FTPClientBuilder {

    public static enum Protocol {
        FTP, FTPS_IMPLICIT, FTPS, FTPS_EXPLICIT, SFTP
    };
    private String user;
    private String pass;
    private String url;
    private Protocol protocol;
    private TimeZone hostTz;
    private Integer port;

    /**
     * Create FTPClientBuilder with specified endpoint url and protocol and
     * anonymous login.
     *
     * @param pType
     */
    private FTPClientBuilder(Protocol protocol, String user, String pass, String url) throws FtpException {
    	this.protocol = protocol;
        this.user = user;
        this.pass = pass;
        this.url = url;
        this.port = null;
    }

    public static FTPClientBuilder create(Protocol protocol, String url) throws FtpException {
        return new FTPClientBuilder(protocol, "", "", url);
    }

    public FTPClientBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    public FTPClientBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }

    public FTPClientBuilder setPass(String pass) {
        this.pass = pass;
        return this;
    }

    public FTPClientBuilder setHostTz(TimeZone hostTz) {
        this.hostTz = hostTz;
        return this;
    }

    public IFTPClient build() throws FtpException {
        switch (protocol) {
            case FTP:
                return new FTPClient(user, pass, url, port, hostTz, protocol);
            case FTPS://legacy opton
            	return new FTPClient(user, pass, url, port, hostTz, Protocol.FTPS_IMPLICIT);
            case FTPS_IMPLICIT:
            	return new FTPClient(user, pass, url, port, hostTz, protocol);
            case FTPS_EXPLICIT:
                return new FTPClient(user, pass, url, port, hostTz, protocol);
            case SFTP:
                return new SFTPClient(user, pass, url, port);
            default:
                throw new FtpException("Unsupported protocol!");
        }
    }

}
