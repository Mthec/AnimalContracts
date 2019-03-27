package mod.wurmunlimited.contracts.animals;

import com.google.common.base.Joiner;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AnimalContractTests {
    private AnimalContractsObjectsFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AnimalContractsObjectsFactory();
    }

    @Test
    void testLoadContract() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature();
        contractItem.setInscription(Long.toString(creature.getWurmId()), "");

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertTrue(contract.hasCreature(creature));
    }

    @Test
    void testLoadMultipleCreatures() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        List<Creature> creatures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            Creature creature = factory.createNewCreature();
            creatures.add(creature);
            contractItem.setInscription(Objects.requireNonNull(contractItem.getInscription()).getInscription() + "\n" + creature.getWurmId(), "");
        }

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        for (int i = 0; i < 50; i++) {
            assertTrue(contract.hasCreature(creatures.get(i)));
        }
    }

    @Test
    void testLoadInvalidCreatureId() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        contractItem.setInscription("abc", "");

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertEquals(0, contract.getAllCreatures().size());
    }

    @Test
    void testLoadDeadOrRemovedCreature() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature();
        contractItem.setInscription(Long.toString(creature.getWurmId()), "");
        creature.destroy();

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertEquals(0, contract.getAllCreatures().size());
    }

    @Test
    void testAddCreature() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature();

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertDoesNotThrow(() -> contract.addCreature(creature));
        assertTrue(contract.hasCreature(creature));
    }

    @Test
    void testAddPlayerBlocked() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewPlayer();

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertDoesNotThrow(() -> contract.addCreature(creature));
        assertFalse(contract.hasCreature(creature));
    }

    @Test
    void testAddNpcBlocked() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature(CreatureTemplateIds.NPC_HUMAN_CID);

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertDoesNotThrow(() -> contract.addCreature(creature));
        assertFalse(contract.hasCreature(creature));
    }

    @Test
    void testHasCreature() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature();

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertEquals(0, contract.getAllCreatures().size());
        assertFalse(contract.hasCreature(creature));

        contractItem.setInscription(Long.toString(creature.getWurmId()), "");
        contract = AnimalContract.getAnimalContract(contractItem);
        assertEquals(1, contract.getAllCreatures().size());
        assertSame(creature, contract.getAllCreatures().get(0));
        assertTrue(contract.hasCreature(creature));
    }

    @Test
    void testRemoveCreature() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature();

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertDoesNotThrow(() -> contract.addCreature(creature));
        assertTrue(contract.hasCreature(creature));
    }

    @Test
    void testGetAllCreatures() throws ContractFullException {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature();

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        contract.addCreature(creature);
        assert contract.hasCreature(creature);
        assertEquals(1, contract.getAllCreatures().size());
        assertSame(creature, contract.getAllCreatures().get(0));
    }

    @Test
    void testIsAnimalContract() {
        assertTrue(AnimalContract.isAnimalContract(factory.createNewItem(AnimalContractsMod.getContractTemplateId())));
        assertFalse(AnimalContract.isAnimalContract(factory.createNewItem(ItemList.papyrusSheet)));
    }

    @Test
    void testSaveContract() throws ContractFullException {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        Creature creature = factory.createNewCreature();
        Creature creature2 = factory.createNewCreature();

        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);
        assertEquals("", Objects.requireNonNull(contractItem.getInscription()).getInscription());

        contract.addCreature(creature);
        assertEquals(Long.toString(creature.getWurmId()), Objects.requireNonNull(contractItem.getInscription()).getInscription());

        contract.addCreature(creature2);
        assertEquals(String.format("%s\n%s", creature.getWurmId(), creature2.getWurmId()),
                Objects.requireNonNull(contractItem.getInscription()).getInscription());

        contract.removeCreature(creature);
        assertEquals(Long.toString(creature2.getWurmId()), Objects.requireNonNull(contractItem.getInscription()).getInscription());
    }

    @Test
    void testFullContractDoesNotRemoveCurrent() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        List<Creature> creatures = new ArrayList<>();
        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);

        while (true) {
            try {
                Creature creature = factory.createNewCreature();
                contract.addCreature(creature);
                creatures.add(creature);
            } catch (ContractFullException e) {
                assertEquals(creatures, AnimalContract.getAnimalContract(contractItem).getAllCreatures());
                assertEquals(Joiner.on("\n").join(creatures.stream().map(Creature::getWurmId).collect(Collectors.toList())),
                        Objects.requireNonNull(contractItem.getInscription()).getInscription());
                break;
            }
        }
    }

    @Test
    void testAddingToFullContractDoesNotAddCreature() {
        Item contractItem = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        List<Creature> creatures = new ArrayList<>();
        AnimalContract contract = AnimalContract.getAnimalContract(contractItem);

        Creature creature = factory.createNewCreature();
        while (true) {
            try {
                creature = factory.createNewCreature();
                creatures.add(creature);
                contract.addCreature(creature);
            } catch (ContractFullException e) {
                assertEquals(creatures.size() - 1, contract.getAllCreatures().size());
                assertNotEquals(creatures, contract.getAllCreatures());
                assertFalse(contract.hasCreature(creature));
                break;
            }
        }
    }
}
