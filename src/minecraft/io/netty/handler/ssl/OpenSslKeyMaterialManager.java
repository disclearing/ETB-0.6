package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.security.auth.x500.X500Principal;
import org.apache.tomcat.jni.CertificateRequestedCallback.KeyMaterial;
import org.apache.tomcat.jni.SSL;
































class OpenSslKeyMaterialManager
{
  static final String KEY_TYPE_RSA = "RSA";
  static final String KEY_TYPE_DH_RSA = "DH_RSA";
  static final String KEY_TYPE_EC = "EC";
  static final String KEY_TYPE_EC_EC = "EC_EC";
  static final String KEY_TYPE_EC_RSA = "EC_RSA";
  private static final Map<String, String> KEY_TYPES = new HashMap();
  
  static { KEY_TYPES.put("RSA", "RSA");
    KEY_TYPES.put("DHE_RSA", "RSA");
    KEY_TYPES.put("ECDHE_RSA", "RSA");
    KEY_TYPES.put("ECDHE_ECDSA", "EC");
    KEY_TYPES.put("ECDH_RSA", "EC_RSA");
    KEY_TYPES.put("ECDH_ECDSA", "EC_EC");
    KEY_TYPES.put("DH_RSA", "DH_RSA");
  }
  
  private final X509KeyManager keyManager;
  private final String password;
  OpenSslKeyMaterialManager(X509KeyManager keyManager, String password)
  {
    this.keyManager = keyManager;
    this.password = password;
  }
  
  void setKeyMaterial(ReferenceCountedOpenSslEngine engine) throws SSLException {
    long ssl = engine.sslPointer();
    String[] authMethods = SSL.authenticationMethods(ssl);
    Set<String> aliases = new HashSet(authMethods.length);
    for (String authMethod : authMethods) {
      String type = (String)KEY_TYPES.get(authMethod);
      if (type != null) {
        String alias = chooseServerAlias(engine, type);
        if ((alias != null) && (aliases.add(alias))) {
          setKeyMaterial(ssl, alias);
        }
      }
    }
  }
  
  CertificateRequestedCallback.KeyMaterial keyMaterial(ReferenceCountedOpenSslEngine engine, String[] keyTypes, X500Principal[] issuer) throws SSLException
  {
    String alias = chooseClientAlias(engine, keyTypes, issuer);
    long keyBio = 0L;
    long keyCertChainBio = 0L;
    long pkey = 0L;
    long certChain = 0L;
    
    try
    {
      X509Certificate[] certificates = keyManager.getCertificateChain(alias);
      if ((certificates == null) || (certificates.length == 0)) {
        return null;
      }
      
      PrivateKey key = keyManager.getPrivateKey(alias);
      keyCertChainBio = ReferenceCountedOpenSslContext.toBIO(certificates);
      certChain = SSL.parseX509Chain(keyCertChainBio);
      if (key != null) {
        keyBio = ReferenceCountedOpenSslContext.toBIO(key);
        pkey = SSL.parsePrivateKey(keyBio, password);
      }
      CertificateRequestedCallback.KeyMaterial material = new CertificateRequestedCallback.KeyMaterial(certChain, pkey);
      




      certChain = pkey = 0L;
      return material;
    } catch (SSLException e) {
      throw e;
    } catch (Exception e) {
      throw new SSLException(e);
    } finally {
      ReferenceCountedOpenSslContext.freeBio(keyBio);
      ReferenceCountedOpenSslContext.freeBio(keyCertChainBio);
      SSL.freePrivateKey(pkey);
      SSL.freeX509Chain(certChain);
    }
  }
  
  private void setKeyMaterial(long ssl, String alias) throws SSLException {
    long keyBio = 0L;
    long keyCertChainBio = 0L;
    long keyCertChainBio2 = 0L;
    
    try
    {
      X509Certificate[] certificates = keyManager.getCertificateChain(alias);
      if ((certificates == null) || (certificates.length == 0)) {
        return;
      }
      
      PrivateKey key = keyManager.getPrivateKey(alias);
      

      PemEncoded encoded = PemX509Certificate.toPEM(ByteBufAllocator.DEFAULT, true, certificates);
      try {
        keyCertChainBio = ReferenceCountedOpenSslContext.toBIO(ByteBufAllocator.DEFAULT, encoded.retain());
        keyCertChainBio2 = ReferenceCountedOpenSslContext.toBIO(ByteBufAllocator.DEFAULT, encoded.retain());
        
        if (key != null) {
          keyBio = ReferenceCountedOpenSslContext.toBIO(key);
        }
        SSL.setCertificateBio(ssl, keyCertChainBio, keyBio, password);
        

        SSL.setCertificateChainBio(ssl, keyCertChainBio2, true);
      } finally {
        encoded.release();
      }
    } catch (SSLException e) {
      throw e;
    } catch (Exception e) {
      throw new SSLException(e);
    } finally {
      ReferenceCountedOpenSslContext.freeBio(keyBio);
      ReferenceCountedOpenSslContext.freeBio(keyCertChainBio);
      ReferenceCountedOpenSslContext.freeBio(keyCertChainBio2);
    }
  }
  
  protected String chooseClientAlias(ReferenceCountedOpenSslEngine engine, String[] keyTypes, X500Principal[] issuer)
  {
    return keyManager.chooseClientAlias(keyTypes, issuer, null);
  }
  
  protected String chooseServerAlias(ReferenceCountedOpenSslEngine engine, String type) {
    return keyManager.chooseServerAlias(type, null, null);
  }
}
