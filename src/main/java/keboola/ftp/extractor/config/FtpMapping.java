/*
 */
package keboola.ftp.extractor.config;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class FtpMapping {

	private final String srcCharset;
    private final String ftpPath;
    private final String sapiPath;
//default 1
    private final Integer isFolder;

//default 1
    private final Integer incremental;

//default 1
    private final Integer sinceLast;
//required if incremental = 1
    private final String[] pkey;
    private String prefix;
    private Integer prefixWildCardSupport;

    private String delimiter;
    private String enclosure;

    private String extension;

    private String compression;

    public static enum Compression {
    	NONE(""), ZIP("zip"), GZIP("gz");
    	public final String extension;
    	
    	Compression(String extension) {
    		this.extension = extension;
    	}
    }

    public FtpMapping(@JsonProperty("ftpPath") String ftpPath, @JsonProperty("sapiPath") String sapiPath,
            @JsonProperty("isFolder") Integer isFolder, @JsonProperty("incremental") Integer incremental, @JsonProperty("sinceLast") Integer sinceLast,
            @JsonProperty("pkey") String[] pkey, @JsonProperty("prefix") String prefix,@JsonProperty("prefix_wildcard") Integer prefixWildCardSupport,
            @JsonProperty("delimiter") String delimiter,
            @JsonProperty("enclosure") String enclosure, @JsonProperty("extension") String extension, @JsonProperty("compression") String compression,
            @JsonProperty("srcCharset") String srcCharset) {
        this.ftpPath = ftpPath;
        this.sapiPath = sapiPath;
        this.delimiter = StringUtils.defaultIfEmpty(delimiter, ",");
        this.enclosure = StringUtils.defaultIfEmpty(enclosure, "");
        this.compression = StringUtils.defaultIfEmpty(compression, Compression.NONE.name());

        if (isFolder != null) {
            this.isFolder = isFolder;
        } else {
            this.isFolder = 1;
        }
 
        if (prefixWildCardSupport != null) {
        	this.prefixWildCardSupport = prefixWildCardSupport;
        } else {
        	this.prefixWildCardSupport = 0;
        }
//set extension, set to default csv if empty
        if (!StringUtils.isEmpty(extension)) {
            this.extension = extension;
        } else {
            this.extension = "csv";
        }
        //set extension if compression used
        if (!Compression.NONE.equals(getCompressionEnum())) {
        	this.extension = getCompressionEnum().extension;
        }
        if (incremental != null) {
            this.incremental = incremental;
        } else {
            this.incremental = 1;
        }
        if (sinceLast != null) {
        	this.sinceLast = sinceLast;
        } else {
        	this.sinceLast = 1;
        }
        this.pkey = pkey;
        this.prefix = prefix;
        
        this.srcCharset = StringUtils.defaultIfEmpty(srcCharset,"UTF-8");

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
        if (!EnumUtils.isValidEnum(Compression.class, compression)) {
        	message += "Invalid compression parameter!";
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
        return String.valueOf(getDelimiterChar());
    }
    public char getDelimiterChar() {
    	//default if null
    	if (delimiter == null) {
    		return ',';
    	}
    	return StringEscapeUtils.unescapeJava(delimiter).charAt(0);
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getEnclosure() {
        //default if null
        if (StringUtils.isBlank(enclosure)) {
            return "";
        }

        return String.valueOf(getEnclosureChar());
    }
    public char getEnclosureChar() {
    	//default if null
    	if (StringUtils.isBlank(enclosure)) {
    		return Character.MIN_VALUE;
    	}  	
    	return StringEscapeUtils.unescapeJava(enclosure).charAt(0);
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

    public String getSapiTableName() {
    	String []  parts = sapiPath.split("\\.");
    	if (parts.length >0) {
    		return parts[parts.length-1];
    	}
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

    public String getCompression() {
		return compression;
	}

    public Compression getCompressionEnum() {
    	return EnumUtils.getEnum(Compression.class, compression);
    }

    public String getSrcCharsetString() {
		return srcCharset;
	}

    public Charset getSrcCharset() {
    	return Charset.forName(srcCharset);
    }

	public Integer getSinceLast() {
		return sinceLast;
	}

	public Integer getPrefixWildCardSupport() {
		return prefixWildCardSupport;
	}

	public boolean isWildcardSupport(){
		return prefixWildCardSupport == 1;
	}

	@Override
	public String toString() {
		return "FtpMapping [ftpPath=" + ftpPath + ", prefix=" + prefix + ", extension=" + extension + ", compression="
				+ compression + "]";
	}

	@Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FtpMapping)) {
            return false;
        }
        FtpMapping f = (FtpMapping) obj;
        return this.ftpPath.equals(f.ftpPath) && StringUtils.equals(this.prefix, f.prefix)
                && this.extension.endsWith(f.extension) && this.isFolder.equals(f.isFolder)
                && this.sapiPath.equals(f.sapiPath) & this.srcCharset.equals(f.srcCharset)
                && this.prefixWildCardSupport.equals(f.prefixWildCardSupport);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.ftpPath != null ? this.ftpPath.hashCode() : 0);
        hash = 79 * hash + (this.sapiPath != null ? this.sapiPath.hashCode() : 0);
        hash = 79 * hash + (this.isFolder != null ? this.isFolder.hashCode() : 0);
        hash = 79 * hash + (this.incremental != null ? this.incremental.hashCode() : 0);
        hash = 79 * hash + Arrays.deepHashCode(this.pkey);
        hash = 79 * hash + (this.prefix != null ? this.prefix.hashCode() : 0);
        hash = 79 * hash + (this.delimiter != null ? this.delimiter.hashCode() : 0);
        hash = 79 * hash + (this.enclosure != null ? this.enclosure.hashCode() : 0);
        hash = 79 * hash + (this.extension != null ? this.extension.hashCode() : 0);
        hash = 79 * hash + (this.srcCharset != null ? this.srcCharset.hashCode() : 0);
        hash = 79 * hash + (this.prefixWildCardSupport != null ? this.prefixWildCardSupport.hashCode() : 0);
        return hash;
    }
}
