package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.contracts.animals.AnimalContract;
import mod.wurmunlimited.contracts.animals.AnimalContractsMod;
import mod.wurmunlimited.contracts.animals.ContractFullException;
import org.gotti.wurmunlimited.modsupport.IdFactory;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class AssignAnimalAction implements ModAction, BehaviourProvider, ActionPerformer {
    private final short actionId;
    private final int contractTemplateId;

    public AssignAnimalAction(int contractTemplateId) {
        this.contractTemplateId = contractTemplateId;
        actionId = (short)ModActions.getNextActionId();
        ModActions.registerAction(new ActionEntry(actionId, "Assign", "assigning", ItemBehaviour.emptyIntArr, 10));
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        if (subject != null && target != null && subject.getTemplateId() == contractTemplateId
                    && subject.getOwnerId() == performer.getWurmId() && !target.isPlayer()) {
            AnimalContract contract = AnimalContract.getAnimalContract(subject);
            if (contract.hasCreature(target)) {
                return Collections.singletonList(new ActionEntry(actionId, "Unassign", "unassigning"));
            } else {
                return Collections.singletonList(new ActionEntry(actionId, "Assign", "assigning"));
            }
        }
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item subject, Creature target, short num, float counter) {
        if (num != actionId || subject == null || subject.getTemplateId() != AnimalContractsMod.getContractTemplateId() || target == null || !AnimalContract.isValidCreature(target))
            return false;
        AnimalContract contract = AnimalContract.getAnimalContract(subject);
        if (contract.hasCreature(target)) {
            contract.removeCreature(target);
            performer.getCommunicator().sendNormalServerMessage("You remove " + target.getName() + " from the contract.");
        } else {
            if (!Methods.isActionAllowed(target, Actions.LEAD)) {
                performer.getCommunicator().sendNormalServerMessage("You do not have permission to control that animal.");
            } else {
                try {
                    contract.addCreature(target);
                    performer.getCommunicator().sendNormalServerMessage("You add " + target.getName() + " to the contract.");
                } catch (ContractFullException e) {
                    performer.getCommunicator().sendNormalServerMessage("There would not be enough space to add that animal to the contract.");
                }
            }
        }
        return true;

    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
