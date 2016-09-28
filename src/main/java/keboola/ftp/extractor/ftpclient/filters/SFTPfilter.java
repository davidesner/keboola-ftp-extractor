/*
 */
package keboola.ftp.extractor.ftpclient.filters;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 *
 * author David Esner <esnerda at gmail.com>
 * created 2016
 */
public interface SFTPfilter {

    public boolean accept(LsEntry file);
}
