package com.ibm.icu.text;

public abstract interface UnicodeMatcher
{
  public static final int U_MISMATCH = 0;
  public static final int U_PARTIAL_MATCH = 1;
  public static final int U_MATCH = 2;
  public static final char ETHER = '￿';
  
  public abstract int matches(Replaceable paramReplaceable, int[] paramArrayOfInt, int paramInt, boolean paramBoolean);
  
  public abstract String toPattern(boolean paramBoolean);
  
  public abstract boolean matchesIndexValue(int paramInt);
  
  public abstract void addMatchSetTo(UnicodeSet paramUnicodeSet);
}
