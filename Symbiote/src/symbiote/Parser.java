package symbiote;

import java.util.*;

public class Parser {

    private final List<Token>  toks;
    private       int          pos = 0;
    private final List<String> errs = new ArrayList<>();
    private       Programa     ast;

    public Parser(List<Token> toks) { this.toks = toks; }

    public List<String> getErrores() { return errs; }
    public Programa getAst() { return ast; }

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
        ast = new Programa();
        expect(Token.Tipo.ENTRY, "el programa debe iniciar con 'ITS DANGEROUS TO GO ALONE, TAKE THIS'");
        ast.cuerpo = parseBloque();
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

    private FnDecl parseFn() {
        Token f = advance();
        FnDecl n = new FnDecl();
        n.ln = f.ln; n.col = f.col;
        if (!check(Token.Tipo.IDENT)) {
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba nombre de funcion");
            sincronizar();
            return n;
        }
        n.nombre = advance().lex;
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues del nombre de la funcion");
        if (!check(Token.Tipo.RPAREN)) {
            do {
                Param pr = new Param();
                if (!check(Token.Tipo.IDENT)) {
                    errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba nombre de parametro");
                    break;
                }
                pr.nombre = advance().lex;
                expect(Token.Tipo.BE, "se esperaba 'be' despues del parametro '" + pr.nombre + "'");
                pr.tipo = parseTipo();
                if (match(Token.Tipo.LBRACKET)) { expect(Token.Tipo.RBRACKET, "se esperaba ']'"); pr.arr = true; }
                n.params.add(pr);
            } while (match(Token.Tipo.COMMA));
        }
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar los parametros");
        expect(Token.Tipo.ARROW, "se esperaba '->' seguido del tipo de retorno");
        n.retTipo = parseTipo();
        n.cuerpo = parseBloque();
        return n;
    }

    private Bloque parseBloque() {
        Token ab = peek();
        Bloque b = new Bloque();
        b.ln = ab.ln; b.col = ab.col;
        expect(Token.Tipo.LBRACE, "se esperaba '{' para abrir el bloque");
        while (!check(Token.Tipo.RBRACE) && !check(Token.Tipo.EOF)) b.sentencias.add(parseSentencia());
        expect(Token.Tipo.RBRACE, "se esperaba '}' para cerrar el bloque");
        return b;
    }

    private Nodo parseSentencia() {
        if (check(Token.Tipo.ERROR)) { advance(); return new Bloque(); }
        if (check(Token.Tipo.LET)) return parseLet();
        if (check(Token.Tipo.MAKE)) return parseFn();
        if (check(Token.Tipo.IDENT) && (peek2().tipo == Token.Tipo.ASSIGN || peek2().tipo == Token.Tipo.LBRACKET)) return parseAsigna();
        if (check(Token.Tipo.IDENT) && peek2().tipo == Token.Tipo.LPAREN) {
            Llamada ll = parseLlamada();
            expect(Token.Tipo.SEMI, "se esperaba ';'");
            return ll;
        }
        if (check(Token.Tipo.IF))     return parseIf();
        if (check(Token.Tipo.WHILE))  return parseWhile();
        if (check(Token.Tipo.FOR))    return parseFor();
        if (check(Token.Tipo.IN))     return parseIn();
        if (check(Token.Tipo.EMIT))   return parseEmit();
        if (check(Token.Tipo.RETURN)) return parseReturn();
        if (check(Token.Tipo.LBRACE)) return parseBloque();
        Token t = peek();
        errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: sentencia desconocida '" + t.lex + "'");
        advance();
        return new Bloque();
    }

    private LetDecl parseLet() {
        Token kw = advance();
        LetDecl n = new LetDecl();
        n.ln = kw.ln; n.col = kw.col;
        if (!check(Token.Tipo.IDENT)) {
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba nombre de variable despues de 'let'");
            sincronizar();
            return n;
        }
        Token nom = advance();
        n.nombre = nom.lex; n.ln = nom.ln; n.col = nom.col;
        expect(Token.Tipo.BE, "se esperaba 'be' despues de '" + n.nombre + "'");
        n.tipo = parseTipo();
        if (match(Token.Tipo.LBRACKET)) { expect(Token.Tipo.RBRACKET, "se esperaba ']'"); n.arr = true; }

        if (match(Token.Tipo.ASSIGN)) {
            if (n.arr) {
                n.valoresArr = new ArrayList<>();
                expect(Token.Tipo.LBRACKET, "se esperaba '[' para inicializar el arreglo");
                while (!check(Token.Tipo.RBRACKET) && !check(Token.Tipo.EOF)) {
                    n.valoresArr.add(expr());
                    if (!check(Token.Tipo.RBRACKET)) expect(Token.Tipo.COMMA, "se esperaba ',' entre elementos del arreglo");
                }
                expect(Token.Tipo.RBRACKET, "se esperaba ']' para cerrar el arreglo");
            } else {
                n.valor = expr();
            }
        }
        expect(Token.Tipo.SEMI, "se esperaba ';' al final de la declaracion de '" + n.nombre + "'");
        return n;
    }

