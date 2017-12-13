package bembibre.alarmfix.logic.models;

/**
 * Created by Max Power on 13/12/2017.
 */

/**
 * This is the result of a data import operation.
 */
public class DataImportResult {

    private DataImportResultType type;
    private long correctlyImportedElements;
    private long elementsWithError;

    public DataImportResult(DataImportResultType type, long correctlyImportedElements, long elementsWithError) {
        this.type = type;
        this.correctlyImportedElements = correctlyImportedElements;
        this.elementsWithError = elementsWithError;
    }

    public DataImportResultType getType() {
        return type;
    }

    public long getCorrectlyImportedElements() {
        return correctlyImportedElements;
    }

    public void setCorrectlyImportedElements(long correctlyImportedElements) {
        this.correctlyImportedElements = correctlyImportedElements;
    }

    public long getElementsWithError() {
        return elementsWithError;
    }

    public void setElementsWithError(long elementsWithError) {
        this.elementsWithError = elementsWithError;
    }
}
