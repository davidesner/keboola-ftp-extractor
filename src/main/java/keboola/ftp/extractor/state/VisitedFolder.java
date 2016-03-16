/*
 */
package keboola.ftp.extractor.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class VisitedFolder {

    public VisitedFolder(String folderPath, Map<String, Date> fileMap, Date lastRun) {
        this.folderPath = folderPath;
        this.fileMap = fileMap;
        this.lastRun = lastRun;
    }

    @JsonProperty("folderPath")
    private String folderPath;

    @JsonProperty("fileMap")
    private Map<String, Date> fileMap;

    @JsonProperty("lastRun")
    private Date lastRun;

    public VisitedFolder() {
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public Map<String, Date> getFileMap() {
        return fileMap;
    }

    public void setFileMap(Map<String, Date> fileMap) {
        this.fileMap = fileMap;
    }

    public Date getLastRun() {
        return lastRun;
    }

    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }

}
