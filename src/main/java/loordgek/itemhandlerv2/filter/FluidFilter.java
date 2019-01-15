package loordgek.itemhandlerv2.filter;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

public class FluidFilter implements IStackFilter {
    private final FluidStack fluidStackToTest;
    private final MatchingStrategy matchingStrategy;

    public FluidFilter(FluidStack fluidStackToTest, MatchingStrategy matchingStrategy) {
        this.fluidStackToTest = fluidStackToTest;
        this.matchingStrategy = matchingStrategy;
    }

    @Override
    public boolean test(ItemStack stack) {
        FluidStack fluidStack = FluidUtil.getFluidContained(stack);
        if (fluidStack != null) {
            switch (this.matchingStrategy) {
                case EXCEEDED:
                    return fluidStack.isFluidEqual(fluidStackToTest) && fluidStack.amount <= fluidStackToTest.amount;

                case EXACT:
                    return fluidStack.isFluidStackIdentical(fluidStackToTest);

                default: // Should be impossible
                    return false;
            }
        }
        return false;
    }

    public enum MatchingStrategy {
        /**
         * EXACT matching indicates that it will try matching exact amount of fluid.
         * Example: if the FluidIngredient asks for 500 mB water, a vanilla water bucket
         * will not be matched, as it holds 1000 mB water, 1000 != 500.
         */
        EXACT,

        /**
         * EXCEEDED matching is the default matching strategy, when not specified.
         * It will try matching those containers that can drain the specified amount of
         * fluid out. Example: if the FluidIngredient asks for 500 mB water, a vanilla bucket
         * will be matched.
         */
        EXCEEDED,
    }
}
