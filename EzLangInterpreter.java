import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
import java.util.Arrays;

public class EzLangInterpreter extends JFrame {

    // === GUI COMPONENTS ===
    private JTextArea inputArea;     // Code Editor
    private JTextArea consoleArea;   // Output Screen
    private DefaultTableModel tokenModel;  // To show Tokenizer results
    private DefaultTableModel memoryModel; // To show Variables/Arrays in RAM

    // === SYSTEM LIMITS (Fixed Arrays) ===
    private final int MAX_TOKENS = 1000;
    private final int MAX_VARS = 100;

    // === TOKEN STORAGE (The Lexer Output) ===
    private Token[] tokens = new Token[MAX_TOKENS];
    private int tokenCount = 0;

    // === MEMORY SYSTEM (The "RAM" - ARRAYS ONLY) ===
    private String[] varNames = new String[MAX_VARS];   // Names: "x", "score"
    private String[] varTypes = new String[MAX_VARS];   // Types: "NUM", "LIST"
    private int[] scalarMemory = new int[MAX_VARS];     // Values for single numbers
    private int[][] listMemory = new int[MAX_VARS][];   // Values for arrays (Jagged Array)
    private int varCount = 0;

    public EzLangInterpreter() {
        // === WINDOW SETUP ===
        setTitle("Ja-bisaya interpretir");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // === 1. SOURCE CODE INPUT ===
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Source Code"));
        
        inputArea = new JTextArea(15, 30);
        inputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputArea.setText(
            "make list data := [10, 20, 50, 100]\n" +
            "make num index := 2\n" +
            "print \"--- Starting Program ---\"\n" +
            "print \"Value at index 2 is:\"\n" +
            "print data[index]\n" +  // Dynamic Array Access
            "\n" +
            "if (data[3] > 80) {\n" +
            "    print \"High value detected!\"\n" +
            "    make num bonus := 500\n" +
            "    print \"Bonus added:\"\n" +
            "    print bonus\n" +
            "}"
        );
        
        JButton runBtn = new JButton("COMPILE & RUN");
        runBtn.setBackground(new Color(40, 167, 69)); // Green
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        runBtn.addActionListener(e -> executeProgram());

        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        leftPanel.add(runBtn, BorderLayout.SOUTH);

        // === 2. CONSOLE OUTPUT ===
        consoleArea = new JTextArea(10, 30);
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.GREEN);
        consoleArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        consoleArea.setEditable(false);
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Console Output"));

        // === 3. TABLES (Tokens & Memory) ===
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Token Table
        String[] tokenCols = {"Type", "Value"};
        tokenModel = new DefaultTableModel(tokenCols, 0);
        JTable tokenTable = new JTable(tokenModel);
        JScrollPane tokenScroll = new JScrollPane(tokenTable);
        tokenScroll.setBorder(BorderFactory.createTitledBorder("Tokenizer (Lexical Analysis)"));
        
        // Memory Table
        String[] memCols = {"Var Name", "Type", "Value"};
        memoryModel = new DefaultTableModel(memCols, 0);
        JTable memTable = new JTable(memoryModel);
        JScrollPane memScroll = new JScrollPane(memTable);
        memScroll.setBorder(BorderFactory.createTitledBorder("System Memory (RAM)"));

        rightPanel.add(tokenScroll);
        rightPanel.add(memScroll);

