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
        // Configure the main window properties
        setTitle("Array Lexical Analyzer");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centers the window on screen
        setLayout(new BorderLayout(10, 10));

        // === INPUT SECTION (TOP) ===
        // Create a panel for the text input area
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Source Code Input"));
        
        // Text area where user enters code to analyze
        inputArea = new JTextArea(3, 40);
        inputArea.setText("int[] numbers = {10, 20, 50};"); // Sample code
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        // Button that triggers the lexical analysis
        JButton analyzeBtn = new JButton("Analyze / Tokenize");
        analyzeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runAnalysis();  // Call the analysis method when clicked
            }
        });

        topPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        topPanel.add(analyzeBtn, BorderLayout.SOUTH);

        // === OUTPUT SECTION (CENTER) ===
        // Create a table to display the tokens
        String[] columnNames = {"Token Type", "Value / Symbol"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultTable.setRowHeight(25);

        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Token Output"));

        // Add both sections to the main window
        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    // === LEXICAL ANALYZER (THE MAIN LOGIC) ===
    // This method breaks down the input code into tokens
    private void runAnalysis() {
        tableModel.setRowCount(0);  // Clear previous results
        
        String input = inputArea.getText();  // Get the code from text area
        int length = input.length();
        int i = 0;  // Position tracker in the string

        // Go through each character in the input
        while (i < length) {
            char c = input.charAt(i);

            // RULE 1: Skip spaces, tabs, newlines
            if (Character.isWhitespace(c)) {
                i++; 
                continue;
            }

            // RULE 2: Check for special symbols like [], {}, =, ;
            if (isSymbol(c)) {
                addTokenToTable(getSymbolType(c), Character.toString(c));
                i++; 
                continue;
            }

            // RULE 3: Build complete numbers (can be multiple digits)
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                // Keep reading digits until we hit a non-digit
                while (i < length && Character.isDigit(input.charAt(i))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                addTokenToTable("NUMBER", sb.toString());
                continue;
            }

            // RULE 4: Build complete words (keywords or identifiers)
            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                // Keep reading letters/numbers until we hit something else
                while (i < length && (Character.isLetterOrDigit(input.charAt(i)))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                String word = sb.toString();
                // Check if it's a reserved keyword
                if (word.matches("int|String|float|double|new")) {
                    addTokenToTable("KEYWORD", word);
                } else {
                    addTokenToTable("IDENTIFIER", word);  // It's a variable name
                }
                continue;
            }

            // RULE 5: If we can't identify it, mark as unknown
            addTokenToTable("UNKNOWN", Character.toString(c));
            i++;
        }
    }

    // Helper: Add a new row to the results table
    private void addTokenToTable(String type, String value) {
        tableModel.addRow(new Object[]{type, value});
    }

    // Helper: Check if character is a recognized symbol
    private boolean isSymbol(char c) {
        return "[]{},=;".indexOf(c) != -1;
    }

    // Helper: Get the proper name for each symbol
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

    // === PROGRAM ENTRY POINT ===
    public static void main(String[] args) {
        // Launch the GUI on the proper thread for Swing
        SwingUtilities.invokeLater(() -> {
            new ArrayLexerGUI().setVisible(true);
        });
    }
}
