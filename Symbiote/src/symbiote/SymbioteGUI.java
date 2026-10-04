package symbiote;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;

public class SymbioteGUI extends JFrame {

    private boolean oscuro = true;

    private Color bgBase, bgPanel, bgCard, bgEditor, bgRowA, bgRowB, bgHeader;
    private Color textPri, textMut, textCode;
    private Color divider, borde, selBg, errBg, errFg, warnFg;

    private static final Color ACC   = new Color(134, 97, 255);
    private static final Color ACC2  = new Color(35, 196, 170);
    private static final Color OK    = new Color(70, 195, 120);
    private static final Color BAD   = new Color(225, 85, 75);

    private static final Font F_TITLE = new Font("SansSerif", Font.BOLD, 21);
    private static final Font F_SUB   = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font F_UI    = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_UIB   = new Font("SansSerif", Font.BOLD, 12);
    private static final Font F_CODE  = new Font("Monospaced", Font.PLAIN, 14);
    private static final Font F_TABLE = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font F_BADGE = new Font("SansSerif", Font.BOLD, 10);

    private JPanel            root;
    private JTextArea         editor, lineNums;
    private JTable            tabla;
    private DefaultTableModel modelo;
    private JTextArea         errLex, errSin, errSem;
    private JLabel            lblEstado, lblConteo;
    private JTabbedPane       tabs;

