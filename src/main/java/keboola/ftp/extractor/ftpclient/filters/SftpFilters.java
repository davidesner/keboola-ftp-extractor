/*
 */
package keboola.ftp.extractor.ftpclient.filters;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * author David Esner <esnerda at gmail.com>
 * created 2016
 */
public class SftpFilters {

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
    public static final SFTPfilter JUSTFILES_WITH_PREFIX(final String prefix, final String extension) {
        return new SFTPfilter() {
            @Override
            public boolean accept(LsEntry file) {
                if (file == null) {
                    return false;
                }
                SftpATTRS attrs = file.getAttrs();

                return !attrs.isDir() && hasExtension(file.getFilename(), extension) && file.getFilename().startsWith(prefix);
            }

        };
    }

    public static final SFTPfilter FILES_WITH_PREFIX_CHANGED_SINCE(final Date changedSince, final String prefix, final String extension) {
        return new SFTPfilter() {
            @Override
            public boolean accept(LsEntry file) {
                if (file == null) {
                    return false;
                }
                SftpATTRS attrs = file.getAttrs();
                Calendar since = new GregorianCalendar();
                since.setTime(changedSince);
                return !attrs.isDir() && mTimeToCalendar(attrs.getMTime()).after(since) && hasExtension(file.getFilename(), extension) && file.getFilename().startsWith(prefix);
            }

        };
    }

    /**
     * Accepts all (non-null) LsEntry files entries since specified date.
     *
     * @param changedSince
     * @param extension
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
                return !attrs.isDir() && mTimeToCalendar(attrs.getMTime()).after(since) && hasExtension(file.getFilename(), extension);
            }

        };
    }

    private static boolean isCSV(String filename) {
        String ex = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return ex.toLowerCase().equals("csv");
    }

    private static boolean hasExtension(String filename, String extension) {
        String ex = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return ex.toLowerCase().equals(extension);
    }

    private static Calendar mTimeToCalendar(int mTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(mTime * 1000L));
        return cal;
    }
}
