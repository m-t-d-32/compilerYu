package app;

import backend.FourTuple;
import backend.X86Transformer;
import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import exception.PLDLAnalysisException;
import exception.PLDLAssemblingException;
import exception.PLDLParsingException;
import generator.Generator;
import lexer.Lexer;
import org.dom4j.DocumentException;
import parser.AnalysisTree;
import parser.CFG;
import parser.TransformTable;
import preprocessor.Importer;
import symbol.Symbol;
import translator.Translator;
import util.PLDLAnalyzer;

import java.io.*;
import java.util.*;

public class ConsoleApplication {

    private PLDLAnalyzer PLDLAnalyzer = null;
    private Lexer lexer = null;
    private CFG cfg = null;
    private TransformTable table = null;
    private Set<Character> emptyChars = null;

    String inputfilename = null, outputfilename = null;

    private final File []testfolders = {
            new File("test/examples"),
            new File("test/hello"),
            new File("test/GeeOS/src"),
            new File("test/GeeOS/usr"),
    };

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    private String currentDirectory;
    private final String assembler = "nasm";
    private final String assembleConfig = "-f elf32";
    private final String objectgenerator = "gcc -m32";
    private final String objectConfig = "-lc";
    private final String outputConfig = "-o";
    protected String []args;

    public ConsoleApplication(String currentDirectory, String []args){
        this.currentDirectory = currentDirectory;
        this.args = args;
    }

    private List<String> getAllFiles(String root){
        Queue<File> pathqueue = new ArrayDeque<>();
        List<String> result = new ArrayList<>();
        pathqueue.add(new File(root));
        while (!pathqueue.isEmpty()){
            File nowpath = pathqueue.poll();
            result.add(nowpath.getAbsolutePath());
            if (nowpath.isDirectory()) {
                pathqueue.addAll(Arrays.asList(Objects.requireNonNull(nowpath.listFiles())));
            }
        }
        return result;
    }

    public void LLBeginFormXML(InputStream xmlStream) throws PLDLParsingException, PLDLAnalysisException, DocumentException, IOException {
        System.out.println("XML文件解析中...");
        PLDLAnalyzer = new PLDLAnalyzer(xmlStream, "Program");
        System.out.println("XML文件解析成功。");

        System.out.println("正在构建词法分析器...");
        lexer = new Lexer(PLDLAnalyzer.getTerminalRegexes(), PLDLAnalyzer.getBannedStrs());
        emptyChars = new HashSet<>();
        emptyChars.add(' ');
        emptyChars.add('\t');
        emptyChars.add('\n');
        emptyChars.add('\r');
        emptyChars.add('\f');
        System.out.println("词法分析器构建成功。");

        System.out.println("正在构建语法分析器...");
        cfg = PLDLAnalyzer.getCFG();
        table = cfg.getTable();
        System.out.println("表项共" + table.getTableMap().size() + "*" +
                (cfg.getCFGNonterminals().size() + cfg.getCFGTerminals().size()) + "项");
        System.out.println("基于LR（1）分析的语法分析器构建成功。");

        System.out.println("特定语言类型的内部编译器架构形成。");
    }

    public void LLBeginFromModel(InputStream modelStream) throws Exception {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        FSTObjectInput in = conf.getObjectInput(modelStream);
        PLDLAnalyzer = (PLDLAnalyzer) in.readObject(PLDLAnalyzer.class);
        lexer = (Lexer) in.readObject(Lexer.class);
        cfg = (CFG) in.readObject(CFG.class);
        table = (TransformTable) in.readObject(TransformTable.class);

        emptyChars = new HashSet<>();
        emptyChars.add(' ');
        emptyChars.add('\t');
        emptyChars.add('\n');
        emptyChars.add('\r');
        emptyChars.add('\f');
    }

    public void LLParse(InputStream codeStream,
                        OutputStream fourTupleOutputStream, OutputStream assembleOutputStream)
            throws PLDLAnalysisException, PLDLParsingException, IOException, PLDLAssemblingException {

        System.out.println("正在读取代码文件...");
        Scanner sc = new Scanner(codeStream);
        List<String> lines = new ArrayList<>();
        while (sc.hasNextLine()){
            lines.add(sc.nextLine());
        }
        Importer importer = new Importer(currentDirectory);
        lines = importer.parseImport(lines);

        String codestr = String.join("\n", lines);
        codestr += "\n";
        codestr = codestr.replaceAll("[^\\x00-\\x7F]", "");

        System.out.println("正在对代码进行词法分析...");
        List<Symbol> symbols = lexer.analysis(codestr, emptyChars);
        symbols = cfg.revertToStdAbstractSymbols(symbols);
        symbols = cfg.eraseComments(symbols);

        System.out.println("正在对代码进行语法分析构建分析树...");
        AnalysisTree tree = table.getAnalysisTree(symbols);
        List<String> rt4 = new ArrayList<>();

        System.out.println("正在对分析树进行语义赋值生成注释分析树...");
        Translator translator = PLDLAnalyzer.getTranslator();
        translator.checkMovementsMap();
        translator.doTreesMovements(tree);

        System.out.println("正在根据注释分析树生成四元式...");
        Generator generator = PLDLAnalyzer.getGenerator();
        generator.doTreesMovements(tree, rt4);
        System.out.println("生成四元式成功");

        if (fourTupleOutputStream != null) {
            PrintStream fourTuplePrintStream = new PrintStream(fourTupleOutputStream);
            for (String s : rt4) {
                fourTuplePrintStream.println(s);
            }
            fourTuplePrintStream.close();
        }

        List<FourTuple> inputs = new ArrayList<>();
        for (String line: rt4){
            inputs.add(new FourTuple(line));
        }

        System.out.println("调用后端生成汇编文件...");
        X86Transformer transformer = new X86Transformer(inputs);
        String results = transformer.parse();
        PrintStream assemblePrintStream = new PrintStream(assembleOutputStream);
        assemblePrintStream.println(results);
        assemblePrintStream.close();
        System.out.println("生成汇编成功");
    }

