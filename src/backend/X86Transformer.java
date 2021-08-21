package backend;

import exception.PLDLAssemblingException;
import util.StringGenerator;

import java.util.*;

public class X86Transformer {
    private static final String NoNamedStr = "NULL";
    private static final String NoNamedFourTuple = NoNamedStr + ", 0, 0, 0";
    private static final String localbytes = "LOCALBYTES";
    private static final String VarStr = "var";

    private static final Map<String, Integer> priorityOfOperators = new HashMap<String, Integer>(){{
        put(NoNamedStr, -1);
        put("assign", 0);
        put("lt", 1);
        put("gt", 1);
        put("add", 2);
        put("sub", 2);
    }};

    private static final Map<String, List<String>> operatorSpecifiedAssembly = new HashMap<String, List<String>>(){{
        put(NoNamedStr, new ArrayList<>());
        put("add", new ArrayList<String>(){{
            add("add eax, ebx");
        }});
        put("sub", new ArrayList<String>(){{
            add("sub eax, ebx");
        }});
        put("lt", new ArrayList<String>(){{
            add("cmp eax, ebx");
            add("mov eax, 0");
            add("setl al");
        }});
        put("gt", new ArrayList<String>(){{
            add("cmp eax, ebx");
            add("mov eax, 0");
            add("setg al");
        }});
    }};

    private final List<FourTuple> inputs;
    private final List<String> results = new ArrayList<>();
    private final Map<String, Type> typeAlias;
    private final SymbolTable varTable = new SymbolTable();
    private final Stack<Type> nowFuncParamType = new Stack<>();
    private final Map<String, FuncType> declaredFunctions = new HashMap<>();
    private final List<Symbol> initJoinVars = new ArrayList<>();
    private final List<Integer> nowBlockHeight = new ArrayList<>();
    private final Map<String, Function> definedFunctions = new HashMap<>();
    private final List<Symbol> nowFunctionParams = new ArrayList<>();
    private final Map<String, Type> nowStructArgs = new LinkedHashMap<>();
    private Function nowFunction = null;
    private int callingFuncParamIndex = 0;

    private String nowOperatorResult = null;
    private String nowTrueOperatorResult = null;
    private Stack<String> nowSymbolStack = new Stack<>();
    private Stack<String> nowOperatorStack = new Stack<>();

    public X86Transformer(List<FourTuple> inputs){
        Type.initializeBasicTypes();
        this.inputs = inputs;
        typeAlias = new HashMap<>(Type.basicTypeAlias);
    }

