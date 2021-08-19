package preprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Importer {
    private static final String importStr = "import";
    private final String currentDirectory;
    public Importer(String currentDirectory){
        this.currentDirectory = currentDirectory;
    }

    public List<String> parseImport(List<String> inputs) throws FileNotFoundException {
        List<String> temp;
        List<String> result = inputs;
        boolean changed = true;
        while (changed) {
            temp = result;
            result = new ArrayList<>();
            changed = false;
            for (String line: temp) {
                line = line.trim();
                if (line.contains(importStr)) {
                    line = line.substring(line.indexOf(importStr) + importStr.length()).trim();
                    String []importfilenamefamily = line.split("\\.");
                    StringBuilder importfilename = new StringBuilder(currentDirectory);
                    for (String s: importfilenamefamily){
                        importfilename.append(File.separator).append(s);
                    }
                    importfilename.append(".yu");
                    Scanner sc = new Scanner(new FileInputStream(importfilename.toString()));
                    while (sc.hasNextLine()){
                        result.add(sc.nextLine());
                    }
                    changed = true;
                } else {
                    result.add(line);
                }
            }
        }
        return result;
    }
}
