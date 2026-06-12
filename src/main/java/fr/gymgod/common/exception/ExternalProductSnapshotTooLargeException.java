package fr.gymgod.common.exception;

public class ExternalProductSnapshotTooLargeException extends RuntimeException {

    private final String ingredientName;
    private final int serializedLength;

    public ExternalProductSnapshotTooLargeException(String ingredientName, int serializedLength) {
        super("External product snapshot for ingredient '" + ingredientName
                + "' is too large: " + serializedLength + " characters");
        this.ingredientName = ingredientName;
        this.serializedLength = serializedLength;
    }

    public String getIngredientName() { return ingredientName; }
    public int getSerializedLength()  { return serializedLength; }
}
