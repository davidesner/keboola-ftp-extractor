/*
 */
package keboola.ftp.extractor.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class VisitedFoldersList {

    @JsonProperty("visitedFolders")
    private List<VisitedFolder> visitedFolders;

    public VisitedFoldersList() {
    }

    public VisitedFoldersList(List<VisitedFolder> visitedFolders) {
        this.visitedFolders = visitedFolders;
    }

    public List<VisitedFolder> getVisitedFolders() {
        return visitedFolders;
    }

    public void setVisitedFolders(List<VisitedFolder> visitedFolders) {
        this.visitedFolders = visitedFolders;
    }

    public VisitedFolder getFolderByPath(String folderPath) {
        for (VisitedFolder f : visitedFolders) {
            if (f.getFolderPath().contentEquals(folderPath)) {
                return f;
            }
        }
        return null;
    }

}