    public String parse() throws PLDLAssemblingException {
        for (FourTuple tuple: inputs){
            writeString("; " + tuple.toString());
            if (nowFunction != null){
                nowFunction.getFourTuples().add(tuple);
            }
            if (!priorityOfOperators.containsKey(tuple.getTuples()[0])){
                doOperator(new FourTuple(NoNamedFourTuple));
            }
            switch(tuple.getTuples()[0]){
                case "import": /* Do nothing */ break;
                case "importjoin": /* Do nothing */ break;
                case "typedef": TransformTypedef(tuple); break;
                case "type_func": TransformTypeFunc(tuple); break;
                case "structarg":  TransformStructArg(tuple); break;
                case "struct": TransformStruct(tuple); break;
                case "constdefine": TransformConstDefine(tuple); break;
                case "constinit": TransformConstInit(tuple); break;
                case "type_pointer": TransformTypePointer(tuple); break;
                case "type_funcparam": TransformTypeFuncParam(tuple); break;
                case "declare": TransformDeclare(tuple); break;
                case "valinitjoin": TransformValInitJoin(tuple); break;
                case "valinit": TransformValInit(tuple); break;
                case "func": TransformFunc(tuple); break;
                case "type_reference": TransformTypeRefernce(tuple); break;
                case "param": TransformParam(tuple); break;
                case "endparam": TransformEndParam(tuple); break;
                case "in": TransformIn(tuple); break;
                case "out": TransformOut(tuple); break;
                case "access": TransformAccess(tuple); break;
                case "addr": TransformAddr(tuple); break;
                case "typecast": TranformTypeCast(tuple); break;
                case "pushvar": TransformPushVar(tuple); break;
                case "call": TransformCall(tuple); break;
                case "funcend": TransformFuncEnd(tuple); break;
                case "cmp": TransformCmp(tuple); break;
                case "label": TransformLabel(tuple); break;
                case "b": TransformB(tuple); break;
                case "beq": TransformBeq(tuple); break;
                case "define": TransformDefine(tuple); break;
                case "init": TransformInit(tuple); break;
                case "blockassign": TransformBlockAssign(tuple); break;

                case "assign":
                case "add":
                case "sub":
                case "lt":
                case "gt":
                    TransformOperators(tuple); break;
                //TODO: 剩下的还没有实现
                default: throw new PLDLAssemblingException(tuple.toString(), null);
            }
        }

        List<String> allresults = new ArrayList<>();
        for (String funcname: declaredFunctions.keySet()){
            allresults.add("extern " + funcname);
        }
        Set<Symbol> dataSymbols = new HashSet<>();
        allresults.add("section .data");
        for (String varname: varTable.getI32s().keySet()){
            dataSymbols.add(varTable.getI32s().get(varname));
            allresults.add(varTable.getI32s().get(varname).getName() + " dd " + varname);
        }
        for (String varname: varTable.getF32s().keySet()){
            dataSymbols.add(varTable.getF32s().get(varname));
            allresults.add(varTable.getF32s().get(varname).getName() + " dd " + varname);
        }
        for (String varname: varTable.getU8s().keySet()){
            dataSymbols.add(varTable.getU8s().get(varname));
            allresults.add(varTable.getU8s().get(varname).getName() + " db " + varname);
        }
        for (String varname: varTable.getU8pointers().keySet()){
            dataSymbols.add(varTable.getU8pointers().get(varname));
            String newVarname = StringGenerator.getNextCode();
            allresults.add(newVarname + " db " + varname + ", 13, 10, 0");
            allresults.add(varTable.getU8pointers().get(varname).getName() + " dd " + newVarname);
        }
        allresults.add("section .bss");
        varTable.getRoot().variables.removeAll(dataSymbols);
        for (Symbol symbol: varTable.getRoot().variables){
            allresults.add(symbol.getName() + ": resb " + symbol.getType().getLength());
        }
        allresults.add("section .text");
        for (String funcname: definedFunctions.keySet()){
            if (!funcname.equals("main")) {
                allresults.add("global " + funcname);
                allresults.add(funcname + ":");
                allresults.addAll(definedFunctions.get(funcname).getResults());
            }
            else {
                allresults.add("global main");
                allresults.add("main:");
                allresults.addAll(results);
                allresults.addAll(definedFunctions.get(funcname).getResults());
            }
        }
        return String.join("\n", allresults);
    }

    private void TransformBlockAssign(FourTuple tuple) {
        String var1name = tuple.getTuples()[1];
        Symbol var1 = varTable.getVar(nowBlockHeight, var1name);
        String var3name = tuple.getTuples()[3];
        Symbol var3 = varTable.addVar(nowBlockHeight, var3name, var1.getType());
        readAddrToRegister(var1, "eax");
        writeString("mov eax, [eax]");
        readAddrToRegister(var3, "ebx");
        writeRegisterToAddr("eax", "ebx");
    }

    private void TransformOperators(FourTuple tuple) throws PLDLAssemblingException {
        String src1 = tuple.getTuples()[1];
        if (nowOperatorResult == null){
            //如果现在没有操作数，说明第一个也是一个操作数
            nowSymbolStack.add(src1);
        }
        else if (!nowOperatorResult.equals(src1)){
            doOperator(tuple);
        }
        doOperator(tuple);
    }

