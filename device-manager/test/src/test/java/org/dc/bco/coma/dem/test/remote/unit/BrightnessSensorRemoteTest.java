/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import org.dc.bco.registry.device.core.mock.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.BrightnessSensorController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class BrightnessSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BrightnessSensorRemoteTest.class);

    private static BrightnessSensorRemote brightnessSensorRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public BrightnessSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.BRIGHTNESS_SENSOR_LABEL;

        brightnessSensorRemote = new BrightnessSensorRemote();
        brightnessSensorRemote.init(label, location);
        brightnessSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (brightnessSensorRemote != null) {
            brightnessSensorRemote.shutdown();
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
     * Test of notifyUpdated method, of class BrightnessSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBrightness method, of class BrightnessSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        double brightness = 0.5;
        ((BrightnessSensorController) dalService.getUnitRegistry().get(brightnessSensorRemote.getId())).updateBrightness(brightness);
        brightnessSensorRemote.requestStatus();
        assertEquals("The getter for the brightness returns the wrong value!", brightness, brightnessSensorRemote.getBrightness(), 0.1);
    }
}
