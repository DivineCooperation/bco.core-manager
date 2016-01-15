/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.SmokeDetectorController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.SmokeDetectorRemote;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.state.SmokeStateType.SmokeState;

/**
 *
 * @author thuxohl
 */
public class SmokeDetectorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SmokeDetectorRemoteTest.class);

    private static SmokeDetectorRemote smokeDetectorRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public SmokeDetectorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.SMOKE_DETECTOR_LABEL;

        smokeDetectorRemote = new SmokeDetectorRemote();
        smokeDetectorRemote.init(label, location);
        smokeDetectorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (smokeDetectorRemote != null) {
            smokeDetectorRemote.shutdown();
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
     * Test of notifyUpdated method, of class SmokeDetectorRemote.
     */
    @Ignore
    public void testNotifyUpdated() throws Exception {
    }

    /**
     * Test of getSmokeAlarmState method, of class SmokeDetectorRemote.
     */
    @Test(timeout = 60000)
    public void testGetSmokeAlarmState() throws Exception {
        System.out.println("getSmokeAlarmState");
        AlarmState alarmState = AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
        ((SmokeDetectorController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(smokeDetectorRemote.getId())).updateSmokeAlarmState(alarmState);
        smokeDetectorRemote.requestStatus();
        Assert.assertEquals("The getter for the smoke alarm state returns the wrong value!", alarmState, smokeDetectorRemote.getSmokeAlarmState());
    }

    /**
     * Test of getSmokeState method, of class SmokeDetectorRemote.
     */
    @Test(timeout = 60000)
    public void testGetSmokeState() throws Exception {
        System.out.println("getSmokeState");
        SmokeState smokeState = SmokeState.newBuilder().setValue(SmokeState.State.SOME_SMOKE).setSmokeLevel(13d).build();
        ((SmokeDetectorController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(smokeDetectorRemote.getId())).updateSmokeState(smokeState);
        smokeDetectorRemote.requestStatus();
        Assert.assertEquals("The getter for the smoke state returns the wrong value!", smokeState, smokeDetectorRemote.getSmokeState());
    }

}