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
    private JPanel logoContainer; // Container for your full logo+text

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

    public artix() {
        setTitle("Artix");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // === TOP HEADER BAR ===
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(Color.WHITE);
        headerBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 200)));
        headerBar.setPreferredSize(new Dimension(1200, 70));

        // Logo Container (Center of header) - YOU PUT YOUR FULL LOGO HERE
        logoContainer = new JPanel();
        logoContainer.setBackground(Color.WHITE);
        logoContainer.setPreferredSize(new Dimension(300, 60));
        logoContainer.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

        // Wrapper to center the logo
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerWrapper.setBackground(Color.WHITE);
        centerWrapper.add(logoContainer);

        headerBar.add(centerWrapper, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        toolbar.setBackground(new Color(175, 238, 238));
        toolbar.setPreferredSize(new Dimension(1200, 35)); // Original height preserved
        toolbar.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(150, 200, 200)));
        
        JButton runBtn = new JButton("Run");
        runBtn.setPreferredSize(new Dimension(90, 25));
        runBtn.setBackground(new Color(11, 83, 148));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        runBtn.setFocusPainted(false);
        runBtn.setBorderPainted(false);
        runBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        runBtn.addActionListener(e -> executeProgram());
        
        toolbar.add(runBtn);

        // === TOOLBAR (Light cyan bar below header) ===
        //JPanel toolbar = new JPanel();
        //toolbar.setBackground(new Color(175, 238, 238));
        //toolbar.setPreferredSize(new Dimension(1200, 35));
        //toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(150, 200, 200)));

        // === COMBINED HEADER PANEL ===
        JPanel topPanel = new JPanel(new BorderLayout(0, 0));
        topPanel.add(headerBar, BorderLayout.NORTH);
        topPanel.add(toolbar, BorderLayout.SOUTH);

        // === MAIN CONTENT AREA (Blue gradient) ===
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(new Color(108, 166, 205));
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Left Panel - Source Code
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(new Color(108, 166, 205));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 140, 180), 2),
            "Source Code",
            0, 0,
            new Font("SansSerif", Font.BOLD, 14),
            Color.WHITE
        ));
        
        inputArea = new JTextArea(15, 40);
        inputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputArea.setBackground(new Color(250, 250, 250));
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

        // Right Panel - Tables
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        rightPanel.setBackground(new Color(108, 166, 205));
        rightPanel.setPreferredSize(new Dimension(350, 0));
        
        tokenModel = new DefaultTableModel(new String[]{"Type", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        memoryModel = new DefaultTableModel(new String[]{"Var Name", "Type", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable tokenTable = new JTable(tokenModel);
        JTable memoryTable = new JTable(memoryModel);
        
        JScrollPane tokenScroll = new JScrollPane(tokenTable);
        JScrollPane memoryScroll = new JScrollPane(memoryTable);
        
        tokenScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 140, 180), 2),
            "Tokens",
            0, 0,
            new Font("SansSerif", Font.BOLD, 14),
            Color.BLACK
        ));
        
        memoryScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 140, 180), 2),
            "Memory",
            0, 0,
            new Font("SansSerif", Font.BOLD, 14),
            Color.BLACK
        ));
        
        rightPanel.add(tokenScroll);
        rightPanel.add(memoryScroll);

        mainContent.add(leftPanel, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);

        // === BOTTOM CONSOLE (Darker blue) ===
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(new Color(90, 140, 180));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        consolePanel.setPreferredSize(new Dimension(1200, 200));
        
        consoleArea = new JTextArea(8, 30);
        consoleArea.setBackground(new Color(90, 140, 180));
        consoleArea.setForeground(Color.WHITE);
        consoleArea.setFont(new Font("Monospaced", Font.BOLD, 13));
        consoleArea.setEditable(false);
        consoleArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createLineBorder(new Color(70, 120, 160), 2));
        consoleScroll.setBackground(new Color(90, 140, 180));
        
        // Console toolbar with minimize/close buttons
        JPanel consoleToolbar = new JPanel(new BorderLayout());
        consoleToolbar.setBackground(new Color(90, 140, 180));
        consoleToolbar.setPreferredSize(new Dimension(1200, 35));
        
        JLabel consoleTitle = new JLabel("Console Output");
        consoleTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        consoleTitle.setForeground(Color.WHITE);
        consoleTitle.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        
        JPanel consoleButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        consoleButtons.setBackground(new Color(90, 140, 180));
        
        JButton minimizeBtn = new JButton("─");
        JButton closeBtn = new JButton("✕");
        
        for (JButton btn : new JButton[]{minimizeBtn, closeBtn}) {
            btn.setPreferredSize(new Dimension(35, 25));
            btn.setBackground(new Color(70, 120, 160));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            consoleButtons.add(btn);
        }
        
        consoleToolbar.add(consoleTitle, BorderLayout.WEST);
        consoleToolbar.add(consoleButtons, BorderLayout.EAST);
        
        consolePanel.add(consoleToolbar, BorderLayout.NORTH);
        consolePanel.add(consoleScroll, BorderLayout.CENTER);

        // === ADD ALL COMPONENTS ===
        add(topPanel, BorderLayout.NORTH);
        add(mainContent, BorderLayout.CENTER);
        add(consolePanel, BorderLayout.SOUTH);
    }

    // Method to add your logo - call this from main() or constructor
    public void setLogoImage(String imagePath) {
        logoContainer.removeAll();
        ImageIcon originalIcon = new ImageIcon(imagePath);
        Image originalImage = originalIcon.getImage();

        // The image is now exactly 300x60 coming from Figma, so this
        // scales it 1:1 with no distortion.
        Image scaledImage = originalImage.getScaledInstance(300, 60, Image.SCALE_SMOOTH);

        JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
        logoContainer.add(logoLabel);
        logoContainer.revalidate();
        logoContainer.repaint();
    }

    private void executeProgram() {
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

    private void runInterpreter() {
        int i = 0;
        while (i < tokenCount) {
            Token t = tokens[i];

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
                    i = findBlockEnd(i);
                } else {
                    scopeStack[stackTop++] = "IF";
                    while (i < tokenCount && !tokens[i].value.equals("{")) i++;
                    i++; 
                }
            }

            else if (t.value.equals("loop")) {
                String varName = tokens[i+1].value;
                int startVal = resolveNumber(i+3);
                int endVal = resolveNumber(i+5);
                
                boolean isNewLoop = (loopDepth == 0 || !loopVarStack[loopDepth-1].equals(varName) 
                                     || loopStartStack[loopDepth-1] != i);
                
                if (isNewLoop) {
                    saveScalar(varName, startVal);
                    loopStartStack[loopDepth] = i;
                    loopVarStack[loopDepth] = varName;
                    loopLimitStack[loopDepth] = endVal;
                    loopDepth++;
                    scopeStack[stackTop++] = "LOOP";
                    while (i < tokenCount && !tokens[i].value.equals("{")) i++;
                    i++;
                } else {
                    int currentVal = getScalar(varName);
                    if (currentVal > endVal) {
                        loopDepth--;
                        i = findBlockEnd(i);
                    } else {
                        scopeStack[stackTop++] = "LOOP";
                        while (i < tokenCount && !tokens[i].value.equals("{")) i++;
                        i++;
                    }
                }
            }

            else if (t.value.equals("}")) {
                if (stackTop > 0) {
                    String currentScope = scopeStack[--stackTop];
                    if (currentScope.equals("IF")) {
                        i++;
                    } 
                    else if (currentScope.equals("LOOP")) {
                        int depth = loopDepth - 1;
                        String loopVar = loopVarStack[depth];
                        int currentVal = getScalar(loopVar);
                        saveScalar(loopVar, currentVal + 1);
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

    private int findBlockEnd(int startIndex) {
        int index = startIndex;
        int braces = 0;
        while (index < tokenCount && !tokens[index].value.equals("{")) {
            index++;
        }
        if (index < tokenCount) {
            braces = 1;
            index++;
        }
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
        SwingUtilities.invokeLater(() -> {
            artix frame = new artix();
            frame.setLogoImage("Logo.png");
            frame.setVisible(true);
        });
    }
}
