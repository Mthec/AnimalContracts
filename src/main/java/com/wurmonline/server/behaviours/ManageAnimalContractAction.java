package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.AnimalContractManagementQuestion;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;

public class ManageAnimalContractAction implements ModAction, ActionPerformer, BehaviourProvider {
    private final int contractTemplateId;
    private final short actionId;
    private final ActionEntry actionEntry;

    public ManageAnimalContractAction(int contractTemplateId) {
        this.contractTemplateId = contractTemplateId;

        actionId = (short)ModActions.getNextActionId();

        actionEntry = new ActionEntryBuilder(actionId, "Manage", "managing", ItemBehaviour.emptyIntArr).build();
        ModActions.registerAction(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
        if (target.getTemplateId() == contractTemplateId)
            return Collections.singletonList(actionEntry);
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item subject, Item target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId && target.getTemplateId() == contractTemplateId) {
            new AnimalContractManagementQuestion(performer, target).sendQuestion();
            return true;
        }
        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
