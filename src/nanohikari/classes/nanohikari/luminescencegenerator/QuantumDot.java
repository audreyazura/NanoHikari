/*
 * Copyright (C) 2020-2021 Alban Lafuente
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
package nanohikari.luminescencegenerator;

import albanlafuente.physicstools.physics.Material;
import albanlafuente.physicstools.physics.Metamaterial;
import albanlafuente.physicstools.physics.PhysicsVariables;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.nevec.rjm.BigDecimalMath;

/**
 *
 * @author Alban Lafuente
 */
public class QuantumDot extends AbsorberObject
{
    private final ArrayList<BigDecimal> m_listOfStates;
    private final BigDecimal m_radius;
    private final BigDecimal m_height;
    private final BigDecimal m_meanQDEnergy;
    private final double m_baseCaptureProbability;
    private final double m_escapeProbability;
    private final double m_recombinationProbability;
    private final HashMap<BigDecimal, BigDecimal> m_flatOccupationProbabilities;
    private final HashMap<BigDecimal, Integer> m_freeStates;
    
    private final HashMap<Electron, BigDecimal> m_electronsEnergy;
    
    private int m_numberOfFreeStates;
    
    /**
     * WARNING: be careful when using this constructor (or the copy function), it may cause some memory leak by keeping a pointer to the original electrons
     * @param p_positionX
     * @param p_positionY
     * @param p_radius
     * @param p_height
     * @param p_meanEnergy
     * @param p_captureProba
     * @param p_escapeProba
     * @param p_recombinationProba
     * @param p_energyLevelsPopProba
     * @param p_freeStates
     * @param p_electrons
     * @param p_nbFreeLevels 
     */
    private QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_radius, BigDecimal p_height, BigDecimal p_meanEnergy, double p_captureProba, double p_escapeProba, double p_recombinationProba, Map<BigDecimal, BigDecimal> p_energyLevelsPopProba, Map<BigDecimal, Integer> p_freeStates, Map<Electron, BigDecimal> p_electrons, int p_nbFreeLevels)
    {
        m_positionX = new BigDecimal(p_positionX.toString());
        m_positionY = new BigDecimal(p_positionY.toString());
        m_radius = new BigDecimal(p_radius.toString());
        m_height = new BigDecimal(p_height.toString());
        m_meanQDEnergy = new BigDecimal(p_meanEnergy.toString());
        m_baseCaptureProbability = p_captureProba;
        m_escapeProbability = p_escapeProba;
        m_recombinationProbability = p_recombinationProba;
        m_numberOfFreeStates = p_nbFreeLevels;
        m_listOfStates = new ArrayList<>();
        
        m_flatOccupationProbabilities = new HashMap<>();
        for (BigDecimal level: p_energyLevelsPopProba.keySet())
        {
            m_flatOccupationProbabilities.put(new BigDecimal(level.toString()), p_energyLevelsPopProba.get(level));
        }
        
        m_freeStates = new HashMap<>();
        for (BigDecimal level: p_freeStates.keySet())
        {
            m_freeStates.put(new BigDecimal(level.toString()), p_freeStates.get(level));
        }
        
        m_electronsEnergy = new HashMap<>();
        for (Electron electron: p_electrons.keySet())
        {
            m_electronsEnergy.put(electron, p_electrons.get(electron));
        }
    }

    public QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_radius, BigDecimal p_height, BigDecimal p_timeStep, Metamaterial p_sampleMaterial)
    {
        BigDecimal two = new BigDecimal("2");
        Material QDMaterial = p_sampleMaterial.getMaterial("QD");
        Material barrierMaterial = p_sampleMaterial.getMaterial("barrier");
        
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        
        m_radius = p_radius.multiply(BigDecimal.ONE);
        m_height = p_height.multiply(BigDecimal.ONE);
        BigDecimal equivalentSquareSide = m_radius.multiply(BigDecimalMath.sqrt(BigDecimalMath.pi(MathContext.DECIMAL128), MathContext.DECIMAL128));
        
        BigDecimal CBOffset = p_sampleMaterial.getOffset(QDMaterial.getMaterialName(), barrierMaterial.getMaterialName()); //from https://aip.scitation.org/doi/abs/10.1063/1.125965

        //calculating hole confinement energy, only considering one level
        BigDecimal VBOffset = barrierMaterial.getBandgap().subtract(QDMaterial.getBandgap()).subtract(CBOffset);
        BigDecimal planeEnergyParameterHole = energyParameter(0, equivalentSquareSide, VBOffset, QDMaterial.getHoleEffectiveMass());
        BigDecimal heightEnergyParameterHole = energyParameter(0, m_height, VBOffset, QDMaterial.getHoleEffectiveMass());
        BigDecimal holeConfinementEnergy = (two.multiply(PhysicsVariables.hbar.pow(2)).divide(QDMaterial.getHoleEffectiveMass(), MathContext.DECIMAL128)).multiply(heightEnergyParameterHole.add(two.multiply(planeEnergyParameterHole)));
        
        TreeSet<BigDecimal> energyLevels = new TreeSet<>();
        m_freeStates = new HashMap<>();
        m_listOfStates = new ArrayList<>();
        for (int nz = 0 ; nz < 10 ; nz += 1)
        {
            for (int nx = 0 ; nx < 100 ; nx += 1)
            {
                for (int ny = 0 ; ny < 100 ; ny += 1)
                {
                    BigDecimal xEnergyParameterElectron = energyParameter(nx, equivalentSquareSide, CBOffset, QDMaterial.getElectronEffectiveMass());
                    BigDecimal yEnergyParameterElectron = energyParameter(ny, equivalentSquareSide, CBOffset, QDMaterial.getElectronEffectiveMass());
                    BigDecimal zEnergyParameterElectron = energyParameter(nz, p_height, CBOffset, QDMaterial.getElectronEffectiveMass());
                    
                    if (xEnergyParameterElectron.compareTo(BigDecimal.ZERO) < 0 || yEnergyParameterElectron.compareTo(BigDecimal.ZERO) < 0 || zEnergyParameterElectron.compareTo(BigDecimal.ZERO) < 0)
                    {
                        break;
                    }
                    
                    BigDecimal energyXElectron = xEnergyParameterElectron.divide(equivalentSquareSide, MathContext.DECIMAL128).pow(2);
                    BigDecimal energyYElectron = yEnergyParameterElectron.divide(equivalentSquareSide, MathContext.DECIMAL128).pow(2);
                    BigDecimal energyZElectron = zEnergyParameterElectron.divide(p_height, MathContext.DECIMAL128).pow(2);

                    BigDecimal electronConfinementEnergy = (two.multiply(PhysicsVariables.hbar.pow(2)).divide(QDMaterial.getElectronEffectiveMass(), MathContext.DECIMAL128)).multiply(energyXElectron.add(energyYElectron).add(energyZElectron));
                    if (electronConfinementEnergy.compareTo(CBOffset) > 0)
                    {
                        break;
                    }
                    
                    energyLevels.add(electronConfinementEnergy);
                    
                    //adding the energy to the list of states
                    BigDecimal totalRecombinationEnergy = electronConfinementEnergy.add(QDMaterial.getBandgap()).add(holeConfinementEnergy);
                    if (totalRecombinationEnergy.compareTo(BigDecimal.ZERO) < 0)
                    {
                        throw new InternalError("Negative recombination energy.");
                    }
                    m_listOfStates.add(totalRecombinationEnergy);
                    m_listOfStates.add(totalRecombinationEnergy);
                    
                    if (m_freeStates.keySet().contains(totalRecombinationEnergy))
                    {
                        m_freeStates.put(totalRecombinationEnergy, m_freeStates.get(totalRecombinationEnergy) + 2);
                    }
                    else
                    {
                        m_freeStates.put(totalRecombinationEnergy, 2);
                    }
                }
            }
        }
        
        m_numberOfFreeStates = m_listOfStates.size();
        
        m_flatOccupationProbabilities = new HashMap<>();
        
        if (m_numberOfFreeStates == 0)
        {
            //if the first QD energy level is higher than barrier conduction band, the QD cannot confine the carrier, and thus the capture probability is null
            m_baseCaptureProbability = 0;
            m_meanQDEnergy = BigDecimal.ZERO;
        }
        else
        {
            //else, the capture probability is calculated using P_capture = 1 - exp(-Δt/tau_capture), with tau_capture given in https://aip.scitation.org/doi/10.1063/1.1512694
            m_baseCaptureProbability = (BigDecimal.ONE.subtract(BigDecimalMath.exp(p_timeStep.negate().divide(QDMaterial.getCaptureTime(m_radius), MathContext.DECIMAL128)))).doubleValue();
            
            /**RECOMB PROBA PER LEVEL
             * calculate probability for each level using Fermi-Dirac distribution and the energy calculated from the QD material CB position
             * BIG approximation: chemical potential = 0
             */

            //calcul of the probability for an electron to be on a given level
            BigDecimal sumOfProba = BigDecimal.ZERO;
            HashMap<BigDecimal, BigDecimal> levelsProbabilities = new HashMap<>();
            for (BigDecimal energy: energyLevels)
            {
                BigDecimal fermiDiracProba = BigDecimal.ONE.divide(BigDecimal.ONE.add(BigDecimalMath.exp((energy.subtract(BigDecimal.ZERO)).divide(PhysicsVariables.KB.multiply(new BigDecimal("300")), MathContext.DECIMAL128), MathContext.DECIMAL128)), MathContext.DECIMAL128);
                sumOfProba = sumOfProba.add(fermiDiracProba);
                levelsProbabilities.put(energy, fermiDiracProba);
                
                BigDecimal totalRecombinationEnergy = energy.add(QDMaterial.getBandgap()).add(holeConfinementEnergy);
                m_flatOccupationProbabilities.put(totalRecombinationEnergy, fermiDiracProba);
            }

            //normalizing the probabilities in order to get a mean energy as accurate as possible
            BigDecimal meanEnergy = BigDecimal.ZERO;
            for (BigDecimal energy: energyLevels)
            {
                BigDecimal normalizedProba = levelsProbabilities.get(energy).divide(sumOfProba, MathContext.DECIMAL128);

                BigDecimal totalRecombinationEnergy = energy.add(QDMaterial.getBandgap()).add(holeConfinementEnergy);
                meanEnergy = meanEnergy.add(totalRecombinationEnergy.multiply(normalizedProba));
            }
            
            m_meanQDEnergy = meanEnergy;
        }
        
//       System.out.println(m_recombinationProbaTree.size() + "\t" + m_baseCaptureProbability + "\t" + canCapture());
        
        //the escape probability is calculated using P_capture = 1 - exp(-Δt/tau_escape) with tau_escape from https://aip.scitation.org/doi/10.1063/1.4824469
        m_escapeProbability = (BigDecimal.ONE.subtract(BigDecimalMath.exp(p_timeStep.negate().divide(QDMaterial.getEscapeTime(m_radius), MathContext.DECIMAL128)))).doubleValue();
        
        //according to Andreev et al., the electron lifetime in GaN/AlN QD is 3.6 ns https://doi.org/10.1063/1.1386405. I didn't found data on GaAs/InAs for the lifetime once it was captured
        m_recombinationProbability = (BigDecimal.ONE.subtract(BigDecimalMath.exp(p_timeStep.negate().divide(QDMaterial.getRecombinationTime(m_radius), MathContext.DECIMAL128)))).doubleValue();
        
        m_electronsEnergy = new HashMap<>();
    }
    
    synchronized public boolean canCapture()
    {
        return m_baseCaptureProbability > 0 && m_numberOfFreeStates > 0;
    }
    
    /**
     * The capture probability depends on many parameters and demand to be further investigate.
     * At the moment, the probability is calculated by the probability for the electron to reach the QDs multiplied by a capture probability based theoretical calculation (to be implemented)
     * At the moment, it is approximated as the overlapping between the QD and the circle containing the positions the electron can reach
     * See here for the calculation of the overlap: https://www.xarg.org/2016/07/calculate-the-intersection-area-of-two-circles/
     * It had to be slightly adapted with four different cases:
     *  - when the electron is entirely inside the QD (electronDistance + electronSpan &lt; radius)
     *  - when the QD is entirely "inside" the electron (electronDistance + radius &lt; electronSpan)
     *  - when the center of the QD is farther away from the electron position than the intersection between the QD limit and the electron span limit (electronDistance &gt; sqrt(abs(radius^2 - electronSpan^2)))
     *  - when the center of the QD is closer from the electron position than the intersection between the QD limit and the electron span limit (electronDistance &lt; sqrt(abs(radius^2 - electronSpan^2)))
     * @param p_RNG the random number generator
     * @param electronDistance the distance between the center of the QD and electron position
     * @param electronSpan the circle containing the position the electron can reach
     * @return whether the electron has been captured or not
     */
    synchronized public boolean capture(PcgRSFast p_RNG, Electron electronToCapture, BigDecimal electronDistance, BigDecimal electronSpan)
    {
        double reachingProbability = 0;
        boolean result = false;
        
        /** calculating the probability of the electron to reach the QD
        *   This is done by calculating how many of the position the electron can reach are occupied by the QD
        *   case if the electron is entirely inside the QD
        */
        if (canCapture())
        {
            if (electronDistance.add(electronSpan).compareTo(m_radius) <= 0)
            {
                reachingProbability = 1;
            }
            else
            {
                //if the QD is entirely in the electron span
                if (electronDistance.add(m_radius).compareTo(electronSpan) <= 0)
                {
                    reachingProbability = (m_radius.pow(2).divide(electronSpan.pow(2), MathContext.DECIMAL128)).doubleValue();
                }
                else
                {
                    BigDecimal overlapArea = BigDecimal.ZERO;

                    //we compare the distance to sqrt(abs(radius^2 - electronSpan^2))
                    BigDecimal radiusDiff = BigDecimalMath.sqrt(((m_radius.pow(2)).subtract(electronSpan.pow(2))).abs(), MathContext.DECIMAL128);

                    //if the QD center is farther away than the intersection points
                    if (electronDistance.compareTo(radiusDiff) >= 0)
                    {
                        //the base of the triangle for the calculation here is (electronSpan^2 + electronDistance^2 - QDradius^2) / (2 * electronDistance)
                        BigDecimal triangleBase = (electronSpan.pow(2).add(electronDistance.pow(2)).subtract(m_radius.pow(2))).divide(electronDistance.multiply(new BigDecimal("2")), MathContext.DECIMAL128);

                        BigDecimal electronSlice = electronSpan.pow(2).multiply(BigDecimalMath.acos(triangleBase.divide(electronSpan, MathContext.DECIMAL128)));
                        BigDecimal QDSlice = m_radius.pow(2).multiply(BigDecimalMath.acos((electronDistance.subtract(triangleBase)).divide(m_radius, MathContext.DECIMAL128)));
                        BigDecimal triangleCorrection = electronDistance.multiply(BigDecimalMath.sqrt(electronSpan.pow(2).subtract(triangleBase.pow(2)), MathContext.DECIMAL128));

                        overlapArea = electronSlice.add(QDSlice).subtract(triangleCorrection);
                    }
                    else
                    {
                        //the base of the triangle for the calculation here is (electronSpan^2 - electronDistance^2 - QDradius^2) / (2 * electronDistance)
                        BigDecimal triangleBase = (electronSpan.pow(2).subtract(electronDistance.pow(2)).subtract(m_radius.pow(2))).divide(electronDistance.multiply(new BigDecimal("2")), MathContext.DECIMAL128);

                        BigDecimal electronSlice = electronSpan.pow(2).multiply(BigDecimalMath.acos((triangleBase.add(electronDistance)).divide(electronSpan, MathContext.DECIMAL128)));
                        BigDecimal QDSlice = m_radius.pow(2).multiply(BigDecimalMath.pi(MathContext.DECIMAL128).subtract(BigDecimalMath.acos(triangleBase.divide(m_radius, MathContext.DECIMAL128))));
                        BigDecimal triangleCorrection = electronDistance.multiply(BigDecimalMath.sqrt(m_radius.pow(2).subtract(triangleBase.pow(2)), MathContext.DECIMAL128));

                        overlapArea = electronSlice.add(QDSlice).subtract(triangleCorrection);
                    }

                    reachingProbability = overlapArea.divide(BigDecimalMath.pi(MathContext.DECIMAL128).multiply(electronSpan.pow(2)), MathContext.DECIMAL128).doubleValue();
                }
            }

            if (reachingProbability < 0 || reachingProbability > 1)
            {
                System.out.println("Probability has to be bound between 0 and 1");
                Logger.getLogger(QuantumDot.class.getName()).log(Level.SEVERE, null, new ArithmeticException("Probability has to be bound between 0 and 1"));
            }
            
            //the complete capture probability is the probability to reach the QD multiplied by the probability to be captured multiplied by the ratio of remaining free states
            if (p_RNG.nextDouble() < reachingProbability * m_baseCaptureProbability * (m_numberOfFreeStates / m_listOfStates.size()))
            {
                //finding the levels available to the electrons and getting the sum of their probability
                TreeSet<BigDecimal> availableStates = new TreeSet<>();
                BigDecimal sumOfProba = BigDecimal.ZERO;
                for (BigDecimal state: m_freeStates.keySet())
                {
                    if (m_freeStates.get(state) > 0)
                    {
                        availableStates.add(state);
                        sumOfProba = sumOfProba.add(m_flatOccupationProbabilities.get(state));
                    }
                }
                
                //calculating the normalized proba so that the sum of the proba for each level equal one (we know the electron is now on one of them)
                double sumOfPreviousProba = 0;
                HashMap<Double, BigDecimal> statesProba = new HashMap<>();
                for (BigDecimal state: availableStates)
                {
                    double normalizedProba = (m_flatOccupationProbabilities.get(state).divide(sumOfProba)).doubleValue();
                    sumOfPreviousProba += normalizedProba;
                    if (state.compareTo(availableStates.last()) == 0  && sumOfPreviousProba < 1)
                    {
                        sumOfPreviousProba = 1;
                    }
                    
                    statesProba.put(sumOfPreviousProba, state);
                }
                
                //getting the level where the electron will be stored
                double randomNumber = p_RNG.nextDouble();
                TreeSet<Double> probaSet = new TreeSet(statesProba.keySet());
                Iterator<Double> probaIterator = probaSet.iterator();
                double proba = probaSet.first();
                while (probaIterator.hasNext() && (proba = probaIterator.next()) < randomNumber){}
                BigDecimal populatedState = statesProba.get(proba);
                
                m_freeStates.put(populatedState, m_freeStates.get(populatedState) - 1);
                m_electronsEnergy.put(electronToCapture, populatedState);
                
                m_numberOfFreeStates -= 1;
                result = true;
            }
        }
        
        return result;
    }
    
    /**
     * WARNING: be careful when using this constructor (or the copy function), it may cause some memory leak by keeping a pointer to the original electrons
     * @return 
     */
    public QuantumDot copy()
    {
        return new QuantumDot(m_positionX, m_positionY, m_radius, m_height, m_meanQDEnergy, m_baseCaptureProbability, m_escapeProbability, m_recombinationProbability, m_flatOccupationProbabilities, m_freeStates, m_electronsEnergy, m_numberOfFreeStates);
    }
    
    public QuantumDot copyWithSizeChange(BigDecimal p_sizeMultiplier, BigDecimal p_timeStep, Metamaterial p_sampleMaterial)
    {
        BigDecimal newRadius = m_radius;
        BigDecimal newHeight = m_height;
        
        if (m_radius.compareTo(m_height) > 0)
        {
            newHeight = newHeight.multiply(p_sizeMultiplier);
        }
        else
        {
            newRadius = newRadius.multiply(p_sizeMultiplier);
        }
        
        return new QuantumDot(m_positionX, m_positionY, newRadius, newHeight, p_timeStep, p_sampleMaterial);
    }
    
    /**
     * See https://en.wikipedia.org/wiki/Finite_potential_well
     * @param index 
     * @param size
     * @param bandOffset 
     * @param effectiveMass 
     * @return 
     */
    private BigDecimal energyParameter (int index, BigDecimal size, BigDecimal bandOffset, BigDecimal effectiveMass)
    {
        double u02 = (effectiveMass.multiply(size.pow(2)).multiply(bandOffset).divide((new BigDecimal(2)).multiply(PhysicsVariables.hbar.pow(2)), MathContext.DECIMAL128)).doubleValue();
        double vi = 0;
        
        //vi has to be between i*pi/2 and (i+1)*v/2. Minimum Vi should also always be lower than u0
        double minVi = index * Math.PI/2;
        if (Math.pow(minVi, 2) >= u02)
        {
//            System.err.println("Too high of a minVi");
            vi = -1;
        }
        else
        {
            vi = minVi + Math.random()*Double.min(Math.PI/2, Math.sqrt(u02) - minVi);
            
            double maxVi = (index + 1) * Math.PI/2;
            double error = 1E-14;
            double epsilon = 1E-15;
            int counter = 0;
            
            do
            {
                double derivative = derivativeFunction(index, vi);
                if (Math.abs(derivative) <= epsilon)
                {
                    break;
                }
                
                vi = Math.abs(vi - ((functionToOptimize(index, vi) - u02) / derivative));
                
                while (vi <= minVi || vi >= maxVi)
                {
                    //vi has to be between i*pi/2 and (i+1)*pi/2
                    if (vi < minVi)
                    {
                        vi = vi - (Math.PI/2) * (int) ((vi)/(Math.PI/2)) + minVi ;
                    }
                    else
                    {
                        if (vi > maxVi)
                        {
                            vi = vi - (Math.PI/2) * (int) ((vi)/(Math.PI/2)) + minVi;
                        }
                        else
                        {
                            vi *= 1.1;
                        }
                    }
                }
                
                counter += 1;
                if (counter%100 == 0)
                {
                    error *= 2;
                }
            }while(Math.abs(functionToOptimize(index, vi) - u02) >= error);
        }
        
        return new BigDecimal(vi);
    }
    
    private double functionToOptimize(int index, double v)
    {
        if (index % 2 == 0)
        {
            return Math.pow(v, 2) * (1 + Math.pow(Math.tan(v), 2));
        }
        else
        {
            return Math.pow(v, 2) * (1 + 1 / Math.pow(Math.tan(v), 2));
        }
    }
    
    private double derivativeFunction(int index, double v)
    {
        double function = 0;
        double modif = 0;
        
        if (index % 2 == 0)
        {
            function = Math.tan(v);
            modif = 1 / Math.pow(Math.cos(v), 2);
        }
        else
        {
            function = 1 / Math.tan(v);
            modif = 1 / Math.pow(Math.sin(v), 2);
        }
        
        return 2 * v * (1 + Math.pow(function, 2) + v * function * modif);
    }

    //will calculate probability based on phonon density
    synchronized public boolean escape(PcgRSFast p_RNG, Electron p_electronToEscape)
    {
        if (p_RNG.nextDouble() < m_escapeProbability)
        {
            BigDecimal electronEnergy = m_electronsEnergy.get(p_electronToEscape);
            m_freeStates.put(electronEnergy, m_freeStates.get(electronEnergy) + 1);
            m_electronsEnergy.remove(p_electronToEscape);
            m_numberOfFreeStates += 1;
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public BigDecimal getMeanEnergy()
    {
        return m_meanQDEnergy;
    }
    
    public BigDecimal getRadius()
    {
        return m_radius;
    }
    
    public ArrayList<BigDecimal> getStates()
    {
        ArrayList<BigDecimal> listOfStates = new ArrayList<>();
        
        for (BigDecimal state: m_listOfStates)
        {
            listOfStates.add(new BigDecimal(state.toPlainString()));
        }
        
        return listOfStates;
    }
    
    synchronized public BigDecimal recombine(PcgRSFast p_RNG, Electron p_electronToRecombine)
    {
        BigDecimal result;
        
        if (p_RNG.nextDouble() < m_recombinationProbability)
        {
            result = m_electronsEnergy.get(p_electronToRecombine);
            m_electronsEnergy.remove(p_electronToRecombine);
            m_freeStates.put(result, m_freeStates.get(result) + 1);
            m_numberOfFreeStates += 1;
        }
        else
        {
            result = BigDecimal.ONE.negate();
        }
        
        return result;
    }
    
    public String scaledString(BigDecimal p_sizeScale, BigDecimal p_energyScale)
    {
        //new scale: number.scale() - number.precision() gives the number of digits after the point in scientific notation. Setting the scale to this + 11 gives us at least 10 digits after the points, which is enough
        BigDecimal scaledX = (m_positionX.divide(p_sizeScale, MathContext.DECIMAL128)).setScale(m_positionX.scale() - m_positionX.precision() + 11, RoundingMode.HALF_UP).stripTrailingZeros();
        BigDecimal scaledY = (m_positionY.divide(p_sizeScale, MathContext.DECIMAL128)).setScale(m_positionY.scale() - m_positionY.precision() + 11, RoundingMode.HALF_UP).stripTrailingZeros();
        BigDecimal scaledRadius = (m_radius.divide(p_sizeScale, MathContext.DECIMAL128)).setScale(m_radius.scale() - m_radius.precision() + 11, RoundingMode.HALF_UP).stripTrailingZeros();
        BigDecimal scaledHeight = (m_height.divide(p_sizeScale, MathContext.DECIMAL128)).setScale(m_height.scale() - m_height.precision() + 11, RoundingMode.HALF_UP).stripTrailingZeros();
//        BigDecimal scaledEnergy = (m_energyLevelPopulatedProbabilities.divide(p_energyScale, MathContext.DECIMAL128)).setScale(m_energyLevelPopulatedProbabilities.scale() - m_energyLevelPopulatedProbabilities.precision() + 11, RoundingMode.HALF_UP).stripTrailingZeros();
        
        return scaledX + "\t" + scaledY + "\t" + scaledRadius + "\t" + scaledHeight/* + "\t" + scaledEnergy*/;
    }
    
    @Override
    public String toString()
    {
        return m_positionX + "\t" + m_positionY + "\t" + m_radius + "\t" + m_height;
    }
}
