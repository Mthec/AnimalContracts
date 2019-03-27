package mod.wurmunlimited.contracts.animals;

import mod.wurmunlimited.WurmObjectsFactory;

public class AnimalContractsObjectsFactory extends WurmObjectsFactory {

    public AnimalContractsObjectsFactory() throws Exception {
        super();

        new AnimalContractsMod().onItemTemplatesCreated();
    }
}
