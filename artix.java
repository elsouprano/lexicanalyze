import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;

public class artix extends JFrame {

    // === GUI COMPONENTS ===
    private JTextArea inputArea;     
    private JTextArea consoleArea;   
    private DefaultTableModel tokenModel;  
    private DefaultTableModel memoryModel; 
    private JPanel logoContainer; 

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

    // === CONTROL FLOW REGISTERS ===
    private int[] loopStartStack = new int[MAX_SCOPE_DEPTH];
    private String[] loopVarStack = new String[MAX_SCOPE_DEPTH];
    private int[] loopLimitStack = new int[MAX_SCOPE_DEPTH];
    private int loopDepth = 0;

    // === SCOPE STACK ===
    private String[] scopeStack = new String[MAX_SCOPE_DEPTH];
    private int stackTop = 0;

    // === ERROR HANDLING CLASS ===
    class SyntaxException extends Exception {
        public SyntaxException(String message) { super(message); }
    }

    public artix() {
        // === GUI SETUP ===
        setTitle("Artix - Final Fixed Version");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(Color.WHITE);
        headerBar.setPreferredSize(new Dimension(1200, 70));
        
        logoContainer = new JPanel();
        logoContainer.setBackground(Color.WHITE);
        logoContainer.setPreferredSize(new Dimension(300, 60));
        
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerWrapper.setBackground(Color.WHITE);
        centerWrapper.add(logoContainer);
        headerBar.add(centerWrapper, BorderLayout.CENTER);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        toolbar.setBackground(new Color(175, 238, 238));
        toolbar.setPreferredSize(new Dimension(1200, 35));
        
        JButton runBtn = new JButton("Run");
        runBtn.setPreferredSize(new Dimension(90, 25));
        runBtn.setBackground(new Color(11, 83, 148));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        runBtn.addActionListener(e -> executeProgram());
        toolbar.add(runBtn);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerBar, BorderLayout.NORTH);
        topPanel.add(toolbar, BorderLayout.SOUTH);

        // Main Content
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(new Color(108, 166, 205));
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Source Code Area
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(new Color(108, 166, 205));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Source Code"));
        
        inputArea = new JTextArea(15, 40);
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
        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        // Tables Area
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        rightPanel.setBackground(new Color(108, 166, 205));
        rightPanel.setPreferredSize(new Dimension(350, 0));
        
        tokenModel = new DefaultTableModel(new String[]{"Line", "Type", "Value"}, 0);
        memoryModel = new DefaultTableModel(new String[]{"Var Name", "Type", "Value"}, 0);
        
        JScrollPane tokenScroll = new JScrollPane(new JTable(tokenModel));
        tokenScroll.setBorder(BorderFactory.createTitledBorder("Tokens"));
        
        JScrollPane memoryScroll = new JScrollPane(new JTable(memoryModel));
        memoryScroll.setBorder(BorderFactory.createTitledBorder("Memory"));
        
        rightPanel.add(tokenScroll);
        rightPanel.add(memoryScroll);
        
        mainContent.add(leftPanel, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);

        // Console Area
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(new Color(90, 140, 180));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        consolePanel.setPreferredSize(new Dimension(1200, 200));
        
        consoleArea = new JTextArea(8, 30);
        consoleArea.setBackground(new Color(90, 140, 180));
        consoleArea.setForeground(Color.WHITE);
        consoleArea.setFont(new Font("Monospaced", Font.BOLD, 13));
        consoleArea.setEditable(false);
        
