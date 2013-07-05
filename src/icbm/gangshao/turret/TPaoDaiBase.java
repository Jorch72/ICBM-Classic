package icbm.gangshao.turret;

import icbm.core.ZhuYaoBase;
import icbm.gangshao.ISentry;
import icbm.gangshao.ZhuYaoGangShao;
import icbm.gangshao.damage.EntityTileDamagable;
import icbm.gangshao.damage.IHealthTile;
import icbm.gangshao.platform.TPaoTaiZhan;
import icbm.gangshao.task.LookHelper;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.block.IVoltage;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.network.IPacketReceiver;
import universalelectricity.prefab.network.PacketManager;
import universalelectricity.prefab.tile.TileEntityAdvanced;
import calclavia.lib.CalculationHelper;
import calclavia.lib.render.ITagRender;

import com.google.common.io.ByteArrayDataInput;

/**
 * Turret Base Class Class that handles all the basic movement, and block based updates of a turret.
 * 
 * @author Calclavia, Rseifert
 */
public abstract class TPaoDaiBase extends TileEntityAdvanced implements IPacketReceiver, ITagRender, IVoltage, ISentry, IHealthTile
{
	/** OFFSET OF BARREL ROTATION */
	public final float rotationTranslation = 0.0175f;
	/** MAX UPWARD PITCH ANGLE OF THE SENTRY BARREL */
	public static final float MAX_PITCH = 30f;
	/** MAX DOWNWARD PITCH ANGLE OF THE SENTRY BARREL */
	public static final float MIN_PITCH = -30f;
	/** SPAWN DIRECTION OF SENTRY */
	private ForgeDirection platformDirection = ForgeDirection.DOWN;
	/** TURRET AIM & ROTATION HELPER */
	public LookHelper lookHelper = new LookHelper(this);
	/** SHOULD SPEED UP ROATION */
	protected boolean speedUpRotation = false;
	/** CURRENT HP OF THE SENTRY */
	public int health = -1;
	/** DEFUALT FIRING DELAY OF SENTRY */
	public int baseFiringDelay = 10;
	/** TICKS SINCE LAST GUN ACTIVATION */
	public int tickSinceFired = 0;
	/** LOWEST FIRING DELAY */
	public int minFiringDelay = 5;
	/** ENTITY THAT TAKES DAMAGE FOR THE SENTRY */
	private EntityTileDamagable damageEntity;
	/** TARGET ROATION ANGLES */
	public float wantedRotationYaw, wantedRotationPitch = 0;
	/** CURRENT ROATION ANGLES */
	public float currentRotationYaw, currentRotationPitch = 0;
	/**
	 * Amount of time since the last rotational movement.
	 */
	public int lastRotateTick = 0;

	public EntityTileDamagable entityDamagable;

	/** PACKET TYPES THIS TILE RECEIVES */
	public static enum TurretPacketType
	{
		ROTATION, DESCRIPTION, SHOT, STATS, MOUNT;
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();

		if (this.tickSinceFired > 0)
		{
			this.tickSinceFired--;
		}

		if (!this.worldObj.isRemote)
		{
			// SPAWN DAMAGE ENTITY IF ALIVE
			if (!this.isInvul() && this.getDamageEntity() == null && this.getHealth() > 0)
			{
				this.setDamageEntity(new EntityTileDamagable(this));
				this.worldObj.spawnEntityInWorld(this.getDamageEntity());
			}
		}
	}

	/*
	 * ----------------------- PACKET DATA AND SAVING --------------------------------------
	 */
	@Override
	public void handlePacketData(INetworkManager network, int packetType, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
	{
		try
		{
			this.onReceivePacket(dataStream.readInt(), player, dataStream);
		}
		catch (Exception e)
		{
			ZhuYaoBase.LOGGER.severe(MessageFormat.format("Packet receiving failed: {0}", this.getClass().getSimpleName()));
			e.printStackTrace();
		}
	}

	/**
	 * Inherit this function to receive packets. Make sure this function is supered.
	 * 
	 * @throws IOException
	 */
	public void onReceivePacket(int packetID, EntityPlayer player, ByteArrayDataInput dataStream) throws IOException
	{
		if (packetID == TurretPacketType.ROTATION.ordinal())
		{
			this.setRotation(dataStream.readFloat(), dataStream.readFloat());
		}
		else if (packetID == TurretPacketType.DESCRIPTION.ordinal())
		{
			short size = dataStream.readShort();

			if (size > 0)
			{
				byte[] byteCode = new byte[size];
				dataStream.readFully(byteCode);
				this.readFromNBT(CompressedStreamTools.decompress(byteCode));
			}
		}
		else if (packetID == TurretPacketType.STATS.ordinal())
		{
			this.health = dataStream.readInt();
		}
		else if (packetID == TurretPacketType.MOUNT.ordinal())
		{
			this.mount(player);
		}
	}

	public Packet getStatsPacket()
	{
		return PacketManager.getPacket(ZhuYaoGangShao.CHANNEL, this, TurretPacketType.STATS.ordinal(), this.health);
	}

	public Packet getRotationPacket()
	{
		return PacketManager.getPacket(ZhuYaoGangShao.CHANNEL, this, TurretPacketType.ROTATION.ordinal(), this.wantedRotationYaw, this.wantedRotationPitch);
	}

	@Override
	public Packet getDescriptionPacket()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		writeToNBT(nbt);
		return PacketManager.getPacket(ZhuYaoGangShao.CHANNEL, this, TurretPacketType.DESCRIPTION.ordinal(), nbt);
	}

