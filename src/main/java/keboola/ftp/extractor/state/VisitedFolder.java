/*
 */
package keboola.ftp.extractor.state;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import keboola.ftp.extractor.config.FtpMapping;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class VisitedFolder {

    public VisitedFolder(String folderPath, Date lastRun, FtpMapping lastMapping) {
        this.folderPath = folderPath;
        this.lastMapping = lastMapping;
        this.lastRun = lastRun;
    }

    @JsonProperty("folderPath")
    private String folderPath;

    //@JsonProperty("fileMap")
    @JsonIgnore
    private Map<String, Date> fileMap;

    @JsonProperty("lastRun")
    private Date lastRun;

    @JsonProperty("lastMapping")
    private FtpMapping lastMapping;

    public VisitedFolder() {
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public Date getLastRun() {
        return lastRun;
    }

    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }

    public FtpMapping getLastMapping() {
        return lastMapping;
    }

    public void setLastMapping(FtpMapping lastMapping) {
        this.lastMapping = lastMapping;
    }

}
