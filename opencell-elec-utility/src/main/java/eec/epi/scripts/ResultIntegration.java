package eec.epi.scripts;

/**
 * This class represents the result of an integration process, indicating success or failure along with related details.
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class ResultIntegration {
    private boolean result;
    private String error;
    private String code;

    /**
     * Constructor for a successful integration result.
     *
     * @param result The result of the integration (true for success).
     */
    public ResultIntegration(boolean result) {
        this.result = true;
        this.error = null;
        this.code = null;
    }

    /**
     * Constructor for a failed integration result.
     *
     * @param error The error message related to the integration failure.
     * @param code  The error code associated with the integration failure.
     */
    public ResultIntegration(String error, String code) {
        this.result = false;
        this.error = error;
        this.code = code;
    }

    /**
     * Check if the integration result is successful.
     *
     * @return True if the integration was successful, otherwise false.
     */
    public boolean isResult() {
        return result;
    }

    /**
     * Get the error message associated with the integration failure.
     *
     * @return The error message or null if the integration was successful.
     */
    public String getError() {
        return error;
    }

    /**
     * Get the error code associated with the integration failure.
     *
     * @return The error code or null if the integration was successful.
     */
    public String getCode() {
        return code;
    }

}
