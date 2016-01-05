/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import org.dc.bco.registry.device.core.mock.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.RollershutterController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RollershutterRemoteTest.class);

    private static RollershutterRemote rollershutterRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public RollershutterRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.ROLLERSHUTTER_LABEL;

        rollershutterRemote = new RollershutterRemote();
        rollershutterRemote.init(label, location);
        rollershutterRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (rollershutterRemote != null) {
            rollershutterRemote.shutdown();
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
     * Test of setShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testSetShutterState() throws Exception {
        System.out.println("setShutterState");
        ShutterState.State state = ShutterState.State.DOWN;
        rollershutterRemote.setShutter(state);
        rollershutterRemote.requestStatus();
        assertEquals("Shutter has not been set in time!", state, rollershutterRemote.getData().getShutterState().getValue());
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        ShutterState.State state = ShutterState.State.STOP;
        ((RollershutterController) dalService.getUnitRegistry().get(rollershutterRemote.getId())).updateShutter(state);
        rollershutterRemote.requestStatus();
        assertEquals("Shutter has not been set in time!", rollershutterRemote.getShutter().getValue(), state);
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testSetOpeningRatio() throws Exception {
        System.out.println("setOpeningRatio");
        double openingRatio = 34.0D;
        rollershutterRemote.setOpeningRatio(openingRatio);
        rollershutterRemote.requestStatus();
        assertEquals("Opening ration has not been set in time!", openingRatio, rollershutterRemote.getData().getOpeningRatio(), 0.1);
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetOpeningRatio() throws Exception {
        System.out.println("getOpeningRatio");
        Double openingRatio = 70.0D;
        ((RollershutterController) dalService.getUnitRegistry().get(rollershutterRemote.getId())).updateOpeningRatio(openingRatio);
        rollershutterRemote.requestStatus();
        assertEquals("Opening ration has not been set in time!", openingRatio, rollershutterRemote.getOpeningRatio());
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
