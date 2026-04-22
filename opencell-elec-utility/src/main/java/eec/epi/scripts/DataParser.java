package eec.epi.scripts;

import org.meveo.admin.exception.BusinessException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * This class contains utility methods for parsing various data types.
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class DataParser {
    // Date format for parsing dates
    protected static final String[] ACCEPTED_FORMATS = {"dd/MM/yyyy", "yyyy/MM/dd","yyyy-MM-dd"};
    protected static final SimpleDateFormat[] SDFS = new SimpleDateFormat[ACCEPTED_FORMATS.length];

    static {
        for (int i = 0; i < ACCEPTED_FORMATS.length; i++) {
            SDFS[i] = new SimpleDateFormat(ACCEPTED_FORMATS[i]);
        }
    }


    /**
     * Parses a date string into a Date object.
     *
     * @param datestr The date string to be parsed.
     * @return The parsed Date object.
     * @throws BusinessException If there is an issue parsing the date.
     */
    public Date parseDate(String datestr) {
        for (SimpleDateFormat sdf : SDFS) {
            try {
                Date parsedDate = sdf.parse(datestr);
                String reformattedDate = sdf.format(parsedDate); // Reformat the parsed date
                if (reformattedDate.equals(datestr)) {
                    return parsedDate;
                }
            } catch (ParseException ignored) {
                // Ignoring ParseException for this format, will continue checking other formats
            }
        }
        return null;
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
