package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.AnimalContractManagementQuestion;
import mod.wurmunlimited.contracts.animals.AnimalContractsMod;
import mod.wurmunlimited.contracts.animals.AnimalContractsObjectsFactory;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.bmlEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ManageAnimalContractActionTests {
    private AnimalContractsObjectsFactory factory;
    private ManageAnimalContractAction action;
    private Creature owner;
    private Item contract;
    private Action act = mock(Action.class);

    @BeforeEach
    void setUp() throws Exception {
        factory = new AnimalContractsObjectsFactory();
        ActionEntryBuilder.init();
        action = new ManageAnimalContractAction(AnimalContractsMod.getContractTemplateId());
        owner = factory.createNewPlayer();
        contract = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        owner.getInventory().insertItem(contract);
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourFor() {
        List<ActionEntry> entry = action.getBehavioursFor(owner, contract);
        assertNotNull(entry);
        assertEquals(1, entry.size());
        assertEquals(action.getActionId(), entry.get(0).getNumber());
        assertEquals("Manage", entry.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForActiveItem() {
        List<ActionEntry> entry = action.getBehavioursFor(owner, factory.createNewItem(), contract);
        assertNotNull(entry);
        assertEquals(1, entry.size());
        assertEquals(action.getActionId(), entry.get(0).getNumber());
        assertEquals("Manage", entry.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForNonContract() {
        assertNull(action.getBehavioursFor(owner, factory.createNewItem()));
    }

    // action

    @Test
    void testActionManage() {
        assertTrue(action.action(act, owner, contract, action.getActionId(), 0));
        new AnimalContractManagementQuestion(owner, contract).sendQuestion();
        assertThat(owner, bmlEqual());
    }

    @Test
    void testActionManageActiveItem() {
        assertTrue(action.action(act, owner, factory.createNewItem(), contract, action.getActionId(), 0));
        new AnimalContractManagementQuestion(owner, contract).sendQuestion();
        assertThat(owner, bmlEqual());
    }

    @Test
    void testActionIncorrectActionId() {
        assertFalse(action.action(act, owner, contract, (short)(action.getActionId() + 1), 0));
    }

    @Test
    void testActionNotAContract() {
        assertFalse(action.action(act, owner, factory.createNewItem(), action.getActionId(), 0));
    }
}
