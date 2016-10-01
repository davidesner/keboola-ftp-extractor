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
        FTP, FTPS, SFTP
    };
    private String user;
    private String pass;
    private String url;
    private Protocol protocol;
    private TimeZone hostTz;

    /**
     * Create FTPClientBuilder with specified endpoint url and protocol and
     * anonymous login.
     *
     * @param pType
     */
    private FTPClientBuilder(String protocol, String user, String pass, String url) throws FtpException {

        if (Protocol.FTP.name().equals(protocol)) {
            this.protocol = Protocol.FTP;
        } else if (Protocol.SFTP.name().equals(protocol)) {
            this.protocol = Protocol.SFTP;
        } else if (Protocol.FTPS.name().equals(protocol)) {
            this.protocol = Protocol.FTPS;
        } else {
            throw new FtpException("Protocol: " + protocol + " is unsupported!");
        }
        this.user = user;
        this.pass = pass;
        this.url = url;
    }

    public static FTPClientBuilder create(String protocol, String url) throws FtpException {
        return new FTPClientBuilder(protocol, "", "", url);
    }

    public FTPClientBuilder setUser(String user) {
        this.user = user;
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
                return new FTPClient(user, pass, url, hostTz);
            case FTPS:
                return new FTPClient(user, pass, url, hostTz);
            case SFTP:
                return new SFTPClient(user, pass, url);
            default:
                throw new FtpException("Unsupported protocol!");
        }
    }

}