    public void LLSaveModel(OutputStream fileOutputStream) throws IOException {
        System.out.println("保存模型中……");
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        FSTObjectOutput out = conf.getObjectOutput(fileOutputStream);
        out.writeObject(PLDLAnalyzer, PLDLAnalyzer.class);
        out.writeObject(lexer, Lexer.class);
        out.writeObject(cfg, CFG.class);
        out.writeObject(table, TransformTable.class);
        out.flush();
        fileOutputStream.close();
        System.out.println("保存模型成功");
    }

    protected void clearFiles() {
        for (File folder: testfolders){
            List<String> testfiles = getAllFiles(folder.getAbsolutePath());
            for (String f: testfiles){
                if (f.endsWith("yu")){
                    clearfile(new File(f));
                }
            }
        }
        System.out.println("清理成功");
    }

    protected void prepare() throws Exception {
        this.LLBeginFromModel(new FileInputStream("res/model/yu.model"));
//        this.LLBeginFormXML(new FileInputStream("res/xml/yu.xml"));
//        this.LLSaveModel(new FileOutputStream("res/model/yu.model"));
    }

    protected void calculate() throws Exception {
        //compiler -S -o testcase.s testcase.sy
        for (int i = 0; i < args.length; ++i){
            String arg = args[i];
            if (arg.trim().equals("-o")){
                outputfilename = args[i + 1];
            }
            else if (!arg.trim().equals("-S") && !arg.trim().contains("-O")){
                inputfilename = args[i];
            }
        }
        LLParse(new FileInputStream(inputfilename), null, new FileOutputStream(outputfilename));
    }

    protected int executeCmd(String command) throws Exception {
        System.out.println("Execute command : " + command);
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(new String[]{"sh", "-c", command});
        process.waitFor();
        int retVal = process.exitValue();
        Scanner sc = new Scanner(process.getErrorStream());
        while (sc.hasNextLine()){
            System.err.println(sc.nextLine());
        }
        sc = new Scanner(process.getInputStream());
        while (sc.hasNextLine()){
            System.out.println(sc.nextLine());
        }
        return retVal;
    }

    protected boolean executeProgram(String command) throws Exception {
        executeCmd(command);
        return true;
    }

    protected void runFile(File f) throws Exception {
        String codeFileName, fourTupleFileName, assembleFileName, outFileName, outFileInputName, outFileOutputName, outFileStdOutputName, objectFileName;

        codeFileName = f.getAbsolutePath();
        String pureFileName = codeFileName.substring(0, codeFileName.length() - 3);
        fourTupleFileName = pureFileName + ".4tu";
        assembleFileName = pureFileName + ".s";
        outFileName = pureFileName + ".exe";
        objectFileName = pureFileName + ".o";
        //compile
        LLParse(new FileInputStream(codeFileName), new FileOutputStream(fourTupleFileName), new FileOutputStream(assembleFileName));
        //assemble
        boolean t1 = (executeCmd(assembler + " " + assembleFileName + " " + outputConfig + " " + objectFileName + " " + assembleConfig) == 0);
        boolean t2 = (executeCmd(objectgenerator + " " + objectFileName + " " + outputConfig + " " + outFileName + " " + objectConfig) == 0);
        //run
        boolean t3 = executeProgram(outFileName);
        if (!t1 || !t2 || !t3){
            throw new Exception(null, null);
        }
    }

    protected void testFiles() throws Exception {
        int counter = 0;
        int truecounter = 0;
        for (File folder: testfolders){
            setCurrentDirectory(folder.getAbsolutePath());
            List<String> testfiles = getAllFiles(folder.getAbsolutePath());
            for (String f: testfiles){
                if (f.endsWith(".yu")){
                    System.out.println(f);
                    ++counter;
                    clearfile(new File(f));
                    runFile(new File(f));
                    ++truecounter;
                }
            }
        }
        System.out.println("PASS: " + truecounter + "/" + counter);
    }

    protected void clearfile(File f){
        String codeFileName, fourTupleFileName, assembleFileName, outFileName, outFileInputName, outFileOutputName, outFileStdOutputName, objectFileName;

        codeFileName = f.getAbsolutePath();
        String pureFileName = codeFileName.substring(0, codeFileName.length() - 3);
        fourTupleFileName = pureFileName + ".4tu";
        assembleFileName = pureFileName + ".s";
        outFileName = pureFileName + ".exe";
        objectFileName = pureFileName + ".o";

        new File(fourTupleFileName).delete();
        new File(assembleFileName).delete();
        new File(outFileName).delete();
        new File(objectFileName).delete();
    }
}