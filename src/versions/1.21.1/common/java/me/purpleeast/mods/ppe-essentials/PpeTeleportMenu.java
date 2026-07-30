package me.purpleeast.mods.ppe_essentials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class PpeTeleportMenu extends ChestMenu {
    private final PpeTeleportMenuContent content;

    private PpeTeleportMenu(int containerId, Inventory inventory, ServerPlayer owner, boolean here, int requestedPage) {
        this(containerId, inventory, new SimpleContainer(PpeTeleportMenuContent.MENU_SIZE), owner, here, requestedPage);
    }

    private PpeTeleportMenu(
            int containerId,
            Inventory inventory,
            SimpleContainer container,
            ServerPlayer owner,
            boolean here,
            int requestedPage
    ) {
        super(MenuType.GENERIC_9x6, containerId, inventory, container, 6);
        this.content = new PpeTeleportMenuContent(container, owner, here, requestedPage);
    }

    static void open(ServerPlayer player, boolean here, int requestedPage) {
        int page = PpeTeleportMenuContent.currentPage(player, requestedPage);
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new PpeTeleportMenu(containerId, inventory, player, here, page),
                PpeTeleportMenuContent.title(player, here, page)
        );
        player.openMenu(provider);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player != content.owner()) {
            return;
        }
        if (slotId < 0
                || slotId >= PpeTeleportMenuContent.MENU_SIZE
                || clickType != ClickType.PICKUP
                || !content.click(slotId)) {
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
