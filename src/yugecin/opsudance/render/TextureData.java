// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.render;

import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.Texture;

import itdelatrisu.opsu.GameImage;

public class TextureData
{
	public final Image image;
	public int id;
	public float width, height, width2, height2, txtw, txth;

	public TextureData(GameImage image)
	{
		this(image.getImage());
	}

	public TextureData(Image image)
	{
		this.image = image;
		this.width = image.getWidth();
		this.height = image.getHeight();
		this.width2 = this.width / 2f;
		this.height2 = this.height / 2f;
		Texture text = image.getTexture();
		this.id = text.getTextureID();
		this.txtw = text.getWidth();
		this.txth = text.getHeight();
	}

	public void useScale(float scale)
	{
		this.width = image.getWidth() * scale;
		this.height = image.getHeight() * scale;
		this.width2 = this.width / 2f;
		this.height2 = this.height / 2f;
	}
}
