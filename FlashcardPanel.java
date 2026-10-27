import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Flashcard practice panel.
 * Provides a clean interactive card that flips between Term (front)
 * and Definition + Example (back). Supports assessment buttons for
 * spaced repetition level adjustments.
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
    private JButton prevButton;
    private JButton nextButton;
    private JButton flipButton;
    private JButton needReviewButton;
    private JButton masteredButton;

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
        progressLabel.setForeground(new Color(70, 80, 95));

        JLabel hintLabel = new JLabel("Tip: Click card or use 'Flip Card' to toggle front/back", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hintLabel.setForeground(new Color(130, 140, 155));

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

        prevButton = createStyledButton("◀ Previous", new Color(225, 230, 238), new Color(40, 50, 70));
        prevButton.addActionListener(e -> navigatePrevious());

        flipButton = createStyledButton("🔄 Flip Card", new Color(79, 70, 229), Color.WHITE);
        flipButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        flipButton.setPreferredSize(new Dimension(140, 40));
        flipButton.addActionListener(e -> flipCurrentCard());

        nextButton = createStyledButton("Next ▶", new Color(225, 230, 238), new Color(40, 50, 70));
        nextButton.addActionListener(e -> navigateNext());

        navRow.add(prevButton);
        navRow.add(flipButton);
        navRow.add(nextButton);

        // Assessment Row (Need Review vs Mastered)
        JPanel assessmentRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        assessmentRow.setOpaque(false);

        needReviewButton = createStyledButton("⚠ Need Review (Reset Level)", new Color(239, 68, 68), Color.WHITE);
        needReviewButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        needReviewButton.setPreferredSize(new Dimension(210, 42));
        needReviewButton.setToolTipText("Marks this card as needing review and resets its level to 1");
        needReviewButton.addActionListener(e -> recordAssessment(false));

        masteredButton = createStyledButton("✔ Mastered (Promote Level)", new Color(34, 197, 94), Color.WHITE);
        masteredButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        masteredButton.setPreferredSize(new Dimension(210, 42));
        masteredButton.setToolTipText("Advances this card to the next mastery level");
        masteredButton.addActionListener(e -> recordAssessment(true));

        assessmentRow.add(needReviewButton);
        assessmentRow.add(masteredButton);

        bottomPanel.add(navRow, BorderLayout.NORTH);
        bottomPanel.add(assessmentRow, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
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

        // Automatically flip back and advance to next card
        isFlipped = false;
        if (!cards.isEmpty()) {
            currentIndex = (currentIndex + 1) % cards.size();
        }
        loadCurrentCard();
    }

    /**
     * Custom panel that renders a modern physical-looking card
     * with rounded borders and responsive content wrapping.
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

            // Side Indicator (FRONT vs BACK)
            sideIndicator = new JLabel("FRONT", SwingConstants.CENTER);
            sideIndicator.setFont(new Font("Segoe UI", Font.BOLD, 12));
            sideIndicator.setForeground(new Color(120, 130, 145));

            // Primary display (Term on front, Definition on back)
            primaryLabel = new JLabel("", SwingConstants.CENTER);
            primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
            primaryLabel.setForeground(new Color(30, 41, 59));

            // Secondary display (Example on back, or subtitle)
            secondaryLabel = new JLabel("", SwingConstants.CENTER);
            secondaryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            secondaryLabel.setForeground(new Color(71, 85, 105));

            // Level Badge at bottom of card
            levelBadge = new JLabel("Level: Learning", SwingConstants.CENTER);
            levelBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
            levelBadge.setOpaque(true);
            levelBadge.setBorder(new EmptyBorder(4, 12, 4, 12));

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
            secondaryLabel.setText("<html><center>Add new vocabulary words in the 'Vocabulary List' tab.</center></html>");
            levelBadge.setVisible(false);
            repaint();
        }

        public void renderCard(Flashcard card, boolean flipped) {
            levelBadge.setVisible(true);

            // Badge color based on level
            Color badgeBg;
            Color badgeFg = Color.WHITE;
            switch (card.getLevel()) {
                case Flashcard.LEVEL_REVIEWING:
                    badgeBg = new Color(59, 130, 246); // Blue
                    break;
                case Flashcard.LEVEL_MASTERED:
                    badgeBg = new Color(16, 185, 129); // Green
                    break;
                case Flashcard.LEVEL_LEARNING:
                default:
                    badgeBg = new Color(245, 158, 11); // Amber
                    break;
            }
            levelBadge.setBackground(badgeBg);
            levelBadge.setForeground(badgeFg);
            levelBadge.setText(" Mastery Level: " + card.getLevelLabel() + " (L" + card.getLevel() + ") ");

            if (!flipped) {
                // FRONT OF CARD
                sideIndicator.setText("FRONT — TERM");
                primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
                primaryLabel.setText(card.getTerm());
                secondaryLabel.setText("<html><center style='color:#64748B;'>(Click to reveal definition & example)</center></html>");
            } else {
                // BACK OF CARD
                sideIndicator.setText("BACK — DEFINITION & EXAMPLE");
                primaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
                primaryLabel.setText("<html><center style='width: 450px; text-align: center; color:#1E293B;'>"
                        + card.getDefinition() + "</center></html>");

                String exText = card.getExample().isEmpty()
                        ? ""
                        : "Example: \"" + card.getExample() + "\"";
                secondaryLabel.setText("<html><center style='width: 450px; text-align: center; color:#0284C7;'>"
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

            // Draw card shadow
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillRoundRect(8, 8, width - 16, height - 16, 24, 24);

            // Draw card white body
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(4, 4, width - 16, height - 16, 24, 24);

            // Draw card border
            g2.setColor(new Color(226, 232, 240));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(4, 4, width - 16, height - 16, 24, 24);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
