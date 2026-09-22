package symbiote;

import java.util.*;

public class Parser {

    private static class VarInfo {
        Token.Tipo tipo;
        boolean    arr;
        VarInfo(Token.Tipo t, boolean a) { tipo = t; arr = a; }
    }

    private static class FnInfo {
        List<Token.Tipo> params;
        Token.Tipo       ret;
        FnInfo(List<Token.Tipo> p, Token.Tipo r) { params = p; ret = r; }
    }

    private final List<Token>          toks;
    private       int                  pos = 0;
    private final List<String>         errs = new ArrayList<>();
    private       Map<String, VarInfo> vars = new LinkedHashMap<>();
    private final Map<String, FnInfo>  fns  = new LinkedHashMap<>();

    public Parser(List<Token> toks) { this.toks = toks; }

    public List<String> getErrores() { return errs; }

    private Token peek()  { return pos < toks.size() ? toks.get(pos) : toks.get(toks.size() - 1); }
    private Token peek2() { return pos + 1 < toks.size() ? toks.get(pos + 1) : toks.get(toks.size() - 1); }
    private Token advance() { Token t = peek(); if (t.tipo != Token.Tipo.EOF) pos++; return t; }
    private boolean check(Token.Tipo t) { return peek().tipo == t; }
    private boolean match(Token.Tipo t) { if (check(t)) { advance(); return true; } return false; }

    private void expect(Token.Tipo t, String msg) {
        if (!match(t)) {
            Token tk = peek();
            errs.add("Error sintactico [L" + tk.ln + ":C" + tk.col + "]: " + msg + " — se encontro '" + tk.lex + "'");
        }
    }

    private boolean isTipo() {
        Token.Tipo t = peek().tipo;
        return t == Token.Tipo.INT || t == Token.Tipo.FLOAT || t == Token.Tipo.STRING || t == Token.Tipo.BOOL;
    }

    public void parse() {
        while (check(Token.Tipo.FN)) parseFn();
        expect(Token.Tipo.ENTRY, "el programa debe iniciar con 'ITS DANGEROUS TO GO ALONE, TAKE THIS'");
        parseBloque();
        if (!check(Token.Tipo.EOF))
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: codigo inesperado al final del programa");
    }

    private Token.Tipo parseTipo() {
        if (!isTipo()) {
            Token t = peek();
            errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: se esperaba un tipo, se encontro '" + t.lex + "'");
            return null;
        }
        return advance().tipo;
    }

    private void parseFn() {
        advance();
        if (!check(Token.Tipo.IDENT)) {
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba nombre de funcion");
            sincronizar();
            return;
        }
        Token nom = advance();
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues del nombre de la funcion");

        List<Token.Tipo>    params  = new ArrayList<>();
        Map<String, VarInfo> locales = new LinkedHashMap<>();
        if (!check(Token.Tipo.RPAREN)) {
            do {
                if (!check(Token.Tipo.IDENT)) {
                    errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba nombre de parametro");
                    break;
                }
                Token pn = advance();
                expect(Token.Tipo.BE, "se esperaba 'be' despues del parametro '" + pn.lex + "'");
                Token.Tipo pt = parseTipo();
                boolean parr = false;
                if (match(Token.Tipo.LBRACKET)) { expect(Token.Tipo.RBRACKET, "se esperaba ']'"); parr = true; }
                params.add(pt);
                locales.put(pn.lex, new VarInfo(pt, parr));
            } while (match(Token.Tipo.COMMA));
        }
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar los parametros");
        expect(Token.Tipo.ARROW, "se esperaba '->' seguido del tipo de retorno");
        Token.Tipo ret = parseTipo();

        fns.put(nom.lex, new FnInfo(params, ret));

        Map<String, VarInfo> exterior = vars;
        vars = new LinkedHashMap<>(exterior);
        vars.putAll(locales);
        parseBloque();
        vars = exterior;
    }

    private void parseBloque() {
        expect(Token.Tipo.LBRACE, "se esperaba '{' para abrir el bloque");
        while (!check(Token.Tipo.RBRACE) && !check(Token.Tipo.EOF)) parseSentencia();
        expect(Token.Tipo.RBRACE, "se esperaba '}' para cerrar el bloque");
    }

