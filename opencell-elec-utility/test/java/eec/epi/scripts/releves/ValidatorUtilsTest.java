//package eec.epi.scripts.releves;
//
//import eec.epi.scripts.ValidatorUtils;
//import org.junit.Test;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//
//public class ValidatorUtilsTest {
//    private final ValidatorUtils validatorUtils = new ValidatorUtils();
//
//    @Test
//    public void testIsNullOrEmpty() {
//        assertTrue(validatorUtils.isNullOrEmpty(null));
//        assertTrue(validatorUtils.isNullOrEmpty(""));
//        assertTrue(validatorUtils.isNullOrEmpty(new ArrayList<>()));
//
//        assertFalse(validatorUtils.isNullOrEmpty("Hello"));
//        assertFalse(validatorUtils.isNullOrEmpty(new ArrayList<>(List.of(1, 2, 3))));
//    }
//
//    @Test
//    public void testIsNotValidEnumValue() {
//        assertFalse(validatorUtils.isNotValidEnumValue(MyEnum.class, "VALUE1"));
//        assertTrue(validatorUtils.isNotValidEnumValue(MyEnum.class, "INVALID"));
//    }
//
//    @Test
//    public void testIsNotNumeric() {
//        assertTrue(validatorUtils.isNotNumeric("abc"));
//        assertFalse(validatorUtils.isNotNumeric("123"));
//    }
//
//    @Test
//    public void testIsNullOrEmptyOrNotNumericSTR() {
//        assertTrue(validatorUtils.isNullOrEmptyOrNotNumericSTR(null));
//        assertTrue(validatorUtils.isNullOrEmptyOrNotNumericSTR(""));
//        assertTrue(validatorUtils.isNullOrEmptyOrNotNumericSTR("abc"));
//
//        assertFalse(validatorUtils.isNullOrEmptyOrNotNumericSTR("123"));
//        assertFalse(validatorUtils.isNullOrEmptyOrNotNumericSTR("456"));
//    }
//
//    private enum MyEnum {
//        VALUE1, VALUE2, VALUE3
//    }
//}