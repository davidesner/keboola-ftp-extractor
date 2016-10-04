/*
 */
package keboola.ftp.extractor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import keboola.ftp.extractor.ftpclient.FTPClientBuilder;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class KBCParameters {

    private final static String[] REQUIRED_FIELDS = {"user", "pass", "protocol", "timezone", "mappings", "ftpUrl"};
    private final Map<String, Object> parametersMap;

    private Date date_from;
    private Date date_to;
    @JsonProperty("ftpUrl")
    private String ftpUrl;
    @JsonProperty("port")
    private Integer port;
    @JsonProperty("user")
    private String user;
    @JsonProperty("#pass")
    private String pass;
    @JsonProperty("mappings")
    private List<FtpMapping> mappings;
    @JsonProperty("protocol")
    private String protocol;
    @JsonProperty("timezone")
    private String timezone;
    //end date of fetched interval in format: 05-10-2015 21:00
    @JsonProperty("dateTo")
    private String dateTo;
    //start date of fetched interval in format: 05-10-2015 21:00
    @JsonProperty("dateFrom")
    private String dateFrom;

    @JsonIgnore
    private FTPClientBuilder.Protocol ftpProtocol;

    public KBCParameters() {
        parametersMap = new HashMap();

    }

    @JsonCreator
    public KBCParameters(@JsonProperty("ftpUrl") String ftpUrl, @JsonProperty("port") Integer port, @JsonProperty("user") String user, @JsonProperty("#pass") String pass,
            @JsonProperty("protocol") String protocol, @JsonProperty("timezone") String timezone,
            @JsonProperty("dateFrom") int daysInterval, @JsonProperty("dateTo") String dateTo,
            @JsonProperty("mappings") ArrayList<FtpMapping> mappings) throws ParseException {
        parametersMap = new HashMap();
        this.ftpUrl = ftpUrl;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.mappings = mappings;
        this.dateTo = dateTo;
        this.protocol = protocol;
        this.timezone = timezone;
        if (protocol != null) {
            if (protocol.equals(FTPClientBuilder.Protocol.FTP.name())) {
                this.ftpProtocol = FTPClientBuilder.Protocol.FTP;
            } else if (protocol.equals(FTPClientBuilder.Protocol.SFTP.name())) {
                this.ftpProtocol = FTPClientBuilder.Protocol.SFTP;
            } else if (protocol.equals(FTPClientBuilder.Protocol.FTPS.name())) {
                this.ftpProtocol = FTPClientBuilder.Protocol.FTPS;
            } else {
                this.ftpProtocol = FTPClientBuilder.Protocol.FTP;
            }
        } else {
            this.ftpProtocol = FTPClientBuilder.Protocol.FTP;
            this.protocol = FTPClientBuilder.Protocol.FTP.name();
        }
        if (dateTo != null) {
            setDate_to(dateTo);
        }
        if (dateFrom != null) {
            setDate_from(dateFrom);
        }

        //set param map
        parametersMap.put("ftpUrl", ftpUrl);
        parametersMap.put("user", user);
        parametersMap.put("pass", pass);
        parametersMap.put("protocol", protocol);
        parametersMap.put("timezone", timezone);
        parametersMap.put("mappings", new Object());

    }

    public boolean validateMappings() throws ValidationException {
        for (FtpMapping m : mappings) {
            m.validate();
        }
        return this.validate();
    }

    public boolean validate() throws ValidationException {
        if (!Arrays.asList(TimeZone.getAvailableIDs()).contains(this.timezone)) {
            throw new ValidationException("TimeZone " + this.timezone + " is invalid!");
        }
        return true;
    }

    /**
     * Returns list of required fields missing in config
     *
     * @return
     */
    public List<String> getMissingFields() {
        List<String> missing = new ArrayList<String>();
        for (int i = 0; i < REQUIRED_FIELDS.length; i++) {
            Object value = parametersMap.get(REQUIRED_FIELDS[i]);
            if (value == null) {
                missing.add(REQUIRED_FIELDS[i]);
            }
        }

        if (missing.isEmpty()) {
            return null;
        }
        return missing;
    }

    private void setDate_from(String dateString) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        this.date_from = format.parse(dateString);

    }

    public Date getDate_from() {
        return date_from;
    }

    private void setDate_to(String dateString) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        this.date_to = format.parse(dateString);

    }

    public Date getDate_to() {
        return date_to;
    }

    public Map<String, Object> getParametersMap() {
        return parametersMap;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public FTPClientBuilder.Protocol getFtpProtocol() {
        return ftpProtocol;
    }

    public String getFtpUrl() {
        return ftpUrl;
    }

    public void setFtpUrl(String ftpUrl) {
        this.ftpUrl = ftpUrl;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public List<FtpMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<FtpMapping> mappings) {
        this.mappings = mappings;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

}