    private void parseSentencia() {
        if (check(Token.Tipo.ERROR)) { advance(); return; }
        if (check(Token.Tipo.LET)) { parseLet(); return; }
        if (check(Token.Tipo.IDENT) && (peek2().tipo == Token.Tipo.ASSIGN || peek2().tipo == Token.Tipo.LBRACKET)) { parseAsigna(); return; }
        if (check(Token.Tipo.IDENT) && peek2().tipo == Token.Tipo.LPAREN) { parseLlamada(); expect(Token.Tipo.SEMI, "se esperaba ';'"); return; }
        if (check(Token.Tipo.IF))     { parseIf();    return; }
        if (check(Token.Tipo.WHILE))  { parseWhile(); return; }
        if (check(Token.Tipo.FOR))    { parseFor();   return; }
        if (check(Token.Tipo.EMIT))   { parseEmit();  return; }
        if (check(Token.Tipo.RETURN)) { parseReturn(); return; }
        if (check(Token.Tipo.LBRACE)) { parseBloque(); return; }
        Token t = peek();
        errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: sentencia desconocida '" + t.lex + "'");
        advance();
    }

    private void parseLet() {
        advance();
        if (!check(Token.Tipo.IDENT)) {
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba nombre de variable despues de 'let'");
            sincronizar();
            return;
        }
        Token nom = advance();
        expect(Token.Tipo.BE, "se esperaba 'be' despues de '" + nom.lex + "'");
        Token.Tipo tipo = parseTipo();
        boolean arr = false;
        if (match(Token.Tipo.LBRACKET)) { expect(Token.Tipo.RBRACKET, "se esperaba ']'"); arr = true; }

        if (vars.containsKey(nom.lex))
            errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: variable '" + nom.lex + "' ya declarada");
        else
            vars.put(nom.lex, new VarInfo(tipo, arr));

        if (match(Token.Tipo.ASSIGN)) {
            if (arr) {
                expect(Token.Tipo.LBRACKET, "se esperaba '[' para inicializar el arreglo");
                while (!check(Token.Tipo.RBRACKET) && !check(Token.Tipo.EOF)) {
                    Token.Tipo tv = expr();
                    if (tipo != null && tv != null && !compat(tipo, tv))
                        errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: elemento de arreglo incompatible con " + nomTipo(tipo));
                    if (!check(Token.Tipo.RBRACKET)) expect(Token.Tipo.COMMA, "se esperaba ',' entre elementos del arreglo");
                }
                expect(Token.Tipo.RBRACKET, "se esperaba ']' para cerrar el arreglo");
            } else {
                Token.Tipo tv = expr();
                if (tipo != null && tv != null && !compat(tipo, tv))
                    errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: tipo incompatible — '" + nom.lex + "' es " + nomTipo(tipo) + " pero se asigna " + nomTipo(tv));
            }
        }
        expect(Token.Tipo.SEMI, "se esperaba ';' al final de la declaracion de '" + nom.lex + "'");
    }

    private void parseAsigna() {
        Token nom = advance();
        if (!vars.containsKey(nom.lex))
            errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: variable '" + nom.lex + "' no declarada");
        VarInfo info = vars.get(nom.lex);

        if (match(Token.Tipo.LBRACKET)) {
            if (info != null && !info.arr)
                errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: '" + nom.lex + "' no es un arreglo");
            Token.Tipo ti = expr();
            if (ti != null && ti != Token.Tipo.INT)
                errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: el indice debe ser int");
            expect(Token.Tipo.RBRACKET, "se esperaba ']'");
        }

        expect(Token.Tipo.ASSIGN, "se esperaba '=' en la asignacion de '" + nom.lex + "'");
        Token.Tipo tipo = info != null ? info.tipo : null;
        Token.Tipo tv = expr();
        if (tipo != null && tv != null && !compat(tipo, tv))
            errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: tipo incompatible — '" + nom.lex + "' es " + nomTipo(tipo) + " pero se asigna " + nomTipo(tv));
        expect(Token.Tipo.SEMI, "se esperaba ';' al final de la asignacion de '" + nom.lex + "'");
    }

