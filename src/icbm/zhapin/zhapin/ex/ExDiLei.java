package icbm.zhapin.zhapin.ex;

import icbm.core.SheDing;
import icbm.core.base.MICBM;
import icbm.zhapin.baozha.bz.BzDiLei;
import icbm.zhapin.muoxing.jiqi.MDiLei;
import icbm.zhapin.zhapin.ZhaPin;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.RecipeHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ExDiLei extends ZhaPin
{
	public ExDiLei(String mingZi, int tier)
	{
		super(mingZi, tier);
		this.setYinXin(20);
		this.hasGrenade = false;
		this.hasMinecart = false;
		this.hasMissile = false;
	}

	@Override
	public void onYinZha(World worldObj, Vector3 position, int fuseTicks)
	{

	}

	@Override
	public void init()
	{
		RecipeHelper.addRecipe(new ShapedOreRecipe(this.getItemStack(), new Object[] { "S", "L", "R", 'S', ZhaPin.qunDan.getItemStack(), 'L', ZhaPin.la.getItemStack(), 'R', ZhaPin.tui.getItemStack() }), this.getUnlocalizedName(), SheDing.CONFIGURATION, true);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public MICBM getBlockModel()
	{
		return MDiLei.INSTANCE;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public ResourceLocation getBlockResource()
	{
		return MDiLei.TEXTURE;
	}

	@Override
	public void doCreateExplosion(World world, double x, double y, double z, Entity entity)
	{
		new BzDiLei(world, entity, x, y, z, 5).explode();
	}

}