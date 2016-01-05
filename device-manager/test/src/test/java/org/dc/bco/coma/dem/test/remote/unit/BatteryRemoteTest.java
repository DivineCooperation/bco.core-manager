/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import org.dc.bco.registry.device.core.mock.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.BatteryController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.BatteryStateType.BatteryState;

/**
 *
 * @author thuxohl
 */
public class BatteryRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BatteryRemoteTest.class);

    private static BatteryRemote batteryRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public BatteryRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        dalService = new DALService();
        dalService.init();

        location = new Location(registry.getLocation());
        label = MockRegistry.BATTERY_LABEL;

        batteryRemote = new BatteryRemote();
        batteryRemote.init(label, location);
        batteryRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (batteryRemote != null) {
            batteryRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class BatteryRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBattaryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetBatteryLevel() throws Exception {
        System.out.println("getBatteryLevel");
        double level = 34.0;
        BatteryState state = BatteryState.newBuilder().setLevel(level).setValue(BatteryState.State.OK).build();
        ((BatteryController) dalService.getUnitRegistry().get(batteryRemote.getId())).updateBattery(state);
        batteryRemote.requestStatus();
        assertEquals("The getter for the battery level returns the wrong value!", state, batteryRemote.getBattery());
    }
}
