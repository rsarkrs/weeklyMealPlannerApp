package com.rami.weeklymealplanner.mealplanner.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NutritionCalculatorTest {

    private final NutritionCalculator calculator = new NutritionCalculator();

    @Test
    void calculateAppliesFemaleCalorieFloor() {
        NutritionCalculator.NutritionResult result = calculator.calculate(
                Sex.FEMALE,
                30,
                64,
                140,
                2.0
        );

        assertThat(result.dailyCalorieTarget()).isGreaterThanOrEqualTo(1200.0);
        assertThat(result.appliedFloor()).isEqualTo(1200.0);
    }

    @Test
    void calculateAppliesMaleCalorieFloor() {
        NutritionCalculator.NutritionResult result = calculator.calculate(
                Sex.MALE,
                35,
                70,
                180,
                3.0
        );

        assertThat(result.dailyCalorieTarget()).isGreaterThanOrEqualTo(1600.0);
        assertThat(result.appliedFloor()).isEqualTo(1600.0);
    }
}
