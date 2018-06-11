package com.brussell.rollingbox;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class RollingBox extends Game {
  private static final float BOX_ROTATE_SPEED = 1f;
  private static final float BOX_SIZE = 10f;
  private static final float BOX_DIAGONAL = (float) Math.sqrt(BOX_SIZE * BOX_SIZE + BOX_SIZE * BOX_SIZE) / 2f;
  private static final float Y_OFFSET = -20f;
  private static final float Z_OFFSET = -40f;

  private ModelBatch _modelBatch;
  private Viewport _viewport;
  private ModelInstance _box;
  private ModelInstance _floor;

  private int _gridPosX = 0;
  private int _gridPosZ = 0;
  private float _rotationAcc;
  private Quaternion _tempRotation = new Quaternion();

  private RollingBox.RotationDirection _currentDirection = RollingBox.RotationDirection.NONE;

  public enum RotationDirection {
    FORWARD, BACK, LEFT, RIGHT, NONE
  }

  private BoxInput _boxInput;
  private Environment _environment;

  @Override
  public void create() {
    _modelBatch = new ModelBatch(new DefaultShaderProvider());
    _environment = new Environment();
    _environment.add(new DirectionalLight().set(new Color(Color.GOLD).mul(0.7f), new Vector3(1f, -1f, -1f).nor()));
    _environment.add(new DirectionalLight().set(new Color(Color.GOLD).mul(0.7f), new Vector3(-1f, -1f, -1f).nor()));
    _viewport = new FitViewport(108f, 72f, new PerspectiveCamera());
    _viewport.getCamera().direction.rotate(Vector3.X, -30f);

    ModelBuilder modelBuilder = new ModelBuilder();
    _box = new ModelInstance(modelBuilder.createBox(BOX_SIZE, BOX_SIZE, BOX_SIZE, new Material(ColorAttribute.createDiffuse(Color.GOLD)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal));
    _box.transform.setToTranslation(0f, Y_OFFSET + BOX_SIZE / 2, Z_OFFSET);

    _floor = new ModelInstance(modelBuilder.createLineGrid(10, 10, 10, 10, new Material(ColorAttribute.createDiffuse(Color.BLUE)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal));
    _floor.transform.setToTranslation(0f, Y_OFFSET - 1f, Z_OFFSET);

    _boxInput = new BoxInput();
    Gdx.input.setInputProcessor(_boxInput);
  }

  @Override
  public void render() {
    Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

    _viewport.apply();
    _modelBatch.begin(_viewport.getCamera());
    _modelBatch.render(_box, _environment);
    _modelBatch.render(_floor);
    _modelBatch.end();

    update(Gdx.graphics.getDeltaTime());
  }

  private void update(final float deltaTime) {
    if (_currentDirection == RotationDirection.NONE && _boxInput.getNextDirection() == RotationDirection.NONE) {
      return;
    }
    else if (_currentDirection == RotationDirection.NONE) {
      _currentDirection = _boxInput.getNextDirection();
      _rotationAcc = 0f;
    }

    continueRotation(deltaTime);
  }

  private void continueRotation(final float deltaTime) {
    _rotationAcc += deltaTime * BOX_ROTATE_SPEED * 360f;

    if (_rotationAcc > 90f) {
      finishRotation();
    }

    setTransform(_rotationAcc, _currentDirection, _gridPosX, _gridPosZ);
  }

  private void finishRotation() {
    // Increment/decrement global grid postion
    if (_currentDirection == RotationDirection.FORWARD) {
      _gridPosZ++;
    }
    else if (_currentDirection == RotationDirection.BACK) {
      _gridPosZ--;
    }
    else if (_currentDirection == RotationDirection.RIGHT) {
      _gridPosX++;
    }
    else if (_currentDirection == RotationDirection.LEFT) {
      _gridPosX--;
    }

    // Complete this rotation neatly if there are no more instructions
    if (_boxInput.getNextDirection() == RotationDirection.NONE) {
      _currentDirection = RotationDirection.NONE;
      _rotationAcc = 0f;
    }
    // Move to next instruction if there is one
    else {
      _currentDirection = _boxInput.getNextDirection();
      _rotationAcc %= 90f;
    }
  }

  // Translation is related to where we are in the rotation, the locus of the centre of the box
  // is quarter-circles around a pivoting corner.
  private void setTransform(final float rotation, final RotationDirection direction, final int gridPosX, final int gridPosZ) {
    // Makes the box look "heavy"
    float smoothRotation = Interpolation.pow2In.apply(0f, 90f, rotation / 90f);

    // Position on the circle
    float verticalTranslation = BOX_DIAGONAL * MathUtils.sinDeg(smoothRotation + 45f);
    float horizontalTranslation = BOX_DIAGONAL * MathUtils.cosDeg(smoothRotation + 45f) - BOX_SIZE / 2f;

    // Base translation
    float x = gridPosX * BOX_SIZE;
    float y = verticalTranslation + Y_OFFSET;
    float z = gridPosZ * BOX_SIZE + Z_OFFSET;

    // Correction depending on which way we are rolling
    if (direction == RotationDirection.FORWARD) {
      z -= horizontalTranslation;
      _tempRotation.set(Vector3.X, smoothRotation);
    }
    else if (direction == RotationDirection.BACK) {
      z += horizontalTranslation;
      _tempRotation.set(Vector3.X, -smoothRotation);
    }
    else if (direction == RotationDirection.RIGHT) {
      x -= horizontalTranslation;
      _tempRotation.set(Vector3.Z, -smoothRotation);
    }
    else if (direction == RotationDirection.LEFT) {
      x += horizontalTranslation;
      _tempRotation.set(Vector3.Z, smoothRotation);
    }
    else {
      _tempRotation.idt();
    }

    // Apply the transformation
    _box.transform.setToTranslation(x, y, z);
    _box.transform.rotate(_tempRotation);
  }

  @Override
  public void dispose() {
    _modelBatch.dispose();
  }

  @Override
  public void resize(final int width, final int height) {
    super.resize(width, height);
    _viewport.update(width, height, false);
  }
}
