package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

































@Beta
public final class TypeResolver
{
  private final TypeTable typeTable;
  
  public TypeResolver()
  {
    typeTable = new TypeTable();
  }
  
  private TypeResolver(TypeTable typeTable) {
    this.typeTable = typeTable;
  }
  
  static TypeResolver accordingTo(Type type) {
    return new TypeResolver().where(TypeMappingIntrospector.getTypeMappings(type));
  }
  


















  public TypeResolver where(Type formal, Type actual)
  {
    Map<TypeVariableKey, Type> mappings = Maps.newHashMap();
    populateTypeMappings(mappings, (Type)Preconditions.checkNotNull(formal), (Type)Preconditions.checkNotNull(actual));
    return where(mappings);
  }
  
  TypeResolver where(Map<TypeVariableKey, ? extends Type> mappings)
  {
    return new TypeResolver(typeTable.where(mappings));
  }
  
  private static void populateTypeMappings(Map<TypeVariableKey, Type> mappings, Type from, final Type to)
  {
    if (from.equals(to)) {
      return;
    }
    new TypeVisitor()
    {
      void visitTypeVariable(TypeVariable<?> typeVariable) { val$mappings.put(new TypeResolver.TypeVariableKey(typeVariable), to); }
      
      void visitWildcardType(WildcardType fromWildcardType) {
        WildcardType toWildcardType = (WildcardType)TypeResolver.expectArgument(WildcardType.class, to);
        Type[] fromUpperBounds = fromWildcardType.getUpperBounds();
        Type[] toUpperBounds = toWildcardType.getUpperBounds();
        Type[] fromLowerBounds = fromWildcardType.getLowerBounds();
        Type[] toLowerBounds = toWildcardType.getLowerBounds();
        Preconditions.checkArgument((fromUpperBounds.length == toUpperBounds.length) && (fromLowerBounds.length == toLowerBounds.length), "Incompatible type: %s vs. %s", new Object[] { fromWildcardType, to });
        


        for (int i = 0; i < fromUpperBounds.length; i++) {
          TypeResolver.populateTypeMappings(val$mappings, fromUpperBounds[i], toUpperBounds[i]);
        }
        for (int i = 0; i < fromLowerBounds.length; i++)
          TypeResolver.populateTypeMappings(val$mappings, fromLowerBounds[i], toLowerBounds[i]);
      }
      
      void visitParameterizedType(ParameterizedType fromParameterizedType) {
        ParameterizedType toParameterizedType = (ParameterizedType)TypeResolver.expectArgument(ParameterizedType.class, to);
        Preconditions.checkArgument(fromParameterizedType.getRawType().equals(toParameterizedType.getRawType()), "Inconsistent raw type: %s vs. %s", new Object[] { fromParameterizedType, to });
        
        Type[] fromArgs = fromParameterizedType.getActualTypeArguments();
        Type[] toArgs = toParameterizedType.getActualTypeArguments();
        Preconditions.checkArgument(fromArgs.length == toArgs.length, "%s not compatible with %s", new Object[] { fromParameterizedType, toParameterizedType });
        
        for (int i = 0; i < fromArgs.length; i++)
          TypeResolver.populateTypeMappings(val$mappings, fromArgs[i], toArgs[i]);
      }
      
      void visitGenericArrayType(GenericArrayType fromArrayType) {
        Type componentType = Types.getComponentType(to);
        Preconditions.checkArgument(componentType != null, "%s is not an array type.", new Object[] { to });
        TypeResolver.populateTypeMappings(val$mappings, fromArrayType.getGenericComponentType(), componentType);
      }
      



      void visitClass(Class<?> fromClass) { throw new IllegalArgumentException("No type mapping from " + fromClass); } }.visit(new Type[] { from });
  }
  





  public Type resolveType(Type type)
  {
    Preconditions.checkNotNull(type);
    if ((type instanceof TypeVariable))
      return typeTable.resolve((TypeVariable)type);
    if ((type instanceof ParameterizedType))
      return resolveParameterizedType((ParameterizedType)type);
    if ((type instanceof GenericArrayType))
      return resolveGenericArrayType((GenericArrayType)type);
    if ((type instanceof WildcardType)) {
      return resolveWildcardType((WildcardType)type);
    }
    
    return type;
  }
  
  private Type[] resolveTypes(Type[] types)
  {
    Type[] result = new Type[types.length];
    for (int i = 0; i < types.length; i++) {
      result[i] = resolveType(types[i]);
    }
    return result;
  }
  
