package ee.taltech.inbankbackend.business;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.infrastructure.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import ee.taltech.inbankbackend.business.dto.Decision;
import ee.taltech.inbankbackend.infrastructure.exceptions.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     * @throws NoValidLoanException         If there is no valid loan found for the given ID code, loan amount and loan period
     * @throws InvalidAgeException          If the age does not fall within the allowed range (18-80)
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, InvalidAgeException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        String errorMessage = "You are not approved for a loan.";
        int outputLoanAmount;

        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException(errorMessage);
        }

        while (highestValidLoanAmount(loanPeriod, loanAmount) < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            loanPeriod++;
        }

        if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod, loanAmount));
        } else {
            throw new NoValidLoanException(errorMessage);
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     * @throws InvalidAgeException          If the age does not fall within the allowed range (18-80)
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, InvalidAgeException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }

        int age = getAge(personalCode);
        if (age < 18 || age > 80) {
            throw new InvalidAgeException("You are not approved for a loan due to age.");
        }

        if ((DecisionEngineConstants.MINIMUM_LOAN_AMOUNT > loanAmount)
                || (loanAmount > DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }

        if ((DecisionEngineConstants.MINIMUM_LOAN_PERIOD > loanPeriod)
                || (loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Calculates the largest valid loan for the current credit modifier, credit score and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod, Long loanAmount) {
        float initialCreditScore = getCreditScore(loanPeriod, loanAmount);
        float creditScore = 0;

        if (initialCreditScore == 1) {
            creditScore = initialCreditScore;
        } else if (initialCreditScore > 1) {
            while (initialCreditScore > 1) {
                loanAmount += 100;
                initialCreditScore = getCreditScore(loanPeriod, loanAmount);
            }
            creditScore = initialCreditScore;
        } else {
            while (creditScore < 1) {
                loanAmount -= 100;
                creditScore = getCreditScore(loanPeriod, loanAmount);
            }
        }
        return (int) (Math.floor(creditModifier * loanPeriod / creditScore / 100) * 100);
    }


    /**
     * Calculates credit score of the customer.
     *
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return Customer credit score.
     */
    private float getCreditScore(int loanPeriod, Long loanAmount) {
        return (float) creditModifier / loanAmount * loanPeriod;
    }

    /**
     * Calculates age of the customer at the time of data entry
     *
     * @param personalCode Provided personal ID code
     * @return Customer's age at the time of data entry
     */

    private int getAge(String personalCode) {
        int birthYear = extractBirthYear(personalCode);
        int birthMonth = extractBirthMonth(personalCode);
        int birthDay = extractBirthDay(personalCode);
        return calculateAge(birthYear, birthMonth, birthDay);
    }

    /**
     * Extracts customer's birth year from provided personal ID code.
     *
     * @param personalCode Provided personal ID code
     * @return Customer's birth year
     */
    private int extractBirthYear(String personalCode) {
        int year = Integer.parseInt(personalCode.substring(1, 3));

        int century = Integer.parseInt(personalCode.substring(0, 1));
        if (century == 3 || century == 4) {
            return 1900 + year;
        } else {
            return 2000 + year;
        }
    }

    /**
     * Extracts customer's birth month from provided personal ID code.
     *
     * @param personalCode Provided personal ID code
     * @return Customer's birth month
     */
    private int extractBirthMonth(String personalCode) {
        return Integer.parseInt(personalCode.substring(3, 5));
    }

    /**
     * Extracts customer's birth day from provided personal ID code.
     *
     * @param personalCode Provided personal ID code
     * @return Customer's birth day
     */
    private int extractBirthDay(String personalCode) {
        return Integer.parseInt(personalCode.substring(5, 7));
    }

    /**
     * Calculates age of the customer at the time of data entry
     *
     * @param birthYear  Extracted birth year of the customer
     * @param birthMonth Extracted birth month of the customer
     * @param birthDay   Extracted birth day of the customer
     * @return Customer's age at the time of data entry
     */
    private int calculateAge(int birthYear, int birthMonth, int birthDay) {
        LocalDate birthdate = LocalDate.of(birthYear, birthMonth, birthDay);
        LocalDate now = LocalDate.now();
        return Period.between(birthdate, now).getYears();
    }
}
