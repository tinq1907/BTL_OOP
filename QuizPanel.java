import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Multiple-choice Quiz practice panel.
 * Prompts the user with a vocabulary term and 4 definition options (A, B, C, D).
 * Validates answers immediately, applies high-contrast visual feedback,
 * updates Spaced Repetition, and transitions smoothly to the next question.
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
    private QuizOptionButton[] optionButtons;
    private JPanel optionsPanel;
    private JPanel contentCard;
    private JPanel warningPanel;
    private Timer transitionTimer;
    private boolean isAnsweringAllowed = true;

    // Colors
    private static final Color COLOR_TEXT_DARK = new Color(33, 37, 41);   // #212529
    private static final Color COLOR_DEFAULT_BTN = new Color(248, 249, 250); // #F8F9FA
    private static final Color COLOR_CORRECT = new Color(25, 135, 84);    // #198754 Solid Green
    private static final Color COLOR_INCORRECT = new Color(220, 53, 69);  // #DC3545 Solid Red

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

        streakLabel = new JLabel("Streak: 0");
        streakLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        streakLabel.setForeground(new Color(217, 119, 6));

        scoreLabel = new JLabel("Score: 0 / 0 (0%)");
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        scoreLabel.setForeground(new Color(13, 110, 253));

        statsPanel.add(streakLabel);
        statsPanel.add(scoreLabel);

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(statsPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- Center: Quiz Card Container ---
        contentCard = new JPanel(new BorderLayout(20, 20)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(6, 6, getWidth() - 12, getHeight() - 12, 20, 20);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(3, 3, getWidth() - 12, getHeight() - 12, 20, 20);
                g2.setColor(new Color(222, 226, 230));
                g2.drawRoundRect(3, 3, getWidth() - 12, getHeight() - 12, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentCard.setOpaque(false);
        contentCard.setBorder(new EmptyBorder(30, 40, 25, 40));

        // Prompt Word
        JPanel promptContainer = new JPanel();
        promptContainer.setLayout(new BoxLayout(promptContainer, BoxLayout.Y_AXIS));
        promptContainer.setOpaque(false);

        JLabel questionHeader = new JLabel("What is the correct definition for:");
        questionHeader.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        questionHeader.setForeground(new Color(108, 117, 125));
        questionHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        promptLabel = new JLabel("Vocabulary Word", SwingConstants.CENTER);
        promptLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        promptLabel.setForeground(COLOR_TEXT_DARK);
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        promptContainer.add(questionHeader);
        promptContainer.add(Box.createVerticalStrut(8));
        promptContainer.add(promptLabel);
        contentCard.add(promptContainer, BorderLayout.NORTH);

        // 4 Options Grid (2x2 layout)
        optionsPanel = new JPanel(new GridLayout(2, 2, 16, 16));
        optionsPanel.setOpaque(false);
        optionButtons = new QuizOptionButton[4];

        for (int i = 0; i < 4; i++) {
            final int optionIndex = i;
            QuizOptionButton btn = new QuizOptionButton();
            btn.addActionListener(e -> {
                if (isAnsweringAllowed) {
                    handleOptionSelected(optionIndex);
                }
            });
            optionButtons[i] = btn;
            optionsPanel.add(btn);
        }
        contentCard.add(optionsPanel, BorderLayout.CENTER);

        // Feedback Label
        feedbackLabel = new JLabel("Choose an option to answer", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        feedbackLabel.setForeground(new Color(108, 117, 125));
        feedbackLabel.setPreferredSize(new Dimension(300, 30));
        contentCard.add(feedbackLabel, BorderLayout.SOUTH);

        // Warning Panel
        warningPanel = new JPanel(new GridBagLayout());
        warningPanel.setOpaque(false);
        JLabel warnMsg = new JLabel("<html><center><h3>At least 4 vocabulary cards are required to play Quiz Mode.</h3><p>Please switch to the Vocabulary List tab and add more words.</p></center></html>", SwingConstants.CENTER);
        warnMsg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        warnMsg.setForeground(new Color(108, 117, 125));
        warningPanel.add(warnMsg);

        add(contentCard, BorderLayout.CENTER);
    }

    public void reloadQuiz() {
        if (transitionTimer != null && transitionTimer.isRunning()) {
            transitionTimer.stop();
        }
        loadNextQuestion();
    }

    private void loadNextQuestion() {
        isAnsweringAllowed = true;

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
        feedbackLabel.setText("Select the best definition from options A-D");
        feedbackLabel.setForeground(new Color(108, 117, 125));

        char[] letters = {'A', 'B', 'C', 'D'};
        List<Flashcard> options = currentQuestion.getOptions();

        for (int i = 0; i < 4; i++) {
            QuizOptionButton btn = optionButtons[i];
            btn.resetDefault();
            String def = options.get(i).getDefinition();
            btn.setText("<html><b style='font-size:14px;'>" + letters[i] + ".</b> " + def + "</html>");
        }
    }

    private void handleOptionSelected(int selectedIndex) {
        if (currentQuestion == null || !isAnsweringAllowed) return;

        isAnsweringAllowed = false; // Lock answers during feedback delay
        int correctIndex = currentQuestion.getCorrectOptionIndex();
        boolean isCorrect = (selectedIndex == correctIndex);
        totalAnswered++;

        // Update Spaced Repetition logic
        manager.recordReview(currentQuestion.getTargetCard(), isCorrect);

        // Visual coloring with high-contrast feedback
        if (isCorrect) {
            correctCount++;
            currentStreak++;
            optionButtons[selectedIndex].setFeedbackColor(COLOR_CORRECT, Color.WHITE);

            feedbackLabel.setText("Correct! Promoted to " + currentQuestion.getTargetCard().getLevelLabel() + " (Level " + currentQuestion.getTargetCard().getLevel() + ")");
            feedbackLabel.setForeground(COLOR_CORRECT);
        } else {
            currentStreak = 0;
            optionButtons[selectedIndex].setFeedbackColor(COLOR_INCORRECT, Color.WHITE);
            optionButtons[correctIndex].setFeedbackColor(COLOR_CORRECT, Color.WHITE);

            char correctLetter = (char) ('A' + correctIndex);
            feedbackLabel.setText("Incorrect! Correct was: " + correctLetter + ". Reset to Learning (Level 1)");
            feedbackLabel.setForeground(COLOR_INCORRECT);
        }

        // Update Stats Bar
        streakLabel.setText("Streak: " + currentStreak);
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

    /**
     * Dedicated option button guaranteeing crisp background rendering
     * and high contrast across all states.
     */
    private static class QuizOptionButton extends JButton {
        private static final long serialVersionUID = 1L;

        private Color currentBg;
        private Color currentBorder;

        public QuizOptionButton() {
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 16, 12, 16));
            resetDefault();
        }

        public void resetDefault() {
            this.currentBg = COLOR_DEFAULT_BTN;
            this.currentBorder = new Color(206, 212, 218);
            setForeground(COLOR_TEXT_DARK);
            repaint();
        }

        public void setFeedbackColor(Color bg, Color fg) {
            this.currentBg = bg;
            this.currentBorder = bg.darker();
            setForeground(fg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Hover state only when in default unselected mode
            if (currentBg.equals(COLOR_DEFAULT_BTN) && getModel().isRollover()) {
                g2.setColor(new Color(233, 236, 239));
            } else {
                g2.setColor(currentBg);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            // Border
            g2.setColor(currentBorder);
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
