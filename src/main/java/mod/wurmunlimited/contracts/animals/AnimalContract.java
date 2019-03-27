package mod.wurmunlimited.contracts.animals;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.InscriptionData;
import com.wurmonline.server.items.Item;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AnimalContract {
    private static final Logger logger = Logger.getLogger(AnimalContract.class.getName());
    private static final int MAX_INSCRIPTION_LENGTH = 500;
    private final Item contractItem;
    private List<Creature> creatures = new ArrayList<>();

    private AnimalContract(Item item) {
        assert isAnimalContract(item);

        contractItem = item;

        InscriptionData inscription = item.getInscription();
        if (inscription == null) {
            item.setInscription("", "");
        } else {
            String str = inscription.getInscription();
            if (!str.isEmpty()) {
                String[] lines = str.split("\n");

                for (String line : lines) {
                    try {
                        creatures.add(Creatures.getInstance().getCreature(Long.parseLong(line)));
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid creature id on animal contract - \"" + line + "\"");
                    } catch (NoSuchCreatureException ignored) {
                    }
                    // Creature probably died.
                }
            }
        }
    }

    public void addCreature(@NotNull Creature creature) throws ContractFullException {
        if (!isValidCreature(creature)) {
            return;
        }
        creatures.add(creature);
        try {
            saveContract();
        } catch (ContractFullException e) {
            creatures.remove(creature);
            throw e;
        }
        AnimalContractsMod.addProtectedCreature(creature);
    }

    public boolean hasCreature(Creature creature) {
        return creatures.contains(creature);
    }

    public void removeCreature(Creature creature) {
        creatures.remove(creature);
        try {
            saveContract();
        } catch (ContractFullException ignored) {}
        // ^ Shouldn't ever happen.
        AnimalContractsMod.removeProtectedCreature(creature);
    }

    private void saveContract() throws ContractFullException {
        StringBuilder inscription = new StringBuilder();
        for (Creature creature : creatures) {
            inscription.append(creature.getWurmId()).append("\n");
            if (inscription.length() > MAX_INSCRIPTION_LENGTH) {
                throw new ContractFullException("Contract already has too many values.");
            }
        }
        if (inscription.length() != 0)
            inscription.deleteCharAt(inscription.length() - 1);
        contractItem.setInscription(inscription.toString(), "");
    }

    public List<Creature> getAllCreatures() {
        return new ArrayList<>(creatures);
    }

    public static AnimalContract getAnimalContract(Item item) {
        return new AnimalContract(item);
    }

    public static boolean isAnimalContract(Item item) {
        return item.getTemplateId() == AnimalContractsMod.getContractTemplateId();
    }

    public static boolean isValidCreature(Creature creature) {
        return !creature.isPlayer() && !creature.isNpc();
    }
}
