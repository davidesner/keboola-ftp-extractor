/*
 */
package keboola.ftp.extractor.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class FtpMapping {

    private final String ftpPath;
    private final String sapiPath;
//default 1
    private final Integer isFolder;

//default 1
    private final Integer incremental;
//required if incremental = 1
    private final String[] pkey;
    private String prefix;

    private String delimiter;
    private String enclosure;

    private String extension;

    public FtpMapping(@JsonProperty("ftpPath") String ftpPath, @JsonProperty("sapiPath") String sapiPath,
            @JsonProperty("isFolder") Integer isFolder, @JsonProperty("incremental") Integer incremental,
            @JsonProperty("pkey") String[] pkey, @JsonProperty("prefix") String prefix, @JsonProperty("delimiter") String delimiter,
            @JsonProperty("enclosure") String enclosure, @JsonProperty("extension") String extension) {
        this.ftpPath = ftpPath;
        this.sapiPath = sapiPath;
        this.delimiter = delimiter;
        this.enclosure = enclosure;

        if (isFolder != null) {
            this.isFolder = isFolder;
        } else {
            this.isFolder = 1;
        }
//set extension, set to default csv, if isFolder is not 1
        if (extension != null && this.isFolder == 1) {
            this.extension = extension;
        } else {
            this.extension = "csv";
        }
        if (incremental != null) {
            this.incremental = incremental;
        } else {
            this.incremental = 1;
        }
        this.pkey = pkey;
        this.prefix = prefix;

    }

    public boolean validate() throws ValidationException {
        String message = "Ftp file mapping configuration error: ";
        int l = message.length();
        if (ftpPath == null) {
            message += "ftpPath parameter is missing! ";
        }
        if (sapiPath == null) {
            message += "sapiPath parameter is missing! ";
        }
        if (incremental == 1 && pkey == null) {
            message += "pKey parameter has to be set for incremental import! ";
        }
        if (isFolder == 0 && prefix != null) {
            prefix = null;
        }
        if (message.length() > l) {
            throw new ValidationException(message);
        }
        return true;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getDelimiter() {
        //default if null
        if (delimiter == null) {
            return ",";
        }
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getEnclosure() {
        //default if null
        if (enclosure == null) {
            return "\"";
        }

        return enclosure;
    }

    public void setEnclosure(String enclosure) {
        this.enclosure = enclosure;
    }

    public boolean isIncremental() {
        return incremental == 1;
    }

    public boolean isFolder() {
        return isFolder == 1;
    }

    public String getFtpPath() {
        return ftpPath;
    }

    public String getSapiPath() {
        return sapiPath;
    }

    public Integer getIsFolder() {
        return isFolder;
    }

    public Integer getIncremental() {
        return incremental;
    }

    public String[] getPkey() {
        return pkey;
    }

    public String getPrefix() {
        return prefix;
    }

}
