import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ArrayLexerGUI extends JFrame {

    // GUI components that we'll use throughout the program
    private JTextArea inputArea;      // Where user types code
    private JTable resultTable;       // Shows the tokens in a table
    private DefaultTableModel tableModel;  // Manages the table data

    public ArrayLexerGUI() {
        // === WINDOW SETUP ===
        setTitle("Array Lexical Analyzer");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // === INPUT SECTION (TOP) ===
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Source Code Input"));
        
        inputArea = new JTextArea(3, 40);
        inputArea.setText("int[] numbers = {10, 20, 50};");
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
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultTable.setRowHeight(25);

        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Token Output"));

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    // === INPUT VALIDATION ===
    // Returns error message if input is invalid, null if valid
    private String validateInput(String input) {
        // Check 1: Empty or whitespace-only input
        if (input == null || input.trim().isEmpty()) {
            return "Error: Input cannot be empty";
        }

        // Check 2: Must contain at least one valid keyword for array declaration
        if (!input.matches(".*\\b(int|String|float|double)\\s*\\[\\].*")) {
            return "Error: Input must contain a valid array declaration (int[], String[], float[], or double[])";
        }

        // Check 3: Must have proper array syntax with brackets
        if (!input.contains("[") || !input.contains("]")) {
            return "Error: Array declaration must include square brackets []";
        }

        // Check 4: Check for invalid characters (only allow letters, digits, spaces, and valid symbols)
        if (input.matches(".*[^a-zA-Z0-9\\s\\[\\]\\{\\}\\,\\=\\;].*")) {
            return "Error: Input contains invalid characters. Only letters, numbers, and symbols [ ] { } , = ; are allowed";
        }

        // Check 5: Must have an identifier (variable name) after the type
        if (!input.matches(".*\\b(int|String|float|double)\\s*\\[\\]\\s+[a-zA-Z][a-zA-Z0-9]*.*")) {
            return "Error: Array declaration must have a valid identifier (variable name)";
        }

        // Check 6: If assignment operator exists, must have values
        if (input.contains("=") && !input.contains("{")) {
            return "Error: Array assignment must use curly braces { } for initialization";
        }

        // Check 7: Check for balanced brackets and braces
        int bracketCount = 0;
        int braceCount = 0;
        for (char c : input.toCharArray()) {
            if (c == '[') bracketCount++;
            if (c == ']') bracketCount--;
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            
            // Brackets or braces should never go negative
            if (bracketCount < 0) return "Error: Mismatched brackets - closing ] without opening [";
            if (braceCount < 0) return "Error: Mismatched braces - closing } without opening {";
        }
        
        if (bracketCount != 0) {
            return "Error: Unbalanced square brackets [ ]";
        }
        if (braceCount != 0) {
            return "Error: Unbalanced curly braces { }";
        }

        // Check 8: Must end with semicolon
        if (!input.trim().endsWith(";")) {
            return "Error: Statement must end with a semicolon ;";
        }

        // Check 9: Variable name cannot be a keyword
        String varPattern = "\\b(int|String|float|double)\\s*\\[\\]\\s+([a-zA-Z][a-zA-Z0-9]*)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(varPattern);
        java.util.regex.Matcher m = p.matcher(input);
        if (m.find()) {
            String varName = m.group(2);
            if (varName.matches("int|String|float|double|new")) {
                return "Error: Variable name '" + varName + "' cannot be a reserved keyword";
            }
        }

        return null; // All validation passed
    }

    // === LEXICAL ANALYZER ===
    private void runAnalysis() {
        tableModel.setRowCount(0);
        
        String input = inputArea.getText();
        
        // === VALIDATE INPUT FIRST ===
        String validationError = validateInput(input);
        if (validationError != null) {
            // Show error message in the table
            addTokenToTable("VALIDATION ERROR", validationError);
            JOptionPane.showMessageDialog(this, 
                validationError, 
                "Invalid Input", 
                JOptionPane.ERROR_MESSAGE);
            return; // Stop processing
        }
        
        // If validation passed, proceed with tokenization
        int length = input.length();
        int i = 0;

        while (i < length) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                i++; 
                continue;
            }

            if (isSymbol(c)) {
                addTokenToTable(getSymbolType(c), Character.toString(c));
                i++; 
                continue;
            }

            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < length && Character.isDigit(input.charAt(i))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                addTokenToTable("NUMBER", sb.toString());
                continue;
            }

            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < length && (Character.isLetterOrDigit(input.charAt(i)))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                String word = sb.toString();
                if (word.matches("int|String|float|double|new")) {
                    addTokenToTable("KEYWORD", word);
                } else {
                    addTokenToTable("IDENTIFIER", word);
                }
                continue;
            }

            addTokenToTable("UNKNOWN", Character.toString(c));
            i++;
        }
    }

    private void addTokenToTable(String type, String value) {
        tableModel.addRow(new Object[]{type, value});
    }

    private boolean isSymbol(char c) {
        return "[]{},=;".indexOf(c) != -1;
    }

    private String getSymbolType(char c) {
        switch (c) {
            case '[': return "L_BRACKET";
            case ']': return "R_BRACKET";
            case '{': return "L_BRACE";
            case '}': return "R_BRACE";
            case ',': return "COMMA";
            case '=': return "EQUALS";
            case ';': return "SEMICOLON";
            default: return "SYMBOL";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ArrayLexerGUI().setVisible(true);
        });
    }
}