	/** Writes a tile entity to NBT. */
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setFloat("yaw", this.wantedRotationYaw);
		nbt.setFloat("pitch", this.wantedRotationPitch);
		nbt.setFloat("cYaw", this.currentRotationYaw);
		nbt.setFloat("cPitch", this.currentRotationPitch);
		nbt.setInteger("dir", this.platformDirection.ordinal());
		nbt.setInteger("health", this.getHealth());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.wantedRotationYaw = nbt.getFloat("yaw");
		this.wantedRotationPitch = nbt.getFloat("pitch");
		this.currentRotationYaw = nbt.getFloat("cYaw");
		this.currentRotationPitch = nbt.getFloat("cPitch");
		this.platformDirection = ForgeDirection.getOrientation(nbt.getInteger("dir"));

		if (nbt.hasKey("health"))
		{
			this.health = nbt.getInteger("health");
		}
	}

	/*
	 * -------------------------- GENERIC SENTRY CODE --------------------------------------
	 */

	/** Energy consumed each time the weapon activates */
	public abstract double getFiringRequest();

	/** is this sentry currently operating */
	public boolean isRunning()
	{
		return this.getPlatform() != null && this.getPlatform().isRunning() && this.isAlive();
	}

	/** get the turrets control platform */
	@Override
	public TPaoTaiZhan getPlatform()
	{
		TileEntity tileEntity = this.worldObj.getBlockTileEntity(this.xCoord + this.platformDirection.offsetX, this.yCoord + this.platformDirection.offsetY, this.zCoord + this.platformDirection.offsetZ);

		if (tileEntity instanceof TPaoTaiZhan)
		{
			return (TPaoTaiZhan) tileEntity;
		}
		else
		{
			return null;
		}

	}

	/**
	 * Removes the sentry when called and optional creates a small local explosion
	 * 
	 * @param doExplosion - True too create a small local explosion
	 */
	public void destroy(boolean doExplosion)
	{
		if (doExplosion)
		{
			this.worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, 0);

			if (this.isAlive())
			{
				this.worldObj.createExplosion(null, this.xCoord, this.yCoord, this.zCoord, 2f, true);
			}
		}
		else if (!this.worldObj.isRemote)
		{
			this.getBlockType().dropBlockAsItem(this.worldObj, this.xCoord, this.yCoord, this.zCoord, this.getBlockMetadata(), 0);
			this.worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, 0);
		}
	}

	/** Gets the number of barrels this sentry has */
	public int getBarrels()
	{
		return 1;
	}

	/** Sets the deploy or connection direction of the sentry */
	public void setDeploySide(ForgeDirection side)
	{
		this.platformDirection = side.getOpposite();
	}

	@Override
	public float addInformation(HashMap<String, Integer> map, EntityPlayer player)
	{
		map.put(this.getName(), 0x88FF88);
		return 1;
	}

	@Override
	public String getName()
	{
		return new ItemStack(this.getBlockType(), 1, this.getBlockMetadata()).getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth();
	}

	/**
	 * Performs a ray trace for the distance specified and using the partial tick time. Args:
	 * distance, partialTickTime
	 */
	public MovingObjectPosition rayTrace(double distance)
	{
		return CalculationHelper.doCustomRayTrace(this.worldObj, this.getMuzzle(), this.wantedRotationYaw / this.rotationTranslation, this.wantedRotationPitch / this.rotationTranslation, true, distance);
	}

	@Override
	public Vector3 getMuzzle()
	{
		return new Vector3(this).add(0.5).add(Vector3.multiply(LookHelper.getDeltaPositionFromRotation(this.currentRotationYaw, this.currentRotationPitch), 1));
	}

	@Override
	public void onWeaponActivated()
	{
		this.tickSinceFired += this.baseFiringDelay;
	}

	public void mount(EntityPlayer entityPlayer)
	{

	}

	/*
	 * ------------------------------- HEALTH & DAMAGE CODE--------------------------------------
	 */
	/** Max Health this sentry has */
	@Override
	public abstract int getMaxHealth();

	/** Is the sentry immune to being attacked */
	public boolean isInvul()
	{
		return false;
	}

	@Override
	public int getHealth()
	{
		if (this.health == -1)
		{
			this.health = this.getMaxHealth();
		}
		return this.health;
	}

	@Override
	public void setHealth(int i, boolean increase)
	{
		if (increase)
		{
			i += this.health;
		}

		this.health = Math.min(Math.max(i, 0), this.getMaxHealth());

		if (!this.worldObj.isRemote)
		{
			PacketManager.sendPacketToClients(this.getStatsPacket(), this.worldObj, new Vector3(this), 100);
		}
	}

	@Override
	public boolean isAlive()
	{
		return this.getHealth() > 0 || this.isInvul();
	}

	@Override
	public boolean onDamageTaken(DamageSource source, int amount)
	{
		if (this.isInvul())
		{
			return false;
		}
		else if (source != null && source.equals(DamageSource.onFire))
		{
			return true;
		}
		else
		{
			this.health -= amount;

			if (this.health <= 0)
			{
				this.destroy(true);
			}
			else
			{
				PacketManager.sendPacketToClients(this.getStatsPacket(), this.worldObj, new Vector3(this), 100);
			}

			return true;
		}
	}

	/** Entity that takes damage for the sentry */
	public EntityTileDamagable getDamageEntity()
	{
		return damageEntity;
	}

	/** Sets the entity that takes damage for this sentry */
	public void setDamageEntity(EntityTileDamagable damageEntity)
	{
		this.damageEntity = damageEntity;
	}

	/*
	 * ----------------------------- ROTATION CODE --------------------------------------
	 */
	@Override
	public void setRotation(float yaw, float pitch)
	{
		this.wantedRotationYaw = MathHelper.wrapAngleTo180_float(yaw);
		this.wantedRotationPitch = MathHelper.wrapAngleTo180_float(pitch);
	}

	public void rotateTo(float wantedRotationYaw, float wantedRotationPitch)
	{
		if (!this.worldObj.isRemote)
		{
			if (this.lastRotateTick > 0 && (this.wantedRotationYaw != wantedRotationYaw || this.wantedRotationPitch != wantedRotationPitch))
			{
				this.setRotation(wantedRotationYaw, wantedRotationPitch);
				this.lastRotateTick = 0;

				if (!this.worldObj.isRemote)
				{
					PacketManager.sendPacketToClients(this.getRotationPacket(), this.worldObj, new Vector3(this), 50);
				}
			}
		}
	}

	public void cancelRotation()
	{
		this.setRotation(this.currentRotationYaw, this.currentRotationPitch);
	}

	/*
	 * ----------------------------- CLIENT SIDE --------------------------------------
	 */
	public void drawParticleStreamTo(Vector3 endPosition)
	{
		if (this.worldObj.isRemote)
		{
			Vector3 startPosition = this.getMuzzle();
			Vector3 direction = LookHelper.getDeltaPositionFromRotation(this.currentRotationYaw, this.currentRotationPitch);
			double xoffset = 0;
			double yoffset = 0;
			double zoffset = 0;
			Vector3 horzdir = direction.normalize();
			horzdir.y = 0;
			horzdir = horzdir.normalize();
			double cx = startPosition.x + direction.x * xoffset - direction.y * horzdir.x * yoffset - horzdir.z * zoffset;
			double cy = startPosition.y + direction.y * xoffset + (1 - Math.abs(direction.y)) * yoffset;
			double cz = startPosition.z + direction.x * xoffset - direction.y * horzdir.x * yoffset + horzdir.x * zoffset;
			double dx = endPosition.x - cx;
			double dy = endPosition.y - cy;
			double dz = endPosition.z - cz;
			double ratio = Math.sqrt(dx * dx + dy * dy + dz * dz);

			while (Math.abs(cx - endPosition.x) > Math.abs(dx / ratio))
			{
				this.worldObj.spawnParticle("townaura", cx, cy, cz, 0.0D, 0.0D, 0.0D);
				cx += dx * 0.1 / ratio;
				cy += dy * 0.1 / ratio;
				cz += dz * 0.1 / ratio;
			}
		}
	}

	/** creates client side effects when the sentry fires its weapon */
	public abstract void renderShot(Vector3 target);

	/** Sound this turrets plays each time its fires */
	public abstract void playFiringSound();

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}

}