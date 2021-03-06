package com.ibm.icu.impl.locale;



public class ParseStatus
{
  public ParseStatus() {}
  

  int _parseLength = 0;
  int _errorIndex = -1;
  String _errorMsg = null;
  
  public void reset() {
    _parseLength = 0;
    _errorIndex = -1;
    _errorMsg = null;
  }
  
  public boolean isError() {
    return _errorIndex >= 0;
  }
  
  public int getErrorIndex() {
    return _errorIndex;
  }
  
  public int getParseLength() {
    return _parseLength;
  }
  
  public String getErrorMessage() {
    return _errorMsg;
  }
}
