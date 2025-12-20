import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;

public class EzLangInterpreter extends JFrame {

    // === GUI COMPONENTS ===
    private JTextArea inputArea;     
    private JTextArea consoleArea;   
    private DefaultTableModel tokenModel;  
    private DefaultTableModel memoryModel; 

    // === SYSTEM LIMITS ===
    private final int MAX_TOKENS = 1000;
    private final int MAX_VARS = 100;
    private final int MAX_SCOPE_DEPTH = 50;

    // === TOKEN STORAGE ===
    private Token[] tokens = new Token[MAX_TOKENS];
    private int tokenCount = 0;

    // === MEMORY SYSTEM (RAM) ===
    private String[] varNames = new String[MAX_VARS];   
    private String[] varTypes = new String[MAX_VARS];   
    private int[] scalarMemory = new int[MAX_VARS];     
    private int[][] listMemory = new int[MAX_VARS][];   
    private int varCount = 0;

    // === CONTROL FLOW REGISTERS (Enhanced for nested loops) ===
    private int[] loopStartStack = new int[MAX_SCOPE_DEPTH];
    private String[] loopVarStack = new String[MAX_SCOPE_DEPTH];
    private int[] loopLimitStack = new int[MAX_SCOPE_DEPTH];
    private int loopDepth = 0;

    // === SCOPE STACK ===
    private String[] scopeStack = new String[MAX_SCOPE_DEPTH];
    private int stackTop = 0;

    public EzLangInterpreter() {
        setTitle("EzLang Interpreter (Fixed Looping Logic)");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // === INPUT ===
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Source Code"));
        
        inputArea = new JTextArea(15, 30);
        inputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputArea.setText(
            "make list data := [5, 10, 15]\n" +
            "make num limit := 2\n" +
            "print \"--- Nested Loop Test ---\"\n" +
            "\n" +
            "loop i from 0 to limit {\n" +
            "    print \"Index:\"\n" +
            "    print i\n" +
            "    \n" +
            "    if (data[i] > 8) {\n" +
            "        print \"  -> High Value Found!\"\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "print \"--- Finished ---\""
        );
        
        JButton runBtn = new JButton("COMPILE & RUN");
        runBtn.setBackground(new Color(200, 0, 0));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        runBtn.addActionListener(e -> executeProgram());

        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        leftPanel.add(runBtn, BorderLayout.SOUTH);

        // === OUTPUT ===
        consoleArea = new JTextArea(10, 30);
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.GREEN);
        consoleArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        consoleArea.setEditable(false);
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Console Output"));

        // === TABLES ===
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        tokenModel = new DefaultTableModel(new String[]{"Type", "Value"}, 0);
        memoryModel = new DefaultTableModel(new String[]{"Var Name", "Type", "Value"}, 0);
        
        rightPanel.add(new JScrollPane(new JTable(tokenModel)));
        rightPanel.add(new JScrollPane(new JTable(memoryModel)));

