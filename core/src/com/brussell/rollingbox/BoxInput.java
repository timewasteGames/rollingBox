package com.brussell.rollingbox;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

/**
 * Keeps track of which direction to go next based on player input.
 */

public class BoxInput extends InputAdapter {
  private RollingBox.RotationDirection _nextDirection = RollingBox.RotationDirection.NONE;

  @Override
  public boolean keyDown(final int keycode) {
    if (keycode == Input.Keys.W) {
      _nextDirection = RollingBox.RotationDirection.BACK;
    }
    else if (keycode == Input.Keys.S) {
      _nextDirection = RollingBox.RotationDirection.FORWARD;
    }
    if (keycode == Input.Keys.A) {
      _nextDirection = RollingBox.RotationDirection.LEFT;
    }
    if (keycode == Input.Keys.D) {
      _nextDirection = RollingBox.RotationDirection.RIGHT;
    }
    return false;
  }

  @Override
  public boolean keyUp(final int keycode) {
    if (keycode == Input.Keys.W && _nextDirection == RollingBox.RotationDirection.BACK) {
      _nextDirection = RollingBox.RotationDirection.NONE;
    }
    else if (keycode == Input.Keys.S && _nextDirection == RollingBox.RotationDirection.FORWARD) {
      _nextDirection = RollingBox.RotationDirection.NONE;
    }
    if (keycode == Input.Keys.A && _nextDirection == RollingBox.RotationDirection.LEFT) {
      _nextDirection = RollingBox.RotationDirection.NONE;
    }
    if (keycode == Input.Keys.D && _nextDirection == RollingBox.RotationDirection.RIGHT) {
      _nextDirection = RollingBox.RotationDirection.NONE;
    }
    return false;
  }

  public RollingBox.RotationDirection getNextDirection() {
    return _nextDirection;
  }
}