    private void doOperator(FourTuple tuple) {
        while (!nowOperatorStack.isEmpty()){
            String lastOperator = nowOperatorStack.peek();
            String nowOperator = tuple.getTuples()[0];
            if (priorityOfOperators.get(lastOperator) < priorityOfOperators.get(nowOperator)){
                break;
            }

            lastOperator = nowOperatorStack.pop();
            if (lastOperator.equals("assign")) {
                TransformAssign(tuple);
            } else {
                String var2name = nowSymbolStack.pop();
                String var1name = nowSymbolStack.pop();
                Symbol var1 = varTable.getVar(nowBlockHeight, var1name);
                Symbol var2 = varTable.getVar(nowBlockHeight, var2name);
                readAddrToRegister(var1, "eax");
                writeString("mov eax, [eax]");
                readAddrToRegister(var2, "ebx");
                writeString("mov ebx, [ebx]");
                writeString(operatorSpecifiedAssembly.get(lastOperator));
                String var3name = StringGenerator.getNextCode();
                nowTrueOperatorResult = var3name;
                Symbol var3 = varTable.addVar(nowBlockHeight, var3name, Type.bool1);
                readAddrToRegister(var3, "ebx");
                writeRegisterToAddr("eax", "ebx");
                nowSymbolStack.add(var3name);
            }
        }
        if (!tuple.getTuples()[0].equals(NoNamedStr)) {
            nowOperatorStack.add(tuple.getTuples()[0]);
            nowSymbolStack.add(tuple.getTuples()[2]);
            nowOperatorResult = tuple.getTuples()[3];
        }
        else {
            if (nowOperatorResult != null && nowTrueOperatorResult != null) {
                Symbol nowLast = varTable.getVar(nowBlockHeight, nowTrueOperatorResult);
                nowLast.setName(nowOperatorResult);
            }
            nowOperatorStack.clear();
            nowSymbolStack.clear();
            nowOperatorResult = null;
            nowTrueOperatorResult = null;
        }
    }

    private void TransformAssign(FourTuple tuple) {
        String var2name = nowSymbolStack.pop();
        String var1name = nowSymbolStack.pop();
        Symbol var2 = varTable.getVar(nowBlockHeight, var2name);
        readAddrToRegister(var2, "eax");
        writeString("mov eax, [eax]");
        String var3name = StringGenerator.getNextCode();
        nowTrueOperatorResult = var3name;
        Symbol var1 = varTable.getVar(nowBlockHeight, var1name);
        Symbol var3 = varTable.addVar(nowBlockHeight, var3name, var2.getType());
        readAddrToRegister(var1, "ebx");
        writeRegisterToAddr("eax", "ebx");
        readAddrToRegister(var3, "ebx");
        writeRegisterToAddr("eax", "ebx");
        nowSymbolStack.add(var3name);
    }

    private void TransformInit(FourTuple tuple) {
        TransformConstInit(tuple);
    }

    private void TransformDefine(FourTuple tuple) {
        TransformConstDefine(tuple);
    }

    private void TransformBeq(FourTuple tuple) {
        String labelname = tuple.getTuples()[3];
        writeString("jz " + labelname);
    }

    private void TransformB(FourTuple tuple) {
        String labelname = tuple.getTuples()[3];
        writeString("jmp " + labelname);
    }

    private void TransformCmp(FourTuple tuple) {
        String var1name = tuple.getTuples()[1];
        String var2name = tuple.getTuples()[2];
        Symbol var1 = varTable.getVar(nowBlockHeight, var1name);
        Symbol var2 = varTable.getVar(nowBlockHeight, var2name);
        readAddrToRegister(var1, "eax");
        writeString("mov eax, [eax]");
        readAddrToRegister(var2, "ebx");
        writeString("mov ebx, [ebx]");
        writeString("cmp eax, ebx");
    }

    private void TransformLabel(FourTuple tuple) {
        String labelname = tuple.getTuples()[3];
        writeString(labelname + ":");
    }

    private void TransformFuncEnd(FourTuple tuple) {
        writeString("mov esp, ebp");
        writeString("pop ebp");
        writeString("ret");
        nowFunction.setTrueSub(varTable.getInner(nowBlockHeight));
        nowFunction = null;
    }

    private void TransformCall(FourTuple tuple) throws PLDLAssemblingException {
        String funcname = tuple.getTuples()[1];
        String returnvarname = tuple.getTuples()[3];
        int paramsize = 0;
        if (declaredFunctions.containsKey(funcname)){
            writeString("call " + funcname);
            FuncType func = declaredFunctions.get(funcname);
            if (func.getReturnType() != null) {
                //一般从eax返回
                Symbol returnvar = varTable.addVar(nowBlockHeight, returnvarname, func.getReturnType());
                readAddrToRegister(returnvar, "ebx");
                writeRegisterToAddr("eax", "ebx");
            }
            for (Type type: func.getParamTypes()){
                paramsize += type.getLength();
            }
            writeString("add esp, " + paramsize);
        }
        else if (definedFunctions.containsKey(funcname)){
            writeString("call " + funcname);
            FuncType func = (FuncType) declaredFunctions.get(funcname).getReturnType();
            if (func.getReturnType() != null) {
                //约定从[eax]返回
                Symbol returnvar = varTable.addVar(nowBlockHeight, returnvarname, func.getReturnType());
                getRegisterToVar("eax", returnvar, 0, returnvar.getType().getLength());
            }
            for (Type type: func.getParamTypes()){
                paramsize += type.getLength();
            }
            writeString("add esp, " + paramsize);
        }
        else {
            throw new PLDLAssemblingException("函数没有定义", null);
        }
        callingFuncParamIndex = 0;
    }