    public SymbioteGUI() {
        setTitle("Symbiote — Analizador");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 820);
        setMinimumSize(new Dimension(1040, 680));
        setLocationRelativeTo(null);
        aplicarTema();
        construir();
        setVisible(true);
    }

    private void aplicarTema() {
        if (oscuro) {
            bgBase   = new Color(24, 25, 30);
            bgPanel  = new Color(31, 33, 39);
            bgCard   = new Color(37, 39, 46);
            bgEditor = new Color(27, 28, 34);
            bgRowA   = new Color(33, 35, 41);
            bgRowB   = new Color(38, 40, 47);
            bgHeader = new Color(29, 30, 36);
            textPri  = new Color(228, 229, 234);
            textMut  = new Color(140, 143, 153);
            textCode = new Color(220, 222, 228);
            divider  = new Color(48, 50, 58);
            borde    = new Color(52, 54, 63);
            selBg    = new Color(70, 58, 120);
            errBg    = new Color(48, 32, 34);
            errFg    = new Color(235, 110, 100);
            warnFg   = new Color(230, 170, 80);
        } else {
            bgBase   = new Color(245, 245, 248);
            bgPanel  = new Color(255, 255, 255);
            bgCard   = new Color(255, 255, 255);
            bgEditor = new Color(250, 250, 252);
            bgRowA   = new Color(255, 255, 255);
            bgRowB   = new Color(246, 246, 250);
            bgHeader = new Color(255, 255, 255);
            textPri  = new Color(30, 32, 38);
            textMut  = new Color(110, 114, 124);
            textCode = new Color(35, 37, 44);
            divider  = new Color(228, 229, 235);
            borde    = new Color(222, 224, 230);
            selBg    = new Color(228, 220, 250);
            errBg    = new Color(253, 236, 235);
            errFg    = new Color(200, 55, 45);
            warnFg   = new Color(185, 120, 15);
        }
    }

    private void construir() {
        root = new JPanel(new BorderLayout());
        root.setBackground(bgBase);
        setContentPane(root);
        root.add(construirHeader(), BorderLayout.NORTH);
        root.add(construirCuerpo(), BorderLayout.CENTER);
        root.add(construirFooter(), BorderLayout.SOUTH);
        registrarAtajos();
    }

    private void reconstruir() {
        String actual = editor != null ? editor.getText() : "";
        aplicarTema();
        root.removeAll();
        root.setBackground(bgBase);
        root.add(construirHeader(), BorderLayout.NORTH);
        root.add(construirCuerpo(), BorderLayout.CENTER);
        root.add(construirFooter(), BorderLayout.SOUTH);
        registrarAtajos();
        editor.setText(actual);
        root.revalidate();
        root.repaint();
    }

    private JPanel construirHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(bgHeader);
        hdr.setPreferredSize(new Dimension(0, 68));
        hdr.setBorder(new MatteBorder(0, 0, 2, 0, ACC));

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new BoxLayout(marca, BoxLayout.Y_AXIS));
        marca.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        JLabel titulo = new JLabel("SYMBIOTE");
        titulo.setFont(F_TITLE);
        titulo.setForeground(ACC);
        JLabel sub = new JLabel("ANALIZADOR LEXICO Y SINTACTICO");
        sub.setFont(F_SUB);
        sub.setForeground(textMut);
        marca.add(Box.createVerticalGlue());
        marca.add(titulo);
        marca.add(sub);
        marca.add(Box.createVerticalGlue());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));
        acciones.add(boton("Ejemplo", null, this::cargarEjemplo, false));
        acciones.add(boton("Abrir", null, this::abrirArchivo, false));
        acciones.add(boton("Guardar", null, this::guardarArchivo, false));
        acciones.add(boton(oscuro ? "☀" : "☾", "Cambiar tema", this::alternarTema, false));
        acciones.add(Box.createHorizontalStrut(10));
        acciones.add(boton("Lexico  F5", null, e -> ejecutarLexico(true), true));
        acciones.add(boton("Sintactico  F6", null, e -> ejecutarSintactico(), true));
        acciones.add(boton("Semantico  F7", null, e -> ejecutarSemantico(), true));

        hdr.add(marca, BorderLayout.WEST);
        hdr.add(acciones, BorderLayout.EAST);
        return hdr;
    }

    private JButton boton(String txt, String tip, ActionListener al, boolean destacado) {
        Color bg = destacado ? ACC : bgCard;
        Color fg = destacado ? Color.WHITE : textPri;
        Color bd = destacado ? ACC : borde;

        JButton b = new JButton(txt) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color relleno = getModel().isPressed() ? bg.darker() : bg;
                g2.setColor(relleno);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(bd);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setFont(getFont());
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        b.setFont(F_UIB);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (tip != null) b.setToolTipText(tip);
        b.addActionListener(al);
        return b;
    }

    private JPanel construirCuerpo() {
        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 14, 8, 14));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelEditor(), panelResultados());
        split.setResizeWeight(0.5);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        cuerpo.add(split, BorderLayout.CENTER);
        return cuerpo;
    }

    private JPanel tarjeta(String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bgCard);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borde, 1, true),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel lbl = new JLabel(titulo.toUpperCase());
        lbl.setFont(F_SUB);
        lbl.setForeground(ACC2);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));
        p.add(lbl, BorderLayout.NORTH);
        return p;
    }

    private JPanel panelEditor() {
        JPanel p = tarjeta("Codigo fuente");

        editor = new JTextArea();
        editor.setFont(F_CODE);
        editor.setBackground(bgEditor);
        editor.setForeground(textCode);
        editor.setCaretColor(ACC);
        editor.setTabSize(4);
        editor.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        lineNums = new JTextArea("1");
        lineNums.setFont(F_CODE);
        lineNums.setBackground(bgEditor);
        lineNums.setForeground(textMut);
        lineNums.setEditable(false);
        lineNums.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 6));

        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { actualizarLineas(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { actualizarLineas(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarLineas(); }
        });

        JScrollPane sp = new JScrollPane(editor);
        sp.setRowHeaderView(lineNums);
        sp.setBorder(new LineBorder(divider, 1));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        estilizarBarra(sp.getVerticalScrollBar());

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private void actualizarLineas() {
        int n = editor.getLineCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= Math.max(n, 1); i++) sb.append(i).append("\n");
        lineNums.setText(sb.toString());
    }

    private JPanel panelResultados() {
        JPanel p = tarjeta("Resultados");

        tabs = new JTabbedPane();
        tabs.setFont(F_UIB);
        tabs.setBackground(bgCard);
        tabs.setForeground(textPri);

        modelo = new DefaultTableModel(new Object[]{"Linea", "Col", "Lexema", "Token", "Categoria"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setFont(F_TABLE);
        tabla.setRowHeight(24);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(selBg);
        tabla.setBackground(bgCard);
        tabla.getTableHeader().setFont(F_UIB);
        tabla.getTableHeader().setBackground(bgPanel);
        tabla.getTableHeader().setForeground(textMut);
        tabla.setDefaultRenderer(Object.class, new FilaRenderer());
        tabla.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(50);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new BadgeRenderer());
        JScrollPane spTabla = new JScrollPane(tabla);
        spTabla.setBorder(new LineBorder(divider, 1));
        estilizarBarra(spTabla.getVerticalScrollBar());

        errLex = areaErrores();
        errSin = areaErrores();
        errSem = areaErrores();

        tabs.addTab("Tokens", spTabla);
        tabs.addTab("Lexico", envolver(errLex));
        tabs.addTab("Sintactico", envolver(errSin));
        tabs.addTab("Semantico", envolver(errSem));
        tabs.addTab("Guia", envolver(areaGuia()));

        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    private JTextArea areaErrores() {
        JTextArea a = new JTextArea();
        a.setFont(F_TABLE);
        a.setEditable(false);
        a.setBackground(bgCard);
        a.setForeground(errFg);
        a.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return a;
    }

    private JTextArea areaGuia() {
        JTextArea a = new JTextArea(textoGuia());
        a.setFont(F_TABLE);
        a.setEditable(false);
        a.setBackground(bgCard);
        a.setForeground(textPri);
        a.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return a;
    }

    private JScrollPane envolver(JTextArea a) {
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(new LineBorder(divider, 1));
        estilizarBarra(sp.getVerticalScrollBar());
        return sp;
    }

    private JPanel construirFooter() {
        JPanel f = new JPanel(new BorderLayout());
        f.setBackground(bgHeader);
        f.setPreferredSize(new Dimension(0, 34));
        f.setBorder(new MatteBorder(1, 0, 0, 0, divider));

        lblEstado = new JLabel("  Listo");
        lblEstado.setFont(F_UI);
        lblEstado.setForeground(textMut);

        lblConteo = new JLabel("0 tokens  ");
        lblConteo.setFont(F_UI);
        lblConteo.setForeground(textMut);
        lblConteo.setHorizontalAlignment(SwingConstants.RIGHT);

        f.add(lblEstado, BorderLayout.WEST);
        f.add(lblConteo, BorderLayout.EAST);
        return f;
    }

    private void registrarAtajos() {
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke("F5"), "lex");
        am.put("lex", new AbstractAction() { public void actionPerformed(ActionEvent e) { ejecutarLexico(true); } });
        im.put(KeyStroke.getKeyStroke("F6"), "sin");
        am.put("sin", new AbstractAction() { public void actionPerformed(ActionEvent e) { ejecutarSintactico(); } });
        im.put(KeyStroke.getKeyStroke("F7"), "sem");
        am.put("sem", new AbstractAction() { public void actionPerformed(ActionEvent e) { ejecutarSemantico(); } });
    }

    private void alternarTema(ActionEvent e) { oscuro = !oscuro; reconstruir(); }

    private List<Token> ejecutarLexico(boolean mostrarTab) {
        Lexer lex = new Lexer(editor.getText());
        List<Token> toks = lex.analizar();

        modelo.setRowCount(0);
        for (Token t : toks) {
            if (t.tipo == Token.Tipo.EOF) continue;
            modelo.addRow(new Object[]{t.ln, t.col, t.lex, t.display(), t.categoria()});
        }

        List<String> errs = lex.getErrores();
        errLex.setText(errs.isEmpty() ? "Sin errores lexicos." : String.join("\n", errs));
        errLex.setForeground(errs.isEmpty() ? OK : errFg);

        lblConteo.setText((toks.size() - 1) + " tokens  ");
        if (mostrarTab) {
            tabs.setSelectedIndex(0);
            setEstado(errs.isEmpty() ? "Analisis lexico completado sin errores" : errs.size() + " error(es) lexico(s) encontrado(s)", errs.isEmpty());
        }
        return toks;
    }

    private Parser ejecutarSintactico() {
        List<Token> toks = ejecutarLexico(false);
        Parser p = new Parser(toks);
        p.parse();
        List<String> errs = p.getErrores();
        errSin.setText(errs.isEmpty() ? "Sin errores sintacticos." : String.join("\n", errs));
        errSin.setForeground(errs.isEmpty() ? OK : errFg);
        tabs.setSelectedIndex(2);
        setEstado(errs.isEmpty() ? "Analisis sintactico completado sin errores" : errs.size() + " error(es) sintactico(s) encontrado(s)", errs.isEmpty());
        return p;
    }

    private void ejecutarSemantico() {
        List<Token> toks = ejecutarLexico(false);
        Parser p = new Parser(toks);
        p.parse();

        Semantico s = new Semantico();
        s.analizar(p.getAst());
        List<String> errs = s.getErrores();
        errSem.setText(errs.isEmpty() ? "Sin errores semanticos." : String.join("\n", errs));
        errSem.setForeground(errs.isEmpty() ? OK : errFg);
        tabs.setSelectedIndex(3);
        setEstado(errs.isEmpty() ? "Analisis semantico completado sin errores" : errs.size() + " error(es) semantico(s) encontrado(s)", errs.isEmpty());
    }

    private void setEstado(String msg, boolean ok) {
        lblEstado.setText("  " + msg);
        lblEstado.setForeground(ok ? OK : BAD);
    }

    private void cargarEjemplo(ActionEvent e) {
        editor.setText(
            "ITS DANGEROUS TO GO ALONE, TAKE THIS\n" +
            "{\n" +
            "    make add(a be int, b be int) -> int {\n" +
            "        return a + b;\n" +
            "    }\n\n" +
            "    let scores be int[] = [10, 20, 30];\n" +
            "    let total be int = 0;\n\n" +
            "    for i in 0 to 2 {\n" +
            "        total = total + scores[i];\n" +
            "    }\n\n" +
            "    if (total > 50) {\n" +
            "        emit(\"High score!\");\n" +
            "    } else {\n" +
            "        emit(\"Keep trying.\");\n" +
            "    }\n\n" +
            "    let sum be int = add(total, 5);\n" +
            "    let msg be string = (sum as string) + \" puntos\";\n" +
            "    emit(msg);\n" +
            "}\n"
        );
        setEstado("Ejemplo cargado", true);
    }

    private void abrirArchivo(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Symbiote (*.txt)", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String contenido = new String(Files.readAllBytes(fc.getSelectedFile().toPath()));
                editor.setText(contenido);
                setEstado("Archivo abierto: " + fc.getSelectedFile().getName(), true);
            } catch (IOException ex) {
                setEstado("No se pudo abrir el archivo", false);
            }
        }
    }

    private void guardarArchivo(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Symbiote (*.sym)", "sym"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".sym")) f = new File(f.getParentFile(), f.getName() + ".sym");
            try (FileWriter w = new FileWriter(f)) {
                w.write(editor.getText());
                setEstado("Archivo guardado: " + f.getName(), true);
            } catch (IOException ex) {
                setEstado("No se pudo guardar el archivo", false);
            }
        }
    }

    private String textoGuia() {
        return
        "SYMBIOTE — GUIA RAPIDA\n" +
        "══════════════════════\n\n" +
        "INICIO DE PROGRAMA\n──────────────────\n" +
        "  ITS DANGEROUS TO GO ALONE, TAKE THIS\n" +
        "  { ...todo el programa va aqui, incluyendo funciones... }\n\n" +
        "VARIABLES\n─────────\n" +
        "  let nombre be tipo = valor;\n" +
        "  let lista be tipo[] = [v1, v2, v3];\n\n" +
        "TIPOS\n─────\n" +
        "  int      entero\n" +
        "  float    decimal\n" +
        "  string   cadena\n" +
        "  bool     true / false\n\n" +
        "FUNCIONES\n─────────\n" +
        "  make nombre(param be tipo, ...) -> tipo {\n" +
        "      return valor;\n" +
        "  }\n\n" +
        "CONTROL DE FLUJO\n────────────────\n" +
        "  if (cond) { ... } else { ... }\n" +
        "  while cond { ... }\n" +
        "  for i in inicio to fin { ... }   (incluye fin, i ya es int)\n\n" +
        "SALIDA\n──────\n" +
        "  emit(expresion);\n\n" +
        "OPERADORES\n──────────\n" +
        "  + - * /       aritmeticos\n" +
        "  = == !=       asignacion / igualdad\n" +
        "  < > <= >=     comparacion\n" +
        "  && || !       logicos\n\n" +
        "CONVERSION DE TIPOS (CAST)\n──────────────────────────\n" +
        "  int -> float         se hace sola (cast implicito)\n" +
        "  cualquier otro caso  hay que pedirlo con 'as'\n" +
        "  valor as tipo        ejemplo: (n as string) + \" pts\"\n\n" +
        "COMENTARIOS\n───────────\n" +
        "  // texto hasta fin de linea\n\n" +
        "FASES DEL ANALIZADOR\n─────────────────────\n" +
        "  Lexico       convierte el texto en tokens\n" +
        "  Sintactico   valida la gramatica y arma el AST\n" +
        "  Semantico    construye la tabla de simbolos y\n" +
        "               revisa que los tipos coincidan\n\n" +
        "ATAJOS\n──────\n" +
        "  F5   analisis lexico\n" +
        "  F6   analisis sintactico\n" +
        "  F7   analisis semantico\n";
    }

    private void estilizarBarra(JScrollBar sb) {
        sb.setBackground(bgPanel);
        sb.setUI(new BasicScrollBarUI() {
            protected void configureScrollBarColors() { thumbColor = ACC; trackColor = bgPanel; }
            protected JButton createDecreaseButton(int o) { return invisible(); }
            protected JButton createIncreaseButton(int o) { return invisible(); }
            private JButton invisible() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(ACC.getRed(), ACC.getGreen(), ACC.getBlue(), 140));
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            }
        });
    }

    private Color colorCategoria(String cat) {
        switch (cat) {
            case "Entrada":                return ACC;
            case "Palabra clave":          return oscuro ? new Color(210, 170, 60) : new Color(150, 105, 5);
            case "Tipo de dato":           return ACC2;
            case "Literal booleana":       return oscuro ? new Color(180, 130, 240) : new Color(120, 60, 195);
            case "Literal entera":         return oscuro ? new Color(90, 165, 230) : new Color(30, 95, 180);
            case "Literal decimal":        return oscuro ? new Color(90, 200, 230) : new Color(25, 125, 180);
            case "Literal cadena":         return oscuro ? new Color(220, 170, 60) : new Color(165, 110, 5);
            case "Identificador":          return oscuro ? new Color(130, 225, 150) : new Color(30, 135, 55);
            case "Operador aritmetico":    return oscuro ? new Color(230, 150, 60) : new Color(175, 90, 5);
            case "Operador de asignacion": return oscuro ? new Color(230, 100, 60) : new Color(180, 55, 10);
            case "Operador relacional":    return oscuro ? new Color(240, 130, 170) : new Color(180, 55, 100);
            case "Operador logico":        return oscuro ? new Color(170, 140, 240) : new Color(105, 65, 190);
            case "Delimitador":            return oscuro ? new Color(125, 155, 150) : new Color(65, 100, 95);
            case "Arreglo":                return oscuro ? new Color(165, 135, 225) : new Color(105, 75, 180);
            case "Error lexico":           return BAD;
            default:                       return textMut;
        }
    }

    private class FilaRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setFont(F_TABLE);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setBackground(sel ? selBg : (row % 2 == 0 ? bgRowA : bgRowB));
            setForeground(sel ? Color.WHITE : textPri);
            return this;
        }
    }

    private class BadgeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            String txt = val != null ? val.toString() : "";
            Color base = colorCategoria(txt);
            Color bg = sel ? selBg : (row % 2 == 0 ? bgRowA : bgRowB);
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
            cell.setBackground(bg);
            JLabel badge = new JLabel(txt) {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), oscuro ? 32 : 45));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), oscuro ? 130 : 180));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    super.paintComponent(g);
                }
            };
            badge.setFont(F_BADGE);
            badge.setForeground(oscuro ? base.brighter() : base.darker());
            badge.setOpaque(false);
            badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            cell.add(badge);
            return cell;
        }
    }
}