package net.glasslauncher.hmifabric;

import net.minecraft.client.gui.screen.ScreenBase;
import net.minecraft.client.gui.widgets.Textbox;
import net.minecraft.client.render.TextRenderer;

public class GuiTextFieldHMI extends Textbox {

	public GuiTextFieldHMI(ScreenBase guiscreen, TextRenderer fontrenderer, int i, int j, int k, int l, String s) {
		super(guiscreen, fontrenderer, i, j, k, l, s);
		xPos = i;
        yPos = j;
        width = k;
        height = l;
	}

	public boolean hovered(int posX, int posY)
    {
        return enabled && posX >= xPos && posX < xPos + width && posY >= yPos && posY < yPos + height;
    }

    // onClick
	@Override
	public void mouseClicked(int posX, int posY, int eventButton) {
        super.mouseClicked(posX, posY, eventButton);
        if(this.selected && eventButton == 1) {
        	GuiOverlay.clearSearchBox();
        }
    }
	
	private final int xPos;
    private final int yPos;
    private final int width;
    private final int height;
}
