package org.jurassicraft.server.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jurassicraft.server.entity.vehicle.VehicleEntity;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SideOnly(Side.CLIENT)
public class KeyBindingHandler {
    public static final KeyBinding MICRORAPTOR_DISMOUNT = new KeyBinding("key.microraptor_dismount", Keyboard.KEY_C, "JurassiCraft");
    public static final KeyBinding HELICOPTER_UP = new KeyBinding("key.vehicle_helicopter.up", Keyboard.KEY_SPACE, "JurassiCraft");
    public static final KeyBinding HELICOPTER_DOWN = new KeyBinding("key.vehicle_helicopter.down", Keyboard.KEY_LCONTROL, "JurassiCraft");

    private static IKeyConflictContext context = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            EntityPlayer player = Minecraft.getMinecraft().player;
            return player != null && player.getRidingEntity() instanceof VehicleEntity;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return true;
        }
    };

    public static List<KeyBinding> VEHICLE_KEY_BINDINGS = IntStream.range(0, 9)
            .mapToObj(i -> new KeyBinding("key.vehicle_" + i + ".switch", context, KeyModifier.CONTROL, Keyboard.KEY_1 + i, "JurassiCraft"))
            .collect(Collectors.toList());

    public static void init() {
        ClientRegistry.registerKeyBinding(MICRORAPTOR_DISMOUNT);
        ClientRegistry.registerKeyBinding(HELICOPTER_UP);
        ClientRegistry.registerKeyBinding(HELICOPTER_DOWN);
        VEHICLE_KEY_BINDINGS.forEach(ClientRegistry::registerKeyBinding);
    }
}