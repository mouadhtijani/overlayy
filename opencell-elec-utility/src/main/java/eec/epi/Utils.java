package eec.epi;

import org.meveo.admin.exception.BusinessException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class contains utility methods for parsing date strings and converting string values to Long.
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class Utils {
    // SimpleDateFormat instance for parsing dates
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Parses a date string into a Date object.
     *
     * @param datestr The date string to be parsed.
     * @return The parsed Date object.
     * @throws BusinessException If there is an issue parsing the date.
     */
    public Date parseDate(String datestr) {
        try {
            synchronized (SDF) {
                return SDF.parse(datestr);
            }
        } catch (ParseException pe) {
            throw new BusinessException("Fail parse date : " + pe.getMessage());
        }

    }

    /**
     * Parses a string into a Long value.
     *
     * @param longStr The string to be parsed as a Long.
     * @return The parsed Long value.
     * @throws BusinessException If there is an issue parsing the Long value.
     */
    public Long parseLong(String longStr) {
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            throw new BusinessException("Fail parse string to Long  : " + e.getMessage());
        }

    }

}