    private Asigna parseAsigna() {
        Token nom = advance();
        Asigna n = new Asigna();
        n.nombre = nom.lex; n.ln = nom.ln; n.col = nom.col;
        if (match(Token.Tipo.LBRACKET)) {
            n.indice = expr();
            expect(Token.Tipo.RBRACKET, "se esperaba ']'");
        }
        expect(Token.Tipo.ASSIGN, "se esperaba '=' en la asignacion de '" + n.nombre + "'");
        n.valor = expr();
        expect(Token.Tipo.SEMI, "se esperaba ';' al final de la asignacion de '" + n.nombre + "'");
        return n;
    }

    private Llamada parseLlamada() {
        Token nom = advance();
        Llamada n = new Llamada();
        n.nombre = nom.lex; n.ln = nom.ln; n.col = nom.col;
        advance();
        if (!check(Token.Tipo.RPAREN)) {
            do { n.args.add(expr()); } while (match(Token.Tipo.COMMA));
        }
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar la llamada a '" + n.nombre + "'");
        return n;
    }

    private Si parseIf() {
        Token kw = advance();
        Si n = new Si();
        n.ln = kw.ln; n.col = kw.col;
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues de 'if'");
        n.cond = expr();
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar la condicion");
        n.entonces = parseBloque();
        if (match(Token.Tipo.ELSE)) {
            n.sino = check(Token.Tipo.IF) ? parseIf() : parseBloque();
        }
        return n;
    }

    private Mientras parseWhile() {
        Token kw = advance();
        Mientras n = new Mientras();
        n.ln = kw.ln; n.col = kw.col;
        n.cond = expr();
        n.cuerpo = parseBloque();
        return n;
    }

    private Para parseFor() {
        Token kw = advance();
        Para n = new Para();
        n.ln = kw.ln; n.col = kw.col;
        if (!check(Token.Tipo.IDENT)) {
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba el nombre del contador despues de 'for'");
            sincronizar();
            return n;
        }
        n.variable = advance().lex;
        expect(Token.Tipo.IN, "se esperaba 'in' despues del contador de 'for'");
        n.inicio = expr();
        expect(Token.Tipo.TO, "se esperaba 'to' entre los dos extremos de 'for'");
        n.fin = expr();
        n.cuerpo = parseBloque();
        return n;
    }

    private Emitir parseEmit() {
        Token kw = advance();
        Emitir n = new Emitir();
        n.ln = kw.ln; n.col = kw.col;
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues de 'emit'");
        n.valor = expr();
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar 'emit'");
        expect(Token.Tipo.SEMI, "se esperaba ';' despues de 'emit'");
        return n;
    }
    
    private Entrada parseIn() {
        Token kw = advance();
        Entrada n = new Entrada();
        n.ln = kw.ln; n.col = kw.col;
        expect(Token.Tipo.LPAREN, "se esperaba '(' despues de 'in'");
        if (!check(Token.Tipo.IDENT)) {
            errs.add("Error sintactico [L" + peek().ln + ":C" + peek().col + "]: se esperaba nombre de variable en 'in'");
            sincronizar();
            return n;
        }
        n.variable = advance().lex;
        if (match(Token.Tipo.LBRACKET)) {
            n.indice = expr();
            expect(Token.Tipo.RBRACKET, "se esperaba ']'");
        }
        expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar 'in'");
        expect(Token.Tipo.SEMI, "se esperaba ';' despues de 'in'");
        return n;
    }

    private Retorno parseReturn() {
        Token kw = advance();
        Retorno n = new Retorno();
        n.ln = kw.ln; n.col = kw.col;
        if (!check(Token.Tipo.SEMI)) n.valor = expr();
        expect(Token.Tipo.SEMI, "se esperaba ';' despues de 'return'");
        return n;
    }

