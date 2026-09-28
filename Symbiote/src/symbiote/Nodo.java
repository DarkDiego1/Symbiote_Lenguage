package symbiote;

import java.util.*;

public abstract class Nodo {
    public int ln, col;
}

abstract class Expr extends Nodo {}

class Param {
    String     nombre;
    Token.Tipo tipo;
    boolean    arr;
}

class Programa extends Nodo {
    Bloque cuerpo;
}

class FnDecl extends Nodo {
    String       nombre;
    List<Param>  params = new ArrayList<>();
    Token.Tipo   retTipo;
    Bloque       cuerpo;
}

class Bloque extends Nodo {
    List<Nodo> sentencias = new ArrayList<>();
}

class LetDecl extends Nodo {
    String     nombre;
    Token.Tipo tipo;
    boolean    arr;
    Expr       valor;
    List<Expr> valoresArr;
}

class Asigna extends Nodo {
    String nombre;
    Expr   indice;
    Expr   valor;
}

class Llamada extends Expr {
    String     nombre;
    List<Expr> args = new ArrayList<>();
}

class Si extends Nodo {
    Expr   cond;
    Bloque entonces;
    Nodo   sino;
}

class Mientras extends Nodo {
    Expr   cond;
    Bloque cuerpo;
}

class Para extends Nodo {
    LetDecl init;
    Expr    cond;
    Asigna  incremento;
    Bloque  cuerpo;
}

class Emitir extends Nodo {
    Expr valor;
}

class Retorno extends Nodo {
    Expr valor;
}

class Literal extends Expr {
    Token.Tipo tipo;
    String     valor;
}

class Variable extends Expr {
    String nombre;
    Expr   indice;
}

class Binaria extends Expr {
    Token.Tipo op;
    Expr       izq, der;
}

class Unaria extends Expr {
    Token.Tipo op;
    Expr       expr;
}