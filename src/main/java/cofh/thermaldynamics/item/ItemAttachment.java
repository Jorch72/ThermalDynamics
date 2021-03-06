package cofh.thermaldynamics.item;

import cofh.api.core.IInitializer;
import cofh.core.render.hitbox.CustomHitBox;
import cofh.core.render.hitbox.RenderHitbox;
import cofh.lib.util.helpers.BlockHelper;
import cofh.repack.codechicken.lib.raytracer.RayTracer;
import cofh.repack.codechicken.lib.vec.Cuboid6;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.TileTDBase;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;

public abstract class ItemAttachment extends Item implements IInitializer {

	public ItemAttachment() {

		super();
		setHasSubtypes(true);
		this.setCreativeTab(ThermalDynamics.tabCommon);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {

		Attachment attachment = getAttachment(stack, player, world, x, y, z, side);

		if (attachment != null && attachment.addToTile()) {
			if (!player.capabilities.isCreativeMode) {
				stack.stackSize--;
			}
			return true;
		}

		return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
	}

	public Attachment getAttachment(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {

		Attachment attachment = null;

		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile instanceof TileTDBase) {
			int s = -1;
			MovingObjectPosition movingObjectPosition = RayTracer.retraceBlock(world, player, x, y, z);
			if (movingObjectPosition != null) {
				int subHit = movingObjectPosition.subHit;
				if (subHit < 6) {
					s = subHit;
				} else if (subHit < 12) {
					s = (subHit - 6);
				} else if (subHit == 13) {
					s = side;
				} else {
					s = ((subHit - 14) % 6);
				}
				if (s != -1) {
					attachment = getAttachment(s ^ 1, stack, (TileTDBase) tile);
				}
			}
		} else {
			tile = BlockHelper.getAdjacentTileEntity(world, x, y, z, side);
			if (tile instanceof TileTDBase) {
				attachment = getAttachment(side, stack, (TileTDBase) tile);
			}
		}
		return attachment;
	}

	public abstract Attachment getAttachment(int side, ItemStack stack, TileTDBase tile);

	@Override
	public boolean initialize() {

		MinecraftForge.EVENT_BUS.register(this);
		return true;
	}

	@Override
	public boolean postInit() {

		return true;
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onBlockHighlight(DrawBlockHighlightEvent event) {

		if (event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || event.player.getHeldItem() == null
				|| event.player.getHeldItem().getItem() != this) {
			return;
		}

		RayTracer.retraceBlock(event.player.worldObj, event.player, event.target.blockX, event.target.blockY, event.target.blockZ);

		Attachment attachment = getAttachment(event.player.getHeldItem(), event.player, event.player.getEntityWorld(), event.target.blockX,
				event.target.blockY, event.target.blockZ, event.target.sideHit);

		if (attachment == null || !attachment.canAddToTile(attachment.tile)) {
			return;
		}

		Cuboid6 c = attachment.getCuboid();
		c.max.sub(c.min);

		RenderHitbox.drawSelectionBox(event.player, event.target, event.partialTicks, new CustomHitBox(c.max.y, c.max.z, c.max.x,
				attachment.tile.x() + c.min.x, attachment.tile.y() + c.min.y, attachment.tile.z() + c.min.z));

		attachment.drawSelectionExtra(event.player, event.target, event.partialTicks);

		event.setCanceled(true);
	}

}