    private Expr expr() {
        Expr e = orExpr();
        while (check(Token.Tipo.AS)) {
            Token kw = advance();
            Token.Tipo destino = parseTipo();
            Cast c = new Cast();
            c.expr = e; c.destino = destino; c.ln = kw.ln; c.col = kw.col;
            e = c;
        }
        return e;
    }

    private Expr orExpr() {
        Expr t = andExpr();
        while (check(Token.Tipo.OR)) { Token op = advance(); Expr d = andExpr(); t = bin(op, t, d); }
        return t;
    }

    private Expr andExpr() {
        Expr t = eqExpr();
        while (check(Token.Tipo.AND)) { Token op = advance(); Expr d = eqExpr(); t = bin(op, t, d); }
        return t;
    }

    private Expr eqExpr() {
        Expr t = cmpExpr();
        while (check(Token.Tipo.EQ) || check(Token.Tipo.NEQ)) { Token op = advance(); Expr d = cmpExpr(); t = bin(op, t, d); }
        return t;
    }

    private Expr cmpExpr() {
        Expr t = addExpr();
        while (check(Token.Tipo.LT) || check(Token.Tipo.GT) || check(Token.Tipo.LE) || check(Token.Tipo.GE)) {
            Token op = advance(); Expr d = addExpr(); t = bin(op, t, d);
        }
        return t;
    }

    private Expr addExpr() {
        Expr t = mulExpr();
        while (check(Token.Tipo.PLUS) || check(Token.Tipo.MINUS)) { Token op = advance(); Expr d = mulExpr(); t = bin(op, t, d); }
        return t;
    }

    private Expr mulExpr() {
        Expr t = unaryExpr();
        while (check(Token.Tipo.STAR) || check(Token.Tipo.SLASH)) { Token op = advance(); Expr d = unaryExpr(); t = bin(op, t, d); }
        return t;
    }

    private Expr bin(Token op, Expr izq, Expr der) {
        Binaria b = new Binaria();
        b.op = op.tipo; b.izq = izq; b.der = der; b.ln = op.ln; b.col = op.col;
        return b;
    }

    private Expr unaryExpr() {
        if (check(Token.Tipo.NOT) || check(Token.Tipo.MINUS)) {
            Token op = advance();
            Unaria u = new Unaria();
            u.op = op.tipo; u.expr = unaryExpr(); u.ln = op.ln; u.col = op.col;
            return u;
        }
        return primario();
    }

    private Expr primario() {
        if (check(Token.Tipo.LIT_INT) || check(Token.Tipo.LIT_FLOAT) || check(Token.Tipo.LIT_STRING) ||
            check(Token.Tipo.TRUE) || check(Token.Tipo.FALSE)) {
            Token t = advance();
            Literal l = new Literal();
            l.tipo = t.tipo; l.valor = t.lex; l.ln = t.ln; l.col = t.col;
            return l;
        }

        if (check(Token.Tipo.IDENT) && peek2().tipo == Token.Tipo.LPAREN) return parseLlamada();

        if (check(Token.Tipo.IDENT)) {
            Token t = advance();
            Variable v = new Variable();
            v.nombre = t.lex; v.ln = t.ln; v.col = t.col;
            if (match(Token.Tipo.LBRACKET)) {
                v.indice = expr();
                expect(Token.Tipo.RBRACKET, "se esperaba ']'");
            }
            return v;
        }

        if (match(Token.Tipo.LPAREN)) {
            Expr t = expr();
            expect(Token.Tipo.RPAREN, "se esperaba ')' para cerrar la expresion");
            return t;
        }

        Token t = peek();
        if (t.tipo != Token.Tipo.EOF && t.tipo != Token.Tipo.SEMI && t.tipo != Token.Tipo.RBRACE &&
            t.tipo != Token.Tipo.RPAREN && t.tipo != Token.Tipo.RBRACKET && t.tipo != Token.Tipo.COMMA) {
            errs.add("Error sintactico [L" + t.ln + ":C" + t.col + "]: expresion invalida '" + t.lex + "'");
            advance();
        }
        Literal dummy = new Literal();
        dummy.ln = t.ln; dummy.col = t.col;
        return dummy;
    }

    private void sincronizar() {
        while (!check(Token.Tipo.SEMI) && !check(Token.Tipo.RBRACE) && !check(Token.Tipo.EOF)) advance();
        if (check(Token.Tipo.SEMI)) advance();
    }
}