import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Flashcard practice panel.
 * Displays interactive cards that flip between Term (front) and Definition + Example (back).
 * Provides distinct, accessible assessment buttons for spaced repetition.
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
    private StyledButton prevButton;
    private StyledButton nextButton;
    private StyledButton flipButton;
    private StyledButton needReviewButton;
    private StyledButton masteredButton;

    // Standard Palette
    private static final Color COLOR_TEXT_DARK = new Color(33, 37, 41);   // #212529
    private static final Color COLOR_GRAY_BG = new Color(240, 240, 240);   // #F0F0F0
    private static final Color COLOR_FLIP_BG = new Color(73, 80, 87);     // #495057
    private static final Color COLOR_GREEN = new Color(25, 135, 84);      // #198754
    private static final Color COLOR_RED = new Color(220, 53, 69);        // #DC3545

    private static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    public FlashcardPanel(FlashcardManager manager) {
        this.manager = manager;
        this.cards = manager.getCards();

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 30, 25, 30));
        setBackground(new Color(245, 247, 250));

        initComponents();
        loadCurrentCard();
    }

    private void initComponents() {
        // --- Top Bar: Progress and Hints ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        progressLabel = new JLabel("Card 0 of 0", SwingConstants.CENTER);
        progressLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        progressLabel.setForeground(COLOR_TEXT_DARK);

        JLabel hintLabel = new JLabel("Tip: Click card or use 'Flip Card' to toggle front/back", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        hintLabel.setForeground(new Color(108, 117, 125));

        topBar.add(progressLabel, BorderLayout.NORTH);
        topBar.add(hintLabel, BorderLayout.SOUTH);
        add(topBar, BorderLayout.NORTH);

        // --- Center: Interactive Flashcard Display ---
        cardViewPanel = new CardViewPanel();
        cardViewPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cardViewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                flipCurrentCard();
            }
        });
        add(cardViewPanel, BorderLayout.CENTER);

        // --- Bottom Controls Panel ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 15));
        bottomPanel.setOpaque(false);

        // Navigation Row (Previous, Flip, Next)
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        navRow.setOpaque(false);

        prevButton = new StyledButton("Previous", COLOR_GRAY_BG, COLOR_TEXT_DARK, FONT_BODY_BOLD);
        prevButton.addActionListener(e -> navigatePrevious());

        flipButton = new StyledButton("Flip Card", COLOR_FLIP_BG, Color.WHITE, new Font("Segoe UI", Font.BOLD, 15));
        flipButton.setPreferredSize(new Dimension(140, 42));
        flipButton.addActionListener(e -> flipCurrentCard());

        nextButton = new StyledButton("Next", COLOR_GRAY_BG, COLOR_TEXT_DARK, FONT_BODY_BOLD);
        nextButton.addActionListener(e -> navigateNext());

        navRow.add(prevButton);
        navRow.add(flipButton);
        navRow.add(nextButton);

        // Assessment Row (Need Review vs Mastered)
        JPanel assessmentRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        assessmentRow.setOpaque(false);

        needReviewButton = new StyledButton("Need Review", COLOR_RED, Color.WHITE, FONT_BODY_BOLD);
        needReviewButton.setPreferredSize(new Dimension(190, 42));
        needReviewButton.setToolTipText("Marks this card as needing review and resets its level to 1");
        needReviewButton.addActionListener(e -> recordAssessment(false));

        masteredButton = new StyledButton("Mastered", COLOR_GREEN, Color.WHITE, FONT_BODY_BOLD);
        masteredButton.setPreferredSize(new Dimension(190, 42));
        masteredButton.setToolTipText("Advances this card to the next mastery level");
        masteredButton.addActionListener(e -> recordAssessment(true));

        assessmentRow.add(needReviewButton);
        assessmentRow.add(masteredButton);

        bottomPanel.add(navRow, BorderLayout.NORTH);
        bottomPanel.add(assessmentRow, BorderLayout.SOUTH);
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
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            flipButton.setEnabled(false);
            needReviewButton.setEnabled(false);
            masteredButton.setEnabled(false);
            cardViewPanel.showEmptyState();
            return;
        }

        prevButton.setEnabled(true);
        nextButton.setEnabled(true);
        flipButton.setEnabled(true);
        needReviewButton.setEnabled(true);
        masteredButton.setEnabled(true);

        progressLabel.setText(String.format("Card %d of %d", currentIndex + 1, cards.size()));
        Flashcard currentCard = cards.get(currentIndex);
        cardViewPanel.renderCard(currentCard, isFlipped);
    }

    private void flipCurrentCard() {
        if (cards == null || cards.isEmpty()) return;
        isFlipped = !isFlipped;
        loadCurrentCard();
    }

    private void navigatePrevious() {
        if (cards == null || cards.isEmpty()) return;
        isFlipped = false;
        currentIndex = (currentIndex - 1 + cards.size()) % cards.size();
        loadCurrentCard();
    }

    private void navigateNext() {
        if (cards == null || cards.isEmpty()) return;
        isFlipped = false;
        currentIndex = (currentIndex + 1) % cards.size();
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
     * Custom panel rendering a clean, physical card aesthetic.
     */
    private static class CardViewPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JLabel sideIndicator;
        private final JLabel primaryLabel;
        private final JLabel secondaryLabel;
        private final JLabel levelBadge;

        public CardViewPanel() {
            setLayout(new BorderLayout(15, 15));
            setBorder(new EmptyBorder(30, 40, 30, 40));
            setOpaque(false);

            sideIndicator = new JLabel("FRONT - TERM", SwingConstants.CENTER);
            sideIndicator.setFont(new Font("Segoe UI", Font.BOLD, 13));
            sideIndicator.setForeground(new Color(108, 117, 125));

            primaryLabel = new JLabel("", SwingConstants.CENTER);
            primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
            primaryLabel.setForeground(COLOR_TEXT_DARK);

            secondaryLabel = new JLabel("", SwingConstants.CENTER);
            secondaryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            secondaryLabel.setForeground(new Color(73, 80, 87));

            levelBadge = new JLabel("Level: Learning", SwingConstants.CENTER);
            levelBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
            levelBadge.setOpaque(true);
            levelBadge.setBorder(new EmptyBorder(5, 14, 5, 14));

            JPanel centerHolder = new JPanel();
            centerHolder.setLayout(new BoxLayout(centerHolder, BoxLayout.Y_AXIS));
            centerHolder.setOpaque(false);

            primaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            secondaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            levelBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

            centerHolder.add(Box.createVerticalGlue());
            centerHolder.add(primaryLabel);
            centerHolder.add(Box.createVerticalStrut(15));
            centerHolder.add(secondaryLabel);
            centerHolder.add(Box.createVerticalStrut(20));
            centerHolder.add(levelBadge);
            centerHolder.add(Box.createVerticalGlue());

            add(sideIndicator, BorderLayout.NORTH);
            add(centerHolder, BorderLayout.CENTER);
        }

        public void showEmptyState() {
            sideIndicator.setText("");
            primaryLabel.setText("No Flashcards");
            secondaryLabel.setText("<html><center>Add new vocabulary words in the Vocabulary List tab.</center></html>");
            levelBadge.setVisible(false);
            repaint();
        }

        public void renderCard(Flashcard card, boolean flipped) {
            levelBadge.setVisible(true);

            Color badgeBg;
            switch (card.getLevel()) {
                case Flashcard.LEVEL_REVIEWING:
                    badgeBg = new Color(13, 110, 253); // Blue
                    break;
                case Flashcard.LEVEL_MASTERED:
                    badgeBg = new Color(25, 135, 84);  // Green
                    break;
                case Flashcard.LEVEL_LEARNING:
                default:
                    badgeBg = new Color(217, 119, 6);  // Amber
                    break;
            }
            levelBadge.setBackground(badgeBg);
            levelBadge.setForeground(Color.WHITE);
            levelBadge.setText(" Mastery Level: " + card.getLevelLabel() + " (Level " + card.getLevel() + ") ");

            if (!flipped) {
                sideIndicator.setText("FRONT - TERM");
                primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
                primaryLabel.setForeground(COLOR_TEXT_DARK);
                primaryLabel.setText(card.getTerm());
                secondaryLabel.setText("<html><center style='color:#6C757D;'>(Click to reveal definition & example)</center></html>");
            } else {
                sideIndicator.setText("BACK - DEFINITION & EXAMPLE");
                primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
                primaryLabel.setForeground(COLOR_TEXT_DARK);
                primaryLabel.setText("<html><center style='width: 480px; text-align: center; color:#212529;'>"
                        + card.getDefinition() + "</center></html>");

                String exText = card.getExample().isEmpty()
                        ? ""
                        : "Example: \"" + card.getExample() + "\"";
                secondaryLabel.setText("<html><center style='width: 480px; text-align: center; color:#0D6EFD; font-size:14px;'>"
                        + exText + "</center></html>");
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Card subtle shadow
            g2.setColor(new Color(0, 0, 0, 20));
            g2.fillRoundRect(6, 6, width - 12, height - 12, 20, 20);

            // Card solid body
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(3, 3, width - 12, height - 12, 20, 20);

            // Card border
            g2.setColor(new Color(222, 226, 230));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(3, 3, width - 12, height - 12, 20, 20);

            g2.dispose();
            super.paintComponent(g);
        }
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
