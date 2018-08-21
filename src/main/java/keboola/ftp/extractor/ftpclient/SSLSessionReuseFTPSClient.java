package keboola.ftp.extractor.ftpclient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import com.google.common.base.Throwables;

/**
 * Some FTP server require SSL session reuse. This may lead to an error '425 Unable to build data connection: Operation not permitted' on LIST command.
 * 
 * A hack is needed to allow SSL session reuse for org.apache.commons.net.ftp.FTPSClient. The implementation is described on  http://eng.wealthfront.com/2016/06/10/connecting-to-an-ftps-server-with-ssl-session-reuse-in-java-7-and-8/
 * 
 * NOTE: Above JDK 8u161 following java property needs to be set: 
 * In case of compatibility issues, an application may disable negotiation of this extension by setting the System Property jdk.tls.useExtendedMasterSecret to false in the JDK
 * (https://stackoverflow.com/questions/32398754/how-to-connect-to-ftps-server-with-data-connection-using-same-tls-session)
 * 
 * The code below is taken from there. 
 * 
 *
 */
public class SSLSessionReuseFTPSClient extends FTPSClient {
	
    /**
     * Constructor for FTPSClient allowing specification of protocol
     * and security mode. If isImplicit is true, the port is set to
     * {@link #DEFAULT_FTPS_PORT} i.e. 990.
     * The default TrustManager is set from {@link TrustManagerUtils#getValidateServerCertificateTrustManager()}
     * @param protocol the protocol
     * @param isImplicit The security mode(Implicit/Explicit).
     */
    public SSLSessionReuseFTPSClient(String protocol, boolean isImplicit) {
        super(protocol, isImplicit);
    }

  // adapted from: https://trac.cyberduck.io/changeset/10760
  @Override
  protected void _prepareDataSocket_(final Socket socket) throws IOException {
    if(socket instanceof SSLSocket) {
      final SSLSession session = ((SSLSocket) _socket_).getSession();
      final SSLSessionContext context = session.getSessionContext();
      try {
        final Field sessionHostPortCache = context.getClass().getDeclaredField("sessionHostPortCache");
        sessionHostPortCache.setAccessible(true);
        final Object cache = sessionHostPortCache.get(context);
        final Method putMethod = cache.getClass().getDeclaredMethod("put", Object.class, Object.class);
        putMethod.setAccessible(true);
        final Method getHostMethod = socket.getClass().getDeclaredMethod("getHost");
        getHostMethod.setAccessible(true);
        Object host = getHostMethod.invoke(socket);
        final String key = String.format("%s:%s", host, String.valueOf(socket.getPort())).toLowerCase(Locale.ROOT);
        putMethod.invoke(cache, key, session);
      } catch(Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

}