package com.sun.jna;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.Adler32;























































public abstract class Structure
{
  private static final boolean REVERSE_FIELDS;
  private static final boolean REQUIRES_FIELD_ORDER;
  static final boolean isPPC;
  static final boolean isSPARC;
  static final boolean isARM;
  public static final int ALIGN_DEFAULT = 0;
  public static final int ALIGN_NONE = 1;
  public static final int ALIGN_GNUC = 2;
  public static final int ALIGN_MSVC = 3;
  
  public static abstract interface ByValue {}
  
  public static abstract interface ByReference {}
  
  private static class MemberOrder
  {
    private static final String[] FIELDS = { "first", "second", "middle", "penultimate", "last" };
    

    public int first;
    
    public int second;
    
    public int middle;
    
    public int penultimate;
    
    public int last;
    

    private MemberOrder() {}
  }
  

  static
  {
    Field[] fields = MemberOrder.class.getFields();
    List names = new ArrayList();
    for (int i = 0; i < fields.length; i++) {
      names.add(fields[i].getName());
    }
    List expected = Arrays.asList(MemberOrder.FIELDS);
    List reversed = new ArrayList(expected);
    Collections.reverse(reversed);
    
    REVERSE_FIELDS = names.equals(reversed);
    REQUIRES_FIELD_ORDER = (!names.equals(expected)) && (!REVERSE_FIELDS);
    String arch = System.getProperty("os.arch").toLowerCase();
    isPPC = ("ppc".equals(arch)) || ("powerpc".equals(arch));
    isSPARC = "sparc".equals(arch);
    isARM = "arm".equals(arch);
  }
  
















  static final int MAX_GNUC_ALIGNMENT = (isSPARC) || (((isPPC) || (isARM)) && (Platform.isLinux())) ? 8 : Native.LONG_SIZE;
  
  protected static final int CALCULATE_SIZE = -1;
  
  static final Map layoutInfo = new WeakHashMap();
  
  private Pointer memory;
  
  private int size = -1;
  
  private int alignType;
  private int structAlignment;
  private Map structFields;
  private final Map nativeStrings = new HashMap();
  
  private TypeMapper typeMapper;
  private long typeInfo;
  private List fieldOrder;
  private boolean autoRead = true;
  private boolean autoWrite = true;
  private Structure[] array;
  
  protected Structure() {
    this((Pointer)null);
  }
  
  protected Structure(TypeMapper mapper) {
    this((Pointer)null, 0, mapper);
  }
  
  protected Structure(Pointer p)
  {
    this(p, 0);
  }
  
  protected Structure(Pointer p, int alignType) {
    this(p, alignType, null);
  }
  
  protected Structure(Pointer p, int alignType, TypeMapper mapper) {
    setAlignType(alignType);
    setTypeMapper(mapper);
    if (p != null) {
      useMemory(p);
    }
    else {
      allocateMemory(-1);
    }
  }
  





  Map fields()
  {
    return structFields;
  }
  
  TypeMapper getTypeMapper()
  {
    return typeMapper;
  }
  




  protected void setTypeMapper(TypeMapper mapper)
  {
    if (mapper == null) {
      Class declaring = getClass().getDeclaringClass();
      if (declaring != null) {
        mapper = Native.getTypeMapper(declaring);
      }
    }
    typeMapper = mapper;
    size = -1;
    if ((memory instanceof AutoAllocated)) {
      memory = null;
    }
  }
  



  protected void setAlignType(int alignType)
  {
    if (alignType == 0) {
      Class declaring = getClass().getDeclaringClass();
      if (declaring != null)
        alignType = Native.getStructureAlignment(declaring);
      if (alignType == 0) {
        if (Platform.isWindows()) {
          alignType = 3;
        } else
          alignType = 2;
      }
    }
    this.alignType = alignType;
    size = -1;
    if ((memory instanceof AutoAllocated)) {
      memory = null;
    }
  }
  
  protected Memory autoAllocate(int size) {
    return new AutoAllocated(size);
  }
  




  protected void useMemory(Pointer m)
  {
    useMemory(m, 0);
  }
  







