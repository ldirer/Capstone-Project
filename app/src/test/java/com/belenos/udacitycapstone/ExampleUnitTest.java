package com.belenos.udacitycapstone;

import com.belenos.udacitycapstone.utils.Utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void getProbabilityDistributionTest() throws Exception {


        List<Integer> attemptsWordIdArray = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        List<Integer> attemptsCountArray = new ArrayList<>(Arrays.asList(0, 10, 2, 5, 0, 0, 1, 20, 2, 1));
        List<Float> attemptsSuccessRateArray = new ArrayList<>(Arrays.asList(null, 0.4f, 0.5f, 0.6f, null, null, 0f, 0.9f, 0.5f, 1f));

        Integer nWordsSeenByUser = 0;
        Float totalSuccessRatesInverse = 0f;
        for (int i = 0; i < attemptsCountArray.size(); i++) {
            Integer count = attemptsCountArray.get(i);
            Float successRate = attemptsSuccessRateArray.get(i);
            if (count != 0 && successRate != 0){
                nWordsSeenByUser += 1;
                totalSuccessRatesInverse += 1 / successRate;
            }
        }

        List<Float> distribution = Utils.getProbabilityDistribution(attemptsWordIdArray, attemptsCountArray, attemptsSuccessRateArray, nWordsSeenByUser, totalSuccessRatesInverse);

        Float totalProbability = 0f;
        for (Float p : distribution) {
            totalProbability += p;
        }
        // We just make sure we have a probability distribution.
        assertEquals(1f, totalProbability, 0.0001f);
    }
}