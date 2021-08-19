package backend;

import exception.PLDLAssemblingException;

import java.util.*;

//类是对象
public class Type {
    public String getTypename() {
        return typename;
    }

    public void setTypename(String typename) {
        this.typename = typename;
    }

    private String typename;
    public static final int MACHINE_LENGTH = 4;
    public static final Map<String, Type> basicTypeAlias = new HashMap<>();
    public static final Set<Type> basicTypes = new HashSet<>();

    private final LinkedHashMap<String, Type> fields;

    private int length;

    public Type(String typename, int length, boolean isVolatile) {
        this.typename = typename;
        this.fields = new LinkedHashMap<>();
        this.length = length;
        this.isVolatile = isVolatile;
    }

    public Type(String typename, Map<String, Type> fields, boolean isVolatile) {
        this.typename = typename;
        this.fields = new LinkedHashMap<>(fields);
        setAllLength();
        this.isVolatile = isVolatile;
    }

    private void setAllLength() {
        int allLength = 0;
        for (String fieldname: fields.keySet()){
            allLength += fields.get(fieldname).length;
        }
        this.length = allLength;
    }

    public int getOffsetLength(String field) throws PLDLAssemblingException {
        int allLength = 0;
        for (String fieldname: fields.keySet()){
            if (field.equals(fieldname)){
                return allLength;
            }
            allLength += fields.get(fieldname).length;
        }
        throw new PLDLAssemblingException("没有这个字段", null);
    }

