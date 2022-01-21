/*
 * Copyright (C) 2021 audreyazura
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nanohikari.executionmanager;

import albanlafuente.physicstools.math.ContinuousFunction;
import albanlafuente.physicstools.physics.PhysicsVariables;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Alban Lafuente
 */
public class SimulationJudge
{
    private final MatchObject<BigDecimal> m_maxMatching;
    private final MatchObject<HashMap<BigDecimal, BigDecimal>> m_shapeMatchingHighEnergy;
    
    public SimulationJudge (ContinuousFunction p_experimentalLuminescence, ContinuousFunction p_simulatedLuminescence)
    {
        BigDecimal experimentalMaxPosition = p_experimentalLuminescence.maximum().get("abscissa");
        BigDecimal simulatedMaxPosition = p_simulatedLuminescence.maximum().get("abscissa");
        
        //comparing the position of maximum
        //supposing an acceptable error on the abscissa of +/-1 meV
        BigDecimal maxErrorMaximum = (new BigDecimal("0.001")).multiply(PhysicsVariables.EV);
        BigDecimal differenceMaxEnergy = experimentalMaxPosition.subtract(simulatedMaxPosition);
        if(differenceMaxEnergy.abs().compareTo(maxErrorMaximum) <= 0)
        {
            m_maxMatching = new MatchObject<>(true, BigDecimal.ZERO);
        }
        else
        {
            m_maxMatching = new MatchObject<>(false, experimentalMaxPosition.divide(simulatedMaxPosition, MathContext.DECIMAL128));
        }
        
        //putting an acceptable error on the shape of 5%
        BigDecimal maxErrorShape = new BigDecimal("0.05");
        BigDecimal experimentalLuminescenceIntegral = p_experimentalLuminescence.integrate();
        BigDecimal calculatedLuminescenceIntegral = p_simulatedLuminescence.integrate();
        BigDecimal intervalSize = p_experimentalLuminescence.getMeanIntervalSize();
        Set<BigDecimal> experimentalLuminescenceAbscissa = p_experimentalLuminescence.getAbscissa();
        
        boolean shapeMatch = true;
        HashMap<BigDecimal, BigDecimal> shapeDifferences = new HashMap<>();
        
        for (BigDecimal position: experimentalLuminescenceAbscissa)
        {
            if (position != p_experimentalLuminescence.end())
            {
                BigDecimal distanceFromMaximum = position.subtract(experimentalMaxPosition);
            
                BigDecimal startPositionCalculation = simulatedMaxPosition.add(distanceFromMaximum);
                BigDecimal endPositionCalculation = startPositionCalculation.add(intervalSize);
                BigDecimal calculatedSliceIntegral;
                if (p_simulatedLuminescence.isInRange(startPositionCalculation))
                {
                    if (p_simulatedLuminescence.isInRange(endPositionCalculation))
                    {
                        calculatedSliceIntegral = p_simulatedLuminescence.integrate(startPositionCalculation, endPositionCalculation);
                    }
                    else
                    {
                        calculatedSliceIntegral = p_simulatedLuminescence.integrate(startPositionCalculation, p_simulatedLuminescence.end());
                    }
                }
                else
                {
                    if (p_simulatedLuminescence.isInRange(endPositionCalculation))
                    {
                        calculatedSliceIntegral = p_simulatedLuminescence.integrate(p_simulatedLuminescence.start(), endPositionCalculation);
                    }
                    else
                    {
                       calculatedSliceIntegral = BigDecimal.ZERO;
                    }
                }

                BigDecimal experimentalIntegralRatio = p_experimentalLuminescence.integrate(position, position.add(intervalSize)).divide(experimentalLuminescenceIntegral, MathContext.DECIMAL128);
                BigDecimal calculatedIntegralRatio = calculatedSliceIntegral.divide(calculatedLuminescenceIntegral, MathContext.DECIMAL128);
                BigDecimal integralRatioDifference = experimentalIntegralRatio.subtract(calculatedIntegralRatio);

                if (integralRatioDifference.abs().compareTo(BigDecimal.ONE) >= 0)
                {
                    throw new ArithmeticException("Probability difference bigger than 1");
                }

                shapeMatch &= integralRatioDifference.abs().compareTo(maxErrorShape) <= 0;
                shapeDifferences.put(distanceFromMaximum, integralRatioDifference);
            }
        }
        
        //the error should compensate each other. If they don't, an error occured. We give ourselves a leeway of 1e-20.
//        if((differenceShapeHighEnergy.add(differenceShapeLowEnergy)).compareTo(new BigDecimal("1e-20")) <= 0)
//        {
//            //the absolute error is basically the same on each side, therefore we can only care on one side
//            m_shapeMatchingHighEnergy = new MatchObject<>(((differenceShapeHighEnergy.abs()).compareTo(maxErrorShape) <= 0), differenceShapeHighEnergy);
//        }
//        else
//        {
//            throw new ArithmeticException("Sum of ratioed integral different than 1.");
//        }
        
        m_shapeMatchingHighEnergy = new MatchObject<>(shapeMatch, shapeDifferences);
    }
    
    public boolean maximumMatch()
    {
        return m_maxMatching.isMatching();
    }
    
    public boolean shapeMatch()
    {
        return m_shapeMatchingHighEnergy.isMatching();
    }
    
    public BigDecimal maximumRatio() throws NumberFormatException
    {
        BigDecimal difference = BigDecimal.ZERO;
        
        if (!m_maxMatching.isMatching())
        {
            try
            {
                difference = new BigDecimal(m_maxMatching.comment().toString());
            }
            catch (NumberFormatException exception)
            {
                throw exception;
            }
        }
        
        return difference;
    }
    
    public HashMap<BigDecimal, BigDecimal> shapeDifferenceMap() throws NumberFormatException
    {
        HashMap<BigDecimal, BigDecimal> differences = new HashMap<>();
        
        if (!m_shapeMatchingHighEnergy.isMatching())
        {
            try
            {
                HashMap<BigDecimal, BigDecimal> judgedDiff = m_shapeMatchingHighEnergy.comment();
                for (BigDecimal key: judgedDiff.keySet())
                {
                    differences.put(new BigDecimal(key.toString()), new BigDecimal(judgedDiff.get(key).toString()));
                }
                
            }
            catch (NumberFormatException exception)
            {
                throw exception;
            }
        }
        
        return differences;
    }
    
    private class MatchObject<E>
    {
        private final boolean m_matching;
        private final E m_comment;

        public MatchObject(boolean p_match, E p_comment)
        {
            m_matching = p_match;
            m_comment = p_comment;
        }

        @Override
        public String toString()
        {
            String returnString;

            if (m_matching)
            {
                returnString = "Matching";
            }
            else
            {
                returnString = "Not matching, " + m_comment.toString();
            }

            return returnString;
        }

        public boolean isMatching()
        {
            return m_matching;
        }

        public E comment()
        {
            return m_comment;
        }
    }
}
