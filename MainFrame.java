import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/**
 * Main application window for the English Vocabulary Flashcard & Quiz desktop application.
 * Polished to a SaaS/Notion aesthetic with custom cards, pill badges, and refined typography.
 * Manages the top-level navigation containing:
 *  1. Vocabulary List (JTable, Search, Form CRUD, Metric Strip)
 *  2. Flashcard Mode (FlashcardPanel)
 *  3. Quiz Practice (QuizPanel)
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final transient FlashcardManager manager;

    // Tabs
    private JTabbedPane tabbedPane;
    private FlashcardPanel flashcardPanel;
    private QuizPanel quizPanel;

    // Tab 1 Components
    private JTable vocabTable;
    private DefaultTableModel tableModel;
    private ModernTextField searchField;
    private ModernTextField idField;
    private ModernTextField termField;
    private ModernTextField definitionField;
    private ModernTextField exampleField;
    private JComboBox<String> levelCombo;
    private StyledButton addButton;
    private StyledButton updateButton;
    private StyledButton deleteButton;
    private StyledButton clearButton;

    // Metric Strip Components
    private JLabel totalMetricLabel;
    private JLabel reviewMetricLabel;
    private JLabel masteredMetricLabel;
    private ModernProgressBar masteryProgressBar;
    private JLabel masteryPercentLabel;

    // Notion / SaaS Color Palette
    public static final Color COLOR_APP_BG = new Color(0xF8, 0xFA, 0xFC);       // #F8FAFC
    public static final Color COLOR_SURFACE = new Color(0xFF, 0xFF, 0xFF);      // #FFFFFF
    public static final Color COLOR_BORDER = new Color(0xE2, 0xE8, 0xF0);       // #E2E8F0
    public static final Color COLOR_BORDER_INPUT = new Color(0xCB, 0xD5, 0xE1); // #CBD5E1
    public static final Color COLOR_DIVIDER = new Color(0xF1, 0xF5, 0xF9);      // #F1F5F9

    // Accents
    public static final Color COLOR_PRIMARY = new Color(0x4F, 0x46, 0xE5);      // #4F46E5 Indigo
    public static final Color COLOR_PRIMARY_HOVER = new Color(0x43, 0x38, 0xCA);// #4338CA
    public static final Color COLOR_PRIMARY_TINT = new Color(0xEE, 0xF2, 0xFF); // #EEF2FF

    public static final Color COLOR_SUCCESS = new Color(0x10, 0xB9, 0x81);      // #10B981 Emerald
    public static final Color COLOR_SUCCESS_HOVER = new Color(0x05, 0x96, 0x69);// #059669
    public static final Color COLOR_SUCCESS_TINT = new Color(0xD1, 0xFA, 0xE5); // #D1FAE5

    public static final Color COLOR_WARNING = new Color(0xF5, 0x9E, 0x0B);      // #F59E0B Amber
    public static final Color COLOR_WARNING_HOVER = new Color(0xD9, 0x77, 0x06);// #D97706
    public static final Color COLOR_WARNING_TINT = new Color(0xFE, 0xF3, 0xC7); // #FEF3C7

    public static final Color COLOR_DANGER = new Color(0xEF, 0x44, 0x44);       // #EF4444 Rose
    public static final Color COLOR_DANGER_HOVER = new Color(0xDC, 0x26, 0x26); // #DC2626
    public static final Color COLOR_DANGER_TINT = new Color(0xFE, 0xE2, 0xE2);  // #FEE2E2

    // Text Colors
    public static final Color COLOR_TEXT_TITLE = new Color(0x0F, 0x17, 0x2A);   // #0F172A
    public static final Color COLOR_TEXT_BODY = new Color(0x33, 0x41, 0x55);    // #334155
    public static final Color COLOR_TEXT_MUTED = new Color(0x64, 0x74, 0x8B);   // #64748B

    // Typography
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);

    public MainFrame() {
        super("English Vocabulary Flashcard & Quiz Mastery");
        this.manager = new FlashcardManager();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 720);
        setMinimumSize(new Dimension(940, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_APP_BG);

        initUI();
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_BODY_BOLD);
        tabbedPane.setBackground(COLOR_APP_BG);
        tabbedPane.setForeground(COLOR_TEXT_BODY);

        // Tab 1: Vocabulary List
        JPanel vocabListPanel = createVocabularyListTab();
        tabbedPane.addTab("Vocabulary List", vocabListPanel);

        // Tab 2: Flashcard Mode
        flashcardPanel = new FlashcardPanel(manager);
        tabbedPane.addTab("Flashcard Mode", flashcardPanel);

        // Tab 3: Quiz Practice
        quizPanel = new QuizPanel(manager);
        tabbedPane.addTab("Quiz Practice", quizPanel);

        // Tab switch listener to synchronize data across tabs
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                refreshTable(manager.getCards());
                updateMetrics();
            } else if (selectedIndex == 1) {
                flashcardPanel.reloadCards();
            } else if (selectedIndex == 2) {
                quizPanel.reloadQuiz();
            }
        });

        add(tabbedPane);
        refreshTable(manager.getCards());
        updateMetrics();
    }

    /**
     * Builds Tab 1 - Vocabulary List with top metric dashboard, polished JTable,
     * and a modern rounded card container for the CRUD form.
     */
    private JPanel createVocabularyListTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));
        panel.setBackground(COLOR_APP_BG);

        // ==========================================
        // 1. TOP HEADER / DASHBOARD CARD
        // ==========================================
        ModernCardPanel topCard = new ModernCardPanel(14, new EmptyBorder(12, 16, 12, 16));
        topCard.setLayout(new BorderLayout(16, 10));

        // Left: Clean Search Field with Vector Magnifier Icon
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBox.setOpaque(false);

        JLabel searchIconLabel = new JLabel("Search:");
        searchIconLabel.setFont(FONT_BODY_BOLD);
        searchIconLabel.setForeground(COLOR_TEXT_TITLE);

        searchField = new ModernTextField(24, true);
        searchField.setPlaceholder("Search term, definition, or example...");
        searchField.setFont(FONT_BODY);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { doSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { doSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { doSearch(); }
        });

        searchBox.add(searchIconLabel);
        searchBox.add(searchField);
        topCard.add(searchBox, BorderLayout.WEST);

        // Right: Metric Strip & Slim Progress Bar
        JPanel metricsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        metricsPanel.setOpaque(false);

        totalMetricLabel = createPillBadge("Total: 0", COLOR_DIVIDER, COLOR_TEXT_BODY, COLOR_BORDER);
        reviewMetricLabel = createPillBadge("Need Review: 0", COLOR_WARNING_TINT, new Color(0xB4, 0x53, 0x09), new Color(0xFD, 0xE6, 0x8A));
        masteredMetricLabel = createPillBadge("Mastered: 0", COLOR_SUCCESS_TINT, new Color(0x06, 0x5F, 0x46), new Color(0xA7, 0xF3, 0xD0));

        masteryProgressBar = new ModernProgressBar(COLOR_PRIMARY, 8, 110);
        masteryPercentLabel = new JLabel("0% Mastered");
        masteryPercentLabel.setFont(FONT_SMALL_BOLD);
        masteryPercentLabel.setForeground(COLOR_PRIMARY);

        metricsPanel.add(totalMetricLabel);
        metricsPanel.add(reviewMetricLabel);
        metricsPanel.add(masteredMetricLabel);
        metricsPanel.add(Box.createHorizontalStrut(4));
        metricsPanel.add(masteryProgressBar);
        metricsPanel.add(masteryPercentLabel);

        topCard.add(metricsPanel, BorderLayout.EAST);
        panel.add(topCard, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER: JTABLE CONTAINER CARD
        // ==========================================
        String[] columns = {"ID", "Term", "Definition", "Mastery Level"};
        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };

        vocabTable = new JTable(tableModel);
        vocabTable.setRowHeight(40); // 40px row height
        vocabTable.setFont(FONT_BODY);
        vocabTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vocabTable.setShowVerticalLines(false);
        vocabTable.setShowHorizontalLines(true);
        vocabTable.setGridColor(COLOR_DIVIDER);
        vocabTable.setSelectionBackground(COLOR_PRIMARY_TINT);
        vocabTable.setSelectionForeground(new Color(0x1E, 0x1B, 0x4B));

        // Header Styling (42px height, flat background #F1F5F9, bold #475569)
        JTableHeader header = vocabTable.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        header.setFont(FONT_BODY_BOLD);
        header.setBackground(COLOR_DIVIDER);
        header.setForeground(new Color(0x47, 0x55, 0x69));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new ModernHeaderRenderer());

        // Column widths
        vocabTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        vocabTable.getColumnModel().getColumn(0).setMaxWidth(75);
        vocabTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        vocabTable.getColumnModel().getColumn(2).setPreferredWidth(480);
        vocabTable.getColumnModel().getColumn(3).setPreferredWidth(160);

        // Cell Renderers with generous padding (14px) and zebra striping
        ModernTextCellRenderer textRenderer = new ModernTextCellRenderer();
        ModernTextCellRenderer idRenderer = new ModernTextCellRenderer(SwingConstants.CENTER, COLOR_TEXT_MUTED);

        vocabTable.getColumnModel().getColumn(0).setCellRenderer(idRenderer);
        vocabTable.getColumnModel().getColumn(1).setCellRenderer(new ModernTextCellRenderer(SwingConstants.LEFT, COLOR_TEXT_TITLE, true));
        vocabTable.getColumnModel().getColumn(2).setCellRenderer(textRenderer);
        vocabTable.getColumnModel().getColumn(3).setCellRenderer(new LevelBadgeCellRenderer());

        // Row Selection Listener
        vocabTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && vocabTable.getSelectedRow() != -1) {
                int modelRow = vocabTable.convertRowIndexToModel(vocabTable.getSelectedRow());
                int cardId = (int) tableModel.getValueAt(modelRow, 0);
                Flashcard card = manager.getCardById(cardId);
                if (card != null) {
                    populateForm(card);
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(vocabTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));
        tableScroll.getViewport().setBackground(COLOR_SURFACE);

        panel.add(tableScroll, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM: FORM CARD CONTAINER
        // ==========================================
        ModernCardPanel formCard = new ModernCardPanel(16, new EmptyBorder(16, 20, 16, 20));
        formCard.setLayout(new BorderLayout(12, 12));

        // Form Card Header
        JPanel formHeader = new JPanel(new BorderLayout(8, 2));
        formHeader.setOpaque(false);

        JLabel formTitle = new JLabel("Card Details & Actions");
        formTitle.setFont(FONT_SUBHEADING);
        formTitle.setForeground(COLOR_TEXT_TITLE);

        JLabel formSubtitle = new JLabel("Select a card from the table above to edit, or fill fields below to create a new entry.");
        formSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formSubtitle.setForeground(COLOR_TEXT_MUTED);

        formHeader.add(formTitle, BorderLayout.NORTH);
        formHeader.add(formSubtitle, BorderLayout.SOUTH);
        formCard.add(formHeader, BorderLayout.NORTH);

        // Form Inputs Grid
        JPanel formFields = new JPanel(new GridBagLayout());
        formFields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new ModernTextField(4);
        idField.setEditable(false);
        idField.setBackground(COLOR_DIVIDER);
        idField.setForeground(COLOR_TEXT_MUTED);
        idField.setFont(FONT_BODY);

        termField = new ModernTextField(14);
        termField.setPlaceholder("e.g. ephemeral");

        definitionField = new ModernTextField(24);
        definitionField.setPlaceholder("e.g. lasting for a very short time; transitory");

        exampleField = new ModernTextField(24);
        exampleField.setPlaceholder("e.g. Fame in the digital era is often ephemeral.");

        levelCombo = new JComboBox<>(new String[]{"1 - Learning", "2 - Reviewing", "3 - Mastered"});
        levelCombo.setFont(FONT_BODY);
        levelCombo.setBackground(COLOR_SURFACE);
        levelCombo.setForeground(COLOR_TEXT_BODY);

        // Row 1: ID, Term, Level
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formFields.add(createFieldLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.08;
        formFields.add(idField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formFields.add(createFieldLabel("Term:*"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.42;
        formFields.add(termField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        formFields.add(createFieldLabel("Level:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.42;
        formFields.add(levelCombo, gbc);

        // Row 2: Definition, Example
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formFields.add(createFieldLabel("Definition:*"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.50;
        formFields.add(definitionField, gbc);

        gbc.gridx = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        formFields.add(createFieldLabel("Example:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.50;
        formFields.add(exampleField, gbc);

        formCard.add(formFields, BorderLayout.CENTER);

        // Action Buttons Row (clean typography without broken emoji glyphs)
        JPanel buttonsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttonsBar.setOpaque(false);

        clearButton = new StyledButton("Clear Form", COLOR_DIVIDER, COLOR_TEXT_BODY, COLOR_BORDER, FONT_BODY_BOLD);
        clearButton.addActionListener(e -> clearForm());

        addButton = new StyledButton("Add Word", COLOR_PRIMARY, Color.WHITE, COLOR_PRIMARY, FONT_BODY_BOLD);
        addButton.addActionListener(e -> handleAddCard());

        updateButton = new StyledButton("Update", COLOR_SUCCESS, Color.WHITE, COLOR_SUCCESS, FONT_BODY_BOLD);
        updateButton.addActionListener(e -> handleUpdateCard());

        deleteButton = new StyledButton("Delete", COLOR_DANGER, Color.WHITE, COLOR_DANGER, FONT_BODY_BOLD);
        deleteButton.addActionListener(e -> handleDeleteCard());

        buttonsBar.add(clearButton);
        buttonsBar.add(addButton);
        buttonsBar.add(updateButton);
        buttonsBar.add(deleteButton);

        formCard.add(buttonsBar, BorderLayout.SOUTH);
        panel.add(formCard, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY_BOLD);
        label.setForeground(COLOR_TEXT_TITLE);
        return label;
    }

    private JLabel createPillBadge(String text, Color bg, Color fg, Color border) {
        JLabel pill = new JLabel(text, SwingConstants.CENTER) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(FONT_SMALL_BOLD);
        pill.setForeground(fg);
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(4, 10, 4, 10));
        return pill;
    }

    private void updateMetrics() {
        int total = manager.getTotalCount();
        int needReview = manager.getNeedReviewCount();
        int mastered = manager.getMasteredCount();
        int percentage = manager.getMasteryPercentage();

        totalMetricLabel.setText("Total: " + total);
        reviewMetricLabel.setText("Need Review: " + needReview);
        masteredMetricLabel.setText("Mastered: " + mastered);

        masteryProgressBar.setValue(percentage);
        masteryPercentLabel.setText(percentage + "% Mastered");
    }

    private void doSearch() {
        String query = searchField.getText();
        List<Flashcard> results = manager.searchCards(query);
        refreshTable(results);
    }

    private void refreshTable(List<Flashcard> list) {
        tableModel.setRowCount(0);
        for (Flashcard card : list) {
            tableModel.addRow(new Object[]{
                    card.getId(),
                    card.getTerm(),
                    card.getDefinition(),
                    card.getLevel()
            });
        }
    }

    private void populateForm(Flashcard card) {
        idField.setText(String.valueOf(card.getId()));
        termField.setText(card.getTerm());
        definitionField.setText(card.getDefinition());
        exampleField.setText(card.getExample());
        levelCombo.setSelectedIndex(card.getLevel() - 1);
    }

    private void clearForm() {
        idField.setText("");
        termField.setText("");
        definitionField.setText("");
        exampleField.setText("");
        levelCombo.setSelectedIndex(0);
        vocabTable.clearSelection();
    }

    private void handleAddCard() {
        String term = termField.getText().trim();
        String definition = definitionField.getText().trim();
        String example = exampleField.getText().trim();
        int level = levelCombo.getSelectedIndex() + 1;

        if (term.isEmpty() || definition.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Both 'Term' and 'Definition' are required fields.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Flashcard card = new Flashcard(manager.getNextId(), term, definition, example, level);
        manager.addCard(card);
        clearForm();
        doSearch();
        updateMetrics();
        JOptionPane.showMessageDialog(this, "Card added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleUpdateCard() {
        String idText = idField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a card from the table to update.",
                    "No Card Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String term = termField.getText().trim();
        String definition = definitionField.getText().trim();
        String example = exampleField.getText().trim();
        int level = levelCombo.getSelectedIndex() + 1;

        if (term.isEmpty() || definition.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Both 'Term' and 'Definition' cannot be empty.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = Integer.parseInt(idText);
        Flashcard updatedCard = new Flashcard(id, term, definition, example, level);
        boolean success = manager.updateCard(updatedCard);
        if (success) {
            clearForm();
            doSearch();
            updateMetrics();
            JOptionPane.showMessageDialog(this, "Card updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update card. ID not found.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteCard() {
        String idText = idField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a card from the table to delete.",
                    "No Card Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = Integer.parseInt(idText);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete vocabulary card #" + id + " (" + termField.getText() + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = manager.deleteCard(id);
            if (success) {
                clearForm();
                doSearch();
                updateMetrics();
                JOptionPane.showMessageDialog(this, "Card deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    // ==========================================
    // REUSABLE CUSTOM COMPONENTS
    // ==========================================

    /**
     * Modern rounded card panel with pure white body, anti-aliased rounded corners,
     * soft drop shadow, and subtle border.
     */
    public static class ModernCardPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int cornerRadius;

        public ModernCardPanel() {
            this(16, new EmptyBorder(20, 24, 20, 24));
        }

        public ModernCardPanel(int cornerRadius, EmptyBorder border) {
            this.cornerRadius = cornerRadius;
            setLayout(new BorderLayout());
            setBorder(border);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 2;
            int y = 2;
            int w = getWidth() - 4;
            int h = getHeight() - 5;

            // Ambient soft drop shadow
            g2.setColor(new Color(15, 23, 42, 6));
            g2.fillRoundRect(x, y + 3, w, h, cornerRadius, cornerRadius);
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(x, y + 1, w, h, cornerRadius, cornerRadius);

            // Card body
            g2.setColor(COLOR_SURFACE);
            g2.fillRoundRect(x, y, w, h, cornerRadius, cornerRadius);

            // Subtle border
            g2.setColor(COLOR_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(x, y, w - 1, h - 1, cornerRadius, cornerRadius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Reusable custom button that renders anti-aliased rounded corners (radius 8px),
     * smooth hover transitions, and clean typography.
     */
    public static class StyledButton extends JButton {
        private static final long serialVersionUID = 1L;

        private Color normalBg;
        private Color hoverBg;
        private Color pressedBg;
        private Color normalBorder;
        private int radius = 8;

        public StyledButton(String text, Color bg, Color fg, Font font) {
            this(text, bg, fg, bg, font);
        }

        public StyledButton(String text, Color bg, Color fg, Color border, Font font) {
            super(text);
            initColors(bg, fg, border);

            setFont(font);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));
        }

        public void setCornerRadius(int r) {
            this.radius = r;
            repaint();
        }

        private void initColors(Color bg, Color fg, Color border) {
            this.normalBg = bg;
            this.hoverBg = computeHover(bg);
            this.pressedBg = computePressed(bg);
            this.normalBorder = border;
            setForeground(fg);
        }

        private static Color computeHover(Color c) {
            if (c.equals(COLOR_DIVIDER)) return new Color(0xE2, 0xE8, 0xF0);
            if (c.equals(COLOR_PRIMARY)) return COLOR_PRIMARY_HOVER;
            if (c.equals(COLOR_SUCCESS)) return COLOR_SUCCESS_HOVER;
            if (c.equals(COLOR_DANGER)) return COLOR_DANGER_HOVER;
            if (c.equals(COLOR_WARNING)) return COLOR_WARNING_HOVER;
            int r = Math.min(255, c.getRed() + 18);
            int g = Math.min(255, c.getGreen() + 18);
            int b = Math.min(255, c.getBlue() + 18);
            return new Color(r, g, b);
        }

        private static Color computePressed(Color c) {
            return c.darker();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(new Color(241, 245, 249));
            } else if (getModel().isPressed()) {
                g2.setColor(pressedBg);
            } else if (getModel().isRollover()) {
                g2.setColor(hoverBg);
            } else {
                g2.setColor(normalBg);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            // Border
            if (!isEnabled()) {
                g2.setColor(COLOR_BORDER);
            } else {
                g2.setColor(normalBorder);
            }
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Modern text field with internal padding, subtle border, smooth background,
     * focus highlight ring, and optional search icon with placeholder text.
     */
    public static class ModernTextField extends JTextField {
        private static final long serialVersionUID = 1L;

        private String placeholder = "";
        private boolean isFocused = false;
        private boolean hasSearchIcon = false;

        public ModernTextField(int columns) {
            this(columns, false);
        }

        public ModernTextField(int columns, boolean hasSearchIcon) {
            super(columns);
            this.hasSearchIcon = hasSearchIcon;
            setFont(FONT_BODY);
            setOpaque(false);
            setBorder(new EmptyBorder(8, hasSearchIcon ? 28 : 12, 8, 12));
            setBackground(COLOR_SURFACE);
            setForeground(COLOR_TEXT_BODY);

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fill background
            g2.setColor(isEditable() ? getBackground() : COLOR_DIVIDER);
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);

            // Border / Focus ring
            if (isFocused && isEditable()) {
                g2.setColor(COLOR_PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(COLOR_BORDER_INPUT);
                g2.setStroke(new BasicStroke(1.0f));
            }
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);

            // Vector Magnifier Icon for search fields
            if (hasSearchIcon) {
                g2.setColor(isFocused ? COLOR_PRIMARY : new Color(0x94, 0xA3, 0xB8));
                g2.setStroke(new BasicStroke(1.6f));
                int cx = 14;
                int cy = getHeight() / 2 - 1;
                g2.drawOval(cx - 5, cy - 5, 8, 8);
                g2.drawLine(cx + 1, cy + 1, cx + 5, cy + 5);
            }

            g2.dispose();
            super.paintComponent(g);

            // Paint placeholder text if empty
            if (getText().isEmpty() && !placeholder.isEmpty() && !isFocused) {
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g3.setColor(new Color(0x94, 0xA3, 0xB8)); // #94A3B8
                g3.setFont(getFont());
                FontMetrics fm = g3.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g3.drawString(placeholder, hasSearchIcon ? 28 : 12, y);
                g3.dispose();
            }
        }
    }

    /**
     * Slim elegant progress bar with rounded track and accent colored fill.
     */
    public static class ModernProgressBar extends JComponent {
        private static final long serialVersionUID = 1L;
        private int value = 0;
        private final Color accentColor;
        private final int barHeight;

        public ModernProgressBar(Color accentColor, int barHeight, int barWidth) {
            this.accentColor = accentColor;
            this.barHeight = barHeight;
            setPreferredSize(new Dimension(barWidth, barHeight + 4));
            setOpaque(false);
        }

        public void setValue(int v) {
            this.value = Math.max(0, Math.min(100, v));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int y = (getHeight() - barHeight) / 2;
            int width = getWidth();

            // Track
            g2.setColor(COLOR_BORDER);
            g2.fillRoundRect(0, y, width, barHeight, barHeight, barHeight);

            // Fill
            if (value > 0) {
                int fillWidth = Math.max(barHeight, (int) Math.round((width * value) / 100.0));
                g2.setColor(accentColor);
                g2.fillRoundRect(0, y, fillWidth, barHeight, barHeight, barHeight);
            }

            g2.dispose();
        }
    }

    /**
     * Custom Table Header Renderer matching SaaS flat aesthetic.
     */
    private static class ModernHeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        public ModernHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setFont(FONT_BODY_BOLD);
            setBackground(COLOR_DIVIDER);
            setForeground(new Color(0x47, 0x55, 0x69));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 0 || column == 3) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(COLOR_BORDER);
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }

    /**
     * Custom Table Cell Renderer with generous padding, zebra striping, and clean typography.
     */
    private static class ModernTextCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final int align;
        private final Color customFg;
        private final boolean isBold;

        public ModernTextCellRenderer() {
            this(SwingConstants.LEFT, COLOR_TEXT_BODY, false);
        }

        public ModernTextCellRenderer(int align, Color customFg) {
            this(align, customFg, false);
        }

        public ModernTextCellRenderer(int align, Color customFg, boolean isBold) {
            this.align = align;
            this.customFg = customFg;
            this.isBold = isBold;
            setBorder(new EmptyBorder(0, 14, 0, 14));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(align);
            setFont(isBold ? FONT_BODY_BOLD : FONT_BODY);

            if (isSelected) {
                setBackground(COLOR_PRIMARY_TINT);
                setForeground(new Color(0x1E, 0x1B, 0x4B));
            } else {
                setBackground((row % 2 == 0) ? COLOR_SURFACE : COLOR_APP_BG);
                setForeground(customFg);
            }
            return this;
        }
    }

    /**
     * Custom Table Cell Renderer that renders mastery levels as modern pill badges.
     */
    private static class LevelBadgeCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final JLabel badgeLabel;
        private Color currentBg;
        private Color currentBorder;

        public LevelBadgeCellRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(true);

            badgeLabel = new JLabel() {
                private static final long serialVersionUID = 1L;
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(currentBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.setColor(currentBorder);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badgeLabel.setFont(FONT_SMALL_BOLD);
            badgeLabel.setOpaque(false);
            badgeLabel.setBorder(new EmptyBorder(3, 12, 3, 12));
            add(badgeLabel);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(COLOR_PRIMARY_TINT);
            } else {
                setBackground((row % 2 == 0) ? COLOR_SURFACE : COLOR_APP_BG);
            }

            int level = 1;
            if (value instanceof Integer) {
                level = (Integer) value;
            } else if (value != null) {
                try {
                    level = Integer.parseInt(value.toString());
                } catch (Exception ignored) {
                }
            }

            switch (level) {
                case Flashcard.LEVEL_REVIEWING:
                    currentBg = Flashcard.COLOR_REVIEWING_BG;
                    currentBorder = Flashcard.COLOR_REVIEWING_BORDER;
                    badgeLabel.setForeground(Flashcard.COLOR_REVIEWING_TEXT);
                    badgeLabel.setText("● Reviewing");
                    break;
                case Flashcard.LEVEL_MASTERED:
                    currentBg = Flashcard.COLOR_MASTERED_BG;
                    currentBorder = Flashcard.COLOR_MASTERED_BORDER;
                    badgeLabel.setForeground(Flashcard.COLOR_MASTERED_TEXT);
                    badgeLabel.setText("● Mastered");
                    break;
                case Flashcard.LEVEL_LEARNING:
                default:
                    currentBg = Flashcard.COLOR_LEARNING_BG;
                    currentBorder = Flashcard.COLOR_LEARNING_BORDER;
                    badgeLabel.setForeground(Flashcard.COLOR_LEARNING_TEXT);
                    badgeLabel.setText("● Learning");
                    break;
            }

            return this;
        }
    }

    /**
     * Application entry point.
     * Applies FlatLightLaf with fallback to system look and feel,
     * initializes FlashcardManager, and displays the main window.
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        boolean lafLoaded = false;
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            LookAndFeel flatLaf = (LookAndFeel) flatLafClass.getDeclaredConstructor().newInstance();
            UIManager.setLookAndFeel(flatLaf);
            lafLoaded = true;
        } catch (Exception e) {
            // FlatLaf is not in classpath or failed to load
        }

        if (!lafLoaded) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Adjust UI defaults for clean font rendering
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 18, 8, 18));
        UIManager.put("TabbedPane.selected", COLOR_SURFACE);
        UIManager.put("TabbedPane.background", COLOR_APP_BG);

        SwingUtilities.invokeLater(() -> {
            MainFrame app = new MainFrame();
            app.setVisible(true);
        });
    }
}
