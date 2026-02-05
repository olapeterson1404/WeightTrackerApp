package weighttracker;

public class Exercise {
    public enum Type {
        BODYWEIGHT_RATIO,
        REP_COUNT,
        TIME_LOWER_BETTER
    }

    private final String id;
    private final String name;
    private final Type type;
    private final double[] ratioLevels;
    private final int[] repLevels;
    private final double[] timeLevels;

    private Exercise(String id, String name, Type type, double[] ratioLevels, int[] repLevels, double[] timeLevels) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ratioLevels = ratioLevels;
        this.repLevels = repLevels;
        this.timeLevels = timeLevels;
    }

    public static Exercise ratioExercise(String id, String name, double[] ratioLevels) {
        return new Exercise(id, name, Type.BODYWEIGHT_RATIO, ratioLevels, null, null);
    }

    public static Exercise repExercise(String id, String name, int[] repLevels) {
        return new Exercise(id, name, Type.REP_COUNT, null, repLevels, null);
    }

    public static Exercise timeExercise(String id, String name, double[] timeLevels) {
        return new Exercise(id, name, Type.TIME_LOWER_BETTER, null, null, timeLevels);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getLevelCount() {
        if (type == Type.BODYWEIGHT_RATIO) {
            return ratioLevels.length;
        }
        if (type == Type.TIME_LOWER_BETTER) {
            return timeLevels.length;
        }
        return repLevels.length;
    }

    public int levelFor(double value) {
        if (type == Type.BODYWEIGHT_RATIO) {
            return levelForRatio(value);
        }
        if (type == Type.TIME_LOWER_BETTER) {
            return levelForLowerIsBetter(value, timeLevels);
        }
        return levelForReps((int) Math.round(value));
    }

    public double thresholdForLevel(int level) {
        if (type == Type.BODYWEIGHT_RATIO) {
            return ratioLevels[level - 1];
        }
        if (type == Type.TIME_LOWER_BETTER) {
            return timeLevels[level - 1];
        }
        return repLevels[level - 1];
    }

    private int levelForRatio(double ratio) {
        int level = 1;
        for (int i = 0; i < ratioLevels.length; i++) {
            if (ratio >= ratioLevels[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    private int levelForReps(int reps) {
        int level = 1;
        for (int i = 0; i < repLevels.length; i++) {
            if (reps >= repLevels[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    private int levelForLowerIsBetter(double value, double[] thresholds) {
        int level = 1;
        for (int i = 0; i < thresholds.length; i++) {
            if (value <= thresholds[i]) {
                level = i + 1;
            }
        }
        return level;
    }
}
