package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.PowerConsumptionSensorController;
import org.openbase.bco.dal.remote.unit.PowerConsumptionSensorRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionSensorRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PowerConsumptionSensorRemoteTest.class);

    private static PowerConsumptionSensorRemote powerConsumptionRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public PowerConsumptionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.POWER_CONSUMPTION_LABEL;

            powerConsumptionRemote = new PowerConsumptionSensorRemote();
            powerConsumptionRemote.initByLabel(label);
            powerConsumptionRemote.activate();
            powerConsumptionRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (powerConsumptionRemote != null) {
                powerConsumptionRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class PowerConsumptionSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getPowerConsumption method, of class
     * PowerConsumptionSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerConsumption() throws Exception {
        System.out.println("getPowerConsumption");
        double consumption = 200d;
        double voltage = 100d;
        double current = 2d;
        PowerConsumptionState state = PowerConsumptionState.newBuilder().setConsumption(consumption).setCurrent(current).setVoltage(voltage).build();
        ((PowerConsumptionSensorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(powerConsumptionRemote.getId())).updatePowerConsumptionStateProvider(state);
        powerConsumptionRemote.requestData().get();
        Assert.assertEquals("The getter for the power consumption returns the wrong voltage value!", state.getVoltage(), powerConsumptionRemote.getPowerConsumptionState().getVoltage(), 0.1);
        Assert.assertEquals("The getter for the power consumption returns the wrong consumption value!", state.getConsumption(), powerConsumptionRemote.getPowerConsumptionState().getConsumption(), 0.1);
        Assert.assertEquals("The getter for the power consumption returns the wrong cirremt value!", state.getCurrent(), powerConsumptionRemote.getPowerConsumptionState().getCurrent(), 0.1);
    }
}