        add(leftPanel, BorderLayout.CENTER);
        add(consoleScroll, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);
    }

    private void executeProgram() {
        // Reset Everything
        tokenCount = 0;
        varCount = 0;
        loopDepth = 0;
        stackTop = 0;
        tokenModel.setRowCount(0);
        memoryModel.setRowCount(0);
        consoleArea.setText("--- EXECUTION STARTED ---\n");

        if (runTokenizer()) {
            runInterpreter();
            updateMemoryTable();
        } else {
            consoleArea.append("\nERROR: Syntax Error detected.");
        }
    }

    // ==========================================
    // PHASE 1: TOKENIZER
    // ==========================================
    private boolean runTokenizer() {
        String input = inputArea.getText();
        char[] chars = input.toCharArray();
        int n = chars.length;
        int i = 0;

        try {
            while (i < n) {
                char c = chars[i];
                if (Character.isWhitespace(c)) { i++; continue; }

                if (c == '"') {
                    String s = ""; i++;
                    while (i < n && chars[i] != '"') { s += chars[i]; i++; }
                    if (i < n) i++; 
                    addToken("STRING", s);
                    continue;
                }

                if ("[](){},;".indexOf(c) != -1) {
                    addToken("SYMBOL", Character.toString(c)); i++; continue;
                }

                if (":=><".indexOf(c) != -1) {
                    if (i + 1 < n && chars[i+1] == '=') {
                        addToken("OPERATOR", "" + c + chars[i+1]); i += 2;
                    } else {
                        addToken("OPERATOR", Character.toString(c)); i++;
                    }
                    continue;
                }

                if (Character.isDigit(c)) {
                    String num = "";
                    while (i < n && Character.isDigit(chars[i])) { num += chars[i]; i++; }
                    addToken("NUMBER", num);
                    continue;
                }

                if (Character.isLetter(c)) {
                    String word = "";
                    while (i < n && (Character.isLetterOrDigit(chars[i]) || chars[i] == '_')) {
                        word += chars[i]; i++;
                    }
                    if (isKeyword(word)) addToken("KEYWORD", word);
                    else addToken("IDENTIFIER", word);
                    continue;
                }
                i++;
            }
            return true;
        } catch (Exception e) { return false; }
    }

    // ==========================================
    // PHASE 2: INTERPRETER (Fixed Loop Logic)
    // ==========================================
    private void runInterpreter() {
        int i = 0;
        while (i < tokenCount) {
            Token t = tokens[i];

            // --- COMMAND: PRINT ---
            if (t.value.equals("print")) {
                String output = "";
                Token next = tokens[i+1];
                if (next.type.equals("STRING")) {
                    output = next.value; i += 2;
                } else {
                    int val = resolveNumber(i + 1);
                    output = String.valueOf(val);
                    if (i+2 < tokenCount && tokens[i+2].value.equals("[")) i += 5; 
                    else i += 2;
                }
                consoleArea.append(output + "\n");
            }

            // --- COMMAND: MAKE ---
            else if (t.value.equals("make")) {
                String type = tokens[i+1].value; 
                String name = tokens[i+2].value;
                if (type.equals("num")) {
                    int val = Integer.parseInt(tokens[i+4].value);
                    saveScalar(name, val);
                    i += 5;
                } else if (type.equals("list")) {
                    int k = i + 5;
                    int[] tempBuffer = new int[50];
                    int foundCount = 0;
                    while (!tokens[k].value.equals("]")) {
                        if (tokens[k].type.equals("NUMBER")) {
                            tempBuffer[foundCount++] = Integer.parseInt(tokens[k].value);
                        }
                        k++;
                    }
                    int[] finalArray = new int[foundCount];
                    for(int z=0; z<foundCount; z++) finalArray[z] = tempBuffer[z];
                    saveList(name, finalArray);
                    i = k + 1;
                }
            }

            // --- COMMAND: IF ---
            else if (t.value.equals("if")) {
                int leftVal = resolveNumber(i + 2);
                int opIndex = i + 3;
                if (tokens[i+3].value.equals("[")) opIndex = i + 6;
                String op = tokens[opIndex].value;
                int rightVal = resolveNumber(opIndex + 1);
                
                boolean cond = false;
                if (op.equals(">")) cond = leftVal > rightVal;
                if (op.equals("<")) cond = leftVal < rightVal;
                if (op.equals("==")) cond = leftVal == rightVal;
                
                if (!cond) {
                    // False - skip the entire block
                    i = findBlockEnd(i);
                } else {
                    // True - enter the block
                    scopeStack[stackTop++] = "IF";
                    // Move past the opening {
                    while (i < tokenCount && !tokens[i].value.equals("{")) i++;
                    i++; 
                }
            }

            // --- COMMAND: LOOP ---
            else if (t.value.equals("loop")) {
                String varName = tokens[i+1].value;
                int startVal = resolveNumber(i+3);
                int endVal = resolveNumber(i+5);
                
                // Check if this is a brand new loop entry or a continuation
                boolean isNewLoop = (loopDepth == 0 || !loopVarStack[loopDepth-1].equals(varName) 
                                     || loopStartStack[loopDepth-1] != i);
                
                if (isNewLoop) {
                    // FIRST TIME entering this loop
                    saveScalar(varName, startVal);
                    
                    // Push loop info onto stack
                    loopStartStack[loopDepth] = i;
                    loopVarStack[loopDepth] = varName;
                    loopLimitStack[loopDepth] = endVal;
                    loopDepth++;
                    
                    scopeStack[stackTop++] = "LOOP";
                    
                    // Move past "loop i from X to Y {"
                    while (i < tokenCount && !tokens[i].value.equals("{")) i++;
                    i++;
                } else {
                    // JUMP BACK - check if we should continue
                    int currentVal = getScalar(varName);
                    
                    if (currentVal > endVal) {
                        // Loop is done - exit
                        loopDepth--;
                        i = findBlockEnd(i);
                    } else {
                        // Continue looping
                        scopeStack[stackTop++] = "LOOP";
                        
                        // Move past "loop i from X to Y {"
                        while (i < tokenCount && !tokens[i].value.equals("{")) i++;
                        i++;
                    }
                }
            }

            // --- COMMAND: CLOSING BRACE } ---
            else if (t.value.equals("}")) {
                if (stackTop > 0) {
                    String currentScope = scopeStack[--stackTop];
                    
                    if (currentScope.equals("IF")) {
                        // Just exit the IF block
                        i++;
                    } 
                    else if (currentScope.equals("LOOP")) {
                        // Increment loop variable and jump back
                        int depth = loopDepth - 1;
                        String loopVar = loopVarStack[depth];
                        int currentVal = getScalar(loopVar);
                        saveScalar(loopVar, currentVal + 1);
                        
                        // Jump back to loop start
                        i = loopStartStack[depth];
                    }
                } else {
                    i++;
                }
            }
            
            else {
                i++;
            }
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private int findBlockEnd(int startIndex) {
        int index = startIndex;
        int braces = 0;
        
        // Find the opening brace first
        while (index < tokenCount && !tokens[index].value.equals("{")) {
            index++;
        }
        
        if (index < tokenCount) {
            braces = 1;
            index++;
        }
        
        // Now find the matching closing brace
        while (index < tokenCount && braces > 0) {
            if (tokens[index].value.equals("{")) braces++;
            if (tokens[index].value.equals("}")) braces--;
            index++;
        }
        return index;
    }

    private int resolveNumber(int index) {
        Token t = tokens[index];
        if (t.type.equals("NUMBER")) return Integer.parseInt(t.value);
        if (t.type.equals("IDENTIFIER")) {
            String name = t.value;
            if (index + 1 < tokenCount && tokens[index+1].value.equals("[")) {
                String idxStr = tokens[index+2].value;
                int arrIndex = Character.isLetter(idxStr.charAt(0)) ? getScalar(idxStr) : Integer.parseInt(idxStr);
                return getListValue(name, arrIndex);
            } else {
                return getScalar(name);
            }
        }
        return 0;
    }

    private void addToken(String t, String v) {
        if (tokenCount < MAX_TOKENS) {
            tokens[tokenCount] = new Token(t, v);
            tokenModel.addRow(new Object[]{t, v});
            tokenCount++;
        }
    }

    private boolean isKeyword(String w) {
        String[] k = {"make", "print", "if", "list", "num", "loop", "from", "to"};
        for (String s : k) if (s.equals(w)) return true;
        return false;
    }

    private void saveScalar(String name, int val) {
        for(int k=0; k<varCount; k++) {
            if(varNames[k].equals(name) && varTypes[k].equals("NUM")) {
                scalarMemory[k] = val; return;
            }
        }
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
        return -1;
    }

    private int getListValue(String name, int index) {
        for(int k=0; k<varCount; k++) {
            if(varNames[k].equals(name) && varTypes[k].equals("LIST")) {
                if(index >= 0 && index < listMemory[k].length) return listMemory[k][index];
            }
        }
        return -1;
    }

    private void updateMemoryTable() {
        memoryModel.setRowCount(0);
        for(int k=0; k<varCount; k++) {
            String valStr = varTypes[k].equals("NUM") ? String.valueOf(scalarMemory[k]) : Arrays.toString(listMemory[k]);
            memoryModel.addRow(new Object[]{varNames[k], varTypes[k], valStr});
        }
    }

    class Token {
        String type, value;
        public Token(String t, String v) { type = t; value = v; }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new EzLangInterpreter().setVisible(true));
    }
}
