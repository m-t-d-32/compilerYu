package backend;

import java.util.ArrayList;
import java.util.List;

class SymbolTableInner {
    List<SymbolTableInner> children = new ArrayList<>();
    List<Symbol> variables = new ArrayList<>();
    SymbolTableInner parent;
}
