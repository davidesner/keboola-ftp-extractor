/*
 */
package keboola.ftp.extractor.ftpclient;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

/**
 * Implements some FtpFile filters
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class FtpFilters {

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
     */
    public static final FTPFileFilter JUSTCSVFILES = new FTPFileFilter() {
        @Override
        public boolean accept(FTPFile file) {
            return file != null && !file.isDirectory() && isCSV(file.getName());
        }

    };

    /**
     * Accepts all (non-null) FTPFile csv files entries.
     */
    public static final FTPFileFilter JUSTCSVFILES_WITH_PREFIX(String prefix) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                if (file == null) {
                    return false;
                }
                String normName = file.getName();//.toLowerCase();

                return !file.isDirectory() && isCSV(file.getName()) && normName.startsWith(prefix);
            }

        };
    }

    public static final FTPFileFilter CSVFILES_WITH_PREFIX_CHANGED_SINCE(Date changedSince, String prefix) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                if (file == null) {
                    return false;
                }
                String normName = file.getName();//.toLowerCase();
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                return !file.isDirectory() && file.getTimestamp().after(since) && isCSV(file.getName()) && normName.startsWith(prefix);
            }

        };
    }

    /**
     * Accepts all (non-null) FTPFile files entries since specified date.
     *
     * @param changedSince
     * @return
     */
    public static final FTPFileFilter CSVFILES_CHANGED_SINCE(Date changedSince) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                return file != null && !file.isDirectory() && file.getTimestamp().after(since) && isCSV(file.getName());
            }

        };
    }

    /**
     * Accepts all (non-null) FTPFile files entries since specified date.
     *
     * @param changedSince
     * @return
     */
    public static final FTPFileFilter FILES_CHANGED_SINCE(Date changedSince) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                return file != null && !file.isDirectory() && file.getTimestamp().after(since);
            }

        };
    }

    /**
     * Accepts all (non-null) FTPFile files entries since specified date.
     *
     * @param changedSince
     * @return
     */
    /**
     * Accepts all (non-null) FTPFile files entries that are not present in
     * specified collection or have changed since.
     *
     * @param prevDownFiles
     * @return
     */
    public static final FTPFileFilter FILES_CHANGED_SINCE(Map<String, Date> prevDownFiles) {
        return new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile file) {
                Calendar since = new GregorianCalendar();
                Date changedSince = prevDownFiles.get(file.getName());
                if (file.isFile() && changedSince != null) {
                    since.setTime(changedSince);
                    return file.getTimestamp().after(since);
                } else {
                    return file.isFile();
                }
            }

        };
    }

    private static boolean isCSV(String filename) {
        String ex = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return ex.toLowerCase().equals("csv");
    }
}
