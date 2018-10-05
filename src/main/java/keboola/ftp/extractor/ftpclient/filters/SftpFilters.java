/*
 */
package keboola.ftp.extractor.ftpclient.filters;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 *
 * author David Esner <esnerda at gmail.com>
 * created 2016
 */
public class SftpFilters {

	private static final String WILDCARD_CHAR = "*";
	private static final String SINGLE_CHAR_WILDCARD_CHAR = "?";

    /**
     * Accepts all (non-null) LsEntry csv files entries.
     *
     * @param extension
     * @return SFTPfilter
     */
    public static final SFTPfilter JUSTFILES_WITH_EXT(final String extension) {
        return new SFTPfilter() {
            @Override
            public boolean accept(LsEntry file) {
                if (file == null) {
                    return false;
                }
                SftpATTRS attrs = file.getAttrs();
                return !attrs.isDir() && hasExtension(file.getFilename(), extension);
            }
        };
    }

    /**
     * Accepts all (non-null) LsEntry csv files entries.
     *
     * @param prefix
     * @param extension
     * @return
     */
    public static final SFTPfilter JUSTFILES_WITH_PREFIX(final String prefix, final String extension, boolean wildcardSupport) {
        return new SFTPfilter() {
            @Override
            public boolean accept(LsEntry file) {
                if (file == null) {
                    return false;
                }
                SftpATTRS attrs = file.getAttrs();
                boolean hasPrefix = false;
                if (wildcardSupport) {
                	hasPrefix = hasPrefix(file.getFilename(), prefix);
                } else {
                	//backward compatibility support
                	hasPrefix = file.getFilename().startsWith(prefix);
                }
                return !attrs.isDir() && hasExtension(file.getFilename(), extension) && hasPrefix;
            }

        };
    }

    public static final SFTPfilter FILES_WITH_PREFIX_CHANGED_SINCE(final Date changedSince, final String prefix, final String extension, boolean wildcardSupport) {
        return new SFTPfilter() {
            @Override
            public boolean accept(LsEntry file) {
                if (file == null) {
                    return false;
                }
                SftpATTRS attrs = file.getAttrs();
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                Calendar fileChanged = mTimeToCalendar(attrs.getMTime());
                
                boolean hasPrefix = false;
                if (wildcardSupport) {
                	hasPrefix = hasPrefix(file.getFilename(), prefix);
                } else {
                	//backward compatibility support
                	hasPrefix = file.getFilename().startsWith(prefix);
                }
                
                return !attrs.isDir() && fileChanged.after(since) && hasExtension(file.getFilename(), extension) && hasPrefix;
            }

        };
    }

    /**
     * Accepts all (non-null) LsEntry files entries since specified date.
     *
     * @param changedSince
     * @param extension
     * @param hostTz
     * @return
     */
    public static final SFTPfilter FILES_CHANGED_SINCE(final Date changedSince, final String extension) {
        return new SFTPfilter() {
            @Override
            public boolean accept(LsEntry file) {
                if (file == null) {
                    return false;
                }
                SftpATTRS attrs = file.getAttrs();
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                Calendar fileChanged = mTimeToCalendar(attrs.getMTime());
                return !attrs.isDir() && fileChanged.after(since) && hasExtension(file.getFilename(), extension);
            }

        };
    }

    protected static boolean isCSV(String filename) {
        String ex = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return ex.toLowerCase().equals("csv");
    }

    protected static boolean hasPrefix(String filename, String prefix) {
    	String prefix_mod = prefix;
    	//escape regex special chars
    	//treat as a prefix => if not specified add wildcard to the end, also helps with backward compatibility
    	if (!prefix.endsWith("*")){
    		prefix_mod = prefix + "*";
    	}
    	prefix_mod = escapeRegex(prefix_mod);
    	//replace wildcard chars with regex
    	prefix_mod = prefix_mod.replace(WILDCARD_CHAR, ".*").replace(SINGLE_CHAR_WILDCARD_CHAR, ".");     	

    	return filename.matches(prefix_mod);
    }

    protected static String escapeRegex(String in) {
   	StringBuilder sb = new StringBuilder();
    	
    	String [] wildCardParts = in.split("\\*");
    	int i=0;
    	boolean endsWithStar = in.endsWith("*");
    	for (String wc : wildCardParts){
    		boolean wcPartSaved = false;
    		i++;
    		boolean endsWithQuote = wc.endsWith("?");
    		
    		String[] singleChars = new String [0];
    		if (wc.contains("?")){
    			singleChars = wc.split("\\?");
    		}
    		int j = 0;
    		
    		for (String schar : singleChars){
    			j++;
    			if (!schar.isEmpty()) {
    				sb.append(Pattern.quote(schar));
    			}
    			if (singleChars.length!=j || endsWithQuote){
    				sb.append(SINGLE_CHAR_WILDCARD_CHAR);
    			}
    			wcPartSaved = true;
    		}
    		if (!wc.isEmpty() && !wcPartSaved){
    			sb.append(Pattern.quote(wc));
    		}
    		if (singleChars.length!=j || endsWithStar){
    			sb.append(WILDCARD_CHAR);
    		}
    	}
    	return sb.toString();
    }
  
    protected static boolean hasExtension(String filename, String extension) {
    	//ignore extension if wildcard present
    	if (WILDCARD_CHAR.equals(extension)) {
    		return true;
    	}
        String ex = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return ex.toLowerCase().equals(extension);
    }

    protected static Calendar mTimeToCalendar(int mTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(mTime * 1000L));
        return cal;

    }
}
