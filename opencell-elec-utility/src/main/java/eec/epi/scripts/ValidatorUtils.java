package eec.epi.scripts;

import eec.epi.scripts.DataParser;
import org.meveo.admin.exception.BusinessException;


import java.util.ArrayList;


import static java.util.Objects.isNull;

/**
 * This class contains utility methods for validating data.
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class ValidatorUtils extends DataParser {


    /**
     * Checks if a value is null or empty.
     *
     * @param value The value to be checked.
     * @param <T>   The type of the value.
     * @return True if the value is null or empty, otherwise false.
     */
    public <T> boolean isNullOrEmpty(T value) {
        return isNull(value) || (value instanceof String && ((String) value).isEmpty()) || (value instanceof ArrayList && ((ArrayList<?>) value).isEmpty());
    }
    /**
     * Checks if a value is not Date.
     *
     * @param value The value to be checked.
     * @param <T>   The type of the value.
     * @return True if the value is null or empty, otherwise false.
     */
    public <T> boolean isNullOrEmptyOrNotValidDate(T value) {
        return isNullOrEmpty(value) || parseDate((String) value)==null;
    }

    /**
     * Checks if a given value is not a valid member of the specified enum class.
     *
     * @param enumClass The class of the enum.
     * @param value     The value to be checked.
     * @param <E>       The type of the enum.
     * @return True if the value is not a valid enum value, otherwise false.
     * @throws BusinessException If there is an issue checking the enum value.
     */
    public <E extends Enum<E>> boolean isNotValidEnumValue(Class<E> enumClass, String value) {
        try {
            for (E enumValue : enumClass.getEnumConstants()) {
                if (enumValue.toString().equals(value)) {
                    return false; //enum exist == isNotValidEnumValue =false
                }
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new BusinessException("Fail while checking enum value: " + e.getMessage());
        }
        return true;
    }

    /**
     * Checks if a given input is not a numeric string.
     *
     * @param input The input string to be checked.
     * @return True if the input is not numeric, otherwise false.
     */
    public boolean isNotNumeric(String input) {
        return !input.matches("\\d+");
    }

    /**
     * Checks if a given input is null, empty, or not a numeric string.
     *
     * @param input The input string to be checked.
     * @return True if the input is null, empty, or not numeric, otherwise false.
     */
    public boolean isNullOrEmptyOrNotNumericSTR(String input) {
        return isNullOrEmpty(input) || isNotNumeric(input);
    }




}
