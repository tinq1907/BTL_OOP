import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Flashcard practice panel polished to a SaaS/Notion aesthetic.
 * Displays interactive cards that flip between Term (front) and Definition + Example (back)
 * via direct clicks on the card.
 * Streamlined bottom action controls with 2 prominent assessment buttons:
 * "Need Review" and "Mastered" for focused spaced repetition practice.
 */
public class FlashcardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final transient FlashcardManager manager;
    private transient List<Flashcard> cards;
    private int currentIndex = 0;
    private boolean isFlipped = false;

    // UI Components
    private CardViewPanel cardViewPanel;
    private JLabel progressLabel;
    private JLabel subtitleLabel;
    private StyledButton needReviewButton;
    private StyledButton masteredButton;

    // SaaS Color Palette
    private static final Color COLOR_APP_BG = new Color(0xF8, 0xFA, 0xFC);       // #F8FAFC
    private static final Color COLOR_SURFACE = new Color(0xFF, 0xFF, 0xFF);      // #FFFFFF
    private static final Color COLOR_BORDER = new Color(0xE2, 0xE8, 0xF0);       // #E2E8F0
    private static final Color COLOR_DIVIDER = new Color(0xF1, 0xF5, 0xF9);      // #F1F5F9

    private static final Color COLOR_PRIMARY = new Color(0x4F, 0x46, 0xE5);      // #4F46E5 Indigo
    private static final Color COLOR_PRIMARY_TINT = new Color(0xEE, 0xF2, 0xFF); // #EEF2FF
    private static final Color COLOR_PRIMARY_BORDER = new Color(0xC7, 0xD2, 0xFE);// #C7D2FE

    private static final Color COLOR_SUCCESS = new Color(0x10, 0xB9, 0x81);      // #10B981 Emerald
    private static final Color COLOR_SUCCESS_HOVER = new Color(0x05, 0x96, 0x69);// #059669

    private static final Color COLOR_DANGER = new Color(0xEF, 0x44, 0x44);       // #EF4444 Rose
    private static final Color COLOR_DANGER_HOVER = new Color(0xDC, 0x26, 0x26); // #DC2626

    private static final Color COLOR_TEXT_TITLE = new Color(0x0F, 0x17, 0x2A);   // #0F172A
    private static final Color COLOR_TEXT_BODY = new Color(0x33, 0x41, 0x55);    // #334155
    private static final Color COLOR_TEXT_MUTED = new Color(0x64, 0x74, 0x8B);   // #64748B

    private static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);

    public FlashcardPanel(FlashcardManager manager) {
        this.manager = manager;
        this.cards = manager.getCards();

        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(16, 24, 20, 24));
        setBackground(COLOR_APP_BG);

        initComponents();
        loadCurrentCard();
    }

    private void initComponents() {
        // --- Top Bar: Progress and Guidance ---
        JPanel topBar = new JPanel(new BorderLayout(0, 4));
        topBar.setOpaque(false);

        progressLabel = new JLabel("Card 0 of 0", SwingConstants.CENTER);
        progressLabel.setFont(FONT_HEADING);
        progressLabel.setForeground(COLOR_TEXT_TITLE);

        subtitleLabel = new JLabel("Click the card to flip between term & definition. Choose an assessment below to continue.", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);

        topBar.add(progressLabel, BorderLayout.NORTH);
        topBar.add(subtitleLabel, BorderLayout.SOUTH);
        add(topBar, BorderLayout.NORTH);

        // --- Center: Hero Flashcard Display (Centered Container) ---
        JPanel cardHolder = new JPanel(new GridBagLayout());
        cardHolder.setOpaque(false);

        cardViewPanel = new CardViewPanel();
        cardViewPanel.setPreferredSize(new Dimension(660, 360));
        cardViewPanel.setMinimumSize(new Dimension(540, 320));
        cardViewPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cardViewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                flipCurrentCard();
            }
        });

        cardHolder.add(cardViewPanel);
        add(cardHolder, BorderLayout.CENTER);

        // --- Bottom Controls Panel: ONLY 2 Buttons ("Need Review" & "Mastered") ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        bottomPanel.setOpaque(false);

        needReviewButton = new StyledButton("Need Review", COLOR_DANGER, Color.WHITE, COLOR_DANGER, FONT_BODY_BOLD);
        needReviewButton.setCornerRadius(10);
        needReviewButton.setPreferredSize(new Dimension(200, 48));
        needReviewButton.setToolTipText("Marks this card as needing review and resets its level to Learning (Level 1)");
        needReviewButton.addActionListener(e -> recordAssessment(false));

        masteredButton = new StyledButton("Mastered", COLOR_SUCCESS, Color.WHITE, COLOR_SUCCESS, FONT_BODY_BOLD);
        masteredButton.setCornerRadius(10);
        masteredButton.setPreferredSize(new Dimension(200, 48));
        masteredButton.setToolTipText("Advances this card to the next mastery level");
        masteredButton.addActionListener(e -> recordAssessment(true));

        bottomPanel.add(needReviewButton);
        bottomPanel.add(masteredButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void reloadCards() {
        this.cards = manager.getCards();
        if (currentIndex >= cards.size()) {
            currentIndex = Math.max(0, cards.size() - 1);
        }
        isFlipped = false;
        loadCurrentCard();
    }

    private void loadCurrentCard() {
        if (cards == null || cards.isEmpty()) {
            progressLabel.setText("No Flashcards Available");
            subtitleLabel.setText("Add vocabulary words in the Vocabulary List tab to start practicing.");
            needReviewButton.setEnabled(false);
            masteredButton.setEnabled(false);
            cardViewPanel.showEmptyState();
            return;
        }

        needReviewButton.setEnabled(true);
        masteredButton.setEnabled(true);

        progressLabel.setText(String.format("Card %d of %d", currentIndex + 1, cards.size()));
        subtitleLabel.setText("Click the card to flip between term & definition. Choose an assessment below to continue.");

        Flashcard currentCard = cards.get(currentIndex);
        cardViewPanel.renderCard(currentCard, isFlipped);
    }

    private void flipCurrentCard() {
        if (cards == null || cards.isEmpty()) return;
        isFlipped = !isFlipped;
        loadCurrentCard();
    }

    private void recordAssessment(boolean correct) {
        if (cards == null || cards.isEmpty()) return;
        Flashcard current = cards.get(currentIndex);
        manager.recordReview(current, correct);

        isFlipped = false;
        if (!cards.isEmpty()) {
            currentIndex = (currentIndex + 1) % cards.size();
        }
        loadCurrentCard();
    }

    /**
     * Hero Flashcard Panel rendering a physical, elevated card aesthetic
     * with 20px rounded corners and dual soft drop shadows.
     */
    private static class CardViewPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JLabel sideIndicator;
        private final JLabel termHeaderLabel;
        private final JLabel primaryLabel;
        private final QuoteBoxPanel exampleBox;
        private final JLabel hintLabel;
        private final LevelBadgePanel levelBadge;

        public CardViewPanel() {
            setLayout(new BorderLayout(12, 10));
            setBorder(new EmptyBorder(22, 32, 22, 32));
            setOpaque(false);

            // Top Pill Side Indicator
            sideIndicator = new JLabel("FRONT  •  TERM", SwingConstants.CENTER) {
                private static final long serialVersionUID = 1L;
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(COLOR_PRIMARY_TINT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(COLOR_PRIMARY_BORDER);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            sideIndicator.setFont(FONT_SMALL_BOLD);
            sideIndicator.setForeground(COLOR_PRIMARY);
            sideIndicator.setOpaque(false);
            sideIndicator.setBorder(new EmptyBorder(4, 14, 4, 14));

            JPanel topPillHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            topPillHolder.setOpaque(false);
            topPillHolder.add(sideIndicator);
            add(topPillHolder, BorderLayout.NORTH);

            // Center Content Holder
            JPanel centerHolder = new JPanel();
            centerHolder.setLayout(new BoxLayout(centerHolder, BoxLayout.Y_AXIS));
            centerHolder.setOpaque(false);

            termHeaderLabel = new JLabel("", SwingConstants.CENTER);
            termHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            termHeaderLabel.setForeground(COLOR_PRIMARY);
            termHeaderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            termHeaderLabel.setVisible(false);

            primaryLabel = new JLabel("", SwingConstants.CENTER);
            primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 34)); // 34pt Bold per requirement
            primaryLabel.setForeground(COLOR_TEXT_TITLE);
            primaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            hintLabel = new JLabel("Click anywhere to flip", SwingConstants.CENTER);
            hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            hintLabel.setForeground(new Color(0x94, 0xA3, 0xB8));
            hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            exampleBox = new QuoteBoxPanel();
            exampleBox.setAlignmentX(Component.CENTER_ALIGNMENT);
            exampleBox.setVisible(false);

            centerHolder.add(Box.createVerticalGlue());
            centerHolder.add(termHeaderLabel);
            centerHolder.add(primaryLabel);
            centerHolder.add(Box.createVerticalStrut(10));
            centerHolder.add(hintLabel);
            centerHolder.add(exampleBox);
            centerHolder.add(Box.createVerticalGlue());

            add(centerHolder, BorderLayout.CENTER);

            // Bottom Level Badge Holder
            levelBadge = new LevelBadgePanel();
            JPanel bottomHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            bottomHolder.setOpaque(false);
            bottomHolder.add(levelBadge);
            add(bottomHolder, BorderLayout.SOUTH);
        }

        public void showEmptyState() {
            sideIndicator.setText("NO CARDS");
            termHeaderLabel.setVisible(false);
            primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
            primaryLabel.setForeground(COLOR_TEXT_TITLE);
            primaryLabel.setText("No Flashcards Available");
            hintLabel.setText("Please switch to Vocabulary List tab to add cards.");
            hintLabel.setVisible(true);
            exampleBox.setVisible(false);
            levelBadge.setVisible(false);
            repaint();
        }

        public void renderCard(Flashcard card, boolean flipped) {
            levelBadge.setVisible(true);
            levelBadge.setLevel(card.getLevel(), card.getLevelLabel());

            if (!flipped) {
                // FRONT VIEW
                sideIndicator.setText("FRONT  •  TERM");
                termHeaderLabel.setVisible(false);
                exampleBox.setVisible(false);
                hintLabel.setVisible(true);
                hintLabel.setText("Click anywhere to flip");

                primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 34)); // 34pt Bold
                primaryLabel.setForeground(COLOR_TEXT_TITLE);
                primaryLabel.setText(card.getTerm());
            } else {
                // BACK VIEW
                sideIndicator.setText("BACK  •  DEFINITION & EXAMPLE");
                termHeaderLabel.setVisible(true);
                termHeaderLabel.setText("Term: " + card.getTerm());
                hintLabel.setVisible(false);

                // Definition in clean 18pt regular text
                primaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                primaryLabel.setForeground(COLOR_TEXT_BODY);
                primaryLabel.setText("<html><center style='width: 480px; text-align: center; color:#1E293B;'>"
                        + card.getDefinition() + "</center></html>");

                // Example in accent-tinted quote box (#EFF6FF, 4px solid #3B82F6 border)
                if (card.getExample() != null && !card.getExample().trim().isEmpty()) {
                    exampleBox.setText("“" + card.getExample() + "”");
                    exampleBox.setVisible(true);
                } else {
                    exampleBox.setVisible(false);
                }
            }

            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 4;
            int y = 4;
            int w = getWidth() - 8;
            int h = getHeight() - 10;
            int radius = 20; // 20px rounded corners

            // Dual soft drop shadow for physical presence
            g2.setColor(new Color(15, 23, 42, 6));
            g2.fillRoundRect(x, y + 8, w, h, radius, radius);
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(x, y + 4, w, h, radius, radius);
            g2.setColor(new Color(15, 23, 42, 14));
            g2.fillRoundRect(x, y + 2, w, h, radius, radius);

            // Card solid pure white body
            g2.setColor(COLOR_SURFACE);
            g2.fillRoundRect(x, y, w, h, radius, radius);

            // Card subtle border
            g2.setColor(COLOR_BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Accent-tinted quote box for contextual example sentences.
     * Features #EFF6FF background and a solid 4px #3B82F6 left border.
     */
    private static class QuoteBoxPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel quoteLabel;
        private static final Color COLOR_QUOTE_BG = new Color(0xEF, 0xF6, 0xFF);     // #EFF6FF
        private static final Color COLOR_QUOTE_BORDER = new Color(0x3B, 0x82, 0xF6); // #3B82F6
        private static final Color COLOR_QUOTE_TEXT = new Color(0x1E, 0x40, 0xAF);   // #1E40AF

        public QuoteBoxPanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setMaximumSize(new Dimension(520, 70));

            quoteLabel = new JLabel("", SwingConstants.CENTER);
            quoteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            quoteLabel.setForeground(COLOR_QUOTE_TEXT);
            add(quoteLabel, BorderLayout.CENTER);
        }

        public void setText(String text) {
            quoteLabel.setText("<html><center style='width: 460px; text-align: center;'>" + text + "</center></html>");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background fill
            g2.setColor(COLOR_QUOTE_BG);
            g2.fillRoundRect(0, 0, w, h, 8, 8);

            // 4px left accent border
            g2.setColor(COLOR_QUOTE_BORDER);
            g2.fillRect(0, 0, 4, h);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Modern Pill Badge rendering for mastery levels in Flashcard mode.
     */
    private static class LevelBadgePanel extends JLabel {
        private static final long serialVersionUID = 1L;
        private Color currentBg = Flashcard.COLOR_LEARNING_BG;
        private Color currentBorder = Flashcard.COLOR_LEARNING_BORDER;

        public LevelBadgePanel() {
            setFont(FONT_SMALL_BOLD);
            setOpaque(false);
            setBorder(new EmptyBorder(4, 14, 4, 14));
        }

        public void setLevel(int level, String label) {
            switch (level) {
                case Flashcard.LEVEL_REVIEWING:
                    currentBg = Flashcard.COLOR_REVIEWING_BG;
                    currentBorder = Flashcard.COLOR_REVIEWING_BORDER;
                    setForeground(Flashcard.COLOR_REVIEWING_TEXT);
                    setText("● Mastery: Reviewing (Level 2)");
                    break;
                case Flashcard.LEVEL_MASTERED:
                    currentBg = Flashcard.COLOR_MASTERED_BG;
                    currentBorder = Flashcard.COLOR_MASTERED_BORDER;
                    setForeground(Flashcard.COLOR_MASTERED_TEXT);
                    setText("● Mastery: Mastered (Level 3)");
                    break;
                case Flashcard.LEVEL_LEARNING:
                default:
                    currentBg = Flashcard.COLOR_LEARNING_BG;
                    currentBorder = Flashcard.COLOR_LEARNING_BORDER;
                    setForeground(Flashcard.COLOR_LEARNING_TEXT);
                    setText("● Mastery: Learning (Level 1)");
                    break;
            }
            repaint();
        }

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
    }

    /**
     * Reusable custom button that renders anti-aliased rounded corners,
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
            if (c.equals(COLOR_PRIMARY)) return new Color(0x43, 0x38, 0xCA);
            if (c.equals(COLOR_SUCCESS)) return COLOR_SUCCESS_HOVER;
            if (c.equals(COLOR_DANGER)) return COLOR_DANGER_HOVER;
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
}