    private void getRegisterToVar(String register, Symbol dest, int destoffset, int length) {
        readAddrToRegister(dest, "ebx");
        writeString("add ebx, " + destoffset);
        for (int i = 0; i < length; i += 4){
            writeString("mov ecx, [eax + " + i + "]");
            writeString("mov [ebx + " + i + "], ecx");
        }
    }

    private void TransformPushVar(FourTuple tuple) throws PLDLAssemblingException {
        String varname = tuple.getTuples()[1];
        String willcallfuncname = tuple.getTuples()[3];
        Symbol symbol = varTable.getVar(nowBlockHeight, varname);
        FuncType willcallfunc;
        if (definedFunctions.containsKey(willcallfuncname)){
            willcallfunc = definedFunctions.get(willcallfuncname).getFunctype();
        }
        else if (declaredFunctions.containsKey(willcallfuncname)){
            willcallfunc = declaredFunctions.get(willcallfuncname);
        }
        else {
            throw new PLDLAssemblingException("函数没有定义：" + willcallfuncname, null);
        }

        int nowIndex = willcallfunc.getParamTypes().size() - callingFuncParamIndex - 1;
        if (nowIndex < 0){
            throw new PLDLAssemblingException("参数个数不正确", null);
        }
        Type willType = willcallfunc.getParamTypes().get(nowIndex);
        Type nowType = symbol.getType();
        if (willType.getClass().equals(RefType.class) && willType.getField("value").equals(nowType)){
            pushAddress(symbol);
        }
        else if (willType.equals(nowType)){
            pushAll(symbol);
        }
        else {
            throw new PLDLAssemblingException("参数类型不正确", null);
        }
        callingFuncParamIndex++;
    }

    private void pushAll(Symbol symbol) {
        readAddrToRegister(symbol, "eax");
        for (int i = 0; i < symbol.getType().getLength(); i += 4){
            writeString("mov ebx, [eax + " + i + "]");
            writeString("push ebx");
        }
    }

    private void pushAddress(Symbol symbol) {
        readAddrToRegister(symbol, "eax");
        writeString("push eax");
    }

    private void TranformTypeCast(FourTuple tuple) throws PLDLAssemblingException {
        Symbol symbol1 = varTable.getVar(nowBlockHeight, tuple.getTuples()[1]);
        Type oldType = symbol1.getType();
        Type newType = typeAlias.get(tuple.getTuples()[2]);
        Symbol symbol2 = varTable.addVar(nowBlockHeight, tuple.getTuples()[3], newType);
        getRefAddrToVar(symbol1, null, symbol2, null);
    }

    private void TransformAddr(FourTuple tuple) throws PLDLAssemblingException {
        String varname = tuple.getTuples()[1];
        String objname = tuple.getTuples()[3];
        Symbol var = varTable.getVar(nowBlockHeight, varname);
        Type varType = var.getType();
        Type newType = new PointerType(varType.getTypename() + "pointer", varType, false);
        Symbol newvar = varTable.addVar(nowBlockHeight, objname, newType);
        getAddrToVar(var, null, newvar, null);
    }

    private void TransformAccess(FourTuple tuple) throws PLDLAssemblingException {
        String varname = tuple.getTuples()[1];
        String fieldname = tuple.getTuples()[2];
        String objname = tuple.getTuples()[3];
        Symbol var = varTable.getVar(nowBlockHeight, varname);
        Type varType = var.getType();
        while (varType.getClass().equals(RefType.class)){
            varType = ((RefType)(varType)).getRefToType();
        }
        varType = varType.getField(fieldname);
        Symbol newvar = varTable.addVar(nowBlockHeight, objname, varType);
        getRefAddrToVar(var, fieldname, newvar, null);
    }

