package symbiote;

public class Token {

    public enum Tipo {
        ENTRY,
        LET, BE, IF, ELSE, WHILE, FOR, FN, RETURN, EMIT,
        INT, FLOAT, STRING, BOOL,
        TRUE, FALSE,
        LIT_INT, LIT_FLOAT, LIT_STRING,
        IDENT,
        PLUS, MINUS, STAR, SLASH,
        ASSIGN, EQ, NEQ, LT, GT, LE, GE,
        AND, OR, NOT,
        ARROW,
        LBRACE, RBRACE, LPAREN, RPAREN, LBRACKET, RBRACKET,
        SEMI, COMMA,
        ERROR, EOF
    }

    public final Tipo   tipo;
    public final String lex;
    public final int    ln;
    public final int    col;

    public Token(Tipo tipo, String lex, int ln, int col) {
        this.tipo = tipo;
        this.lex  = lex;
        this.ln   = ln;
        this.col  = col;
    }

    public String display() {
        switch (tipo) {
            case IDENT:      return "ID(" + lex + ")";
            case LIT_INT:    return "INT(" + lex + ")";
            case LIT_FLOAT:  return "FLOAT(" + lex + ")";
            case LIT_STRING: return "STR(" + lex + ")";
            case ERROR:      return "ERROR(" + lex + ")";
            case EOF:        return "EOF";
            default:         return lex;
        }
    }

    public String categoria() {
        switch (tipo) {
            case ENTRY:
                return "Entrada";
            case LET: case BE: case IF: case ELSE: case WHILE:
            case FOR: case FN: case RETURN: case EMIT:
                return "Palabra clave";
            case INT: case FLOAT: case STRING: case BOOL:
                return "Tipo de dato";
            case TRUE: case FALSE:
                return "Literal booleana";
            case LIT_INT:
                return "Literal entera";
            case LIT_FLOAT:
                return "Literal decimal";
            case LIT_STRING:
                return "Literal cadena";
            case IDENT:
                return "Identificador";
            case PLUS: case MINUS: case STAR: case SLASH:
                return "Operador aritmetico";
            case ASSIGN:
                return "Operador de asignacion";
            case EQ: case NEQ: case LT: case GT: case LE: case GE:
                return "Operador relacional";
            case AND: case OR: case NOT:
                return "Operador logico";
            case ARROW:
                return "Delimitador";
            case LBRACE: case RBRACE: case LPAREN: case RPAREN: case SEMI:
                return "Delimitador";
            case LBRACKET: case RBRACKET: case COMMA:
                return "Arreglo";
            case ERROR:
                return "Error lexico";
            default:
                return "Desconocido";
        }
    }
}