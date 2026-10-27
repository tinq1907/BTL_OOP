import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

/**
 * Main application window for the English Vocabulary Flashcard & Quiz desktop application.
 * Manages the top-level JTabbedPane containing:
 *  1. Vocabulary List (JTable, Search, Form CRUD)
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
    private JTextField searchField;
    private JTextField idField;
    private JTextField termField;
    private JTextField definitionField;
    private JTextField exampleField;
    private JComboBox<String> levelCombo;
    private StyledButton addButton;
    private StyledButton updateButton;
    private StyledButton deleteButton;
    private StyledButton clearButton;

    // Consistent Color Palette
    private static final Color COLOR_TEXT_DARK = new Color(33, 37, 41);   // #212529
    private static final Color COLOR_PRIMARY_BLUE = new Color(13, 110, 253); // #0D6EFD
    private static final Color COLOR_GREEN = new Color(25, 135, 84);       // #198754
    private static final Color COLOR_RED = new Color(220, 53, 69);         // #DC3545
    private static final Color COLOR_GRAY_BG = new Color(240, 240, 240);   // #F0F0F0

    // Typography
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    public MainFrame() {
        super("English Vocabulary Flashcard & Quiz Mastery");
        this.manager = new FlashcardManager();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setMinimumSize(new Dimension(880, 580));
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_BODY_BOLD);

        // Tab 1: Vocabulary List (Plain text, no glyphs)
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
            } else if (selectedIndex == 1) {
                flashcardPanel.reloadCards();
            } else if (selectedIndex == 2) {
                quizPanel.reloadQuiz();
            }
        });

        add(tabbedPane);
        refreshTable(manager.getCards());
    }

    /**
     * Builds Tab 1 - Vocabulary List with table, clean search bar, and CRUD input form.
     */
    private JPanel createVocabularyListTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(15, 18, 15, 18));
        panel.setBackground(new Color(248, 250, 252));

        // --- Top Bar: Search Bar Only (Export CSV removed) ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topBar.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(FONT_BODY_BOLD);
        searchLabel.setForeground(COLOR_TEXT_DARK);

        searchField = new JTextField(28);
        searchField.setFont(FONT_BODY);
        searchField.putClientProperty("JTextField.placeholderText", "Filter by term, definition, or example...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { doSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { doSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { doSearch(); }
        });

        topBar.add(searchLabel);
        topBar.add(searchField);
        panel.add(topBar, BorderLayout.NORTH);

        // --- Center: Table of Cards ---
        String[] columns = {"ID", "Term", "Definition", "Level"};
        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };

        vocabTable = new JTable(tableModel);
        vocabTable.setRowHeight(32);
        vocabTable.setFont(FONT_BODY);
        vocabTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vocabTable.setShowGrid(true);
        vocabTable.setGridColor(new Color(226, 232, 240));

        // Column widths and alignment
        JTableHeader header = vocabTable.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(COLOR_TEXT_DARK);

        vocabTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        vocabTable.getColumnModel().getColumn(0).setMaxWidth(70);
        vocabTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        vocabTable.getColumnModel().getColumn(2).setPreferredWidth(450);
        vocabTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        // Center align ID and Level columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        vocabTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        vocabTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Populate form fields on row selection
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
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230), 1));
        panel.add(tableScroll, BorderLayout.CENTER);

        // --- Bottom: Input Form & Action Buttons ---
        JPanel bottomContainer = new JPanel(new BorderLayout(10, 10));
        bottomContainer.setOpaque(false);
        bottomContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
                " Card Details & Actions ",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                FONT_BODY_BOLD,
                COLOR_TEXT_DARK
        ));

        JPanel formFields = new JPanel(new GridBagLayout());
        formFields.setOpaque(false);
        formFields.setBorder(new EmptyBorder(8, 12, 8, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField(4);
        idField.setEditable(false);
        idField.setBackground(new Color(233, 236, 239));
        idField.setFont(FONT_BODY);

        termField = new JTextField(15);
        termField.setFont(FONT_BODY);

        definitionField = new JTextField(25);
        definitionField.setFont(FONT_BODY);

        exampleField = new JTextField(25);
        exampleField.setFont(FONT_BODY);

        levelCombo = new JComboBox<>(new String[]{"1 - Learning", "2 - Reviewing", "3 - Mastered"});
        levelCombo.setFont(FONT_BODY);

        // Row 1: ID, Term, Level
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formFields.add(createLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.1;
        formFields.add(idField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formFields.add(createLabel("Term:*"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.4;
        formFields.add(termField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        formFields.add(createLabel("Level:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.4;
        formFields.add(levelCombo, gbc);

        // Row 2: Definition & Example
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formFields.add(createLabel("Def:*"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.5;
        formFields.add(definitionField, gbc);

        gbc.gridx = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        formFields.add(createLabel("Example:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.5;
        formFields.add(exampleField, gbc);

        // Buttons Bar with High-Contrast Colors
        JPanel buttonsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        buttonsBar.setOpaque(false);

        clearButton = new StyledButton("Clear", COLOR_GRAY_BG, COLOR_TEXT_DARK, FONT_BODY_BOLD);
        clearButton.addActionListener(e -> clearForm());

        addButton = new StyledButton("Add Word", COLOR_PRIMARY_BLUE, Color.WHITE, FONT_BODY_BOLD);
        addButton.addActionListener(e -> handleAddCard());

        updateButton = new StyledButton("Update", COLOR_GREEN, Color.WHITE, FONT_BODY_BOLD);
        updateButton.addActionListener(e -> handleUpdateCard());

        deleteButton = new StyledButton("Delete", COLOR_RED, Color.WHITE, FONT_BODY_BOLD);
        deleteButton.addActionListener(e -> handleDeleteCard());

        buttonsBar.add(clearButton);
        buttonsBar.add(addButton);
        buttonsBar.add(updateButton);
        buttonsBar.add(deleteButton);

        bottomContainer.add(formFields, BorderLayout.CENTER);
        bottomContainer.add(buttonsBar, BorderLayout.SOUTH);
        panel.add(bottomContainer, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY_BOLD);
        label.setForeground(COLOR_TEXT_DARK);
        return label;
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
                    card.getLevelLabel() + " (L" + card.getLevel() + ")"
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
                JOptionPane.showMessageDialog(this, "Card deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            }
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

        SwingUtilities.invokeLater(() -> {
            MainFrame app = new MainFrame();
            app.setVisible(true);
        });
    }

    /**
     * Reusable custom button that renders crisp, high-contrast colors
     * reliably across any Look and Feel.
     */
    public static class StyledButton extends JButton {
        private static final long serialVersionUID = 1L;

        private Color normalBg;
        private Color hoverBg;
        private Color pressedBg;
        private Color borderColor;

        public StyledButton(String text, Color bg, Color fg, Font font) {
            super(text);
            initColors(bg, fg);

            setFont(font);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));
        }

        private void initColors(Color bg, Color fg) {
            this.normalBg = bg;
            this.hoverBg = computeHover(bg);
            this.pressedBg = bg.darker();
            this.borderColor = bg.equals(COLOR_GRAY_BG) ? new Color(206, 212, 218) : bg.darker();
            setForeground(fg);
        }

        private static Color computeHover(Color c) {
            int r = Math.min(255, c.getRed() + 20);
            int g = Math.min(255, c.getGreen() + 20);
            int b = Math.min(255, c.getBlue() + 20);
            return new Color(r, g, b);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(new Color(233, 236, 239));
            } else if (getModel().isPressed()) {
                g2.setColor(pressedBg);
            } else if (getModel().isRollover()) {
                g2.setColor(hoverBg);
            } else {
                g2.setColor(normalBg);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

            // Border
            if (!isEnabled()) {
                g2.setColor(new Color(206, 212, 218));
            } else {
                g2.setColor(borderColor);
            }
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            g2.dispose();

            super.paintComponent(g);
        }
    }
}
