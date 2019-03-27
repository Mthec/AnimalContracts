package mod.wurmunlimited.contracts.animals;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.AssignAnimalAction;
import com.wurmonline.server.behaviours.BehaviourList;
import com.wurmonline.server.behaviours.ManageAnimalContractAction;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureStatus;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.InscriptionData;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.IconConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AnimalContractsMod implements WurmServerMod, Configurable, PreInitable, ItemTemplatesCreatedListener, ServerStartedListener {
    private static final Logger logger = Logger.getLogger(AnimalContractsMod.class.getName());
    private static int contractTemplateId;
    private int contractPrice = 1000;
    private boolean updateTraders = false;
    private boolean contractsOnTraders = true;
    private static boolean useAnimalHusbandryForTraits = false;
    private static Set<Creature> protectedCreatures = new HashSet<>();
    private static Field statusHolder;

    public static int getContractTemplateId() {
        return contractTemplateId;
    }

    public static boolean useAnimalHusbandry() {
        return useAnimalHusbandryForTraits;
    }

    static void addProtectedCreature(Creature creature) {
        if (AnimalContract.isValidCreature(creature)) {
            protectedCreatures.add(creature);
        }
    }

    static void removeProtectedCreature(Creature creature) {
        protectedCreatures.remove(creature);
    }

    @Override
    public void configure(Properties properties) {
        try {
            contractPrice = Integer.parseInt(properties.getProperty("contract_price_in_irons"));
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for contract_price_in_irons, using default.");
        }
        try {
            updateTraders = Boolean.parseBoolean(properties.getProperty("update_traders"));
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for update_traders, using default.");
        }
        try {
            contractsOnTraders = Boolean.parseBoolean(properties.getProperty("contracts_on_traders"));
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for contracts_on_traders, using default.");
        }
        try {
            useAnimalHusbandryForTraits = Boolean.parseBoolean(properties.getProperty("use_animal_husbandry_for_traits"));
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for use_animal_husbandry_for_traits, using default.");
        }
    }

    @Override
    public void onItemTemplatesCreated() {
        try {
            contractTemplateId = new ItemTemplateBuilder("writ.animal")
                 .name("animal contract", "animal contracts", "A contract for preventing the aging of the assigned animals.")
                 .modelName("model.writ.merchant")
                 .imageNumber((short)IconConstants.ICON_TRADER_CONTRACT)
                 .weightGrams(0)
                 .dimensions(1, 10, 10)
                 .decayTime(Long.MAX_VALUE)
                 .material(ItemMaterials.MATERIAL_PAPER)
                 .itemTypes(new short[] {
                         ItemTypes.ITEM_TYPE_INDESTRUCTIBLE,
                         ItemTypes.ITEM_TYPE_NODROP,
                         ItemTypes.ITEM_TYPE_FULLPRICE,
                         ItemTypes.ITEM_TYPE_LOADED,
                         ItemTypes.ITEM_TYPE_NOT_MISSION,
                         ItemTypes.ITEM_TYPE_NOSELLBACK,
                         ItemTypes.ITEM_TYPE_CAN_HAVE_INSCRIPTION
                 })
                 .behaviourType(BehaviourList.itemBehaviour)
                 .value(contractPrice)
                 .difficulty(100.0F)
                 .build().getTemplateId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preInit() {
        ModActions.init();
    }

    @Override
    public void init() {
        HookManager manager = HookManager.getInstance();

        manager.registerHook("com.wurmonline.server.creatures.CreatureStatus",
                "pollAge",
                "(I)Z",
            () -> this::pollAge);
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(new AssignAnimalAction(contractTemplateId));
        ModActions.registerAction(new ManageAnimalContractAction(contractTemplateId));

        try {
            Field inscriptions = Items.class.getDeclaredField("itemInscriptionDataMap");
            inscriptions.setAccessible(true);
            //noinspection unchecked
            for (InscriptionData inscription : ((Map<Long, InscriptionData>)inscriptions.get(null)).values()) {
                try {
                    Item item = Items.getItem(inscription.getWurmId());
                    if (item.getTemplateId() == contractTemplateId) {
                        protectedCreatures.addAll(AnimalContract.getAnimalContract(item).getAllCreatures());
                    }
                } catch (NoSuchItemException ignored) {}
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.severe("Could not find Item InscriptionData.");
            throw new RuntimeException(e);
        }

        if (updateTraders) {
            if (contractsOnTraders) {
                for (Shop shop : Economy.getTraders()) {
                    Creature creature = Creatures.getInstance().getCreatureOrNull(shop.getWurmId());
                    if (!shop.isPersonal() && creature != null && creature.getInventory().getItems().stream().noneMatch(i -> i.getTemplateId() == contractTemplateId)) {
                        try {
                            creature.getInventory().insertItem(Creature.createItem(contractTemplateId, (float) (10 + Server.rand.nextInt(80))));
                            shop.setMerchantData(shop.getNumberOfItems() + 1);
                        } catch (Exception e) {
                            logger.info("Failed to create trader inventory items for shop, creature: " + creature.getName());
                        }
                    }
                }
            } else {
                for (Shop shop : Economy.getTraders()) {
                    Creature creature = Creatures.getInstance().getCreatureOrNull(shop.getWurmId());
                    if (!shop.isPersonal() && creature != null) {
                        creature.getInventory().getItems().stream().filter(i -> i.getTemplateId() == contractTemplateId).collect(Collectors.toList()).forEach(item -> {
                            Items.destroyItem(item.getWurmId());
                            shop.setMerchantData(shop.getNumberOfItems() - 1);
                        });
                    }
                }
            }
        }
    }

    Object pollAge(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (statusHolder == null) {
            try {
                statusHolder = CreatureStatus.class.getDeclaredField("statusHolder");
            } catch (NoSuchFieldException e) {
                logger.severe("Could not find statusHolder field on CreatureStatus.");
                throw new RuntimeException(e);
            }
            statusHolder.setAccessible(true);
        }

        if (protectedCreatures.contains((Creature)statusHolder.get(o))) {
            return false;
        }
        return method.invoke(o, args);
    }
}
