package symbiote;

import java.util.*;

public class Lexer {

    private static final Map<String, Token.Tipo> KW = new HashMap<>();
    static {
        KW.put("let",    Token.Tipo.LET);
        KW.put("be",     Token.Tipo.BE);
        KW.put("if",     Token.Tipo.IF);
        KW.put("else",   Token.Tipo.ELSE);
        KW.put("while",  Token.Tipo.WHILE);
        KW.put("for",    Token.Tipo.FOR);
        KW.put("fn",     Token.Tipo.FN);
        KW.put("return", Token.Tipo.RETURN);
        KW.put("emit",   Token.Tipo.EMIT);
        KW.put("int",    Token.Tipo.INT);
        KW.put("float",  Token.Tipo.FLOAT);
        KW.put("string", Token.Tipo.STRING);
        KW.put("bool",   Token.Tipo.BOOL);
        KW.put("true",   Token.Tipo.TRUE);
        KW.put("false",  Token.Tipo.FALSE);
    }

    private static final String ENTRY_KW = "ITS DANGEROUS TO GO ALONE, TAKE THIS";

    private final String       src;
    private       int          pos = 0;
    private       int          ln  = 1;
    private       int          col = 1;
    private final List<Token>  toks = new ArrayList<>();
    private final List<String> errs = new ArrayList<>();

    public Lexer(String src) { this.src = src; }

    public List<Token> analizar() {
        toks.clear(); errs.clear();
        pos = 0; ln = 1; col = 1;

        while (pos < src.length()) {
            saltar();
            if (pos >= src.length()) break;

            char c = src.charAt(pos);
            if (c == '\n') { ln++; col = 1; pos++; continue; }

            if (intentarEntry()) continue;
            if (Character.isDigit(c))               { numero(); continue; }
            if (c == '"')                            { cadena(); continue; }
            if (Character.isLetter(c) || c == '_')   { palabra(); continue; }
            if (simbolo(c)) continue;

            errs.add("Error lexico [L" + ln + ":C" + col + "]: caracter desconocido '" + c + "'");
            toks.add(new Token(Token.Tipo.ERROR, String.valueOf(c), ln, col));
            pos++; col++;
        }

        toks.add(new Token(Token.Tipo.EOF, "", ln, col));
        return toks;
    }

    public List<String> getErrores() { return errs; }

