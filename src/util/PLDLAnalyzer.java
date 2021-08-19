package util;

import exception.PLDLAnalysisException;
import exception.PLDLParsingException;
import exception.PLDLParsingWarning;
import generator.Generator;
import lexer.NFA;
import lexer.SimpleREApply;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import parser.AnalysisTree;
import parser.CFG;
import parser.CFGProduction;
import symbol.SymbolPool;
import translator.MovementCreator;
import translator.Translator;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class PLDLAnalyzer implements Serializable {

    List<Map.Entry<String, NFA>> terminalsNFA = new ArrayList<>();
    Translator translator = null;
    Generator generator = null;
    CFG cfg = null;

    private List<AnalysisTree> getAnalysisTreeList(Element rootEl, String str, MovementCreator creator) throws PLDLAnalysisException {
        Element movementRoot = rootEl.element(str);
        if (movementRoot != null) {
            List<Element> movements = movementRoot.elements("item");
            List<AnalysisTree> movementsTree = new ArrayList<>();
            for (Element movement : movements) {
                try {
                    movementsTree.add(creator.getMovementTree(movement.getText().trim()));
                } catch (PLDLAnalysisException | PLDLParsingException pe) {
                    throw new PLDLAnalysisException(movement.getText().trim(), pe);
                }
            }
            return movementsTree;
        } else {
            return new ArrayList<>();
        }
    }

    public PLDLAnalyzer(InputStream inputStream, String markinStr) throws PLDLParsingException, PLDLAnalysisException, DocumentException {
        translator = new Translator();
        generator = new Generator();
        Set<String> terminals = new HashSet<>();
        Set<String> nonterminals = new HashSet<>();
        Set<String> comments = new HashSet<>();
        Map<String, Set<String>> bannedStrMap = new HashMap<>();
        List<String> prods = new ArrayList<>();
        List<List<AnalysisTree> > movementsTrees = new ArrayList<>();
        List<List<AnalysisTree>> beforeGenerationsTrees = new ArrayList<>();
        List<List<AnalysisTree>> afterGenerationsTrees = new ArrayList<>();

        SAXReader reader = new SAXReader();
        Document document = reader.read(inputStream);
        Element root = document.getRootElement();
        if (root.getName().equals("pldl")) {
            Element el = root.element("terminals");
            //兼容性
            if (el == null){
                el = root.element("terminators");
            }
            List<Element> terminalsEl = null;
            if (el != null){
                terminalsEl = el.elements("item");
            }
            el = root.element("cfgproductions");
            List<Element> pdsEl = null;
            if (el != null){
                pdsEl = el.elements("item");
            }
            el = root.element("comments");
            List<Element> commentsEl = null;
            if (el != null){
                commentsEl = el.elements("item");
            }
            if (pdsEl != null) {
                for (Element e : pdsEl) {
                    String production = e.element("production").getText().trim();
                    prods.add(production);
                    nonterminals.add(production.split("->")[0].trim());

                    movementsTrees.add(getAnalysisTreeList(e, "movements", translator));
                    beforeGenerationsTrees.add(getAnalysisTreeList(e, "before-generations", generator));
                    afterGenerationsTrees.add(getAnalysisTreeList(e, "after-generations", generator));
                }
            }
            if (terminalsEl != null){
                for (Element e : terminalsEl) {
                    String name = e.element("name").getText().trim();
                    terminals.add(name);
                }
            }
            if (commentsEl != null) {
                for (Element e : commentsEl) {
                    String name = e.element("name").getText().trim();
                    comments.add(name);
                }
            }
            if (pdsEl != null) {
                for (Element e : pdsEl) {
                    String production = e.element("production").getText().trim();
                    String[] afters = production.split("->")[1].trim().split(" +");
                    for (String after : afters) {
                        if (!after.equals("null") && !comments.contains(after.trim()) &&
                                !nonterminals.contains(after.trim()) && !terminals.contains(after.trim())) {
                            terminalsNFA.add(new AbstractMap.SimpleEntry<>(after.trim(), NFA.fastNFA(after.trim())));
                            terminals.add(after.trim());
                        }
                    }
                }
            }
            if (terminalsEl != null) {
                for (Element e : terminalsEl) {
                    String name = e.element("name").getText().trim();
                    String regex = e.element("regex").getText().trim();
                    terminalsNFA.add(new AbstractMap.SimpleEntry<>(name, new SimpleREApply(regex).getNFA()));
                    if (e.element("ban") != null){
                        String bannedStrs = e.element("ban").getText().trim();
                        Set<String> bannedStrsSet = new HashSet<>();
                        for (char c : bannedStrs.toCharArray()){
                            bannedStrsSet.add(String.valueOf(c));
                        }
                        bannedStrMap.put(name, bannedStrsSet);
                    }
                    else if (e.element("allow") != null){
                        PLDLParsingWarning.setLog("There is likely to be a significant performance cost in making these calls.");
                        String bannedStrs = e.element("ban").getText().trim();
                        Set<String> allStrsSet = new HashSet<>();
                        for (char c = 0x1; c < 0xffff; ++c){
                            allStrsSet.add(String.valueOf(c));
                        }
                        Set<String> bannedStrsSet = new HashSet<>();
                        for (char c : bannedStrs.toCharArray()){
                            bannedStrsSet.add(String.valueOf(c));
                        }
                        allStrsSet.removeAll(bannedStrsSet);
                        bannedStrMap.put(name, allStrsSet);
                    }
                }
            }
            if (commentsEl != null) {
                for (Element e : commentsEl) {
                    String name = e.element("name").getText().trim();
                    String regex = e.element("regex").getText().trim();
                    terminalsNFA.add(new AbstractMap.SimpleEntry<>(name, new SimpleREApply(regex).getNFA()));
                }
            }
        }


        SymbolPool pool = new SymbolPool();
        pool.initTerminalString(terminals);
        pool.initNonterminalString(nonterminals);
        for (String comment : comments){
            pool.addCommentStr(comment);
        }
        Set<CFGProduction> productions = new HashSet<>();
        for (int i = 0; i < prods.size(); ++i){
            String s = prods.get(i);
            CFGProduction production = CFGProduction.getCFGProductionFromCFGString(s, pool);
            production.setSerialNumber(i + 1);
            productions.add(production);
            translator.addToMovementsMap(production, movementsTrees.get(i));
            generator.addToMovementsMap(production, beforeGenerationsTrees.get(i), afterGenerationsTrees.get(i));
        }
        cfg = new CFG(pool, productions, markinStr);
    }

    public CFG getCFG() {
        return cfg;
    }

    public Translator getTranslator() {
        return translator;
    }

    public Generator getGenerator() {
        return generator;
    }

    public List<Map.Entry<String, NFA>> getTerminalRegexes() {
        return terminalsNFA;
    }

    public Map<String, String> getBannedStrs() {
        return null;
    }
}