  private WildcardType resolveWildcardType(WildcardType type) {
    Type[] lowerBounds = type.getLowerBounds();
    Type[] upperBounds = type.getUpperBounds();
    return new Types.WildcardTypeImpl(resolveTypes(lowerBounds), resolveTypes(upperBounds));
  }
  
  private Type resolveGenericArrayType(GenericArrayType type)
  {
    Type componentType = type.getGenericComponentType();
    Type resolvedComponentType = resolveType(componentType);
    return Types.newArrayType(resolvedComponentType);
  }
  
  private ParameterizedType resolveParameterizedType(ParameterizedType type) {
    Type owner = type.getOwnerType();
    Type resolvedOwner = owner == null ? null : resolveType(owner);
    Type resolvedRawType = resolveType(type.getRawType());
    
    Type[] args = type.getActualTypeArguments();
    Type[] resolvedArgs = resolveTypes(args);
    return Types.newParameterizedTypeWithOwner(resolvedOwner, (Class)resolvedRawType, resolvedArgs);
  }
  
  private static <T> T expectArgument(Class<T> type, Object arg)
  {
    try {
      return type.cast(arg);
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(arg + " is not a " + type.getSimpleName());
    }
  }
  
  private static class TypeTable
  {
    private final ImmutableMap<TypeResolver.TypeVariableKey, Type> map;
    
    TypeTable() {
      map = ImmutableMap.of();
    }
    
    private TypeTable(ImmutableMap<TypeResolver.TypeVariableKey, Type> map) {
      this.map = map;
    }
    
    final TypeTable where(Map<TypeResolver.TypeVariableKey, ? extends Type> mappings)
    {
      ImmutableMap.Builder<TypeResolver.TypeVariableKey, Type> builder = ImmutableMap.builder();
      builder.putAll(map);
      for (Map.Entry<TypeResolver.TypeVariableKey, ? extends Type> mapping : mappings.entrySet()) {
        TypeResolver.TypeVariableKey variable = (TypeResolver.TypeVariableKey)mapping.getKey();
        Type type = (Type)mapping.getValue();
        Preconditions.checkArgument(!variable.equalsType(type), "Type variable %s bound to itself", new Object[] { variable });
        builder.put(variable, type);
      }
      return new TypeTable(builder.build());
    }
    
    final Type resolve(final TypeVariable<?> var) {
      final TypeTable unguarded = this;
      TypeTable guarded = new TypeTable()
      {
        public Type resolveInternal(TypeVariable<?> intermediateVar, TypeResolver.TypeTable forDependent) {
          if (intermediateVar.getGenericDeclaration().equals(var.getGenericDeclaration())) {
            return intermediateVar;
          }
          return unguarded.resolveInternal(intermediateVar, forDependent);
        }
      };
      return resolveInternal(var, guarded);
    }
    







    Type resolveInternal(TypeVariable<?> var, TypeTable forDependants)
    {
      Type type = (Type)map.get(new TypeResolver.TypeVariableKey(var));
      if (type == null) {
        Type[] bounds = var.getBounds();
        if (bounds.length == 0) {
          return var;
        }
        Type[] resolvedBounds = new TypeResolver(forDependants, null).resolveTypes(bounds);
        



























        if ((Types.NativeTypeVariableEquals.NATIVE_TYPE_VARIABLE_ONLY) && (Arrays.equals(bounds, resolvedBounds)))
        {
          return var;
        }
        return Types.newArtificialTypeVariable(var.getGenericDeclaration(), var.getName(), resolvedBounds);
      }
      

      return new TypeResolver(forDependants, null).resolveType(type);
    }
  }
  
  private static final class TypeMappingIntrospector extends TypeVisitor
  {
    private static final TypeResolver.WildcardCapturer wildcardCapturer = new TypeResolver.WildcardCapturer(null);
    
    private final Map<TypeResolver.TypeVariableKey, Type> mappings = Maps.newHashMap();
    

    private TypeMappingIntrospector() {}
    

    static ImmutableMap<TypeResolver.TypeVariableKey, Type> getTypeMappings(Type contextType)
    {
      TypeMappingIntrospector introspector = new TypeMappingIntrospector();
      introspector.visit(new Type[] { wildcardCapturer.capture(contextType) });
      return ImmutableMap.copyOf(mappings);
    }
    
    void visitClass(Class<?> clazz) {
      visit(new Type[] { clazz.getGenericSuperclass() });
      visit(clazz.getGenericInterfaces());
    }
    