    private void getRefAddrToVar(Symbol var, String fieldname, Symbol newvar, String newfieldname) throws PLDLAssemblingException {
        Type varType = var.getType();
        readAddrToRegister(var, "eax");
        while (varType.getClass().equals(RefType.class)){
            writeString("mov eax, [eax]");
            varType = ((RefType)(varType)).getRefToType();
        }
        int offset = 0;
        if (fieldname != null) {
            offset = varType.getOffsetLength(fieldname);
        }
        writeString("add eax, " + offset);
        readAddrToRegister(newvar, "ebx");
        Type newvarType = var.getType();
        offset = 0;
        if (newfieldname != null) {
            offset = newvarType.getOffsetLength(newfieldname);
        }
        writeString("add ebx, " + offset);
        writeRegisterToAddr("eax", "ebx");
    }

    private void getAddrToVar(Symbol var, String fieldname, Symbol newvar, String newfieldname) throws PLDLAssemblingException {
        readAddrToRegister(var, "eax");
        Type varType = var.getType();
        int offset = 0;
        if (fieldname != null) {
            offset = varType.getOffsetLength(fieldname);
        }
        writeString("add eax, " + offset);
        readAddrToRegister(newvar, "ebx");
        Type newvarType = var.getType();
        offset = 0;
        if (newfieldname != null) {
            offset = newvarType.getOffsetLength(newfieldname);
        }
        writeString("add ebx, " + offset);
        writeRegisterToAddr("eax", "ebx");
    }

    private void writeRegisterToAddr(String reg1, String reg2) {
        writeString("mov [" + reg2 + "], " + reg1);
    }

    private void readAddrToRegister(Symbol var, String register) {
        if (varTable.getRoot().variables.contains(var)){
            //全局变量
            writeString("mov " + register + ", " + var.getName());
        }
        else {
            //局部变量
            writeString("lea " + register + ", [ebp-" + var.getOffset() + "]");
        }
    }

    private void writeString(String s) {
        if (nowFunction == null){
            results.add(s);
        }
        else {
            nowFunction.getResults().add(s);
        }
    }

    private void writeString(List<String> s){
        if (nowFunction == null){
            results.addAll(s);
        }
        else {
            nowFunction.getResults().addAll(s);
        }
    }

    private void TransformOut(FourTuple tuple) {
        varTable.moveOut(nowBlockHeight);
    }

    private void TransformIn(FourTuple tuple) {
        varTable.moveIn(nowBlockHeight);
    }

    private void TransformEndParam(FourTuple tuple) {
        nowFunction = definedFunctions.get(tuple.getTuples()[3]);
        nowFunction.getParams().addAll(nowFunctionParams);
        for (Symbol s: nowFunctionParams){
            nowFunction.getFunctype().getParamTypes().add(s.getType());
        }
        nowFunctionParams.clear();
        writeString("push ebp");
        writeString("mov ebp, esp");
        nowFunction.setSubTuple();
    }

    private void TransformParam(FourTuple tuple) {
        String varname = tuple.getTuples()[3];
        String typename = tuple.getTuples()[1];
        Type type = typeAlias.get(typename);
        Symbol symbol = varTable.addParam(nowBlockHeight, varname, type);
        nowFunctionParams.add(symbol);
    }

    private void TransformTypeRefernce(FourTuple tuple) {
        Type type1 = typeAlias.get(tuple.getTuples()[1]);
        boolean isVar = tuple.getTuples()[2].equals(VarStr);
        RefType newType = new RefType(type1.getTypename() + "ref", type1, isVar);
        typeAlias.put(tuple.getTuples()[3], newType);
    }

    private void TransformFunc(FourTuple tuple) {
        String property = tuple.getTuples()[1];
        String funcname = tuple.getTuples()[2];
        String returntypename = tuple.getTuples()[3];

        FuncType funcType = new FuncType(funcname);
        if (returntypename.equals(NoNamedStr)) {
            funcType.setReturnType(null);
        }
        else {
            funcType.setReturnType(typeAlias.get(returntypename));
        }
        typeAlias.put(funcname, funcType);

        Function function = new Function();
        function.setFuncname(funcname);
        function.setFunctype(funcType);
        definedFunctions.put(funcname, function);
    }

    private void TransformTypeFunc(FourTuple tuple) {
        String typename = tuple.getTuples()[3];
        String returntypename = tuple.getTuples()[1];
        Type returntype = typeAlias.get(returntypename);
        FuncType funcType = new FuncType(null);
        funcType.setReturnType(returntype);
        while (!nowFuncParamType.empty()){
            funcType.getParamTypes().add(nowFuncParamType.pop());
        }
        typeAlias.put(typename, funcType);
        nowFuncParamType.clear();
    }

