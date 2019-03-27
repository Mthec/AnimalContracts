package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.contracts.animals.AnimalContract;
import mod.wurmunlimited.contracts.animals.AnimalContractsObjectsFactory;
import mod.wurmunlimited.contracts.animals.AnimalContractsMod;
import mod.wurmunlimited.contracts.animals.ContractFullException;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AssignAnimalActionTests {
    private AnimalContractsObjectsFactory factory;
    private AssignAnimalAction action;
    private Action act = mock(Action.class);
    private Player player;
    private Item contract;
    private Creature creature;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AnimalContractsObjectsFactory();
        action = new AssignAnimalAction(AnimalContractsMod.getContractTemplateId());
        player = factory.createNewPlayer();
        contract = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        player.getInventory().insertItem(contract);
        creature = factory.createNewCreature();
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourForAssign() {
        List<ActionEntry> entries = action.getBehavioursFor(player, contract, creature);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals(action.getActionId(), entries.get(0).getNumber());
        assertEquals("Assign", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForUnassign() throws ContractFullException {
        AnimalContract.getAnimalContract(contract).addCreature(creature);
        List<ActionEntry> entries = action.getBehavioursFor(player, contract, creature);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals(action.getActionId(), entries.get(0).getNumber());
        assertEquals("Unassign", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForPlayerDoesNotOwnContract() {
        assertNull(action.getBehavioursFor(player, factory.createNewItem(AnimalContractsMod.getContractTemplateId()), creature));
    }

    @Test
    void testGetBehaviourForTargetIsPlayer() {
        assertNull(action.getBehavioursFor(player, contract, factory.createNewPlayer()));
    }

    @Test
    void testGetBehaviourForNotContract() {
        assertNull(action.getBehavioursFor(player, factory.createNewItem(), creature));
    }

    @Test
    void testGetBehaviourForBadContract() {
        Item badContract = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        badContract.setInscription("Wurm Wurm Wurm", "");
        assertNull(action.getBehavioursFor(player, badContract, creature));
    }

    // action

    @Test
    void testActionAnimalAssigned() {
        assert !AnimalContract.getAnimalContract(contract).hasCreature(creature);
        assertTrue(action.action(act, player, contract, creature, action.getActionId(), 0));
        assertTrue(AnimalContract.getAnimalContract(contract).hasCreature(creature));
        assertThat(player, receivedMessageContaining("You add " + creature.getName()));
    }

    @Test
    void testActionAnimalUnassigned() throws ContractFullException {
        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(creature);
        assert animalContract.hasCreature(creature);
        assertTrue(action.action(act, player, contract, creature, action.getActionId(), 0));
        assertFalse(AnimalContract.getAnimalContract(contract).hasCreature(creature));
        assertThat(player, receivedMessageContaining("You remove " + creature.getName()));
    }

    private void assertIgnored(boolean bool) {
        assertFalse(bool);
        assertFalse(AnimalContract.getAnimalContract(contract).hasCreature(creature));
        assertThat(player, didNotReceiveMessageContaining("You add " + creature.getName()));
        assertThat(player, didNotReceiveMessageContaining("You remove " + creature.getName()));
    }

    @Test
    void testActionWrongActionId() {
        assertIgnored(action.action(act, player, contract, creature, (short)(action.getActionId() + 1), 0));
    }

    @Test
    void testActionNotContract() {
        assertIgnored(action.action(act, player, factory.createNewItem(), creature, action.getActionId(), 0));
    }

    @Test
    void testActionTargetIsPlayer() {
        assertIgnored(action.action(act, player, contract, factory.createNewPlayer(), action.getActionId(), 0));
    }

    @Test
    void testActionTargetIsNpc() {
        assertIgnored(action.action(act, player, contract, factory.createNewCreature(CreatureTemplateIds.NPC_HUMAN_CID), action.getActionId(), 0));
    }

    private void assertBlocked(boolean bool, @Nullable String expectedMessage) {
        assertTrue(bool);
        assertFalse(AnimalContract.getAnimalContract(contract).hasCreature(creature));
        assertThat(player, didNotReceiveMessageContaining("You add " + creature.getName()));
        assertThat(player, didNotReceiveMessageContaining("You remove " + creature.getName()));
        if (expectedMessage != null)
            assertThat(player, receivedMessageContaining(expectedMessage));
    }

    @Test
    void testActionInsufficientPermissions() {
        factory.createVillageFor(factory.createNewPlayer());
        assertBlocked(action.action(act, player, contract, creature, action.getActionId(), 0),
                "do not have permission");
    }

    @Test
    void testActionContractFull() {
        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        while (true) {
            try {
                animalContract.addCreature(factory.createNewCreature());
            } catch (ContractFullException e) {
                break;
            }
        }
        factory.getCommunicator(player).clear();

        assertBlocked(action.action(act, player, contract, creature, action.getActionId(), 0),
                "enough space");
    }
}
