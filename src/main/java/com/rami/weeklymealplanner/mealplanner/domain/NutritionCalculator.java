package com.rami.weeklymealplanner.mealplanner.domain;

import org.springframework.stereotype.Component;

@Component
public class NutritionCalculator {

    private static final double INCH_TO_CM = 2.54;
    private static final double LB_TO_KG = 0.45359237;
    private static final double SEDENTARY_MULTIPLIER = 1.2;
    private static final double KCAL_PER_WEEK_PER_LB = 3500.0;
    private static final double FEMALE_FLOOR = 1200.0;
    private static final double MALE_FLOOR = 1600.0;

    public NutritionResult calculate(
            Sex sex,
            double ageYears,
            double heightInches,
            double weightLbs,
            double targetLossLbsPerWeek
    ) {
        double weightKg = weightLbs * LB_TO_KG;
        double heightCm = heightInches * INCH_TO_CM;
        double bmr = calculateBmr(sex, ageYears, heightCm, weightKg);
        double tdee = bmr * SEDENTARY_MULTIPLIER;
        double dailyDeficit = (targetLossLbsPerWeek * KCAL_PER_WEEK_PER_LB) / 7.0;
        double uncappedTarget = tdee - dailyDeficit;
        double floor = sex == Sex.FEMALE ? FEMALE_FLOOR : MALE_FLOOR;
        double cappedTarget = Math.max(uncappedTarget, floor);

        return new NutritionResult(
                round2(bmr),
                round2(tdee),
                round2(cappedTarget),
                floor
        );
    }

    private static double calculateBmr(Sex sex, double ageYears, double heightCm, double weightKg) {
        double base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * ageYears);
        return sex == Sex.MALE ? base + 5.0 : base - 161.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record NutritionResult(
            double bmr,
            double tdee,
            double dailyCalorieTarget,
            double appliedFloor
    ) {}
}
