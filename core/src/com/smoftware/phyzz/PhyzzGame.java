package com.smoftware.phyzz;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.smoftware.phyzz.screens.MainGameScreen;


//https://javadocmd.com/blog/libgdx-dynamic-textures-with-pixmap/

public class PhyzzGame extends Game {
	MainGameScreen mainGameScreen;
	SpriteBatch batch;
	Texture img;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("badlogic.jpg");

		mainGameScreen = new MainGameScreen();
		setScreen(mainGameScreen);
	}
/*
	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		//batch.draw(img, 0, 0);
		batch.end();
	}
	*/
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}
}
