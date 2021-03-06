package com.mojang.authlib.properties;

import java.security.Signature;

public class Property
{
  private final String name;
  private final String value;
  private final String signature;
  
  public Property(String value, String name)
  {
    this(value, name, null);
  }
  
  public Property(String name, String value, String signature) {
    this.name = name;
    this.value = value;
    this.signature = signature;
  }
  
  public String getName() {
    return name;
  }
  
  public String getValue() {
    return value;
  }
  
  public String getSignature() {
    return signature;
  }
  
  public boolean hasSignature() {
    return signature != null;
  }
  
  public boolean isSignatureValid(java.security.PublicKey publicKey) {
    try {
      Signature signature = Signature.getInstance("SHA1withRSA");
      signature.initVerify(publicKey);
      signature.update(value.getBytes());
      return signature.verify(org.apache.commons.codec.binary.Base64.decodeBase64(this.signature));
    } catch (java.security.NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (java.security.InvalidKeyException e) {
      e.printStackTrace();
    } catch (java.security.SignatureException e) {
      e.printStackTrace();
    }
    return false;
  }
}