    private Token.Tipo parseLlamada() {
        Token nom = advance();
        advance();
        FnInfo fi = fns.get(nom.lex);
        if (fi == null)
            errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: funcion '" + nom.lex + "' no declarada");
        int i = 0;
        if (!check(Token.Tipo.RPAREN)) {
            do {
                Token.Tipo ta = expr();
                if (fi != null && i < fi.params.size() && ta != null && !compat(fi.params.get(i), ta))
                    errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: argumento " + (i + 1) + " incompatible en la llamada a '" + nom.lex + "'");
                i++;
            } while (match(Token.Tipo.COMMA));
        }
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar la llamada a '" + nom.lex + "'");
        if (fi != null && i != fi.params.size())
            errs.add("Error sintactico [L" + nom.ln + ":C" + nom.col + "]: '" + nom.lex + "' espera " + fi.params.size() + " argumento(s), se dieron " + i);
        return fi != null ? fi.ret : null;
    }

    private void parseIf() {
        advance();
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues de 'if'");
        Token.Tipo tc = expr();
        if (tc != null && tc != Token.Tipo.BOOL)
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: la condicion de 'if' debe ser bool");
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar la condicion");
        parseBloque();
        if (match(Token.Tipo.ELSE)) {
            if (check(Token.Tipo.IF)) parseIf(); else parseBloque();
        }
    }

    private void parseWhile() {
        advance();
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues de 'while'");
        Token.Tipo tc = expr();
        if (tc != null && tc != Token.Tipo.BOOL)
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: la condicion de 'while' debe ser bool");
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar la condicion");
        parseBloque();
    }

    private void parseFor() {
        advance();
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues de 'for'");
        parseLet();
        Token.Tipo tc = expr();
        if (tc != null && tc != Token.Tipo.BOOL)
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: la condicion de 'for' debe ser bool");
        expect(Token.Tipo.SEMI, "se esperaba ';' despues de la condicion");
        if (check(Token.Tipo.IDENT)) {
            Token vt = advance();
            expect(Token.Tipo.ASSIGN, "se esperaba '=' en el incremento de 'for'");
            VarInfo vi = vars.get(vt.lex);
            Token.Tipo tv = expr();
            if (vi != null && tv != null && !compat(vi.tipo, tv))
                errs.add("Error sintactico [L" + vt.ln + ":C" + vt.col + "]: tipo incompatible en el incremento de 'for'");
        } else {
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba una variable en el incremento de 'for'");
        }
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar el encabezado de 'for'");
        parseBloque();
    }

    private void parseEmit() {
        advance();
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues de 'emit'");
        expr();
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar 'emit'");
        expect(Token.Tipo.SEMI, "se esperaba ';' despues de 'emit'");
    }

    private void parseReturn() {
        advance();
        if (!check(Token.Tipo.SEMI)) expr();
        expect(Token.Tipo.SEMI, "se esperaba ';' despues de 'return'");
    }

    private Token.Tipo expr() { return orExpr(); }

    private Token.Tipo orExpr() {
        Token.Tipo t = andExpr();
        while (match(Token.Tipo.OR)) {
            Token.Tipo d = andExpr();
            t = (t == Token.Tipo.BOOL && d == Token.Tipo.BOOL) ? Token.Tipo.BOOL : null;
        }
        return t;
    }

    private Token.Tipo andExpr() {
        Token.Tipo t = eqExpr();
        while (match(Token.Tipo.AND)) {
            Token.Tipo d = eqExpr();
            t = (t == Token.Tipo.BOOL && d == Token.Tipo.BOOL) ? Token.Tipo.BOOL : null;
        }
        return t;
    }

    private Token.Tipo eqExpr() {
        Token.Tipo t = cmpExpr();
        while (check(Token.Tipo.EQ) || check(Token.Tipo.NEQ)) { advance(); cmpExpr(); t = Token.Tipo.BOOL; }
        return t;
    }

    private Token.Tipo cmpExpr() {
        Token.Tipo t = addExpr();
        while (check(Token.Tipo.LT) || check(Token.Tipo.GT) || check(Token.Tipo.LE) || check(Token.Tipo.GE)) {
            advance(); addExpr(); t = Token.Tipo.BOOL;
        }
        return t;
    }

