import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ArrayLexerGUI extends JFrame {

    // --- GUI COMPONENTS ---
    private JTextArea inputArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    public ArrayLexerGUI() {
        // 1. Setup the Window
        setTitle("Array Lexical Analyzer");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center window
        setLayout(new BorderLayout(10, 10));

        // 2. Top Panel: Input Area
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Source Code Input"));
        
        inputArea = new JTextArea(3, 40);
        inputArea.setText("int[] numbers = {10, 20, 50};"); // Default text
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

        // 3. Center Panel: Output Table
        String[] columnNames = {"Token Type", "Value / Symbol"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultTable.setRowHeight(25);

        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Token Output"));

        // 4. Add to Frame
        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    // --- THE LEXER LOGIC (Helper Function) ---
    private void runAnalysis() {
        // Clear previous results
        tableModel.setRowCount(0);
        
        String input = inputArea.getText();
        int length = input.length();
        int i = 0;

        // Loop through the input string
        while (i < length) {
            char c = input.charAt(i);

            // Skip Whitespace
            if (Character.isWhitespace(c)) {
                i++; continue;
            }

            // Handle Array Syntax & Operators
            if (isSymbol(c)) {
                addTokenToTable(getSymbolType(c), Character.toString(c));
                i++; continue;
            }

            // Handle Numbers
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < length && Character.isDigit(input.charAt(i))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                addTokenToTable("NUMBER", sb.toString());
                continue;
            }

            // Handle Words (Keywords/IDs)
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

            // Unknown
            addTokenToTable("UNKNOWN", Character.toString(c));
            i++;
        }
    }

    // Helper to add row to GUI table
    private void addTokenToTable(String type, String value) {
        tableModel.addRow(new Object[]{type, value});
    }

    // Helper to identify symbols
    private boolean isSymbol(char c) {
        return "[]{},=;".indexOf(c) != -1;
    }

    // Helper to map symbol to name
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

    // --- MAIN LAUNCHER ---
    public static void main(String[] args) {
        // Run on Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            new ArrayLexerGUI().setVisible(true);
        });
    }
}
