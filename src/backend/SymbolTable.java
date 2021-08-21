package backend;

import util.StringGenerator;

import java.util.*;
import java.util.regex.Pattern;

public class SymbolTable {

    public Map<String, Symbol> getI32s() {
        return i32s;
    }

    public Map<String, Symbol> getF32s() {
        return f32s;
    }

    public Map<String, Symbol> getU8s() {
        return u8s;
    }

    public Map<String, Symbol> getU8pointers() {
        return u8pointers;
    }

    private final Map<String, Symbol> i32s = new HashMap<>();
    private final Map<String, Symbol> f32s = new HashMap<>();
    private final Map<String, Symbol> u8s = new HashMap<>();
    private final Map<String, Symbol> u8pointers = new HashMap<>();

    private int functionOffsets;
    private int functionParamOffsets;

    public SymbolTableInner getRoot() {
        return root;
    }

    private final SymbolTableInner root;

    public static Type getInferredType(String str) {
        if (Pattern.compile("[1-9][0-9]*|0[0-7]*|0[xX][0-9a-fA-F]+").matcher(str).matches()){
            return Type.i32;
        }
        else if (Pattern.compile("[0-9]*\\.[0-9]*").matcher(str).matches()){
            return Type.f32;
        }
        else if (str.contains("\"")){
            return Type.u8pointer;
        }
        else if (str.contains("'")){
            return Type.u8;
        }
        return null;
    }

    public SymbolTable(){
        root = new SymbolTableInner();
        root.parent = null;
    }

    void moveIn(List<Integer> blockHeight) {
        if (blockHeight.isEmpty()) {
            //说明是函数定义
            functionOffsets = 4;
            functionParamOffsets = -8;
        }
        SymbolTableInner varSymbolTable = root;
        for (Integer i : blockHeight) {
            varSymbolTable = varSymbolTable.children.get(i);
        }
        SymbolTableInner newChild = new SymbolTableInner();
        newChild.parent = varSymbolTable;
        newChild.children = new ArrayList<>();
        newChild.variables = new ArrayList<>();
        blockHeight.add(varSymbolTable.children.size());
        varSymbolTable.children.add(newChild);
    }

    void moveOut(List<Integer> blockHeight) {
        blockHeight.remove(blockHeight.size() - 1);
    }

    public Symbol getVar(List<Integer> blockHeight, String varname) {
        Type specifiedType = getInferredType(varname);
        if (specifiedType != null){
            if (specifiedType.equals(Type.i32)){
                return addInt(varname);
            }
            else if (specifiedType.equals(Type.f32)){
                return addFloat(varname);
            }
            else if (specifiedType.equals(Type.u8pointer)){
                return addString(varname);
            }
            else if (specifiedType.equals(Type.u8)){
                return addChar(varname);
            }
        }
        else {
            SymbolTableInner nowpointer = getInner(blockHeight);
            while (nowpointer != null) {
                for (Symbol s : nowpointer.variables) {
                    if (s.getName().equals(varname)) {
                        return s;
                    }
                }
                nowpointer = nowpointer.parent;
            }
        }
        return null;
    }

    private Symbol addString(String varname) {
        if (u8pointers.containsKey(varname)){
            return u8pointers.get(varname);
        }
        else {
            Symbol symbol = new Symbol(StringGenerator.getNextCode(), Type.u8pointer);
            u8pointers.put(varname, symbol);
            root.variables.add(symbol);
            return symbol;
        }
    }

    private Symbol addChar(String varname) {
        if (u8s.containsKey(varname)){
            return u8s.get(varname);
        }
        else {
            Symbol symbol = new Symbol(StringGenerator.getNextCode(), Type.u8);
            u8s.put(varname, symbol);
            root.variables.add(symbol);
            return symbol;
        }
    }

    private Symbol addFloat(String varname) {
        if (f32s.containsKey(varname)){
            return f32s.get(varname);
        }
        else {
            Symbol symbol = new Symbol(StringGenerator.getNextCode(), Type.f32);
            f32s.put(varname, symbol);
            root.variables.add(symbol);
            return symbol;
        }
    }

    private Symbol addInt(String varname) {
        if (i32s.containsKey(varname)){
            return i32s.get(varname);
        }
        else {
            Symbol symbol = new Symbol(StringGenerator.getNextCode(), Type.i32);
            i32s.put(varname, symbol);
            root.variables.add(symbol);
            return symbol;
        }
    }

    public Symbol addVar(List<Integer> blockHeight, String varname, Type type) {
        SymbolTableInner nowpointer = getInner(blockHeight);
        Symbol symbol = new Symbol(varname, type);
        nowpointer.variables.add(symbol);
        symbol.setOffset(functionOffsets);
        functionOffsets += symbol.getType().getLength();
        return symbol;
    }

    public Symbol addParam(List<Integer> blockHeight, String varname, Type type) {
        SymbolTableInner nowpointer = getInner(blockHeight);
        Symbol symbol = new Symbol(varname, type);
        nowpointer.variables.add(symbol);
        symbol.setOffset(functionParamOffsets);
        functionParamOffsets -= symbol.getType().getLength();
        return symbol;
    }

    public SymbolTableInner getInner(List<Integer> blockHeight) {
        SymbolTableInner nowpointer = root;
        for (Integer x: blockHeight){
            nowpointer = nowpointer.children.get(x);
        }
        return nowpointer;
    }
}
