package symbiote;

import java.util.*;

public class Semantico {

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

    private final List<String>        errs = new ArrayList<>();
    private       Map<String, VarInfo> vars = new LinkedHashMap<>();
    private final Map<String, FnInfo>  fns  = new LinkedHashMap<>();

    public List<String> getErrores() { return errs; }

    public void analizar(Programa p) {
        errs.clear();
        vars = new LinkedHashMap<>();
        fns.clear();
        if (p == null || p.cuerpo == null) return;
        registrarFns(p.cuerpo);
        bloque(p.cuerpo);
    }

    private void registrarFns(Bloque b) {
        for (Nodo n : b.sentencias) {
            if (n instanceof FnDecl) {
                FnDecl f = (FnDecl) n;
                List<Token.Tipo> pts = new ArrayList<>();
                for (Param pr : f.params) pts.add(pr.tipo);
                fns.put(f.nombre, new FnInfo(pts, f.retTipo));
            }
        }
    }

    private void bloque(Bloque b) {
        if (b == null) return;
        for (Nodo n : b.sentencias) sentencia(n);
    }

    private void sentencia(Nodo n) {
        if (n instanceof FnDecl)   { fnDecl((FnDecl) n); return; }
        if (n instanceof LetDecl)  { let((LetDecl) n); return; }
        if (n instanceof Asigna)   { asigna((Asigna) n); return; }
        if (n instanceof Llamada)  { tipoLlamada((Llamada) n); return; }
        if (n instanceof Si)       { si((Si) n); return; }
        if (n instanceof Entrada)  { entrada((Entrada) n); return; }
        if (n instanceof Mientras) { mientras((Mientras) n); return; }
        if (n instanceof Para)     { para((Para) n); return; }
        if (n instanceof Emitir)   { emitir((Emitir) n); return; }
        if (n instanceof Retorno)  { retorno((Retorno) n); return; }
        if (n instanceof Bloque)   { bloque((Bloque) n); return; }
    }

    private void fnDecl(FnDecl f) {
        List<Token.Tipo> pts = new ArrayList<>();
        for (Param pr : f.params) pts.add(pr.tipo);
        fns.putIfAbsent(f.nombre, new FnInfo(pts, f.retTipo));

        List<String> nombresVistos = new ArrayList<>();
        for (Param pr : f.params) {
            if (nombresVistos.contains(pr.nombre))
                errs.add("Nope [L" + f.ln + ":C" + f.col + "]: el parametro '" + pr.nombre + "' esta repetido en la funcion '" + f.nombre + "'");
            nombresVistos.add(pr.nombre);
        }

        Map<String, VarInfo> exterior = vars;
        vars = new LinkedHashMap<>(exterior);
        for (Param pr : f.params) vars.put(pr.nombre, new VarInfo(pr.tipo, pr.arr));
        registrarFns(f.cuerpo);
        bloque(f.cuerpo);
        vars = exterior;
    }

    private void let(LetDecl n) {
        if (n.nombre == null) return;
        if (vars.containsKey(n.nombre))
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: variable '" + n.nombre + "' ya declarada");
        else
            vars.put(n.nombre, new VarInfo(n.tipo, n.arr));

        if (n.arr && n.valoresArr != null) {
            for (Expr e : n.valoresArr) {
                Token.Tipo tv = tipo(e);
                if (n.tipo != null && tv != null && !compat(n.tipo, tv))
                    errs.add("Nope [L" + n.ln + ":C" + n.col + "]: elemento de arreglo incompatible con " + nomTipo(n.tipo));
            }
        } else if (n.valor != null) {
            Token.Tipo tv = tipo(n.valor);
            if (n.tipo != null && tv != null && !compat(n.tipo, tv))
                errs.add("Nope [L" + n.ln + ":C" + n.col + "]: tipo incompatible — '" + n.nombre + "' es " + nomTipo(n.tipo) + " pero se asigna " + nomTipo(tv));
        }
    }

