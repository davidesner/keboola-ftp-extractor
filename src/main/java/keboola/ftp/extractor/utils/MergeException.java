/*
 */
package keboola.ftp.extractor.utils;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class MergeException extends Exception {

    private final int severity;

    public MergeException(String message) {
        super(message);
        severity = 2;
    }

    public MergeException(String message, int severity) {
        super(message);
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}
