package me.purpleeast.mods.ppe_essentials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

abstract class PpeTeleportMenuBase extends ChestMenu {
    private final PpeTeleportMenuContent content;

    PpeTeleportMenuBase(int containerId, Inventory inventory, PpeTeleportMenuContent content) {
        super(MenuType.GENERIC_9x6, containerId, inventory, content.container(), 6);
        this.content = content;
    }

    static void open(ServerPlayer player, boolean here, int page) {
        var content = new PpeTeleportMenuContent(player, here, page);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new PpeTeleportMenu(containerId, inventory, content),
                content.title()));
    }

    final void handleClick(int slotId, boolean pickup, Player player) {
        if (player == content.owner() && (!pickup || slotId < 0 || slotId >= PpeTeleportMenuContent.MENU_SIZE
                || !content.click(slotId))) {
            broadcastFullState();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return false;
    }
}
