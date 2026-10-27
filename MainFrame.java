import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Main application window for the English Vocabulary Flashcard & Quiz desktop application.
 * Manages the top-level JTabbedPane containing:
 *  1. Vocabulary List (JTable, Search, Form CRUD, CSV Export)
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
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton exportButton;

    public MainFrame() {
        super("English Vocabulary Flashcard & Quiz Mastery");
        this.manager = new FlashcardManager();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setMinimumSize(new Dimension(860, 580));
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Vocabulary List
        JPanel vocabListPanel = createVocabularyListTab();
        tabbedPane.addTab("📖 Vocabulary List", vocabListPanel);

        // Tab 2: Flashcard Mode
        flashcardPanel = new FlashcardPanel(manager);
        tabbedPane.addTab("🗂 Flashcard Mode", flashcardPanel);

        // Tab 3: Quiz Practice
        quizPanel = new QuizPanel(manager);
        tabbedPane.addTab("🎯 Quiz Practice", quizPanel);

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
     * Builds Tab 1 - Vocabulary List with table, search bar, and CRUD input form.
     */
    private JPanel createVocabularyListTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(15, 18, 15, 18));
        panel.setBackground(new Color(248, 250, 252));

        // --- Top Bar: Search Bar & Export CSV Button ---
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setOpaque(false);

        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchWrapper.setOpaque(false);

        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchField = new JTextField(24);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Filter by term, definition, or example...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { doSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { doSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { doSearch(); }
        });
        searchWrapper.add(searchLabel);
        searchWrapper.add(searchField);

        exportButton = createStyledButton("📤 Export CSV", new Color(71, 85, 105), Color.WHITE);
        exportButton.addActionListener(e -> handleExportCsv());

        topBar.add(searchWrapper, BorderLayout.WEST);
        topBar.add(exportButton, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // --- Center: Table of Cards ---
        String[] columns = {"ID", "Term", "Definition", "Level"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };

        vocabTable = new JTable(tableModel);
        vocabTable.setRowHeight(30);
        vocabTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        vocabTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vocabTable.setShowGrid(true);
        vocabTable.setGridColor(new Color(226, 232, 240));

        // Column widths and alignment
        JTableHeader header = vocabTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(new Color(51, 65, 85));

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
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        panel.add(tableScroll, BorderLayout.CENTER);

        // --- Bottom: Input Form & Action Buttons ---
        JPanel bottomContainer = new JPanel(new BorderLayout(10, 10));
        bottomContainer.setOpaque(false);
        bottomContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                " Card Details & Actions ",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(71, 85, 105)
        ));

        JPanel formFields = new JPanel(new GridBagLayout());
        formFields.setOpaque(false);
        formFields.setBorder(new EmptyBorder(8, 12, 8, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField(4);
        idField.setEditable(false);
        idField.setBackground(new Color(241, 245, 249));

        termField = new JTextField(15);
        definitionField = new JTextField(25);
        exampleField = new JTextField(25);

        levelCombo = new JComboBox<>(new String[]{"1 - Learning", "2 - Reviewing", "3 - Mastered"});

        // Row 1: ID, Term, Level
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formFields.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.1;
        formFields.add(idField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formFields.add(new JLabel("Term:*"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.4;
        formFields.add(termField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        formFields.add(new JLabel("Level:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.4;
        formFields.add(levelCombo, gbc);

        // Row 2: Definition & Example
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formFields.add(new JLabel("Def:*"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.5;
        formFields.add(definitionField, gbc);

        gbc.gridx = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        formFields.add(new JLabel("Example:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.5;
        formFields.add(exampleField, gbc);

        // Buttons Bar
        JPanel buttonsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        buttonsBar.setOpaque(false);

        addButton = createStyledButton("➕ Add Word", new Color(37, 99, 235), Color.WHITE);
        addButton.addActionListener(e -> handleAddCard());

        updateButton = createStyledButton("✏ Update", new Color(13, 148, 136), Color.WHITE);
        updateButton.addActionListener(e -> handleUpdateCard());

        deleteButton = createStyledButton("🗑 Delete", new Color(220, 38, 38), Color.WHITE);
        deleteButton.addActionListener(e -> handleDeleteCard());

        clearButton = createStyledButton("Clear", new Color(226, 232, 240), new Color(51, 65, 85));
        clearButton.addActionListener(e -> clearForm());

        buttonsBar.add(clearButton);
        buttonsBar.add(addButton);
        buttonsBar.add(updateButton);
        buttonsBar.add(deleteButton);

        bottomContainer.add(formFields, BorderLayout.CENTER);
        bottomContainer.add(buttonsBar, BorderLayout.SOUTH);
        panel.add(bottomContainer, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
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

    private void handleExportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Vocabulary Cards to CSV");
        fileChooser.setSelectedFile(new File("vocabulary_export.csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }
            try {
                manager.exportToCsv(fileToSave);
                JOptionPane.showMessageDialog(this,
                        "Successfully exported " + manager.getCards().size() + " cards to:\n" + fileToSave.getAbsolutePath(),
                        "Export Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to export CSV: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Application entry point.
     * Applies FlatLightLaf with fallback to system look and feel,
     * initializes FlashcardManager, and displays the main window.
     */
    public static void main(String[] args) {
        // Optimize rendering hints for crisp typography
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Attempt to apply com.formdev.flatlaf.FlatLightLaf; fallback to system L&F
        boolean lafLoaded = false;
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            LookAndFeel flatLaf = (LookAndFeel) flatLafClass.getDeclaredConstructor().newInstance();
            UIManager.setLookAndFeel(flatLaf);
            lafLoaded = true;
        } catch (Exception e) {
            // FlatLaf is not in classpath or failed to load; proceed to fallback
        }

        if (!lafLoaded) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Default to Swing standard if system L&F fails
            }
        }

        // Render application on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame app = new MainFrame();
            app.setVisible(true);
        });
    }
}
