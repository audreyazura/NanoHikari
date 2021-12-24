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

import nanohikari.luminescencegenerator.QuantumDot;
import albanlafuente.physicstools.math.ContinuousFunction;
import albanlafuente.physicstools.physics.Metamaterial;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 *
 * @author audreyazura
 */
public class QDFitter
{
    private final boolean m_goodFit;
    private final List<QuantumDot> m_fittedQDs;
    
    public QDFitter ()
    {
        m_goodFit = false;
        m_fittedQDs = new ArrayList<>();
    }
    
    public QDFitter (List<QuantumDot> p_QDList, BigDecimal p_timeStep, ContinuousFunction p_luminescence, SimulationSorter p_sorter, GUIUpdater p_gui, Metamaterial p_sampleMaterial)
    {
        ContinuousFunction calculationResult = p_sorter.getLuminescence();
        SimulationJudge judge = new SimulationJudge(p_luminescence, calculationResult);
        m_goodFit = judge.maximumMatch() && judge.shapeMatch();
        
        List<QuantumDot> tempQDList = new ArrayList<>();
        if (!m_goodFit)
        {
            if (!judge.shapeMatch())
            {
                /*******************************************************************************************************
                 *                                                                                                     *
                 *              CONCENTRATE ALL THE LUMINESCENCE IN A FEW PEAKS... FIND WHY                            *
                 *                                                                                                     *
                 *******************************************************************************************************/
                
                BigDecimal totalNumberOfQD = new BigDecimal(p_QDList.size());
                
                BigDecimal experimentalMaxPosition = p_luminescence.maximum().get("abscissa");
                BigDecimal calculatedMaxPosition = calculationResult.maximum().get("abscissa");
                
                HashMap<BigDecimal, BigDecimal> judgedNeededQDPercentages = judge.shapeDifferenceMap();
                HashMap<BigDecimal, Integer> numberOfQDToAdd = new HashMap<>();
                HashMap<BigDecimal, Integer> numberOfQDToRemove = new HashMap<>();
                for (BigDecimal distanceFromMax: judgedNeededQDPercentages.keySet())
                {
                    BigDecimal ratioOfTotalQD = judgedNeededQDPercentages.get(distanceFromMax);
                    BigDecimal energy = calculatedMaxPosition.add(distanceFromMax);
                    
                    if (ratioOfTotalQD.compareTo(BigDecimal.ZERO) < 0)
                    {
                        numberOfQDToAdd.put(energy, 0);
                        numberOfQDToRemove.put(energy, (totalNumberOfQD.multiply(ratioOfTotalQD.abs())).intValue());
                    }
                    else
                    {
                        numberOfQDToAdd.put(energy, (totalNumberOfQD.multiply(ratioOfTotalQD)).intValue());
                        numberOfQDToRemove.put(energy, 0);
                    }
                }
                
                //defining the range outside of which, if a QD is, it will be put as available automatically
                BigDecimal intervalSize = p_luminescence.getMeanIntervalSize();
                BigDecimal experimentalDistanceBetweenMaxAndLowest = (p_luminescence.start().subtract(experimentalMaxPosition));
                BigDecimal calculationNeededLowest = calculatedMaxPosition.add(experimentalDistanceBetweenMaxAndLowest);
                BigDecimal experimentalDistanceBetweenMaxAndHighest = (p_luminescence.end().subtract(experimentalMaxPosition)).add(intervalSize);
                BigDecimal calculationNeededHighest = calculatedMaxPosition.add(experimentalDistanceBetweenMaxAndHighest);
                
                HashSet<QuantumDot> availableQDs = new HashSet<>();
                TreeSet<BigDecimal> abscissa = new TreeSet(numberOfQDToRemove.keySet());
                for (QuantumDot QD: p_QDList)
                {
                    BigDecimal QDEnergy = QD.getMeanEnergy();
                    
                    if (QDEnergy.compareTo(calculationNeededLowest) < 0 || QDEnergy.compareTo(calculationNeededHighest) > 0)
                    {
                        availableQDs.add(QD);
                    }
                    else
                    {
                        //finding the closest energy to the QD energy
                        BigDecimal energyIntervalStart = abscissa.lower(QDEnergy);
                        
                        if (energyIntervalStart == null)
                        {
                            throw new ArithmeticException("QD energy fit nowhere: " + QDEnergy);
                        }
                        else
                        {
                            int toRemoveInInterval = numberOfQDToRemove.get(energyIntervalStart);
                            if (toRemoveInInterval > 0)
                            {
                                availableQDs.add(QD);
                                numberOfQDToRemove.put(energyIntervalStart, toRemoveInInterval - 1);
                            }
                            else
                            {
                                tempQDList.add(QD.copy());
                            }
                        }
                    }
                }
                
                Iterator<QuantumDot> availableQDIterator = availableQDs.iterator();
                for (BigDecimal targetEnergy: numberOfQDToAdd.keySet())
                {
                    int numberOfQDToMove = numberOfQDToAdd.get(targetEnergy);
                    
                    while (numberOfQDToMove > 0 && availableQDIterator.hasNext())
                    {
                        QuantumDot workingQD = availableQDIterator.next();
                        
                        BigDecimal multiplier = targetEnergy.divide(workingQD.getMeanEnergy(), MathContext.DECIMAL128);
                        tempQDList.add(workingQD.copyWithSizeChange(multiplier, p_timeStep, p_sampleMaterial));
                        
                        numberOfQDToMove -= 1;
                    }
                }
                
                //if there are QD marked available and not used, we add a copy of them to tempQDList
                while (availableQDIterator.hasNext())
                {
                    tempQDList.add(availableQDIterator.next().copy());
                }
                
//                System.out.println("Adjusting the distribution of QD around the maximum.");
//                p_gui.sendMessage("Adjusting the distribution of QD around the maximum.");
//                
//                BigDecimal highEnergyDiff = judge.shapeDifferenceRatio();
//                BigDecimal pivotEnergy = p_sorter.getLuminescence().maximum().get("abscissa");
//                BigDecimal highEnergyExperimentalIntervalSize = p_luminescence.end().subtract(p_luminescence.maximum().get("abscissa"));
//                BigDecimal lowEnergyExperimentalIntervalSize = p_luminescence.start().subtract(p_luminescence.maximum().get("abscissa"));
//                
//                ArrayList<QuantumDot> highEnergyQDs = new ArrayList<>();
//                ArrayList<QuantumDot> lowEnergyQDs = new ArrayList<>();
//                ArrayList<QuantumDot> maxEnergyQDs = new ArrayList<>();
//                for (QuantumDot qd: tempQDList)
//                {
//                    if (qd.getEnergy().compareTo(pivotEnergy) > 0)
//                    {
//                        highEnergyQDs.add(qd);
//                    }
//                    else
//                    {
//                        if (qd.getEnergy().compareTo(pivotEnergy) < 0)
//                        {
//                            lowEnergyQDs.add(qd);
//                        }
//                        else
//                        {
//                            maxEnergyQDs.add(qd);
//                        }
//                    }
//                }
//                
//                if (highEnergyDiff.signum() == 1) //positive difference
//                {
//                    //take HE to LE
//                    BigDecimal numberToSwap = highEnergyDiff.multiply(new BigDecimal(p_QDList.size()));
//                    double swapProba = (numberToSwap.doubleValue())/highEnergyQDs.size();
//                    
//                    if (swapProba < 0)
//                    {
//                        throw new ArithmeticException("Probability of swapping invalid.");
//                    }
//                    else
//                    {
//                        highEnergyQDs = swapQDs(highEnergyQDs, highEnergyExperimentalIntervalSize, pivotEnergy, p_timeStep, p_sampleMaterial, swapProba);
//                    }
//                }
//                else //negative difference
//                {
//                    //take LE to HE
//                    BigDecimal numberToSwap = (BigDecimal.ONE.subtract(highEnergyDiff.abs())).multiply(new BigDecimal(p_QDList.size()));
//                    double swapProba = (numberToSwap.doubleValue())/lowEnergyQDs.size();
//                    
//                    if (swapProba < 0)
//                    {
//                        throw new ArithmeticException("Probability of swapping invalid.");
//                    }
//                    else
//                    {
//                        lowEnergyQDs = swapQDs(lowEnergyQDs, lowEnergyExperimentalIntervalSize, pivotEnergy, p_timeStep, p_sampleMaterial, swapProba);
//                    }
//                }
//                
//                //putting the QD which energy are too far from the maximum in range
//                BigDecimal energyLimit;
//                //high ernergy case
//                energyLimit = pivotEnergy.add(highEnergyExperimentalIntervalSize);
//                List<QuantumDot> inRangeHEQDs = new ArrayList<>();
//                for (QuantumDot qd: highEnergyQDs)
//                {
//                    if (qd.getEnergy().compareTo(pivotEnergy.add(energyLimit)) > 0)
//                    {
//                        inRangeHEQDs.add(getQDInEnergyRange(qd, pivotEnergy, highEnergyExperimentalIntervalSize, p_timeStep, p_sampleMaterial));
//                    }
//                    else
//                    {
//                        inRangeHEQDs.add(qd);
//                    }
//                }
//                //low energy case
//                energyLimit = (pivotEnergy.subtract(lowEnergyExperimentalIntervalSize)).max(BigDecimal.ZERO);
//                List<QuantumDot> inRangeLEQDs = new ArrayList<>();
//                for (QuantumDot qd: lowEnergyQDs)
//                {
//                    if (qd.getEnergy().compareTo(energyLimit) < 0)
//                    {
//                        //the only case where pivotEnergy < lowEnergyExperimentalRange is when the energyLimit get lower to 0 (and thus, is put to 0 by the max in its initialisation)
//                        inRangeLEQDs.add(getQDInEnergyRange(qd, energyLimit, lowEnergyExperimentalIntervalSize.min(pivotEnergy), p_timeStep, p_sampleMaterial));
//                    }
//                    else
//                    {
//                        inRangeLEQDs.add(qd);
//                    }
//                }
//                
//                //we group all the new QDs in a single list (the low energy one) and save it in m_QDList
//                tempQDList = new ArrayList<>();
//                tempQDList.addAll(inRangeLEQDs);
//                tempQDList.addAll(maxEnergyQDs);
//                tempQDList.addAll(inRangeHEQDs);
            }
            else
            {
                for (QuantumDot oldQD: p_QDList)
                {
                    tempQDList.add(oldQD.copy());
                }
            }
            
            if (!judge.maximumMatch())
            {
                System.out.println("Adjusting the position of the maximum.");
                p_gui.sendMessage("Adjusting the position of the maximum.");
                
                BigDecimal multiplier = BigDecimal.ONE.divide(judge.maximumRatio(), MathContext.DECIMAL128);
                ArrayList<QuantumDot> oldQDList = new ArrayList<>();
                for (QuantumDot qd: tempQDList)
                {
                    oldQDList.add(qd.copy());
                }
                tempQDList = new ArrayList<>();
                for (QuantumDot oldQD: oldQDList)
                {
                    tempQDList.add(oldQD.copyWithSizeChange(multiplier, p_timeStep, p_sampleMaterial));
                }
            }
        }
        else
        {
            System.out.println("The simulation is in agreement with the experiment, nothing to do.");
            p_gui.sendMessage("The simulation is in agreement with the experiment, nothing to do.");
        }
        
        m_fittedQDs = new ArrayList<>();
        for (QuantumDot qd: tempQDList)
        {
            m_fittedQDs.add(qd.copy());
        }
    }
    
//    private QuantumDot getQDInEnergyRange (QuantumDot p_originalQD, BigDecimal p_rangeMin, BigDecimal p_intervalSize, BigDecimal p_timeStep, Metamaterial p_sampleMaterial)
//    {
//        BigDecimal newQDEnergy = BigDecimal.ZERO;
//        PcgRSFast RNGenerator = new PcgRSFast();
//        QuantumDot newQD;
//        
//        //we select the new QD energy randomly in the interval ]minEnergy, maxEnergy+intervalSize]. intervalSize can be negative.
//        do
//        {
//            newQDEnergy = p_rangeMin.add(p_intervalSize.multiply(new BigDecimal(RNGenerator.nextDouble(true, true))));
//        }while(newQDEnergy.signum() < 0);
//
//        BigDecimal sizeMultiplier = p_originalQD.getMeanEnergy().divide(newQDEnergy, MathContext.DECIMAL128); //energy multiplier = newEnergy / oldEnergy, size multiplier = 1 / (energy multiplier)
//        newQD = p_originalQD.copyWithSizeChange(sizeMultiplier, p_timeStep, p_sampleMaterial);
//        
//        return newQD;
//    }
//    
//    private ArrayList<QuantumDot> swapQDs (ArrayList<QuantumDot> p_qdToSwap, BigDecimal p_intervalSize, BigDecimal p_pivotEnergy, BigDecimal p_timeStep, Metamaterial p_sampleMaterial, double p_swapProba)
//    {
//        ArrayList<QuantumDot> swappedList = new ArrayList<>();
//        PcgRSFast RNGenerator = new PcgRSFast();
//        
//        for (QuantumDot qd: p_qdToSwap)
//        {
//            if (RNGenerator.nextDouble() < p_swapProba)
//            {
//                qd = getQDInEnergyRange(qd, p_pivotEnergy, p_intervalSize, p_timeStep, p_sampleMaterial);
//            }
//            
//            swappedList.add(qd);
//        }
//        
//        return swappedList;
//    }
    
    public ArrayList<QuantumDot> getFittedQDs()
    {
        ArrayList<QuantumDot> returnQDList = new ArrayList<>();
        
        for (QuantumDot qd: m_fittedQDs)
        {
            returnQDList.add(qd.copy());
        }
        
        return new ArrayList(returnQDList);
    }
    
    public boolean isGoodFit()
    {
        return m_goodFit;
    }
}