    void visitParameterizedType(ParameterizedType parameterizedType) {
      Class<?> rawClass = (Class)parameterizedType.getRawType();
      TypeVariable<?>[] vars = rawClass.getTypeParameters();
      Type[] typeArgs = parameterizedType.getActualTypeArguments();
      Preconditions.checkState(vars.length == typeArgs.length);
      for (int i = 0; i < vars.length; i++) {
        map(new TypeResolver.TypeVariableKey(vars[i]), typeArgs[i]);
      }
      visit(new Type[] { rawClass });
      visit(new Type[] { parameterizedType.getOwnerType() });
    }
    
    void visitTypeVariable(TypeVariable<?> t) {
      visit(t.getBounds());
    }
    
    void visitWildcardType(WildcardType t) {
      visit(t.getUpperBounds());
    }
    
    private void map(TypeResolver.TypeVariableKey var, Type arg) {
      if (mappings.containsKey(var))
      {




        return;
      }
      
      for (Type t = arg; t != null; t = (Type)mappings.get(TypeResolver.TypeVariableKey.forLookup(t))) {
        if (var.equalsType(t))
        {



          for (Type x = arg; x != null; x = (Type)mappings.remove(TypeResolver.TypeVariableKey.forLookup(x))) {}
          return;
        }
      }
      mappings.put(var, arg);
    }
  }
  



  private static final class WildcardCapturer
  {
    private WildcardCapturer() {}
    


    private final AtomicInteger id = new AtomicInteger();
    
    Type capture(Type type) {
      Preconditions.checkNotNull(type);
      if ((type instanceof Class)) {
        return type;
      }
      if ((type instanceof TypeVariable)) {
        return type;
      }
      if ((type instanceof GenericArrayType)) {
        GenericArrayType arrayType = (GenericArrayType)type;
        return Types.newArrayType(capture(arrayType.getGenericComponentType()));
      }
      if ((type instanceof ParameterizedType)) {
        ParameterizedType parameterizedType = (ParameterizedType)type;
        return Types.newParameterizedTypeWithOwner(captureNullable(parameterizedType.getOwnerType()), (Class)parameterizedType.getRawType(), capture(parameterizedType.getActualTypeArguments()));
      }
      


      if ((type instanceof WildcardType)) {
        WildcardType wildcardType = (WildcardType)type;
        Type[] lowerBounds = wildcardType.getLowerBounds();
        if (lowerBounds.length == 0) {
          Type[] upperBounds = wildcardType.getUpperBounds();
          String name = "capture#" + id.incrementAndGet() + "-of ? extends " + Joiner.on('&').join(upperBounds);
          
          return Types.newArtificialTypeVariable(WildcardCapturer.class, name, wildcardType.getUpperBounds());
        }
        

        return type;
      }
      
      throw new AssertionError("must have been one of the known types");
    }
    
    private Type captureNullable(@Nullable Type type) {
      if (type == null) {
        return null;
      }
      return capture(type);
    }
    
    private Type[] capture(Type[] types) {
      Type[] result = new Type[types.length];
      for (int i = 0; i < types.length; i++) {
        result[i] = capture(types[i]);
      }
      return result;
    }
  }
  






  static final class TypeVariableKey
  {
    private final TypeVariable<?> var;
    






    TypeVariableKey(TypeVariable<?> var)
    {
      this.var = ((TypeVariable)Preconditions.checkNotNull(var));
    }
    
    public int hashCode() {
      return Objects.hashCode(new Object[] { var.getGenericDeclaration(), var.getName() });
    }
    
    public boolean equals(Object obj) {
      if ((obj instanceof TypeVariableKey)) {
        TypeVariableKey that = (TypeVariableKey)obj;
        return equalsTypeVariable(var);
      }
      return false;
    }
    
    public String toString()
    {
      return var.toString();
    }
    
    static Object forLookup(Type t)
    {
      if ((t instanceof TypeVariable)) {
        return new TypeVariableKey((TypeVariable)t);
      }
      return null;
    }
    




    boolean equalsType(Type type)
    {
      if ((type instanceof TypeVariable)) {
        return equalsTypeVariable((TypeVariable)type);
      }
      return false;
    }
    
    private boolean equalsTypeVariable(TypeVariable<?> that)
    {
      return (var.getGenericDeclaration().equals(that.getGenericDeclaration())) && (var.getName().equals(that.getName()));
    }
  }
}
