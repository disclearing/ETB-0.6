package com.ibm.icu.text;

import java.text.ParsePosition;



































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































class NumeratorSubstitution
  extends NFSubstitution
{
  double denominator;
  boolean withZeros;
  
  NumeratorSubstitution(int pos, double denominator, NFRuleSet ruleSet, RuleBasedNumberFormat formatter, String description)
  {
    super(pos, ruleSet, formatter, fixdesc(description));
    



    this.denominator = denominator;
    
    withZeros = description.endsWith("<<");
  }
  
  static String fixdesc(String description) {
    return description.endsWith("<<") ? description.substring(0, description.length() - 1) : description;
  }
  










  public boolean equals(Object that)
  {
    if (super.equals(that)) {
      NumeratorSubstitution that2 = (NumeratorSubstitution)that;
      return denominator == denominator;
    }
    return false;
  }
  
  public int hashCode()
  {
    if (!$assertionsDisabled) throw new AssertionError("hashCode not designed");
    return 42;
  }
  
















  public void doSubstitution(double number, StringBuffer toInsertInto, int position)
  {
    double numberToFormat = transformNumber(number);
    
    if ((withZeros) && (ruleSet != null))
    {
      long nf = numberToFormat;
      int len = toInsertInto.length();
      while (nf *= 10L < denominator) {
        toInsertInto.insert(position + pos, ' ');
        ruleSet.format(0L, toInsertInto, position + pos);
      }
      position += toInsertInto.length() - len;
    }
    


    if ((numberToFormat == Math.floor(numberToFormat)) && (ruleSet != null)) {
      ruleSet.format(numberToFormat, toInsertInto, position + pos);




    }
    else if (ruleSet != null) {
      ruleSet.format(numberToFormat, toInsertInto, position + pos);
    } else {
      toInsertInto.insert(position + pos, numberFormat.format(numberToFormat));
    }
  }
  





  public long transformNumber(long number)
  {
    return Math.round(number * denominator);
  }
  




  public double transformNumber(double number)
  {
    return Math.round(number * denominator);
  }
  














  public Number doParse(String text, ParsePosition parsePosition, double baseValue, double upperBound, boolean lenientParse)
  {
    int zeroCount = 0;
    if (withZeros) {
      String workText = text;
      ParsePosition workPos = new ParsePosition(1);
      for (; 
          
          (workText.length() > 0) && (workPos.getIndex() != 0); 
          












          goto 101)
      {
        workPos.setIndex(0);
        ruleSet.parse(workText, workPos, 1.0D).intValue();
        if (workPos.getIndex() == 0) {
          break;
        }
        


        zeroCount++;
        parsePosition.setIndex(parsePosition.getIndex() + workPos.getIndex());
        workText = workText.substring(workPos.getIndex());
        if ((workText.length() > 0) && (workText.charAt(0) == ' ')) {
          workText = workText.substring(1);
          parsePosition.setIndex(parsePosition.getIndex() + 1);
        }
      }
      
      text = text.substring(parsePosition.getIndex());
      parsePosition.setIndex(0);
    }
    

    Number result = super.doParse(text, parsePosition, withZeros ? 1.0D : baseValue, upperBound, false);
    
    if (withZeros)
    {



      long n = result.longValue();
      long d = 1L;
      while (d <= n) {
        d *= 10L;
      }
      
      while (zeroCount > 0) {
        d *= 10L;
        zeroCount--;
      }
      
      result = new Double(n / d);
    }
    
    return result;
  }
  






  public double composeRuleValue(double newRuleValue, double oldRuleValue)
  {
    return newRuleValue / oldRuleValue;
  }
  




  public double calcUpperBound(double oldUpperBound)
  {
    return denominator;
  }
  







  char tokenChar()
  {
    return '<';
  }
}
