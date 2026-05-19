import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class CargoGUI {

    // ─────────────────────────────────────────
    // RENK PALETİ
    // ─────────────────────────────────────────
    static final Color BG         = new Color(0x0D1117);
    static final Color SURFACE    = new Color(0x161B22);
    static final Color SURFACE2   = new Color(0x21262D);
    static final Color BORDER     = new Color(0x30363D);
    static final Color ACCENT     = new Color(0x58A6FF);
    static final Color GREEN      = new Color(0x3FB950);
    static final Color RED        = new Color(0xFF7B72);
    static final Color ORANGE     = new Color(0xF0883E);
    static final Color YELLOW     = new Color(0xE3B341);
    static final Color PURPLE     = new Color(0xBC8CFF);
    static final Color TEXT       = new Color(0xE6EDF3);
    static final Color TEXT_DIM   = new Color(0x8B949E);
    static final Color TEXT_MUTED = new Color(0x484F58);

    static final Font MONO      = new Font("Consolas", Font.PLAIN,  11);
    static final Font MONO_BOLD = new Font("Consolas", Font.BOLD,   11);
    static final Font MONO_SM   = new Font("Consolas", Font.PLAIN,   9);
    static final Font MONO_LG   = new Font("Consolas", Font.BOLD,   20);
    static final Font MONO_MD   = new Font("Consolas", Font.BOLD,   16);

    static Color priorityColor(CargoBackend.Priority p) {
        return switch (p) {
            case ACIL   -> RED;
            case YUKSEK -> ORANGE;
            case NORMAL -> ACCENT;
            case DUSUK  -> TEXT_DIM;
        };
    }

    static Color statusColor(CargoBackend.CargoStatus s) {
        return switch (s) {
            case BEKLEMEDE     -> YELLOW;
            case PAKETLENDI    -> ACCENT;
            case DAGITIMDA     -> ORANGE;
            case TESLIM_EDILDI -> GREEN;
            case IPTAL         -> RED;
        };
    }

    // Tum siniflardan erisebilen tek scrollbar stilleyici
    static void styleScrollBar(JScrollPane sp) {
        javax.swing.JScrollBar vsb = sp.getVerticalScrollBar();
        vsb.setUnitIncrement(12);
        vsb.setPreferredSize(new Dimension(10, 0));
        vsb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor            = new Color(0x30363D);
                thumbHighlightColor   = new Color(0x58A6FF);
                thumbDarkShadowColor  = new Color(0x0D1117);
                thumbLightShadowColor = new Color(0x21262D);
                trackColor            = new Color(0x161B22);
                trackHighlightColor   = new Color(0x161B22);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = isDragging ? new Color(0x58A6FF) : new Color(0x3D4450);
                g2.setColor(base);
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
                g2.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(new Color(0x161B22));
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        });
    }

    // ═══════════════════════════════════════════
    //  GİRİŞ / KAYIT EKRANI
    // ═══════════════════════════════════════════
    static class AuthWindow extends JFrame {

        final CargoBackend.CargoManagementSystem cms;
        CargoBackend.UserSystem.User loggedInUser = null;

        // Panel referansları
        JPanel cardPanel;
        CardLayout cardLayout;

        // Giriş alanları
        JTextField     loginUserField;
        JPasswordField loginPassField;
        JLabel         loginErrLabel;

        // Kayıt alanları
        JTextField     regUserField, regFullNameField, regEmailField;
        JPasswordField regPassField, regPassConfirmField;
        JLabel         regErrLabel;

        AuthWindow(CargoBackend.CargoManagementSystem cms) {
            super("🚚 Kargo Yönetim Sistemi – Giriş");
            this.cms = cms;
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(460, 680);
            setResizable(true);
            setLocationRelativeTo(null);
            getContentPane().setBackground(BG);
            setLayout(new BorderLayout());

            add(buildAuthHeader(), BorderLayout.NORTH);

            cardLayout = new CardLayout();
            cardPanel  = new JPanel(cardLayout);
            cardPanel.setBackground(BG);
            cardPanel.add(buildLoginPanel(),    "LOGIN");
            cardPanel.add(buildRegisterPanel(), "REGISTER");
            add(cardPanel, BorderLayout.CENTER);

            cardLayout.show(cardPanel, "LOGIN");
        }

        // ── Auth Header ──────────────────────
        JPanel buildAuthHeader() {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(SURFACE);
            p.setBorder(BorderFactory.createEmptyBorder(28, 0, 20, 0));

            // Logo/ikon alanı
            JLabel icon = new JLabel("⬡", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
            icon.setForeground(ACCENT);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel title = new JLabel("KARGO YÖNETİM SİSTEMİ", SwingConstants.CENTER);
            title.setFont(new Font("Consolas", Font.BOLD, 16));
            title.setForeground(ACCENT);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel sub = new JLabel("AVL · Heap · Stack · Dijkstra · Deque", SwingConstants.CENTER);
            sub.setFont(MONO_SM);
            sub.setForeground(TEXT_MUTED);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);

            p.add(icon);
            p.add(Box.createVerticalStrut(6));
            p.add(title);
            p.add(Box.createVerticalStrut(4));
            p.add(sub);
            p.add(Box.createVerticalStrut(16));

            JSeparator sep = new JSeparator();
            sep.setForeground(BORDER);
            sep.setMaximumSize(new Dimension(460, 1));
            p.add(sep);

            return p;
        }

        // ── GİRİŞ PANELİ ────────────────────
        JPanel buildLoginPanel() {
            JPanel outer = new JPanel(new GridBagLayout());
            outer.setBackground(BG);

            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(SURFACE);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(28, 32, 28, 32)
            ));
            p.setMaximumSize(new Dimension(360, 999));

            // Başlık
            JLabel h = new JLabel("Hesabına Giriş Yap");
            h.setFont(new Font("Consolas", Font.BOLD, 15));
            h.setForeground(TEXT);
            h.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(h);
            p.add(Box.createVerticalStrut(24));

            // Kullanıcı adı
            p.add(authLabel("Kullanıcı Adı"));
            p.add(Box.createVerticalStrut(4));
            loginUserField = authField("admin");
            p.add(loginUserField);
            p.add(Box.createVerticalStrut(14));

            // Şifre
            p.add(authLabel("Şifre"));
            p.add(Box.createVerticalStrut(4));
            loginPassField = authPassField("••••••••");
            p.add(loginPassField);
            p.add(Box.createVerticalStrut(8));

            // Demo bilgi
            JLabel hint = new JLabel("Demo: kullanıcı=admin  şifre=admin123");
            hint.setFont(MONO_SM);
            hint.setForeground(TEXT_MUTED);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(hint);
            p.add(Box.createVerticalStrut(18));

            // Hata etiketi
            loginErrLabel = new JLabel(" ");
            loginErrLabel.setFont(MONO_SM);
            loginErrLabel.setForeground(RED);
            loginErrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(loginErrLabel);
            p.add(Box.createVerticalStrut(6));

            // Giriş butonu
            JButton loginBtn = bigBtn("Giriş Yap", ACCENT, BG);
            loginBtn.addActionListener(e -> doLogin());
            p.add(loginBtn);
            p.add(Box.createVerticalStrut(16));

            JSeparator sep = new JSeparator();
            sep.setForeground(BORDER);
            sep.setMaximumSize(new Dimension(300, 1));
            sep.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(sep);
            p.add(Box.createVerticalStrut(14));

            // Kayıt ol linki
            JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            linkRow.setBackground(SURFACE);
            JLabel linkLbl = new JLabel("Hesabın yok mu?");
            linkLbl.setFont(MONO_SM);
            linkLbl.setForeground(TEXT_DIM);
            JButton regLink = linkBtn("Kayıt Ol");
            regLink.addActionListener(e -> {
                loginErrLabel.setText(" ");
                cardLayout.show(cardPanel, "REGISTER");
            });
            linkRow.add(linkLbl);
            linkRow.add(regLink);
            linkRow.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(linkRow);

            // Enter key
            getRootPane().setDefaultButton(loginBtn);
            loginPassField.addActionListener(e -> doLogin());

            outer.add(p);
            return outer;
        }

        // ── KAYIT PANELİ ────────────────────
        JPanel buildRegisterPanel() {
            // Form içeriği
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(SURFACE);
            p.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

            JLabel h = new JLabel("Yeni Hesap Oluştur");
            h.setFont(new Font("Consolas", Font.BOLD, 15));
            h.setForeground(TEXT);
            h.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(h);
            p.add(Box.createVerticalStrut(14));

            // Ad Soyad
            p.add(authLabel("Ad Soyad"));
            p.add(Box.createVerticalStrut(3));
            regFullNameField = authField("Ahmet Yilmaz");
            p.add(regFullNameField);
            p.add(Box.createVerticalStrut(9));

            // Kullanici adi
            p.add(authLabel("Kullanici Adi"));
            p.add(Box.createVerticalStrut(3));
            regUserField = authField("kullanici123");
            p.add(regUserField);
            p.add(Box.createVerticalStrut(9));

            // E-posta
            p.add(authLabel("E-posta"));
            p.add(Box.createVerticalStrut(3));
            regEmailField = authField("ornek@email.com");
            p.add(regEmailField);
            p.add(Box.createVerticalStrut(9));

            // Sifre
            p.add(authLabel("Sifre (en az 6 karakter)"));
            p.add(Box.createVerticalStrut(3));
            regPassField = authPassField("");
            p.add(regPassField);
            p.add(Box.createVerticalStrut(9));

            // Sifre tekrar
            p.add(authLabel("Sifre Tekrar"));
            p.add(Box.createVerticalStrut(3));
            regPassConfirmField = authPassField("");
            p.add(regPassConfirmField);
            p.add(Box.createVerticalStrut(8));

            // Hata etiketi
            regErrLabel = new JLabel(" ");
            regErrLabel.setFont(MONO_SM);
            regErrLabel.setForeground(RED);
            regErrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(regErrLabel);
            p.add(Box.createVerticalStrut(4));

            // Kayit butonu
            JButton regBtn = bigBtn("✅  Kayit Ol", GREEN, BG);
            regBtn.addActionListener(e -> doRegister());
            p.add(regBtn);
            p.add(Box.createVerticalStrut(10));

            JSeparator sep = new JSeparator();
            sep.setForeground(BORDER);
            sep.setMaximumSize(new Dimension(300, 1));
            sep.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(sep);
            p.add(Box.createVerticalStrut(10));

            JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            linkRow.setBackground(SURFACE);
            JLabel linkLbl = new JLabel("Zaten hesabin var mi?");
            linkLbl.setFont(MONO_SM);
            linkLbl.setForeground(TEXT_DIM);
            JButton loginLink = linkBtn("Giris Yap");
            loginLink.addActionListener(e -> {
                regErrLabel.setText(" ");
                cardLayout.show(cardPanel, "LOGIN");
            });
            linkRow.add(linkLbl);
            linkRow.add(loginLink);
            linkRow.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(linkRow);

            // ScrollPane - icerik sigmaz ise kaydirilabilir
            JScrollPane scrollPane = new JScrollPane(p,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            scrollPane.getViewport().setBackground(SURFACE);
            styleScrollBar(scrollPane);

            JPanel outer = new JPanel(new BorderLayout());
            outer.setBackground(BG);
            outer.setBorder(BorderFactory.createEmptyBorder(8, 40, 8, 40));
            outer.add(scrollPane, BorderLayout.CENTER);
            return outer;
        }

        // ── AKSİYONLAR ──────────────────────
        void doLogin() {
            String user = loginUserField.getText().trim();
            String pass = new String(loginPassField.getPassword());
            CargoBackend.UserSystem.User u = cms.userSystem.login(user, pass);
            if (u == null) {
                loginErrLabel.setText("❌ Kullanıcı adı veya şifre hatalı.");
                loginPassField.setText("");
                return;
            }
            loggedInUser = u;
            dispose();
            SwingUtilities.invokeLater(() -> new MainWindow(cms, u).setVisible(true));
        }

        void doRegister() {
            String fullName = regFullNameField.getText().trim();
            String username = regUserField.getText().trim();
            String email    = regEmailField.getText().trim();
            String pass     = new String(regPassField.getPassword());
            String passConf = new String(regPassConfirmField.getPassword());

            if (fullName.isEmpty()) { regErrLabel.setText("❌ Ad soyad zorunlu."); return; }
            if (username.isEmpty()) { regErrLabel.setText("❌ Kullanıcı adı zorunlu."); return; }
            if (username.length() < 3) { regErrLabel.setText("❌ Kullanıcı adı en az 3 karakter."); return; }
            if (email.isEmpty() || !email.contains("@")) { regErrLabel.setText("❌ Geçerli bir e-posta girin."); return; }
            if (pass.length() < 6) { regErrLabel.setText("❌ Şifre en az 6 karakter olmalı."); return; }
            if (!pass.equals(passConf)) { regErrLabel.setText("❌ Şifreler uyuşmuyor."); return; }
            if (cms.userSystem.usernameExists(username)) { regErrLabel.setText("❌ Bu kullanıcı adı zaten alınmış."); return; }

            boolean ok = cms.userSystem.register(username, pass, fullName, email);
            if (!ok) { regErrLabel.setText("❌ Kayıt başarısız."); return; }

            // Başarılı kayıt → giriş yaptır
            CargoBackend.UserSystem.User u = cms.userSystem.login(username, pass);
            dispose();
            SwingUtilities.invokeLater(() -> new MainWindow(cms, u).setVisible(true));
        }

        // ── Yardımcı widget'lar ──────────────
        JLabel authLabel(String text) {
            JLabel l = new JLabel(text);
            l.setFont(MONO);
            l.setForeground(TEXT_DIM);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            return l;
        }

        JTextField authField(String placeholder) {
            JTextField f = new JTextField();
            f.setBackground(SURFACE2);
            f.setForeground(TEXT);
            f.setCaretColor(ACCENT);
            f.setFont(MONO);
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Placeholder effect
            f.setForeground(TEXT_MUTED);
            f.setText(placeholder);
            f.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT); }
                }
                public void focusLost(FocusEvent e) {
                    if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_MUTED); }
                }
            });
            return f;
        }

        JPasswordField authPassField(String placeholder) {
            JPasswordField f = new JPasswordField();
            f.setBackground(SURFACE2);
            f.setForeground(TEXT);
            f.setCaretColor(ACCENT);
            f.setFont(MONO);
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            return f;
        }

        JButton bigBtn(String text, Color bg, Color fg) {
            JButton b = new JButton(text);
            b.setFont(new Font("Consolas", Font.BOLD, 13));
            b.setBackground(bg);
            b.setForeground(fg);
            b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            return b;
        }

        JButton linkBtn(String text) {
            JButton b = new JButton(text);
            b.setFont(new Font("Consolas", Font.BOLD, 10));
            b.setForeground(ACCENT);
            b.setBackground(null);
            b.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
    }

    // ═══════════════════════════════════════════
    //  AĞAÇ HARİTASI – Rota Graf Canvas
    // ═══════════════════════════════════════════
    static class NetworkCanvas extends JPanel {

        final CargoBackend.DistributionGraph graph;
        List<String> highlightPath = new ArrayList<>();

        static final Map<String, double[]> CITY_POS = new LinkedHashMap<>();
        static {
            CITY_POS.put("Istanbul",   new double[]{0.08, 0.15});
            CITY_POS.put("Bursa",      new double[]{0.16, 0.30});
            CITY_POS.put("Eskisehir",  new double[]{0.30, 0.32});
            CITY_POS.put("Ankara",     new double[]{0.50, 0.28});
            CITY_POS.put("Samsun",     new double[]{0.62, 0.12});
            CITY_POS.put("Trabzon",    new double[]{0.80, 0.12});
            CITY_POS.put("Konya",      new double[]{0.52, 0.56});
            CITY_POS.put("Izmir",      new double[]{0.14, 0.53});
            CITY_POS.put("Mugla",      new double[]{0.22, 0.73});
            CITY_POS.put("Antalya",    new double[]{0.40, 0.83});
            CITY_POS.put("Adana",      new double[]{0.65, 0.72});
            CITY_POS.put("Gaziantep",  new double[]{0.76, 0.81});
        }

        NetworkCanvas(CargoBackend.DistributionGraph g) {
            this.graph = g;
            setBackground(SURFACE);
        }

        void highlight(List<String> path) {
            this.highlightPath = new ArrayList<>(path);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            if (W < 10 || H < 10) return;

            Set<String> hpEdges = new HashSet<>();
            if (highlightPath.size() > 1) {
                for (int i = 0; i < highlightPath.size() - 1; i++) {
                    String a = highlightPath.get(i), b = highlightPath.get(i + 1);
                    hpEdges.add(a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a);
                }
            }

            for (String[] r : graph.allRoutes()) {
                String a = r[0], b = r[1];
                String key = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
                boolean hl = hpEdges.contains(key);

                double[] pa = pos(a, W, H), pb = pos(b, W, H);
                g.setColor(hl ? ACCENT : BORDER);
                g.setStroke(new BasicStroke(hl ? 2.8f : 1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(pa[0], pa[1], pb[0], pb[1]));

                double mx = (pa[0] + pb[0]) / 2, my = (pa[1] + pb[1]) / 2;
                g.setFont(new Font("Consolas", Font.PLAIN, 9));
                g.setColor(hl ? ACCENT : TEXT_MUTED);
                g.drawString(r[2] + "km", (int) mx, (int) my);
            }

            final int R = 20;
            for (Map.Entry<String, double[]> e : CITY_POS.entrySet()) {
                String city = e.getKey();
                double[] p  = pos(city, W, H);
                boolean hl  = highlightPath.contains(city);

                g.setColor(new Color(0, 0, 0, 60));
                g.fillOval((int) p[0] - R + 2, (int) p[1] - R + 2, R * 2, R * 2);

                g.setColor(hl ? ACCENT : SURFACE2);
                g.fillOval((int) p[0] - R, (int) p[1] - R, R * 2, R * 2);
                g.setColor(hl ? ACCENT : BORDER);
                g.setStroke(new BasicStroke(hl ? 2.5f : 1.5f));
                g.drawOval((int) p[0] - R, (int) p[1] - R, R * 2, R * 2);

                String abbr = city.substring(0, Math.min(3, city.length())).toUpperCase();
                g.setFont(new Font("Consolas", Font.BOLD, 8));
                g.setColor(hl ? BG : TEXT);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(abbr, (int) p[0] - fm.stringWidth(abbr) / 2, (int) p[1] + 4);

                g.setFont(new Font("Consolas", Font.PLAIN, 8));
                g.setColor(TEXT_DIM);
                FontMetrics fm2 = g.getFontMetrics();
                g.drawString(city, (int) p[0] - fm2.stringWidth(city) / 2, (int) p[1] + R + 12);
            }
        }

        double[] pos(String city, int W, int H) {
            double[] rel = CITY_POS.getOrDefault(city, new double[]{0.5, 0.5});
            return new double[]{rel[0] * W, rel[1] * H};
        }
    }

    // ═══════════════════════════════════════════
    //  ANA PENCERE
    // ═══════════════════════════════════════════
    static class MainWindow extends JFrame {

        final CargoBackend.CargoManagementSystem cms;
        final CargoBackend.UserSystem.User       currentUser;

        final Map<String, JLabel> statLabels = new LinkedHashMap<>();
        final Map<String, JLabel> dsLabels   = new LinkedHashMap<>();

        JTable cargoTable, queueTable, pkgTable, pkgDetailTable, historyTable;
        DefaultTableModel cargoModel, queueModel, pkgModel, pkgDetailModel, histModel;

        JTextField        searchField;
        JComboBox<String> statusFilter;

        NetworkCanvas     networkCanvas;
        JComboBox<String> srcCombo, dstCombo;
        JLabel            routeResultLabel;

        JPanel heapBoxesPanel;

        MainWindow(CargoBackend.CargoManagementSystem cms, CargoBackend.UserSystem.User user) {
            super("🚚 Akıllı Kargo Yönetim Sistemi");
            this.cms         = cms;
            this.currentUser = user;
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(1400, 900);
            setMinimumSize(new Dimension(1100, 700));
            setLocationRelativeTo(null);
            getContentPane().setBackground(BG);

            buildUI();
            refreshAll();
        }

        void buildUI() {
            setLayout(new BorderLayout());
            add(buildHeader(), BorderLayout.NORTH);

            JSplitPane split = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildNotebook());
            split.setDividerLocation(245);
            split.setDividerSize(4);
            split.setBorder(null);
            split.setBackground(BG);
            add(split, BorderLayout.CENTER);
        }

        // ── HEADER (kullanıcı bilgisi + çıkış) ─
        JPanel buildHeader() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(SURFACE);
            p.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JLabel title = new JLabel("⬡  KARGO YÖNETİM SİSTEMİ");
            title.setFont(new Font("Consolas", Font.BOLD, 16));
            title.setForeground(ACCENT);

            JLabel sub = new JLabel("   AVL Ağacı · Stack · Priority Queue (Min-Heap) · Graf (Dijkstra) · Deque");
            sub.setFont(MONO_SM);
            sub.setForeground(TEXT_DIM);

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.setBackground(SURFACE);
            left.add(title);
            left.add(sub);

            // Sağ: saat + kullanıcı + çıkış
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
            right.setBackground(SURFACE);

            JLabel clock = new JLabel();
            clock.setFont(MONO_SM);
            clock.setForeground(TEXT_MUTED);
            javax.swing.Timer timer = new javax.swing.Timer(1000, e -> clock.setText(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss"))));
            timer.start();
            clock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss")));

            // Kullanıcı rozeti
            JLabel userBadge = new JLabel("👤 " + currentUser.fullName);
            userBadge.setFont(MONO_BOLD);
            userBadge.setForeground(GREEN);
            userBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x3FB950, true), 1),
                    BorderFactory.createEmptyBorder(3, 10, 3, 10)));

            // Çıkış butonu
            JButton logoutBtn = new JButton("⎋ Çıkış");
            logoutBtn.setFont(MONO_BOLD);
            logoutBtn.setForeground(RED);
            logoutBtn.setBackground(SURFACE2);
            logoutBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(3, 10, 3, 10)));
            logoutBtn.setFocusPainted(false);
            logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            logoutBtn.addActionListener(e -> {
                int ans = JOptionPane.showConfirmDialog(this,
                        "Çıkış yapmak istiyor musunuz?", "Çıkış",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ans == JOptionPane.YES_OPTION) {
                    dispose();
                    SwingUtilities.invokeLater(() -> new AuthWindow(cms).setVisible(true));
                }
            });

            right.add(clock);
            right.add(userBadge);
            right.add(logoutBtn);

            p.add(left,  BorderLayout.WEST);
            p.add(right, BorderLayout.EAST);
            return p;
        }

        // ── SOL PANEL ────────────────────────
        JPanel buildSidebar() {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(SURFACE);
            p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            sideSection(p, "GENEL OZET");
            Object[][] statDefs = {
                    {"toplam",     "Toplam Kargo",   0x58A6FF},
                    {"bekleyen",   "Bekleyen",        0xE3B341},
                    {"paketlendi", "Paketlendi",      0x58A6FF},
                    {"dagitimda",  "Dagitimda",       0xF0883E},
                    {"teslim",     "Teslim Edildi",   0x3FB950},
                    {"iptal",      "Iptal",           0xFF7B72},
            };
            for (Object[] d : statDefs) {
                Color c = new Color((int)(Integer)d[2]);
                p.add(statCard((String)d[0], (String)d[1], c, statLabels, MONO_LG));
                p.add(vgap(4));
            }

            p.add(vgap(8)); sideSep(p); p.add(vgap(8));
            sideSection(p, "VERI YAPILARI");

            Object[][] dsDefs = {
                    {"avlYukseklik", "AVL Yuksekligi",  0xBC8CFF},
                    {"kuyrukBoyut",  "Kuyruk (Heap)",    0xF0883E},
                    {"stackBoyut",   "Stack (Islem)",    0x58A6FF},
                    {"paketSayisi",  "Paket Sayisi",     0x3FB950},
            };
            for (Object[] d : dsDefs) {
                Color c = new Color((int)(Integer)d[2]);
                p.add(statCard((String)d[0], (String)d[1], c, dsLabels, MONO_MD));
                p.add(vgap(4));
            }

            p.add(vgap(8)); sideSep(p); p.add(vgap(8));
            sideSection(p, "HIZLI ISLEMLER");

            String[]   btnTxts  = {"Otomatik Paketle", "Son Islemi Geri Al", "Yenile"};
            Color[]    btnClrs  = {GREEN, ORANGE, ACCENT};
            Runnable[] btnCmds  = {this::autoPackage, this::undoAction, this::refreshAll};
            for (int i = 0; i < btnTxts.length; i++) {
                p.add(sideBtn(btnTxts[i], btnClrs[i], btnCmds[i]));
                p.add(vgap(4));
            }

            p.add(Box.createVerticalGlue());

            // ScrollPane wrapper
            JScrollPane scroll = new JScrollPane(p,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));
            scroll.getViewport().setBackground(SURFACE);
            scroll.setPreferredSize(new Dimension(245, 0));
            styleScrollBar(scroll);

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(SURFACE);
            wrapper.add(scroll, BorderLayout.CENTER);
            return wrapper;
        }

        JPanel statCard(String key, String label, Color color,
                        Map<String, JLabel> map, Font valFont) {
            JPanel card = new JPanel(new GridLayout(2, 1));
            card.setBackground(SURFACE2);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            card.setMaximumSize(new Dimension(230, 62));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lbl = new JLabel(label);
            lbl.setFont(MONO_SM);
            lbl.setForeground(TEXT_DIM);

            JLabel val = new JLabel("0");
            val.setFont(valFont);
            val.setForeground(color);
            map.put(key, val);

            card.add(lbl);
            card.add(val);
            return card;
        }

        JButton sideBtn(String text, Color color, Runnable action) {
            JButton b = new JButton(text);
            b.setFont(MONO_BOLD);
            b.setForeground(color);
            b.setBackground(SURFACE2);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setMaximumSize(new Dimension(230, 36));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.addActionListener(e -> action.run());
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { b.setBackground(BORDER); }
                public void mouseExited(MouseEvent e)  { b.setBackground(SURFACE2); }
            });
            return b;
        }

        void sideSection(JPanel p, String text) {
            JLabel l = new JLabel(text);
            l.setFont(MONO_SM);
            l.setForeground(TEXT_DIM);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            l.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 0));
            p.add(l);
        }

        void sideSep(JPanel p) {
            JSeparator s = new JSeparator();
            s.setForeground(BORDER);
            s.setBackground(BORDER);
            s.setMaximumSize(new Dimension(230, 1));
            p.add(s);
        }

        Component vgap(int h) { return Box.createVerticalStrut(h); }

        // ── SEKMELER ─────────────────────────
        JTabbedPane buildNotebook() {
            JTabbedPane nb = new JTabbedPane();
            nb.setBackground(BG);
            nb.setForeground(TEXT_DIM);
            nb.setFont(MONO_BOLD);

            nb.addTab("📦 Kargolar",         buildCargoTab());
            nb.addTab("⬆ Öncelik Kuyruğu",  buildQueueTab());
            nb.addTab("🗃 Paketler",          buildPackagesTab());
            nb.addTab("🗺 Rota Graf",         buildRouteTab());
            nb.addTab("📋 İşlem Geçmişi",    buildHistoryTab());

            return nb;
        }

        JPanel buildCargoTab() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(BG);

            JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            tb.setBackground(SURFACE);

            tb.add(dimLabel("🔍"));
            searchField = new JTextField(16);
            styleField(searchField);
            searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { refreshCargoTable(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { refreshCargoTable(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshCargoTable(); }
            });
            tb.add(searchField);

            JButton addBtn = accentBtn("+ Yeni Kargo", ACCENT);
            addBtn.addActionListener(e -> openAddCargoDialog());
            tb.add(addBtn);

            tb.add(dimLabel("  Durum:"));
            List<String> opts = new ArrayList<>();
            opts.add("Tümü");
            for (CargoBackend.CargoStatus s : CargoBackend.CargoStatus.values()) opts.add(s.label);
            statusFilter = new JComboBox<>(opts.toArray(new String[0]));
            styleCombo(statusFilter);
            statusFilter.addActionListener(e -> refreshCargoTable());
            tb.add(statusFilter);

            p.add(tb, BorderLayout.NORTH);

            String[] cols = {"ID","Gönderici","Alıcı","Ağırlık","Hacim","Öncelik","Durum","Hedef","Değer"};
            cargoModel = model(cols);
            cargoTable = makeTable(cargoModel);
            setupCargoRenderer();
            p.add(scroll(cargoTable), BorderLayout.CENTER);

            JPanel act = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            act.setBackground(SURFACE);

            JButton deliv  = actionBtn("Teslim Et",   GREEN);
            JButton cancel = actionBtn("Iptal Et",    RED);
            JButton pkg    = actionBtn("Paketle",     ACCENT);
            JButton detail = actionBtn("Detay",       PURPLE);
            JButton delete = actionBtn("Sil",         RED);

            deliv.addActionListener(e  -> deliverSelected());
            cancel.addActionListener(e -> cancelSelected());
            pkg.addActionListener(e    -> packageSelected());
            detail.addActionListener(e -> showCargoDetail());
            delete.addActionListener(e -> deleteSelected());

            act.add(deliv); act.add(cancel); act.add(pkg); act.add(detail);
            act.add(Box.createHorizontalStrut(12));
            act.add(delete);
            p.add(act, BorderLayout.SOUTH);
            return p;
        }

        JPanel buildQueueTab() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(BG);

            JLabel info = padLabel("  Min-Heap tabanlı Öncelik Kuyruğu – En yüksek öncelikli kargo en üstte");
            info.setBackground(SURFACE);
            info.setOpaque(true);
            p.add(info, BorderLayout.NORTH);

            String[] cols = {"Sıra","ID","Alıcı","Öncelik","Hedef","Ağırlık","Oluşturma"};
            queueModel = model(cols);
            queueTable = makeTable(queueModel);
            p.add(scroll(queueTable), BorderLayout.CENTER);

            JPanel heapRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 6));
            heapRow.setBackground(BG);
            heapRow.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));
            heapRow.add(dimLabel("HEAP → "));
            heapBoxesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            heapBoxesPanel.setBackground(BG);
            heapRow.add(heapBoxesPanel);
            p.add(heapRow, BorderLayout.SOUTH);

            return p;
        }

        JPanel buildPackagesTab() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(BG);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setBackground(BG);
            split.setDividerSize(4);

            JPanel left = new JPanel(new BorderLayout());
            left.setBackground(BG);
            left.add(padLabel("  PAKETLER (First-Fit Decreasing)"), BorderLayout.NORTH);

            String[] pkgCols = {"Paket ID","Kargo Sayısı","Ağırlık","Hacim","Doluluk %"};
            pkgModel = model(pkgCols);
            pkgTable = makeTable(pkgModel);
            pkgTable.getSelectionModel().addListSelectionListener(e -> onPkgSelect());
            left.add(scroll(pkgTable), BorderLayout.CENTER);
            split.setLeftComponent(left);

            JPanel right = new JPanel(new BorderLayout());
            right.setBackground(SURFACE);
            right.setBorder(BorderFactory.createLineBorder(BORDER));

            JLabel rlbl = new JLabel("  PAKET İÇERİĞİ");
            rlbl.setFont(MONO_BOLD);
            rlbl.setForeground(ACCENT);
            rlbl.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
            rlbl.setBackground(SURFACE);
            rlbl.setOpaque(true);
            right.add(rlbl, BorderLayout.NORTH);

            String[] detCols = {"ID","Alıcı","Ağırlık","Öncelik","Durum"};
            pkgDetailModel = model(detCols);
            pkgDetailTable = makeTable(pkgDetailModel);
            right.add(scroll(pkgDetailTable), BorderLayout.CENTER);
            split.setRightComponent(right);
            split.setDividerLocation(420);

            p.add(split, BorderLayout.CENTER);

            JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            bot.setBackground(SURFACE);
            JButton dispBtn = accentBtn("🚚 Seçili Paketi Sevkiyata Ver", GREEN);
            dispBtn.addActionListener(e -> dispatchSelected());
            bot.add(dispBtn);
            p.add(bot, BorderLayout.SOUTH);

            return p;
        }

        JPanel buildRouteTab() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(BG);

            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
            ctrl.setBackground(BG);

            List<String> cities = new ArrayList<>(cms.graph.cities);
            Collections.sort(cities);

            ctrl.add(dimLabel("Kaynak:"));
            srcCombo = new JComboBox<>(cities.toArray(new String[0]));
            srcCombo.setSelectedItem("Istanbul");
            styleCombo(srcCombo);
            ctrl.add(srcCombo);

            ctrl.add(dimLabel("  Hedef:"));
            dstCombo = new JComboBox<>(cities.toArray(new String[0]));
            dstCombo.setSelectedItem("Antalya");
            styleCombo(dstCombo);
            ctrl.add(dstCombo);

            JButton calc = accentBtn("🗺 Rota Hesapla", ACCENT);
            calc.addActionListener(e -> calcRoute());
            ctrl.add(calc);

            p.add(ctrl, BorderLayout.NORTH);

            networkCanvas = new NetworkCanvas(cms.graph);
            p.add(networkCanvas, BorderLayout.CENTER);

            routeResultLabel = new JLabel(" ");
            routeResultLabel.setFont(MONO);
            routeResultLabel.setForeground(ACCENT);
            routeResultLabel.setBorder(BorderFactory.createEmptyBorder(6, 16, 8, 16));
            routeResultLabel.setBackground(BG);
            routeResultLabel.setOpaque(true);
            p.add(routeResultLabel, BorderLayout.SOUTH);

            return p;
        }

        JPanel buildHistoryTab() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(BG);

            JLabel info = padLabel("  İşlem Yığını (Stack) – En son işlem en üstte. Geri Al butonuyla tersine çevirebilirsiniz.");
            info.setBackground(SURFACE);
            info.setOpaque(true);
            p.add(info, BorderLayout.NORTH);

            String[] cols = {"Sıra","Tür","Detay","Zaman"};
            histModel = model(cols);
            historyTable = makeTable(histModel);
            p.add(scroll(historyTable), BorderLayout.CENTER);

            return p;
        }

        // ─────────────────────────────────────
        // YENİLEME
        // ─────────────────────────────────────
        void refreshAll() {
            refreshStats();
            refreshCargoTable();
            refreshQueueTab();
            refreshPackagesTab();
            refreshHistoryTab();
        }

        void refreshStats() {
            Map<String, Object> st = cms.dashboardStats();
            st.forEach((k, v) -> {
                JLabel l = statLabels.containsKey(k) ? statLabels.get(k) : dsLabels.get(k);
                if (l != null) l.setText(v.toString());
            });
        }

        void refreshCargoTable() {
            cargoModel.setRowCount(0);
            String srch = searchField  != null ? searchField.getText().toLowerCase().trim() : "";
            String filt = statusFilter != null ? (String) statusFilter.getSelectedItem() : "Tümü";

            for (CargoBackend.Cargo c : cms.getAllCargosSorted()) {
                if (!"Tümü".equals(filt) && !c.status.label.equals(filt)) continue;
                if (!srch.isEmpty()
                        && !c.id.toLowerCase().contains(srch)
                        && !c.receiver.toLowerCase().contains(srch)
                        && !c.sender.toLowerCase().contains(srch)) continue;
                cargoModel.addRow(new Object[]{
                        c.id, c.sender, c.receiver,
                        String.format("%.1f kg", c.weight),
                        String.format("%.1f L",  c.volume),
                        c.priority.label, c.status.label,
                        c.destination, String.format("₺%.0f", c.value)
                });
            }
        }

        void setupCargoRenderer() {
            cargoTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable t, Object v,
                                                               boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                    setBackground(row % 2 == 0 ? SURFACE : SURFACE2);
                    setForeground(TEXT);
                    if (col == 5) {
                        for (CargoBackend.Priority pr : CargoBackend.Priority.values())
                            if (pr.label.equals(v)) { setForeground(priorityColor(pr)); break; }
                    } else if (col == 6) {
                        for (CargoBackend.CargoStatus st : CargoBackend.CargoStatus.values())
                            if (st.label.equals(v)) { setForeground(statusColor(st)); break; }
                    }
                    if (sel) setBackground(BORDER);
                    setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    setFont(MONO);
                    return this;
                }
            });
        }

        void refreshQueueTab() {
            queueModel.setRowCount(0);
            List<CargoBackend.Cargo> items = cms.getWaitingQueue();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd HH:mm");
            int i = 1;
            for (CargoBackend.Cargo c : items) {
                queueModel.addRow(new Object[]{
                        i++, c.id, c.receiver, c.priority.label,
                        c.destination, c.weight + "kg",
                        c.createdAt.format(fmt)
                });
            }

            queueTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable t, Object v,
                                                               boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                    setBackground(row % 2 == 0 ? SURFACE : SURFACE2);
                    setForeground(TEXT);
                    if (col == 3 && row < items.size())
                        setForeground(priorityColor(items.get(row).priority));
                    if (sel) setBackground(BORDER);
                    setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    setFont(MONO);
                    return this;
                }
            });

            heapBoxesPanel.removeAll();
            int lim = Math.min(items.size(), 14);
            for (int j = 0; j < lim; j++) {
                CargoBackend.Cargo c = items.get(j);
                Color col = priorityColor(c.priority);
                JLabel box = new JLabel(c.id + " [" + c.priority.label + "]");
                box.setFont(MONO_SM);
                box.setForeground(col);
                box.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(col, 1),
                        BorderFactory.createEmptyBorder(3, 6, 3, 6)));
                box.setBackground(new Color(col.getRed(), col.getGreen(), col.getBlue(), 28));
                box.setOpaque(true);
                heapBoxesPanel.add(box);
            }
            heapBoxesPanel.revalidate();
            heapBoxesPanel.repaint();
        }

        void refreshPackagesTab() {
            pkgModel.setRowCount(0);
            for (CargoBackend.Package pkg : cms.getPackagesList()) {
                pkgModel.addRow(new Object[]{
                        pkg.id, pkg.cargos.size(),
                        String.format("%.1f/%.0f kg", pkg.currentWeight(), pkg.maxWeight),
                        String.format("%.1f/%.0f L",  pkg.currentVolume(), pkg.maxVolume),
                        String.format("%.1f%%", pkg.utilization())
                });
            }
        }

        void onPkgSelect() {
            int row = pkgTable.getSelectedRow();
            if (row < 0) return;
            String pkgId = (String) pkgModel.getValueAt(row, 0);
            CargoBackend.Package pkg = cms.packages.get(pkgId);
            if (pkg == null) return;

            pkgDetailModel.setRowCount(0);
            for (CargoBackend.Cargo c : pkg.cargos) {
                pkgDetailModel.addRow(new Object[]{
                        c.id, c.receiver,
                        String.format("%.1f kg", c.weight),
                        c.priority.label, c.status.label
                });
            }
        }

        void refreshHistoryTab() {
            histModel.setRowCount(0);
            int i = 1;
            for (CargoBackend.ActionStack.Action a : cms.getHistory()) {
                histModel.addRow(new Object[]{i++, a.type, a.detail, a.formattedTime()});
            }
        }

        // ─────────────────────────────────────
        // AKSİYONLAR
        // ─────────────────────────────────────
        void autoPackage() {
            List<CargoBackend.Package> newPkgs = cms.autoPackage();
            refreshAll();
            info(newPkgs.size() + " yeni paket oluşturuldu.\nFirst-Fit Decreasing algoritması kullanıldı.");
        }

        void undoAction() {
            CargoBackend.ActionStack.Action a = cms.undoLast();
            if (a != null) { refreshAll(); info("Geri alındı:\n" + a.detail); }
            else info("Geri alınacak işlem yok.");
        }

        String selectedCargoId() {
            int row = cargoTable.getSelectedRow();
            return row < 0 ? null : (String) cargoModel.getValueAt(row, 0);
        }

        void deliverSelected() {
            String id = selectedCargoId();
            if (id == null) { warn("Lütfen bir kargo seçin."); return; }
            if (cms.deliverCargo(id)) { refreshAll(); info("✅ " + id + " teslim edildi!"); }
            else err("Kargo teslim edilemedi.\nDağıtımda durumunda olmalı.");
        }

        void cancelSelected() {
            String id = selectedCargoId();
            if (id == null) { warn("Lutfen bir kargo secin."); return; }
            if (cms.cancelCargo(id)) { refreshAll(); info(id + " iptal edildi."); }
            else err("Kargo iptal edilemedi.");
        }

        void deleteSelected() {
            String id = selectedCargoId();
            if (id == null) { warn("Lutfen bir kargo secin."); return; }
            int ans = JOptionPane.showConfirmDialog(this,
                    id + " numarali kargo kalici olarak silinecek.\nEmin misiniz?",
                    "Kargoyu Sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) return;
            if (cms.deleteCargo(id)) { refreshAll(); info(id + " silindi."); }
            else err("Kargo silinemedi.");
        }

        void packageSelected() {
            String id = selectedCargoId();
            if (id == null) { warn("Lütfen bir kargo seçin."); return; }
            if (cms.packageCargo(id, null)) { refreshAll(); info("📦 Kargo yeni pakete yerleştirildi."); }
            else err("Paketleme başarısız.\nKargo beklemede durumunda olmalı.");
        }

        void showCargoDetail() {
            String id = selectedCargoId();
            if (id == null) { warn("Lütfen bir kargo seçin."); return; }
            CargoBackend.Cargo c = cms.searchCargo(id);
            if (c == null) return;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            info(
                    "Kargo ID    : " + c.id       + "\n" +
                            "Gönderici   : " + c.sender    + "\n" +
                            "Alıcı       : " + c.receiver  + "\n" +
                            "Ağırlık     : " + c.weight    + " kg\n" +
                            "Hacim       : " + c.volume    + " L\n"  +
                            "Öncelik     : " + c.priority.label + "\n" +
                            "Durum       : " + c.status.label   + "\n" +
                            "Hedef       : " + c.destination    + "\n" +
                            "Değer       : ₺" + String.format("%.2f", c.value) + "\n" +
                            "Paket       : " + (c.packageId != null ? c.packageId : "Paketlenmedi") + "\n" +
                            "Oluşturulma : " + c.createdAt.format(fmt) + "\n" +
                            "Güncelleme  : " + c.updatedAt.format(fmt)
            );
        }

        void dispatchSelected() {
            int row = pkgTable.getSelectedRow();
            if (row < 0) { warn("Lütfen bir paket seçin."); return; }
            String pkgId = (String) pkgModel.getValueAt(row, 0);
            if (cms.dispatchPackage(pkgId)) { refreshAll(); info("🚚 " + pkgId + " sevkiyata verildi!"); }
            else err("Sevkiyat başarısız.");
        }

        void calcRoute() {
            String src = (String) srcCombo.getSelectedItem();
            String dst = (String) dstCombo.getSelectedItem();
            if (src == null || dst == null || src.equals(dst)) {
                warn("Kaynak ve hedef farklı şehirler olmalı."); return;
            }
            CargoBackend.DistributionGraph.RouteResult fastest  = cms.graph.dijkstra(src, dst, true);
            CargoBackend.DistributionGraph.RouteResult shortest = cms.graph.dijkstra(src, dst, false);

            networkCanvas.highlight(fastest.path);
            routeResultLabel.setText(
                    "⚡ En Hızlı: " + String.join(" → ", fastest.path)  +
                            String.format("  (%.1f saat)    ", fastest.cost) +
                            "📏 En Kısa: " + String.join(" → ", shortest.path) +
                            String.format("  (%.0f km)", shortest.cost));
        }

        // ─────────────────────────────────────
        // YENİ KARGO DİYALOĞU
        // ─────────────────────────────────────
        void openAddCargoDialog() {
            JDialog dlg = new JDialog(this, "Yeni Kargo Ekle", true);
            dlg.getContentPane().setBackground(BG);
            dlg.setSize(430, 510);
            dlg.setLocationRelativeTo(this);
            dlg.setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(BG);
            form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
            GridBagConstraints g = new GridBagConstraints();
            g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(5, 5, 5, 5);

            JLabel title = new JLabel("Yeni Kargo Ekle");
            title.setFont(new Font("Consolas", Font.BOLD, 15));
            title.setForeground(ACCENT);
            g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
            form.add(title, g); g.gridwidth = 1;

            String[] lbls = {"Gönderici","Alıcı","Ağırlık (kg)","Hacim (L)","Değer (₺)"};
            JTextField[] flds = new JTextField[lbls.length];
            for (int i = 0; i < lbls.length; i++) {
                JLabel l = new JLabel(lbls[i]);
                l.setFont(MONO); l.setForeground(TEXT_DIM);
                g.gridx = 0; g.gridy = i + 1; g.weightx = 0;
                form.add(l, g);
                flds[i] = new JTextField();
                styleField(flds[i]);
                g.gridx = 1; g.weightx = 1;
                form.add(flds[i], g);
            }

            JLabel priLbl = new JLabel("Öncelik");
            priLbl.setFont(MONO); priLbl.setForeground(TEXT_DIM);
            g.gridx = 0; g.gridy = 6; g.weightx = 0;
            form.add(priLbl, g);
            JComboBox<String> priBox = new JComboBox<>(
                    Arrays.stream(CargoBackend.Priority.values()).map(pr -> pr.label).toArray(String[]::new));
            priBox.setSelectedIndex(2);
            styleCombo(priBox);
            g.gridx = 1; g.weightx = 1;
            form.add(priBox, g);

            JLabel dstLbl = new JLabel("Hedef Şehir");
            dstLbl.setFont(MONO); dstLbl.setForeground(TEXT_DIM);
            g.gridx = 0; g.gridy = 7; g.weightx = 0;
            form.add(dstLbl, g);
            List<String> cities = new ArrayList<>(cms.graph.cities);
            Collections.sort(cities);
            JComboBox<String> dstBox = new JComboBox<>(cities.toArray(new String[0]));
            styleCombo(dstBox);
            g.gridx = 1; g.weightx = 1;
            form.add(dstBox, g);

            JLabel errLbl = new JLabel(" ");
            errLbl.setFont(MONO_SM);
            errLbl.setForeground(RED);
            g.gridx = 0; g.gridy = 8; g.gridwidth = 2;
            form.add(errLbl, g);

            dlg.add(form, BorderLayout.CENTER);

            JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bot.setBackground(BG);
            JButton ok = accentBtn("✅ Kargo Ekle", GREEN);
            ok.addActionListener(e -> {
                try {
                    String s   = flds[0].getText().trim();
                    String r   = flds[1].getText().trim();
                    if (s.isEmpty() || r.isEmpty()) throw new Exception("Gönderici ve alıcı zorunlu.");
                    double w   = Double.parseDouble(flds[2].getText().trim());
                    double v   = Double.parseDouble(flds[3].getText().trim());
                    double val = flds[4].getText().trim().isEmpty() ? 0
                            : Double.parseDouble(flds[4].getText().trim());
                    String priStr = (String) priBox.getSelectedItem();
                    CargoBackend.Priority pri = Arrays.stream(CargoBackend.Priority.values())
                            .filter(pr -> pr.label.equals(priStr)).findFirst()
                            .orElse(CargoBackend.Priority.NORMAL);
                    String dst = (String) dstBox.getSelectedItem();
                    cms.addCargo(s, r, w, v, pri, dst, val);
                    refreshAll();
                    dlg.dispose();
                } catch (NumberFormatException ex) {
                    errLbl.setText("Ağırlık, hacim ve değer sayı olmalı.");
                } catch (Exception ex) {
                    errLbl.setText(ex.getMessage());
                }
            });
            bot.add(ok);
            dlg.add(bot, BorderLayout.SOUTH);
            dlg.setVisible(true);
        }

        // ─────────────────────────────────────
        // SWING YARDIMCILARI
        // ─────────────────────────────────────
        DefaultTableModel model(String[] cols) {
            return new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
        }

        JTable makeTable(DefaultTableModel m) {
            JTable t = new JTable(m);
            t.setBackground(SURFACE);
            t.setForeground(TEXT);
            t.setFont(MONO);
            t.setRowHeight(28);
            t.setShowGrid(false);
            t.setIntercellSpacing(new Dimension(0, 0));
            t.setSelectionBackground(BORDER);
            t.setSelectionForeground(TEXT);
            t.setFillsViewportHeight(true);
            t.getTableHeader().setBackground(SURFACE2);
            t.getTableHeader().setForeground(ACCENT);
            t.getTableHeader().setFont(MONO_BOLD);
            t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            return t;
        }

        JScrollPane scroll(JTable t) {
            JScrollPane sp = new JScrollPane(t);
            sp.getViewport().setBackground(SURFACE);
            sp.setBorder(BorderFactory.createLineBorder(BORDER));
            styleScrollBar(sp);
            return sp;
        }

        void styleField(JTextField f) {
            f.setBackground(SURFACE2);
            f.setForeground(TEXT);
            f.setCaretColor(ACCENT);
            f.setFont(MONO);
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }

        void styleCombo(JComboBox<?> c) {
            c.setBackground(SURFACE2);
            c.setForeground(TEXT);
            c.setFont(MONO);
            c.setBorder(BorderFactory.createLineBorder(BORDER));
        }

        JButton accentBtn(String text, Color color) {
            JButton b = new JButton(text);
            b.setFont(MONO_BOLD);
            b.setForeground(BG);
            b.setBackground(color);
            b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        JButton actionBtn(String text, Color color) {
            JButton b = new JButton(text);
            b.setFont(MONO_BOLD);
            b.setForeground(color);
            b.setBackground(SURFACE2);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { b.setBackground(BORDER); }
                public void mouseExited(MouseEvent e)  { b.setBackground(SURFACE2); }
            });
            return b;
        }

        JLabel dimLabel(String text) {
            JLabel l = new JLabel(text);
            l.setFont(MONO);
            l.setForeground(TEXT_DIM);
            return l;
        }

        JLabel padLabel(String text) {
            JLabel l = new JLabel(text);
            l.setFont(MONO_SM);
            l.setForeground(TEXT_DIM);
            l.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            return l;
        }

        void info(String msg) {
            JOptionPane.showMessageDialog(this, msg, "Bilgi", JOptionPane.INFORMATION_MESSAGE);
        }
        void warn(String msg) {
            JOptionPane.showMessageDialog(this, msg, "Uyarı", JOptionPane.WARNING_MESSAGE);
        }
        void err(String msg) {
            JOptionPane.showMessageDialog(this, msg, "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════
    //  GİRİŞ NOKTASI
    // ═══════════════════════════════════════════
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("Panel.background",             new Color(0x0D1117));
                UIManager.put("OptionPane.background",        new Color(0x161B22));
                UIManager.put("OptionPane.messageForeground", new Color(0xE6EDF3));
                UIManager.put("Button.background",            new Color(0x21262D));
                UIManager.put("Button.foreground",            new Color(0xE6EDF3));
                UIManager.put("ComboBox.background",          new Color(0x21262D));
                UIManager.put("ComboBox.foreground",          new Color(0xE6EDF3));
                UIManager.put("TextField.background",         new Color(0x21262D));
                UIManager.put("TextField.foreground",         new Color(0xE6EDF3));
                UIManager.put("Label.foreground",             new Color(0xE6EDF3));
                UIManager.put("TabbedPane.background",        new Color(0x0D1117));
                UIManager.put("TabbedPane.foreground",        new Color(0x8B949E));
                UIManager.put("SplitPane.background",         new Color(0x0D1117));
                UIManager.put("ScrollBar.background",         new Color(0x161B22));
                UIManager.put("ScrollBar.thumb",              new Color(0x30363D));
                UIManager.put("ScrollBar.thumbHighlight",     new Color(0x58A6FF));
                UIManager.put("ScrollBar.thumbShadow",        new Color(0x21262D));
                UIManager.put("ScrollBar.track",              new Color(0x161B22));
                UIManager.put("ScrollBar.trackHighlight",     new Color(0x161B22));
                UIManager.put("ScrollBar.darkShadow",         new Color(0x0D1117));
                UIManager.put("ScrollBar.shadow",             new Color(0x21262D));
                UIManager.put("ScrollBar.highlight",          new Color(0x30363D));
                UIManager.put("ScrollBar.width",              14);
                UIManager.put("ScrollPane.border",            BorderFactory.createEmptyBorder());
            } catch (Exception ignored) {}

            // Önce sistemi oluştur, sonra login ekranını aç
            CargoBackend.CargoManagementSystem cms = new CargoBackend.CargoManagementSystem();
            new AuthWindow(cms).setVisible(true);
        });
    }
}