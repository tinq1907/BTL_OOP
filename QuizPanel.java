import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Multiple-choice Quiz practice panel.
 * Prompts the user with a vocabulary term and 4 definition options (A, B, C, D).
 * Validates answers immediately, applies visual color feedback, updates Spaced Repetition,
 * and auto-transitions to the next question.
 */
public class QuizPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final transient FlashcardManager manager;
    private transient FlashcardManager.QuizQuestion currentQuestion;

    // Statistics
    private int totalAnswered = 0;
    private int correctCount = 0;
    private int currentStreak = 0;

    // UI Components
    private JLabel scoreLabel;
    private JLabel streakLabel;
    private JLabel promptLabel;
    private JLabel feedbackLabel;
    private JButton[] optionButtons;
    private JPanel optionsPanel;
    private JPanel contentCard;
    private JPanel warningPanel;
    private Timer transitionTimer;

    // Colors
    private static final Color COLOR_DEFAULT_BTN = new Color(248, 250, 252);
    private static final Color COLOR_DEFAULT_BORDER = new Color(203, 213, 225);
    private static final Color COLOR_CORRECT = new Color(34, 197, 94); // Vibrant Green
    private static final Color COLOR_INCORRECT = new Color(239, 68, 68); // Vibrant Red
    private static final Color COLOR_TEXT_DARK = new Color(30, 41, 59);

    public QuizPanel(FlashcardManager manager) {
        this.manager = manager;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 30, 20, 30));
        setBackground(new Color(245, 247, 250));

        initComponents();
        loadNextQuestion();
    }

    private void initComponents() {
        // --- Top Bar: Header and Stats ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Multiple-Choice Vocabulary Quiz");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(COLOR_TEXT_DARK);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        statsPanel.setOpaque(false);

        streakLabel = new JLabel("Streak: 0 🔥");
        streakLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        streakLabel.setForeground(new Color(234, 88, 12));

        scoreLabel = new JLabel("Score: 0 / 0 (0%)");
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        scoreLabel.setForeground(new Color(79, 70, 229));

        statsPanel.add(streakLabel);
        statsPanel.add(scoreLabel);

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(statsPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- Center: Quiz Card Container ---
        contentCard = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(6, 6, getWidth() - 12, getHeight() - 12, 20, 20);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(3, 3, getWidth() - 12, getHeight() - 12, 20, 20);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(3, 3, getWidth() - 12, getHeight() - 12, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentCard.setOpaque(false);
        contentCard.setBorder(new EmptyBorder(30, 40, 25, 40));

        // Prompt (Question Word)
        JPanel promptContainer = new JPanel();
        promptContainer.setLayout(new BoxLayout(promptContainer, BoxLayout.Y_AXIS));
        promptContainer.setOpaque(false);

        JLabel questionHeader = new JLabel("What is the correct definition for:");
        questionHeader.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        questionHeader.setForeground(new Color(100, 116, 139));
        questionHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        promptLabel = new JLabel("Vocabulary Word", SwingConstants.CENTER);
        promptLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        promptLabel.setForeground(new Color(15, 23, 42));
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        promptContainer.add(questionHeader);
        promptContainer.add(Box.createVerticalStrut(8));
        promptContainer.add(promptLabel);
        contentCard.add(promptContainer, BorderLayout.NORTH);

        // 4 Options Grid (2x2 layout)
        optionsPanel = new JPanel(new GridLayout(2, 2, 16, 16));
        optionsPanel.setOpaque(false);
        optionButtons = new JButton[4];

        for (int i = 0; i < 4; i++) {
            final int optionIndex = i;
            JButton btn = new JButton();
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setBackground(COLOR_DEFAULT_BTN);
            btn.setForeground(COLOR_TEXT_DARK);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_DEFAULT_BORDER, 1, true),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            btn.addActionListener(e -> handleOptionSelected(optionIndex));
            optionButtons[i] = btn;
            optionsPanel.add(btn);
        }
        contentCard.add(optionsPanel, BorderLayout.CENTER);

        // Feedback Label at bottom of card
        feedbackLabel = new JLabel("Choose an option to answer", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        feedbackLabel.setForeground(new Color(100, 116, 139));
        feedbackLabel.setPreferredSize(new Dimension(300, 30));
        contentCard.add(feedbackLabel, BorderLayout.SOUTH);

        // Warning Panel when fewer than 4 cards are present
        warningPanel = new JPanel(new GridBagLayout());
        warningPanel.setOpaque(false);
        JLabel warnMsg = new JLabel("<html><center><h2>At least 4 vocabulary cards are required to play Quiz Mode.</h2><p>Please switch to the <b>Vocabulary List</b> tab and add more words.</p></center></html>", SwingConstants.CENTER);
        warnMsg.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        warnMsg.setForeground(new Color(100, 116, 139));
        warningPanel.add(warnMsg);

        add(contentCard, BorderLayout.CENTER);
    }

    /**
     * Resets and loads a fresh question or shows warning if fewer than 4 cards exist.
     */
    public void reloadQuiz() {
        if (transitionTimer != null && transitionTimer.isRunning()) {
            transitionTimer.stop();
        }
        loadNextQuestion();
    }

    private void loadNextQuestion() {
        if (manager.getCards().size() < 4) {
            remove(contentCard);
            add(warningPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        } else {
            remove(warningPanel);
            add(contentCard, BorderLayout.CENTER);
            revalidate();
            repaint();
        }

        currentQuestion = manager.generateQuizQuestion();
        if (currentQuestion == null) return;

        promptLabel.setText(currentQuestion.getTargetCard().getTerm());
        feedbackLabel.setText("Select the best definition from options A–D");
        feedbackLabel.setForeground(new Color(100, 116, 139));

        char[] letters = {'A', 'B', 'C', 'D'};
        List<Flashcard> options = currentQuestion.getOptions();

        for (int i = 0; i < 4; i++) {
            JButton btn = optionButtons[i];
            btn.setEnabled(true);
            btn.setBackground(COLOR_DEFAULT_BTN);
            btn.setForeground(COLOR_TEXT_DARK);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_DEFAULT_BORDER, 1, true),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            String def = options.get(i).getDefinition();
            btn.setText("<html><b>" + letters[i] + ".</b> " + def + "</html>");
        }
    }

    private void handleOptionSelected(int selectedIndex) {
        if (currentQuestion == null) return;

        // Disable all buttons immediately to prevent multiple clicks
        for (JButton btn : optionButtons) {
            btn.setEnabled(false);
        }

        int correctIndex = currentQuestion.getCorrectOptionIndex();
        boolean isCorrect = (selectedIndex == correctIndex);
        totalAnswered++;

        // Update Spaced Repetition logic
        manager.recordReview(currentQuestion.getTargetCard(), isCorrect);

        // Visual coloring
        if (isCorrect) {
            correctCount++;
            currentStreak++;
            JButton correctBtn = optionButtons[selectedIndex];
            correctBtn.setBackground(COLOR_CORRECT);
            correctBtn.setForeground(Color.WHITE);
            correctBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_CORRECT.darker(), 2, true),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            feedbackLabel.setText("✔ Correct! Promoted to " + currentQuestion.getTargetCard().getLevelLabel() + " (Level " + currentQuestion.getTargetCard().getLevel() + ")");
            feedbackLabel.setForeground(COLOR_CORRECT);
        } else {
            currentStreak = 0;
            // Highlight selected incorrect button
            JButton wrongBtn = optionButtons[selectedIndex];
            wrongBtn.setBackground(COLOR_INCORRECT);
            wrongBtn.setForeground(Color.WHITE);
            wrongBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_INCORRECT.darker(), 2, true),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            // Highlight the actual correct button in green
            JButton rightBtn = optionButtons[correctIndex];
            rightBtn.setBackground(COLOR_CORRECT);
            rightBtn.setForeground(Color.WHITE);
            rightBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_CORRECT.darker(), 2, true),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            char correctLetter = (char) ('A' + correctIndex);
            feedbackLabel.setText("✗ Incorrect! Correct was " + correctLetter + ". Reset to Learning (Level 1)");
            feedbackLabel.setForeground(COLOR_INCORRECT);
        }

        // Update Stats Bar
        streakLabel.setText("Streak: " + currentStreak + " 🔥");
        int percent = totalAnswered > 0 ? (int) Math.round(((double) correctCount / totalAnswered) * 100) : 0;
        scoreLabel.setText(String.format("Score: %d / %d (%d%%)", correctCount, totalAnswered, percent));

        // Auto transition after brief delay (1200ms)
        transitionTimer = new Timer(1200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transitionTimer.stop();
                loadNextQuestion();
            }
        });
        transitionTimer.setRepeats(false);
        transitionTimer.start();
    }
}
