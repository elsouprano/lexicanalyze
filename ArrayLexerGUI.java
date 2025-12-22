import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
                return false; // Bawal edith sa result table
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

    // === ENHANCED INPUT VALIDATION ===
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

        return null;
    }

    // === SEMANTIC VALIDATION ===
    private String validateTokenSequence(ArrayList<Token> tokens) {
        // Check for array initialization syntax: { num , num , num }
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            
            // Found opening brace of array initialization
            if (t.type.equals("L_BRACE")) {
                int j = i + 1;
                boolean expectingValue = true;
                boolean expectingComma = false;
                
                while (j < tokens.size() && !tokens.get(j).type.equals("R_BRACE")) {
                    Token current = tokens.get(j);
                    
                    // Skip INDENT, WHITESPACE tokens
                    if (current.type.equals("INDENT") || 
                        current.type.equals("WHITESPACE")) {
                        j++;
                        continue;
                    }
                    
                    if (expectingValue) {
                        if (!current.type.equals("NUMBER") && 
                            !current.type.equals("IDENTIFIER") &&
                            !current.type.equals("STRING")) {
                            return "Error: Expected a value in array initialization at position " + j + 
                                   ", found " + current.type + " '" + current.value + "'";
                        }
                        expectingValue = false;
                        expectingComma = true;
                    } else if (expectingComma) {
                        if (current.type.equals("COMMA")) {
                            expectingValue = true;
                            expectingComma = false;
                        } else if (current.type.equals("NUMBER") || 
                                   current.type.equals("IDENTIFIER") ||
                                   current.type.equals("STRING")) {
                            return "Error: Missing comma between array elements. Found '" + 
                                   tokens.get(j-1).value + "' and '" + current.value + "' without comma separator";
                        } else {
                            return "Error: Expected comma or closing brace in array initialization, found " + 
                                   current.type + " '" + current.value + "'";
                        }
                    }
                    j++;
                }
            }
        }
        return null;
    }
    
    // Token class to store type and value
    private static class Token {
        String type;
        String value;
        
        Token(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    // === ENHANCED LEXICAL ANALYZER WITH INDENTATION ===
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
                // Found indentation
                tokens.add(new Token("INDENT DELIMITER", "  "));
                
                // Skip all indentation characters
                while (i < length && (input.charAt(i) == ' ' || input.charAt(i) == '\t')) {
                    i++;
                }
                
                // Check if line is not just whitespace
                if (i < length && input.charAt(i) != '\n' && input.charAt(i) != '\r') {
                    atLineStart = false;
                }
                continue;
            }

            // Handle newlines
            if (c == '\n' || c == '\r') {
                atLineStart = true;
                i++;
                // Skip \r\n combination (new line to)
                if (c == '\r' && i < length && input.charAt(i) == '\n') {
                    i++;
                }
                continue;
            }

            // Handle whitespace (spaces/tabs not at line start)
            if (c == ' ' || c == '\t') {
                tokens.add(new Token("WHITESPACE", " "));
                // Skip consecutive whitespace
                while (i < length && (input.charAt(i) == ' ' || input.charAt(i) == '\t')) {
                    i++;
                }
                continue;
            }

            // If we encounter non-whitespace, we're no longer at line start
            atLineStart = false;

            // Handle single-character symbols
            if (isSingleCharSymbol(c)) {
                String type = getSymbolType(c);
                tokens.add(new Token(type, Character.toString(c)));
                i++; 
                continue;
            }

            // Handle numbers (integers and floats)
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                boolean hasDecimal = false;
                while (i < length && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                    if (input.charAt(i) == '.') {
                        if (hasDecimal) break;
                        hasDecimal = true;
                    }
                    sb.append(input.charAt(i));
                    i++;
                }
                tokens.add(new Token("NUMBER", sb.toString()));
                continue;
            }

            // Handle operators (multi-character aware)
            if (isOperatorChar(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < length && isOperatorChar(input.charAt(i))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                String op = sb.toString();
                tokens.add(new Token(getOperatorType(op), op));
                continue;
            }

            // Handle identifiers and keywords
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                while (i < length && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    sb.append(input.charAt(i));
                    i++;
                }
                String word = sb.toString();
                if (isKeyword(word)) {
                    tokens.add(new Token("KEYWORD", word));
                } else {
                    tokens.add(new Token("IDENTIFIER", word));
                }
                continue;
            }

            // Unknown character
            tokens.add(new Token("UNKNOWN", Character.toString(c)));
            i++;
        }
        
        // Validate token sequence (semantic validation)
        String semanticError = validateTokenSequence(tokens);
        if (semanticError != null) {
            addTokenToTable("SEMANTIC ERROR", semanticError);
            JOptionPane.showMessageDialog(this, 
                semanticError, 
                "Invalid Token Sequence", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // If all validation passed, display tokens
        for (Token token : tokens) {
            addTokenToTable(token.type, token.value);
        }
    }

    private void addTokenToTable(String type, String value) {
        tableModel.addRow(new Object[]{type, value});
    }

    // Single character symbols
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

    // Operator characters
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

    // Enhanced keyword list
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
