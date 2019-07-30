package implementation.gui.model;

/**
 * Small item encapsulating everything needed for a search history
 * Used by the "Previous" Button
 *
 * @author nk-fouque
 */
public class SearchHistoryElement {
    private boolean caseSens;
    private boolean safeMode;
    private boolean bNode;
    private String filter;

    /**
     * Base constructor with every attributes needed
     */
    public SearchHistoryElement(boolean caseSens, boolean safeMode, boolean bNode, String filter) {
        this.caseSens = caseSens;
        this.safeMode = safeMode;
        this.bNode = bNode;
        this.filter = filter;
    }

    /**
     * Constructor by copy
     */
    public SearchHistoryElement(SearchHistoryElement other) {
        caseSens = other.caseSens;
        safeMode = other.safeMode;
        bNode = other.bNode;
        filter = other.filter;
    }

    public boolean isCaseSens() {
        return caseSens;
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public boolean isbNode() {
        return bNode;
    }

    public String getFilter() {
        return filter;
    }
}
