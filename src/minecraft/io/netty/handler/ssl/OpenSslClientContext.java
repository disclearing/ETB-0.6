package io.netty.handler.ssl;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;



























public final class OpenSslClientContext
  extends OpenSslContext
{
  private final OpenSslSessionContext sessionContext;
  
  @Deprecated
  public OpenSslClientContext()
    throws SSLException
  {
    this((File)null, null, null, null, null, null, null, IdentityCipherSuiteFilter.INSTANCE, null, 0L, 0L);
  }
  





  @Deprecated
  public OpenSslClientContext(File certChainFile)
    throws SSLException
  {
    this(certChainFile, null);
  }
  






  @Deprecated
  public OpenSslClientContext(TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    this(null, trustManagerFactory);
  }
  








  @Deprecated
  public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, null, null, null, null, IdentityCipherSuiteFilter.INSTANCE, null, 0L, 0L);
  }
  

















  @Deprecated
  public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, null, null, null, ciphers, IdentityCipherSuiteFilter.INSTANCE, apn, sessionCacheSize, sessionTimeout);
  }
  


















  @Deprecated
  public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, null, null, null, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  



































  @Deprecated
  public OpenSslClientContext(File trustCertCollectionFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(toX509CertificatesInternal(trustCertCollectionFile), trustManagerFactory, toX509CertificatesInternal(keyCertChainFile), toPrivateKeyInternal(keyFile, keyPassword), keyPassword, keyManagerFactory, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  





  OpenSslClientContext(X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    super(ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout, 0, keyCertChain, ClientAuth.NONE, false);
    
    boolean success = false;
    try {
      sessionContext = ReferenceCountedOpenSslClientContext.newSessionContext(this, ctx, engineMap, trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory);
      
      success = true;
    } finally {
      if (!success) {
        release();
      }
    }
  }
  
  public OpenSslSessionContext sessionContext()
  {
    return sessionContext;
  }
  
  OpenSslKeyMaterialManager keyMaterialManager()
  {
    return null;
  }
}