    private void saltar() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ')  { col++; pos++; }
            else if (c == '\t') { col += 4; pos++; }
            else if (c == '\r') { pos++; }
            else if (c == '\n') break;
            else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                while (pos < src.length() && src.charAt(pos) != '\n') { pos++; col++; }
            } else break;
        }
    }

    private boolean intentarEntry() {
        if (src.startsWith(ENTRY_KW, pos)) {
            int ic = col;
            toks.add(new Token(Token.Tipo.ENTRY, ENTRY_KW, ln, ic));
            pos += ENTRY_KW.length();
            col += ENTRY_KW.length();
            return true;
        }
        return false;
    }

    private void numero() {
        int ip = pos, ic = col;
        boolean dec = false;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) { pos++; col++; }
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++; col++;
            if (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                dec = true;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) { pos++; col++; }
            } else {
                String lex = src.substring(ip, pos);
                errs.add("Error lexico [L" + ln + ":C" + ic + "]: decimal mal formado '" + lex + "'");
                toks.add(new Token(Token.Tipo.ERROR, lex, ln, ic));
                return;
            }
        }
        if (pos < src.length() && (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '_')) {
            while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) { pos++; col++; }
            String t = src.substring(ip, pos);
            errs.add("Error lexico [L" + ln + ":C" + ic + "]: numero pegado a letras '" + t + "'");
            toks.add(new Token(Token.Tipo.ERROR, t, ln, ic));
            return;
        }
        toks.add(new Token(dec ? Token.Tipo.LIT_FLOAT : Token.Tipo.LIT_INT, src.substring(ip, pos), ln, ic));
    }

    private void cadena() {
        int ic = col; pos++; col++;
        int ip = pos; boolean cerrada = false;
        while (pos < src.length() && src.charAt(pos) != '\n') {
            if (src.charAt(pos) == '"') { cerrada = true; break; }
            if (src.charAt(pos) == '\\') { pos++; col++; }
            if (pos < src.length()) { pos++; col++; }
        }
        String cont = src.substring(ip, pos);
        if (cerrada) {
            pos++; col++;
            toks.add(new Token(Token.Tipo.LIT_STRING, "\"" + cont + "\"", ln, ic));
        } else {
            errs.add("Error lexico [L" + ln + ":C" + ic + "]: cadena sin cerrar");
            toks.add(new Token(Token.Tipo.ERROR, "\"" + cont, ln, ic));
        }
    }

    private void palabra() {
        int ip = pos, ic = col;
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) { pos++; col++; }
        String lex = src.substring(ip, pos);
        if (lex.length() > 64) {
            errs.add("Error lexico [L" + ln + ":C" + ic + "]: identificador muy largo");
            toks.add(new Token(Token.Tipo.ERROR, lex, ln, ic));
            return;
        }
        Token.Tipo kw = KW.get(lex);
        toks.add(new Token(kw != null ? kw : Token.Tipo.IDENT, lex, ln, ic));
    }

    private boolean simbolo(char c) {
        int ic = col;
        char n = pos + 1 < src.length() ? src.charAt(pos + 1) : '\0';

        if (c == '-' && n == '>') { toks.add(new Token(Token.Tipo.ARROW, "->", ln, ic)); pos += 2; col += 2; return true; }
        if (c == '=' && n == '=') { toks.add(new Token(Token.Tipo.EQ,    "==", ln, ic)); pos += 2; col += 2; return true; }
        if (c == '!' && n == '=') { toks.add(new Token(Token.Tipo.NEQ,   "!=", ln, ic)); pos += 2; col += 2; return true; }
        if (c == '<' && n == '=') { toks.add(new Token(Token.Tipo.LE,    "<=", ln, ic)); pos += 2; col += 2; return true; }
        if (c == '>' && n == '=') { toks.add(new Token(Token.Tipo.GE,    ">=", ln, ic)); pos += 2; col += 2; return true; }
        if (c == '&' && n == '&') { toks.add(new Token(Token.Tipo.AND,   "&&", ln, ic)); pos += 2; col += 2; return true; }
        if (c == '|' && n == '|') { toks.add(new Token(Token.Tipo.OR,    "||", ln, ic)); pos += 2; col += 2; return true; }

        Token.Tipo tipo;
        switch (c) {
            case '+': tipo = Token.Tipo.PLUS;     break;
            case '-': tipo = Token.Tipo.MINUS;    break;
            case '*': tipo = Token.Tipo.STAR;     break;
            case '/': tipo = Token.Tipo.SLASH;    break;
            case '=': tipo = Token.Tipo.ASSIGN;   break;
            case '<': tipo = Token.Tipo.LT;       break;
            case '>': tipo = Token.Tipo.GT;       break;
            case '!': tipo = Token.Tipo.NOT;      break;
            case '{': tipo = Token.Tipo.LBRACE;   break;
            case '}': tipo = Token.Tipo.RBRACE;   break;
            case '(': tipo = Token.Tipo.LPAREN;   break;
            case ')': tipo = Token.Tipo.RPAREN;   break;
            case '[': tipo = Token.Tipo.LBRACKET; break;
            case ']': tipo = Token.Tipo.RBRACKET; break;
            case ';': tipo = Token.Tipo.SEMI;     break;
            case ',': tipo = Token.Tipo.COMMA;    break;
            default: return false;
        }
        toks.add(new Token(tipo, String.valueOf(c), ln, ic));
        pos++; col++;
        return true;
    }
}