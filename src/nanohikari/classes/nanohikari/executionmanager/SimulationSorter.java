/*
 * Copyright (C) 2021 alafuente
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Alban Lafuente
 */
public class SimulationSorter
{
    private final boolean m_wavelengthAbscissa;
    private final ContinuousFunction m_spectra;
    private final HashMap<BigDecimal, BigDecimal> m_densityOfStates = new HashMap<>();
    private final HashMap<BigDecimal, BigDecimal> m_times = new HashMap<>();
    private final HashMap<BigDecimal, BigDecimal> m_energies = new HashMap<>();
    
    public SimulationSorter (boolean p_wavelengthAbscissa, BigDecimal p_energyIntervalSize, List<BigDecimal> p_timesList, List<BigDecimal> p_energyList, List<BigDecimal> p_statesLevels, BigDecimal p_sampleVolume)
    {
        m_wavelengthAbscissa = p_wavelengthAbscissa;
        
        p_timesList.sort(null);
        p_energyList.sort(null);
        p_statesLevels.sort(null);
        
        //INTERVAL CHOICE TO BE REWORKED, DOESN'T WORK WELL AT THE MOMENT

        //cutting the timespan of the experiment into a given number of intervals (here 5000) and puting the number of recombined electrons during each intervals
        BigDecimal maxTime = p_timesList.get(p_timesList.size() - 1);
        BigDecimal timeInterval = maxTime.divide(new BigDecimal("5000"), MathContext.DECIMAL128);
        for (BigDecimal currentTime = BigDecimal.ZERO ; currentTime.compareTo(maxTime) == -1 ; currentTime = currentTime.add(timeInterval))
        {
            BigDecimal currentMax = currentTime.add(timeInterval);
            int nRecomb = 0;
            
            while (p_timesList.size() > 0 && p_timesList.get(0).compareTo(currentMax) <= 0)
            {
                nRecomb += 1;
                p_timesList.remove(0);
            }
            
            m_times.put(currentTime, new BigDecimal(nRecomb));
        }
        
        //the interval for the energy is given in the constructor
        BigDecimal minEnergy = p_energyList.get(0);
        BigDecimal maxEnergy = p_energyList.get(p_energyList.size() - 1);
        BigDecimal maxCounts = BigDecimal.ZERO;
        
        for (BigDecimal currentEnergy = minEnergy ; currentEnergy.compareTo(maxEnergy) == -1 ; currentEnergy = currentEnergy.add(p_energyIntervalSize))
        {
            BigDecimal currentMax = currentEnergy.add(p_energyIntervalSize);
            int nEnergy = 0;
            
            while (p_energyList.size() > 0 && p_energyList.get(0).compareTo(currentMax) <= 0)
            {
                nEnergy += 1;
                p_energyList.remove(0);
            }
            
            BigDecimal nEnergyBig = new BigDecimal(nEnergy);
            m_energies.put(currentEnergy, new BigDecimal(nEnergy));
            
            if (nEnergyBig.compareTo(maxCounts) > 0)
            {
                maxCounts = nEnergyBig;
            }
        }
        
        //normalisation
        for (BigDecimal wavelength: m_energies.keySet())
        {
            m_energies.put(wavelength, m_energies.get(wavelength).divide(maxCounts, MathContext.DECIMAL128));
        }
        
        m_spectra = new ContinuousFunction(m_energies);
        
        //density of state calculation
        BigDecimal minState = p_statesLevels.get(0);
        BigDecimal maxState = p_statesLevels.get(p_statesLevels.size() - 1);
        BigDecimal DOSInterval = (new BigDecimal("0.002")).multiply(PhysicsVariables.EV);
        
        for (BigDecimal lowestBound = minState ; lowestBound.compareTo(maxState) == -1 ; lowestBound = lowestBound.add(DOSInterval))
        {
            BigDecimal currentMax = lowestBound.add(DOSInterval);
            int nLevels = 0;
            
            while (p_statesLevels.size() > 0 && p_statesLevels.get(0).compareTo(currentMax) <= 0)
            {
                nLevels += 1;
                p_statesLevels.remove(0);
            }
            
            m_densityOfStates.put(lowestBound, (new BigDecimal(nLevels)).divide(p_sampleVolume));
        }
    }
    