    private void TransformValInitJoin(FourTuple tuple) {
        String varname = tuple.getTuples()[3];
        Symbol var = varTable.getVar(nowBlockHeight, varname);
        initJoinVars.add(var);
    }

    private void TransformValInit(FourTuple tuple) throws PLDLAssemblingException {
        String typename = tuple.getTuples()[2];
        Type allType = typeAlias.get(typename);
        if (initJoinVars.size() != allType.getFieldSize()){
            throw new PLDLAssemblingException("初始化列表不正确", null);
        }
        String varname = tuple.getTuples()[3];
        Symbol var = varTable.addVar(nowBlockHeight, varname, allType);
        int counter = 0;
        for (String fieldname: allType.getFieldKeys()){
            Type fieldtype = allType.getField(fieldname);
            Symbol nowfield = initJoinVars.get(counter);
            ++counter;

            if (!nowfield.getType().equals(fieldtype)){
                throw new PLDLAssemblingException("初始化列表不正确", null);
            }

            getVarToVar(nowfield, 0, var, var.getType().getOffsetLength(fieldname), var.getType().getField(fieldname).getLength());
        }
        initJoinVars.clear();
    }

    private void getVarToVar(Symbol src, int srcoffset, Symbol dest, int destoffset, int length) {
        readAddrToRegister(src, "eax");
        writeString("add eax, " + srcoffset);
        readAddrToRegister(dest, "ebx");
        writeString("add ebx, " + destoffset);
        for (int i = 0; i < length; i += 4){
            writeString("mov ecx, [eax + " + i + "]");
            writeString("mov [ebx + " + i + "], ecx");
        }
    }

    private void TransformDeclare(FourTuple tuple) {
        //Declare只支持extern?
        String funcname = tuple.getTuples()[2];
        Type type3 = typeAlias.get(tuple.getTuples()[3]);
        declaredFunctions.put(funcname, (FuncType) type3);
        nowFuncParamType.clear();
    }

    private void TransformTypeFuncParam(FourTuple tuple) {
        Type type3 = typeAlias.get(tuple.getTuples()[3]);
        nowFuncParamType.add(type3);
    }

    private void TransformTypePointer(FourTuple tuple) {
        Type type1 = typeAlias.get(tuple.getTuples()[1]);
        boolean isVar = tuple.getTuples()[2].equals(VarStr);
        PointerType newType = new PointerType(type1.getTypename() + "pointer", type1, isVar);
        typeAlias.put(tuple.getTuples()[3], newType);
    }
    
    private void TransformConstInit(FourTuple tuple) {
        String varname = tuple.getTuples()[3];
        Symbol destvar = varTable.getVar(nowBlockHeight, varname);
        String usingvarname = tuple.getTuples()[1];
        Symbol srcvar = varTable.getVar(nowBlockHeight, usingvarname);
        destvar.setType(srcvar.getType());
        getVarToVar(srcvar, 0, destvar, 0, srcvar.getType().getLength());
    }

    private void TransformConstDefine(FourTuple tuple) {
        //inline是干什么的?
        String varname = tuple.getTuples()[3];
        String typename = tuple.getTuples()[2];
        if (!typename.equals(NoNamedStr)){
            Type type = typeAlias.get(typename);
            varTable.addVar(nowBlockHeight, varname, type);
        }
        else {
            varTable.addVar(nowBlockHeight, varname, Type.i32);
        }
    }

    private void TransformTypedef(FourTuple tuple) {
        Type newType = typeAlias.get(tuple.getTuples()[1]);
        newType.setVolatile(!tuple.getTuples()[2].equals(NoNamedStr));
        typeAlias.put(tuple.getTuples()[3], newType);
    }

    private void TransformStructArg(FourTuple tuple) {
        Type type = typeAlias.get(tuple.getTuples()[1]);
        nowStructArgs.put(tuple.getTuples()[3], type);
    }

    private void TransformStruct(FourTuple tuple) {
        Type newType = new Type(tuple.getTuples()[3], nowStructArgs, false);
        nowStructArgs.clear();
        typeAlias.put(tuple.getTuples()[3], newType);
    }
}