    private void asigna(Asigna n) {
        VarInfo info = vars.get(n.nombre);
        if (info == null)
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: variable '" + n.nombre + "' no declarada");

        if (n.indice != null) {
            if (info != null && !info.arr)
                errs.add("Nope [L" + n.ln + ":C" + n.col + "]: '" + n.nombre + "' no es un arreglo");
            Token.Tipo ti = tipo(n.indice);
            if (ti != null && ti != Token.Tipo.INT)
                errs.add("Nope [L" + n.ln + ":C" + n.col + "]: el indice debe ser int");
        }

        Token.Tipo tv = tipo(n.valor);
        if (info != null && tv != null && !compat(info.tipo, tv))
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: tipo incompatible — '" + n.nombre + "' es " + nomTipo(info.tipo) + " pero se asigna " + nomTipo(tv));
    }

    private void si(Si n) {
        Token.Tipo tc = tipo(n.cond);
        if (tc != null && tc != Token.Tipo.BOOL)
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: la condicion de 'if' debe ser bool");
        bloque(n.entonces);
        if (n.sino instanceof Bloque) bloque((Bloque) n.sino);
        else if (n.sino instanceof Si) si((Si) n.sino);
    }

    private void mientras(Mientras n) {
        Token.Tipo tc = tipo(n.cond);
        if (tc != null && tc != Token.Tipo.BOOL)
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: la condicion de 'while' debe ser bool");
        bloque(n.cuerpo);
    }

    private void para(Para n) {
        if (n.variable != null) {
            if (vars.containsKey(n.variable))
                errs.add("Nope [L" + n.ln + ":C" + n.col + "]: variable '" + n.variable + "' ya declarada");
            else
                vars.put(n.variable, new VarInfo(Token.Tipo.INT, false));
        }
        Token.Tipo ti = tipo(n.inicio);
        Token.Tipo tf = tipo(n.fin);
        if (ti != null && ti != Token.Tipo.INT)
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: el inicio de 'for' debe ser int");
        if (tf != null && tf != Token.Tipo.INT)
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: el final de 'for' debe ser int");
        bloque(n.cuerpo);
    }

    private void emitir(Emitir n) { tipo(n.valor); }

    private void retorno(Retorno n) { if (n.valor != null) tipo(n.valor); }

    private Token.Tipo tipoLlamada(Llamada n) {
        FnInfo fi = fns.get(n.nombre);
        if (fi == null)
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: funcion '" + n.nombre + "' no declarada");
        for (int i = 0; i < n.args.size(); i++) {
            Token.Tipo ta = tipo(n.args.get(i));
            if (fi != null && i < fi.params.size() && ta != null && !compat(fi.params.get(i), ta))
                errs.add("Nope [L" + n.ln + ":C" + n.col + "]: argumento " + (i + 1) + " incompatible en la llamada a '" + n.nombre + "'");
        }
        if (fi != null && n.args.size() != fi.params.size())
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: '" + n.nombre + "' espera " + fi.params.size() + " argumento(s), se dieron " + n.args.size());
        return fi != null ? fi.ret : null;
    }