    static public SimulationSorter sorterWithNoIntervalGiven(boolean p_wavelengthAbscissa, List<BigDecimal> p_timesList, List<BigDecimal> p_energiesList, List<BigDecimal> p_energyLevels, BigDecimal p_sampleVolume)
    {
        //guessing a good energy interval size: separating the energy span into 
        BigDecimal energyInterval;
        
        if (p_wavelengthAbscissa)
        {
            p_energiesList.sort(null);
            BigDecimal minEnergy = p_energiesList.get(0);
            BigDecimal maxEnergy = p_energiesList.get(p_energiesList.size() - 1);
            energyInterval = (maxEnergy.subtract(minEnergy)).divide(new BigDecimal("150"), MathContext.DECIMAL128);
        }
        else
        {
            energyInterval = (new BigDecimal("0.002")).multiply(PhysicsVariables.EV);
        }
        
        return new SimulationSorter(p_wavelengthAbscissa, energyInterval, p_timesList, p_energiesList, p_energyLevels, p_sampleVolume);
    }
    
    public void saveToFile(File timeFile, File energyFile, File DOSFile) throws IOException
    {
        //writing times
        Set<BigDecimal> timeSet = new TreeSet(m_times.keySet());
        BufferedWriter timeWriter = new BufferedWriter(new FileWriter(timeFile));
        timeWriter.write("Time (ps)\tIntensity (cps)");
        for (BigDecimal time: timeSet)
        {
            timeWriter.newLine();
            timeWriter.write(time.divide(PhysicsVariables.UnitsPrefix.PICO.getMultiplier(), MathContext.DECIMAL128).toPlainString() + "\t" + m_times.get(time));
        }
        timeWriter.flush();
        timeWriter.close();
        
        //writing wavelength calculated from energies
        Set<BigDecimal> energySet = new TreeSet(m_energies.keySet());
        BufferedWriter spectraWriter = new BufferedWriter(new FileWriter(energyFile));
        if (m_wavelengthAbscissa)
        {
            spectraWriter.write("Wavelength (nm)\tIntensity");
        }
        else
        {
            spectraWriter.write("Energy (eV)\tIntensity");
        }
        for (BigDecimal energy: energySet)
        {
            BigDecimal energyConverted;
            if (m_wavelengthAbscissa)
            {
                energyConverted = energy.divide(PhysicsVariables.UnitsPrefix.NANO.getMultiplier(), MathContext.DECIMAL128);
            }
            else
            {
                energyConverted = energy.divide(PhysicsVariables.EV, MathContext.DECIMAL128);
            }
            energyConverted = energyConverted.setScale(energyConverted.scale() - energyConverted.precision() + 4, RoundingMode.HALF_UP);
            
            spectraWriter.newLine();
            spectraWriter.write(energyConverted.toPlainString() + "\t" + m_energies.get(energy));
        }
        spectraWriter.flush();
        spectraWriter.close();
        
        //writing DOS
        Set<BigDecimal> statesSet = new TreeSet<>(m_densityOfStates.keySet());
        BufferedWriter DOSwriter = new BufferedWriter(new FileWriter(DOSFile));
        DOSwriter.write("Energy (eV)\tDOS (m^-2)");
        for (BigDecimal state: statesSet)
        {
            BigDecimal stateToWrite = state.divide(PhysicsVariables.EV, MathContext.DECIMAL128).setScale(state.scale() - state.precision() + 4, RoundingMode.HALF_UP);
            
            DOSwriter.newLine();
            DOSwriter.write(stateToWrite.toPlainString() + "\t" + m_densityOfStates.get(state));
        }
        DOSwriter.flush();
        DOSwriter.close();
    }
    
    public ContinuousFunction getLuminescence()
    {
        return new ContinuousFunction(m_spectra);
    }
}