package com.ibm.icu.impl.duration.impl;

abstract interface RecordReader
{
  public abstract boolean open(String paramString);
  
  public abstract boolean close();
  
  public abstract boolean bool(String paramString);
  
  public abstract boolean[] boolArray(String paramString);
  
  public abstract char character(String paramString);
  
  public abstract char[] characterArray(String paramString);
  
  public abstract byte namedIndex(String paramString, String[] paramArrayOfString);
  
  public abstract byte[] namedIndexArray(String paramString, String[] paramArrayOfString);
  
  public abstract String string(String paramString);
  
  public abstract String[] stringArray(String paramString);
  
  public abstract String[][] stringTable(String paramString);
}
