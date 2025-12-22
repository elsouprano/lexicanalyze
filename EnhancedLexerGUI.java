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
        // === WINDOW SETUP ===
        setTitle("Javai++ Tokenizer");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // === INPUT SECTION (TOP) ===
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Source Code Input"));

        inputArea = new JTextArea(8, 40);
        inputArea.setText("int[] squared =\n    Arrays.stream(nums)\n          .map(n : n * n)\n          .toArray();");
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JButton analyzeBtn = new JButton("Analyze / Tokenize");
        analyzeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runAnalysis();
            }
        });

        topPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        topPanel.add(analyzeBtn, BorderLayout.SOUTH);

        // === OUTPUT SECTION (CENTER) ===
        String[] columnNames = {"Token Type", "Value / Symbol"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultTable.setRowHeight(25);

        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Token Output"));

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    // Token class to store type and value
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

    // === COMPREHENSIVE INPUT VALIDATION ===
    private String validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Error: Input cannot be empty";
        }

        // Check for balanced parentheses, brackets, and braces
        int parenCount = 0, bracketCount = 0, braceCount = 0;
        for (char c : input.toCharArray()) {
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
            if (c == '[') bracketCount++;
            if (c == ']') bracketCount--;
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;

            if (parenCount < 0) return "Error: Mismatched parentheses - closing ) without opening (";
            if (bracketCount < 0) return "Error: Mismatched brackets - closing ] without opening [";
            if (braceCount < 0) return "Error: Mismatched braces - closing } without opening {";
        }

        if (parenCount != 0) return "Error: Unbalanced parentheses ( )";
        if (bracketCount != 0) return "Error: Unbalanced square brackets [ ]";
        if (braceCount != 0) return "Error: Unbalanced curly braces { }";

        // Must end with semicolon
        if (!input.trim().endsWith(";")) {
            return "Error: Statement must end with a semicolon ;";
        }

        // Check for invalid character sequences
        String error = checkInvalidPatterns(input);
        if (error != null) return error;

        return null;
    }

    // Check for common invalid patterns
    private String checkInvalidPatterns(String input) {
        // Check for numbers immediately after identifiers (e.g., int[]2, var123abc)
        for (int i = 0; i < input.length() - 1; i++) {
            char curr = input.charAt(i);
            char next = input.charAt(i + 1);
            
            // Check for ]followed by digit (e.g., int[]2)
            if (curr == ']' && Character.isDigit(next)) {
                return "Error: Invalid syntax - digit cannot follow ']' directly (found ']" + next + "')";
            }
            
            // Check for multiple dots in sequence (e.g., ..)
            if (curr == '.' && next == '.' && (i == 0 || !Character.isDigit(input.charAt(i-1)))) {
                return "Error: Invalid syntax - multiple consecutive dots '..'";
            }
            
            // Check for invalid operator combinations
            if (curr == '=' && next == '=' && i + 2 < input.length() && input.charAt(i + 2) == '=') {
                return "Error: Invalid operator '===' - Java uses '==' for equality";
            }
        }
        
        // Check for identifiers that mix letters and numbers incorrectly (e.g., int2, var3x)
        String[] tokens = input.split("[\\s\\[\\]\\{\\}\\(\\),;.=+\\-*/%<>!&|:]+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            
            // Check if token starts with digit but contains letters (invalid identifier)
            if (token.length() > 0 && Character.isDigit(token.charAt(0))) {
                for (int i = 1; i < token.length(); i++) {
                    if (Character.isLetter(token.charAt(i))) {
                        return "Error: Invalid identifier '" + token + "' - identifiers cannot start with a digit";
                    }
                }
            }
            
            // Check for Java keywords followed by digits (e.g., int2, String5)
            if (token.length() > 0) {
                String prefix = "";
                int digitStartIndex = -1;
                
                // Find where digits start in the token
                for (int i = 0; i < token.length(); i++) {
                    if (Character.isDigit(token.charAt(i))) {
                        prefix = token.substring(0, i);
                        digitStartIndex = i;
                        break;
                    }
                }
                
                // If we found digits and the prefix (yung dulo) is a keyword
                if (digitStartIndex > 0 && isKeyword(prefix)) {
                    return "Error: Invalid type name '" + token + "' - Java keywords cannot be combined with numbers";
                }
            }
        }
        
        return null;
    }

    // === ENHANCED SEMANTIC VALIDATION ===
    private String validateTokenSequence(ArrayList<Token> tokens) {
        // Remove whitespace and indent tokens for validation
        ArrayList<Token> cleanTokens = new ArrayList<>();
        for (Token t : tokens) {
            if (!t.type.equals("WHITESPACE") && !t.type.equals("INDENT DELIMITER")) {
                cleanTokens.add(t);
            }
        }

        // Validate variable declaration syntax
        String declError = validateDeclarations(cleanTokens);
        if (declError != null) return declError;

        // Validate array syntax
        String arrayError = validateArraySyntax(cleanTokens);
        if (arrayError != null) return arrayError;

        // Validate identifier naming rules
        String identError = validateIdentifiers(cleanTokens);
        if (identError != null) return identError;

        // Validate operator usage
        String opError = validateOperators(cleanTokens);
        if (opError != null) return opError;

        // Validate method calls and chaining
        String methodError = validateMethodCalls(cleanTokens);
        if (methodError != null) return methodError;

        return null;
    }

    // Validate variable declarations
    private String validateDeclarations(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            
            // Check for type declarations (int, String, etc.)
            if (t.type.equals("KEYWORD") && isTypeKeyword(t.value)) {
                // Next token should be [] or identifier
                if (i + 1 >= tokens.size()) {
                    return "Error: Incomplete declaration after '" + t.value + "'";
                }
                
                Token next = tokens.get(i + 1);
                
                // Handle array declaration (e.g., int[])
                if (next.type.equals("L_BRACKET")) {
                    if (i + 2 >= tokens.size() || !tokens.get(i + 2).type.equals("R_BRACKET")) {
                        return "Error: Array brackets must be closed '[]' after type '" + t.value + "'";
                    }
                    
                    // After [], must be an identifier
                    if (i + 3 >= tokens.size() || !tokens.get(i + 3).type.equals("IDENTIFIER")) {
                        return "Error: Expected variable name after '" + t.value + "[]'";
                    }
                    
                    // Check what comes after identifier
                    if (i + 4 < tokens.size()) {
                        Token afterIdent = tokens.get(i + 4);
                        if (afterIdent.type.equals("NUMBER")) {
                            return "Error: Invalid syntax - number '" + afterIdent.value + 
                                   "' cannot follow variable name '" + tokens.get(i + 3).value + "'";
                        }
                        if (!afterIdent.type.equals("ASSIGN") && !afterIdent.type.equals("SEMICOLON") && 
                            !afterIdent.type.equals("COMMA")) {
                            return "Error: Expected '=', ',', or ';' after variable name '" + 
                                   tokens.get(i + 3).value + "', found " + afterIdent.type;
                        }
                    }
                } else if (next.type.equals("IDENTIFIER")) {
                    // Regular variable declaration (e.g., int x)
                    // Check what comes after identifier
                    if (i + 2 < tokens.size()) {
                        Token afterIdent = tokens.get(i + 2);
                        if (afterIdent.type.equals("NUMBER")) {
                            return "Error: Invalid syntax - number '" + afterIdent.value + 
                                   "' cannot follow variable name '" + next.value + "'";
                        }
                    }
                } else {
                    return "Error: Expected variable name or '[]' after type '" + t.value + "'";
                }
            }
        }
        return null;
    }

    // Validate array initialization and access
    private String validateArraySyntax(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            // Array initialization with braces
            if (t.type.equals("L_BRACE")) {
                int j = i + 1;
                boolean expectingValue = true;
                boolean expectingComma = false;
                boolean isEmpty = false;

                if (j < tokens.size() && tokens.get(j).type.equals("R_BRACE")) {
                    isEmpty = true;
                }

                while (j < tokens.size() && !tokens.get(j).type.equals("R_BRACE")) {
                    Token current = tokens.get(j);

                    if (expectingValue) {
                        if (!current.type.equals("NUMBER") && 
                            !current.type.equals("IDENTIFIER") &&
                            !current.type.equals("STRING")) {
                            return "Error: Expected a value in array initialization, found " + 
                                   current.type + " '" + current.value + "'";
                        }
                        expectingValue = false;
                        expectingComma = true;
                    } else if (expectingComma) {
                        if (current.type.equals("COMMA")) {
                            expectingValue = true;
                            expectingComma = false;
                        } else {
                            return "Error: Missing comma between array elements. Expected ',' after '" + 
                                   tokens.get(j - 1).value + "', found " + current.type + " '" + current.value + "'";
                        }
                    }
                    j++;
                }

                if (!isEmpty && expectingValue) {
                    return "Error: Trailing comma in array initialization - expected value after last comma";
                }
            }
        }
        return null;
    }

    // Validate identifier naming rules
    private String validateIdentifiers(ArrayList<Token> tokens) {
        Set<String> reservedWords = getReservedWords();
        
        for (Token t : tokens) {
            if (t.type.equals("IDENTIFIER")) {
                String name = t.value;
                
                // Check if identifier is a reserved word
                if (reservedWords.contains(name.toLowerCase())) {
                    return "Error: '" + name + "' is a reserved keyword and cannot be used as an identifier";
                }
                
                // Check for invalid characters (already handled by lexer, but double-check)
                if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    return "Error: Invalid identifier '" + name + "' - must start with letter or underscore";
                }
                
                // Check for identifiers that are too similar to keywords (common mistake)
                if (name.equalsIgnoreCase("Int") || name.equalsIgnoreCase("Void")) {
                    return "Error: Identifier '" + name + "' is too similar to keyword (keywords are lowercase in Java)";
                }
            }
        }
        return null;
    }

    // Validate operator usage and positioning
    private String validateOperators(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            
            // Check for operator at start (except unary operators)
            if (i == 0 && isOperator(t.type) && !isUnaryOperator(t.type)) {
                return "Error: Statement cannot start with operator '" + t.value + "'";
            }
            
            // Check for consecutive operators (except valid combinations)
            if (i < tokens.size() - 1 && isOperator(t.type)) {
                Token next = tokens.get(i + 1);
                if (isOperator(next.type) && !isValidOperatorSequence(t.value, next.value)) {
                    return "Error: Invalid operator sequence '" + t.value + next.value + "'";
                }
            }
            
            // Check for operator before semicolon
            if (t.type.equals("SEMICOLON") && i > 0) {
                Token prev = tokens.get(i - 1);
                if (isOperator(prev.type) && !prev.type.equals("R_PAREN") && !prev.type.equals("R_BRACKET")) {
                    return "Error: Statement cannot end with operator '" + prev.value + "' before semicolon";
                }
            }
        }
        return null;
    }

    // Validate method calls and dot notation
    private String validateMethodCalls(ArrayList<Token> tokens) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token t = tokens.get(i);
            
            // Check for dot notation
            if (t.type.equals("DOT")) {
                // Dot must have something before it
                if (i == 0) {
                    return "Error: '.' cannot appear at the start of a statement";
                }
                
                Token prev = tokens.get(i - 1);
                if (!prev.type.equals("IDENTIFIER") && !prev.type.equals("R_PAREN") && !prev.type.equals("R_BRACKET")) {
                    return "Error: Invalid token before '.' - expected identifier or method call";
                }
                
                // Dot must be followed by identifier or keyword (for method names)
                if (i + 1 >= tokens.size()) {
                    return "Error: Statement cannot end with '.'";
                }
                
                Token next = tokens.get(i + 1);
                if (!next.type.equals("IDENTIFIER") && !next.type.equals("KEYWORD")) {
                    return "Error: Expected method or field name after '.', found " + next.type + " '" + next.value + "'";
                }
            }
            
            // Check method calls (identifier followed by parenthesis)
            if (t.type.equals("IDENTIFIER") && i + 1 < tokens.size()) {
                Token next = tokens.get(i + 1);
                if (next.type.equals("L_PAREN")) {
                    // This is a method call - validate parentheses content
                    int parenDepth = 1;
                    int j = i + 2;
                    boolean hasContent = false;
                    
                    while (j < tokens.size() && parenDepth > 0) {
                        Token curr = tokens.get(j);
                        if (curr.type.equals("L_PAREN")) parenDepth++;
                        if (curr.type.equals("R_PAREN")) parenDepth--;
                        if (parenDepth > 0 && !curr.type.equals("L_PAREN")) hasContent = true;
                        j++;
                    }
                }
            }
        }
        return null;
    }

    // Helper methods
    private boolean isTypeKeyword(String word) {
        String[] types = {"int", "double", "float", "char", "boolean", "byte", 
                         "short", "long", "String", "void"};
        for (String type : types) {
            if (word.equals(type)) return true;
        }
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
        // Allow unary operators after binary operators
        return (op2.equals("!") || op2.equals("-") || op2.equals("+"));
    }

    private Set<String> getReservedWords() {
        Set<String> reserved = new HashSet<>();
        String[] words = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null", "var"
        };
        for (String word : words) {
            reserved.add(word);
        }
        return reserved;
    }

    // === ENHANCED LEXICAL ANALYZER ===
    private void runAnalysis() {
        tableModel.setRowCount(0);

        String input = inputArea.getText();

        // Validate input syntax
        String validationError = validateInput(input);
        if (validationError != null) {
            addTokenToTable("VALIDATION ERROR", validationError);
            JOptionPane.showMessageDialog(this, 
                validationError, 
                "Invalid Input", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Tokenize the input
        ArrayList<Token> tokens = new ArrayList<>();
        int length = input.length();
        int i = 0;
        boolean atLineStart = true;

        while (i < length) {
            char c = input.charAt(i);

            // Handle indentation at the start of each line
            if (atLineStart && (c == ' ' || c == '\t')) {
                tokens.add(new Token("INDENT DELIMITER", "  ", i));

                while (i < length && (input.charAt(i) == ' ' || input.charAt(i) == '\t')) {
                    i++;
                }

                if (i < length && input.charAt(i) != '\n' && input.charAt(i) != '\r') {
                    atLineStart = false;
                }
                continue;
            }

            // Handle newlines
            if (c == '\n' || c == '\r') {
                atLineStart = true;
                i++;
                if (c == '\r' && i < length && input.charAt(i) == '\n') {
                    i++;
                }
                continue;
            }

            // Handle whitespace
            if (c == ' ' || c == '\t') {
                tokens.add(new Token("WHITESPACE", " ", i));
                while (i < length && (input.charAt(i) == ' ' || input.charAt(i) == '\t')) {
                    i++;
                }
                continue;
            }

            atLineStart = false;

            // Handle single-character symbols
            if (isSingleCharSymbol(c)) {
                String type = getSymbolType(c);
                tokens.add(new Token(type, Character.toString(c), i));
                i++; 
                continue;
            }

            // Handle numbers
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                int startPos = i;
                boolean hasDecimal = false;
                
                while (i < length && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                    if (input.charAt(i) == '.') {
                        // Check if next char is also a dot or a letter (invalid)
                        if (i + 1 < length && (input.charAt(i + 1) == '.' || Character.isLetter(input.charAt(i + 1)))) {
                            break;
                        }
                        if (hasDecimal) break;
                        hasDecimal = true;
                    }
                    sb.append(input.charAt(i));
                    i++;
                }
                
                // Check for invalid number followed by identifier
                if (i < length && (Character.isLetter(input.charAt(i)) || input.charAt(i) == '_')) {
                    addTokenToTable("LEXICAL ERROR", "Invalid token: number '" + sb.toString() + 
                                  "' followed by identifier starting with '" + input.charAt(i) + "'");
                    JOptionPane.showMessageDialog(this, 
                        "Lexical Error: Numbers cannot be directly followed by letters", 
                        "Invalid Token", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                tokens.add(new Token("NUMBER", sb.toString(), startPos));
                continue;
            }

            // Handle operators
            if (isOperatorChar(c)) {
                StringBuilder sb = new StringBuilder();
                int startPos = i;
                while (i < length && isOperatorChar(input.charAt(i))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                String op = sb.toString();
                tokens.add(new Token(getOperatorType(op), op, startPos));
                continue;
            }

            // Handle identifiers and keywords
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                int startPos = i;
                while (i < length && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    sb.append(input.charAt(i));
                    i++;
                }
                String word = sb.toString();
                if (isKeyword(word)) {
                    tokens.add(new Token("KEYWORD", word, startPos));
                } else {
                    tokens.add(new Token("IDENTIFIER", word, startPos));
                }
                continue;
            }

            // Unknown character
            tokens.add(new Token("UNKNOWN", Character.toString(c), i));
            addTokenToTable("LEXICAL ERROR", "Unknown character: '" + c + "' at position " + i);
            JOptionPane.showMessageDialog(this, 
                "Lexical Error: Unknown character '" + c + "'", 
                "Invalid Character", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate token sequence
        String semanticError = validateTokenSequence(tokens);
        if (semanticError != null) {
            addTokenToTable("SEMANTIC ERROR", semanticError);
            JOptionPane.showMessageDialog(this, 
                semanticError, 
                "Invalid Token Sequence", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Display all tokens
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
            case "&": return "BITWISE_AND";
            case "|": return "BITWISE_OR";
            case ":": return "COLON";
            case "->": return "LAMBDA_ARROW";
            default: return "OPERATOR";
        }
    }

    private boolean isKeyword(String word) {
        String[] keywords = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null", "var", "String"
        };

        for (String keyword : keywords) {
            if (word.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EnhancedLexerGUI().setVisible(true);
        });
    }
}
