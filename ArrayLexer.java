import java.util.ArrayList;
import java.util.List;

public class ArrayLexer {

    // 1. Specific Token Types for Array Syntax
    public enum TokenType {
        KEYWORD,    // int, new, etc.
        IDENTIFIER, // variable names
        NUMBER,     // indices or values
        L_BRACKET,  // [
        R_BRACKET,  // ]
        L_BRACE,    // {
        R_BRACE,    // }
        COMMA,      // ,
        EQUALS,     // =
        SEMICOLON,  // ;
        UNKNOWN
    }

    // 2. Token Data Structure
    public static class Token {
        public TokenType type;
        public String value;

        public Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            // Formatting for cleaner output
            return String.format("%-12s : %s", type, value);
        }
    }

    // 3. The Lexer Function for Array Strings
    public static ArrayList<Token> analyzeArraySource(String source) {
        ArrayList<Token> tokens = new ArrayList<>();
        int length = source.length();
        int i = 0;

        while (i < length) {
            char c = source.charAt(i);

            // Skip Whitespace
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Handle Array Symbols (The core requirement)
            switch (c) {
                case '[':
                    tokens.add(new Token(TokenType.L_BRACKET, "["));
                    i++; continue;
                case ']':
                    tokens.add(new Token(TokenType.R_BRACKET, "]"));
                    i++; continue;
                case '{':
                    tokens.add(new Token(TokenType.L_BRACE, "{"));
                    i++; continue;
                case '}':
                    tokens.add(new Token(TokenType.R_BRACE, "}"));
                    i++; continue;
                case ',':
                    tokens.add(new Token(TokenType.COMMA, ","));
                    i++; continue;
                case '=':
                    tokens.add(new Token(TokenType.EQUALS, "="));
                    i++; continue;
                case ';':
                    tokens.add(new Token(TokenType.SEMICOLON, ";"));
                    i++; continue;
            }

            // Handle Numbers (Array indices or values)
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < length && Character.isDigit(source.charAt(i))) {
                    sb.append(source.charAt(i));
                    i++;
                }
                tokens.add(new Token(TokenType.NUMBER, sb.toString()));
                continue;
            }

            // Handle Keywords and Identifiers (e.g., "int", "arr")
            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < length && (Character.isLetterOrDigit(source.charAt(i)))) {
                    sb.append(source.charAt(i));
                    i++;
                }
                String word = sb.toString();
                if (word.equals("int") || word.equals("new") || word.equals("String")) {
                    tokens.add(new Token(TokenType.KEYWORD, word));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, word));
                }
                continue;
            }

            // Unknown Character
            tokens.add(new Token(TokenType.UNKNOWN, Character.toString(c)));
            i++;
        }

        return tokens;
    }

    public static void main(String[] args) {
        // A complex array declaration string
        String arrayCode = "int[] myArr = {10, 20, 500};";

        System.out.println("Input Source: " + arrayCode);
        System.out.println("--- Lexical Analysis ---");

        ArrayList<Token> result = analyzeArraySource(arrayCode);

        for (Token t : result) {
            System.out.println(t);
        }
    }
}
