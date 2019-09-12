package com.smoftware.phyzz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

public class MainGameScreen extends GameScreen {

    private SpriteBatch batch;
    private ShaderProgram shader;
    private DrawablePixmap drawable;
    private Texture outline, color;
    private FileHandle vertexShader, fragmentShader;

    public MainGameScreen() {
        /* Some regular textures to draw on the scene. */
        outline = new Texture("smiley-outline.png");
        color = new Texture("smiley-color.png");

        /* I like to keep my shader programs as text files in the assets
         * directory rather than dealing with horrid Java string formatting. */
        vertexShader = Gdx.files.internal("vertex.glsl");
        fragmentShader = Gdx.files.internal("fragment.glsl");

        /* Bonus: you can set `pedantic = false` while tinkering with your
         * shaders. This will stop it from crashing if you have unused variables
         * and so on. */
        // ShaderProgram.pedantic = false;

        /* Construct our shader program. Spit out a log and quit if the shaders
         * fail to compile. */
        shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            Gdx.app.log("Shader", shader.getLog());
            Gdx.app.exit();
        }

        /* Construct a simple SpriteBatch using our shader program. */
        batch = new SpriteBatch();
        batch.setShader(shader);

        /* Tell our shader that u_texture will be in the TEXTURE0 spot and
         * u_mask will be in the TEXTURE1 spot. We can set these now since
         * they'll never change; we don't have to send them every render frame. */
        shader.begin();
        shader.setUniformi("u_texture", 0);
        shader.setUniformi("u_mask", 1);
        shader.end();

        /* Pixmap blending can result result in some funky looking lines when
         * drawing. You may need to disable it. */
        //Pixmap.setBlending(Blending.None);

        /* Construct our DrawablePixmap (custom class, defined below) with a
         * Pixmap that is the dimensions of our screen. Alpha format is chosen
         * because we are just using it as a mask and don't care about RGB color
         * information. This will require less memory. */
        drawable = new DrawablePixmap(new Pixmap(Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight(), Format.Alpha), 1);

        Gdx.input.setInputProcessor(new DrawingInput());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        /* Update the mask texture (only if necessary). */
        drawable.update();

        /* Color and outline are drawn separately here, but only to demonstrate
         * this technique supports multiple images in a batch. */
        batch.begin();
        batch.draw(color, 0, 0);
        //batch.draw(outline, 0, 0);
        batch.end();
    }

    @Override
    public void dispose() {
        drawable.dispose();
        outline.dispose();
        color.dispose();
        batch.dispose();
    }

    /**
     * Nested (static) class to provide a nice abstraction over Pixmap, exposing
     * only the draw calls we want and handling some of the logic for smoothed
     * (linear interpolated, aka 'lerped') drawing. This will become the 'owner'
     * of the underlying pixmap, so it will need to be disposed.
     */
    private static class DrawablePixmap implements Disposable {

        private final int brushSize = 10;
        private final Color clearColor = new Color(0, 0, 0, 0);
        private final Color drawColor = new Color(1, 1, 1, 1);

        private Pixmap pixmap;
        private Texture texture;
        private boolean dirty;

        public DrawablePixmap(Pixmap pixmap, int textureBinding) {
            this.pixmap = pixmap;
            pixmap.setColor(drawColor);

            /* Create a texture which we'll update from the pixmap. */
            this.texture = new Texture(pixmap);
            this.dirty = false;

            /* Bind the mask texture to TEXTURE<N> (TEXTURE1 for our purposes),
             * which also sets the currently active texture unit. */
            this.texture.bind(textureBinding);

            /* However SpriteBatch will auto-bind to the current active texture,
             * so we must now reset it to TEXTURE0 or else our mask will be
             * overwritten. */
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        }

        /** Write the pixmap onto the texture if the pixmap has changed. */
        public void update() {
            if (dirty) {
                texture.draw(pixmap, 0, 0);
                dirty = false;
            }
        }

        public void clear() {
            pixmap.setColor(clearColor);
            pixmap.fill();
            pixmap.setColor(drawColor);
            dirty = true;
        }

        private void drawDot(Vector2 spot) {
            pixmap.fillCircle((int) spot.x, (int) spot.y, brushSize);
        }

        public void draw(Vector2 spot) {
            drawDot(spot);
            dirty = true;
        }

        public void drawLerped(Vector2 from, Vector2 to) {
            float dist = to.dst(from);
            /* Calc an alpha step to put one dot roughly every 1/8 of the brush
             * radius. 1/8 is arbitrary, but the results are fairly nice. */
            float alphaStep = brushSize / (8f * dist);

            for (float a = 0; a < 1f; a += alphaStep) {
                Vector2 lerped = from.lerp(to, a);
                drawDot(lerped);
            }

            drawDot(to);
            dirty = true;
        }

        @Override
        public void dispose() {
            texture.dispose();
            pixmap.dispose();
        }
    }

    /**
     * Inner (non-static) class to handle mouse and keyboard events. Mostly we
     * want to pass on appropriate draw calls to our DrawablePixmap and this
     * means keeping track of some state (last coordinates drawn and whether or
     * not the left mouse button is pressed) to handle smooth drawing while
     * dragging the mouse.
     */
    private class DrawingInput extends InputAdapter {

        private Vector2 last = null;
        private boolean leftDown = false;

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer,
                                 int button) {
            if (button == Input.Buttons.LEFT) {
                Vector2 curr = new Vector2(screenX, screenY);
                drawable.draw(curr);
                last = curr;
                leftDown = true;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (leftDown) {
                Vector2 curr = new Vector2(screenX, screenY);
                drawable.drawLerped(last, curr);
                last = curr;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.LEFT) {
                drawable.draw(new Vector2(screenX, screenY));
                last = null;
                leftDown = false;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean keyDown(int keycode) {
            switch (keycode) {
                case Input.Keys.ESCAPE:
                case Input.Keys.F5:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean keyUp(int keycode) {
            switch (keycode) {
                case Input.Keys.ESCAPE:
                    Gdx.app.exit();
                    return true;
                case Input.Keys.F5:
                    drawable.clear();
                    return true;
                default:
                    return false;
            }
        }
    }
}
