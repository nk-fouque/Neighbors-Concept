package implementation.gui.model;

public class SearchHistoryElement {
    private boolean caseSens;
    private boolean safeMode;
    private boolean bNode;
    private String filter;

    public SearchHistoryElement(boolean caseSens, boolean safeMode, boolean bNode, String filter) {
        this.caseSens = caseSens;
        this.safeMode = safeMode;
        this.bNode = bNode;
        this.filter = filter;
    }

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
