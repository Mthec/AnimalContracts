package mod.wurmunlimited.contracts.animals;

import com.wurmonline.server.creatures.Creature;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnimalContractModTests {
    private AnimalContractsObjectsFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AnimalContractsObjectsFactory();
        ActionEntryBuilder.init();
        ReflectionUtil.setPrivateField(null, AnimalContractsMod.class.getDeclaredField("protectedCreatures"), new HashSet<>());
    }

    @Test
    void testPollAgeAssignedAnimal() throws Throwable {
        AnimalContractsMod mod = new AnimalContractsMod();
        Creature creature = factory.createNewCreature();
        AnimalContractsMod.addProtectedCreature(creature);

        InvocationHandler handler = mod::pollAge;
        Method method = mock(Method.class);
        Object[] args = new Object[0];

        assertFalse((Boolean)handler.invoke(creature.getStatus(), method, args));
        verify(method, never()).invoke(creature.getStatus(), args);
    }

    @Test
    void testPollAgeUnassignedAnimal() throws Throwable {
        AnimalContractsMod mod = new AnimalContractsMod();
        Creature creature = factory.createNewCreature();

        InvocationHandler handler = mod::pollAge;
        Method method = mock(Method.class);
        Object[] args = new Object[0];

        assertNull(handler.invoke(creature.getStatus(), method, args));
        verify(method, times(1)).invoke(creature.getStatus(), args);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testProtectedCreaturesAddedOnServerLoad() throws ContractFullException, NoSuchFieldException, IllegalAccessException {
        AnimalContract contract = AnimalContract.getAnimalContract(factory.createNewItem(AnimalContractsMod.getContractTemplateId()));
        Creature creature = factory.createNewCreature();
        contract.addCreature(creature);
        ReflectionUtil.setPrivateField(null, AnimalContractsMod.class.getDeclaredField("protectedCreatures"), new HashSet<>());

        Field protectedCreatures = AnimalContractsMod.class.getDeclaredField("protectedCreatures");
        protectedCreatures.setAccessible(true);

        assertFalse(((Set<Creature>)protectedCreatures.get(null)).contains(creature));

        new AnimalContractsMod().onServerStarted();

        assertTrue(((Set<Creature>)protectedCreatures.get(null)).contains(creature));
    }
}
