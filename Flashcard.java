import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing an English Vocabulary Flashcard.
 * Tracks term, definition, contextual example, and spaced repetition mastery level.
 */
public class Flashcard {
    private int id;
    private String term;
    private String definition;
    private String example;
    private int level; // 1 = Learning, 2 = Reviewing, 3 = Mastered

    public static final int LEVEL_LEARNING = 1;
    public static final int LEVEL_REVIEWING = 2;
    public static final int LEVEL_MASTERED = 3;

    public Flashcard(int id, String term, String definition, String example, int level) {
        this.id = id;
        this.term = term != null ? term.trim() : "";
        this.definition = definition != null ? definition.trim() : "";
        this.example = example != null ? example.trim() : "";
        this.level = sanitizeLevel(level);
    }

    public Flashcard(int id, String term, String definition, String example) {
        this(id, term, definition, example, LEVEL_LEARNING);
    }

    private static int sanitizeLevel(int lvl) {
        if (lvl < LEVEL_LEARNING) return LEVEL_LEARNING;
        if (lvl > LEVEL_MASTERED) return LEVEL_MASTERED;
        return lvl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term != null ? term.trim() : "";
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition != null ? definition.trim() : "";
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example != null ? example.trim() : "";
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = sanitizeLevel(level);
    }

    public String getLevelLabel() {
        switch (level) {
            case LEVEL_LEARNING:
                return "Learning";
            case LEVEL_REVIEWING:
                return "Reviewing";
            case LEVEL_MASTERED:
                return "Mastered";
            default:
                return "Learning";
        }
    }

    /**
     * Serializes this flashcard into a CSV line with proper escaping.
     */
    public String toCsvString() {
        return escapeCsv(String.valueOf(id)) + "," +
               escapeCsv(term) + "," +
               escapeCsv(definition) + "," +
               escapeCsv(example) + "," +
               escapeCsv(String.valueOf(level));
    }

    /**
     * Deserializes a CSV line into a Flashcard instance.
     * Returns null if the line is invalid or empty.
     */
    public static Flashcard fromCsv(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        List<String> tokens = parseCsvLine(line);
        if (tokens.size() < 4) {
            return null;
        }

        try {
            int id = Integer.parseInt(tokens.get(0).trim());
            String term = tokens.get(1);
            String definition = tokens.get(2);
            String example = tokens.get(3);
            int level = tokens.size() >= 5 ? Integer.parseInt(tokens.get(4).trim()) : LEVEL_LEARNING;

            return new Flashcard(id, term, definition, example, level);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Escapes a single CSV field according to RFC 4180 standards.
     */
    public static String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Parses a CSV line respecting quoted values and escaped quotes.
     */
    public static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    @Override
    public String toString() {
        return "Flashcard{" +
                "id=" + id +
                ", term='" + term + '\'' +
                ", definition='" + definition + '\'' +
                ", level=" + getLevelLabel() +
                '}';
    }
}
