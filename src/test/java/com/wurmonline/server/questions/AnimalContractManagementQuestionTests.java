package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.WurmObjectsFactory;
import mod.wurmunlimited.contracts.animals.AnimalContract;
import mod.wurmunlimited.contracts.animals.AnimalContractsMod;
import mod.wurmunlimited.contracts.animals.AnimalContractsObjectsFactory;
import mod.wurmunlimited.contracts.animals.ContractFullException;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class AnimalContractManagementQuestionTests {
    private static final Pattern traits = Pattern.compile("label\\{text=\"[a-zA-Z\\d ]+\"};label\\{text=\"[a-zA-Z ]+\"};label\\{text=\"[a-zA-Z ]+\"};text\\{text=\"([\\d?]+)\";hover=\"([A-Za-z \\n.]+)\"}");
    private AnimalContractsObjectsFactory factory;
    private Player player;
    private FakeCommunicator comm;
    private Item contract;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AnimalContractsObjectsFactory();
        ReflectionUtil.setPrivateField(null, AnimalContractsMod.class.getDeclaredField("useAnimalHusbandryForTraits"), false);
        player = factory.createNewPlayer();
        comm = factory.getCommunicator(player);
        contract = factory.createNewItem(AnimalContractsMod.getContractTemplateId());
        player.getInventory().insertItem(contract);
    }

    @Test
    void testSendQuestion() {
        new AnimalContractManagementQuestion(player, contract).sendQuestion();
        assertFalse(comm.lastBmlContent.isEmpty());
        assertNotEquals(FakeCommunicator.empty, factory.getCommunicator(player).lastBmlContent);
    }

    @Test
    void testSendQuestionEmptyContractMessage() {
        new AnimalContractManagementQuestion(player, contract).sendQuestion();
        assertTrue(comm.lastBmlContent.contains("No creatures are assigned"));
    }

    @Test
    void testSendQuestionCorrectName() throws ContractFullException {
        Creature creature = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(creature);

        for (int i = 0; i < 10; i++) {
            String name = Offspring.getRandomGenericName();
            creature.setName(name);
            new AnimalContractManagementQuestion(player, contract).sendQuestion();
            assertTrue(comm.lastBmlContent.contains(StringUtilities.raiseFirstLetter(name)),
                    String.format("Expected %s\nGot: %s", name, comm.lastBmlContent));
        }
    }

    @Test
    void testSendQuestionCorrectTemplateName() throws ContractFullException {
        Creature horse = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        Creature spider = factory.createNewCreature(CreatureTemplateIds.SPIDER_CID);
        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(horse);
        animalContract.addCreature(spider);
        new AnimalContractManagementQuestion(player, contract).sendQuestion();

        assertTrue(comm.lastBmlContent.contains(horse.getTemplate().getName()));
        assertTrue(comm.lastBmlContent.contains(spider.getTemplate().getName()));
    }

    @Test
    void testSendQuestionCorrectColour() throws ContractFullException {
        Creature creature = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(creature);

        for (WurmObjectsFactory.AnimalTraitColours colour : WurmObjectsFactory.AnimalTraitColours.values()) {
            creature.getStatus().setTraitBit(colour.getNumber(), true);
            new AnimalContractManagementQuestion(player, contract).sendQuestion();
            assertTrue(comm.lastBmlContent.contains(colour.getName()),
                    String.format("Expected %s\nGot: %s", colour.getName(), comm.lastBmlContent));
            creature.getStatus().setTraitBit(colour.getNumber(), false);
        }
    }

    // TODO - Trait strings.

    @Test
    void testCorrectTraitCount() throws ContractFullException {
        Creature horse1 = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        Creature horse2 = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        CreatureStatus status = horse2.getStatus();
        status.setTraitBit(2, true);
        status.setTraitBit(3, true);
        status.setTraitBit(4, true);

        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(horse1);
        animalContract.addCreature(horse2);
        new AnimalContractManagementQuestion(player, contract).sendQuestion();

        Matcher matcher = traits.matcher(comm.lastBmlContent);

        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals(Integer.toString(0), matcher.group(1));
        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals(Integer.toString(3), matcher.group(1));
    }

    @Test
    void testCorrectTraitString() throws ContractFullException {
        Creature horse1 = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        Creature horse2 = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        CreatureStatus status = horse2.getStatus();
        status.setTraitBit(2, true);
        status.setTraitBit(3, true);
        status.setTraitBit(4, true);

        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(horse1);
        animalContract.addCreature(horse2);
        new AnimalContractManagementQuestion(player, contract).sendQuestion();

        Matcher matcher = traits.matcher(comm.lastBmlContent);

        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals("None", matcher.group(2));
        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals(Joiner.on("\n").join(Traits.getTraitString(2), Traits.getTraitString(3), Traits.getTraitString(4)), matcher.group(2));
    }

    @Test
    void testCorrectTraitCountForLowSkill() throws ContractFullException, NoSuchFieldException, IllegalAccessException {
        ReflectionUtil.setPrivateField(null, AnimalContractsMod.class.getDeclaredField("useAnimalHusbandryForTraits"), true);
        Creature horse = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        CreatureStatus status = horse.getStatus();
        status.setTraitBit(2, true);
        status.setTraitBit(3, true);
        status.setTraitBit(32, true);
        player.getSkills().learn(SkillList.BREEDING, 1);

        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(horse);
        new AnimalContractManagementQuestion(player, contract).sendQuestion();

        Matcher matcher = traits.matcher(comm.lastBmlContent);

        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals("???", matcher.group(1));
    }

    @Test
    void testCorrectTraitCountForMediumSkill() throws ContractFullException, NoSuchFieldException, IllegalAccessException {
        ReflectionUtil.setPrivateField(null, AnimalContractsMod.class.getDeclaredField("useAnimalHusbandryForTraits"), true);
        Creature horse = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        CreatureStatus status = horse.getStatus();
        status.setTraitBit(2, true);
        status.setTraitBit(3, true);
        status.setTraitBit(32, true);
        player.getSkills().learn(SkillList.BREEDING, 24);

        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(horse);
        new AnimalContractManagementQuestion(player, contract).sendQuestion();

        Matcher matcher = traits.matcher(comm.lastBmlContent);

        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals("2?", matcher.group(1));
    }

    @Test
    void testCorrectTraitStringForLowSkill() throws ContractFullException, NoSuchFieldException, IllegalAccessException {
        ReflectionUtil.setPrivateField(null, AnimalContractsMod.class.getDeclaredField("useAnimalHusbandryForTraits"), true);
        Creature horse = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        CreatureStatus status = horse.getStatus();
        status.setTraitBit(2, true);
        status.setTraitBit(3, true);
        status.setTraitBit(35, true);
        player.getSkills().learn(SkillList.BREEDING, 1);

        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(horse);
        new AnimalContractManagementQuestion(player, contract).sendQuestion();

        Matcher matcher = traits.matcher(comm.lastBmlContent);

        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals("You do not have enough skill to see traits.", matcher.group(2));
    }

    @Test
    void testCorrectTraitStringForMediumSkill() throws ContractFullException, NoSuchFieldException, IllegalAccessException {
        ReflectionUtil.setPrivateField(null, AnimalContractsMod.class.getDeclaredField("useAnimalHusbandryForTraits"), true);
        Creature horse = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
        CreatureStatus status = horse.getStatus();
        status.setTraitBit(2, true);
        status.setTraitBit(3, true);
        status.setTraitBit(35, true);
        player.getSkills().learn(SkillList.BREEDING, 24);

        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        animalContract.addCreature(horse);
        new AnimalContractManagementQuestion(player, contract).sendQuestion();

        Matcher matcher = traits.matcher(comm.lastBmlContent);

        assertTrue(matcher.find(), comm.lastBmlContent);
        assertEquals(Joiner.on("\n").join(Traits.getTraitString(2), Traits.getTraitString(3), "You do not have enough skill to see any more."), matcher.group(2));
    }

    @Test
    void testRemoveCreatures() throws ContractFullException {
        AnimalContract animalContract = AnimalContract.getAnimalContract(contract);
        List<Creature> toRemove = new ArrayList<>();
        List<Creature> toKeep = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Creature creature = factory.createNewCreature(CreatureTemplateIds.HORSE_CID);
            animalContract.addCreature(creature);

            if (i % 2 == 0)
                toRemove.add(creature);
            else
                toKeep.add(creature);
        }

        Properties properties = new Properties();
        properties.setProperty("update", "true");
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0)
                properties.setProperty(String.valueOf(i), "true");
            else
                properties.setProperty(String.valueOf(i), "false");
        }

        new AnimalContractManagementQuestion(player, contract).answer(properties);
        animalContract = AnimalContract.getAnimalContract(contract);

        assertEquals(toKeep.size(), animalContract.getAllCreatures().size());

        for (Creature creature : toRemove)
            assertFalse(animalContract.hasCreature(creature));

        for (Creature creature : toKeep)
            assertTrue(animalContract.hasCreature(creature));
    }
}