    public Type(Type type) {
        this.fields = new LinkedHashMap<>(type.fields);
        this.length = type.length;
        this.isVolatile = type.isVolatile;
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    public void setVolatile(boolean aVolatile) {
        isVolatile = aVolatile;
    }

    private boolean isVolatile = false;

    public void addToFields(String name, Type type){
        this.fields.put(name, type);
        this.length += type.length;
    }

    public int getLength() {
        return length;
    }

    public static Type i8;
    public static Type i16;
    public static Type i32;
    public static Type i64;
    public static Type isize;
    public static Type u8;
    public static Type u16;
    public static Type u32;
    public static Type u64;
    public static Type usize;
    public static Type f32;
    public static Type f64;
    public static Type bool1;

    public static PointerType i8pointer;
    public static PointerType i16pointer;
    public static PointerType i32pointer;
    public static PointerType i64pointer;
    public static PointerType isizepointer;
    public static PointerType u8pointer;
    public static PointerType u16pointer;
    public static PointerType u32pointer;
    public static PointerType u64pointer;
    public static PointerType usizepointer;
    public static PointerType f32pointer;
    public static PointerType f64pointer;
    public static PointerType boolpointer;

    public static void initializeBasicTypes() {
        i8 = new Type("i8", 1, false);
        i16 = new Type("i16", 2, false);
        i32 = new Type("i32", 4, false);
        i64 = new Type("i64", 8, false);
        isize = new Type("isize", 4, false);
        u8 = new Type("u8", 1, false);
        u16 = new Type("u16", 2, false);
        u32 = new Type("u32", 4, false);
        u64 = new Type("u64", 8, false);
        usize = new Type("usize", 4, false);
        f32 = new Type("f32", 4, false);
        f64 = new Type("f64", 8, false);
        bool1 = new Type("bool", 1, false);

        i8pointer = new PointerType("i8pointer", i8, false);
        i16pointer = new PointerType("i16pointer", i16, false);
        i32pointer = new PointerType("i32pointer", i32, false);
        i64pointer = new PointerType("i64pointer", i64, false);
        isizepointer = new PointerType("isizepointer", isize, false);
        u8pointer = new PointerType("u8pointer", u8, false);
        u16pointer = new PointerType("u16pointer", u16, false);
        u32pointer = new PointerType("u32pointer", u32, false);
        u64pointer = new PointerType("u64pointer", u64, false);
        usizepointer = new PointerType("usizepointer", usize, false);
        f32pointer = new PointerType("f32pointer", f32, false);
        f64pointer = new PointerType("f64pointer", f64, false);
        boolpointer = new PointerType("boolpointer", bool1, false);

        basicTypeAlias.put("i8", Type.i8);
        basicTypeAlias.put("i16", Type.i16);
        basicTypeAlias.put("i32", Type.i32);
        basicTypeAlias.put("i64", Type.i64);
        basicTypeAlias.put("isize", Type.isize);
        basicTypeAlias.put("u8", Type.u8);
        basicTypeAlias.put("u16", Type.u16);
        basicTypeAlias.put("u32", Type.u32);
        basicTypeAlias.put("u64", Type.u64);
        basicTypeAlias.put("usize", Type.usize);
        basicTypeAlias.put("f32", Type.f32);
        basicTypeAlias.put("f64", Type.f64);
        basicTypeAlias.put("bool", Type.bool1);
        basicTypeAlias.put("i8pointer", Type.i8pointer);
        basicTypeAlias.put("i16pointer", Type.i16pointer);
        basicTypeAlias.put("i32pointer", Type.i32pointer);
        basicTypeAlias.put("i64pointer", Type.i64pointer);
        basicTypeAlias.put("isizepointer", Type.isizepointer);
        basicTypeAlias.put("u8pointer", Type.u8pointer);
        basicTypeAlias.put("u16pointer", Type.u16pointer);
        basicTypeAlias.put("u32pointer", Type.u32pointer);
        basicTypeAlias.put("u64pointer", Type.u64pointer);
        basicTypeAlias.put("usizepointer", Type.usizepointer);
        basicTypeAlias.put("f32pointer", Type.f32pointer);
        basicTypeAlias.put("f64pointer", Type.f64pointer);
        basicTypeAlias.put("boolpointer", Type.boolpointer);
        basicTypes.add(Type.i8);
        basicTypes.add(Type.i16);
        basicTypes.add(Type.i32);
        basicTypes.add(Type.i64);
        basicTypes.add(Type.isize);
        basicTypes.add(Type.u8);
        basicTypes.add(Type.u16);
        basicTypes.add(Type.u32);
        basicTypes.add(Type.u64);
        basicTypes.add(Type.usize);
        basicTypes.add(Type.f32);
        basicTypes.add(Type.f64);
        basicTypes.add(Type.bool1);
        basicTypes.add(Type.i8pointer);
        basicTypes.add(Type.i16pointer);
        basicTypes.add(Type.i32pointer);
        basicTypes.add(Type.i64pointer);
        basicTypes.add(Type.isizepointer);
        basicTypes.add(Type.u8pointer);
        basicTypes.add(Type.u16pointer);
        basicTypes.add(Type.u32pointer);
        basicTypes.add(Type.u64pointer);
        basicTypes.add(Type.usizepointer);
        basicTypes.add(Type.f32pointer);
        basicTypes.add(Type.f64pointer);
        basicTypes.add(Type.boolpointer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type typename = (Type) o;
        return length == typename.length && isVolatile == typename.isVolatile && fields.equals(typename.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, length, isVolatile);
    }

    public Type getField(String fieldname) {
        return fields.get(fieldname);
    }

    public int getFieldSize() {
        return fields.size();
    }

    public List<String> getFieldKeys() {
        List<String> results = new ArrayList<>();
        for (String key: fields.keySet()){
            results.add(key);
        }
        return results;
    }
}

class PointerType extends Type {
    private Type pointerToType;
    private boolean isVar;

    public PointerType(String typename, Type pointerToType, boolean isVar){
        super(typename, 0, false);
        this.pointerToType = pointerToType;
        this.addToFields("value", usize);
        this.isVar = isVar;
    }
}

class RefType extends Type {
    public Type getRefToType() {
        return refToType;
    }

    private Type refToType;
    private boolean isVar;

    public RefType(String typename, Type refToType, boolean isVar){
        super(typename, MACHINE_LENGTH, false);
        this.refToType = refToType;
        this.addToFields("value", usize);
        this.isVar = isVar;
    }
}

class ArrayType extends Type {
    private Type refToType;

    private List<Integer> arrayDimensions;

    public ArrayType(String typename, Type refToType, List<Integer> arrayDimensions){
        super(typename, MACHINE_LENGTH, false);
        this.refToType = refToType;
        this.addToFields("value", usize);
        this.arrayDimensions = arrayDimensions;
    }

    public List<Integer> getArrayDimensions() {
        return arrayDimensions;
    }
}

class FuncType extends Type {
    private final List<Type> paramTypes;

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    private Type returnType;

    public FuncType(String typename){
        super(typename, MACHINE_LENGTH, false);
        this.paramTypes = new ArrayList<>();
        this.addToFields("value", usize);
        this.returnType = null;
    }

    public FuncType(String typename, List<Type> paramTypes, Type returnType){
        super(typename, MACHINE_LENGTH, false);
        this.paramTypes = new ArrayList<>(paramTypes);
        this.returnType = returnType;
        this.addToFields("value", usize);
    }
}
