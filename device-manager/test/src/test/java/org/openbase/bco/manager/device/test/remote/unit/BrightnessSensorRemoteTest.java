package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.data.Location;
import org.openbase.bco.dal.lib.layer.unit.BrightnessSensorController;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.remote.unit.BrightnessSensorRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.pattern.Remote;
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
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public BrightnessSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.openbase.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        deviceManagerLauncher.getDeviceManager().waitForInit(30, TimeUnit.SECONDS);

        location = new Location(registry.getLocation());
        label = MockRegistry.BRIGHTNESS_SENSOR_LABEL;

        brightnessSensorRemote = new BrightnessSensorRemote();
        brightnessSensorRemote.init(label, location);
        brightnessSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (brightnessSensorRemote != null) {
            brightnessSensorRemote.shutdown();
        }
        MockRegistryHolder.shutdownMockRegistry();
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
    @Test(timeout = 10000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        brightnessSensorRemote.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        double brightness = 0.5;
        ((BrightnessSensorController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(brightnessSensorRemote.getId())).updateBrightnessProvider(brightness);
        brightnessSensorRemote.requestData().get();
        assertEquals("The getter for the brightness returns the wrong value!", brightness, brightnessSensorRemote.getBrightness(), 0.1);
    }
}
