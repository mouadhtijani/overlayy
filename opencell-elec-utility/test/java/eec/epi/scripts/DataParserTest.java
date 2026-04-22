//package eec.epi.scripts;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.meveo.admin.exception.BusinessException;
//
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.Date;
//
//import static org.junit.Assert.*;
//
//public class DataParserTest {
//
//    private DataParser dataParser;
//
//    @Before
//    public void setUp() {
//        dataParser = new DataParser();
//    }
//
//    @Test
//    public void testValidParseDateFormats() {
//        String[] validDateStrings = {
//                "17/08/2023",
//                "2023/08/17"
//        };
//        LocalDate expectedLocalDate = LocalDate.of(2023, 8, 17);
//        for (String dateStr : validDateStrings) {
//            try {
//                Date parsedDate = dataParser.parseDate(dateStr);
//                assertNotNull(parsedDate);
//                LocalDate parsedLocalDate = parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
//                assertEquals(expectedLocalDate, parsedLocalDate);
//            } catch (BusinessException e) {
//                fail("Exception thrown for valid date: " + dateStr);
//            }
//        }
//    }
//
//    @Test
//    public void testInvalidParseDateFormats() {
//        String[] invalidDateStrings = {
//                "17-08-2023",
//                "2023-08-17",
//                "2023-17-08"
//        };
//        for (String dateStr : invalidDateStrings) {
//            Date date = dataParser.parseDate(dateStr);
//            assertNull(date);
//        }
//    }
//
//    @Test
//    public void testValidParseLong() {
//        String longStr = "12345";
//        Long expectedLong = 12345L;
//
//        try {
//            Long parsedLong = dataParser.parseLong(longStr);
//            assertEquals(expectedLong, parsedLong);
//        } catch (Exception e) {
//            fail("Exception thrown: " + e.getMessage());
//        }
//    }
//
//    @Test
//    public void testNotValidParseLong() {
//        String nonNumericStr = "abc";
//        assertThrows(BusinessException.class, () -> {
//            dataParser.parseLong(nonNumericStr);
//        });
//    }
//}