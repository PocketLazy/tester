package com.pocketlazy.classify.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.pocketlazy.classify.abilities.AssassinAbilityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class AssassinArmorHider {

    // Call this in your plugin's onEnable() to activate the armor hider for Assassin cloak
    public static void register(Plugin plugin) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        manager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                // Get the entity being equipped
                int entityId = packet.getIntegers().read(0);
                Entity entity = null;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getEntityId() == entityId) {
                        entity = online;
                        break;
                    }
                }
                if (!(entity instanceof Player target)) return;

                // Only hide for cloaked assassins
                if (!AssassinAbilityManager.isCloaked(target)) return;

                // This block is REMOVED so that the cloaked player also gets armor hidden:
                // if (event.getPlayer().equals(target)) return;

                // Make a copy of the equipment list
                List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipmentList =
                        new ArrayList<>(packet.getSlotStackPairLists().read(0));

                // Replace armor and offhand with AIR
                for (int i = 0; i < equipmentList.size(); i++) {
                    EnumWrappers.ItemSlot slot = equipmentList.get(i).getFirst();
                    if (slot == EnumWrappers.ItemSlot.HEAD
                            || slot == EnumWrappers.ItemSlot.CHEST
                            || slot == EnumWrappers.ItemSlot.LEGS
                            || slot == EnumWrappers.ItemSlot.FEET
                            || slot == EnumWrappers.ItemSlot.OFFHAND) {
                        equipmentList.set(i, new Pair<>(slot, new ItemStack(Material.AIR)));
                    }
                }

                packet.getSlotStackPairLists().write(0, equipmentList);
            }
        });
    }
}