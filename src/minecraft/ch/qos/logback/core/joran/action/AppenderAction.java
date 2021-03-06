package ch.qos.logback.core.joran.action;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;













public class AppenderAction<E>
  extends Action
{
  Appender<E> appender;
  private boolean inError = false;
  



  public AppenderAction() {}
  


  public void begin(InterpretationContext ec, String localName, Attributes attributes)
    throws ActionException
  {
    appender = null;
    inError = false;
    
    String className = attributes.getValue("class");
    if (OptionHelper.isEmpty(className)) {
      addError("Missing class name for appender. Near [" + localName + "] line " + getLineNumber(ec));
      
      inError = true;
      return;
    }
    try
    {
      addInfo("About to instantiate appender of type [" + className + "]");
      
      appender = ((Appender)OptionHelper.instantiateByClassName(className, Appender.class, context));
      

      appender.setContext(context);
      
      String appenderName = ec.subst(attributes.getValue("name"));
      
      if (OptionHelper.isEmpty(appenderName)) {
        addWarn("No appender name given for appender of type " + className + "].");
      }
      else {
        appender.setName(appenderName);
        addInfo("Naming appender as [" + appenderName + "]");
      }
      


      HashMap<String, Appender<E>> appenderBag = (HashMap)ec.getObjectMap().get("APPENDER_BAG");
      


      appenderBag.put(appenderName, appender);
      
      ec.pushObject(appender);
    } catch (Exception oops) {
      inError = true;
      addError("Could not create an Appender of type [" + className + "].", oops);
      
      throw new ActionException(oops);
    }
  }
  



  public void end(InterpretationContext ec, String name)
  {
    if (inError) {
      return;
    }
    
    if ((appender instanceof LifeCycle)) {
      appender.start();
    }
    
    Object o = ec.peekObject();
    
    if (o != appender) {
      addWarn("The object at the of the stack is not the appender named [" + appender.getName() + "] pushed earlier.");
    }
    else {
      ec.popObject();
    }
  }
}
