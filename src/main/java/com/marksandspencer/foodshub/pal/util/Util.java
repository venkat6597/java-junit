package com.marksandspencer.foodshub.pal.util;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import lombok.extern.slf4j.Slf4j;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
public class Util {

    public static <T> Function<CompletableFuture<T>, T> getFutureObject() {
        return (CompletableFuture<T> future) -> {
            try {
                return future.get();
            } catch (Exception e) {
                log.error("Exception in consuming future for Object - {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
            return null;
        };
    }

    /**
	 * Convert date instance from String to LocalDateTime
	 * @param date Holds the value of a date
	 * @return {@link LocalDateTime}
	 */
	public static LocalDateTime dateConvertor(String date) {
	    try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(date, dateFormatter).atStartOfDay();
        } catch (Exception e)  {
	        throw new PALServiceException(ErrorCode.INVALID_DATE);
        }
	}

    /**
     * Returns exchange rate based on the currency code. Default 1
     * @param currency Holds the value of a currency code
     * @return exchangeRate
     */
    public static double getExchangeRate(String currency) {
        double exchangeRate = 1;
        if (ApplicationConstant.EUR.equalsIgnoreCase(currency))
            exchangeRate = 0.9;
        else if (ApplicationConstant.USD.equalsIgnoreCase(currency))
            exchangeRate = 0.7;

        return exchangeRate;
    }

    /**
     * Converts string to double value
     * @param value Holds the value of a string
     * @return double
     */
    public static Double convertStringToDouble(String value) {
        NumberFormat format = NumberFormat.getInstance(Locale.UK);
        Number number;
        try {
            number = format.parse(value);
            return Double.parseDouble(number.toString());
        } catch (ParseException e) {
            log.error("Parsing the string to double has been failed - {} - {}", value, e.getMessage());
            throw new PALServiceException(ErrorCode.INVALID_NUMBER);
        }
    }

    /**
     * Converts string to Integer value
     * @param value Holds the value of a string
     * @return Integer
     */
    public static Integer convertStringToInteger(String value) {
        try{
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e){
            log.error("Parsing the string to double has been failed - {} - {}", value, e.getMessage());
            throw new PALServiceException(ErrorCode.INVALID_NUMBER);
        }
    }

    /**
     * subtracts number of days to the give date skipping weekends
     * @param date Holds the date
     * @param days Holds the no of days to be added to the date
     * @return LocalDateTime
     */
    public static LocalDateTime subtractDaysSkippingWeekends(LocalDateTime date, int days) {
        LocalDateTime result = date;
        int subtractedDays = 0;
        while (subtractedDays < days) {
            result = result.minusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                ++subtractedDays;
            }
        }
        return result;
    }

    /**
     * adds number of days to the give date skipping weekends
     * @param date Holds the date
     * @param days Holds the no of days to be added to the date
     * @return LocalDateTime
     */
    public static LocalDateTime addDaysSkippingWeekends(LocalDateTime date, int days) {
        LocalDateTime result = date;
        int addedDays = 0;
        while (addedDays < days) {
            result = result.plusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                ++addedDays;
            }
        }
        return result;
    }

    /**
     * Converts local date to string value
     * @param localDateTime Holds the date in dd/MM/yyyy format
     * @return String
     */
    public static String convertLocalDateTimeToString(LocalDateTime localDateTime) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(localDateTime);

    }

    /**
     * get current local date time
     * @return {@link LocalDateTime}
     */
    public static LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now();
    }
}
