package backend;

import java.util.LinkedHashMap;
import java.util.Map;

enum Visiblity {
    DEFAULT,
    PUBLIC,
    EXTERN,
    INLINE,
}

public class Symbol {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;
    private Visiblity visiblity;
    private Type type;
    private boolean isVar;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    private int offset;

    public Symbol(String varname, Type type){
        this.name = varname;
        this.visiblity = Visiblity.DEFAULT;
        this.type = type;
        this.isVar = false;
    }

    public Symbol(Symbol other){
        this.name = other.name;
        this.visiblity = other.visiblity;
        this.type = other.type;
        this.isVar = other.isVar;
    }

    public Visiblity getVisiblity() {
        return visiblity;
    }

    public void setVisiblity(Visiblity visiblity) {
        this.visiblity = visiblity;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isVar() {
        return isVar;
    }

    public void setVar(boolean var) {
        isVar = var;
    }

    public boolean isBasicSymbol(){
        return false;
    }
}