    private Token.Tipo addExpr() {
        Token.Tipo t = mulExpr();
        while (check(Token.Tipo.PLUS) || check(Token.Tipo.MINUS)) {
            Token op = advance();
            Token.Tipo d = mulExpr();
            if (op.tipo == Token.Tipo.PLUS && (t == Token.Tipo.STRING || d == Token.Tipo.STRING)) t = Token.Tipo.STRING;
            else if (t == Token.Tipo.FLOAT || d == Token.Tipo.FLOAT) t = Token.Tipo.FLOAT;
            else if (t == Token.Tipo.INT && d == Token.Tipo.INT) t = Token.Tipo.INT;
            else t = null;
        }
        return t;
    }

    private Token.Tipo mulExpr() {
        Token.Tipo t = unaryExpr();
        while (check(Token.Tipo.STAR) || check(Token.Tipo.SLASH)) {
            advance();
            Token.Tipo d = unaryExpr();
            if (t == Token.Tipo.FLOAT || d == Token.Tipo.FLOAT) t = Token.Tipo.FLOAT;
            else if (t == Token.Tipo.INT && d == Token.Tipo.INT) t = Token.Tipo.INT;
            else t = null;
        }
        return t;
    }

    private Token.Tipo unaryExpr() {
        if (match(Token.Tipo.NOT)) { Token.Tipo t = unaryExpr(); return t == Token.Tipo.BOOL ? Token.Tipo.BOOL : null; }
        if (match(Token.Tipo.MINUS)) return unaryExpr();
        return primario();
    }

    private Token.Tipo primario() {
        if (check(Token.Tipo.LIT_INT))    { advance(); return Token.Tipo.INT; }
        if (check(Token.Tipo.LIT_FLOAT))  { advance(); return Token.Tipo.FLOAT; }
        if (check(Token.Tipo.LIT_STRING)) { advance(); return Token.Tipo.STRING; }
        if (check(Token.Tipo.TRUE) || check(Token.Tipo.FALSE)) { advance(); return Token.Tipo.BOOL; }

        if (check(Token.Tipo.IDENT) && peek2().tipo == Token.Tipo.LPAREN) return parseLlamada();

        if (check(Token.Tipo.IDENT)) {
            Token t = advance();
            VarInfo vi = vars.get(t.lex);
            if (vi == null) {
                errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: variable '" + t.lex + "' no declarada");
                if (match(Token.Tipo.LBRACKET)) { expr(); expect(Token.Tipo.RBRACKET, "se esperaba ']'"); }
                return null;
            }
            if (match(Token.Tipo.LBRACKET)) {
                if (!vi.arr) errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: '" + t.lex + "' no es un arreglo");
                Token.Tipo ti = expr();
                if (ti != null && ti != Token.Tipo.INT) errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: el indice debe ser int");
                expect(Token.Tipo.RBRACKET, "se esperaba ']'");
            }
            return vi.tipo;
        }

        if (match(Token.Tipo.LPAREN)) {
            Token.Tipo t = expr();
            expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar la expresion");
            return t;
        }

        Token t = peek();
        if (t.tipo != Token.Tipo.EOF && t.tipo != Token.Tipo.SEMI && t.tipo != Token.Tipo.RBRACE &&
            t.tipo != Token.Tipo.RPAREN && t.tipo != Token.Tipo.RBRACKET && t.tipo != Token.Tipo.COMMA) {
            errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: expresion invalida '" + t.lex + "'");
            advance();
        }
        return null;
    }

    private boolean compat(Token.Tipo decl, Token.Tipo val) {
        if (decl == val) return true;
        if (decl == Token.Tipo.FLOAT && val == Token.Tipo.INT) return true;
        return false;
    }

    private String nomTipo(Token.Tipo t) {
        if (t == null) return "desconocido";
        switch (t) {
            case INT:    return "int";
            case FLOAT:  return "float";
            case STRING: return "string";
            case BOOL:   return "bool";
            default:     return t.toString().toLowerCase();
        }
    }

    private void sincronizar() {
        while (!check(Token.Tipo.SEMI) && !check(Token.Tipo.RBRACE) && !check(Token.Tipo.EOF)) advance();
        if (check(Token.Tipo.SEMI)) advance();
    }
}