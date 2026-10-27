import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Business logic and data management layer for Flashcards.
 * Handles in-memory storage, CRUD operations, quiz option generation,
 * Spaced Repetition progression, and CSV file persistence.
 */
public class FlashcardManager {
    public static final String DEFAULT_FILE_NAME = "cards.csv";
    private final File storageFile;
    private final List<Flashcard> cards = new ArrayList<>();
    private final Random random = new Random();

    public FlashcardManager() {
        this(new File(DEFAULT_FILE_NAME));
    }

    public FlashcardManager(File storageFile) {
        this.storageFile = storageFile;
        loadFromFile();
    }

    /**
     * Inner class encapsulating a multiple-choice quiz question.
     */
    public static class QuizQuestion {
        private final Flashcard targetCard;
        private final List<Flashcard> options;
        private final int correctOptionIndex;

        public QuizQuestion(Flashcard targetCard, List<Flashcard> options, int correctOptionIndex) {
            this.targetCard = targetCard;
            this.options = options;
            this.correctOptionIndex = correctOptionIndex;
        }

        public Flashcard getTargetCard() {
            return targetCard;
        }

        public List<Flashcard> getOptions() {
            return Collections.unmodifiableList(options);
        }

        public int getCorrectOptionIndex() {
            return correctOptionIndex;
        }
    }

    /**
     * Returns an unmodifiable view of current flashcards.
     */
    public synchronized List<Flashcard> getCards() {
        return Collections.unmodifiableList(new ArrayList<>(cards));
    }

    /**
     * Retrieves a flashcard by ID.
     */
    public synchronized Flashcard getCardById(int id) {
        for (Flashcard card : cards) {
            if (card.getId() == id) {
                return card;
            }
        }
        return null;
    }

    /**
     * Generates a unique next ID for a new flashcard.
     */
    public synchronized int getNextId() {
        int maxId = 0;
        for (Flashcard card : cards) {
            if (card.getId() > maxId) {
                maxId = card.getId();
            }
        }
        return maxId + 1;
    }

    /**
     * Adds a new flashcard and persists changes to disk.
     */
    public synchronized void addCard(Flashcard card) {
        if (card == null) return;
        if (card.getId() <= 0) {
            card.setId(getNextId());
        }
        cards.add(card);
        saveToFile();
    }

    /**
     * Updates an existing flashcard and persists changes.
     */
    public synchronized boolean updateCard(Flashcard updatedCard) {
        if (updatedCard == null) return false;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId() == updatedCard.getId()) {
                cards.set(i, updatedCard);
                saveToFile();
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a flashcard by ID and persists changes.
     */
    public synchronized boolean deleteCard(int id) {
        boolean removed = cards.removeIf(card -> card.getId() == id);
        if (removed) {
            saveToFile();
        }
        return removed;
    }

    /**
     * Filters flashcards by query matching term, definition, or example.
     */
    public synchronized List<Flashcard> searchCards(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getCards();
        }
        String lowerQuery = query.trim().toLowerCase();
        List<Flashcard> results = new ArrayList<>();
        for (Flashcard card : cards) {
            if (card.getTerm().toLowerCase().contains(lowerQuery) ||
                card.getDefinition().toLowerCase().contains(lowerQuery) ||
                card.getExample().toLowerCase().contains(lowerQuery)) {
                results.add(card);
            }
        }
        return results;
    }

    /**
     * Generates a 4-option multiple-choice quiz question.
     * Returns null if there are fewer than 4 cards available.
     */
    public synchronized QuizQuestion generateQuizQuestion() {
        if (cards.size() < 4) {
            return null;
        }

        // Pick 1 target card at random
        int targetIdx = random.nextInt(cards.size());
        Flashcard targetCard = cards.get(targetIdx);

        // Pick 3 distinct distractor cards
        List<Flashcard> pool = new ArrayList<>(cards);
        pool.remove(targetIdx);
        Collections.shuffle(pool, random);

        List<Flashcard> options = new ArrayList<>();
        options.add(targetCard);
        for (int i = 0; i < 3; i++) {
            options.add(pool.get(i));
        }

        // Shuffle options and record correct index
        Collections.shuffle(options, random);
        int correctIndex = options.indexOf(targetCard);

        return new QuizQuestion(targetCard, options, correctIndex);
    }

    /**
     * Applies Simple Spaced Repetition logic:
     * - Incorrect answer resets level to 1 (Learning)
     * - Correct answer promotes level to next (up to 3 - Mastered)
     */
    public synchronized void recordReview(Flashcard card, boolean correct) {
        if (card == null) return;
        if (correct) {
            card.setLevel(Math.min(Flashcard.LEVEL_MASTERED, card.getLevel() + 1));
        } else {
            card.setLevel(Flashcard.LEVEL_LEARNING);
        }
        saveToFile();
    }

    /**
     * Loads cards from CSV. If file does not exist, populates 5 defaults and saves.
     */
    public synchronized void loadFromFile() {
        cards.clear();
        if (!storageFile.exists()) {
            populateDefaultCards();
            saveToFile();
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(storageFile), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // Skip header row if present
                    if (line.toLowerCase().startsWith("id,") || line.toLowerCase().startsWith("\"id\",")) {
                        continue;
                    }
                }
                Flashcard card = Flashcard.fromCsv(line);
                if (card != null) {
                    cards.add(card);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading flashcards from " + storageFile.getAbsolutePath() + ": " + e.getMessage());
        }

        // If file was empty, repopulate defaults
        if (cards.isEmpty()) {
            populateDefaultCards();
            saveToFile();
        }
    }

    /**
     * Saves all current cards to the storage CSV file.
     */
    public synchronized void saveToFile() {
        try {
            exportToCsv(storageFile);
        } catch (IOException e) {
            System.err.println("Error saving flashcards to " + storageFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    /**
     * Exports flashcards to a specified CSV file with UTF-8 encoding.
     */
    public synchronized void exportToCsv(File targetFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
            writer.println("id,term,definition,example,level");
            for (Flashcard card : cards) {
                writer.println(card.toCsvString());
            }
        }
    }

    /**
     * Populates 5 curated default English vocabulary items.
     */
    private void populateDefaultCards() {
        cards.clear();
        cards.add(new Flashcard(1, "ephemeral",
                "lasting for a very short time; transitory",
                "Fame in the fast-paced digital era is often ephemeral.",
                Flashcard.LEVEL_LEARNING));

        cards.add(new Flashcard(2, "serendipity",
                "the occurrence of events by chance in a happy or beneficial way",
                "Finding my favorite rare book at a small garage sale was pure serendipity.",
                Flashcard.LEVEL_LEARNING));

        cards.add(new Flashcard(3, "resilient",
                "able to withstand or recover quickly from difficult conditions",
                "The local economy proved remarkably resilient following the unexpected downturn.",
                Flashcard.LEVEL_REVIEWING));

        cards.add(new Flashcard(4, "eloquent",
                "fluent or persuasive in speaking or writing",
                "Her eloquent defense of human rights earned a standing ovation from the audience.",
                Flashcard.LEVEL_LEARNING));

        cards.add(new Flashcard(5, "pragmatic",
                "dealing with things sensibly and realistically based on practical considerations",
                "The engineering team took a pragmatic approach to meet the tight deadline.",
                Flashcard.LEVEL_MASTERED));
    }
}
