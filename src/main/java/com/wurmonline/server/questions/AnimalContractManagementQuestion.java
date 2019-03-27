package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Traits;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.NoSuchSkillException;
import com.wurmonline.server.skills.Skill;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.contracts.animals.AnimalContract;
import mod.wurmunlimited.contracts.animals.AnimalContractsMod;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class AnimalContractManagementQuestion extends Question {
    private final AnimalContract contract;
    private final List<Creature> creatures;

    public AnimalContractManagementQuestion(Creature responder, Item contract) {
        super(responder, "Manage contract", "", QuestionTypes.MANAGEOBJECTLIST, contract.getWurmId());
        this.contract = AnimalContract.getAnimalContract(contract);
        creatures = this.contract.getAllCreatures();
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        String val = properties.getProperty("update");
        if (val != null && val.equals("true")) {
            for (int i = 0; i < creatures.size(); i++) {
                val = properties.getProperty(Integer.toString(i));
                if (val != null && val.equals("true")) {
                    contract.removeCreature(creatures.get(i));
                    getResponder().getCommunicator().sendNormalServerMessage("You remove " + creatures.get(i).getName() + " from the contract.");
                }
            }
        }
    }

    @Override
    public void sendQuestion() {
        AtomicInteger creatureId = new AtomicInteger(0);

        String bml = new BMLBuilder(id)
                             .header("Animal List")
                             .text("Hover over the number of traits to see the actual traits.")
                             .newLine()
                             .table(new String[] { "Name", "Type", "Colour", "Traits", "Remove?"}, creatures,
                                     (creature, b) -> {
                                                     String[] traitString = getTraitStringFor(creature);
                                                     String traitNumber = traitString[0];
                                                     String traitText = traitString[1];
                                              return b.label(creature.getName())
                                                      .label(creature.getTemplate().getName())
                                                      .label(creature.getColourName())
                                                      .text(traitNumber).hover(traitText)
                                                      .checkbox(Integer.toString(creatureId.getAndIncrement()));
                             })
                             .If(creatures.isEmpty(), b -> b.newLine().text("No creatures are assigned."))
                             .newLine()
                             .harray(b -> b.button("update", "Send"))
                             .build();


        getResponder().getCommunicator().sendBml(350, 400, true, true, bml, 200, 200, 200, title);
    }

    private String[] getTraitStringFor(Creature creature) {
        double knowledge = 100;
        if (AnimalContractsMod.useAnimalHusbandry()) {
            try {
                Skill breeding = getResponder().getSkills().getSkill(10085);
                knowledge = breeding.getKnowledge(0.0D);
                if (knowledge < 20.0D) {
                    return new String[] { "???", "You do not have enough skill to see traits." };
                }
                knowledge -= 20.0D;
            } catch (NoSuchSkillException ignored) {}
        }

        StringBuilder sb = new StringBuilder();
        String countString = "";
        int count = 0;
        for (int i = 0; i < 64; i++) {
            if (creature.hasTrait(i) && knowledge > i) {
                String traitString = Traits.getTraitString(i);
                if (!traitString.isEmpty()) {
                    sb.append(traitString).append("\n");
                    ++count;
                }
            } else if (knowledge <= i) {
                sb.append("You do not have enough skill to see any more.\n");
                countString = count + "?";
                break;
            }
        }
        if (sb.length() == 0)
            return new String[] { "0", "None" };

        if (countString.isEmpty())
            countString = Integer.toString(count);

        sb.deleteCharAt(sb.length() - 1);
        return new String[] { countString, sb.toString() };
    }
}
