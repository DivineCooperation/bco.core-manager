package org.openbase.bco.manager.user.test.remote.user;

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
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.bco.manager.user.core.UserManagerLauncher;
import org.openbase.bco.manager.user.remote.UserRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.authorization.UserActivityType.UserActivity;
import rst.authorization.UserActivityType.UserActivity.Activity;
import rst.authorization.UserPresenceStateType.UserPresenceState;
import rst.authorization.UserConfigType.UserConfig;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UserRemoteTest.class);

    private static UserManagerLauncher userManagerLauncher;
    private static UserRemote userRemote;
    private static MockRegistry registry;

    public UserRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        userManagerLauncher = new UserManagerLauncher();
        userManagerLauncher.launch();

        UserConfig userConfig = MockRegistry.testUser;
        userRemote = new UserRemote();
        userRemote.init(userConfig);
        userRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (userManagerLauncher != null) {
            userManagerLauncher.shutdown();
        }
        if (userRemote != null) {
            userRemote.shutdown();
        }
        MockRegistryHolder.shutdownMockRegistry();
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test of getUsername method, of class UserRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testGetUserName() throws Exception {
        System.out.println("testGetUserName");
        userRemote.requestData().get();
        assertEquals("The user created int he manager has a different user name than the one registered!", MockRegistry.USER_NAME, userRemote.getData().getUserName());
    }

    @Test(timeout = 5000)
    public void testSetUserValues() throws Exception {
        System.out.println("testSetUserValues");

        UserActivity activity = UserActivity.newBuilder().setCurrentActivity(Activity.EATING).setLastActivity(Activity.COOKING).setNextActivity(Activity.RELAXING).build();;
        UserPresenceState presenceState = UserPresenceState.newBuilder().setValue(UserPresenceState.State.AT_HOME).build();

        userRemote.setUserActivity(activity).get();
        userRemote.setUserPresenceState(presenceState).get();

        assertEquals("UserActivity has not been set!", activity, userRemote.getUserActivity());
        assertEquals("UserPresenceState has not been set!", presenceState, userRemote.getUserPresenceState());
    }
}
