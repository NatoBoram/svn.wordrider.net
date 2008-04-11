package cz.omnicom.ermodeller.errorlog;

/**
 * Interface to implement ShowError event listener.
 *
 * @see cz.omnicom.ermodeller.errorlog.ShowErrorEvent
 */
public interface ShowErrorListener {
    /**
     * Method to implement.
     *
     * @param anEvent cz.omnicom.ermodeller.errorlog.ShowErrorEvent
     */
    public void showError(ShowErrorEvent anEvent);
}
