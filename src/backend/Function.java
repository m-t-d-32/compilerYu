package backend;

import java.util.ArrayList;
import java.util.List;

public class Function {
    public String getFuncname() {
        return funcname;
    }

    public void setFuncname(String funcname) {
        this.funcname = funcname;
    }

    public FuncType getFunctype() {
        return functype;
    }

    public void setFunctype(FuncType functype) {
        this.functype = functype;
    }

    public List<FourTuple> getFourTuples() {
        return fourTuples;
    }

    private String funcname;
    private FuncType functype;

    public List<Symbol> getParams() {
        return params;
    }

    private final List<Symbol> params = new ArrayList<>();
    private final List<FourTuple> fourTuples = new ArrayList<>();

    public List<String> getResults() {
        return results;
    }

    private final List<String> results = new ArrayList<>();
    private int indexOfSubtuple = 0;

    public void setSubTuple(){
        indexOfSubtuple = results.size();
    }

    public int getIndexOfSubtuple(){
        return indexOfSubtuple;
    }

    public void setTrueSub(SymbolTableInner table){
        int all = 0;
        for (Symbol symbol: table.variables){
            all += symbol.getType().getLength();
        }
        results.add(indexOfSubtuple, "sub esp, " + all);
    }
}
