/*
 */
package keboola.ftp.extractor.ftpclient.filters;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

/**
 * Implements some FtpFile filters
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class FtpFilters {
	
	private static final String WILDCARD_CHAR = "*";
	private static final String SINGLE_CHAR_WILDCARD_CHAR = "?";

    /**
     * Accepts all (non-null) FTPFile files entries.
     */
    public static final FTPFileFilter JUSTFILES = new FTPFileFilter() {
        @Override
        public boolean accept(FTPFile file) {
            return file != null && !file.isDirectory();
        }

    };

    /**
     * Accepts all (non-null) FTPFile csv files entries.
     *
     * @param extension
     * @return FTPFileFilter
     */
    public static final FTPFileFilter JUSTFILES_WITH_EXT(final String extension) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                return file != null && !file.isDirectory() && hasExtension(file.getName(), extension);
            }
        };
    }

    /**
     * Accepts all (non-null) FTPFile csv files entries.
     *
     * @param prefix
     * @param extension
     * @return
     */
    public static final FTPFileFilter JUSTFILES_WITH_PREFIX(final String prefix, final String extension, boolean wildcardSupport) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                if (file == null) {
                    return false;
                }
                String normName = file.getName();//.toLowerCase();

                return !file.isDirectory() && hasExtension(file.getName(), extension) && hasPrefix(normName, prefix);
            }

        };
    }

    public static final FTPFileFilter FILES_WITH_PREFIX_CHANGED_SINCE(final Date changedSince, final String prefix, 
    		final String extension, final TimeZone hostTz, boolean wildcardSupport) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                if (file == null) {
                    return false;
                }
                String normName = file.getName();//.toLowerCase();
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                Calendar fileChanged = convertDateFromTimezone(file.getTimestamp().getTime(), hostTz);
                return !file.isDirectory() && fileChanged.after(since) && hasExtension(file.getName(), extension) && hasPrefix(normName, prefix);
            }

        };
    }

    /**
     * Accepts all (non-null) FTPFile files entries since specified date.
     *
     * @param changedSince
     * @param extension
     * @param hostTz
     * @return
     */
    public static final FTPFileFilter FILES_CHANGED_SINCE(final Date changedSince, final String extension, final TimeZone hostTz) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                if (file == null) {
                    return false;
                }
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                Calendar fileChanged = convertDateFromTimezone(file.getTimestamp().getTime(), hostTz);
                return !file.isDirectory() && fileChanged.after(since) && hasExtension(file.getName(), extension);
            }

        };
    }

    /**
     * Accepts all (non-null) FTPFile files entries since specified date.
     *
     * @param changedSince
     * @param hostTz
     * @return
     */
    public static final FTPFileFilter FILES_CHANGED_SINCE(final Date changedSince, final TimeZone hostTz) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                if (file == null) {
                    return false;
                }
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                Calendar fileChanged = convertDateFromTimezone(file.getTimestamp().getTime(), hostTz);
                return !file.isDirectory() && fileChanged.after(since);
            }

        };
    }

    private static boolean isCSV(String filename) {
        String ex = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return ex.toLowerCase().equals("csv");
    }

    private static boolean hasExtension(String filename, String extension) {
    	//ignore extension if wildcard present
    	if (WILDCARD_CHAR.equals(extension)) {
    		return true;
    	}
        String ex = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return ex.toLowerCase().equals(extension);
    }

    protected static boolean hasPrefix(String filename, String prefix) {
    	//escape regex special chars
 
    	//treat as a prefix => if not specified add wildcard to the end, also helps with backward compatibility
    	if (!prefix.endsWith("*")){
    		prefix += "*";
    	}
    	
    	//replace wildcard chars with regex
    	String prefix_mod = prefix.replace(WILDCARD_CHAR, "\\w*").replace(SINGLE_CHAR_WILDCARD_CHAR, "\\w");   
    	
    	

    	return filename.matches(prefix_mod);
    }

    protected static String escapeRegex(String in) {
   	StringBuilder sb = new StringBuilder();
    	
    	String [] wildCardParts = in.split("\\*");
    	int i=0;
    	boolean endsWithStar = in.endsWith("*");
    	for (String wc : wildCardParts){
    		i++;
    		boolean endsWithQuote = wc.endsWith("?");
    		String[] singleChars = wc.split("\\?");
    		int j = 0;
    		for (String schar : singleChars){
    			j++;
    			
    			sb.append(Pattern.quote(schar));
    			if (singleChars.length!=j || endsWithQuote){
    				sb.append(SINGLE_CHAR_WILDCARD_CHAR);
    			}
    		}
    		sb.append(Pattern.quote(wc));
    		if (singleChars.length!=j || endsWithStar){
    			sb.append(WILDCARD_CHAR);
    		}
    	}
    	return sb.toString();
    }


    /**
     * Returns Date converted from given timezone.
     *
     * @param date
     * @param tz - timezone of the Date representation
     * @return Calendar in current system timezone
     */
    private static Calendar convertDateFromTimezone(Date date, TimeZone tz) {
        Calendar toTime = new GregorianCalendar();

        //convert to actual utc time
        long gmtTime = date.getTime() - tz.getOffset(date.getTime());
        long currentTime = gmtTime + toTime.getTimeZone().getOffset(new Date().getTime());

        toTime.setTimeInMillis(currentTime);

        Calendar res = new GregorianCalendar();
        res.setTime(toTime.getTime());
        return res;
    }
}
