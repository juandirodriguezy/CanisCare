package com.example.caniscare

import com.example.caniscare.data.calculator.DogAgeCalculator
import com.example.caniscare.data.calculator.DogSize
import com.example.caniscare.data.calculator.LifeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DogAgeCalculatorTest {

    @Test
    fun testDefaultValuesWhenInputsAreMissing() {
        val result = DogAgeCalculator.calculate(name = "", ageYearsInput = null)
        assertNotNull(result)
        assertEquals("Tu compañero canino", result.dogName)
        assertEquals(1.0, result.dogAgeYears, 0.001)
        assertEquals(15, result.humanEquivalentAge)
        assertTrue(result.careRecommendations.isNotEmpty())
    }

    @Test
    fun testOneYearOldDog() {
        val result = DogAgeCalculator.calculate(name = "Rocky", ageYearsInput = 1.0, size = DogSize.SMALL)
        assertEquals("Rocky", result.dogName)
        assertEquals(15, result.humanEquivalentAge)
        assertEquals(LifeStage.ADULT, result.stage)
    }

    @Test
    fun testTwoYearOldDog() {
        val result = DogAgeCalculator.calculate(name = "Max", ageYearsInput = 2.0, size = DogSize.MEDIUM)
        assertEquals(24, result.humanEquivalentAge)
        assertEquals(LifeStage.ADULT, result.stage)
    }

    @Test
    fun testSevenYearOldDogCalculation() {
        // A los 7 años:
        // Pequeño: 24 + 5 * 4 = 44 años humanos
        val smallDog = DogAgeCalculator.calculate(ageYearsInput = 7.0, size = DogSize.SMALL)
        assertEquals(44, smallDog.humanEquivalentAge)
        assertEquals(LifeStage.SENIOR, smallDog.stage)

        // Mediano: 24 + 5 * 5 = 49 años humanos
        val mediumDog = DogAgeCalculator.calculate(ageYearsInput = 7.0, size = DogSize.MEDIUM)
        assertEquals(49, mediumDog.humanEquivalentAge)
        assertEquals(LifeStage.SENIOR, mediumDog.stage)

        // Grande: 24 + 5 * 6.5 = 56 años humanos
        val largeDog = DogAgeCalculator.calculate(ageYearsInput = 7.0, size = DogSize.LARGE)
        assertEquals(56, largeDog.humanEquivalentAge)
        assertEquals(LifeStage.SENIOR, largeDog.stage)
    }

    @Test
    fun testPuppyUnderOneYear() {
        val result = DogAgeCalculator.calculate(name = "Luna", ageYearsInput = 0.5, size = DogSize.MEDIUM)
        assertEquals(LifeStage.PUPPY, result.stage)
        assertTrue(result.humanEquivalentAge in 1..10)
        assertTrue(result.veterinarySchedule.contains("Vacunación"))
    }
}