    private Token.Tipo tipo(Expr e) {
        if (e == null) return null;

        if (e instanceof Literal) {
            Token.Tipo t = ((Literal) e).tipo;
            if (t == Token.Tipo.LIT_INT) return Token.Tipo.INT;
            if (t == Token.Tipo.LIT_FLOAT) return Token.Tipo.FLOAT;
            if (t == Token.Tipo.LIT_STRING) return Token.Tipo.STRING;
            if (t == Token.Tipo.TRUE || t == Token.Tipo.FALSE) return Token.Tipo.BOOL;
            return null;
        }

        if (e instanceof Variable) {
            Variable v = (Variable) e;
            VarInfo vi = vars.get(v.nombre);
            if (vi == null) {
                errs.add("Nope [L" + v.ln + ":C" + v.col + "]: variable '" + v.nombre + "' no declarada");
                if (v.indice != null) tipo(v.indice);
                return null;
            }
            if (v.indice != null) {
                if (!vi.arr) errs.add("Nope [L" + v.ln + ":C" + v.col + "]: '" + v.nombre + "' no es un arreglo");
                Token.Tipo ti = tipo(v.indice);
                if (ti != null && ti != Token.Tipo.INT) errs.add("Nope [L" + v.ln + ":C" + v.col + "]: el indice debe ser int");
            }
            return vi.tipo;
        }

        if (e instanceof Llamada) return tipoLlamada((Llamada) e);

        if (e instanceof Cast) {
            Cast c = (Cast) e;
            tipo(c.expr);
            if (c.destino == null) return null;
            return c.destino;
        }

        if (e instanceof Unaria) {
            Unaria u = (Unaria) e;
            Token.Tipo t = tipo(u.expr);
            if (u.op == Token.Tipo.NOT) {
                if (t != null && t != Token.Tipo.BOOL)
                    errs.add("Nope [L" + u.ln + ":C" + u.col + "]: el operador '!' requiere bool, se uso " + nomTipo(t));
                return Token.Tipo.BOOL;
            }
            if (t != null && t != Token.Tipo.INT && t != Token.Tipo.FLOAT) {
                errs.add("Nope [L" + u.ln + ":C" + u.col + "]: el operador unario '-' no es valido para " + nomTipo(t));
                return null;
            }
            return t;
        }

        if (e instanceof Binaria) {
            Binaria b = (Binaria) e;
            Token.Tipo izq = tipo(b.izq);
            Token.Tipo der = tipo(b.der);
            boolean sabemos = izq != null && der != null;
            switch (b.op) {
                case PLUS:
                    if (!sabemos) return null;
                    if (izq == Token.Tipo.INT && der == Token.Tipo.INT) return Token.Tipo.INT;
                    if (esNumerico(izq) && esNumerico(der)) return Token.Tipo.FLOAT;
                    if (izq == Token.Tipo.STRING && der == Token.Tipo.STRING) return Token.Tipo.STRING;
                    errs.add("Nope [L" + b.ln + ":C" + b.col + "]: operacion '+' invalida entre " + nomTipo(izq) + " y " + nomTipo(der));
                    return null;
                case MINUS: case STAR: case SLASH:
                    if (!sabemos) return null;
                    if (izq == Token.Tipo.INT && der == Token.Tipo.INT) return Token.Tipo.INT;
                    if (esNumerico(izq) && esNumerico(der)) return Token.Tipo.FLOAT;
                    errs.add("Nope [L" + b.ln + ":C" + b.col + "]: operacion '" + simboloOp(b.op) + "' invalida entre " + nomTipo(izq) + " y " + nomTipo(der));
                    return null;
                case EQ: case NEQ:
                    if (sabemos && !((esNumerico(izq) && esNumerico(der)) || izq == der))
                        errs.add("Nope [L" + b.ln + ":C" + b.col + "]: no se puede comparar " + nomTipo(izq) + " con " + nomTipo(der));
                    return Token.Tipo.BOOL;
                case LT: case GT: case LE: case GE:
                    if (sabemos && !(esNumerico(izq) && esNumerico(der)))
                        errs.add("Nope [L" + b.ln + ":C" + b.col + "]: la comparacion '" + simboloOp(b.op) + "' requiere valores numericos, se uso " + nomTipo(izq) + " y " + nomTipo(der));
                    return Token.Tipo.BOOL;
                case AND: case OR:
                    if (sabemos && !(izq == Token.Tipo.BOOL && der == Token.Tipo.BOOL))
                        errs.add("Nope [L" + b.ln + ":C" + b.col + "]: el operador '" + simboloOp(b.op) + "' requiere bool, se uso " + nomTipo(izq) + " y " + nomTipo(der));
                    return Token.Tipo.BOOL;
                default:
                    return null;
            }
        }

        return null;
    }

    private boolean esNumerico(Token.Tipo t) { return t == Token.Tipo.INT || t == Token.Tipo.FLOAT; }

    private String simboloOp(Token.Tipo op) {
        switch (op) {
            case MINUS: return "-";
            case STAR:  return "*";
            case SLASH: return "/";
            case LT:    return "<";
            case GT:    return ">";
            case LE:    return "<=";
            case GE:    return ">=";
            case AND:   return "&&";
            case OR:    return "||";
            default:    return op.toString();
        }
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
    
    private void entrada(Entrada n) {
        if (n.variable == null) return;
        VarInfo info = vars.get(n.variable);
        if (info == null) {
            errs.add("Nope [L" + n.ln + ":C" + n.col + "]: variable '" + n.variable + "' no declarada en 'in'");
            return;
        }
        if (n.indice != null) {
            if (!info.arr)
                errs.add("Nope [L" + n.ln + ":C" + n.col + "]: '" + n.variable + "' no es un arreglo");
            Token.Tipo ti = tipo(n.indice);
            if (ti != null && ti != Token.Tipo.INT)
                errs.add("Nope [L" + n.ln + ":C" + n.col + "]: el indice debe ser int");
        }
    }
}