  protected void useMemory(Pointer m, int offset)
  {
    try
    {
      memory = m.share(offset);
      if (size == -1) {
        size = calculateSize(false);
      }
      if (size != -1) {
        memory = m.share(offset, size);
      }
      array = null;
    }
    catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Structure exceeds provided memory bounds");
    }
  }
  

  protected void ensureAllocated()
  {
    ensureAllocated(false);
  }
  




  private void ensureAllocated(boolean avoidFFIType)
  {
    if (memory == null) {
      allocateMemory(avoidFFIType);
    }
    else if (size == -1) {
      size = calculateSize(true, avoidFFIType);
    }
  }
  


  protected void allocateMemory()
  {
    allocateMemory(false);
  }
  
  private void allocateMemory(boolean avoidFFIType) {
    allocateMemory(calculateSize(true, avoidFFIType));
  }
  





  protected void allocateMemory(int size)
  {
    if (size == -1)
    {
      size = calculateSize(false);
    }
    else if (size <= 0) {
      throw new IllegalArgumentException("Structure size must be greater than zero: " + size);
    }
    

    if (size != -1) {
      if ((memory == null) || ((memory instanceof AutoAllocated)))
      {
        memory = autoAllocate(size);
      }
      this.size = size;
    }
  }
  
  public int size() {
    ensureAllocated();
    if (size == -1) {
      size = calculateSize(true);
    }
    return size;
  }
  
  public void clear() {
    memory.clear(size());
  }
  






  public Pointer getPointer()
  {
    ensureAllocated();
    return memory;
  }
  






  private static final ThreadLocal reads = new ThreadLocal() {
    protected synchronized Object initialValue() {
      return new HashMap();
    }
  };
  


  private static final ThreadLocal busy = new ThreadLocal() {
    class StructureSet extends AbstractCollection implements Set {
      private Structure[] elements;
      private int count;
      
      StructureSet() {}
      
      private void ensureCapacity(int size) {
        if (elements == null) {
          elements = new Structure[size * 3 / 2];
        }
        else if (elements.length < size) {
          Structure[] e = new Structure[size * 3 / 2];
          System.arraycopy(elements, 0, e, 0, elements.length);
          elements = e;
        } }
      
      public int size() { return count; }
      
      public boolean contains(Object o) { return indexOf(o) != -1; }
      
      public boolean add(Object o) {
        if (!contains(o)) {
          ensureCapacity(count + 1);
          elements[(count++)] = ((Structure)o);
        }
        return true;
      }
      
      private int indexOf(Object o) { Structure s1 = (Structure)o;
        for (int i = 0; i < count; i++) {
          Structure s2 = elements[i];
          if ((s1 == s2) || ((s1.getClass() == s2.getClass()) && (s1.size() == s2.size()) && (s1.getPointer().equals(s2.getPointer()))))
          {


            return i;
          }
        }
        return -1;
      }
      
      public boolean remove(Object o) { int idx = indexOf(o);
        if (idx != -1) {
          if (--count > 0) {
            elements[idx] = elements[count];
            elements[count] = null;
          }
          return true;
        }
        return false;
      }
      

      public Iterator iterator()
      {
        Structure[] e = new Structure[count];
        if (count > 0) {
          System.arraycopy(elements, 0, e, 0, count);
        }
        return Arrays.asList(e).iterator();
      }
    }
    
    protected synchronized Object initialValue() { return new StructureSet(); }
  };
  
  static Set busy() {
    return (Set)busy.get();
  }
  
  static Map reading() { return (Map)reads.get(); }
  







  public void read()
  {
    ensureAllocated();
    

    if (busy().contains(this)) {
      return;
    }
    busy().add(this);
    if ((this instanceof ByReference)) {
      reading().put(getPointer(), this);
    }
    try {
      for (i = fields().values().iterator(); i.hasNext();) {
        readField((StructField)i.next());
      }
    } finally {
      Iterator i;
      busy().remove(this);
      if (reading().get(getPointer()) == this) {
        reading().remove(getPointer());
      }
    }
  }
  
  protected int fieldOffset(String name)
  {
    ensureAllocated();
    StructField f = (StructField)fields().get(name);
    if (f == null)
      throw new IllegalArgumentException("No such field: " + name);
    return offset;
  }
  




  public Object readField(String name)
  {
    ensureAllocated();
    StructField f = (StructField)fields().get(name);
    if (f == null)
      throw new IllegalArgumentException("No such field: " + name);
    return readField(f);
  }
  

  Object getField(StructField structField)
  {
    try
    {
      return field.get(this);
    }
    catch (Exception e) {
      throw new Error("Exception reading field '" + name + "' in " + getClass() + ": " + e);
    }
  }
  

  void setField(StructField structField, Object value)
  {
    setField(structField, value, false);
  }
  
  void setField(StructField structField, Object value, boolean overrideFinal) {
    try {
      field.set(this, value);
    }
    catch (IllegalAccessException e) {
      int modifiers = field.getModifiers();
      if (Modifier.isFinal(modifiers)) {
        if (overrideFinal)
        {

          throw new UnsupportedOperationException("This VM does not support Structures with final fields (field '" + name + "' within " + getClass() + ")");
        }
        throw new UnsupportedOperationException("Attempt to write to read-only field '" + name + "' within " + getClass());
      }
      throw new Error("Unexpectedly unable to write to field '" + name + "' within " + getClass() + ": " + e);
    }
  }
  








  static Structure updateStructureByReference(Class type, Structure s, Pointer address)
  {
    if (address == null) {
      s = null;
    }
    else {
      if ((s == null) || (!address.equals(s.getPointer()))) {
        Structure s1 = (Structure)reading().get(address);
        if ((s1 != null) && (type.equals(s1.getClass()))) {
          s = s1;
        }
        else {
          s = newInstance(type);
          s.useMemory(address);
        }
      }
      s.autoRead();
    }
    return s;
  }
  





  Object readField(StructField structField)
  {
    int offset = offset;
    

    Class fieldType = type;
    FromNativeConverter readConverter = readConverter;
    if (readConverter != null) {
      fieldType = readConverter.nativeType();
    }
    
    Object currentValue = (Structure.class.isAssignableFrom(fieldType)) || (Callback.class.isAssignableFrom(fieldType)) || ((Platform.HAS_BUFFERS) && (Buffer.class.isAssignableFrom(fieldType))) || (Pointer.class.isAssignableFrom(fieldType)) || (NativeMapped.class.isAssignableFrom(fieldType)) || (fieldType.isArray()) ? getField(structField) : null;
    






    Object result = memory.getValue(offset, fieldType, currentValue);
    if (readConverter != null) {
      result = readConverter.fromNative(result, context);
    }
    

    setField(structField, result, true);
    return result;
  }
  





  public void write()
  {
    ensureAllocated();
    

    if ((this instanceof ByValue)) {
      getTypeInfo();
    }
    

    if (busy().contains(this)) {
      return;
    }
    busy().add(this);
    try
    {
      for (i = fields().values().iterator(); i.hasNext();) {
        StructField sf = (StructField)i.next();
        if (!isVolatile) {
          writeField(sf);
        }
      }
    } finally {
      Iterator i;
      busy().remove(this);
    }
  }
  



  public void writeField(String name)
  {
    ensureAllocated();
    StructField f = (StructField)fields().get(name);
    if (f == null)
      throw new IllegalArgumentException("No such field: " + name);
    writeField(f);
  }
  




  public void writeField(String name, Object value)
  {
    ensureAllocated();
    StructField f = (StructField)fields().get(name);
    if (f == null)
      throw new IllegalArgumentException("No such field: " + name);
    setField(f, value);
    writeField(f);
  }
  
  void writeField(StructField structField)
  {
    if (isReadOnly) {
      return;
    }
    
    int offset = offset;
    

    Object value = getField(structField);
    

    Class fieldType = type;
    ToNativeConverter converter = writeConverter;
    if (converter != null) {
      value = converter.toNative(value, new StructureWriteContext(this, field));
      fieldType = converter.nativeType();
    }
    

    if ((String.class == fieldType) || (WString.class == fieldType))
    {


      boolean wide = fieldType == WString.class;
      if (value != null) {
        NativeString nativeString = new NativeString(value.toString(), wide);
        

        nativeStrings.put(name, nativeString);
        value = nativeString.getPointer();
      }
      else {
        value = null;
        nativeStrings.remove(name);
      }
    }
    try
    {
      memory.setValue(offset, value, fieldType);
    }
    catch (IllegalArgumentException e) {
      String msg = "Structure field \"" + name + "\" was declared as " + type + (type == fieldType ? "" : new StringBuffer().append(" (native type ").append(fieldType).append(")").toString()) + ", which is not supported within a Structure";
      



      throw new IllegalArgumentException(msg);
    }
  }
  
  private boolean hasFieldOrder() {
    synchronized (this) {
      return fieldOrder != null;
    }
  }
  
  protected List getFieldOrder() {
    synchronized (this) {
      if (fieldOrder == null) {
        fieldOrder = new ArrayList();
      }
      return fieldOrder;
    }
  }
  


  protected void setFieldOrder(String[] fields)
  {
    getFieldOrder().addAll(Arrays.asList(fields));
    

    size = -1;
    if ((memory instanceof AutoAllocated)) {
      memory = null;
    }
  }
  
  protected void sortFields(List fields, List names)
  {
    for (int i = 0; i < names.size(); i++) {
      String name = (String)names.get(i);
      for (int f = 0; f < fields.size(); f++) {
        Field field = (Field)fields.get(f);
        if (name.equals(field.getName())) {
          Collections.swap(fields, i, f);
          break;
        }
      }
    }
  }
  
  protected List getFields(boolean force)
  {
    List flist = new ArrayList();
    for (Class cls = getClass(); 
        !cls.equals(Structure.class); 
        cls = cls.getSuperclass()) {
      List classFields = new ArrayList();
      Field[] fields = cls.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        int modifiers = fields[i].getModifiers();
        if ((!Modifier.isStatic(modifiers)) && (Modifier.isPublic(modifiers)))
        {

          classFields.add(fields[i]); }
      }
      if (REVERSE_FIELDS) {
        Collections.reverse(classFields);
      }
      flist.addAll(0, classFields);
    }
    if ((REQUIRES_FIELD_ORDER) || (hasFieldOrder())) {
      List fieldOrder = getFieldOrder();
      if (fieldOrder.size() < flist.size()) {
        if (force) {
          throw new Error("This VM does not store fields in a predictable order; you must use Structure.setFieldOrder to explicitly indicate the field order: " + System.getProperty("java.vendor") + ", " + System.getProperty("java.version"));
        }
        return null;
      }
      sortFields(flist, fieldOrder);
    }
    return flist;
  }
  
  private synchronized boolean fieldOrderMatch(List fieldOrder)
  {
    return (this.fieldOrder == fieldOrder) || ((this.fieldOrder != null) && (this.fieldOrder.equals(fieldOrder)));
  }
  











  private int calculateSize(boolean force)
  {
    return calculateSize(force, false);
  }
  
  int calculateSize(boolean force, boolean avoidFFIType)
  {
    boolean needsInit = true;
    LayoutInfo info; synchronized (layoutInfo) {
      info = (LayoutInfo)layoutInfo.get(getClass());
    }
    if ((info == null) || (alignType != alignType) || (typeMapper != typeMapper) || (!fieldOrderMatch(fieldOrder)))
    {


      info = deriveLayout(force, avoidFFIType);
      needsInit = false;
    }
    if (info != null) {
      structAlignment = alignment;
      structFields = fields;
      alignType = alignType;
      typeMapper = typeMapper;
      fieldOrder = fieldOrder;
      if (!variable) {
        synchronized (layoutInfo) {
          layoutInfo.put(getClass(), info);
        }
      }
      if (needsInit) {
        initializeFields();
      }
      return size;
    }
    return -1;
  }
  
  private class LayoutInfo {
    private LayoutInfo() {}
    
    LayoutInfo(Structure.1 x1) { this(); }
    int size = -1;
    int alignment = 1;
    Map fields = Collections.synchronizedMap(new LinkedHashMap());
    int alignType = 0;
    
    TypeMapper typeMapper;
    
    List fieldOrder;
    boolean variable;
  }
  
  private LayoutInfo deriveLayout(boolean force, boolean avoidFFIType)
  {
    LayoutInfo info = new LayoutInfo(null);
    int calculatedSize = 0;
    List fields = getFields(force);
    if (fields == null) {
      return null;
    }
    
    boolean firstField = true;
    for (Iterator i = fields.iterator(); i.hasNext(); firstField = false) {
      Field field = (Field)i.next();
      int modifiers = field.getModifiers();
      
      Class type = field.getType();
      if (type.isArray()) {
        variable = true;
      }
      StructField structField = new StructField();
      isVolatile = Modifier.isVolatile(modifiers);
      isReadOnly = Modifier.isFinal(modifiers);
      if (isReadOnly) {
        if (!Platform.RO_FIELDS) {
          throw new IllegalArgumentException("This VM does not support read-only fields (field '" + field.getName() + "' within " + getClass() + ")");
        }
        

        field.setAccessible(true);
      }
      field = field;
      name = field.getName();
      type = type;
      

      if ((Callback.class.isAssignableFrom(type)) && (!type.isInterface())) {
        throw new IllegalArgumentException("Structure Callback field '" + field.getName() + "' must be an interface");
      }
      

      if ((type.isArray()) && (Structure.class.equals(type.getComponentType())))
      {
        String msg = "Nested Structure arrays must use a derived Structure type so that the size of the elements can be determined";
        

        throw new IllegalArgumentException(msg);
      }
      
      int fieldAlignment = 1;
      if (Modifier.isPublic(field.getModifiers()))
      {


        Object value = getField(structField);
        if ((value == null) && (type.isArray())) {
          if (force) {
            throw new IllegalStateException("Array fields must be initialized");
          }
          
          return null;
        }
        Class nativeType = type;
        if (NativeMapped.class.isAssignableFrom(type)) {
          NativeMappedConverter tc = NativeMappedConverter.getInstance(type);
          nativeType = tc.nativeType();
          writeConverter = tc;
          readConverter = tc;
          context = new StructureReadContext(this, field);
        }
        else if (typeMapper != null) {
          ToNativeConverter writeConverter = typeMapper.getToNativeConverter(type);
          FromNativeConverter readConverter = typeMapper.getFromNativeConverter(type);
          if ((writeConverter != null) && (readConverter != null)) {
            value = writeConverter.toNative(value, new StructureWriteContext(this, field));
            
            nativeType = Pointer.class;
            writeConverter = writeConverter;
            readConverter = readConverter;
            context = new StructureReadContext(this, field);
          }
          else if ((writeConverter != null) || (readConverter != null)) {
            String msg = "Structures require bidirectional type conversion for " + type;
            throw new IllegalArgumentException(msg);
          }
        }
        
        if (value == null) {
          value = initializeField(structField, type);
        }
        try
        {
          size = getNativeSize(nativeType, value);
          fieldAlignment = getNativeAlignment(nativeType, value, firstField);
        }
        catch (IllegalArgumentException e)
        {
          if ((!force) && (typeMapper == null)) {
            return null;
          }
          String msg = "Invalid Structure field in " + getClass() + ", field name '" + name + "', " + type + ": " + e.getMessage();
          throw new IllegalArgumentException(msg);
        }
        

        alignment = Math.max(alignment, fieldAlignment);
        if (calculatedSize % fieldAlignment != 0) {
          calculatedSize += fieldAlignment - calculatedSize % fieldAlignment;
        }
        offset = calculatedSize;
        calculatedSize += size;
        

        fields.put(name, structField);
      }
    }
    if (calculatedSize > 0) {
      int size = calculateAlignedSize(calculatedSize, alignment);
      
      if (((this instanceof ByValue)) && (!avoidFFIType)) {
        getTypeInfo();
      }
      if ((memory != null) && (!(memory instanceof AutoAllocated)))
      {

        memory = memory.share(0L, size);
      }
      size = size;
      return info;
    }
    
    throw new IllegalArgumentException("Structure " + getClass() + " has unknown size (ensure " + "all fields are public)");
  }
  



  private void initializeFields()
  {
    for (Iterator i = fields().values().iterator(); i.hasNext();) {
      StructField f = (StructField)i.next();
      initializeField(f, type);
    }
  }
  
  private Object initializeField(StructField structField, Class type) {
    Object value = null;
    if ((Structure.class.isAssignableFrom(type)) && (!ByReference.class.isAssignableFrom(type)))
    {
      try {
        value = newInstance(type);
        setField(structField, value);
      }
      catch (IllegalArgumentException e) {
        String msg = "Can't determine size of nested structure: " + e.getMessage();
        
        throw new IllegalArgumentException(msg);
      }
    }
    else if (NativeMapped.class.isAssignableFrom(type)) {
      NativeMappedConverter tc = NativeMappedConverter.getInstance(type);
      value = tc.defaultValue();
      setField(structField, value);
    }
    return value;
  }
  
  int calculateAlignedSize(int calculatedSize) {
    return calculateAlignedSize(calculatedSize, structAlignment);
  }
  

  private int calculateAlignedSize(int calculatedSize, int alignment)
  {
    if ((alignType != 1) && 
      (calculatedSize % alignment != 0)) {
      calculatedSize += alignment - calculatedSize % alignment;
    }
    
    return calculatedSize;
  }
  
  protected int getStructAlignment() {
    if (size == -1)
    {
      calculateSize(true);
    }
    return structAlignment;
  }
  



  protected int getNativeAlignment(Class type, Object value, boolean isFirstElement)
  {
    int alignment = 1;
    if (NativeMapped.class.isAssignableFrom(type)) {
      NativeMappedConverter tc = NativeMappedConverter.getInstance(type);
      type = tc.nativeType();
      value = tc.toNative(value, new ToNativeContext());
    }
    int size = Native.getNativeSize(type, value);
    if ((type.isPrimitive()) || (Long.class == type) || (Integer.class == type) || (Short.class == type) || (Character.class == type) || (Byte.class == type) || (Boolean.class == type) || (Float.class == type) || (Double.class == type))
    {


      alignment = size;
    }
    else if ((Pointer.class == type) || ((Platform.HAS_BUFFERS) && (Buffer.class.isAssignableFrom(type))) || (Callback.class.isAssignableFrom(type)) || (WString.class == type) || (String.class == type))
    {



      alignment = Pointer.SIZE;
    }
    else if (Structure.class.isAssignableFrom(type)) {
      if (ByReference.class.isAssignableFrom(type)) {
        alignment = Pointer.SIZE;
      }
      else {
        if (value == null)
          value = newInstance(type);
        alignment = ((Structure)value).getStructAlignment();
      }
    }
    else if (type.isArray()) {
      alignment = getNativeAlignment(type.getComponentType(), null, isFirstElement);
    }
    else {
      throw new IllegalArgumentException("Type " + type + " has unknown " + "native alignment");
    }
    
    if (alignType == 1) {
      alignment = 1;
    }
    else if (alignType == 3) {
      alignment = Math.min(8, alignment);
    }
    else if (alignType == 2)
    {

      if ((!isFirstElement) || (!Platform.isMac()) || (!isPPC)) {
        alignment = Math.min(MAX_GNUC_ALIGNMENT, alignment);
      }
    }
    return alignment;
  }
  
  public String toString() {
    return toString(Boolean.getBoolean("jna.dump_memory"));
  }
  
  public String toString(boolean debug) {
    return toString(0, true, true);
  }
  
  private String format(Class type) {
    String s = type.getName();
    int dot = s.lastIndexOf(".");
    return s.substring(dot + 1);
  }
  
  private String toString(int indent, boolean showContents, boolean dumpMemory) {
    ensureAllocated();
    String LS = System.getProperty("line.separator");
    String name = format(getClass()) + "(" + getPointer() + ")";
    if (!(getPointer() instanceof Memory)) {
      name = name + " (" + size() + " bytes)";
    }
    String prefix = "";
    for (int idx = 0; idx < indent; idx++) {
      prefix = prefix + "  ";
    }
    String contents = LS;
    Iterator i; if (!showContents) {
      contents = "...}";
    } else
      for (i = fields().values().iterator(); i.hasNext();) {
        StructField sf = (StructField)i.next();
        Object value = getField(sf);
        String type = format(type);
        String index = "";
        contents = contents + prefix;
        if ((type.isArray()) && (value != null)) {
          type = format(type.getComponentType());
          index = "[" + Array.getLength(value) + "]";
        }
        contents = contents + "  " + type + " " + name + index + "@" + Integer.toHexString(offset);
        
        if ((value instanceof Structure)) {
          value = ((Structure)value).toString(indent + 1, !(value instanceof ByReference), dumpMemory);
        }
        contents = contents + "=";
        if ((value instanceof Long)) {
          contents = contents + Long.toHexString(((Long)value).longValue());
        }
        else if ((value instanceof Integer)) {
          contents = contents + Integer.toHexString(((Integer)value).intValue());
        }
        else if ((value instanceof Short)) {
          contents = contents + Integer.toHexString(((Short)value).shortValue());
        }
        else if ((value instanceof Byte)) {
          contents = contents + Integer.toHexString(((Byte)value).byteValue());
        }
        else {
          contents = contents + String.valueOf(value).trim();
        }
        contents = contents + LS;
        if (!i.hasNext())
          contents = contents + prefix + "}";
      }
    if ((indent == 0) && (dumpMemory)) {
      int BYTES_PER_ROW = 4;
      contents = contents + LS + "memory dump" + LS;
      byte[] buf = getPointer().getByteArray(0L, size());
      for (int i = 0; i < buf.length; i++) {
        if (i % 4 == 0) contents = contents + "[";
        if ((buf[i] >= 0) && (buf[i] < 16))
          contents = contents + "0";
        contents = contents + Integer.toHexString(buf[i] & 0xFF);
        if ((i % 4 == 3) && (i < buf.length - 1))
          contents = contents + "]" + LS;
      }
      contents = contents + "]";
    }
    return name + " {" + contents;
  }
  





  public Structure[] toArray(Structure[] array)
  {
    ensureAllocated();
    if ((memory instanceof AutoAllocated))
    {
      Memory m = (Memory)memory;
      int requiredSize = array.length * size();
      if (m.size() < requiredSize) {
        useMemory(autoAllocate(requiredSize));
      }
    }
    array[0] = this;
    int size = size();
    for (int i = 1; i < array.length; i++) {
      array[i] = newInstance(getClass());
      array[i].useMemory(memory.share(i * size, size));
      array[i].read();
    }
    
    if (!(this instanceof ByValue))
    {
      this.array = array;
    }
    
    return array;
  }
  





  public Structure[] toArray(int size)
  {
    return toArray((Structure[])Array.newInstance(getClass(), size));
  }
  
  private Class baseClass() {
    if ((((this instanceof ByReference)) || ((this instanceof ByValue))) && (Structure.class.isAssignableFrom(getClass().getSuperclass())))
    {

      return getClass().getSuperclass();
    }
    return getClass();
  }
  


  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Structure)) {
      return false;
    }
    if ((o.getClass() != getClass()) && (((Structure)o).baseClass() != baseClass()))
    {
      return false;
    }
    Structure s = (Structure)o;
    if (s.getPointer().equals(getPointer())) {
      return true;
    }
    if (s.size() == size()) {
      clear();write();
      byte[] buf = getPointer().getByteArray(0L, size());
      s.clear();s.write();
      byte[] sbuf = s.getPointer().getByteArray(0L, s.size());
      return Arrays.equals(buf, sbuf);
    }
    return false;
  }
  


  public int hashCode()
  {
    clear();write();
    Adler32 code = new Adler32();
    code.update(getPointer().getByteArray(0L, size()));
    return (int)code.getValue();
  }
  
  protected void cacheTypeInfo(Pointer p) {
    typeInfo = peer;
  }
  
  protected Pointer getFieldTypeInfo(StructField f)
  {
    Class type = type;
    Object value = getField(f);
    if (typeMapper != null) {
      ToNativeConverter nc = typeMapper.getToNativeConverter(type);
      if (nc != null) {
        type = nc.nativeType();
        value = nc.toNative(value, new ToNativeContext());
      }
    }
    return FFIType.get(value, type);
  }
  
  Pointer getTypeInfo()
  {
    Pointer p = getTypeInfo(this);
    cacheTypeInfo(p);
    return p;
  }
  







  public void setAutoSynch(boolean auto)
  {
    setAutoRead(auto);
    setAutoWrite(auto);
  }
  


  public void setAutoRead(boolean auto)
  {
    autoRead = auto;
  }
  


  public boolean getAutoRead()
  {
    return autoRead;
  }
  


  public void setAutoWrite(boolean auto)
  {
    autoWrite = auto;
  }
  


  public boolean getAutoWrite()
  {
    return autoWrite;
  }
  
  static Pointer getTypeInfo(Object obj)
  {
    return FFIType.get(obj);
  }
  


  public static Structure newInstance(Class type)
    throws IllegalArgumentException
  {
    try
    {
      Structure s = (Structure)type.newInstance();
      if ((s instanceof ByValue)) {
        s.allocateMemory();
      }
      return s;
    }
    catch (InstantiationException e) {
      String msg = "Can't instantiate " + type + " (" + e + ")";
      throw new IllegalArgumentException(msg);
    }
    catch (IllegalAccessException e) {
      String msg = "Instantiation of " + type + " not allowed, is it public? (" + e + ")";
      
      throw new IllegalArgumentException(msg); } }
  
  class StructField { public String name;
    public Class type;
    public Field field;
    
    StructField() {}
    
    public int size = -1;
    public int offset = -1;
    public boolean isVolatile;
    public boolean isReadOnly;
    public FromNativeConverter readConverter;
    public ToNativeConverter writeConverter;
    public FromNativeContext context;
    
    public String toString() { return name + "@" + offset + "[" + size + "] (" + type + ")"; } }
  
  static class FFIType extends Structure { private static class FFITypes { private static Pointer ffi_type_void;
      private static Pointer ffi_type_float;
      private static Pointer ffi_type_double;
      private static Pointer ffi_type_longdouble;
      private static Pointer ffi_type_uint8;
      private static Pointer ffi_type_sint8; private static Pointer ffi_type_uint16; private static Pointer ffi_type_sint16; private static Pointer ffi_type_uint32; private static Pointer ffi_type_sint32; private static Pointer ffi_type_uint64; private static Pointer ffi_type_sint64; private static Pointer ffi_type_pointer;
      private FFITypes() {} }
    public static class size_t extends IntegerType { public size_t() { this(0L); }
      public size_t(long value) { super(value); } }
    
    private static Map typeInfoMap = new WeakHashMap();
    



    private static final int FFI_TYPE_STRUCT = 13;
    


    public size_t size;
    


    public short alignment;
    



    static
    {
      if (Native.POINTER_SIZE == 0)
        throw new Error("Native library not initialized");
      if (FFITypes.ffi_type_void == null)
        throw new Error("FFI types not initialized");
      typeInfoMap.put(Void.TYPE, FFITypes.ffi_type_void);
      typeInfoMap.put(Void.class, FFITypes.ffi_type_void);
      typeInfoMap.put(Float.TYPE, FFITypes.ffi_type_float);
      typeInfoMap.put(Float.class, FFITypes.ffi_type_float);
      typeInfoMap.put(Double.TYPE, FFITypes.ffi_type_double);
      typeInfoMap.put(Double.class, FFITypes.ffi_type_double);
      typeInfoMap.put(Long.TYPE, FFITypes.ffi_type_sint64);
      typeInfoMap.put(Long.class, FFITypes.ffi_type_sint64);
      typeInfoMap.put(Integer.TYPE, FFITypes.ffi_type_sint32);
      typeInfoMap.put(Integer.class, FFITypes.ffi_type_sint32);
      typeInfoMap.put(Short.TYPE, FFITypes.ffi_type_sint16);
      typeInfoMap.put(Short.class, FFITypes.ffi_type_sint16);
      Pointer ctype = Native.WCHAR_SIZE == 2 ? FFITypes.ffi_type_uint16 : FFITypes.ffi_type_uint32;
      
      typeInfoMap.put(Character.TYPE, ctype);
      typeInfoMap.put(Character.class, ctype);
      typeInfoMap.put(Byte.TYPE, FFITypes.ffi_type_sint8);
      typeInfoMap.put(Byte.class, FFITypes.ffi_type_sint8);
      typeInfoMap.put(Pointer.class, FFITypes.ffi_type_pointer);
      typeInfoMap.put(String.class, FFITypes.ffi_type_pointer);
      typeInfoMap.put(WString.class, FFITypes.ffi_type_pointer);
      typeInfoMap.put(Boolean.TYPE, FFITypes.ffi_type_uint32);
      typeInfoMap.put(Boolean.class, FFITypes.ffi_type_uint32);
    }
    




    public short type = 13;
    
    public Pointer elements;
    
    private FFIType(Structure ref)
    {
      ref.ensureAllocated(true);
      Pointer[] els;
      Pointer[] els; int idx; Iterator i; if ((ref instanceof Union)) {
        Structure.StructField sf = biggestField;
        els = new Pointer[] { get(ref.getField(sf), type), null };

      }
      else
      {
        els = new Pointer[ref.fields().size() + 1];
        idx = 0;
        for (i = ref.fields().values().iterator(); i.hasNext();) {
          Structure.StructField sf = (Structure.StructField)i.next();
          els[(idx++)] = ref.getFieldTypeInfo(sf);
        }
      }
      init(els);
    }
    
    private FFIType(Object array, Class type) {
      int length = Array.getLength(array);
      Pointer[] els = new Pointer[length + 1];
      Pointer p = get(null, type.getComponentType());
      for (int i = 0; i < length; i++) {
        els[i] = p;
      }
      init(els);
    }
    
    private void init(Pointer[] els) { elements = new Memory(Pointer.SIZE * els.length);
      elements.write(0L, els, 0, els.length);
      write();
    }
    
    static Pointer get(Object obj) {
      if (obj == null)
        return FFITypes.ffi_type_pointer;
      if ((obj instanceof Class))
        return get(null, (Class)obj);
      return get(obj, obj.getClass());
    }
    
    private static Pointer get(Object obj, Class cls) {
      TypeMapper mapper = Native.getTypeMapper(cls);
      if (mapper != null) {
        ToNativeConverter nc = mapper.getToNativeConverter(cls);
        if (nc != null) {
          cls = nc.nativeType();
        }
      }
      synchronized (typeInfoMap) {
        Object o = typeInfoMap.get(cls);
        if ((o instanceof Pointer)) {
          return (Pointer)o;
        }
        if ((o instanceof FFIType)) {
          return ((FFIType)o).getPointer();
        }
        if (((Platform.HAS_BUFFERS) && (Buffer.class.isAssignableFrom(cls))) || (Callback.class.isAssignableFrom(cls)))
        {
          typeInfoMap.put(cls, FFITypes.ffi_type_pointer);
          return FFITypes.ffi_type_pointer;
        }
        if (Structure.class.isAssignableFrom(cls)) {
          if (obj == null) obj = newInstance(cls);
          if (Structure.ByReference.class.isAssignableFrom(cls)) {
            typeInfoMap.put(cls, FFITypes.ffi_type_pointer);
            return FFITypes.ffi_type_pointer;
          }
          FFIType type = new FFIType((Structure)obj);
          typeInfoMap.put(cls, type);
          return type.getPointer();
        }
        if (NativeMapped.class.isAssignableFrom(cls)) {
          NativeMappedConverter c = NativeMappedConverter.getInstance(cls);
          return get(c.toNative(obj, new ToNativeContext()), c.nativeType());
        }
        if (cls.isArray()) {
          FFIType type = new FFIType(obj, cls);
          
          typeInfoMap.put(obj, type);
          return type.getPointer();
        }
        throw new IllegalArgumentException("Unsupported Structure field type " + cls);
      }
    }
  }
  
  private class AutoAllocated extends Memory {
    public AutoAllocated(int size) {
      super();
      
      super.clear();
    }
  }
  
  private static void structureArrayCheck(Structure[] ss) {
    Pointer base = ss[0].getPointer();
    int size = ss[0].size();
    for (int si = 1; si < ss.length; si++) {
      if (getPointerpeer != peer + size * si) {
        String msg = "Structure array elements must use contiguous memory (bad backing address at Structure array index " + si + ")";
        
        throw new IllegalArgumentException(msg);
      }
    }
  }
  
  public static void autoRead(Structure[] ss) {
    structureArrayCheck(ss);
    if (0array == ss) {
      ss[0].autoRead();
    }
    else {
      for (int si = 0; si < ss.length; si++) {
        ss[si].autoRead();
      }
    }
  }
  
  public void autoRead() {
    if (getAutoRead()) {
      read();
      if (array != null) {
        for (int i = 1; i < array.length; i++) {
          array[i].autoRead();
        }
      }
    }
  }
  
  public static void autoWrite(Structure[] ss) {
    structureArrayCheck(ss);
    if (0array == ss) {
      ss[0].autoWrite();
    }
    else {
      for (int si = 0; si < ss.length; si++) {
        ss[si].autoWrite();
      }
    }
  }
  
  public void autoWrite() {
    if (getAutoWrite()) {
      write();
      if (array != null) {
        for (int i = 1; i < array.length; i++) {
          array[i].autoWrite();
        }
      }
    }
  }
  
  protected int getNativeSize(Class nativeType, Object value) {
    return Native.getNativeSize(nativeType, value);
  }
}
