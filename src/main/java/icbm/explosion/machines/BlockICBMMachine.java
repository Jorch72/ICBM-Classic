package icbm.explosion.machines;

import icbm.api.ITier;
import icbm.core.prefab.BlockICBM;
import icbm.explosion.ICBMExplosion;
import icbm.explosion.render.tile.BlockRenderHandler;

import java.util.List;
import java.util.Random;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.api.UniversalElectricity;
import calclavia.lib.multiblock.link.IMultiBlock;
import calclavia.lib.prefab.tile.IRedstoneReceptor;
import calclavia.lib.prefab.tile.IRotatable;
import calclavia.lib.utility.LanguageUtility;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockICBMMachine extends BlockICBM
{
    public enum MachineData
    {
        LauncherBase(TileLauncherBase.class),
        LauncherScreen(TileEntityLauncherScreen.class),
        LauncherFrame(TileEntitySupportFrame.class),
        RadarStation(TileEntityRadarStation.class),
        EmpTower(TileEMPTower.class),
        CruiseLauncher(TileEntityCruiseLauncher.class),
        MissileCoordinator(TileMissileCoordinator.class);

        public Class<? extends TileEntity> tileEntity;

        MachineData(Class<? extends TileEntity> tileEntity)
        {
            this.tileEntity = tileEntity;
        }

        public static MachineData get(int id)
        {
            if (id < MachineData.values().length && id >= 0)
            {
                return MachineData.values()[id];
            }

            return null;
        }
    }

    public BlockICBMMachine(int id)
    {
        super(id, "machine", UniversalElectricity.machine);
    }

    /** Can this block provide power. Only wire currently seems to have this change based on its
     * state. */
    @Override
    public boolean canProvidePower()
    {
        return true;
    }

    @Override
    public void onBlockAdded(World par1World, int x, int y, int z)
    {
        this.isBeingPowered(par1World, x, y, z);
    }

    /** Called when the block is placed in the world. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase par5EntityLiving, ItemStack itemStack)
    {
        int angle = MathHelper.floor_double((par5EntityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;

        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (tileEntity instanceof IRotatable)
        {
            IRotatable rotatableEntity = ((IRotatable) tileEntity);

            switch (angle)
            {
                case 0:
                    rotatableEntity.setDirection(ForgeDirection.getOrientation(3));
                    break;
                case 1:
                    rotatableEntity.setDirection(ForgeDirection.getOrientation(4));
                    break;
                case 2:
                    rotatableEntity.setDirection(ForgeDirection.getOrientation(2));
                    break;
                case 3:
                    rotatableEntity.setDirection(ForgeDirection.getOrientation(5));
                    break;
            }
        }

        if (tileEntity instanceof IMultiBlock)
        {
            DarkCore.multiBlock.createMultiBlockStructure((IMultiBlock) tileEntity);
        }
    }

    public static boolean canBePlacedAt(World world, int x, int y, int z, int metadata, int direction)
    {
        switch (metadata)
        {
            default:
            {
                return world.getBlockMaterial(x, y - 1, z).isSolid();
            }
            case 0:
            {
                // Launcher Base
                if (direction == 0 || direction == 2)
                {
                    return world.getBlockId(x, y, z) == 0 &&
                    // Left
                            world.getBlockId(x + 1, y, z) == 0 && world.getBlockId(x + 1, y + 1, z) == 0 && world.getBlockId(x + 1, y + 2, z) == 0 &&
                            // Right
                            world.getBlockId(x - 1, y, z) == 0 && world.getBlockId(x - 1, y + 1, z) == 0 && world.getBlockId(x - 1, y + 2, z) == 0;
                }
                else if (direction == 1 || direction == 3)
                {
                    return world.getBlockId(x, y, z) == 0 &&
                    // Front
                            world.getBlockId(x, y, z + 1) == 0 && world.getBlockId(x, y + 1, z + 1) == 0 && world.getBlockId(x, y + 2, z + 1) == 0 &&
                            // Back
                            world.getBlockId(x, y, z - 1) == 0 && world.getBlockId(x, y + 1, z - 1) == 0 && world.getBlockId(x, y + 2, z - 1) == 0;
                }
            }
            case 2:
            {
                // Launcher Frame
                return world.getBlockMaterial(x, y - 1, z).isSolid() && world.getBlockId(x, y, z) == 0 && world.getBlockId(x, y + 1, z) == 0 && world.getBlockId(x, y + 2, z) == 0;
            }
            case 4:
            {
                return world.getBlockId(x, y, z) == 0 && world.getBlockId(x, y + 1, z) == 0;
            }
        }
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z)
    {
        int direction = 0;
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (tileEntity instanceof IRotatable)
        {
            direction = ((IRotatable) tileEntity).getDirection().ordinal();
        }

        return canBePlacedAt(world, x, y, z, world.getBlockMetadata(x, y, z), direction);
    }

    @Override
    public void onNeighborBlockChange(World par1World, int x, int y, int z, int par5)
    {
        this.isBeingPowered(par1World, x, y, z);
    }

    /** Called when the block is right clicked by the player */
    @Override
    public boolean onMachineActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (player.inventory.getCurrentItem() != null)
        {
            if (player.inventory.getCurrentItem().itemID == ICBMExplosion.itemLaserDesignator.itemID)
            {
                return false;
            }
            else if (player.inventory.getCurrentItem().itemID == ICBMExplosion.itemRadarGun.itemID)
            {
                return false;
            }
        }

        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (tileEntity != null)
        {
            if (tileEntity instanceof IBlockActivated)
            {
                return ((IBlockActivated) tileEntity).onActivated(player);
            }
        }

        return false;
    }

    @Override
    public boolean onUseWrench(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        return this.onMachineActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
    }

    /** Checks of this block is being powered by redstone */
    public void isBeingPowered(World world, int x, int y, int z)
    {
        int metadata = world.getBlockMetadata(x, y, z);

        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (tileEntity instanceof IRedstoneReceptor)
        {
            if (world.isBlockIndirectlyGettingPowered(x, y, z))
            {
                // Send signal to tile entity
                ((IRedstoneReceptor) tileEntity).onPowerOn();
            }
            else
            {
                ((IRedstoneReceptor) tileEntity).onPowerOff();
            }
        }
    }

    /** If this block doesn't render as an ordinary block it will return False (examples: signs,
     * buttons, stairs, etc) */
    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, int par5, int par6)
    {
        int metadata = world.getBlockMetadata(x, y, z);
        Random random = new Random();

        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (tileEntity != null)
        {

            // Drops the machine
            int itemMetadata = 0;

            if (tileEntity instanceof ITier)
            {
                itemMetadata = ((ITier) tileEntity).getTier() + metadata * 3;
            }
            else
            {
                itemMetadata = 9 + metadata - 3;
            }

            this.dropBlockAsItem_do(world, x, y, z, new ItemStack(ICBMExplosion.blockMachine, 1, itemMetadata));

            if (tileEntity instanceof IMultiBlock)
            {
                DarkCore.multiBlock.destroyMultiBlockStructure((IMultiBlock) tileEntity);
            }
        }

        super.breakBlock(world, x, y, z, par5, par6);
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata)
    {
        if (MachineData.get(metadata) != null)
        {
            try
            {
                return MachineData.get(metadata).tileEntity.newInstance();
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    /** Returns the quantity of items to drop on block destruction. */
    @Override
    public int quantityDropped(Random par1Random)
    {
        return 0;
    }

    /** The type of render function that is called for this block */
    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType()
    {
        return BlockRenderHandler.ID;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public void getSubBlocks(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        for (int i = 0; i < MachineData.values().length + 6; i++)
        {
            par3List.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z)
    {
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return new ItemStack(ICBMExplosion.blockMachine, 1, getJiQiID(tileEntity));
    }

    @Override
    public int damageDropped(int metadata)
    {
        return metadata;
    }

    public static int getJiQiID(TileEntity tileEntity)
    {
        int itemMetadata = 0;

        if (tileEntity != null)
        {
            int metadata = tileEntity.getBlockMetadata();

            if (tileEntity instanceof ITier)
            {
                itemMetadata = ((ITier) tileEntity).getTier() + metadata * 3;
            }
            else
            {
                itemMetadata = 9 + metadata - 3;
            }
        }

        return itemMetadata;
    }

    public static String getJiQiMing(TileEntity tileEntity)
    {
        return LanguageUtility.getLocal("icbm.machine." + getJiQiID(tileEntity) + ".name");
    }
}
