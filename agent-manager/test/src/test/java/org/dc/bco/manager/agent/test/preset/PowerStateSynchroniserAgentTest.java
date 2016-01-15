/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.test.preset;

import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.bco.dal.remote.unit.DimmerRemote;
import org.dc.bco.dal.remote.unit.PowerPlugRemote;
import org.dc.bco.manager.agent.core.preset.PowerStateSynchroniserAgent;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgentTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerStateSynchroniserAgentTest.class);

    public static final String POWER_STATE_SYNC_AGENT_LABEL = "Power_State_Sync_Agent_Unit_Test";

    private static PowerStateSynchroniserAgent agent;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static AgentRegistryRemote agentRemote;
    private static DeviceRegistryRemote deviceRemote;
    private static LocationRegistryRemote locationRemote;

    public PowerStateSynchroniserAgentTest() {
    }

    @BeforeClass
    public static void setUpClass() throws CouldNotPerformException, InstantiationException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();

        agentRemote = new AgentRegistryRemote();
        agentRemote.init();
        agentRemote.activate();

        deviceRemote = new DeviceRegistryRemote();
        deviceRemote.init();
        deviceRemote.activate();

        locationRemote = new LocationRegistryRemote();
        locationRemote.init();
        locationRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (agentRemote != null) {
            agentRemote.shutdown();
        }
        if (deviceRemote != null) {
            deviceRemote.shutdown();
        }
        if (locationRemote != null) {
            locationRemote.shutdown();
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
     * Test of activate method, of class PowerStateSynchroniserAgent.
     */
    @Test
    public void testPowerStateSyncAgent() throws Exception {
        System.out.println("testPowerStateSyncAgent");
        AgentConfig config = registerAgent();
        agent = new PowerStateSynchroniserAgent(config);

        DimmerRemote dimmerRemote = (DimmerRemote) agent.getSourceRemote();
        AmbientLightRemote ambientLightRemote = (AmbientLightRemote) agent.getTargetRemotes().get(0);
        PowerPlugRemote powerPlugRemote = (PowerPlugRemote) agent.getTargetRemotes().get(1);

        Thread.sleep(5000);
        agent.activate();

        logger.info("Ambient light id [" + ambientLightRemote.getId() + "]");

        dimmerRemote.setPower(PowerState.State.OFF);
        dimmerRemote.requestStatus();
        Thread.sleep(50);
        ambientLightRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("Dimmer[Source] has not been turned off", PowerState.State.OFF, dimmerRemote.getPower().getValue());
        assertEquals("AmbientLight[Target] has not been turned off as a reaction", PowerState.State.OFF, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] has not been turned off as a reaction", PowerState.State.OFF, powerPlugRemote.getPower().getValue());

        dimmerRemote.setPower(PowerState.State.ON);
        dimmerRemote.requestStatus();
        Thread.sleep(50);
        ambientLightRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("Dimmer[Source] has not been turned on", PowerState.State.ON, dimmerRemote.getPower().getValue());
        assertEquals("AmbientLight[Target] has not been turned on as a reaction", PowerState.State.ON, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] has not been turned on as a reaction", PowerState.State.ON, powerPlugRemote.getPower().getValue());

        ambientLightRemote.setPower(PowerState.State.OFF);
        ambientLightRemote.requestStatus();
        Thread.sleep(50);
        dimmerRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("AmbientLight[Target] has not been turned off", PowerState.State.OFF, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] should not have turned off as a reaction", PowerState.State.ON, powerPlugRemote.getPower().getValue());
        assertEquals("Dimmer[Source] should not have been turned off as a reaction. One target was still on.", PowerState.State.ON, dimmerRemote.getPower().getValue());

        powerPlugRemote.setPower(PowerState.State.OFF);
        powerPlugRemote.requestStatus();
        Thread.sleep(50);
        ambientLightRemote.requestStatus();
        dimmerRemote.requestStatus();
        assertEquals("PowerPlug[Target] has not been turned off", PowerState.State.OFF, powerPlugRemote.getPower().getValue());
        assertEquals("AmbientLight[Target] should still be off", PowerState.State.OFF, ambientLightRemote.getPower().getValue());
        assertEquals("Dimmer[Source] should have been turned off as a reaction.", PowerState.State.OFF, dimmerRemote.getPower().getValue());

        ambientLightRemote.setPower(PowerState.State.ON);
        ambientLightRemote.requestStatus();
        Thread.sleep(50);
        dimmerRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("AmbientLight[Target] has not been turned on", PowerState.State.ON, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] should still be off", PowerState.State.OFF, powerPlugRemote.getPower().getValue());
        assertEquals("Dimmer[Source] has not been turned on as a reaction", PowerState.State.ON, dimmerRemote.getPower().getValue());

        agent.deactivate();
    }

    private AgentConfig registerAgent() throws CouldNotPerformException {
        Entry.Builder source = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_KEY);
        Entry.Builder target1 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_1");
        Entry.Builder target2 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_2");
        Entry.Builder sourceBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_BEHAVIOUR_KEY).setValue("OFF");
        Entry.Builder targetBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_BEHAVIOUR_KEY).setValue("ON");

        for (UnitConfig unit : deviceRemote.getUnitConfigs()) {
            if (unit.getType() == UnitType.DIMMER && source.getValue().isEmpty()) {
                source.setValue(unit.getId());
            } else if (unit.getType() == UnitType.AMBIENT_LIGHT && target1.getValue().isEmpty()) {
                target1.setValue(unit.getId());
            } else if (unit.getType() == UnitType.POWER_PLUG && target2.getValue().isEmpty()) {
                target2.setValue(unit.getId());
            }

            if (source.hasValue() && target1.hasValue() && target2.hasValue()) {
                break;
            }
        }

        MetaConfig metaConfig = MetaConfig.newBuilder()
                .addEntry(source)
                .addEntry(target1)
                .addEntry(target2)
                .addEntry(sourceBehaviour)
                .addEntry(targetBehaviour).build();
        return agentRemote.registerAgentConfig(AgentConfig.newBuilder().setLabel(POWER_STATE_SYNC_AGENT_LABEL).setLocationId(locationRemote.getRootLocationConfig().getId()).setMetaConfig(metaConfig).build());
    }
}
