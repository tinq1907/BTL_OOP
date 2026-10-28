import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Multiple-choice Quiz practice panel polished to a SaaS/Notion aesthetic.
 * Prompts the user with a prominent vocabulary term and 4 definition card buttons (A, B, C, D).
 * Each option button features a distinct circular pill indicator on the left.
 * Provides immediate feedback with clean color transitions and spaced repetition progression.
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
    private JLabel scoreBadge;
    private JLabel streakBadge;
    private JLabel promptLabel;
    private FeedbackBanner feedbackBanner;
    private QuizOptionButton[] optionButtons;
    private JPanel optionsPanel;
    private ModernQuizCard contentCard;
    private JPanel warningPanel;
    private Timer transitionTimer;
    private boolean isAnsweringAllowed = true;

    // SaaS Color Palette
    private static final Color COLOR_APP_BG = new Color(0xF8, 0xFA, 0xFC);       // #F8FAFC
    private static final Color COLOR_SURFACE = new Color(0xFF, 0xFF, 0xFF);      // #FFFFFF
    private static final Color COLOR_BORDER = new Color(0xE2, 0xE8, 0xF0);       // #E2E8F0
    private static final Color COLOR_BORDER_HOVER = new Color(0xCB, 0xD5, 0xE1); // #CBD5E1
    private static final Color COLOR_DIVIDER = new Color(0xF1, 0xF5, 0xF9);      // #F1F5F9

    // Feedback States
    public static final Color COLOR_FEEDBACK_CORRECT_BG = new Color(0xD1, 0xFA, 0xE5);     // #D1FAE5
    public static final Color COLOR_FEEDBACK_CORRECT_TEXT = new Color(0x06, 0x5F, 0x46);   // #065F46
    public static final Color COLOR_FEEDBACK_CORRECT_BORDER = new Color(0x10, 0xB9, 0x81); // #10B981

    public static final Color COLOR_FEEDBACK_INCORRECT_BG = new Color(0xFE, 0xE2, 0xE2);     // #FEE2E2
    public static final Color COLOR_FEEDBACK_INCORRECT_TEXT = new Color(0x99, 0x1B, 0x1B);   // #991B1B
    public static final Color COLOR_FEEDBACK_INCORRECT_BORDER = new Color(0xEF, 0x44, 0x44); // #EF4444

    // Text Colors
    private static final Color COLOR_TEXT_TITLE = new Color(0x0F, 0x17, 0x2A);   // #0F172A
    private static final Color COLOR_TEXT_BODY = new Color(0x33, 0x41, 0x55);    // #334155
    private static final Color COLOR_TEXT_MUTED = new Color(0x64, 0x74, 0x8B);   // #64748B

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_PROMPT = new Font("Segoe UI", Font.BOLD, 32); // 32pt Bold per requirement
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);

    public QuizPanel(FlashcardManager manager) {
        this.manager = manager;

        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(16, 24, 20, 24));
        setBackground(COLOR_APP_BG);

        initComponents();
        loadNextQuestion();
    }

    private void initComponents() {
        // --- Top Bar: Header and Stats ---
        JPanel topPanel = new JPanel(new BorderLayout(12, 10));
        topPanel.setOpaque(false);

        JPanel titleContainer = new JPanel();
        titleContainer.setLayout(new BoxLayout(titleContainer, BoxLayout.Y_AXIS));
        titleContainer.setOpaque(false);

        JLabel titleLabel = new JLabel("Multiple-Choice Vocabulary Quiz");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(COLOR_TEXT_TITLE);

        JLabel subtitleLabel = new JLabel("Test your mastery and advance words through spaced repetition");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);

        titleContainer.add(titleLabel);
        titleContainer.add(Box.createVerticalStrut(2));
        titleContainer.add(subtitleLabel);

        // Stats Badges (Streak & Score)
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statsPanel.setOpaque(false);

        streakBadge = createPillBadge("Streak: 0",
                new Color(0xFE, 0xF3, 0xC7), new Color(0xB4, 0x53, 0x09), new Color(0xFD, 0xE6, 0x8A));

        scoreBadge = createPillBadge("Score: 0 / 0 (0%)",
                new Color(0xEE, 0xF2, 0xFF), new Color(0x43, 0x38, 0xCA), new Color(0xC7, 0xD2, 0xFE));

        statsPanel.add(streakBadge);
        statsPanel.add(scoreBadge);

        topPanel.add(titleContainer, BorderLayout.WEST);
        topPanel.add(statsPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- Center: Quiz Card Container ---
        contentCard = new ModernQuizCard();
        contentCard.setLayout(new BorderLayout(0, 18));
        contentCard.setBorder(new EmptyBorder(24, 32, 22, 32));

        // Question Header Area
        JPanel promptContainer = new JPanel();
        promptContainer.setLayout(new BoxLayout(promptContainer, BoxLayout.Y_AXIS));
        promptContainer.setOpaque(false);

        JLabel questionEyebrow = new JLabel("WHAT IS THE CORRECT DEFINITION FOR:");
        questionEyebrow.setFont(FONT_SMALL_BOLD);
        questionEyebrow.setForeground(COLOR_TEXT_MUTED);
        questionEyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);

        promptLabel = new JLabel("Vocabulary Word", SwingConstants.CENTER);
        promptLabel.setFont(FONT_PROMPT); // 32pt bold
        promptLabel.setForeground(COLOR_TEXT_TITLE);
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtle divider
        JPanel divider = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(COLOR_DIVIDER);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        divider.setMaximumSize(new Dimension(500, 1));
        divider.setPreferredSize(new Dimension(500, 1));
        divider.setOpaque(false);
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);

        promptContainer.add(questionEyebrow);
        promptContainer.add(Box.createVerticalStrut(6));
        promptContainer.add(promptLabel);
        promptContainer.add(Box.createVerticalStrut(14));
        promptContainer.add(divider);

        contentCard.add(promptContainer, BorderLayout.NORTH);

        // 4 Options Grid (2x2 layout)
        optionsPanel = new JPanel(new GridLayout(2, 2, 16, 16));
        optionsPanel.setOpaque(false);
        optionButtons = new QuizOptionButton[4];

        char[] letters = {'A', 'B', 'C', 'D'};
        for (int i = 0; i < 4; i++) {
            final int optionIndex = i;
            QuizOptionButton btn = new QuizOptionButton(String.valueOf(letters[i]));
            btn.addActionListener(e -> {
                if (isAnsweringAllowed) {
                    handleOptionSelected(optionIndex);
                }
            });
            optionButtons[i] = btn;
            optionsPanel.add(btn);
        }
        contentCard.add(optionsPanel, BorderLayout.CENTER);

        // Feedback Banner
        feedbackBanner = new FeedbackBanner();
        contentCard.add(feedbackBanner, BorderLayout.SOUTH);

        // Warning Panel (when < 4 cards)
        warningPanel = createWarningPanel();

        add(contentCard, BorderLayout.CENTER);
    }

    private JPanel createWarningPanel() {
        JPanel wp = new JPanel(new GridBagLayout());
        wp.setOpaque(false);

        ModernQuizCard card = new ModernQuizCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        JLabel iconLabel = new JLabel("Vocabulary Quiz Mode", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        iconLabel.setForeground(COLOR_TEXT_TITLE);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msgLabel = new JLabel("<html><center style='width:420px; line-height:1.5; color:#64748B;'>"
                + "At least <b>4 vocabulary flashcards</b> are required to play Quiz Practice Mode.<br><br>"
                + "Please switch to the <b>Vocabulary List</b> tab and add more words to your collection.</center></html>", SwingConstants.CENTER);
        msgLabel.setFont(FONT_BODY);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(msgLabel);

        wp.add(card);
        return wp;
    }

    private JLabel createPillBadge(String text, Color bg, Color fg, Color border) {
        JLabel pill = new JLabel(text, SwingConstants.CENTER) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(FONT_SMALL_BOLD);
        pill.setForeground(fg);
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(5, 12, 5, 12));
        return pill;
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
        feedbackBanner.showDefault("Select the best definition from options A–D");

        List<Flashcard> options = currentQuestion.getOptions();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].resetDefault(options.get(i).getDefinition());
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

        // Visual coloring with modern SaaS state transitions
        if (isCorrect) {
            correctCount++;
            currentStreak++;
            optionButtons[selectedIndex].setFeedbackState(true);

            feedbackBanner.showCorrect("Correct! Promoted to "
                    + currentQuestion.getTargetCard().getLevelLabel()
                    + " (Level " + currentQuestion.getTargetCard().getLevel() + ")");
        } else {
            currentStreak = 0;
            optionButtons[selectedIndex].setFeedbackState(false);
            optionButtons[correctIndex].setFeedbackState(true);

            char correctLetter = (char) ('A' + correctIndex);
            feedbackBanner.showIncorrect("Incorrect! Correct definition was ("
                    + correctLetter + "). Reset to Learning (Level 1)");
        }

        // Update Stats Bar
        streakBadge.setText("Streak: " + currentStreak);
        int percent = totalAnswered > 0 ? (int) Math.round(((double) correctCount / totalAnswered) * 100) : 0;
        scoreBadge.setText(String.format("Score: %d / %d (%d%%)", correctCount, totalAnswered, percent));

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
     * Modern Quiz Card container with 20px rounded corners and soft drop shadow.
     */
    private static class ModernQuizCard extends JPanel {
        private static final long serialVersionUID = 1L;

        public ModernQuizCard() {
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
            int radius = 20;

            // Soft shadow
            g2.setColor(new Color(15, 23, 42, 6));
            g2.fillRoundRect(x, y + 4, w, h, radius, radius);
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(x, y + 2, w, h, radius, radius);

            // White surface
            g2.setColor(COLOR_SURFACE);
            g2.fillRoundRect(x, y, w, h, radius, radius);

            // Subtle border
            g2.setColor(COLOR_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Dedicated 2x2 option button featuring a distinct circle pill containing the
     * option letter on the left, followed by clean wrapped definition text.
     */
    private class QuizOptionButton extends JButton {
        private static final long serialVersionUID = 1L;

        private final String letter;
        private final LetterCircle letterCircle;
        private final JLabel defLabel;

        private Color currentBg = COLOR_SURFACE;
        private Color currentBorder = COLOR_BORDER;
        private boolean isHovered = false;
        private boolean isFeedbackActive = false;

        public QuizOptionButton(String letter) {
            this.letter = letter;
            setLayout(new BorderLayout(14, 0));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 16, 12, 16));

            letterCircle = new LetterCircle(letter);
            add(letterCircle, BorderLayout.WEST);

            defLabel = new JLabel();
            defLabel.setFont(FONT_BODY);
            defLabel.setForeground(COLOR_TEXT_BODY);
            add(defLabel, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isAnsweringAllowed && !isFeedbackActive) {
                        isHovered = true;
                        letterCircle.setHovered(true);
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (isAnsweringAllowed && !isFeedbackActive) {
                        isHovered = false;
                        letterCircle.setHovered(false);
                        repaint();
                    }
                }
            });
        }

        public void resetDefault(String definition) {
            this.isFeedbackActive = false;
            this.isHovered = false;
            this.currentBg = COLOR_SURFACE;
            this.currentBorder = COLOR_BORDER;

            letterCircle.reset();
            defLabel.setForeground(COLOR_TEXT_BODY);
            defLabel.setText("<html><body style='width: 330px; font-family: Segoe UI, sans-serif; font-size: 11pt; color: #334155;'>"
                    + definition + "</body></html>");
            repaint();
        }

        public void setFeedbackState(boolean isCorrect) {
            this.isFeedbackActive = true;
            this.isHovered = false;

            if (isCorrect) {
                this.currentBg = COLOR_FEEDBACK_CORRECT_BG;
                this.currentBorder = COLOR_FEEDBACK_CORRECT_BORDER;
                defLabel.setForeground(COLOR_FEEDBACK_CORRECT_TEXT);
                letterCircle.setFeedback(true);
            } else {
                this.currentBg = COLOR_FEEDBACK_INCORRECT_BG;
                this.currentBorder = COLOR_FEEDBACK_INCORRECT_BORDER;
                defLabel.setForeground(COLOR_FEEDBACK_INCORRECT_TEXT);
                letterCircle.setFeedback(false);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bgToPaint = currentBg;
            Color borderToPaint = currentBorder;

            if (isHovered && !isFeedbackActive) {
                bgToPaint = COLOR_DIVIDER; // #F1F5F9 hover
                borderToPaint = COLOR_BORDER_HOVER;
            }

            // Fill card body
            g2.setColor(bgToPaint);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            // Border
            g2.setColor(borderToPaint);
            g2.setStroke(new BasicStroke(isFeedbackActive ? 1.5f : 1.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Circular letter pill indicator displayed on the left of each option button.
     */
    private static class LetterCircle extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String letter;
        private Color circleBg = COLOR_DIVIDER;
        private Color circleText = new Color(0x47, 0x55, 0x69);
        private Color circleBorder = COLOR_BORDER;

        public LetterCircle(String letter) {
            this.letter = letter;
            setPreferredSize(new Dimension(34, 34));
            setMinimumSize(new Dimension(34, 34));
            setOpaque(false);
        }

        public void reset() {
            this.circleBg = COLOR_DIVIDER;
            this.circleText = new Color(0x47, 0x55, 0x69);
            this.circleBorder = COLOR_BORDER;
            repaint();
        }

        public void setHovered(boolean hovered) {
            if (hovered) {
                this.circleBg = new Color(0xE2, 0xE8, 0xF0);
            } else {
                this.circleBg = COLOR_DIVIDER;
            }
            repaint();
        }

        public void setFeedback(boolean isCorrect) {
            if (isCorrect) {
                this.circleBg = COLOR_FEEDBACK_CORRECT_BORDER; // Emerald
                this.circleText = Color.WHITE;
                this.circleBorder = COLOR_FEEDBACK_CORRECT_BORDER;
            } else {
                this.circleBg = COLOR_FEEDBACK_INCORRECT_BORDER; // Rose
                this.circleText = Color.WHITE;
                this.circleBorder = COLOR_FEEDBACK_INCORRECT_BORDER;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int d = 32;
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;

            g2.setColor(circleBg);
            g2.fillOval(x, y, d, d);

            g2.setColor(circleBorder);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawOval(x, y, d, d);

            g2.setColor(circleText);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (d - fm.stringWidth(letter)) / 2;
            int ty = y + (d - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(letter, tx, ty);

            g2.dispose();
        }
    }

    /**
     * Modern Status Banner at the bottom of the Quiz Card.
     */
    private static class FeedbackBanner extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel label;
        private Color currentBg = COLOR_DIVIDER;
        private Color currentBorder = COLOR_BORDER;

        public FeedbackBanner() {
            setLayout(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(300, 38));
            setBorder(new EmptyBorder(8, 16, 8, 16));

            label = new JLabel("Select the best definition from options A–D", SwingConstants.CENTER);
            label.setFont(FONT_BODY);
            label.setForeground(COLOR_TEXT_MUTED);
            add(label, BorderLayout.CENTER);
        }

        public void showDefault(String text) {
            currentBg = COLOR_DIVIDER;
            currentBorder = COLOR_BORDER;
            label.setText(text);
            label.setForeground(COLOR_TEXT_MUTED);
            repaint();
        }

        public void showCorrect(String text) {
            currentBg = COLOR_FEEDBACK_CORRECT_BG;
            currentBorder = new Color(0xA7, 0xF3, 0xD0);
            label.setText(text);
            label.setForeground(COLOR_FEEDBACK_CORRECT_TEXT);
            repaint();
        }

        public void showIncorrect(String text) {
            currentBg = COLOR_FEEDBACK_INCORRECT_BG;
            currentBorder = new Color(0xFE, 0xCA, 0xCA);
            label.setText(text);
            label.setForeground(COLOR_FEEDBACK_INCORRECT_TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(currentBg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2.setColor(currentBorder);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