        // Add panels to frame
        add(leftPanel, BorderLayout.CENTER);
        add(consoleScroll, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);
    }

    // ==========================================
    // MAIN EXECUTION FLOW
    // ==========================================
    private void executeProgram() {
        // 1. CLEAR PREVIOUS DATA
        tokenCount = 0;
        varCount = 0;
        tokenModel.setRowCount(0);
        memoryModel.setRowCount(0);
        consoleArea.setText("--- EXECUTION STARTED ---\n");

        // 2. RUN TOKENIZER (Lexer)
        boolean lexerSuccess = runTokenizer();
        
        // 3. IF TOKENIZER OK, RUN INTERPRETER
        if (lexerSuccess) {
            runInterpreter();
            updateMemoryTable(); // Show final memory state
        } else {
            consoleArea.append("\nERROR: Lexical Analysis Failed.");
        }
    }

    // ==========================================
    // PHASE 1: TOKENIZER (LEXER)
    // ==========================================
    private boolean runTokenizer() {
        String input = inputArea.getText();
        char[] chars = input.toCharArray(); // Converting string to char array
        int n = chars.length;
        int i = 0;

        try {
            while (i < n) {
                char c = chars[i];

                // Skip Whitespace
                if (Character.isWhitespace(c)) { i++; continue; }

                // RULE: Strings ("...")
                if (c == '"') {
                    String s = ""; i++;
                    while (i < n && chars[i] != '"') { s += chars[i]; i++; }
                    if (i < n) i++; // Consume closing quote
                    addToken("STRING", s);
                    continue;
                }

                // RULE: Symbols & Brackets
                if ("[](){},;".indexOf(c) != -1) {
                    addToken("SYMBOL", Character.toString(c));
                    i++;
                    continue;
                }

                // RULE: Operators (:=, >, <)
                if (":=><".indexOf(c) != -1) {
                    if (i + 1 < n && chars[i+1] == '=') {
                        addToken("OPERATOR", "" + c + chars[i+1]);
                        i += 2;
                    } else {
                        addToken("OPERATOR", Character.toString(c));
                        i++;
                    }
                    continue;
                }

                // RULE: Numbers
                if (Character.isDigit(c)) {
                    String num = "";
                    while (i < n && Character.isDigit(chars[i])) { num += chars[i]; i++; }
                    addToken("NUMBER", num);
                    continue;
                }

                // RULE: Words (Keywords or Identifiers)
                if (Character.isLetter(c)) {
                    String word = "";
                    while (i < n && (Character.isLetterOrDigit(chars[i]) || chars[i] == '_')) {
                        word += chars[i];
                        i++;
                    }
                    if (isKeyword(word)) {
                        addToken("KEYWORD", word);
                    } else {
                        addToken("IDENTIFIER", word);
                    }
                    continue;
                }

                // Unknown Character
                i++;
            }
            return true;
        } catch (Exception e) {
            consoleArea.append("Lexer Error: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // PHASE 2: INTERPRETER (RUNNER)
    // ==========================================
    private void runInterpreter() {
        int i = 0;
        while (i < tokenCount) {
            Token t = tokens[i];

            // --- COMMAND: PRINT ---
            if (t.value.equals("print")) {
                // Syntax: print "hello"  OR  print x  OR print x[0]
                String output = "";
                
                // Peek ahead to see what we are printing
                Token next = tokens[i+1];
                
                if (next.type.equals("STRING")) {
                    output = next.value;
                    i += 2;
                } 
                else {
                    // It's a number or variable. We use a helper to resolve it.
                    // We must check if it is an array access (x[0])
                    int val = resolveNumber(i + 1);
                    output = String.valueOf(val);
                    
                    // Advance index manually based on complexity
                    if (i+2 < tokenCount && tokens[i+2].value.equals("[")) {
                         // It was an array access (e.g. data[0]), so we skipped: name, [, index, ]
                         i += 5; 
                    } else {
                         i += 2;
                    }
                }
                consoleArea.append(output + "\n");
            }

            // --- COMMAND: MAKE (Variable Declaration) ---
            else if (t.value.equals("make")) {
                // Syntax: make [type] [name] := [value]
                String type = tokens[i+1].value; 
                String name = tokens[i+2].value;
                // i+3 is ":="
                
                if (type.equals("num")) {
                    int val = Integer.parseInt(tokens[i+4].value);
                    saveScalar(name, val);
                    i += 5;
                } 
                else if (type.equals("list")) {
                    // Syntax: make list x := [ 10 , 20 ]
                    // Parse the array literal manually
                    int startToken = i + 5; 
                    int k = startToken;
                    
                    // Temporary buffer to hold numbers found
                    int[] tempBuffer = new int[50];
                    int foundCount = 0;
                    
                    while (!tokens[k].value.equals("]")) {
                        if (tokens[k].type.equals("NUMBER")) {
                            tempBuffer[foundCount] = Integer.parseInt(tokens[k].value);
                            foundCount++;
                        }
                        k++;
                    }
                    
                    // Create exact size array
                    int[] finalArray = new int[foundCount];
                    // Manual array copy loop
                    for(int z=0; z<foundCount; z++) finalArray[z] = tempBuffer[z];
                    
                    saveList(name, finalArray);
                    
                    // Set main index after the closing bracket
                    i = k + 1;
                }
            }

            // --- COMMAND: IF (Conditional) ---
            else if (t.value.equals("if")) {
                // Syntax: if ( A > B ) {
                int leftVal = resolveNumber(i + 2);
                
                // The operator position depends on if A was a variable (x) or array (x[0])
                int opIndex = i + 3;
                if (tokens[i+3].value.equals("[")) opIndex = i + 6;
                
                String op = tokens[opIndex].value;
                int rightVal = resolveNumber(opIndex + 1);
                
                boolean condition = false;
                if (op.equals(">")) condition = leftVal > rightVal;
                if (op.equals("<")) condition = leftVal < rightVal;
                if (op.equals("==")) condition = leftVal == rightVal;
                
                // If FALSE, skip until "}"
                if (!condition) {
                    while (!tokens[i].value.equals("}")) i++;
                    i++; // Skip the closing brace
                } else {
                    // If TRUE, just move past the "{" and keep executing
                    while (!tokens[i].value.equals("{")) i++;
                    i++; 
                }
            } 
            
            else {
                // Skip unrecognized tokens (or closing braces)
                i++;
            }
        }
    }

    // ==========================================
    // HELPERS (MEMORY & PARSING)
    // ==========================================

    // Resolves a number from tokens. Handles: "50", "x", and "x[0]"
    private int resolveNumber(int index) {
        Token t = tokens[index];
        
        // 1. Literal Number
        if (t.type.equals("NUMBER")) return Integer.parseInt(t.value);
        
        // 2. Variable or Array
        if (t.type.equals("IDENTIFIER")) {
            String name = t.value;
            
            // Check if next token is '[' (Array Access)
            if (index + 1 < tokenCount && tokens[index+1].value.equals("[")) {
                // Format: name [ index ]
                String indexValStr = tokens[index+2].value; 
                int arrIndex = 0;
                
                // Is the index a raw number "0" or a variable "i"?
                if (Character.isLetter(indexValStr.charAt(0))) {
                    arrIndex = getScalar(indexValStr);
                } else {
                    arrIndex = Integer.parseInt(indexValStr);
                }
                return getListValue(name, arrIndex);
            } 
            else {
                // Regular Variable
                return getScalar(name);
            }
        }
        return 0;
    }

    private void addToken(String type, String value) {
        if (tokenCount < MAX_TOKENS) {
            tokens[tokenCount] = new Token(type, value);
            tokenModel.addRow(new Object[]{type, value});
            tokenCount++;
        }
    }

    private boolean isKeyword(String w) {
        // Manual array check
        String[] k = {"make", "print", "if", "list", "num"};
        for (String s : k) {
            if (s.equals(w)) return true;
        }
        return false;
    }

    // --- MEMORY OPERATIONS ---
    private void saveScalar(String name, int val) {
        varNames[varCount] = name;
        varTypes[varCount] = "NUM";
        scalarMemory[varCount] = val;
        varCount++;
    }

    private void saveList(String name, int[] arr) {
        varNames[varCount] = name;
        varTypes[varCount] = "LIST";
        listMemory[varCount] = arr;
        varCount++;
    }

    private int getScalar(String name) {
        for(int k=0; k<varCount; k++) {
            if(varNames[k].equals(name) && varTypes[k].equals("NUM")) return scalarMemory[k];
        }
        return -1; // Error
    }

    private int getListValue(String name, int index) {
        for(int k=0; k<varCount; k++) {
            if(varNames[k].equals(name) && varTypes[k].equals("LIST")) {
                if(index >= 0 && index < listMemory[k].length) return listMemory[k][index];
            }
        }
        return -1; // Error
    }

    private void updateMemoryTable() {
        memoryModel.setRowCount(0); // Clear
        for(int k=0; k<varCount; k++) {
            String valStr = "";
            if (varTypes[k].equals("NUM")) valStr = String.valueOf(scalarMemory[k]);
            else valStr = Arrays.toString(listMemory[k]);
            memoryModel.addRow(new Object[]{varNames[k], varTypes[k], valStr});
        }
    }

    // Simple Token Class
    class Token {
        String type, value;
        public Token(String t, String v) { type = t; value = v; }
    }

    public static void main(String[] args) {
        // Set Look and Feel for better UI
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new EzLangInterpreter().setVisible(true));
    }
}
