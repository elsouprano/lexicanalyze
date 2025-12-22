import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EnhancedLexerGUI extends JFrame {

    private JTextArea inputArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    public EnhancedLexerGUI() {
        // === WINDOW SETUP (Setup ng GUI) ===
        setTitle("Javai++ Tokenizer (ito na tala yun men)");
        setSize(750, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // === INPUT SECTION (Dito ita-type ang code) ===
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Source Code Input"));

        inputArea = new JTextArea(10, 40);
        // Sample text to test invalid adjacency
        inputArea.setText("int[] num nums = {1, 2}; // Error: Double identifier");
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JButton analyzeBtn = new JButton("Analyze / Tokenize");
        analyzeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        analyzeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runAnalysis(); // Tawagin ang main logic pag-click
            }
        });

        topPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        topPanel.add(analyzeBtn, BorderLayout.SOUTH);

        // === OUTPUT SECTION (Dito lalabas ang tokens) ===
        String[] columnNames = {"Token Type", "Value / Symbol"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Bawal i-edit ang table manually
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        resultTable.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultTable.setRowHeight(25);

        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Token Output"));

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    // Token class para sa storage ng Type at Value
    private static class Token {
        String type;
        String value;
        int position;

        Token(String type, String value, int position) {
            this.type = type;
            this.value = value;
            this.position = position;
        }
    }

    // === 1. INPUT VALIDATION (Structure check) ===
    private String validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Error: Input cannot be empty";
        }

        int parenCount = 0, bracketCount = 0, braceCount = 0;
        boolean inString = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == '"' && (i == 0 || input.charAt(i-1) != '\\')) {
                inString = !inString; 
                continue;
            }
            
            if (!inString) {
                if (c == '(') parenCount++;
                if (c == ')') parenCount--;
                if (c == '[') bracketCount++;
                if (c == ']') bracketCount--;
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
            }

            if (parenCount < 0) return "Error: Mismatched parentheses - closing ) without opening (";
            if (bracketCount < 0) return "Error: Mismatched brackets - closing ] without opening [";
            if (braceCount < 0) return "Error: Mismatched braces - closing } without opening {";
        }

        if (parenCount != 0) return "Error: Unbalanced parentheses ( )";
        if (bracketCount != 0) return "Error: Unbalanced square brackets [ ]";
        if (braceCount != 0) return "Error: Unbalanced curly braces { }";

        String trimmed = input.trim();
        if (!trimmed.endsWith(";") && !trimmed.endsWith("}")) {
             return "Error: Code snippet must generally end with a semicolon ; or brace }";
        }

        return null;
    }

    // === 2. SEMANTIC VALIDATION (Logic checking) ===
    private String validateTokenSequence(ArrayList<Token> tokens) {
        
        // === NEW CHECK: Strict Adjacency (Dito natin mahuhuli ang "num nums") ===
        String adjError = validateAdjacentTokens(tokens);
        if (adjError != null) return adjError;

        // Validate variable declaration
        String declError = validateDeclarations(tokens);
        if (declError != null) return declError;

        // Validate array syntax
        String arrayError = validateArraySyntax(tokens);
        if (arrayError != null) return arrayError;

        // Validate identifier naming rules
        String identError = validateIdentifiers(tokens);
        if (identError != null) return identError;

        // Validate operators
        String opError = validateOperators(tokens);
        if (opError != null) return opError;

        return null;
    }

    // === NEW METHOD: Validate Adjacent Tokens (Neighbor Check) ===
    private String validateAdjacentTokens(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token current = tokens.get(i);
            Token next = tokens.get(i + 1);

            // CASE 1: Identifier followed by Identifier (e.g., "num nums")
            // Ito yung fix sa "int[] num nums" -> Bawal magkadikit ang dalawang variable name
            if (current.type.equals("IDENTIFIER") && next.type.equals("IDENTIFIER")) {
                return "Error: Unexpected identifier '" + next.value + "' after '" + current.value + "'. Missing operator?";
            }

            // CASE 2: Identifier followed by Number/String (e.g., "x 5")
            // Bawal ang "x 5", dapat "x = 5"
            if (current.type.equals("IDENTIFIER") && 
               (next.type.equals("NUMBER") || next.type.equals("STRING_LITERAL"))) {
                return "Error: Missing operator between '" + current.value + "' and value '" + next.value + "'";
            }

            // CASE 3: Identifier followed by Keyword (e.g., "x int")
            // Bawal ang "myVar int" -> exception lang ay 'instanceof' pero di natin covered yun dito
            if (current.type.equals("IDENTIFIER") && next.type.equals("KEYWORD")) {
                return "Error: Unexpected keyword '" + next.value + "' after identifier '" + current.value + "'";
            }

            // CASE 4: Double Type Declaration (e.g., "int boolean")
            // Bawal ang dalawang type na magkatabi
            if (current.type.equals("KEYWORD") && next.type.equals("KEYWORD")) {
                if (isTypeKeyword(current.value) && isTypeKeyword(next.value)) {
                    return "Error: Invalid syntax. Cannot have two types '" + current.value + " " + next.value + "' together.";
                }
            }
        }
        return null;
    }

    // Validation para sa Variable Declarations
    private String validateDeclarations(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            
            if (t.type.equals("KEYWORD") && isTypeKeyword(t.value)) {
                if (i + 1 >= tokens.size()) {
                    return "Error: Incomplete declaration after '" + t.value + "'";
                }
                Token next = tokens.get(i + 1);
                
                // Array declaration (e.g., int[])
                if (next.type.equals("L_BRACKET")) {
                    if (i + 2 >= tokens.size() || !tokens.get(i + 2).type.equals("R_BRACKET")) {
                        return "Error: Array brackets must be closed '[]' after type '" + t.value + "'";
                    }
                    if (i + 3 >= tokens.size() || !tokens.get(i + 3).type.equals("IDENTIFIER")) {
                        return "Error: Expected variable name after '" + t.value + "[]'";
                    }
                } 
                // Regular declaration check
                else if (!next.type.equals("IDENTIFIER")) {
                    return "Error: Expected variable name after type '" + t.value + "', found '" + next.value + "'";
                }
            }
        }
        return null;
    }

    // Validation para sa Array values
    private String validateArraySyntax(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.type.equals("L_BRACE")) {
                int j = i + 1;
                boolean expectingValue = true;
                boolean expectingComma = false;

                while (j < tokens.size() && !tokens.get(j).type.equals("R_BRACE")) {
                    Token current = tokens.get(j);

                    if (expectingValue) {
                        if (!current.type.equals("NUMBER") && 
                            !current.type.equals("IDENTIFIER") &&
                            !current.type.equals("STRING_LITERAL")) {
                            return "Error: Expected a value in array, found " + 
                                   current.type + " '" + current.value + "'";
                        }
                        expectingValue = false;
                        expectingComma = true;
                    } else if (expectingComma) {
                        if (current.type.equals("COMMA")) {
                            expectingValue = true;
                            expectingComma = false;
                        } else {
                            return "Error: Missing comma between array elements.";
                        }
                    }
                    j++;
                }
            }
        }
        return null;
    }

    // Validation para sa pangalan ng variables
    private String validateIdentifiers(ArrayList<Token> tokens) {
        Set<String> reservedWords = getReservedWords();
        for (Token t : tokens) {
            if (t.type.equals("IDENTIFIER")) {
                String name = t.value;
                if (reservedWords.contains(name.toLowerCase())) {
                    return "Error: '" + name + "' is a reserved keyword";
                }
                if (!name.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
                    return "Error: Invalid identifier format '" + name + "'";
                }
            }
        }
        return null;
    }

    // Validation para sa Operators
    private String validateOperators(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (i == 0 && isOperator(t.type) && !isUnaryOperator(t.type)) {
                return "Error: Statement cannot start with operator '" + t.value + "'";
            }
            if (i < tokens.size() - 1 && isOperator(t.type)) {
                Token next = tokens.get(i + 1);
                if (isOperator(next.type) && !isValidOperatorSequence(t.value, next.value)) {
                    return "Error: Invalid operator sequence '" + t.value + next.value + "'";
                }
            }
        }
        return null;
    }

    // === HELPER METHODS ===
    private boolean isTypeKeyword(String word) {
        String[] types = {"int", "double", "float", "char", "boolean", "byte", 
                         "short", "long", "String", "void"};
        for (String type : types) if (word.equals(type)) return true;
        return false;
    }

    private boolean isOperator(String type) {
        return type.contains("ASSIGN") || type.contains("PLUS") || type.contains("MINUS") ||
               type.contains("MULTIPLY") || type.contains("DIVIDE") || type.contains("MODULO") ||
               type.contains("EQUALS") || type.contains("LESS") || type.contains("GREATER") ||
               type.contains("LOGICAL") || type.contains("BITWISE");
    }

    private boolean isUnaryOperator(String type) {
        return type.equals("LOGICAL_NOT") || type.equals("MINUS") || type.equals("PLUS");
    }

    private boolean isValidOperatorSequence(String op1, String op2) {
        return (op2.equals("!") || op2.equals("-") || op2.equals("+"));
    }

    private Set<String> getReservedWords() {
        Set<String> reserved = new HashSet<>();
        String[] words = {
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", 
            "continue", "default", "do", "double", "else", "enum", "extends", "final", 
            "finally", "float", "for", "if", "implements", "import", "instanceof", "int", 
            "interface", "long", "new", "package", "private", "protected", "public", 
            "return", "short", "static", "super", "switch", "this", "throw", "throws", 
            "try", "void", "while", "true", "false", "null", "var", "String"
        };
        for (String w : words) reserved.add(w);
        return reserved;
    }

    // === 3. CORE LEXER ENGINE ===
    private void runAnalysis() {
        tableModel.setRowCount(0); 

        String input = inputArea.getText();
        
        String validationError = validateInput(input);
        if (validationError != null) {
            addTokenToTable("VALIDATION ERROR", validationError);
            JOptionPane.showMessageDialog(this, validationError, "Syntax Error", JOptionPane.WARNING_MESSAGE);
        }

        ArrayList<Token> tokens = new ArrayList<>();
        int length = input.length();
        int i = 0;

        while (i < length) {
            char c = input.charAt(i);

            // 1. SKIP WHITESPACE
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // 2. HANDLE COMMENTS
            if (c == '/' && i + 1 < length && input.charAt(i + 1) == '/') {
                i += 2; 
                while (i < length && input.charAt(i) != '\n' && input.charAt(i) != '\r') {
                    i++;
                }
                continue;
            }

            // 3. HANDLE STRING LITERALS
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                sb.append(c);
                int startPos = i;
                i++; 
                while (i < length) {
                    char nextC = input.charAt(i);
                    sb.append(nextC);
                    i++;
                    if (nextC == '"' && input.charAt(i-2) != '\\') {
                        break; 
                    }
                }
                tokens.add(new Token("STRING_LITERAL", sb.toString(), startPos));
                continue;
            }

            // 4. HANDLE SINGLE CHAR SYMBOLS
            if (isSingleCharSymbol(c)) {
                String type = getSymbolType(c);
                tokens.add(new Token(type, Character.toString(c), i));
                i++;
                continue;
            }

            // 5. HANDLE NUMBERS
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                int startPos = i;
                boolean hasDecimal = false;
                
                while (i < length && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                    if (input.charAt(i) == '.') {
                        if (hasDecimal) break; 
                        hasDecimal = true;
                    }
                    sb.append(input.charAt(i));
                    i++;
                }

                // Guard: Check for "2int"
                if (i < length && (Character.isLetter(input.charAt(i)) || input.charAt(i) == '_')) {
                     addTokenToTable("LEXICAL ERROR", "Invalid Identifier starting with digit: " + sb.toString() + input.charAt(i) + "...");
                     JOptionPane.showMessageDialog(this, 
                        "Lexical Error: Identifiers cannot start with numbers (found '" + sb.toString() + input.charAt(i) + "...')", 
                        "Invalid Token", 
                        JOptionPane.ERROR_MESSAGE);
                     return;
                }
                
                tokens.add(new Token("NUMBER", sb.toString(), startPos));
                continue;
            }

            // 6. HANDLE OPERATORS
            if (isOperatorChar(c)) {
                StringBuilder sb = new StringBuilder();
                int startPos = i;
                while (i < length && isOperatorChar(input.charAt(i))) {
                    if (input.charAt(i) == '/' && i + 1 < length && input.charAt(i+1) == '/') {
                        break; 
                    }
                    sb.append(input.charAt(i));
                    i++;
                }
                String op = sb.toString();
                tokens.add(new Token(getOperatorType(op), op, startPos));
                continue;
            }

            // 7. HANDLE IDENTIFIERS AND KEYWORDS
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                int startPos = i;
                while (i < length && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    sb.append(input.charAt(i));
                    i++;
                }
                String word = sb.toString();

                // Guard: Check for "int2"
                for (String k : getReservedWords()) {
                    if (word.startsWith(k) && word.length() > k.length()) {
                         String suffix = word.substring(k.length());
                         if (Character.isDigit(suffix.charAt(0))) {
                             addTokenToTable("LEXICAL ERROR", "Invalid Keyword format: '" + word + "'");
                             JOptionPane.showMessageDialog(this, 
                                "Lexical Error: Keywords cannot be followed by numbers (Found: '" + word + "')", 
                                "Invalid Token", 
                                JOptionPane.ERROR_MESSAGE);
                             return; 
                         }
                    }
                }

                if (isKeyword(word)) {
                    tokens.add(new Token("KEYWORD", word, startPos));
                } else {
                    tokens.add(new Token("IDENTIFIER", word, startPos));
                }
                continue;
            }

            // 8. UNKNOWN
            addTokenToTable("UNKNOWN", Character.toString(c));
            i++;
        }

        // Semantic Check
        String semanticError = validateTokenSequence(tokens);
        if (semanticError != null) {
            addTokenToTable("SEMANTIC ERROR", semanticError);
            JOptionPane.showMessageDialog(this, semanticError, "Semantic Error", JOptionPane.ERROR_MESSAGE);
            return; // <--- FIX: STOP EXECUTION HERE IF ERROR FOUND
        }

        for (Token token : tokens) {
            addTokenToTable(token.type, token.value);
        }
    }

    private void addTokenToTable(String type, String value) {
        tableModel.addRow(new Object[]{type, value});
    }

    private boolean isSingleCharSymbol(char c) {
        return "[]{},;().".indexOf(c) != -1;
    }

    private String getSymbolType(char c) {
        switch (c) {
            case '[': return "L_BRACKET";
            case ']': return "R_BRACKET";
            case '{': return "L_BRACE";
            case '}': return "R_BRACE";
            case '(': return "L_PAREN";
            case ')': return "R_PAREN";
            case ',': return "COMMA";
            case ';': return "SEMICOLON";
            case '.': return "DOT";
            default: return "SYMBOL";
        }
    }

    private boolean isOperatorChar(char c) {
        return "=+-*/%<>!&|:".indexOf(c) != -1;
    }

    private String getOperatorType(String op) {
        switch (op) {
            case "=": return "ASSIGN";
            case "==": return "EQUALS";
            case "!=": return "NOT_EQUALS";
            case "+": return "PLUS";
            case "-": return "MINUS";
            case "*": return "MULTIPLY";
            case "/": return "DIVIDE";
            case "%": return "MODULO";
            case "<": return "LESS_THAN";
            case ">": return "GREATER_THAN";
            case "<=": return "LESS_EQUAL";
            case ">=": return "GREATER_EQUAL";
            case "&&": return "LOGICAL_AND";
            case "||": return "LOGICAL_OR";
            case "!": return "LOGICAL_NOT";
            case "++": return "INCREMENT";
            case "--": return "DECREMENT";
            case "+=": return "ADD_ASSIGN";
            case "-=": return "SUB_ASSIGN";
            default: return "OPERATOR";
        }
    }

    private boolean isKeyword(String word) {
        String[] keywords = {
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", 
            "continue", "default", "do", "double", "else", "enum", "extends", "final", 
            "finally", "float", "for", "if", "implements", "import", "instanceof", "int", 
            "interface", "long", "new", "package", "private", "protected", "public", 
            "return", "short", "static", "super", "switch", "this", "throw", "throws", 
            "try", "void", "while", "true", "false", "null", "var", "String"
        };
        for (String k : keywords) if (word.equals(k)) return true;
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EnhancedLexerGUI().setVisible(true);
        });
    }
}
