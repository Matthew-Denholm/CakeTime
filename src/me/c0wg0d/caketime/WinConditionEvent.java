package me.c0wg0d.caketime;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;


public class WinConditionEvent extends Event {
	
	Player player;
	Item item;
	int remaining;
	
	@EventHandler
	public void PlayerPickupItemEvent(Player player, Item item, int remaining) {
		this.player = player;
		this.item = item;
		this.remaining = remaining;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Item getItem() {
		return item;
	}
	
	public int getRemaining() {
		return remaining;
	}
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
