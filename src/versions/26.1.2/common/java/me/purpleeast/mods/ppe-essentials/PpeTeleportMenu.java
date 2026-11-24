package me.purpleeast.mods.ppe_essentials;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;

final class PpeTeleportMenu extends PpeTeleportMenuBase {
    PpeTeleportMenu(int containerId, Inventory inventory, PpeTeleportMenuContent content) {
        super(containerId, inventory, content);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        handleClick(slotId, input == ContainerInput.PICKUP, player);
    }
}