        consolePanel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(mainContent, BorderLayout.CENTER);
        add(consolePanel, BorderLayout.SOUTH);
    }

    public void setLogoImage(String imagePath) {
        logoContainer.removeAll();
        try {
            ImageIcon originalIcon = new ImageIcon(imagePath);
            Image originalImage = originalIcon.getImage();
            Image scaledImage = originalImage.getScaledInstance(300, 60, Image.SCALE_SMOOTH);
            logoContainer.add(new JLabel(new ImageIcon(scaledImage)));
            logoContainer.revalidate();
            logoContainer.repaint();
        } catch (Exception e) {
            logoContainer.add(new JLabel("Logo Not Found"));
        }
    }

    // === EXECUTION CORE ===

    private void executeProgram() {
        // HARD RESET
        tokenCount = 0;
        varCount = 0;
        loopDepth = 0;
        stackTop = 0;
        
        // Clear Arrays
        Arrays.fill(loopStartStack, 0);
        Arrays.fill(loopVarStack, null);
        Arrays.fill(scopeStack, null);
        
        tokenModel.setRowCount(0);
        memoryModel.setRowCount(0);
        consoleArea.setText("--- EXECUTION STARTED ---\n");
        consoleArea.setForeground(Color.WHITE);

        try {
            if (runTokenizer()) {
                preCheckStructure(); 
                runInterpreter();
                updateMemoryTable();
                consoleArea.append("\n--- SUCCESS ---");
            }
        } catch (SyntaxException e) {
            consoleArea.setForeground(new Color(255, 100, 100)); // Red for Error
            consoleArea.append("\nRUNTIME ERROR:\n" + e.getMessage());
        } catch (Exception e) {
            consoleArea.setForeground(new Color(255, 100, 100));
            consoleArea.append("\nCRITICAL ERROR: " + e.toString());
            e.printStackTrace();
        }
    }

    private boolean runTokenizer() throws SyntaxException {
        String input = inputArea.getText();
        char[] chars = input.toCharArray();
        int n = chars.length;
        int i = 0;
        int lineNum = 1;

        while (i < n) {
            char c = chars[i];
            
            if (c == '\n') { lineNum++; i++; continue; }
            if (Character.isWhitespace(c)) { i++; continue; }

            // Strings
            if (c == '"') {
                String s = ""; i++;
                boolean closed = false;
                while (i < n) {
                    if (chars[i] == '"') { closed = true; break; }
                    if (chars[i] == '\n') lineNum++;
                    s += chars[i];
                    i++;
                }
                if (!closed) throw new SyntaxException("Line " + lineNum + ": Unclosed string literal.");
                if (i < n) i++; 
                addToken("STRING", s, lineNum);
                continue;
            }

            // Symbols
            if ("[](){},;".indexOf(c) != -1) {
                addToken("SYMBOL", Character.toString(c), lineNum); i++; continue;
            }

            // Operators
            if (":=><".indexOf(c) != -1) {
                if (i + 1 < n && chars[i+1] == '=') {
                    addToken("OPERATOR", "" + c + chars[i+1], lineNum); i += 2;
                } else {
                    addToken("OPERATOR", Character.toString(c), lineNum); i++;
                }
                continue;
            }

            // Numbers
            if (Character.isDigit(c)) {
                String num = "";
                while (i < n && Character.isDigit(chars[i])) { num += chars[i]; i++; }
                addToken("NUMBER", num, lineNum);
                continue;
            }

            // Identifiers
            if (Character.isLetter(c)) {
                String word = "";
                while (i < n && (Character.isLetterOrDigit(chars[i]) || chars[i] == '_')) {
                    word += chars[i]; i++;
                }
                if (isKeyword(word)) addToken("KEYWORD", word, lineNum);
                else addToken("IDENTIFIER", word, lineNum);
                continue;
            }

            throw new SyntaxException("Line " + lineNum + ": Unexpected character '" + c + "'");
        }
        return true;
    }

    private void preCheckStructure() throws SyntaxException {
        int openBraces = 0;
        for (int i = 0; i < tokenCount; i++) {
            if (tokens[i].value.equals("{")) openBraces++;
            if (tokens[i].value.equals("}")) openBraces--;
            if (openBraces < 0) throw new SyntaxException("Line " + tokens[i].line + ": Unexpected '}' without matching '{'.");
        }
        if (openBraces > 0) throw new SyntaxException("Code Structure Error: Missing " + openBraces + " closing brace(s) '}'.");
    }

    private void runInterpreter() throws SyntaxException {
        int i = 0;
        int safetyLimit = 0;

        while (i < tokenCount) {
            Token t = tokens[i];
            
            if (safetyLimit++ > 100000) throw new SyntaxException("Execution Halt: Infinite loop detected or program too long.");

            // === PRINT ===
            if (t.value.equals("print")) {
                ensureToken(i + 1);
                String output = "";
                Token next = tokens[i+1];
                
                if (next.type.equals("STRING")) {
                    output = next.value; 
                    i += 2;
                } else {
                    int val = resolveNumber(i + 1);
                    output = String.valueOf(val);
                    int stride = getStride(i + 1);
                    i += (1 + stride);
                }
                consoleArea.append(output + "\n");
            }

            // === MAKE ===
            else if (t.value.equals("make")) {
                ensureToken(i + 4);
                String type = tokens[i+1].value;
                String name = tokens[i+2].value;
                
                if (!tokens[i+2].type.equals("IDENTIFIER")) throw new SyntaxException("Line " + t.line + ": invalid variable name '" + name + "'.");
                if (!tokens[i+3].value.equals(":=")) throw new SyntaxException("Line " + t.line + ": Expected ':=' in variable declaration.");
                if (isKeyword(name)) throw new SyntaxException("Line " + t.line + ": Cannot use keyword '" + name + "' as variable name.");
                if (varExists(name)) throw new SyntaxException("Line " + t.line + ": Variable '" + name + "' is already defined.");

                if (type.equals("num")) {
                    int val = resolveNumber(i + 4);
                    saveScalar(name, val);
                    int stride = getStride(i + 4);
                    i += (4 + stride);
                } else if (type.equals("list")) {
                    if (!tokens[i+4].value.equals("[")) throw new SyntaxException("Line " + t.line + ": List must start with '['.");
                    int k = i + 5;
                    int[] buffer = new int[100];
                    int count = 0;
                    
                    while (k < tokenCount && !tokens[k].value.equals("]")) {
                        if (tokens[k].type.equals("NUMBER")) {
                            buffer[count++] = Integer.parseInt(tokens[k].value);
                        } else if (tokens[k].type.equals("SYMBOL") && tokens[k].value.equals(",")) {
                            // ignore commas
                        } else {
                            throw new SyntaxException("Line " + tokens[k].line + ": Invalid item in list. Only numbers allowed.");
                        }
                        k++;
                    }
                    if (k >= tokenCount) throw new SyntaxException("Line " + t.line + ": Unclosed list.");
                    
                    int[] finalArr = new int[count];
                    System.arraycopy(buffer, 0, finalArr, 0, count);
                    saveList(name, finalArr);
                    i = k + 1;
                } else {
                    throw new SyntaxException("Line " + t.line + ": Unknown type '" + type + "'. Use 'num' or 'list'.");
                }
            }

            // === IF STATEMENT ===
            else if (t.value.equals("if")) {
                ensureToken(i + 4);
                int offset = tokens[i+1].value.equals("(") ? 2 : 1; 
                
                int leftIndex = i + offset;
                int leftVal = resolveNumber(leftIndex);
                int leftStride = getStride(leftIndex); 

                int opIndex = leftIndex + leftStride;
                ensureToken(opIndex);
                String op = tokens[opIndex].value;

                int rightIndex = opIndex + 1;
                int rightVal = resolveNumber(rightIndex);
                int rightStride = getStride(rightIndex);

                int braceIndex = rightIndex + rightStride;
                ensureToken(braceIndex);
                if (tokens[braceIndex].value.equals(")")) braceIndex++; 
                
                if (!tokens[braceIndex].value.equals("{")) 
                    throw new SyntaxException("Line " + t.line + ": Missing '{' after if condition.");

                boolean cond = false;
                if (op.equals(">")) cond = leftVal > rightVal;
                else if (op.equals("<")) cond = leftVal < rightVal;
                else if (op.equals("==")) cond = leftVal == rightVal;
                else throw new SyntaxException("Line " + t.line + ": Unknown operator '" + op + "'");

                if (!cond) {
                    i = findBlockEnd(i);
                } else {
                    if (stackTop >= MAX_SCOPE_DEPTH) throw new SyntaxException("Stack Overflow: Nested too deep.");
                    scopeStack[stackTop++] = "IF";
                    i = braceIndex + 1;
                }
            }

            // === LOOP STATEMENT ===
            else if (t.value.equals("loop")) {
                ensureToken(i + 6);
                String varName = tokens[i+1].value;
                if(!tokens[i+2].value.equals("from")) throw new SyntaxException("Line " + t.line + ": Loop missing 'from'.");
                
                int fromIndex = i + 3;
                int startVal = resolveNumber(fromIndex);
                int fromStride = getStride(fromIndex);
                
                int toKwIndex = fromIndex + fromStride;
                ensureToken(toKwIndex);
                if(!tokens[toKwIndex].value.equals("to")) throw new SyntaxException("Line " + tokens[toKwIndex].line + ": Loop missing 'to'.");
                
                int toValIndex = toKwIndex + 1;
                int endVal = resolveNumber(toValIndex);
                int toStride = getStride(toValIndex);
                
                int braceIndex = toValIndex + toStride;
                ensureToken(braceIndex);
                if(!tokens[braceIndex].value.equals("{")) throw new SyntaxException("Line " + tokens[braceIndex].line + ": Loop missing '{'.");

                // Determine if we are starting a new loop or iterating an existing one
                boolean isNewLoop = (loopDepth == 0 || !loopVarStack[loopDepth-1].equals(varName) 
                                     || loopStartStack[loopDepth-1] != i);
                
                if (isNewLoop) {
                    // *** BUG FIX: THIS CHECK IS NOW INSIDE isNewLoop ***
                    // Only check for nesting conflict if we are actually starting a new loop layer
                    if (loopDepth > 0 && loopVarStack[loopDepth-1].equals(varName)) {
                        throw new SyntaxException("Line " + t.line + ": Cannot nest loop with same variable '" + varName + "'.");
                    }

                    if(varExists(varName)) {
                        if(!getVarType(varName).equals("NUM")) throw new SyntaxException("Line " + t.line + ": Loop variable '" + varName + "' must be a number.");
                    }
                    saveScalar(varName, startVal);
                    
                    loopStartStack[loopDepth] = i;
                    loopVarStack[loopDepth] = varName;
                    loopLimitStack[loopDepth] = endVal;
                    loopDepth++;
                    if (stackTop >= MAX_SCOPE_DEPTH) throw new SyntaxException("Stack Overflow: Loop nested too deep.");
                    scopeStack[stackTop++] = "LOOP";
                    i = braceIndex + 1;
                } else {
                    int currentVal = getScalar(varName);
                    if (currentVal >= endVal) {
                        loopDepth--;
                        i = findBlockEnd(i);
                    } else {
                        scopeStack[stackTop++] = "LOOP";
                        i = braceIndex + 1;
                    }
                }
            }

            // === CLOSING BRACE ===
            else if (t.value.equals("}")) {
                if (stackTop > 0) {
                    String scope = scopeStack[--stackTop];
                    if (scope.equals("IF")) {
                        i++;
                    } else if (scope.equals("LOOP")) {
                        int depth = loopDepth - 1;
                        if (depth < 0) throw new SyntaxException("Critical Error: Loop Stack corrupted.");
                        
                        String loopVar = loopVarStack[depth];
                        int currentVal = getScalar(loopVar);
                        saveScalar(loopVar, currentVal + 1);
                        i = loopStartStack[depth]; // Jump back to start of loop
                    }
                } else {
                    throw new SyntaxException("Line " + t.line + ": Unexpected '}'.");
                }
            }
            else {
                 if(t.type.equals("IDENTIFIER")) throw new SyntaxException("Line " + t.line + ": Unknown command '" + t.value + "'");
                i++;
            }
        }
    }

    // === HELPER METHODS ===

    private int getStride(int index) {
        if (index >= tokenCount) return 0;
        Token t = tokens[index];
        if (t.type.equals("IDENTIFIER")) {
            if (index + 1 < tokenCount && tokens[index+1].value.equals("[")) {
                return 4; // var, [, index, ]
            }
        }
        return 1;
    }

    private void ensureToken(int index) throws SyntaxException {
        if (index >= tokenCount) throw new SyntaxException("Unexpected end of code. Expected more tokens.");
    }

    private int findBlockEnd(int startIndex) {
        int index = startIndex;
        int braces = 0;
        while (index < tokenCount && !tokens[index].value.equals("{")) index++;
        if (index < tokenCount) { braces = 1; index++; }
        while (index < tokenCount && braces > 0) {
            if (tokens[index].value.equals("{")) braces++;
            if (tokens[index].value.equals("}")) braces--;
            index++;
        }
        return index;
    }

    private int resolveNumber(int index) throws SyntaxException {
        ensureToken(index);
        Token t = tokens[index];
        
        if (t.type.equals("NUMBER")) {
            try { return Integer.parseInt(t.value); } 
            catch (Exception e) { throw new SyntaxException("Line " + t.line + ": Number too large."); }
        }
        
        if (t.type.equals("IDENTIFIER")) {
            String name = t.value;
            if (index + 1 < tokenCount && tokens[index+1].value.equals("[")) {
                ensureToken(index + 3);
                String idxStr = tokens[index+2].value;
                int arrIndex;
                
                if (Character.isLetter(idxStr.charAt(0))) {
                     arrIndex = getScalar(idxStr);
                     if (arrIndex == Integer.MIN_VALUE) throw new SyntaxException("Line " + t.line + ": Undefined index variable '" + idxStr + "'.");
                } else {
                     try { arrIndex = Integer.parseInt(idxStr); }
                     catch(Exception e) { throw new SyntaxException("Line " + t.line + ": Invalid array index."); }
                }
                
                int val = getListValue(name, arrIndex);
                if (val == Integer.MIN_VALUE) {
                    if (!varExists(name)) throw new SyntaxException("Line " + t.line + ": Undefined list '" + name + "'.");
                    if (!getVarType(name).equals("LIST")) throw new SyntaxException("Line " + t.line + ": Variable '" + name + "' is not a list.");
                    throw new SyntaxException("Line " + t.line + ": Index [" + arrIndex + "] out of bounds for '" + name + "'.");
                }
                return val;
            } else {
                int val = getScalar(name);
                if (val == Integer.MIN_VALUE) {
                    if (!varExists(name)) throw new SyntaxException("Line " + t.line + ": Undefined variable '" + name + "'.");
                    if (getVarType(name).equals("LIST")) throw new SyntaxException("Line " + t.line + ": Cannot use list '" + name + "' as a number.");
                }
                return val;
            }
        }
        throw new SyntaxException("Line " + t.line + ": Expected a number or variable, found '" + t.value + "'");
    }

    private void addToken(String t, String v, int line) {
        if (tokenCount < MAX_TOKENS) {
            tokens[tokenCount] = new Token(t, v, line);
            tokenModel.addRow(new Object[]{line, t, v});
            tokenCount++;
        }
    }

    private boolean isKeyword(String w) {
        String[] k = {"make", "print", "if", "list", "num", "loop", "from", "to"};
        for (String s : k) if (s.equals(w)) return true;
        return false;
    }

    // === MEMORY HELPERS ===

    private boolean varExists(String name) {
        for(int k=0; k<varCount; k++) if(varNames[k].equals(name)) return true;
        return false;
    }
    
    private String getVarType(String name) {
        for(int k=0; k<varCount; k++) if(varNames[k].equals(name)) return varTypes[k];
        return "UNKNOWN";
    }

    private void saveScalar(String name, int val) {
        for(int k=0; k<varCount; k++) {
            if(varNames[k].equals(name)) { scalarMemory[k] = val; varTypes[k] = "NUM"; return; }
        }
        varNames[varCount] = name; varTypes[varCount] = "NUM"; scalarMemory[varCount] = val; varCount++;
    }

    private void saveList(String name, int[] arr) {
        varNames[varCount] = name; varTypes[varCount] = "LIST"; listMemory[varCount] = arr; varCount++;
    }

    private int getScalar(String name) {
        for(int k=0; k<varCount; k++) {
            if(varNames[k].equals(name) && varTypes[k].equals("NUM")) return scalarMemory[k];
        }
        return Integer.MIN_VALUE;
    }

    private int getListValue(String name, int index) {
        for(int k=0; k<varCount; k++) {
            if(varNames[k].equals(name) && varTypes[k].equals("LIST")) {
                if(index >= 0 && index < listMemory[k].length) return listMemory[k][index];
            }
        }
        return Integer.MIN_VALUE;
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
        int line; 
        public Token(String t, String v, int l) { type = t; value = v; line = l; }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> {
            artix frame = new artix();
            frame.setLogoImage("Logo.png");
            frame.setVisible(true);
        });
    }
}
